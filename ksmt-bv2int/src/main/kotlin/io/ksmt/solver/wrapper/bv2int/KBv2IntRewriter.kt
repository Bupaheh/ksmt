package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KAndBinaryExpr
import io.ksmt.expr.KAndExpr
import io.ksmt.expr.KApp
import io.ksmt.expr.KArray2Lambda
import io.ksmt.expr.KArray2Select
import io.ksmt.expr.KArray2Store
import io.ksmt.expr.KArray3Lambda
import io.ksmt.expr.KArray3Select
import io.ksmt.expr.KArray3Store
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KArrayLambda
import io.ksmt.expr.KArrayNLambda
import io.ksmt.expr.KArrayNSelect
import io.ksmt.expr.KArrayNStore
import io.ksmt.expr.KArraySelect
import io.ksmt.expr.KArrayStore
import io.ksmt.expr.KBitVec16Value
import io.ksmt.expr.KBitVec1Value
import io.ksmt.expr.KBitVec32Value
import io.ksmt.expr.KBitVec64Value
import io.ksmt.expr.KBitVec8Value
import io.ksmt.expr.KBitVecCustomValue
import io.ksmt.expr.KBv2IntExpr
import io.ksmt.expr.KBvAddExpr
import io.ksmt.expr.KBvAddNoOverflowExpr
import io.ksmt.expr.KBvAddNoUnderflowExpr
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvArithShiftRightExpr
import io.ksmt.expr.KBvConcatExpr
import io.ksmt.expr.KBvDivNoOverflowExpr
import io.ksmt.expr.KBvExtractExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvMulExpr
import io.ksmt.expr.KBvMulNoOverflowExpr
import io.ksmt.expr.KBvMulNoUnderflowExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNegNoOverflowExpr
import io.ksmt.expr.KBvNegationExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvNotExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvReductionAndExpr
import io.ksmt.expr.KBvReductionOrExpr
import io.ksmt.expr.KBvRepeatExpr
import io.ksmt.expr.KBvRotateLeftExpr
import io.ksmt.expr.KBvRotateLeftIndexedExpr
import io.ksmt.expr.KBvRotateRightExpr
import io.ksmt.expr.KBvRotateRightIndexedExpr
import io.ksmt.expr.KBvShiftLeftExpr
import io.ksmt.expr.KBvSignExtensionExpr
import io.ksmt.expr.KBvSignedDivExpr
import io.ksmt.expr.KBvSignedGreaterExpr
import io.ksmt.expr.KBvSignedGreaterOrEqualExpr
import io.ksmt.expr.KBvSignedLessExpr
import io.ksmt.expr.KBvSignedLessOrEqualExpr
import io.ksmt.expr.KBvSignedModExpr
import io.ksmt.expr.KBvSignedRemExpr
import io.ksmt.expr.KBvSubExpr
import io.ksmt.expr.KBvSubNoOverflowExpr
import io.ksmt.expr.KBvSubNoUnderflowExpr
import io.ksmt.expr.KBvToFpExpr
import io.ksmt.expr.KBvUnsignedDivExpr
import io.ksmt.expr.KBvUnsignedGreaterExpr
import io.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import io.ksmt.expr.KBvUnsignedLessExpr
import io.ksmt.expr.KBvUnsignedLessOrEqualExpr
import io.ksmt.expr.KBvUnsignedRemExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KBvZeroExtensionExpr
import io.ksmt.expr.KConst
import io.ksmt.expr.KDistinctExpr
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExistentialQuantifier
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpFromBvExpr
import io.ksmt.expr.KFpToBvExpr
import io.ksmt.expr.KFpToIEEEBvExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KFunctionAsArray
import io.ksmt.expr.KIteExpr
import io.ksmt.expr.KLeArithExpr
import io.ksmt.expr.KNotExpr
import io.ksmt.expr.KOrBinaryExpr
import io.ksmt.expr.KQuantifier
import io.ksmt.expr.KUniversalQuantifier
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBv16Sort
import io.ksmt.sort.KBv1Sort
import io.ksmt.sort.KBv32Sort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KBv8Sort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KFpSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.mkFreshConst
import io.ksmt.utils.normalizeValue
import io.ksmt.utils.toBigInteger
import io.ksmt.utils.uncheckedCast
import kotlin.math.max

@Suppress("LargeClass")
class KBv2IntRewriter(
    ctx: KContext,
    private val bv2IntContext: KBv2IntContext,
    private val rewriteMode: RewriteMode = RewriteMode.LAZY,
    private val andRewriteMode: AndRewriteMode = AndRewriteMode.SUM,
    private val signednessMode: SignednessMode = SignednessMode.UNSIGNED
) : KNonRecursiveTransformer(ctx) {
    enum class AndRewriteMode {
        SUM,
        BITWISE
    }

    enum class RewriteMode {
        EAGER,
        LAZY
    }

    enum class Signedness {
        UNSIGNED,
        SIGNED,
    }

    enum class SignednessMode {
        UNSIGNED,
        SIGNED_NO_OVERFLOW,
        SIGNED_LAZY_OVERFLOW,
    }

    private val signedness = if (signednessMode == SignednessMode.UNSIGNED) {
        Signedness.UNSIGNED
    } else {
        Signedness.SIGNED
    }

    private val powerOfTwoMaxArg = hashMapOf<KExpr<*>, Long>()

    private val isLazyOverflow: Boolean = signednessMode != SignednessMode.UNSIGNED
    private val overflowSizeBits = hashMapOf<KExpr<KIntSort>, UInt>()

    private val lemmas = hashMapOf<KExpr<*>, KExpr<KBoolSort>>()
    private val bvAndLemmas = hashMapOf<KExpr<*>, KExpr<KBoolSort>>()
    private val bvAndLemmaApplication = hashMapOf<KExpr<KBoolSort>, KExpr<KIntSort>>()

    fun rewriteBv2Int(expr: KExpr<KBoolSort>): KExpr<KBoolSort> = with(ctx) {
        val transformedExpr = apply(expr)

        for (pow in 0L..transformedExpr.getPowerOfTwoMaxArg()) {
            transformedExpr.addLemma(bv2IntContext.mkPowerOfTwoApp(pow.expr) eq mkPowerOfTwoExpr(pow.toUInt()))
        }

        val lemma = mkAnd(LemmaFlatter.flatLemma(transformedExpr.getLemma()))

        mkAnd(transformedExpr, lemma, flat = false).addBvAndLemma(transformedExpr.getBvAndLemma())
    }

    fun bvAndLemmas(expr: KExpr<KBoolSort>): List<KExpr<KBoolSort>> {
        return LemmaFlatter.flatLemma(expr.getBvAndLemma())
    }

    fun extractBvAndApplication(expr: KExpr<KBoolSort>) = bvAndLemmaApplication[expr]

    fun rewriteDecl(decl: KDecl<*>): KDecl<KSort> = with(ctx) {
        mkFuncDecl(
            decl.name + signedness,
            decl.sort.tryRewriteSort(),
            decl.argSorts.map { it.tryRewriteSort() }
        ).also { bv2IntContext.saveDecl(decl, it, signedness) }
    }

    fun getOverflowSizeBits(expr: KExpr<KIntSort>) = overflowSizeBits[expr]

    fun setOverflowSizeBits(expr: KExpr<KIntSort>, sizeBits: UInt) {
        overflowSizeBits[expr] = sizeBits
    }

    private fun KExpr<*>.getPowerOfTwoMaxArg() = powerOfTwoMaxArg.getOrDefault(tryUnwrap(), -1L)

    private fun <T : KSort> KExpr<T>.updatePowerOfTwoMaxArg(value: Long): KExpr<T> = apply {
        powerOfTwoMaxArg[tryUnwrap()] = max(tryUnwrap().getPowerOfTwoMaxArg(), value)
    }

    private fun <T : KSort> KExpr<T>.addLemma(lemma: KExpr<KBoolSort>): KExpr<T> = apply {
        lemmas[tryUnwrap()] = ctx.mkAnd(getLemma(), lemma, flat = false)
    }

    private fun <T : KSort> KExpr<T>.addBvAndLemma(lemma: KExpr<KBoolSort>): KExpr<T> = apply {
        bvAndLemmas[tryUnwrap()] = ctx.mkAnd(getBvAndLemma(), lemma, flat = false)
    }

    private fun KExpr<*>.getLemma() = lemmas.getOrDefault(tryUnwrap(), ctx.trueExpr)

    private fun KExpr<*>.getBvAndLemma() = bvAndLemmas.getOrDefault(tryUnwrap(), ctx.trueExpr)

    private fun KExpr<KBoolSort>.registerApplication(application: KExpr<KIntSort>): KExpr<KBoolSort> = apply {
        if (this !is KAndBinaryExpr) {
            bvAndLemmaApplication[this] = application
        } else {
            lhs.registerApplication(application)
            rhs.registerApplication(application)
        }
    }

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        return expr.distributeDependencies(expr.args)
    }

    override fun <T : KSort> transform(expr: KConst<T>): KExpr<T> =
        transformExprAfterTransformedBv2Int(expr) {
            rewriteDecl(expr.decl).apply(listOf()).tryAddBoundLemmas(expr.sort)
        }


    override fun <T : KSort> transform(expr: KFunctionApp<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.args) { args ->
            rewriteDecl(expr.decl).apply(args).tryAddBoundLemmas(expr.sort)
        }
    }

    override fun <T : KSort> transform(expr: KEqExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int<KBoolSort, KExpr<KSort>, KExpr<KSort>>(
            expr = expr,
            dependency0 = expr.lhs,
            dependency1 = expr.rhs,
            transformer = ::mkEq
        )
    }

    override fun <T : KSort> transform(expr: KDistinctExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.args, transformer = ::mkDistinct)
    }

    override fun <T : KSort> transform(expr: KIteExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int<_, _, _, KExpr<KSort>>(
            expr,
            expr.condition,
            expr.trueBranch,
            expr.falseBranch,
            transformer = ::mkIte
        )
    }

    override fun transform(expr: KBitVec1Value): KExpr<KBv1Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(mkIntNum(if (expr.value) 1L else 0L), 1u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec8Value): KExpr<KBv8Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(expr.numberValue.toUByte().toLong().expr, 8u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec16Value): KExpr<KBv16Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(expr.numberValue.toUShort().toLong().expr, 16u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec32Value): KExpr<KBv32Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(expr.numberValue.toUInt().toLong().expr, 32u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec64Value): KExpr<KBv64Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(
                expr.numberValue.toBigInteger().normalizeValue(expr.sort.sizeBits).expr,
                64u,
                Signedness.UNSIGNED
            )
        }
    }

    override fun transform(expr: KBitVecCustomValue): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            toSignedness(expr.value.normalizeValue(expr.sizeBits).expr, expr.sizeBits, Signedness.UNSIGNED)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { arg: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits

            toSignedness(mkPowerOfTwoExpr(sizeBits) - toUnsigned(arg, sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): KExpr<KBv1Sort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { arg: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            mkIte(
                condition = arg eq toSignedness(
                    mkPowerOfTwoExpr(sizeBits) - 1.expr,
                    expr.sort.sizeBits,
                    Signedness.UNSIGNED
                ),
                trueBranch = toSignedness(1.expr, sizeBits, Signedness.UNSIGNED),
                falseBranch = 0.expr
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvReductionOrExpr<T>): KExpr<KBv1Sort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { arg: KExpr<KIntSort> ->
            mkIte(
                condition = arg eq 0.expr,
                trueBranch = 0.expr,
                falseBranch = toSignedness(1.expr, expr.sort.sizeBits, Signedness.UNSIGNED)
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> =
        when (andRewriteMode) {
            AndRewriteMode.SUM -> transformBvAndSum(expr)
            AndRewriteMode.BITWISE -> transformBvAndBitwise(expr)
        }

    private fun KContext.mkIntExtractBit(arg: KExpr<KIntSort>, bit: UInt) = arg / mkPowerOfTwoExpr(bit) mod 2.expr

    private fun KContext.mkIntBvAnd(arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort>, bit: UInt): KExpr<KIntSort> {
//        return (arg0 / mkPowerOfTwoExpr(bit) mod 2.expr) * (arg1 / mkPowerOfTwoExpr(bit) mod 2.expr)
        return mkIte(
            condition = mkOr(
                mkIntExtractBit(arg0, bit) eq 0.expr,
                mkIntExtractBit(arg1, bit) eq 0.expr
            ),
            trueBranch = 0.expr,
            falseBranch = 1.expr
        )
    }

    private fun KContext.mkIntBvAndBitwise(
        arg0: KExpr<KIntSort>,
        arg1: KExpr<KIntSort>,
        res: KExpr<KIntSort>,
        bit: UInt
    ): KExpr<KBoolSort> {
        return mkIte(
            condition = mkOr(
                mkIntExtractBit(arg0, bit) eq 0.expr,
                mkIntExtractBit(arg1, bit) eq 0.expr
            ),
            trueBranch = mkIntExtractBit(res, bit) eq 0.expr,
            falseBranch = mkIntExtractBit(res, bit) eq 1.expr
        )
    }

    private fun generatePropLemmas(
        arg0: KExpr<KIntSort>,
        arg1: KExpr<KIntSort>,
        sizeBits: UInt
    ): KExpr<KBoolSort> = with(ctx) {
        val application = bv2IntContext.mkBvAndApp(arg0, arg1)

        mkAnd(
            application le arg0,
            application le arg1,
            arg0 eq arg1 implies (application eq arg0),
            application eq bv2IntContext.mkBvAndApp(arg1, arg0),
            arg0 eq 0.expr implies (application eq 0.expr),
            arg0 eq mkPowerOfTwoExpr(sizeBits) - 1.expr implies (application eq arg1),
            arg1 eq 0.expr implies (application eq 0.expr),
            arg1 eq mkPowerOfTwoExpr(sizeBits) - 1.expr implies (application eq arg0)
        )
    }

    private fun <T : KBvSort> transformBvAndSum(expr: KBvAndExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.NONE
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.sort.sizeBits
            var result: KExpr<KIntSort> = 0.expr

            for (i in 0u until expr.sort.sizeBits) {
                result += mkIntBvAnd(
                    arg0 = toUnsigned(arg0.denormalized, sizeBits),
                    arg1 = toUnsigned(arg1.denormalized, sizeBits),
                    bit = i
                ) * mkPowerOfTwoExpr(i)
            }

            when (rewriteMode) {
                RewriteMode.EAGER -> result
                RewriteMode.LAZY -> {
                    val application = bv2IntContext.mkBvAndApp(arg0.normalized, arg1.normalized)
                    val bvAndLemma = application eq result
                    bvAndLemma.registerApplication(application)
                    application.addBvAndLemma(bvAndLemma)
                    application.addLemma(generatePropLemmas(
                        arg0 = toUnsigned(arg0.normalized, sizeBits),
                        arg1 = toUnsigned(arg1.normalized, sizeBits),
                        sizeBits = expr.sort.sizeBits
                    ))

                    application
                }
            }
        }
    }

    private fun <T : KBvSort> transformBvAndBitwise(expr: KBvAndExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.NONE
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val result = if (rewriteMode == RewriteMode.EAGER) {
                intSort.mkFreshConst("bvAnd").also { bv2IntContext.saveAuxDecl(it.decl) }
            } else {
                bv2IntContext.mkBvAndApp(arg0.normalized, arg1.normalized)
            }

            val bvAndLemma = mkAnd(
                (0u until expr.sort.sizeBits).map {
                    mkIntBvAndBitwise(arg0.denormalized, arg1.denormalized, result, it)
//                    mkIntExtractBit(result, it) eq mkIntBvAnd(arg0.denormalized, arg1.denormalized, it)
                }
            )

            when (rewriteMode) {
                RewriteMode.EAGER -> result.addLemma(bvAndLemma)
                RewriteMode.LAZY -> {
                    bvAndLemma.registerApplication(result)
                    result.addLemma(generatePropLemmas(arg0.normalized, arg1.normalized, expr.sort.sizeBits))
                    result.addBvAndLemma(bvAndLemma)
                }
            }

            result.tryAddBoundLemmas(expr.sort)
        }
    }

    private fun <T : KBvSort> rewriteOr(arg0: KExpr<T>, arg1: KExpr<T>): KExpr<T> = with(ctx) {
        mkBvSubExprNoSimplify(
            mkBvAddExprNoSimplify(arg0, arg1),
            mkBvAndExprNoSimplify(arg0, arg1)
        )
    }

    private fun <T : KBvSort> rewriteXor(arg0: KExpr<T>, arg1: KExpr<T>): KExpr<T> = with(ctx) {
        mkBvSubExprNoSimplify(
            rewriteOr(arg0, arg1),
            mkBvAndExprNoSimplify(arg0, arg1)
        )
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> =
        rewriteOr(expr.arg0, expr.arg1).accept(this@KBv2IntRewriter)

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> =
        rewriteXor(expr.arg0, expr.arg1).accept(this@KBv2IntRewriter)

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> = with(ctx) {
        mkBvNotExpr(mkBvAndExpr(expr.arg0, expr.arg1)).accept(this@KBv2IntRewriter)
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> = with(ctx) {
        mkBvNotExprNoSimplify(rewriteOr(expr.arg0, expr.arg1)).accept(this@KBv2IntRewriter)
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> = with(ctx) {
        mkBvNotExprNoSimplify(rewriteXor(expr.arg0, expr.arg1)).accept(this@KBv2IntRewriter)
    }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int<_, KExpr<KIntSort>>(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { arg ->
            val sizeBits = expr.sort.sizeBits
            mkIte(
                arg eq toSignedness(mkPowerOfTwoExpr(sizeBits - 1u), sizeBits, Signedness.UNSIGNED),
                arg,
                -arg
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED,
            checkOverflow = true
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            arg0 + arg1
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED,
            checkOverflow = true
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            arg0 - arg1
        }
    }

    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED,
            checkOverflow = true
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            arg0 * arg1
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            mkIte(
                arg1 eq 0.expr,
                toSignedness(mkPowerOfTwoExpr(expr.sort.sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED),
                toSignedness(toUnsigned(arg0, sizeBits) / toUnsigned(arg1, sizeBits), sizeBits, Signedness.UNSIGNED)
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            val signedArg0 = toSigned(arg0, sizeBits)
            val signedArg1 = toSigned(arg1, sizeBits)
            val signedRes = mkIte(
                condition = mkAnd(
                    arg0 eq toSignedness(mkPowerOfTwoExpr(sizeBits - 1u), sizeBits, Signedness.UNSIGNED),
                    arg1 eq toSignedness(mkPowerOfTwoExpr(sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED)
                ),
                trueBranch = arg0,
                falseBranch = mkIte(
                    condition = signedArg0 gt 0.expr,
                    trueBranch = signedArg0 / signedArg1,
                    falseBranch = -signedArg0 / -signedArg1
                )
            )


            mkIte(
                condition = arg1 eq 0.expr,
                trueBranch = mkIte(
                    condition = signedArg0 ge 0.expr,
                    trueBranch = toSignedness(mkPowerOfTwoExpr(expr.sort.sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED),
                    falseBranch = toSignedness(1.expr, sizeBits, Signedness.UNSIGNED)
                ),
                falseBranch = toSignedness(signedRes, sizeBits, Signedness.SIGNED)
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            val unsignedArg0 = toUnsigned(arg0, sizeBits)
            val unsignedArg1 = toUnsigned(arg1, sizeBits)
            mkIte(
                condition = arg1 eq 0.expr,
                trueBranch = arg0,
                falseBranch = toSignedness(unsignedArg0 mod unsignedArg1, sizeBits, Signedness.UNSIGNED)
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            val signedArg0 = toSigned(arg0, sizeBits)
            val signedArg1 = toSigned(arg1, sizeBits)
            val arg1Flag = mkIte(signedArg1 gt 0.expr, (-1).expr, 1.expr)
            val signedRes = mkIte(
                signedArg0 gt 0.expr,
                signedArg0 mod signedArg1,
                signedArg1 * arg1Flag + (signedArg0 mod signedArg1)
            )

            mkIte(
                arg1 eq 0.expr,
                arg0,
                mkIte(
                    (signedArg0 mod signedArg1) eq 0.expr,
                    0.expr,
                    toSignedness(signedRes, sizeBits, Signedness.SIGNED)
                )
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            val signedArg0 = toSigned(arg0, sizeBits)
            val signedArg1 = toSigned(arg1, sizeBits)
            val signedRes = mkIte(
                signedArg1 gt 0.expr,
                signedArg0 mod signedArg1,
                signedArg1 + (signedArg0 mod signedArg1)
            )

            mkIte(
                arg1 eq 0.expr,
                arg0,
                mkIte(
                    (signedArg0 mod signedArg1) eq 0.expr,
                    0.expr,
                    toSignedness(signedRes, sizeBits, Signedness.SIGNED)
                )
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits

            toUnsigned(arg0, sizeBits) lt toUnsigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toSigned(arg0, sizeBits) lt toSigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits

            toUnsigned(arg0, sizeBits) le toUnsigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toSigned(arg0, sizeBits) le toSigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toUnsigned(arg0, sizeBits) ge toUnsigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toSigned(arg0, sizeBits) ge toSigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toUnsigned(arg0, sizeBits) gt toUnsigned(arg1, sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.arg0.sort.sizeBits
            toSigned(arg0, sizeBits) gt toSigned(arg1, sizeBits)
        }
    }

    override fun transform(expr: KBvConcatExpr): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = WrapMode.NONE,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            arg0.denormalized * mkPowerOfTwoExpr(expr.arg1.sort.sizeBits) + toUnsigned(arg1.normalized, expr.arg1.sort.sizeBits)
        }
    }

    override fun transform(expr: KBvExtractExpr): KExpr<KBvSort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.DENORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { value: KExpr<KIntSort> ->
            when (signedness) {
                Signedness.UNSIGNED -> value / mkPowerOfTwoExpr(expr.low.toUInt())
                Signedness.SIGNED ->  {
                    val unsignedArg = toUnsigned(value, expr.value.sort.sizeBits)
                    val result = unsignedArg / mkPowerOfTwoExpr(expr.low.toUInt()) mod mkPowerOfTwoExpr(expr.sort.sizeBits)

                    unsignedToSigned(result, expr.sort.sizeBits)
                }
            }
        }
    }

    override fun transform(expr: KBvSignExtensionExpr): KExpr<KBvSort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KIntSort> ->
            when (signedness) {
                Signedness.SIGNED -> value
                Signedness.UNSIGNED -> {
                    val valueSizeBits = expr.value.sort.sizeBits
                    val signCondition = value / mkPowerOfTwoExpr(valueSizeBits - 1u) eq 0.expr
                    val extensionBits = (mkPowerOfTwoExpr(expr.extensionSize.toUInt()) - 1.expr) *
                            mkPowerOfTwoExpr(valueSizeBits)

                    mkIte(signCondition, value, value + extensionBits)
                }
            }
        }
    }

    override fun transform(expr: KBvZeroExtensionExpr): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.NORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { value: KExpr<KIntSort> ->
            when (signedness) {
                Signedness.UNSIGNED -> value
                Signedness.SIGNED -> {
                    unsignedToSigned(signedToUnsigned(value, expr.value.sort.sizeBits), expr.sort.sizeBits)
                }
            }
        }
    }

    override fun transform(expr: KBvRepeatExpr): KExpr<KBvSort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KIntSort> ->
            if (expr.repeatNumber <= 0) error("repeat number must be positive")

            var currentValue = toUnsigned(value, expr.value.sort.sizeBits)

            for (i in 1 until expr.repeatNumber) {
                currentValue += value * mkPowerOfTwoExpr(expr.value.sort.sizeBits * i.toUInt())
            }

            toSignedness(currentValue, expr.sort.sizeBits, Signedness.UNSIGNED)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.shift,
            WrapMode.NONE,
            WrapMode.DENORMALIZED
        ) { arg: KBv2IntAuxExpr, shift: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits
            val unsignedRes = arg.denormalized * bv2IntContext.mkPowerOfTwoApp(shift.normalized)

            val result = when (signedness) {
                Signedness.UNSIGNED -> unsignedRes
                Signedness.SIGNED -> unsignedToSigned(unsignedRes mod mkPowerOfTwoExpr(sizeBits), sizeBits)
            }

            mkIte(
                condition = toUnsigned(shift.normalized, expr.shift.sort.sizeBits) ge sizeBits.toLong().expr,
                trueBranch = 0.expr,
                falseBranch = result
            ).updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.shift
        ) { arg: KExpr<KIntSort>, shift: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits

            mkIte(
                condition = toUnsigned(shift, sizeBits) ge sizeBits.toLong().expr,
                trueBranch = 0.expr,
                falseBranch = toSignedness(
                    toUnsigned(arg, sizeBits) / bv2IntContext.mkPowerOfTwoApp(shift),
                    sizeBits,
                    Signedness.UNSIGNED
                )
            ).updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.shift
        ) { arg: KExpr<KIntSort>, shift: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits

            when (signedness) {
                Signedness.SIGNED -> mkIte(
                    shift ge sizeBits.toLong().expr or (shift lt 0.expr),
                    mkIte(arg le 0.expr, 0.expr, (-1).expr),
                    arg / bv2IntContext.mkPowerOfTwoApp(shift)
                )
                Signedness.UNSIGNED -> {
                    val signCondition = arg / mkPowerOfTwoExpr(sizeBits - 1u) eq 0.expr
                    val onesHigherBits = (bv2IntContext.mkPowerOfTwoApp(shift) - 1.expr) *
                            bv2IntContext.mkPowerOfTwoApp(sizeBits.toLong().expr - shift)
                    val result = mkIte(
                        condition = signCondition,
                        trueBranch = arg / bv2IntContext.mkPowerOfTwoApp(shift),
                        falseBranch = onesHigherBits + arg / bv2IntContext.mkPowerOfTwoApp(shift)
                    )

                    mkIte(
                        condition = shift ge sizeBits.toLong().expr,
                        trueBranch = mkIte(signCondition, 0.expr, mkPowerOfTwoExpr(sizeBits) - 1.expr),
                        falseBranch = result
                    )
                }
            }.updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg,
            dependency1 = expr.rotation,
            preprocessMode = WrapMode.NONE,
            postRewriteMode = WrapMode.DENORMALIZED,
            checkOverflow = true
        ) { value: KBv2IntAuxExpr, rotation: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.sort.sizeBits
            val sizeBitsLong = sizeBits.toLong()
            val normalizedRotation = toUnsigned(rotation.normalized, sizeBits) mod sizeBitsLong.expr
            val higherBits = toUnsigned(value.denormalized, sizeBits) * bv2IntContext.mkPowerOfTwoApp(normalizedRotation)
            val lowerBits = toUnsigned(value.normalized, sizeBits) /
                    bv2IntContext.mkPowerOfTwoApp(sizeBitsLong.expr - normalizedRotation)

            toSignedness(higherBits + lowerBits, sizeBits, Signedness.UNSIGNED)
                .updatePowerOfTwoMaxArg(sizeBits.toLong())
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.NONE,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { value: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.sort.sizeBits
            val sizeBitsLong = sizeBits.toLong()
            val normalizedRotation = ((expr.rotationNumber % sizeBitsLong + sizeBitsLong) % sizeBitsLong).toUInt()
            val higherBits = value.denormalized * mkPowerOfTwoExpr(normalizedRotation)
            val lowerBits = value.normalized / mkPowerOfTwoExpr(sizeBits - normalizedRotation)

            higherBits + lowerBits
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.rotation,
            WrapMode.NONE,
            WrapMode.DENORMALIZED
        ) { value: KBv2IntAuxExpr, rotation: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.sort.sizeBits
            val sizeBitsLong = sizeBits.toLong()
            val normalizedRotation = rotation.normalized mod sizeBitsLong.expr
            val lowerBits = value.normalized / bv2IntContext.mkPowerOfTwoApp(normalizedRotation)
            val higherBits = value.denormalized * bv2IntContext.mkPowerOfTwoApp(sizeBitsLong.expr - normalizedRotation)

            (higherBits + lowerBits).updatePowerOfTwoMaxArg(sizeBits.toLong())
        }
    }


    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.NONE,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { value: KBv2IntAuxExpr ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.sort.sizeBits
            val sizeBitsLong = sizeBits.toLong()
            val normalizedRotation = ((expr.rotationNumber % sizeBitsLong + sizeBitsLong) % sizeBitsLong).toUInt()
            val lowerBits = value.normalized / mkPowerOfTwoExpr(normalizedRotation)
            val higherBits = value.denormalized * mkPowerOfTwoExpr(sizeBits - normalizedRotation)

            higherBits + lowerBits
        }
    }

    override fun transform(expr: KBv2IntExpr): KExpr<KIntSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = WrapMode.NORMALIZED,
            postRewriteMode = WrapMode.DENORMALIZED
        ) { arg: KExpr<KIntSort> ->
            val sizeBits = expr.value.sort.sizeBits
            when (expr.isSigned) {
                true -> toSigned(arg, sizeBits)
                false -> toUnsigned(arg, sizeBits)
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val maxValue = if (expr.isSigned) {
                mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u) - 1.expr
            } else {
                mkPowerOfTwoExpr(expr.arg0.sort.sizeBits) - 1.expr
            }

            if (!expr.isSigned) {
                arg0 + arg1 le maxValue
            } else {
                unsignedToSigned(arg0, sizeBits) + unsignedToSigned(arg1, sizeBits) le maxValue
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoUnderflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val minValue = -mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u)

            unsignedToSigned(arg0, sizeBits) + unsignedToSigned(arg1, sizeBits) ge minValue
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val maxValue = mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u) - 1.expr

            unsignedToSigned(arg0, sizeBits) - unsignedToSigned(arg1, sizeBits) le maxValue
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoUnderflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val minValue = if (expr.isSigned) {
                -mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u)
            } else {
                0.expr
            }

            if (!expr.isSigned) {
                arg0 - arg1 ge minValue
            } else {
                unsignedToSigned(arg0, sizeBits) - unsignedToSigned(arg1, sizeBits) ge minValue
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvDivNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            (arg0 neq mkPowerOfTwoExpr(sizeBits - 1u)) or (arg1 neq (mkPowerOfTwoExpr(sizeBits) - 1.expr))
        }
    }

    override fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()
            val sizeBits = expr.value.sort.sizeBits
            value neq mkPowerOfTwoExpr(sizeBits - 1u)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val maxValue = if (expr.isSigned) {
                mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u) - 1.expr
            } else {
                mkPowerOfTwoExpr(expr.arg0.sort.sizeBits) - 1.expr
            }

            if (!expr.isSigned) {
                arg0 * arg1 le maxValue
            } else {
                unsignedToSigned(arg0, sizeBits) * unsignedToSigned(arg1, sizeBits) le maxValue
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoUnderflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            val minValue = -mkPowerOfTwoExpr(expr.arg0.sort.sizeBits - 1u)

            unsignedToSigned(arg0, sizeBits) * unsignedToSigned(arg1, sizeBits) ge minValue
        }
    }

    override fun <T : KFpSort> transform(expr: KFpToBvExpr<T>): KExpr<KBvSort> = with(ctx) {
        TODO()
    }

    override fun <T : KFpSort> transform(expr: KFpToIEEEBvExpr<T>): KExpr<KBvSort> = with(ctx) {
        TODO()
    }

    override fun <T : KFpSort> transform(expr: KFpFromBvExpr<T>): KExpr<T> = with(ctx) {
        TODO()
    }

    override fun <T : KFpSort> transform(expr: KBvToFpExpr<T>): KExpr<T> = with(ctx) {
        TODO()
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayStore<D, R>): KExpr<KArraySort<D, R>> = with(ctx) {
        transformExprAfterTransformedBv2Int<_, KExpr<KArraySort<KSort, KSort>>, _, _>(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index,
            dependency2 = expr.value,
            transformer = ::mkArrayStore
        )
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Store<D0, D1, R>
    ): KExpr<KArray2Sort<D0, D1, R>> = with(ctx) {
        transformExprAfterTransformedBv2Int<_, KExpr<KArray2Sort<KSort, KSort, KSort>>, _, _, _>(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index0,
            dependency2 = expr.index1,
            dependency3 = expr.value,
            transformer = ::mkArrayStore
        )
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Store<D0, D1, D2, R>
    ): KExpr<KArray3Sort<D0, D1, D2, R>> = with(ctx) {
        transformExprAfterTransformedBv2Int<_, KExpr<KArray3Sort<KSort, KSort, KSort, KSort>>, _, _, _, _>(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index0,
            dependency2 = expr.index1,
            dependency3 = expr.index2,
            dependency4 = expr.value,
            transformer = ::mkArrayStore
        )
    }

    override fun <R : KSort> transform(
        expr: KArrayNStore<R>
    ): KExpr<KArrayNSort<R>> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.args) { args ->
            val array = args.first()
            val indices = args.subList(fromIndex = 1, toIndex = args.size - 1)
            val value = args.last()

            mkArrayNStore(array.uncheckedCast(), indices, value)
        }
    }

    override fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): KExpr<R> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.array,
            expr.index
        ) { array: KExpr<KArraySort<KSort, KSort>>, index: KExpr<KSort> ->
            mkArraySelect(array, index.uncheckedCast()).tryAddBoundLemmas(expr.sort)
        }
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Select<D0, D1, R>
    ): KExpr<R> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.array,
            expr.index0,
            expr.index1,
        ) { array: KExpr<KArray2Sort<KSort, KSort, KSort>>, i0: KExpr<KSort>, i1: KExpr<KSort> ->
            mkArraySelect(array, i0.uncheckedCast(), i1.uncheckedCast()).tryAddBoundLemmas(expr.sort)
        }
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Select<D0, D1, D2, R>
    ): KExpr<R> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.array,
            expr.index0,
            expr.index1,
            expr.index2,
        ) { array: KExpr<KArray3Sort<KSort, KSort, KSort, KSort>>,
            i0: KExpr<KSort>,
            i1: KExpr<KSort>,
            i2: KExpr<KSort> ->

            mkArraySelect(
                array,
                i0.uncheckedCast(),
                i1.uncheckedCast(),
                i2.uncheckedCast()
            ).tryAddBoundLemmas(expr.sort)
        }
    }

    override fun <R : KSort> transform(expr: KArrayNSelect<R>): KExpr<R> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.args) { args ->
            val array = args.first()
            val indices = args.subList(fromIndex = 1, toIndex = args.size - 1)
            val value = args.last()

            mkArrayNStore(array.uncheckedCast(), indices, value)
                .tryAddBoundLemmas(expr.sort)
        }
    }

    override fun <A : KArraySortBase<R>, R : KSort> transform(
        expr: KArrayConst<A, R>
    ): KExpr<A> = transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KSort> ->
        ctx.mkArrayConst(expr.sort.tryRewriteSort().uncheckedCast(), value)
    }

    override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> = with(ctx) {
        TODO()
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayLambda<D, R>): KExpr<KArraySort<D, R>> = with(ctx) {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Lambda<D0, D1, R>
    ): KExpr<KArray2Sort<D0, D1, R>> = with(ctx) {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Lambda<D0, D1, D2, R>
    ): KExpr<KArray3Sort<D0, D1, D2, R>> = with(ctx) {
        TODO()
    }

    override fun <R : KSort> transform(expr: KArrayNLambda<R>): KExpr<KArrayNSort<R>> = with(ctx) {
        TODO()
    }

    override fun transform(expr: KExistentialQuantifier): KExpr<KBoolSort> = with(ctx) {
        transformQuantifier(expr, ::mkExistentialQuantifier)
    }

    override fun transform(expr: KUniversalQuantifier): KExpr<KBoolSort> = with(ctx) {
        transformQuantifier(expr, ::mkUniversalQuantifier)
    }

    private inline fun <T : KQuantifier> transformQuantifier(
        expr: T,
        constructor: (KExpr<KBoolSort>, List<KDecl<*>>) -> KExpr<KBoolSort>
    ): KExpr<KBoolSort> = transformExprAfterTransformed(expr, expr.body) { body ->
        QuantifierLemmaDistributor(body, expr.bounds)
            .distributeLemmas(constructor)
            .updatePowerOfTwoMaxArg(body.getPowerOfTwoMaxArg())
    }

    private fun KContext.toSignedness(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
        valueSignedness: Signedness
    ): KExpr<KIntSort> =
        when (signedness) {
            Signedness.UNSIGNED -> toUnsigned(value, sizeBits, valueSignedness)
            Signedness.SIGNED -> toSigned(value, sizeBits, valueSignedness)
        }

    private fun KContext.toSigned(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
        valueSignedness: Signedness = signedness
    ): KExpr<KIntSort> =
        if (valueSignedness == Signedness.SIGNED) {
            value
        } else {
            unsignedToSigned(value, sizeBits)
        }

    private fun KContext.toUnsigned(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
        valueSignedness: Signedness = signedness
    ): KExpr<KIntSort> =
        if (valueSignedness == Signedness.UNSIGNED) {
            value
        } else {
            signedToUnsigned(value, sizeBits)
        }

    private enum class WrapMode {
        NONE,
        NORMALIZED,
        DENORMALIZED
    }

    private fun <T : KSort> KExpr<T>.preprocessArg(mode: WrapMode): KExpr<T> {
        if (this !is KBv2IntAuxExpr) return this

        return when (mode) {
            WrapMode.NONE -> this
            WrapMode.NORMALIZED -> normalized
            WrapMode.DENORMALIZED -> denormalized
        }.uncheckedCast()
    }

    private fun <T : KSort> KExpr<*>.postRewriteResult(mode: WrapMode, sort: KSort): KExpr<T> {
        if (sort !is KBvSort || this is KBv2IntAuxExpr) return this.uncheckedCast()

        require(this.sort is KIntSort)

        if (isLazyOverflow) return KBv2IntAuxExprNormalized(this.uncheckedCast()).uncheckedCast()

        return when (mode) {
            WrapMode.NORMALIZED -> KBv2IntAuxExprNormalized(this.uncheckedCast())
            WrapMode.DENORMALIZED -> KBv2IntAuxExprDenormalized(this.uncheckedCast(), sort.sizeBits)
            WrapMode.NONE -> error("Unexpected KIntSort")
        }.uncheckedCast()
    }

    private fun <T : KSort> KExpr<T>.distributeDependencies(args: List<KExpr<*>>): KExpr<T> = apply {
        if (args.isEmpty()) return this

        args.forEach {
            addLemma(it.getLemma())
            addBvAndLemma(it.getBvAndLemma())
        }
        updatePowerOfTwoMaxArg(args.maxOf { it.getPowerOfTwoMaxArg() })
    }

    private fun <T : KSort> KExpr<T>.addForOverflowCheck(sort: KSort, flag: Boolean): KExpr<T> = apply {
        val expr = this

        if (signedness == Signedness.UNSIGNED || !flag || expr.sort !is KIntSort || sort !is KBvSort) return expr

        if (signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW) {
            setOverflowSizeBits(expr.uncheckedCast(), sort.sizeBits)
        } else {
            expr.tryAddBoundLemmas(sort)
        }
    }

    private inline fun <T : KSort> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        transformer: () -> KExpr<*>
    ): KExpr<T> = transformer().postRewriteResult(postRewriteMode, expr.sort)

    private inline fun <T : KSort, B : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency: KExpr<*>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (B) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency) { arg ->
        transformer(arg.preprocessArg(preprocessMode).uncheckedCast())
            .distributeDependencies(listOf(arg))
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    @Suppress("LongParameterList")
    private inline fun <T : KSort, B0 : KExpr<*>, B1 : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (B0, B1) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1) { arg0, arg1 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1))
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    @Suppress("LongParameterList")
    private inline fun <T : KSort, B0 : KExpr<*>, B1 : KExpr<*>, B2 : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (B0, B1, B2) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { arg0, arg1, arg2 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast(),
            arg2.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1, arg2))
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    @Suppress("LongParameterList")
    private inline fun <
        T : KSort,
        B0 : KExpr<*>,
        B1 : KExpr<*>,
        B2 : KExpr<*>,
        B3 : KExpr<*>
    > transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        dependency3: KExpr<*>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (B0, B1, B2, B3) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(
        expr,
        dependency0,
        dependency1,
        dependency2,
        dependency3
    ) { arg0, arg1, arg2, arg3 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast(),
            arg2.preprocessArg(preprocessMode).uncheckedCast(),
            arg3.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1, arg2, arg3))
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    @Suppress("LongParameterList")
    private inline fun <
        T : KSort,
        B0 : KExpr<*>,
        B1 : KExpr<*>,
        B2 : KExpr<*>,
        B3 : KExpr<*>,
        B4 : KExpr<*>,
    > transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        dependency3: KExpr<*>,
        dependency4: KExpr<*>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (B0, B1, B2, B3, B4) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(
        expr,
        dependency0,
        dependency1,
        dependency2,
        dependency3,
        dependency4
    ) { arg0, arg1, arg2, arg3, arg4 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast(),
            arg2.preprocessArg(preprocessMode).uncheckedCast(),
            arg3.preprocessArg(preprocessMode).uncheckedCast(),
            arg4.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1, arg2, arg3, arg4))
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    private inline fun <T : KSort, A : KSort> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependencies: List<KExpr<A>>,
        preprocessMode: WrapMode = WrapMode.NORMALIZED,
        postRewriteMode: WrapMode = WrapMode.NORMALIZED,
        checkOverflow: Boolean = false,
        transformer: (List<KExpr<KSort>>) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { args ->
        transformer(args.map { it.preprocessArg(preprocessMode) }.uncheckedCast())
            .distributeDependencies(args)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .postRewriteResult(postRewriteMode, expr.sort)
    }

    private fun <T : KSort> KExpr<T>.tryUnwrap(): KExpr<T> =
        if (this is KBv2IntAuxExpr) {
            expr.uncheckedCast()
        } else {
            this
        }

    private fun KSort.tryRewriteSort(): KSort = with(ctx) {
        when (this@tryRewriteSort) {
            is KBvSort -> intSort
            is KArraySort<*, *> -> mkArraySort(domain.tryRewriteSort(), range.tryRewriteSort())
            is KArray2Sort<*, *, *> -> mkArraySort(
                domain0.tryRewriteSort(),
                domain1.tryRewriteSort(),
                range.tryRewriteSort()
            )
            is KArray3Sort<*, *, *, *> -> mkArraySort(
                domain0.tryRewriteSort(),
                domain1.tryRewriteSort(),
                domain2.tryRewriteSort(),
                range.tryRewriteSort()
            )
            is KArrayNSort<*> -> mkArrayNSort(domainSorts.map { it.tryRewriteSort() }, range.tryRewriteSort())
            else -> this@tryRewriteSort
        }
    }

    private fun <T : KSort> KExpr<T>.tryAddBoundLemmas(sort: KSort) = with(ctx) {
        val expr = this@tryAddBoundLemmas
        if (sort !is KBvSort) return expr
        val sizeBits = sort.sizeBits

        when (signedness) {
            Signedness.UNSIGNED -> expr.addLemma(0.expr le expr.uncheckedCast())
                .addLemma(mkPowerOfTwoExpr(sizeBits) gt expr.uncheckedCast())
            Signedness.SIGNED -> expr.addLemma(mkPowerOfTwoExpr(sizeBits - 1u) - 1.expr ge expr.uncheckedCast())
                .addLemma(-mkPowerOfTwoExpr(sizeBits - 1u) le expr.uncheckedCast())
        }

        expr
    }

    private class KBv2IntAuxExprNormalized(
        expr: KExpr<KIntSort>,
    ) : KBv2IntAuxExpr(expr) {
        override val normalized: KExpr<KIntSort> = expr

        override val denormalized: KExpr<KIntSort> = expr
    }

    private class KBv2IntAuxExprDenormalized(
        expr: KExpr<KIntSort>,
        sizeBits: UInt
    ) : KBv2IntAuxExpr(expr) {
        override val normalized: KExpr<KIntSort> = with(ctx) {
            expr mod mkPowerOfTwoExpr(sizeBits)
        }

        override val denormalized: KExpr<KIntSort> = expr
    }

    internal abstract class KBv2IntAuxExpr(
        val expr: KExpr<KIntSort>
    ) : KExpr<KIntSort>(expr.ctx) {
        abstract val normalized: KExpr<KIntSort>

        abstract val denormalized: KExpr<KIntSort>

        override val sort = ctx.intSort
        override fun print(printer: ExpressionPrinter) = expr.print(printer)
        override fun internEquals(other: Any): Boolean = expr.internEquals(other)
        override fun internHashCode(): Int = expr.internHashCode()

        override fun accept(transformer: KTransformerBase): KExpr<KIntSort> {
            error("Unexpected accept call")
        }
    }

    private inner class QuantifierLemmaDistributor(
        private val body: KExpr<KBoolSort>,
        private val bounds: List<KDecl<*>>
    ) : KNonRecursiveTransformer(ctx) {
        private val boundsSet = bounds.toHashSet()
        private var newBody: KExpr<KBoolSort> = body

        inline fun distributeLemmas(
            constructor: (KExpr<KBoolSort>, List<KDecl<*>>) -> KExpr<KBoolSort>
        ): KExpr<KBoolSort> {
            val lemma = apply(body.getLemma())
            val bvAndLemma = apply(body.getBvAndLemma())

            return constructor(newBody, bounds)
                .addLemma(lemma)
                .addBvAndLemma(bvAndLemma)
        }

        override fun <T : KSort> exprTransformationRequired(expr: KExpr<T>): Boolean {
            return expr is KAndExpr
        }

        override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> = with(ctx) {
            transformExprAfterTransformed(expr, expr.lhs, expr.rhs) { l, r ->
                mkAnd(processDependency(l), processDependency(r), flat = false)
            }
        }

        private fun processDependency(expr: KExpr<KBoolSort>): KExpr<KBoolSort> = with(ctx) {
            if (expr is KAndExpr) return expr

            val uninterpretedDecls = KExprUninterpretedDeclCollector.collectUninterpretedDeclarations(expr)

            if (uninterpretedDecls.any { it in boundsSet }) {
                newBody = mkAnd(newBody, expr, flat = false)
                trueExpr
            } else {
                expr
            }
        }
    }
}

private class LemmaFlatter private constructor(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private val lemmas: MutableList<KExpr<KBoolSort>> = mutableListOf()

    override fun <T : KSort> exprTransformationRequired(expr: KExpr<T>): Boolean {
        return expr is KAndBinaryExpr
    }

    override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> =
        transformExprAfterTransformed(expr, expr.lhs, expr.rhs) { l, r ->
            processDependency(l)
            processDependency(r)

            expr
        }

    private fun processDependency(expr: KExpr<KBoolSort>) {
        if (expr is KAndBinaryExpr) return
        lemmas.add(expr)
    }

    companion object {
        fun flatLemma(lemma: KExpr<KBoolSort>) = LemmaFlatter(lemma.ctx).run {
            processDependency(lemma)
            apply(lemma)
            lemmas
        }
    }
}
