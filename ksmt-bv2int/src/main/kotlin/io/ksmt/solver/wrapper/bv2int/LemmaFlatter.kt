package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KAndBinaryExpr
import io.ksmt.expr.KAndExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KSort

class LemmaFlatter private constructor(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private val lemmas: MutableList<KExpr<KBoolSort>> = mutableListOf()

    override fun <T : KSort> exprTransformationRequired(expr: KExpr<T>): Boolean {
        return expr is KAndExpr
    }

    override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> =
        transformExprAfterTransformed(expr, expr.lhs, expr.rhs) { l, r ->
            processDependency(l)
            processDependency(r)

            expr
        }

    override fun transform(expr: KAndExpr): KExpr<KBoolSort> =
        transformExprAfterTransformed(expr, expr.args) { args ->
            args.forEach { processDependency(it) }

            expr
        }

    private fun processDependency(expr: KExpr<KBoolSort>) {
        if (expr is KAndExpr || expr == ctx.trueExpr) return
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
