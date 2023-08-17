package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KBvUnsignedGreaterExpr
import io.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import io.ksmt.expr.KBvUnsignedLessExpr
import io.ksmt.expr.KBvUnsignedLessOrEqualExpr
import io.ksmt.expr.KBvZeroExtensionExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort

class KUnsignedToSignedBvRewriter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformed(expr, expr.arg0, expr.arg1, ::mkBvSignedLessExpr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformed(expr, expr.arg0, expr.arg1, ::mkBvSignedLessOrEqualExpr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformed(expr, expr.arg0, expr.arg1, ::mkBvSignedGreaterOrEqualExpr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> = with(ctx) {
        transformExprAfterTransformed(expr, expr.arg0, expr.arg1, ::mkBvSignedGreaterExpr)
    }

    override fun transform(expr: KBvZeroExtensionExpr): KExpr<KBvSort> = with(ctx) {
        transformExprAfterTransformed(expr, expr.value) { value -> mkBvSignExtensionExpr(expr.extensionSize, value) }
    }
}