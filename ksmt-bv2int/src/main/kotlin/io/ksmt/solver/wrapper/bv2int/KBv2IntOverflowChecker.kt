package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KAddArithExpr
import io.ksmt.expr.KArray2Select
import io.ksmt.expr.KArray3Select
import io.ksmt.expr.KArrayNSelect
import io.ksmt.expr.KArraySelect
import io.ksmt.expr.KArraySelectBase
import io.ksmt.expr.KBv2IntExpr
import io.ksmt.expr.KConst
import io.ksmt.expr.KDivArithExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KIntNumExpr
import io.ksmt.expr.KIteExpr
import io.ksmt.expr.KModIntExpr
import io.ksmt.expr.KMulArithExpr
import io.ksmt.expr.KPowerArithExpr
import io.ksmt.expr.KRemIntExpr
import io.ksmt.expr.KSubArithExpr
import io.ksmt.expr.KUnaryMinusArithExpr
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.solver.KModel
import io.ksmt.sort.KArithSort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.ArithUtils.bigIntegerValue
import io.ksmt.utils.uncheckedCast
import java.math.BigInteger

class KBv2IntOverflowChecker private constructor(
    ctx: KContext,
    private val model: KModel,
    private val bv2IntContext: KBv2IntContext
) : KNonRecursiveTransformer(ctx) {
    private var flag = false
    private val boundLemmas = mutableListOf<KExpr<KBoolSort>>()

    override fun <T : KSort> transform(expr: KConst<T>): KExpr<T> =
        transformExprAfterTransformedOverflow(expr) { expr }

    override fun <T : KSort> transform(expr: KIteExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.condition, expr.trueBranch, expr.falseBranch, ::mkIte)
    }

    override fun transform(expr: KBv2IntExpr): KExpr<KIntSort> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.value) { arg ->
            mkBv2IntExpr(arg, expr.isSigned)
        }
    }

    override fun <T : KArithSort> transform(expr: KAddArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.args, ::mkArithAdd)
    }

    override fun <T : KArithSort> transform(expr: KMulArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.args, ::mkArithMul)
    }

    override fun <T : KArithSort> transform(expr: KSubArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.args, ::mkArithSub)
    }

    override fun <T : KArithSort> transform(expr: KUnaryMinusArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.arg, ::mkArithUnaryMinus)
    }

    override fun <T : KArithSort> transform(expr: KDivArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.lhs, expr.rhs, ::mkArithDiv)
    }

    override fun <T : KArithSort> transform(expr: KPowerArithExpr<T>): KExpr<T> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.lhs, expr.rhs, ::mkArithPower)
    }

    override fun transform(expr: KModIntExpr): KExpr<KIntSort> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.lhs, expr.rhs, ::mkIntMod)
    }

    override fun transform(expr: KRemIntExpr): KExpr<KIntSort> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.lhs, expr.rhs, ::mkIntRem)
    }

    override fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): KExpr<R> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.array, expr.index, ::mkArraySelect)
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(expr: KArray2Select<D0, D1, R>): KExpr<R> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.array, expr.index0, expr.index1, ::mkArraySelect)
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Select<D0, D1, D2, R>
    ): KExpr<R> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.array, expr.index0, expr.index1, expr.index2, ::mkArraySelect)
    }

    override fun <R : KSort> transform(expr: KArrayNSelect<R>): KExpr<R> = with(ctx) {
        transformExprAfterTransformedOverflow(expr, expr.args) { args ->
            val array: KExpr<KArrayNSort<KSort>> = args.first().uncheckedCast()
            val indices = args.subList(fromIndex = 1, toIndex = args.size)

            mkArrayNSelect(array, indices).uncheckedCast()
        }
    }

    private fun transformIfOverflow(expr: KExpr<KIntSort>, sizeBits: UInt): KExpr<KIntSort> = with(ctx) {
        val value = model.eval(expr, false)
        val pow = BigInteger.TWO.pow(sizeBits.toInt() - 1)
        val lowerBound = -pow
        val upperBound = pow - BigInteger.ONE

        if (value is KIntNumExpr) {
            val integerValue = value.bigIntegerValue

            if (integerValue in lowerBound..upperBound) {
                bv2IntContext.setOverflowSizeBits(expr, sizeBits)
                return expr
            }
        }

        flag = true

        if (expr is KConst<*> || expr is KFunctionApp<*> || expr is KArraySelectBase<*, *>) {
            boundLemmas.add(lowerBound.expr le expr)
            boundLemmas.add(upperBound.expr ge expr)

            expr
        } else {
            unsignedToSigned(expr mod mkPowerOfTwoExpr(sizeBits), sizeBits)
        }
    }

    private inline fun <T : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        transformer: () -> KExpr<T>
    ): KExpr<T> {
        val transformed = transformer()
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        return transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependency: KExpr<B>,
        transformer: (KExpr<B>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency) { arg ->
        val transformed = transformer(arg)
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B0 : KSort, B1 : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependency0: KExpr<B0>,
        dependency1: KExpr<B1>,
        transformer: (KExpr<B0>, KExpr<B1>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1) { arg0, arg1 ->
        val transformed = transformer(arg0, arg1)
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B0 : KSort, B1 : KSort, B2 : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependency0: KExpr<B0>,
        dependency1: KExpr<B1>,
        dependency2: KExpr<B2>,
        transformer: (KExpr<B0>, KExpr<B1>, KExpr<B2>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { arg0, arg1, arg2 ->
        val transformed = transformer(arg0, arg1, arg2)
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B0 : KSort, B1 : KSort, B2 : KSort, B3 : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependency0: KExpr<B0>,
        dependency1: KExpr<B1>,
        dependency2: KExpr<B2>,
        dependency3: KExpr<B3>,
        transformer: (KExpr<B0>, KExpr<B1>, KExpr<B2>, KExpr<B3>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(
        expr,
        dependency0,
        dependency1,
        dependency2,
        dependency3
    ) { arg0, arg1, arg2, arg3 ->
        val transformed = transformer(arg0, arg1, arg2, arg3)
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependencies: List<KExpr<B>>,
        transformer: (List<KExpr<B>>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { args ->
        val transformed = transformer(args)
        val sizeBits = bv2IntContext.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    companion object {
        fun overflowCheck(
            expr: KExpr<KBoolSort>,
            model: KModel,
            bv2IntContext: KBv2IntContext
        ): KExpr<KBoolSort>? = with(expr.ctx) {
            val checker = KBv2IntOverflowChecker(expr.ctx, model, bv2IntContext)
            val transformed = checker.apply(expr)

            return if (checker.flag) {
                transformed and mkAnd(checker.boundLemmas)
            } else {
                null
            }
        }
    }
}