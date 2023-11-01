package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KAddArithExpr
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
import io.ksmt.expr.KBitVecValue
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
import io.ksmt.expr.KDivArithExpr
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExistentialQuantifier
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpAbsExpr
import io.ksmt.expr.KFpAddExpr
import io.ksmt.expr.KFpDivExpr
import io.ksmt.expr.KFpEqualExpr
import io.ksmt.expr.KFpFromBvExpr
import io.ksmt.expr.KFpFusedMulAddExpr
import io.ksmt.expr.KFpGreaterExpr
import io.ksmt.expr.KFpGreaterOrEqualExpr
import io.ksmt.expr.KFpIsInfiniteExpr
import io.ksmt.expr.KFpIsNaNExpr
import io.ksmt.expr.KFpIsNegativeExpr
import io.ksmt.expr.KFpIsNormalExpr
import io.ksmt.expr.KFpIsPositiveExpr
import io.ksmt.expr.KFpIsSubnormalExpr
import io.ksmt.expr.KFpIsZeroExpr
import io.ksmt.expr.KFpLessExpr
import io.ksmt.expr.KFpLessOrEqualExpr
import io.ksmt.expr.KFpMaxExpr
import io.ksmt.expr.KFpMinExpr
import io.ksmt.expr.KFpMulExpr
import io.ksmt.expr.KFpNegationExpr
import io.ksmt.expr.KFpRemExpr
import io.ksmt.expr.KFpRoundToIntegralExpr
import io.ksmt.expr.KFpSqrtExpr
import io.ksmt.expr.KFpSubExpr
import io.ksmt.expr.KFpToBvExpr
import io.ksmt.expr.KFpToFpExpr
import io.ksmt.expr.KFpToIEEEBvExpr
import io.ksmt.expr.KFpToRealExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KFunctionAsArray
import io.ksmt.expr.KGeArithExpr
import io.ksmt.expr.KGtArithExpr
import io.ksmt.expr.KImpliesExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.KIsIntRealExpr
import io.ksmt.expr.KIteExpr
import io.ksmt.expr.KLeArithExpr
import io.ksmt.expr.KLtArithExpr
import io.ksmt.expr.KModIntExpr
import io.ksmt.expr.KMulArithExpr
import io.ksmt.expr.KNotExpr
import io.ksmt.expr.KOrBinaryExpr
import io.ksmt.expr.KOrExpr
import io.ksmt.expr.KPowerArithExpr
import io.ksmt.expr.KQuantifier
import io.ksmt.expr.KRealToFpExpr
import io.ksmt.expr.KRemIntExpr
import io.ksmt.expr.KSubArithExpr
import io.ksmt.expr.KToIntRealExpr
import io.ksmt.expr.KToRealIntExpr
import io.ksmt.expr.KUnaryMinusArithExpr
import io.ksmt.expr.KUniversalQuantifier
import io.ksmt.expr.KXorExpr
import io.ksmt.expr.printer.ExpressionPrinter
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KArithSort
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
import io.ksmt.sort.KRealSort
import io.ksmt.sort.KSort
import io.ksmt.utils.BvUtils.toBigIntegerUnsigned
import io.ksmt.utils.mkFreshConst
import io.ksmt.utils.normalizeValue
import io.ksmt.utils.toBigInteger
import io.ksmt.utils.uncheckedCast
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

@Suppress("LargeClass")
class KBv2IntRewriter(
    ctx: KContext,
    private val bv2IntContext: KBv2IntContext,
    private val rewriteMode: RewriteMode = RewriteMode.EAGER,
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

    enum class SignednessMode {
        UNSIGNED,
        SIGNED_LAZY_OVERFLOW,
        SIGNED_LAZY_OVERFLOW_NO_BOUNDS,
        SIGNED
    }

    private val signedness = if (signednessMode == SignednessMode.UNSIGNED) {
        Signedness.UNSIGNED
    } else {
        Signedness.SIGNED
    }

    private val powerOfTwoMaxArg = hashMapOf<KExpr<*>, Long>()

    private val isLazyOverflow: Boolean = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW ||
            signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS

    private val lemmas = hashMapOf<KExpr<*>, KExpr<KBoolSort>>()
    private val overflowLemmas = hashMapOf<KExpr<*>, KExpr<KBoolSort>>()
    private val bvAndLemmas = hashMapOf<KExpr<*>, KExpr<KBoolSort>>()

    fun rewriteBv2Int(expr: KExpr<KBoolSort>): KExpr<KBoolSort> = with(ctx) {
        val transformedExpr = apply(expr)

        for (pow in 0L..transformedExpr.getPowerOfTwoMaxArg()) {
            transformedExpr.addLemma(
                bv2IntContext.powerOfTwoFunc.apply(listOf(pow.expr)) eq mkPowerOfTwoExpr(pow.toUInt())
            )
        }

        val lemma = mkAnd(LemmaFlatter.flatLemma(transformedExpr.getLemma()))

        mkAndNoSimplify(transformedExpr, lemma)
            .addBvAndLemma(transformedExpr.getBvAndLemma())
            .addOverflowLemma(transformedExpr.getOverflowLemma())
    }

    fun bvAndLemmas(expr: KExpr<KBoolSort>): List<KExpr<KBoolSort>> {
        return LemmaFlatter.flatLemma(expr.getBvAndLemma())
    }

    fun overflowLemmas(expr: KExpr<KBoolSort>): KExpr<KBoolSort> = expr.getOverflowLemma()

    fun rewriteDecl(decl: KDecl<*>): KDecl<KSort> = with(ctx) {
        mkFuncDecl(
            decl.name,
            decl.sort.tryRewriteSort(),
            decl.argSorts.map { it.tryRewriteSort() }
        ).also { bv2IntContext.saveDecl(decl, it) }
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

    private fun <T : KSort> KExpr<T>.addOverflowLemma(lemma: KExpr<KBoolSort>): KExpr<T> = apply {
        overflowLemmas[tryUnwrap()] = ctx.mkAnd(getOverflowLemma(), lemma, flat = false)
    }

    private fun KExpr<*>.getLemma() = lemmas.getOrDefault(tryUnwrap(), ctx.trueExpr)

    private fun KExpr<*>.getBvAndLemma() = bvAndLemmas.getOrDefault(tryUnwrap(), ctx.trueExpr)

    private fun KExpr<*>.getOverflowLemma() = overflowLemmas.getOrDefault(tryUnwrap(), ctx.trueExpr)

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        return expr.distributeDependencies(expr.args)
    }

    override fun <T : KSort> transform(expr: KConst<T>): KExpr<T> =
        transformExprAfterTransformedBv2Int(
            expr,
            postRewriteMode = None,
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
        ) {
            val sort = expr.sort
            val const = rewriteDecl(expr.decl).apply(listOf())

            if (sort is KBvSort) {
               KBv2IntAuxExprLazySignedness(const.uncheckedCast(), sort.sizeBits)
            } else {
                const
            }
        }


    override fun <T : KSort> transform(expr: KFunctionApp<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.args,
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
        ) { args ->
            rewriteDecl(expr.decl).apply(args).tryAddBoundLemmas(expr.sort)
        }
    }

    private fun <T : KBvSort> transformBvAndBitwiseEq(
        expr: KExpr<KBoolSort>,
        bvand: KBvAndExpr<T>,
        other: KExpr<T>
    ): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = bvand.arg0,
            dependency1 = bvand.arg1,
            dependency2 = other,
            preprocessMode = None,
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr, other: KBv2IntAuxExpr ->
            val sizeBits = bvand.sort.sizeBits
            val denormalizedOther = other.denormalized

            when {
                bvand.arg0 is KBitVecValue<*> ->
                    rewriteBvAndBitwiseConst(
                        bvand.arg0.uncheckedCast(),
                        arg1,
                        denormalizedOther
                    )
                bvand.arg1 is KBitVecValue<*> ->
                    rewriteBvAndBitwiseConst(
                        bvand.arg1.uncheckedCast(),
                        arg0,
                        denormalizedOther
                    )
                else -> mkAnd(
                    (0u until sizeBits).map {
                        mkIntBvAndBitwise(arg0, arg1, denormalizedOther, it)
                    }
                )
            }
        }.uncheckedCast()
    }

    override fun <T : KSort> transform(expr: KEqExpr<T>): KExpr<KBoolSort> = with(ctx) {
        val lhs = expr.lhs
        val rhs = expr.rhs
        when {
            lhs is KBvAndExpr && andRewriteMode == AndRewriteMode.BITWISE && rewriteMode == RewriteMode.EAGER ->
                transformBvAndBitwiseEq<KBvSort>(expr, lhs.uncheckedCast(), rhs.uncheckedCast())
            rhs is KBvAndExpr && andRewriteMode == AndRewriteMode.BITWISE && rewriteMode == RewriteMode.EAGER ->
                transformBvAndBitwiseEq<KBvSort>(expr, rhs.uncheckedCast(), lhs.uncheckedCast())
            else -> {
                transformExprAfterTransformedBv2Int(
                    expr = expr,
                    dependency0 = expr.lhs,
                    dependency1 = expr.rhs,
                    preprocessMode = None,
                    postRewriteMode = None,
                ) { arg0: KExpr<KSort>, arg1: KExpr<KSort> ->
                    if (arg0 is KBv2IntAuxExpr) {
                        rewriteSignedCmp(arg0, arg1.uncheckedCast(), ::mkEq)
                    } else {
                        arg0.preprocessArg(Normalized(signedness)) eq arg1.preprocessArg(Normalized(signedness))
                    }
                }
            }
        }
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
            KBv2IntAuxExprConst(mkIntNum(if (expr.value) 1L else 0L), 1u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec8Value): KExpr<KBv8Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            KBv2IntAuxExprConst(expr.numberValue.toUByte().toLong().expr, 8u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec16Value): KExpr<KBv16Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            KBv2IntAuxExprConst(expr.numberValue.toUShort().toLong().expr, 16u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec32Value): KExpr<KBv32Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            KBv2IntAuxExprConst(expr.numberValue.toUInt().toLong().expr, 32u, Signedness.UNSIGNED)
        }
    }

    override fun transform(expr: KBitVec64Value): KExpr<KBv64Sort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            KBv2IntAuxExprConst(
                expr.numberValue.toBigInteger().normalizeValue(expr.sort.sizeBits).expr,
                64u,
                Signedness.UNSIGNED
            )
        }
    }

    override fun transform(expr: KBitVecCustomValue): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr) {
            KBv2IntAuxExprConst(expr.value.normalizeValue(expr.sizeBits).expr, expr.sizeBits, Signedness.UNSIGNED)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = Denormalized
        ) { arg: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits

            when {
                canNormalize(arg) -> {
                    val result = if (arg.isNormalizedUnsigned) {
                        mkPowerOfTwoExpr(sizeBits) - 1.expr - arg.normalized(Signedness.UNSIGNED)
                    } else {
                        val signedArg = arg.normalized(Signedness.SIGNED)

                        mkIte(
                            condition = signedArg ge 0.expr,
                            trueBranch = mkPowerOfTwoExpr(sizeBits) - 1.expr - arg.normalized(Signedness.SIGNED),
                            falseBranch = -signedArg - 1.expr
                        )
                    }

                    KBv2IntAuxExprNormalized(
                        result,
                        sizeBits,
                        Signedness.UNSIGNED
                    )
                }

                else -> -arg.denormalized - 1.expr
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): KExpr<KBv1Sort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { arg: KExpr<KIntSort> ->
            TODO("check for bugs")
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
            TODO("check for bugs")
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

    private fun KContext.mkIntBvAnd(arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr, bit: UInt): KExpr<KIntSort> {
//        return (arg0 / mkPowerOfTwoExpr(bit) mod 2.expr) * (arg1 / mkPowerOfTwoExpr(bit) mod 2.expr)
        return mkIte(
            condition = mkOr(
                arg0.extractBit(bit) eq 0.expr,
                arg1.extractBit(bit) eq 0.expr
            ),
            trueBranch = 0.expr,
            falseBranch = 1.expr
        )
    }

    private fun KContext.mkIntBvAndBitwise(
        arg0: KBv2IntAuxExpr,
        arg1: KBv2IntAuxExpr,
        res: KExpr<KIntSort>,
        bit: UInt
    ): KExpr<KBoolSort> {
        val resBit = mkIntExtractBit(res, bit)

        return mkIte(
            condition = mkOr(
                arg0.extractBit(bit) eq 0.expr,
                arg1.extractBit(bit) eq 0.expr
            ),
            trueBranch = resBit eq 0.expr,
            falseBranch = resBit eq 1.expr
        )
    }

//    private fun generatePropLemmas(
//        arg0: KExpr<KIntSort>,
//        arg1: KExpr<KIntSort>,
//        sizeBits: UInt,
//        signedness: Signedness = Signedness.UNSIGNED
//    ): KExpr<KBoolSort> = with(ctx) {
//        val application = bv2IntContext.mkBvAndApp(arg0, arg1)
//        val minusOne = toSignedness(mkPowerOfTwoExpr(sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED, signedness)
//
//        val lazyConstraint = if (isLazyOverflow) {
//            mkOr(arg0 eq 0.expr, arg0 eq minusOne, arg1 eq 0.expr, arg1 eq minusOne, arg0 eq arg1)
//        } else {
//            trueExpr
//        }
//
//        mkAnd(
////            application le arg0,
////            application le arg1,
//            arg0 eq arg1 implies (application eq toUnsigned(arg0, sizeBits, signedness)),
////            application eq bv2IntContext.mkBvAndApp(arg1, arg0),
//            arg0 eq 0.expr implies (application eq 0.expr),
//            arg0 eq minusOne implies (application eq toUnsigned(arg1, sizeBits, signedness)),
//            arg1 eq 0.expr implies (application eq 0.expr),
//            arg1 eq minusOne implies (application eq toUnsigned(arg0, sizeBits, signedness)),
//            lazyConstraint,
//        )
//    }

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

    private fun rewriteBvAndSumConst(const: KBitVecValue<*>, arg: KBv2IntAuxExpr): KExpr<KIntSort> = with(ctx) {
        val stringValue = const.stringValue.reversed()
        var result: KExpr<KIntSort> = 0.expr

        var l = 0
        while (l < stringValue.length) {
            if (stringValue[l] == '0') {
                l++
                continue
            }

            var r = l + 1
            while (r < stringValue.length && stringValue[r] == '1') r++

            result += arg.extractBits((r - 1).toUInt(), l.toUInt()) * mkPowerOfTwoExpr(l.toUInt())

            l = r
        }

        return result
    }

    private fun <T : KBvSort> transformBvAndSum(expr: KBvAndExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = Normalized(Signedness.UNSIGNED)
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits

            val result = when {
                expr.arg0 is KBitVecValue<*> ->
                    rewriteBvAndSumConst(
                        expr.arg0.uncheckedCast(),
                        arg1
                    )
                expr.arg1 is KBitVecValue<*> ->
                    rewriteBvAndSumConst(
                        expr.arg1.uncheckedCast(),
                        arg0
                    )
                else -> {
                    var acc: KExpr<KIntSort> = 0.expr

                    for (i in 0u until expr.sort.sizeBits) {
                        acc += mkIntBvAnd(
                            arg0 = arg0,
                            arg1 = arg1,
                            bit = i
                        ) * mkPowerOfTwoExpr(i)
                    }

                    acc
                }
            }

            when {
                rewriteMode == RewriteMode.EAGER || result is KInterpretedValue -> result
                rewriteMode == RewriteMode.LAZY -> {
                    val normalizedSignedness = Signedness.UNSIGNED

                    val normalizedArg0 = arg0.normalized(normalizedSignedness)
                    val normalizedArg1 = arg1.normalized(normalizedSignedness)
                    val application = bv2IntContext.mkBvAndApp(normalizedArg0, normalizedArg1)
                    val bvAndLemma = application eq result

                    bv2IntContext.registerApplication(bvAndLemma, application)

                    application.addBvAndLemma(bvAndLemma)
                        .addLemma(generatePropLemmas(normalizedArg0, normalizedArg1, sizeBits))
                }
                else -> error("Unexpected")
            }
        }
    }

    private fun rewriteBvAndBitwiseConst(
        const: KBitVecValue<*>,
        arg: KBv2IntAuxExpr,
        result: KExpr<KIntSort>
    ): KExpr<KBoolSort> = with(ctx) {
        val stringValue = const.stringValue.reversed()
        val lemma = mutableListOf<KExpr<KBoolSort>>()

        var l = 0
        while (l < stringValue.length) {
            val c = stringValue[l]

            var r = l + 1
            while (r < stringValue.length && stringValue[r] == c) r++

            val high = (r - 1).toUInt()
            val low = l.toUInt()

            lemma.add(
                if (c == '0') {
                    mkIntExtractBits(result, high, low) eq 0.expr
                } else {
                    mkIntExtractBits(result, high, low) eq arg.extractBits(high, low)
                }
            )

            l = r
        }

        return mkAnd(lemma)
    }

    private fun <T : KBvSort> transformBvAndBitwise(expr: KBvAndExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = Normalized(Signedness.UNSIGNED),
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits
            val normalizedArg0 = arg0.normalized(Signedness.UNSIGNED)
            val normalizedArg1 = arg1.normalized(Signedness.UNSIGNED)
            val result = if (rewriteMode == RewriteMode.EAGER) {
                intSort.mkFreshConst("bvAnd").also { bv2IntContext.saveAuxDecl(it.decl) }
            } else {
                bv2IntContext.mkBvAndApp(normalizedArg0, normalizedArg1)
            }

            val bvAndLemma = when {
                expr.arg0 is KBitVecValue<*> ->
                    rewriteBvAndBitwiseConst(
                        expr.arg0.uncheckedCast(),
                        arg1,
                        result
                    )
                expr.arg1 is KBitVecValue<*> ->
                    rewriteBvAndBitwiseConst(
                        expr.arg1.uncheckedCast(),
                        arg0,
                        result
                    )
                else -> mkAnd(
                    (0u until expr.sort.sizeBits).map {
                        mkIntBvAndBitwise(arg0, arg1, result, it)
                    }
                )
            }

            when (rewriteMode) {
                RewriteMode.EAGER -> result.addLemma(bvAndLemma)
                RewriteMode.LAZY -> {
                    bv2IntContext.registerApplication(bvAndLemma, result)
                    result.addLemma(generatePropLemmas(normalizedArg0, normalizedArg1, sizeBits))
                    result.addBvAndLemma(bvAndLemma)
                }
            }

            result.tryAddBoundLemmas(expr.sort, Signedness.UNSIGNED)
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

    private fun canNormalize(expr: KBv2IntAuxExpr): Boolean =
        expr is KBv2IntAuxExprNormalized || expr is KBv2IntAuxExprSingleOverflow ||
                expr is KBv2IntAuxExprShift && (expr.shift < 0 || isLazyOverflow) ||
                expr is KBv2IntAuxExprExtract && isLazyOverflow ||
                expr is KBv2IntAuxExprZeroExtension || expr is KBv2IntAuxExprConst ||
                expr is KBv2IntAuxExprLazySignedness

    private fun rewriteLia(
        normalizedSignednessArgsValue: KExpr<KIntSort>,
        denormalizedValue: KExpr<KIntSort>,
        canNormalize: Boolean,
        sizeBits: UInt
    ): KBv2IntAuxExpr =
        when {
            signednessMode == SignednessMode.SIGNED && canNormalize ->
                KBv2IntAuxExprSingleOverflow(normalizedSignednessArgsValue, denormalizedValue, sizeBits, signedness)
            isLazyOverflow -> KBv2IntAuxExprNormalized(normalizedSignednessArgsValue, sizeBits, signedness)
            else -> KBv2IntAuxExprDenormalized(denormalizedValue, sizeBits)
        }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { arg: KBv2IntAuxExpr ->
            rewriteLia(-arg.normalized(signedness), -arg.denormalized, canNormalize(arg), expr.sort.sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteLia(
                normalizedSignednessArgsValue = arg0.normalized(signedness) + arg1.normalized(signedness),
                denormalizedValue = arg0.denormalized + arg1.denormalized,
                canNormalize = canNormalize(arg0) && canNormalize(arg1),
                sizeBits = expr.sort.sizeBits
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteLia(
                normalizedSignednessArgsValue = arg0.normalized(signedness) - arg1.normalized(signedness),
                denormalizedValue = arg0.denormalized - arg1.denormalized,
                canNormalize = canNormalize(arg0) && canNormalize(arg1),
                sizeBits = expr.sort.sizeBits
            )
        }
    }

    private fun rewriteBvMulExpr(arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr, sizeBits: UInt): KExpr<KIntSort> = with(ctx) {
        return if (isLazyOverflow) {
            KBv2IntAuxExprNormalized(
                arg0.normalized(signedness) * arg1.normalized(signedness),
                sizeBits,
                signedness,
            )
        } else {
            KBv2IntAuxExprDenormalized(arg0.denormalized * arg1.denormalized, sizeBits)
        }
//        if (!canNormalize(arg0) || !canNormalize(arg1) || signednessMode != SignednessMode.SIGNED) {
//            return KBv2IntAuxExprDenormalized(arg0.denormalized * arg1.denormalized, sizeBits)
//        }
//
//        val const = when {
//            arg0.normalized is KIntNumExpr -> arg0.normalized as KIntNumExpr
//            arg1.normalized is KIntNumExpr -> arg1.normalized as KIntNumExpr
//            else -> return KBv2IntAuxExprDenormalized(arg0.denormalized * arg1.denormalized, sizeBits)
//        }
//
//        val (lowerBound, upperBound) = getBounds(sizeBits, signedness)
//        val step = mkPowerOfTwoExpr(sizeBits)
//        val overflowNum = const.bigIntegerValue.abs() - BigInteger.ONE
//        var result = arg0.normalized * arg1.normalized
//        val powers = mutableListOf<BigInteger>()
//        var power = BigInteger.ONE
//
//        while (power < overflowNum) {
//            powers.add(power)
//            power *= BigInteger.TWO
//        }
//
//        val positive = powers.map { it.expr + 1.expr to upperBound + it.expr * step }.reversed()
//        val negative = powers.map { it.expr + 1.expr to lowerBound - it.expr * step }.reversed()
//
//        positive.forEach { (idx, bound) ->
//            result = mkIte(
//                result gt bound,
//                result - idx * step,
//                result
//            )
//        }
//
//        negative.forEach { (idx, bound) ->
//            result = mkIte(
//                result lt bound,
//                result + idx * step,
//                result
//            )
//        }
//
//        KBv2IntAuxExprSingleOverflow(result, arg0.denormalized * arg1.denormalized, sizeBits)
    }

    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteBvMulExpr(arg0, arg1, expr.sort.sizeBits)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = Normalized(Signedness.UNSIGNED),
            postRewriteMode = Normalized(Signedness.UNSIGNED)
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            mkIte(
                arg1 eq 0.expr,
                mkPowerOfTwoExpr(expr.sort.sizeBits) - 1.expr,
                arg0 / arg1
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = Normalized(Signedness.SIGNED),
            postRewriteMode = Normalized(Signedness.SIGNED)
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val sizeBits = expr.sort.sizeBits
            val signedRes = mkIte(
                condition = mkAnd(
                    arg0 eq toSigned(mkPowerOfTwoExpr(sizeBits - 1u), sizeBits, Signedness.UNSIGNED),
                    arg1 eq toSigned(mkPowerOfTwoExpr(sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED)
                ),
                trueBranch = arg0,
                falseBranch = mkIte(
                    condition = arg0 gt 0.expr,
                    trueBranch = arg0 / arg1,
                    falseBranch = -arg0 / -arg1
                )
            )


            mkIte(
                condition = arg1 eq 0.expr,
                trueBranch = mkIte(
                    condition = arg0 ge 0.expr,
                    trueBranch = toSigned(mkPowerOfTwoExpr(expr.sort.sizeBits) - 1.expr, sizeBits, Signedness.UNSIGNED),
                    falseBranch = toSigned(1.expr, sizeBits, Signedness.UNSIGNED)
                ),
                falseBranch = signedRes
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = Normalized(Signedness.UNSIGNED),
            postRewriteMode = Normalized(Signedness.UNSIGNED),
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            mkIte(
                condition = arg1 eq 0.expr,
                trueBranch = arg0,
                falseBranch = arg0 mod arg1
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            preprocessMode = Normalized(Signedness.SIGNED),
            postRewriteMode = Normalized(Signedness.SIGNED)
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val arg1Flag = mkIte(arg1 gt 0.expr, (-1).expr, 1.expr)
            val signedRes = mkIte(
                arg0 gt 0.expr,
                arg0 mod arg1,
                arg1 * arg1Flag + (arg0 mod arg1)
            )

            mkIte(
                arg1 eq 0.expr,
                arg0,
                mkIte(
                    (arg0 mod arg1) eq 0.expr,
                    0.expr,
                    signedRes
                )
            )
        }
    }

    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            preprocessMode = Normalized(Signedness.SIGNED),
            postRewriteMode = Normalized(Signedness.SIGNED)
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            val signedRes = mkIte(
                arg1 gt 0.expr,
                arg0 mod arg1,
                arg1 + (arg0 mod arg1)
            )

            mkIte(
                arg1 eq 0.expr,
                arg0,
                mkIte(
                    (arg0 mod arg1) eq 0.expr,
                    0.expr,
                    signedRes
                )
            )
        }
    }

    private inline fun rewriteUnsignedCmp(
        arg0: KBv2IntAuxExpr,
        arg1: KBv2IntAuxExpr,
        op: KContext.(KExpr<KIntSort>, KExpr<KIntSort>) -> KExpr<KBoolSort>
    ): KExpr<KBoolSort> = with(ctx) {
//        return op(arg0.normalized(Signedness.UNSIGNED), arg1.normalized(Signedness.UNSIGNED))
        when {
            arg0.isNormalizedUnsigned || arg1.isNormalizedUnsigned ->
                op(arg0.normalized(Signedness.UNSIGNED), arg1.normalized(Signedness.UNSIGNED))
            else -> {
                val signedArg0 = arg0.normalized(Signedness.SIGNED)
                val signedArg1 = arg1.normalized(Signedness.SIGNED)

                val condition = mkOr(
                    signedArg0 ge 0.expr and (signedArg1 ge 0.expr),
                    signedArg0 lt 0.expr and (signedArg1 lt 0.expr)
                )

                mkIte(
                    condition,
                    op(signedArg0, signedArg1),
                    op(signedArg1, signedArg0)
                )
            }
        }
    }

    private inline fun rewriteSignedCmp(
        arg0: KBv2IntAuxExpr,
        arg1: KBv2IntAuxExpr,
        crossinline op: (KExpr<KIntSort>, KExpr<KIntSort>) -> KExpr<KBoolSort>
    ): KExpr<KBoolSort> =
        if (signednessMode == SignednessMode.SIGNED) {
            when {
                arg0 is KBv2IntAuxExprSingleOverflow && arg1 is KBv2IntAuxExprSingleOverflow ->
                    arg0.normalizedOp { a0 -> arg1.normalizedOp { a1 -> op(a0, a1) } }
                arg0 is KBv2IntAuxExprSingleOverflow -> arg0.normalizedOp { arg -> op(arg, arg1.normalized(Signedness.SIGNED)) }
                arg1 is KBv2IntAuxExprSingleOverflow -> arg1.normalizedOp { arg -> op(arg0.normalized(Signedness.SIGNED), arg) }
                else -> op(arg0.normalized(Signedness.SIGNED), arg1.normalized(Signedness.SIGNED))
            }
        } else {
//            println("tut")
//            println(ctx.evalInt(arg0.normalized(Signedness.SIGNED)))
//            println(ctx.evalInt(arg1.normalized(Signedness.SIGNED)))
            op(arg0.normalized(Signedness.SIGNED), arg1.normalized(Signedness.SIGNED))
        }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteUnsignedCmp(arg0, arg1, KContext::mkArithLt)
        }

    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            None,
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteSignedCmp(arg0, arg1, ::mkArithLt)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteUnsignedCmp(arg0, arg1, KContext::mkArithLe)
        }

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            None,
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteSignedCmp(arg0, arg1, ::mkArithLe)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteUnsignedCmp(arg0, arg1, KContext::mkArithGe)
        }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            None,
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteSignedCmp(arg0, arg1, ::mkArithGe)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteUnsignedCmp(arg0, arg1, KContext::mkArithGt)
        }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1,
            None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            rewriteSignedCmp(arg0, arg1, ::mkArithGt)
        }
    }

    override fun transform(expr: KBvConcatExpr): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg0,
            dependency1 = expr.arg1,
            preprocessMode = None,
            postRewriteMode = None
        ) { arg0: KBv2IntAuxExpr, arg1: KBv2IntAuxExpr ->
            val lowerBits = arg1.normalized(Signedness.UNSIGNED)
            val normalizedResultSignedness = if (arg0.isNormalizedSigned) Signedness.SIGNED else Signedness.UNSIGNED
            val normalizedResult =
                arg0.normalized(normalizedResultSignedness) * mkPowerOfTwoExpr(expr.arg1.sort.sizeBits) + lowerBits

            when {
                canNormalize(arg0) -> KBv2IntAuxExprNormalized(
                    normalizedResult,
                    expr.sort.sizeBits,
                    normalizedResultSignedness
                )
                else -> KBv2IntAuxExprDenormalized(
                    arg0.denormalized * mkPowerOfTwoExpr(expr.arg1.sort.sizeBits) + lowerBits,
                    expr.sort.sizeBits
                )
            }
        }
    }

    override fun transform(expr: KBvExtractExpr): KExpr<KBvSort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { value: KBv2IntAuxExpr ->
            if (isLazyOverflow) {
                val lazyOverflowSignedness = if (value.isNormalizedSigned) Signedness.SIGNED else Signedness.UNSIGNED

                KBv2IntAuxExprExtract(
                    denormalized = value.normalized(lazyOverflowSignedness) / mkPowerOfTwoExpr(expr.low.toUInt()),
                    originalExpr = value.denormalized,
                    low = expr.low.toUInt(),
                    sizeBits = expr.sort.sizeBits,
                    lazyOverflowSignedness
                )
            } else {
                KBv2IntAuxExprExtract(
                    denormalized = value.denormalized / mkPowerOfTwoExpr(expr.low.toUInt()),
                    originalExpr = value.denormalized,
                    low = expr.low.toUInt(),
                    sizeBits = expr.sort.sizeBits,
                    Signedness.UNSIGNED
                )
            }
        }
    }

    override fun transform(expr: KBvSignExtensionExpr): KExpr<KBvSort> =
        transformExprAfterTransformedBv2Int(
            expr,
            expr.value,
            Normalized(Signedness.SIGNED),
            Normalized(Signedness.SIGNED)
        ) { value: KExpr<KIntSort> ->
            value
        }

    override fun transform(expr: KBvZeroExtensionExpr): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = Normalized(Signedness.UNSIGNED),
            postRewriteMode = None
        ) { value: KExpr<KIntSort> ->
            KBv2IntAuxExprZeroExtension(
                value,
                expr.sort.sizeBits,
                expr.extensionSize.toUInt()
            )
        }
    }

    override fun transform(expr: KBvRepeatExpr): KExpr<KBvSort> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KIntSort> ->
            TODO("check for bugs")
            if (expr.repeatNumber <= 0) error("repeat number must be positive")

            var currentValue = toUnsigned(value, expr.value.sort.sizeBits)

            for (i in 1 until expr.repeatNumber) {
                currentValue += value * mkPowerOfTwoExpr(expr.value.sort.sizeBits * i.toUInt())
            }

            toSignedness(currentValue, expr.sort.sizeBits, Signedness.UNSIGNED)
        }
    }

    private inline fun <T : KBvSort> tryRewriteShift(
        bvShift: KExpr<T>,
        normalized: KExpr<KIntSort>,
        denormalized: KExpr<KIntSort> = normalized,
        expr: KExpr<KIntSort>,
        sizeBits: UInt,
        defaultBit: KExpr<KIntSort> = with(ctx) { 0.expr },
        unsignedToSignedShift: (Long) -> Long,
        normalizedSignedness: Signedness
    ): KExpr<KIntSort>? =
        if (bvShift is KBitVecValue<*> && bvShift.toBigIntegerUnsigned() < BigInteger.valueOf(sizeBits.toLong())) {
            KBv2IntAuxExprShift(
                normalized = normalized,
                denormalized = denormalized,
                originalExpr = expr,
                shift = unsignedToSignedShift(bvShift.toBigIntegerUnsigned().toLong()),
                sizeBits = sizeBits,
                defaultBit = defaultBit,
                normalizedSignedness = normalizedSignedness
            )
        } else {
            null
        }

    private fun KContext.mkShiftCondition(value: KBv2IntAuxExpr, sizeBits: UInt) =
        if (value.isNormalizedUnsigned) {
            value.normalized(Signedness.UNSIGNED) ge sizeBits.toLong().expr
        } else {
            val signedValue = value.normalized(Signedness.SIGNED)

            mkOr(
                signedValue ge sizeBits.toLong().expr,
                signedValue lt 0.expr
            )
        }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg,
            dependency1 = expr.shift,
            preprocessMode = None,
            postRewriteMode = None,
            checkOverflow = true
        ) { arg: KBv2IntAuxExpr, shift: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits
            val normalizedShift = shift.normalized(Signedness.UNSIGNED)
            val normalizedSignedness: Signedness = if (arg.isNormalizedUnsigned) {
                Signedness.UNSIGNED
            } else {
                Signedness.SIGNED
            }

            val higherBits = if (isLazyOverflow) arg.normalized(normalizedSignedness) else arg.denormalized
            val shifted = higherBits * bv2IntContext.mkPowerOfTwoApp(normalizedShift)
            val result = mkIte(
                condition = normalizedShift ge sizeBits.toLong().expr,
                trueBranch = 0.expr,
                falseBranch = shifted
            )

            val rewrittenShift = tryRewriteShift(
                bvShift = expr.shift,
                normalized = ctx.tryNormalizeExpr(result, sizeBits, normalizedSignedness, normalizedSignedness),
                denormalized = result,
                expr = arg.denormalized,
                sizeBits = sizeBits,
                unsignedToSignedShift = { it },
                normalizedSignedness = normalizedSignedness
            )

            when {
                rewrittenShift == null && isLazyOverflow ->
                    KBv2IntAuxExprNormalized(result, sizeBits, normalizedSignedness)
                rewrittenShift == null -> KBv2IntAuxExprDenormalized(result, sizeBits)
                else -> rewrittenShift
            }.updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.shift,
            preprocessMode = None,
            postRewriteMode = Normalized(Signedness.UNSIGNED),
        ) { arg: KBv2IntAuxExpr, shift: KBv2IntAuxExpr ->
            val normalizedShift = shift.normalized(Signedness.UNSIGNED)
            val sizeBits = expr.sort.sizeBits
            val result = mkIte(
                condition = normalizedShift ge sizeBits.toLong().expr,
                trueBranch = 0.expr,
                falseBranch = arg.normalized(Signedness.UNSIGNED) / bv2IntContext.mkPowerOfTwoApp(normalizedShift),
            )

            (tryRewriteShift(
                bvShift = expr.shift,
                normalized = result,
                expr = arg.denormalized,
                sizeBits = sizeBits,
                unsignedToSignedShift = { -it },
                normalizedSignedness = Signedness.UNSIGNED
            ) ?: result).updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.shift,
            preprocessMode = None,
            postRewriteMode = Normalized(Signedness.SIGNED),
        ) { arg: KBv2IntAuxExpr, shift: KBv2IntAuxExpr ->
            val sizeBits = expr.sort.sizeBits
            val signedArg = arg.normalized(Signedness.SIGNED)
            val signCondition = signedArg ge 0.expr
            val result = mkIte(
                condition = mkShiftCondition(shift, sizeBits),
                trueBranch = mkIte(signCondition, 0.expr, (-1).expr),
                falseBranch = if (shift.isNormalizedUnsigned) {
                    signedArg / bv2IntContext.mkPowerOfTwoApp(shift.normalized(Signedness.UNSIGNED))
                } else {
                    signedArg / bv2IntContext.mkPowerOfTwoApp(shift.normalized(Signedness.SIGNED))
                }
            )

            (tryRewriteShift(
                bvShift = expr.shift,
                normalized = result,
                expr = arg.denormalized,
                sizeBits = sizeBits,
                defaultBit = mkIte(signCondition, 0.expr, 1.expr),
                unsignedToSignedShift = { -it },
                normalizedSignedness = Signedness.SIGNED
            ) ?: result).updatePowerOfTwoMaxArg(sizeBits.toLong() - 1)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency0 = expr.arg,
            dependency1 = expr.rotation,
            preprocessMode = None,
            postRewriteMode = Denormalized,
            checkOverflow = true
        ) { value: KBv2IntAuxExpr, rotation: KBv2IntAuxExpr ->
            TODO("check for bugs")
//            if (signedness == Signedness.SIGNED) TODO()
//
//            val sizeBits = expr.sort.sizeBits
//            val sizeBitsLong = sizeBits.toLong()
//            val normalizedRotation = toUnsigned(rotation.normalized, sizeBits) mod sizeBitsLong.expr
//            val higherBits = toUnsigned(value.denormalized, sizeBits) * bv2IntContext.mkPowerOfTwoApp(normalizedRotation)
//            val lowerBits = toUnsigned(value.normalized, sizeBits) /
//                    bv2IntContext.mkPowerOfTwoApp(sizeBitsLong.expr - normalizedRotation)
//
//            toSignedness(higherBits + lowerBits, sizeBits, Signedness.UNSIGNED)
//                .updatePowerOfTwoMaxArg(sizeBits.toLong())
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = Denormalized
        ) { value: KBv2IntAuxExpr ->
            TODO("check for bugs")
//            if (signedness == Signedness.SIGNED) TODO()
//
//            val sizeBits = expr.sort.sizeBits
//            val sizeBitsLong = sizeBits.toLong()
//            val normalizedRotation = ((expr.rotationNumber % sizeBitsLong + sizeBitsLong) % sizeBitsLong).toUInt()
//            val higherBits = value.denormalized * mkPowerOfTwoExpr(normalizedRotation)
//            val lowerBits = value.normalized / mkPowerOfTwoExpr(sizeBits - normalizedRotation)
//
//            higherBits + lowerBits
        }
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg,
            expr.rotation,
            None,
            Denormalized
        ) { value: KBv2IntAuxExpr, rotation: KBv2IntAuxExpr ->
            TODO("check for bugs")
//            if (signedness == Signedness.SIGNED) TODO()
//
//            val sizeBits = expr.sort.sizeBits
//            val sizeBitsLong = sizeBits.toLong()
//            val normalizedRotation = rotation.normalized mod sizeBitsLong.expr
//            val lowerBits = value.normalized / bv2IntContext.mkPowerOfTwoApp(normalizedRotation)
//            val higherBits = value.denormalized * bv2IntContext.mkPowerOfTwoApp(sizeBitsLong.expr - normalizedRotation)
//
//            (higherBits + lowerBits).updatePowerOfTwoMaxArg(sizeBits.toLong())
        }
    }


    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): KExpr<T> = with(ctx) {
        this@KBv2IntRewriter.transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = Denormalized
        ) { value: KBv2IntAuxExpr ->
            TODO("check for bugs")
//            if (signedness == Signedness.SIGNED) TODO()
//
//            val sizeBits = expr.sort.sizeBits
//            val sizeBitsLong = sizeBits.toLong()
//            val normalizedRotation = ((expr.rotationNumber % sizeBitsLong + sizeBitsLong) % sizeBitsLong).toUInt()
//            val lowerBits = value.normalized / mkPowerOfTwoExpr(normalizedRotation)
//            val higherBits = value.denormalized * mkPowerOfTwoExpr(sizeBits - normalizedRotation)
//
//            higherBits + lowerBits
        }
    }

    override fun transform(expr: KBv2IntExpr): KExpr<KIntSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr = expr,
            dependency = expr.value,
            preprocessMode = None,
            postRewriteMode = None
        ) { arg: KBv2IntAuxExpr ->
            when (expr.isSigned) {
                true -> arg.normalized(Signedness.SIGNED)
                false -> arg.normalized(Signedness.UNSIGNED)
            }
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(
            expr,
            expr.arg0,
            expr.arg1
        ) { arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort> ->
            TODO("check for bugs")
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
            TODO("check for bugs")
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
            TODO("check for bugs")
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
            TODO("check for bugs")
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
            TODO("check for bugs")
            if (signedness == Signedness.SIGNED) TODO()

            val sizeBits = expr.arg0.sort.sizeBits
            (arg0 neq mkPowerOfTwoExpr(sizeBits - 1u)) or (arg1 neq (mkPowerOfTwoExpr(sizeBits) - 1.expr))
        }
    }

    override fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformedBv2Int(expr, expr.value) { value: KExpr<KIntSort> ->
            TODO("check for bugs")
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
            TODO("check for bugs")
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
            TODO("check for bugs")
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
            expr.index,
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
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
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
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
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
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
        transformExprAfterTransformedBv2Int(
            expr,
            expr.args,
            checkOverflow = signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS && expr.sort is KBvSort
        ) { args ->
            val array: KExpr<KArrayNSort<KSort>> = args.first().uncheckedCast()
            val indices = args.subList(fromIndex = 1, toIndex = args.size)

            mkArrayNSelect(array, indices).tryAddBoundLemmas(expr.sort)
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

    override fun transform(expr: KAndExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.args
        ) { args -> mkAnd(args, flat = false, order = false) }

    override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.lhs, expr.rhs
        ) { l, r -> mkAnd(l, r, flat = false, order = false) }

    override fun transform(expr: KOrExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.args
        ) { args -> mkOr(args, flat = false) }

    override fun transform(expr: KOrBinaryExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.lhs, expr.rhs
        ) { l, r -> mkOr(l, r, flat = false, order = false) }

    override fun transform(expr: KNotExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg, KContext::mkNot)

    override fun transform(expr: KImpliesExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.p, expr.q, KContext::mkImplies)

    override fun transform(expr: KXorExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.a, expr.b, KContext::mkXor)

    override fun <T : KFpSort> transform(expr: KFpAbsExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpAbsExpr)

    override fun <T : KFpSort> transform(expr: KFpNegationExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpNegationExpr)

    override fun <T : KFpSort> transform(expr: KFpAddExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.arg0, expr.arg1, KContext::mkFpAddExpr
        )

    override fun <T : KFpSort> transform(expr: KFpSubExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.arg0, expr.arg1, KContext::mkFpSubExpr
        )

    override fun <T : KFpSort> transform(expr: KFpMulExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.arg0, expr.arg1, KContext::mkFpMulExpr
        )

    override fun <T : KFpSort> transform(expr: KFpDivExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.arg0, expr.arg1, KContext::mkFpDivExpr
        )

    override fun <T : KFpSort> transform(expr: KFpFusedMulAddExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.arg0, expr.arg1, expr.arg2, KContext::mkFpFusedMulAddExpr
        )

    override fun <T : KFpSort> transform(expr: KFpSqrtExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.value, KContext::mkFpSqrtExpr
        )

    override fun <T : KFpSort> transform(expr: KFpRemExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpRemExpr)

    override fun <T : KFpSort> transform(expr: KFpRoundToIntegralExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.value, KContext::mkFpRoundToIntegralExpr
        )

    override fun <T : KFpSort> transform(expr: KFpMinExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpMinExpr)

    override fun <T : KFpSort> transform(expr: KFpMaxExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpMaxExpr)

    override fun <T : KFpSort> transform(expr: KFpLessOrEqualExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpLessOrEqualExpr)

    override fun <T : KFpSort> transform(expr: KFpLessExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpLessExpr)

    override fun <T : KFpSort> transform(expr: KFpGreaterOrEqualExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.arg0, expr.arg1, KContext::mkFpGreaterOrEqualExpr
        )

    override fun <T : KFpSort> transform(expr: KFpGreaterExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpGreaterExpr)

    override fun <T : KFpSort> transform(expr: KFpEqualExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg0, expr.arg1, KContext::mkFpEqualExpr)

    override fun <T : KFpSort> transform(expr: KFpIsNormalExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsNormalExpr)

    override fun <T : KFpSort> transform(expr: KFpIsSubnormalExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsSubnormalExpr)

    override fun <T : KFpSort> transform(expr: KFpIsZeroExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsZeroExpr)

    override fun <T : KFpSort> transform(expr: KFpIsInfiniteExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsInfiniteExpr)

    override fun <T : KFpSort> transform(expr: KFpIsNaNExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsNaNExpr)

    override fun <T : KFpSort> transform(expr: KFpIsNegativeExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsNegativeExpr)

    override fun <T : KFpSort> transform(expr: KFpIsPositiveExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpIsPositiveExpr)

    override fun <T : KFpSort> transform(expr: KFpToRealExpr<T>): KExpr<KRealSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.value, KContext::mkFpToRealExpr)

    override fun <T : KFpSort> transform(expr: KFpToFpExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.value
        ) { rm, value -> mkFpToFpExpr(expr.sort, rm, value) }

    override fun <T : KFpSort> transform(expr: KRealToFpExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(
            expr, expr.roundingMode, expr.value
        ) { rm, value -> mkRealToFpExpr(expr.sort, rm, value) }

    override fun <T : KArithSort> transform(expr: KAddArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.args, KContext::mkArithAdd)

    override fun <T : KArithSort> transform(expr: KMulArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.args, KContext::mkArithMul)

    override fun <T : KArithSort> transform(expr: KSubArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.args, KContext::mkArithSub)

    override fun <T : KArithSort> transform(expr: KUnaryMinusArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg, KContext::mkArithUnaryMinus)

    override fun <T : KArithSort> transform(expr: KDivArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithDiv)

    override fun <T : KArithSort> transform(expr: KPowerArithExpr<T>): KExpr<T> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithPower)

    override fun <T : KArithSort> transform(expr: KLtArithExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithLt)

    override fun <T : KArithSort> transform(expr: KLeArithExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithLe)

    override fun <T : KArithSort> transform(expr: KGtArithExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithGt)

    override fun <T : KArithSort> transform(expr: KGeArithExpr<T>): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkArithGe)

    override fun transform(expr: KModIntExpr): KExpr<KIntSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkIntMod)

    override fun transform(expr: KRemIntExpr): KExpr<KIntSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.lhs, expr.rhs, KContext::mkIntRem)

    override fun transform(expr: KToRealIntExpr): KExpr<KRealSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg, KContext::mkIntToReal)

    override fun transform(expr: KToIntRealExpr): KExpr<KIntSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg, KContext::mkRealToInt)

    override fun transform(expr: KIsIntRealExpr): KExpr<KBoolSort> =
        transformExprAfterTransformedBv2IntDefault(expr, expr.arg, KContext::mkRealIsInt)

    private fun KContext.toSignedness(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
        valueSignedness: Signedness
    ): KExpr<KIntSort> = toSignedness(value, sizeBits, valueSignedness, signedness)

    private fun KContext.toSigned(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
    ): KExpr<KIntSort> = toSigned(value, sizeBits, signedness)

    private fun KContext.toUnsigned(
        value: KExpr<KIntSort>,
        sizeBits: UInt,
    ): KExpr<KIntSort> = toUnsigned(value, sizeBits, signedness)

    private fun <T : KSort> KExpr<T>.preprocessArg(mode: WrapMode): KExpr<T> {
        if (this !is KBv2IntAuxExpr) return this

        return when (mode) {
            is None -> this
            is Denormalized -> denormalized
            is Normalized -> normalized(mode.signedness)
        }.uncheckedCast()
    }

    private fun KExpr<*>.postRewriteResult(mode: WrapMode, sort: KSort): KExpr<*> {
        if (sort !is KBvSort || this is KBv2IntAuxExpr) return this.uncheckedCast()

        require(this.sort is KIntSort)

        return when (mode) {
            is None -> error("Unexpected KInstSort")
            is Denormalized -> KBv2IntAuxExprDenormalized(
                this.uncheckedCast(),
                sizeBits = sort.sizeBits,
            )
            is Normalized -> KBv2IntAuxExprNormalized(
                normalized = this.uncheckedCast(),
                sizeBits = sort.sizeBits,
                normalizedSignedness = mode.signedness
            )
        }
    }

    private fun <T : KSort> KExpr<T>.distributeDependencies(args: List<KExpr<*>>): KExpr<T> = apply {
        if (args.isEmpty()) return this

        args.forEach {
            addLemma(it.getLemma())
            addBvAndLemma(it.getBvAndLemma())
            addOverflowLemma(it.getOverflowLemma())
        }
        updatePowerOfTwoMaxArg(args.maxOf { it.getPowerOfTwoMaxArg() })
    }

    /**
     *  should be used after postRewriteResult
     *  */
    private fun <T : KSort> KExpr<T>.addForOverflowCheck(sort: KSort, flag: Boolean): KExpr<T> = with(ctx) {
        val expr = this@addForOverflowCheck

        if (!isLazyOverflow || !flag || sort !is KBvSort) return expr

        require(expr is KBv2IntAuxExprNormalized || expr is KBv2IntAuxExprExtract ||
                expr is KBv2IntAuxExprShift || expr is KBv2IntAuxExprLazySignedness) { "Unexpected" }

        val normalizedValue = (expr as KBv2IntAuxExpr).normalized(signedness)
        val (lowerBound, upperBound) = getBounds(sort.sizeBits, signedness)
        val lemma = (normalizedValue ge lowerBound) and (normalizedValue le upperBound)

        addOverflowLemma(lemma)

        expr
    }

    private inline fun <T : KSort> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        postRewriteMode: WrapMode = Normalized(signedness),
        checkOverflow: Boolean = false,
        transformer: () -> KExpr<*>
    ): KExpr<T> = transformer()
        .postRewriteResult(postRewriteMode, expr.sort)
        .addForOverflowCheck(expr.sort, checkOverflow)
        .uncheckedCast()

    private inline fun <T : KSort, B : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency: KExpr<*>,
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
        checkOverflow: Boolean = false,
        transformer: (B) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency) { arg ->
        transformer(arg.preprocessArg(preprocessMode).uncheckedCast())
            .distributeDependencies(listOf(arg))
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
    }

    @Suppress("LongParameterList")
    private inline fun <T : KSort, B0 : KExpr<*>, B1 : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
        checkOverflow: Boolean = false,
        transformer: (B0, B1) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1) { arg0, arg1 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1))
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
    }

    @Suppress("LongParameterList")
    private inline fun <T : KSort, B0 : KExpr<*>, B1 : KExpr<*>, B2 : KExpr<*>> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
        checkOverflow: Boolean = false,
        transformer: (B0, B1, B2) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { arg0, arg1, arg2 ->
        transformer(
            arg0.preprocessArg(preprocessMode).uncheckedCast(),
            arg1.preprocessArg(preprocessMode).uncheckedCast(),
            arg2.preprocessArg(preprocessMode).uncheckedCast()
        ).distributeDependencies(listOf(arg0, arg1, arg2))
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
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
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
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
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
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
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
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
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
    }

    private inline fun <T : KSort, A : KSort> transformExprAfterTransformedBv2Int(
        expr: KExpr<T>,
        dependencies: List<KExpr<A>>,
        preprocessMode: WrapMode = Normalized(signedness),
        postRewriteMode: WrapMode = Normalized(signedness),
        checkOverflow: Boolean = false,
        transformer: (List<KExpr<KSort>>) -> KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { args ->
        transformer(args.map { it.preprocessArg(preprocessMode) }.uncheckedCast())
            .distributeDependencies(args)
            .postRewriteResult(postRewriteMode, expr.sort)
            .addForOverflowCheck(expr.sort, checkOverflow)
            .uncheckedCast()
    }

    private inline fun <In : KExpr<T>, Out : KExpr<T>, T : KSort, A : KSort> transformExprAfterTransformedBv2IntDefault(
        expr: In,
        dependencies: List<KExpr<A>>,
        transformer: KContext.(List<KExpr<A>>) -> Out
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { transformedDependencies ->
        return ctx.transformer(transformedDependencies)
            .distributeDependencies(transformedDependencies)
    }

    private inline fun <In : KExpr<T>, Out : KExpr<T>, T : KSort, A : KSort> transformExprAfterTransformedBv2IntDefault(
        expr: In,
        dependency: KExpr<A>,
        transformer: KContext.(KExpr<A>) -> Out
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency) { td ->
        return ctx.transformer(td)
            .distributeDependencies(listOf(td))
    }

    private inline fun <In : KExpr<T>, Out : KExpr<T>, T : KSort, A0 : KSort, A1 : KSort>
    transformExprAfterTransformedBv2IntDefault(
        expr: In,
        dependency0: KExpr<A0>,
        dependency1: KExpr<A1>,
        transformer: KContext.(KExpr<A0>, KExpr<A1>) -> Out
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1) { td0, td1 ->
        return ctx.transformer(td0, td1)
            .distributeDependencies(listOf(td0, td1))
    }

    private inline fun <In : KExpr<T>, Out : KExpr<T>, T : KSort, A0 : KSort, A1 : KSort, A2 : KSort>
    transformExprAfterTransformedBv2IntDefault(
        expr: In,
        dependency0: KExpr<A0>,
        dependency1: KExpr<A1>,
        dependency2: KExpr<A2>,
        transformer: KContext.(KExpr<A0>, KExpr<A1>, KExpr<A2>) -> Out
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { td0, td1, td2 ->
        return ctx.transformer(td0, td1, td2)
            .distributeDependencies(listOf(td0, td1, td2))
    }

    private inline fun <In : KExpr<T>, Out : KExpr<T>, T : KSort, A0 : KSort, A1 : KSort, A2 : KSort, A3 : KSort>
    transformExprAfterTransformedBv2IntDefault(
        expr: In,
        dependency0: KExpr<A0>,
        dependency1: KExpr<A1>,
        dependency2: KExpr<A2>,
        dependency3: KExpr<A3>,
        transformer: KContext.(KExpr<A0>, KExpr<A1>, KExpr<A2>, KExpr<A3>) -> Out
    ): KExpr<T> =
        transformExprAfterTransformed(expr, dependency0, dependency1, dependency2, dependency3) { td0, td1, td2, td3 ->
            return ctx.transformer(td0, td1, td2, td3)
                .distributeDependencies(listOf(td0, td1, td2, td3))
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

    private fun <T : KSort> KExpr<T>.tryAddBoundLemmas(sort: KSort, exprSignedness: Signedness = signedness) = with(ctx) {
        val expr = tryUnwrap()
        if (sort !is KBvSort || signednessMode == SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS) return expr

        val sizeBits = sort.sizeBits
        val (lowerBound, upperBound) = getBounds(sizeBits, exprSignedness)

        expr.addLemma(lowerBound le expr.uncheckedCast())
            .addLemma(upperBound ge expr.uncheckedCast())
    }

    private fun KContext.tryNormalizeExpr(
        expr: KExpr<KIntSort>,
        sizeBits: UInt,
        valueSignedness: Signedness,
        signedness: Signedness
    ): KExpr<KIntSort> =
        if (isLazyOverflow) {
            toSignedness(expr, sizeBits, valueSignedness, signedness)
        } else {
            normalizeExpr(expr, signedness, sizeBits)
        }

    private fun <T : KSort> KExpr<T>.tryUnwrap(): KExpr<T> =
        if (this is KBv2IntAuxExpr) {
            denormalized.uncheckedCast()
        } else {
            this
        }

    private inner class KBv2IntAuxExprLazySignedness(
        private val normalized: KConst<KIntSort>,
        sizeBits: UInt,
    ) : KBv2IntAuxExpr(normalized.ctx, sizeBits) {
        private var normalizedSignedness: Signedness? = null

        private inline fun accessWrapper(
            preferredSignedness: Signedness,
            body: (Signedness) -> KExpr<KIntSort>
        ) = with(ctx) {
            if (normalizedSignedness == null) {
                normalizedSignedness = preferredSignedness
                normalized.tryAddBoundLemmas(mkBvSort(sizeBits), preferredSignedness)
            }

            body(normalizedSignedness ?: error("Unexpected"))
        }

        override val denormalized: KExpr<KIntSort>
            get() = accessWrapper(signedness) { normalized }

        override val isNormalizedSigned: Boolean
            get() = if (normalizedSignedness == null) true else normalizedSignedness == Signedness.SIGNED
        override val isNormalizedUnsigned: Boolean
            get() = if (normalizedSignedness == null) true else normalizedSignedness == Signedness.UNSIGNED

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            accessWrapper(signedness) { normalizedSignedness ->
                ctx.toSignedness(normalized, sizeBits, normalizedSignedness, signedness)
            }
    }

    private class KBv2IntAuxExprConst(
        private val normalized: KExpr<KIntSort>,
        sizeBits: UInt,
        private val normalizedSignedness: Signedness
    ) : KBv2IntAuxExpr(normalized.ctx, sizeBits) {
        override val denormalized: KExpr<KIntSort> = normalized
        override val isNormalizedSigned: Boolean = true
        override val isNormalizedUnsigned: Boolean = true

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.toSignedness(normalized, sizeBits, normalizedSignedness, signedness)
    }

    private class KBv2IntAuxExprZeroExtension(
        private val normalized: KExpr<KIntSort>,
        sizeBits: UInt,
        private val extensionSize: UInt,
    ) : KBv2IntAuxExpr(normalized.ctx, sizeBits) {
        override val denormalized: KExpr<KIntSort> = normalized
        override val isNormalizedSigned: Boolean = true
        override val isNormalizedUnsigned: Boolean = true

        override fun normalized(signedness: Signedness): KExpr<KIntSort> = normalized

        override fun extractBit(bit: UInt): KExpr<KIntSort> = with(ctx) {
            if (bit >= sizeBits - extensionSize) {
                0.expr
            } else {
                super.extractBit(bit)
            }
        }


        override fun extractBits(high: UInt, low: UInt): KExpr<KIntSort> = with(ctx) {
            if (low >= sizeBits - extensionSize) {
                0.expr
            } else {
                super.extractBits(high, low)
            }
        }
    }

    private inner class KBv2IntAuxExprExtract(
        override val denormalized: KExpr<KIntSort>,
        private val originalExpr: KExpr<KIntSort>,
        val low: UInt,
        sizeBits: UInt,
        private val normalizedSignedness: Signedness,
    ) : KBv2IntAuxExpr(originalExpr.ctx, sizeBits) {
        override val isNormalizedSigned: Boolean = normalizedSignedness == Signedness.SIGNED
        override val isNormalizedUnsigned: Boolean = normalizedSignedness == Signedness.UNSIGNED

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.tryNormalizeExpr(denormalized, sizeBits, normalizedSignedness, signedness)

        override fun extractBit(bit: UInt): KExpr<KIntSort> =
            ctx.mkIntExtractBit(originalExpr, bit + low)

        override fun extractBits(high: UInt, low: UInt): KExpr<KIntSort> =
            ctx.mkIntExtractBits(originalExpr, high + this.low, low + this.low)
    }

    private class KBv2IntAuxExprShift(
        private val normalized: KExpr<KIntSort>,
        override val denormalized: KExpr<KIntSort>,
        private val originalExpr: KExpr<KIntSort>,
        val shift: Long,
        val defaultBit: KExpr<KIntSort> = with(denormalized.ctx) { 0.expr },
        sizeBits: UInt,
        private val normalizedSignedness: Signedness,
    ) : KBv2IntAuxExpr(denormalized.ctx, sizeBits) {
        override val isNormalizedSigned: Boolean = normalizedSignedness == Signedness.SIGNED
        override val isNormalizedUnsigned: Boolean = normalizedSignedness == Signedness.UNSIGNED

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.toSignedness(normalized, sizeBits, normalizedSignedness, signedness)

        override fun extractBit(bit: UInt): KExpr<KIntSort> = with(ctx) {
            if (bit.toLong() < shift || bit.toLong() - shift >= sizeBits.toLong()) {
                return defaultBit
            } else {
                mkIntExtractBit(originalExpr, (bit.toLong() - shift).toUInt())
            }
        }

        override fun extractBits(high: UInt, low: UInt): KExpr<KIntSort> = with(ctx) {
            if (high.toLong() < shift) return defaultBit

            val minBit = max(low.toLong() - shift, 0L).toUInt()
            val maxBit = min(high.toLong() - shift, sizeBits.toLong() - 1L).toUInt()
            val offset = max(shift - low.toLong(), 0L).toUInt()
            val originalBitsNum = max(maxBit.toLong() - minBit.toLong() + 1L, 0L).toUInt()
            val higherBitsNum = high - low + 1u - originalBitsNum
            val higherBits = mkIte(
                condition = defaultBit eq 1.expr,
                trueBranch = mkPowerOfTwoExpr(higherBitsNum) - 1.expr,
                falseBranch = 0.expr,
            ) * mkPowerOfTwoExpr(originalBitsNum)
            val originalBits = mkIntExtractBits(originalExpr, maxBit, minBit)

            (originalBits + higherBits) * mkPowerOfTwoExpr(offset)
        }
    }

    private class KBv2IntAuxExprNormalized(
        normalized: KExpr<KIntSort>,
        sizeBits: UInt,
        private val normalizedSignedness: Signedness,
    ) : KBv2IntAuxExpr(normalized.ctx, sizeBits) {
        override val denormalized: KExpr<KIntSort> = normalized

        override val isNormalizedSigned: Boolean = normalizedSignedness == Signedness.SIGNED
        override val isNormalizedUnsigned: Boolean = normalizedSignedness == Signedness.UNSIGNED

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.toSignedness(denormalized, sizeBits, normalizedSignedness, signedness)
    }

    private class KBv2IntAuxExprSingleOverflow(
        private val normalizedArgsValue: KExpr<KIntSort>,
        override val denormalized: KExpr<KIntSort>,
        sizeBits: UInt,
        private val normalizedSignedness: Signedness,
    ) : KBv2IntAuxExpr(denormalized.ctx, sizeBits) {
        override val isNormalizedSigned: Boolean = normalizedSignedness == Signedness.SIGNED
        override val isNormalizedUnsigned: Boolean = normalizedSignedness == Signedness.UNSIGNED

        private val normalized: KExpr<KIntSort> = with(ctx) {
            val (lowerBound, upperBound) = getBounds(sizeBits, normalizedSignedness)
            val step = mkPowerOfTwoExpr(sizeBits)

            mkIte(
                condition = normalizedArgsValue gt upperBound,
                trueBranch = normalizedArgsValue - step,
                falseBranch = mkIte(
                    condition = normalizedArgsValue lt lowerBound,
                    trueBranch = normalizedArgsValue + step,
                    falseBranch = normalizedArgsValue
                )
            )
        }

        inline fun normalizedOp(op: (KExpr<KIntSort>) -> KExpr<KBoolSort>): KExpr<KBoolSort> = with(ctx) {
            val (lowerBound, upperBound) = getBounds(sizeBits, normalizedSignedness)
            val step = mkPowerOfTwoExpr(sizeBits)

            return@with mkIte(
                condition = normalizedArgsValue gt upperBound,
                trueBranch = op(normalizedArgsValue - step),
                falseBranch = mkIte(
                    condition = normalizedArgsValue lt lowerBound,
                    trueBranch = op(normalizedArgsValue + step),
                    falseBranch = op(normalizedArgsValue)
                )
            )
        }

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.toSignedness(normalized, sizeBits, normalizedSignedness, signedness)
    }

    private inner class KBv2IntAuxExprDenormalized(
        override val denormalized: KExpr<KIntSort>,
        sizeBits: UInt
    ) : KBv2IntAuxExpr(denormalized.ctx, sizeBits) {
        override val isNormalizedSigned: Boolean = false
        override val isNormalizedUnsigned: Boolean = true

        override fun normalized(signedness: Signedness): KExpr<KIntSort> =
            ctx.normalizeExpr(denormalized, signedness, sizeBits)
    }

    abstract class KBv2IntAuxExpr(
        ctx: KContext,
        val sizeBits: UInt,
    ) : KExpr<KIntSort>(ctx) {
        abstract val denormalized: KExpr<KIntSort>

        abstract val isNormalizedSigned: Boolean
        abstract val isNormalizedUnsigned: Boolean

        abstract fun normalized(signedness: Signedness): KExpr<KIntSort>
        open fun extractBit(bit: UInt) = ctx.mkIntExtractBit(denormalized, bit)
        open fun extractBits(high: UInt, low: UInt) = ctx.mkIntExtractBits(denormalized, high, low)

        override val sort = ctx.intSort
        override fun print(printer: ExpressionPrinter) {
            printer.append("Bv2Int(")
            denormalized.print(printer)
            printer.append(")")
        }

        override fun internEquals(other: Any): Boolean = denormalized.internEquals(other)
        override fun internHashCode(): Int = denormalized.internHashCode()

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
            val overflowLemma = apply(body.getOverflowLemma())

            return constructor(newBody, bounds)
                .addLemma(lemma)
                .addBvAndLemma(bvAndLemma)
                .addOverflowLemma(overflowLemma)
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
