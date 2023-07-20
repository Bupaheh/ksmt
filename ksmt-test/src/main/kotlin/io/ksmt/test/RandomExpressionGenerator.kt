package io.ksmt.test

import io.ksmt.KAst
import io.ksmt.KContext
import io.ksmt.expr.KBvExtractExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.expr.KInterpretedValue
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KFp128Sort
import io.ksmt.sort.KFpRoundingModeSort
import io.ksmt.sort.KFpSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KRealSort
import io.ksmt.sort.KSort
import io.ksmt.sort.KSortVisitor
import io.ksmt.sort.KUninterpretedSort
import io.ksmt.utils.uncheckedCast
import java.util.SortedMap
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextUInt
import kotlin.random.nextULong
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import io.ksmt.test.RandomExpressionGenerator.Companion.AstFilter
import io.ksmt.test.RandomExpressionGenerator.Companion.mkKExprSortProvider

/**
 * Expression generation parameters.
 *
 * [seedExpressionsPerSort] -- initial expressions for each KSMT sort.
 *
 * [deepExpressionProbability] -- probability of picking the most deeply nested expression as an argument.
 * Controls the depth of generated expressions.
 * Default: 0.4. In our experiments, the probability value of 0.4 results
 * in an average expression depth of about 0.0023 * #generated expressions, 0.3 -- 0.0017, 0.5 -- 0.0029.
 * For example, if we generate 10000 expressions, with a probability of 0.4, the average expression depth will be 23,
 * 0.3 -- 17 and with a probability of 0.5 -- 29.
 *
 * [generatedListSize] -- allowed sizes of generated lists.
 * Default: 2-10, since lists are mostly used for expressions like And, Or, etc.
 * This size range seems to be the most common for expressions of such kind.
 *
 * [generatedStringLength] -- allowed sizes of generated strings.
 * Default: 7-10, since most of generated strings are names of constants, functions, etc.
 *
 * [possibleIntValues] -- allowed values for Int and UInt values.
 * Default: 3-100, since such values are often used for Bv size or Fp size,
 * and we don't want these values to be extremely large.
 *
 * [possibleStringChars] -- allowed symbols for generated strings.
 * Default: a-z, since most of generated strings are names of constants, functions, etc.
 * */
data class GenerationParameters(
    val seedExpressionsPerSort: Int = 10,
    val deepExpressionProbability: Double = 0.4,
    val generatedListSize: IntRange = 2..10,
    val generatedStringLength: IntRange = 7..10,
    val possibleIntValues: IntRange = 3..100,
    val possibleStringChars: CharRange = 'a'..'z',
    val astFilter: AstFilter = AstFilter()
)

class RandomExpressionGenerator {
    private lateinit var generationContext: GenerationContext

    /**
     * Generate [limit] random expressions with shared subexpressions.
     * */
    fun generate(
        limit: Int,
        context: KContext,
        random: Random = Random(42),
        params: GenerationParameters = GenerationParameters(),
        generatorFilter: (KFunction<*>) -> Boolean = { true }
    ): List<KExpr<*>> {
        generationContext = GenerationContext(
            random = random,
            context = context,
            params = params,
            expressionsAmountEstimation = limit,
            sortsAmountEstimation = 1000
        )

        generateInitialSeed(
            samplesPerSort = params.seedExpressionsPerSort,
            sortGenerators = sortGenerators.filter { generatorFilter(it.function) }
        )

        val filteredGenerators = generators.filter { generatorFilter(it.function) }

        while (generationContext.expressions.size < limit) {
            val generator = filteredGenerators.random(random)

            nullIfGenerationFailed {
                generator.generate(generationContext)
            }
        }

        return generationContext.expressions
    }

    /**
     * Specialized version of [generate] for weighted expression generation
     * */
    fun generate(
        limit: Int,
        context: KContext,
        random: Random = Random(42),
        params: GenerationParameters = GenerationParameters(),
        generatorFilter: (KFunction<*>) -> Boolean = { true },
        weights: Map<String, Double> = mapOf()
    ): List<KExpr<*>> {
        generationContext = GenerationContext(
            random = random,
            context = context,
            params = params,
            expressionsAmountEstimation = limit,
            sortsAmountEstimation = 1000
        )

        generateInitialSeed(
            samplesPerSort = params.seedExpressionsPerSort,
            sortGenerators = sortGenerators.filter { generatorFilter(it.function) }
        )

        val filteredGenerators = generators.filter { generatorFilter(it.function) }
        val sampler = WeightedSampler(
            elements = filteredGenerators,
            weights = filteredGenerators.map { weights.getOrDefault(it.function.name, 1.0) },
            random = Random(random.nextInt())
        )

        while (generationContext.expressions.size < limit) {
            val generator = sampler.sample()

            nullIfGenerationFailed {
                generator.generate(generationContext)
            }
        }

        return generationContext.expressions
    }

    /**
     * Recreate previously generated by [generate] expressions in a new [context].
     * */
    fun replay(context: KContext): List<KExpr<*>> {
        val replayContext = GenerationContext(
            random = Random(0),
            context = context,
            params = GenerationParameters(),
            expressionsAmountEstimation = generationContext.expressions.size,
            sortsAmountEstimation = generationContext.sorts.size
        )

        for (entry in generationContext.trace) {
            val resolvedEntry = resolveTraceEntry(entry, replayContext)
            val result = resolvedEntry.call(context)
            when (result) {
                // We don't care about expression depth since it is unused during replay
                is KExpr<*> -> replayContext.registerExpr(result, depth = 0, inReplayMode = true)
                is KSort -> replayContext.registerSort(result, inReplayMode = true)
            }
        }

        return replayContext.expressions
    }

    private fun resolveTraceEntry(
        entry: FunctionInvocation,
        replayContext: GenerationContext
    ): FunctionInvocation {
        val args = entry.args.map { resolveArgument(it, replayContext) }
        return FunctionInvocation(entry.function, args)
    }

    private fun resolveArgument(argument: Argument, replayContext: GenerationContext): Argument =
        when (argument) {
            is SimpleArgument -> argument
            is ListArgument -> ListArgument(argument.nested.map { resolveArgument(it, replayContext) })
            is ExprArgument -> ExprArgument(replayContext.expressions[argument.idx], argument.idx, argument.depth)
            is SortArgument -> SortArgument(replayContext.sorts[argument.idx], argument.idx)
        }

    private fun generateInitialSeed(samplesPerSort: Int, sortGenerators: List<AstGenerator<SortArgument>>) {
        for (sortGenerator in sortGenerators.sortedBy { it.refSortProviders.isNotEmpty() }) {
            val prevSize = generationContext.expressions.size
            while (generationContext.expressions.size - prevSize < samplesPerSort) {
                nullIfGenerationFailed { sortGenerator.mkSeed(generationContext) }
            }
        }
    }

    companion object {

        val noFreshConstants: (KFunction<*>) -> Boolean = {  it != freshConstGen }

        private val ctxFunctions by lazy {
            KContext::class.members
                .filter { it.visibility == KVisibility.PUBLIC }
                .filterIsInstance<KFunction<*>>()
        }

        private val generators by lazy {
            ctxFunctions
                .asSequence()
                .filter { it.returnType.isKExpr() }
                .map { it.uncheckedCast<KFunction<*>, KFunction<KExpr<*>>>() }
                .filterComplexStringGenerators()
                .mapNotNull {
                    nullIfGenerationFailed {
                        it.mkGenerator()?.uncheckedCast<AstGenerator<*>, AstGenerator<ExprArgument>>()
                    }
                }
                .toList()
        }

        private val sortGenerators by lazy {
            ctxFunctions
                .asSequence()
                .filter { it.returnType.isKSort() }
                .map { it.uncheckedCast<KFunction<*>, KFunction<KSort>>() }
                .mapNotNull {
                    nullIfGenerationFailed {
                        it.mkGenerator()?.uncheckedCast<AstGenerator<*>, AstGenerator<SortArgument>>()
                    }
                }
                .toList()
        }

        private val freshConstGen: KFunction<KExpr<KSort>> by lazy { KContext::mkFreshConst }

        /**
         * Filter out generators that require a string with special format.
         * E.g. binary string for Bv or numeric for IntNum.
         * */
        private fun Sequence<KFunction<KExpr<*>>>.filterComplexStringGenerators() = this
            .filterNot { (it.name == "mkBv" && it.parameters.any { p -> p.type.isSubclassOf(String::class) }) }
            .filterNot { (it.name == "mkBvHex" && it.parameters.any { p -> p.type.isSubclassOf(String::class) }) }
            .filterNot { (it.name == "mkIntNum" && it.parameters.any { p -> p.type.isSubclassOf(String::class) }) }
            .filterNot { (it.name == "mkRealNum" && it.parameters.any { p -> p.type.isSubclassOf(String::class) }) }

        private val boolGen by lazy { generators.single { it.function.match("mkBool", Boolean::class) } }
        private val intGen by lazy { generators.single { it.function.match("mkIntNum", Int::class) } }
        private val realGen by lazy { generators.single { it.function.match("mkRealNum", Int::class) } }
        private val bvGen by lazy { generators.single { it.function.match("mkBv", Int::class, UInt::class) } }
        private val fpGen by lazy { generators.single { it.function.match("mkFp", Double::class, KSort::class) } }
        private val arrayGen by lazy {
            generators.single {
                it.function.match("mkArrayConst", KSort::class, KExpr::class)
            }
        }
        private val fpRmGen by lazy {
            generators.single {
                it.function.match("mkFpRoundingModeExpr", KFpRoundingMode::class)
            }
        }
        private val constGen by lazy {
            generators.single {
                it.function.match("mkConst", String::class, KSort::class)
            }
        }
        private val arraySortGen by lazy {
            sortGenerators.single {
                it.function.match("mkArraySort", KSort::class, KSort::class)
            }
        }
        private val array2SortGen by lazy {
            sortGenerators.single {
                it.function.match("mkArraySort", KSort::class, KSort::class, KSort::class)
            }
        }
        private val array3SortGen by lazy {
            sortGenerators.single {
                it.function.match("mkArraySort", KSort::class, KSort::class, KSort::class, KSort::class)
            }
        }

        private fun KType.isSubclassOf(other: KClass<*>): Boolean =
            when (val cls = classifier) {
                is KClass<*> -> cls.isSubclassOf(other)
                is KTypeParameter -> cls.upperBounds.all { it.isSubclassOf(other) }
                else -> false
            }

        private fun KType.isSimple(): Boolean =
            simpleValueGenerators.keys.any { this.isSubclassOf(it) }

        private fun KType.isKExpr(): Boolean = isSubclassOf(KExpr::class)
        private fun KType.isConst(): Boolean = isSubclassOf(KInterpretedValue::class)
        private fun KType.isKSort(): Boolean = isSubclassOf(KSort::class)
        private fun KType.isKContext(): Boolean = this == KContext::class.createType()

        private fun KFunction<*>.match(name: String, vararg valueParams: KClass<*>): Boolean {
            if (this.name != name) return false
            val actualValueParams = parameters.drop(1)
            if (actualValueParams.size != valueParams.size) return false
            return valueParams.zip(actualValueParams).all { (expect, actual) -> actual.type.isSubclassOf(expect) }
        }

        private fun <T : KAst> KFunction<T>.mkGenerator(): AstGenerator<*>? {
            if (!parametersAreCorrect()) return null
            val valueParams = parameters.drop(1)

            val typeParametersProviders = hashMapOf<String, SortProvider>()
            typeParameters.forEach {
                it.mkReferenceSortProvider(typeParametersProviders)
            }

            val argumentProviders = valueParams.map {
                it.mkArgProvider(typeParametersProviders, name) ?: return null
            }

            return AstGenerator<Argument>(this, typeParametersProviders, argumentProviders) { args ->
                when (name) {
                    /**
                     * Bv repeat operation can enormously increase the size of bv.
                     * To avoid extremely large bvs, we move the repetitions count to the range 1..3.
                     * */
                    "mkBvRepeatExpr", "mkBvRepeatExprNoSimplify" -> listOf(
                        SimpleArgument(((args[0] as SimpleArgument).value as Int) % 3 + 1),
                        args[1]
                    )

                    "mkFpRemExpr" -> {
                        val argSort = (args[0] as? ExprArgument)?.value?.sort as? KFpSort
                        if (argSort != null && argSort.exponentBits > KFp128Sort.exponentBits) {
                            generationFailed(
                                "Exponent size ${argSort.exponentBits} can result in slow fp.rem computation"
                            )
                        }
                        args
                    }

                    /**
                     * Avoid floating point with extremely large exponents since
                     * such expression creation may involve slow computations.
                     * */
                    "mkFpSort" -> {
                        val exponentSize = (args[0] as SimpleArgument).value as UInt
                        val significandSize = (args[1] as SimpleArgument).value as UInt
                        listOf(
                            SimpleArgument(exponentSize.coerceAtMost(KFp128Sort.exponentBits)),
                            SimpleArgument(significandSize)
                        )
                    }

                    else -> args
                }
            }
        }

        private fun KFunction<*>.parametersAreCorrect(): Boolean {
            if (parameters.isEmpty()) return false
            if (parameters.any { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) return false
            if (parameters[0].kind != KParameter.Kind.INSTANCE || !parameters[0].type.isKContext()) return false
            if (parameters.drop(1).any { it.kind != KParameter.Kind.VALUE }) return false
            return true
        }

        private fun KParameter.mkArgProvider(typeParametersProviders: MutableMap<String, SortProvider>, functionName: String): ArgumentProvider? {
            if (type.isKExpr()) {
                val sortProvider = type.mkKExprSortProvider(typeParametersProviders, functionName)
                return if (type.isConst()) {
                    ConstExprProvider(sortProvider)
                } else {
                    ExprProvider(sortProvider)
                }
            }

            if (type.isKSort()) {
                return type.mkSortProvider(typeParametersProviders)
            }

            if (type.isSubclassOf(List::class)) {
                val elementType = type.arguments.single().type ?: return null
                if (!elementType.isKExpr()) return null
                val sortProvider = elementType.mkKExprSortProvider(typeParametersProviders, functionName)
                return ListProvider(sortProvider)
            }

            if (type.isSimple()) {
                val cls = type.classifier as? KClass<*> ?: return null
                return SimpleProvider(cls)
            }

            val sortClass = type.classifier
            if (sortClass is KTypeParameter && sortClass.name in typeParametersProviders) {
                return type.mkSortProvider(typeParametersProviders)
            }

            return null
        }

        private fun KTypeParameter.mkReferenceSortProvider(references: MutableMap<String, SortProvider>) {
            val sortProvider = if (upperBounds.size == 1 && upperBounds.single().isKSort()) {
                upperBounds.single().mkSortProvider(references)
            } else {
                generationFailed("Not a KSort type argument")
            }
            references[name] = sortProvider
        }

        private fun KType.mkKExprSortProvider(references: MutableMap<String, SortProvider>, functionName: String): SortProvider {
            if (functionName == "mkArrayConst") return ArrayConstValueSortProvider
            val expr = findKExprType()
            val sort = expr.arguments.single()
            if (sort == KTypeProjection.STAR) return SingleSortProvider(KSort::class.java)
            val sortType = sort.type ?: generationFailed("No type available")
            return sortType.mkSortProvider(references)
        }

        private fun KType.findKExprType() = if (classifier == KExpr::class) {
            this
        } else {
            (classifier as? KClass<*>)?.allSupertypes?.find { it.classifier == KExpr::class }
                ?: generationFailed("No KExpr superclass found")
        }

        private fun KType.mkSortProvider(references: MutableMap<String, SortProvider>): SortProvider {
            val sortClass = classifier
            if (sortClass is KTypeParameter) {
                if (sortClass.name !in references) {
                    sortClass.mkReferenceSortProvider(references)
                }
                return ReferenceSortProvider(sortClass.name)
            }

            if (this.isSubclassOf(KArraySortBase::class)) {
                val sortProviders = arguments.map {
                    it.type?.mkSortProvider(references) ?: generationFailed("Array sort type is not available")
                }
                when {
                    this.isSubclassOf(KArraySort::class) ->
                        return ArraySortProvider(sortProviders[0], sortProviders[1])
                    this.isSubclassOf(KArray2Sort::class) ->
                        return Array2SortProvider(sortProviders[0], sortProviders[1], sortProviders[2])
                    this.isSubclassOf(KArray3Sort::class) ->
                        return Array3SortProvider(sortProviders[0], sortProviders[1], sortProviders[2], sortProviders[3])
                }
            }

            if (this.isKSort() && sortClass != null) {
                return SingleSortProvider((sortClass.uncheckedCast<KClassifier, KClass<KSort>>()).java)
            }

            generationFailed("Unexpected type $this")
        }


        private class ConstExprGenerator(
            val sortArgument: SortArgument,
            val generationContext: GenerationContext
        ) : KSortVisitor<AstGenerator<ExprArgument>> {
            override fun visit(sort: KBoolSort): AstGenerator<ExprArgument> = boolGen
            override fun visit(sort: KIntSort): AstGenerator<ExprArgument> = intGen
            override fun visit(sort: KRealSort): AstGenerator<ExprArgument> = realGen
            override fun visit(sort: KFpRoundingModeSort): AstGenerator<ExprArgument> = fpRmGen

            override fun <S : KBvSort> visit(sort: S): AstGenerator<ExprArgument> =
                AstGenerator(bvGen.function, bvGen.refSortProviders, listOf(SimpleProvider(Int::class))) { args ->
                    args + SimpleArgument(sort.sizeBits)
                }

            override fun <S : KFpSort> visit(sort: S): AstGenerator<ExprArgument> =
                AstGenerator(fpGen.function, fpGen.refSortProviders, listOf(SimpleProvider(Double::class))) { args ->
                    args + sortArgument
                }

            override fun visit(sort: KUninterpretedSort): AstGenerator<ExprArgument> =
                AstGenerator(constGen.function, constGen.refSortProviders, listOf(SimpleProvider(String::class))) { args ->
                    args + sortArgument
                }

            private fun <A : KArraySortBase<R>, R : KSort> generateArray(sort: A): AstGenerator<ExprArgument> {
                val rangeSortArgument = SortArgument(sort.range, generationContext.findSortIdx(sort.range))
                val rangeExprGenerator = sort.range.accept(ConstExprGenerator(rangeSortArgument, generationContext))
                val rangeExpr = rangeExprGenerator.generate(generationContext)
                return AstGenerator(arrayGen.function, arrayGen.refSortProviders, emptyList()) {
                    listOf(sortArgument, rangeExpr)
                }
            }

            override fun <D : KSort, R : KSort> visit(sort: KArraySort<D, R>): AstGenerator<ExprArgument> =
                generateArray(sort)

            override fun <D0 : KSort, D1 : KSort, R : KSort> visit(
                sort: KArray2Sort<D0, D1, R>
            ): AstGenerator<ExprArgument> = generateArray(sort)

            override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> visit(
                sort: KArray3Sort<D0, D1, D2, R>
            ): AstGenerator<ExprArgument> = generateArray(sort)

            override fun <R : KSort> visit(sort: KArrayNSort<R>): AstGenerator<ExprArgument> =
                generateArray(sort)
        }

        sealed interface Argument {
            val value: Any
            val depth: Int
        }

        class ListArgument(val nested: List<Argument>) : Argument {
            override val value: Any
                get() = nested.map { it.value }

            override val depth: Int
                get() = nested.maxOf { it.depth }
        }

        class SimpleArgument(override val value: Any) : Argument {
            override val depth: Int = 0
        }

        class SortArgument(override val value: KSort, val idx: Int) : Argument {
            override val depth: Int = 0
        }

        class ExprArgument(override val value: KExpr<*>, val idx: Int, override val depth: Int) : Argument

        private sealed interface ArgumentProvider {
            fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument
        }

        private sealed interface SortProvider : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument =
                resolve(generationContext, references)

            fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument
        }

        private class SingleSortProvider(val sort: Class<KSort>) : SortProvider {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                val candidates = generationContext.sortIndex[sort] ?: generationFailed("No sort matching $sort")
                val idx = candidates.random(generationContext.random)
                return SortArgument(generationContext.sorts[idx], idx)
            }
        }

        private object ArrayConstValueSortProvider : SortProvider {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                val arraySortArgument  = references["A"] ?: generationFailed("Unresolved sort reference $references")
                val arraySort = arraySortArgument.value as KArraySortBase<*>

                return SortArgument(arraySort.range, generationContext.findSortIdx(arraySort.range))
            }
        }

        private class ReferenceSortProvider(val reference: String) : SortProvider {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                return references[reference] ?: generationFailed("Unresolved sort reference $references")
            }
        }

        private abstract class ArraySortProviderBase : SortProvider {
            protected fun resolve(
                generator: AstGenerator<SortArgument>,
                generationContext: GenerationContext,
                references: Map<String, SortArgument>
            ): SortArgument {
                val expr = generator.mkSeed(generationContext, references)
                val sort = expr.value.sort
                val sortIdx = generationContext.findSortIdx(sort)
                return SortArgument(generationContext.sorts[sortIdx], sortIdx)
            }
        }

        private class ArraySortProvider(val domain: SortProvider, val range: SortProvider) : ArraySortProviderBase() {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                val generationParams = mapOf(
                    "D" to domain,
                    "R" to range
                )
                val generator = AstGenerator<SortArgument>(
                    arraySortGen.function,
                    generationParams,
                    generationParams.keys.map { ReferenceSortProvider(it) }
                )
                return resolve(generator, generationContext, references)
            }
        }

        private class Array2SortProvider(
            val d0: SortProvider,
            val d1: SortProvider,
            val range: SortProvider
        ) : ArraySortProviderBase() {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                val generationParams = mapOf(
                    "D0" to d0,
                    "D1" to d1,
                    "R" to range
                )
                val generator = AstGenerator<SortArgument>(
                    array2SortGen.function,
                    generationParams,
                    generationParams.keys.map { ReferenceSortProvider(it) }
                )
                return resolve(generator, generationContext, references)
            }
        }

        private class Array3SortProvider(
            val d0: SortProvider,
            val d1: SortProvider,
            val d2: SortProvider,
            val range: SortProvider
        ) : ArraySortProviderBase() {
            override fun resolve(generationContext: GenerationContext, references: Map<String, SortArgument>): SortArgument {
                val generationParams = mapOf(
                    "D0" to d0,
                    "D1" to d1,
                    "D2" to d2,
                    "R" to range
                )
                val generator = AstGenerator<SortArgument>(
                    array3SortGen.function,
                    generationParams,
                    generationParams.keys.map { ReferenceSortProvider(it) }
                )
                return resolve(generator, generationContext, references)
            }
        }

        private class SimpleProvider(val type: KClass<*>) : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument {
                val value = type.generateSimpleValue(generationContext)
                return SimpleArgument(value)
            }
        }

        private class ListProvider(val element: SortProvider) : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument {
                val concreteSort = element.resolve(generationContext, references).value
                val size = generationContext.random.nextInt(generationContext.params.generatedListSize)
                val candidateExpressions = generationContext.expressionIndex[concreteSort]
                    ?: generationFailed("No expressions for sort $concreteSort")

                val nested = List(size) {
                    val (exprId, exprDepth) = selectRandomExpressionId(generationContext, candidateExpressions)
                    val expr = generationContext.expressions[exprId]
                    ExprArgument(expr, exprId, exprDepth)
                }
                return ListArgument(nested)
            }
        }

        private class ExprProvider(val sort: SortProvider) : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument {
                val concreteSort = sort.resolve(generationContext, references).value
                val candidateExpressions = generationContext.expressionIndex[concreteSort]
                    ?: generationFailed("No expressions for sort $concreteSort")
                val (exprId, exprDepth) = selectRandomExpressionId(generationContext, candidateExpressions)
                return ExprArgument(generationContext.expressions[exprId], exprId, exprDepth)
            }
        }

        private class ConstExprProvider(val sort: SortProvider) : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument {
                val concreteSort = sort.resolve(generationContext, references)
                val constants = generationContext.constantIndex[concreteSort.value] ?: emptyList()

                return if (constants.isNotEmpty()) {
                    val idx = constants.random(generationContext.random)
                    ExprArgument(generationContext.expressions[idx], idx, depth = 1)
                } else {
                    val generator = concreteSort.value.accept(ConstExprGenerator(concreteSort, generationContext))
                    generator.generate(generationContext, references)
                }
            }
        }

        private class AstGenerator<T : Argument>(
            val function: KFunction<*>,
            val refSortProviders: Map<String, SortProvider>,
            val argProviders: List<ArgumentProvider>,
            val provideArguments: (List<Argument>) -> List<Argument> = { it }
        ) : ArgumentProvider {
            override fun provide(generationContext: GenerationContext, references: Map<String, SortArgument>): Argument =
                generate(generationContext, references)

            fun generate(generationContext: GenerationContext, context: Map<String, SortArgument> = emptyMap()): T {
                val resolvedRefProviders = refSortProviders.mapValues {
                    it.value.resolve(generationContext, context)
                }
                val baseArguments = argProviders.map {
                    it.provide(generationContext, context + resolvedRefProviders)
                }
                val arguments = provideArguments(baseArguments)

                val invocation = FunctionInvocation(function, arguments)

                val ast = try {
                    invocation.call(generationContext.context)
                } catch (ex: Throwable) {
                    throw GenerationFailedException("Generator failed", ex)
                }

                if (ast is KExpr<*>) {
                    if (!ast.isCorrect()) {
                        generationFailed("Incorrect ast generated")
                    }
                    if (!generationContext.params.astFilter.filterExpr(ast)) {
                        generationFailed("Expr is filtered")
                    }
                    val depth = (arguments.maxOfOrNull { it.depth } ?: 0) + 1
                    val idx = generationContext.registerExpr(ast, depth)
                    generationContext.trace += invocation
                    return ExprArgument(ast, idx, depth).uncheckedCast()
                }

                if (ast is KSort) {
                    if (!generationContext.params.astFilter.filterSort(ast)) {
                        generationFailed("Sort is filtered")
                    }
                    var idx = generationContext.registerSort(ast)
                    if (idx == -1) {
                        idx = generationContext.findSortIdx(ast)
                    }
                    generationContext.trace += invocation
                    return SortArgument(ast, idx).uncheckedCast()
                }

                generationFailed("Unexpected generation result: $ast")
            }

            private fun KExpr<*>.isCorrect(): Boolean {
                val sort = sort
                if (sort is KBvSort && sort.sizeBits == 0u) return false
                if (this is KBvExtractExpr && high >= value.sort.sizeBits.toLong()) return false
                return true
            }
        }

        private fun AstGenerator<SortArgument>.mkSeed(
            generationContext: GenerationContext,
            context: Map<String, SortArgument> = emptyMap()
        ): ExprArgument {
            val exprGenerator = AstGenerator<ExprArgument>(
                constGen.function,
                emptyMap(),
                listOf(SimpleProvider(String::class), this)
            )
            return exprGenerator.generate(generationContext, context)
        }

        private fun selectRandomExpressionId(
            context: GenerationContext,
            expressionIds: SortedMap<Int, MutableList<Int>>
        ): Pair<Int, Int> {
            val expressionDepth = when (context.random.nextDouble()) {
                in 0.0..context.params.deepExpressionProbability -> expressionIds.lastKey()
                else -> expressionIds.keys.random(context.random)
            }
            val candidateExpressions = expressionIds.getValue(expressionDepth)
            val exprId = candidateExpressions.random(context.random)
            return exprId to expressionDepth
        }

        private val simpleValueGenerators = mapOf<KClass<*>, GenerationContext.(KClass<*>) -> Any>(
            Boolean::class to { random.nextBoolean() },
            Byte::class to { random.nextInt().toByte() },
            UByte::class to { random.nextInt().toUByte() },
            Short::class to { random.nextInt().toShort() },
            UShort::class to { random.nextInt().toUShort() },
            Int::class to { random.nextInt(params.possibleIntValues) },
            UInt::class to {
                val range = params.possibleIntValues.let { it.first.toUInt()..it.last.toUInt() }
                random.nextUInt(range)
            },
            Long::class to { random.nextLong() },
            ULong::class to { random.nextULong() },
            Float::class to { random.nextFloat() },
            Double::class to { random.nextDouble() },
            String::class to {
                val stringLength = random.nextInt(params.generatedStringLength)
                val chars = CharArray(stringLength) { params.possibleStringChars.random(random) }
                String(chars)
            },
            Enum::class to { it.java.enumConstants.random(random) }
        )

        private fun KClass<*>.generateSimpleValue(context: GenerationContext): Any {
            val generator = simpleValueGenerators[this]
                ?: (if (this.isSubclassOf(Enum::class)) simpleValueGenerators[Enum::class] else null)
                ?: generationFailed("Unexpected simple type: $this")
            return context.generator(this)
        }

        private class FunctionInvocation(
            val function: KFunction<*>,
            val args: List<Argument>
        ) {
            fun call(ctx: KContext): Any? {
                val argumentValues = args.map { it.value }
                return function.call(ctx, *argumentValues.toTypedArray())
            }
        }

        private class GenerationContext(
            val random: Random,
            val context: KContext,
            val params: GenerationParameters,
            expressionsAmountEstimation: Int,
            sortsAmountEstimation: Int
        ) {
            val expressions = ArrayList<KExpr<*>>(expressionsAmountEstimation)
            val sorts = ArrayList<KSort>(sortsAmountEstimation)
            val registeredSorts = HashMap<KSort, Int>(sortsAmountEstimation)
            val expressionIndex = hashMapOf<KSort, SortedMap<Int, MutableList<Int>>>()
            val constantIndex = hashMapOf<KSort, MutableList<Int>>()
            val sortIndex = hashMapOf<Class<*>, MutableList<Int>>()
            val trace = ArrayList<FunctionInvocation>(expressionsAmountEstimation + sortsAmountEstimation)

            fun registerSort(sort: KSort, inReplayMode: Boolean = false): Int {
                val knownSortId = registeredSorts[sort]

                val sortId = if (knownSortId != null) {
                    knownSortId
                } else {
                    val idx = sorts.size
                    sorts.add(sort)
                    registeredSorts[sort] = idx
                    idx
                }

                if (knownSortId != null || inReplayMode) {
                    return sortId
                }

                var sortCls: Class<*> = sort::class.java
                while (sortCls != KSort::class.java) {
                    sortIndex.getOrPut(sortCls) { arrayListOf() }.add(sortId)
                    sortCls = sortCls.superclass
                }
                sortIndex.getOrPut(sortCls) { arrayListOf() }.add(sortId)

                return sortId
            }

            fun registerExpr(expr: KExpr<*>, depth: Int, inReplayMode: Boolean = false): Int {
                registerSort(expr.sort, inReplayMode)

                val exprId = expressions.size
                expressions.add(expr)

                if (inReplayMode) {
                    return exprId
                }

                val index = expressionIndex.getOrPut(expr.sort) { sortedMapOf() }
                val expressionIds = index.getOrPut(depth) { arrayListOf() }
                expressionIds.add(exprId)

                if (expr is KInterpretedValue<*>) {
                    constantIndex.getOrPut(expr.sort) { arrayListOf() }.add(exprId)
                }

                return exprId
            }

            fun findSortIdx(sort: KSort): Int =
                registeredSorts[sort] ?: generationFailed("No idx for sort $sort")
        }

        open class AstFilter {
            open fun filterSort(sort: KSort) = true
            open fun filterExpr(expr: KExpr<*>) = true
        }
    }
}

private class WeightedSampler<T>(
    elements: List<T>,
    weights: List<Double>,
    val random: Random
) {
    val data: ArrayList<T> = ArrayList(elements)
    val prefixSum: ArrayList<Double> = ArrayList(weights.scan(0.0) { acc, w -> acc + w }.drop(1))

    fun sample(): T {
        val randomValue = random.nextDouble(0.0, prefixSum.last())
        val idx = prefixSum.binarySearch(randomValue).let { if (it >= 0) it else -it - 1 }

        return data[idx]
    }

}

@Suppress("SwallowedException")
private inline fun <T> nullIfGenerationFailed(body: () -> T): T? = try {
    body()
} catch (ex: GenerationFailedException) {
    null
}

private class GenerationFailedException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

private fun generationFailed(message: String): Nothing =
    throw GenerationFailedException(message)
