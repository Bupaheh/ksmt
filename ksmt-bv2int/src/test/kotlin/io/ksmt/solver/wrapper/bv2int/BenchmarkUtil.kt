package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KConst
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.sort.KBoolSort
import io.ksmt.test.GenerationParameters
import io.ksmt.test.RandomExpressionGenerator
import io.ksmt.utils.uncheckedCast
import kotlin.random.Random
import kotlin.reflect.KFunction

fun bv2IntBenchmarkGeneratorFilter(function: KFunction<*>): Boolean {
    val name = function.name
    if (name == "mkArraySelect" && function.typeParameters.size > 2) return false
    if (name == "mkArrayStore" && function.typeParameters.size > 2) return false
    if (name == "mkArraySort" && function.typeParameters.size > 2) return false

    return name in Bv2IntBenchmarkWeightBuilder.allGenerators
}


fun KContext.generateRandomExpressions(
    size: Int,
    batchSize: Int,
    params: GenerationParameters,
    random: Random,
    isVerbose: Boolean = false,
    weights: Map<String, Double> = mapOf(),
    predicate: (KExpr<KBoolSort>) -> Boolean = { true }
): List<KExpr<KBoolSort>> = List(size) {
    if (isVerbose) println("$it/$size")
    while (true) {
        val expr = try {
            RandomExpressionGenerator().generate(
                batchSize,
                this,
                params = params,
                random = random,
                generatorFilter = ::bv2IntBenchmarkGeneratorFilter,
                weights = weights
            ).last { expr ->
                expr.sort is KBoolSort && expr !is KConst<*>
                        && expr !is KInterpretedValue<*> && predicate(expr.uncheckedCast())
            }
        } catch (_: Exception) { continue }

        return@List expr
    }
}.uncheckedCast()
