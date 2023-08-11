package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KAddArithExpr
import io.ksmt.expr.KBv2IntExpr
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
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.ArithUtils.bigIntegerValue
import io.ksmt.utils.uncheckedCast
import java.math.BigInteger

class KBv2IntOverflowChecker private constructor(
    ctx: KContext,
    private val model: KModel,
    private val rewriter: KBv2IntRewriter
) : KNonRecursiveTransformer(ctx) {

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

    private fun transformIfOverflow(expr: KExpr<KIntSort>, sizeBits: UInt): KExpr<KIntSort> = with(ctx) {
        val value = model.eval(expr, false)

        if (value !is KIntNumExpr) {
            rewriter.setOverflowSizeBits(expr, sizeBits)
            return expr
        }

        val integerValue = value.bigIntegerValue
        val pow = BigInteger.TWO.pow(sizeBits.toInt() - 1)
        val lowerBound = -pow
        val upperBound = pow - BigInteger.ONE

        if (integerValue in lowerBound..upperBound) {
            rewriter.setOverflowSizeBits(expr, sizeBits)
            return expr
        }

        unsignedToSigned(expr mod mkPowerOfTwoExpr(sizeBits), sizeBits)
    }

    private inline fun <T : KSort, B : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependency: KExpr<B>,
        transformer: (KExpr<B>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency) { arg ->
        val transformed = transformer(arg)
        val sizeBits = rewriter.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
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
        val sizeBits = rewriter.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
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
        val sizeBits = rewriter.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    private inline fun <T : KSort, B : KSort> transformExprAfterTransformedOverflow(
        expr: KExpr<T>,
        dependencies: List<KExpr<B>>,
        transformer: (List<KExpr<B>>) -> KExpr<T>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { args ->
        val transformed = transformer(args)
        val sizeBits = rewriter.getOverflowSizeBits(expr.uncheckedCast()) ?: return transformed
        require(transformed.sort is KIntSort)
        transformIfOverflow(transformed.uncheckedCast(), sizeBits).uncheckedCast()
    }

    companion object {
        fun <T : KSort> overflowCheck(
            expr: KExpr<T>,
            model: KModel,
            rewriter: KBv2IntRewriter
        ): KExpr<T> = KBv2IntOverflowChecker(expr.ctx, model, rewriter).apply(expr)
    }
}