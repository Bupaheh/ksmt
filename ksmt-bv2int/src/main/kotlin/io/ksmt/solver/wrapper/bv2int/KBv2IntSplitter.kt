package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KArray2Select
import io.ksmt.expr.KArray2Store
import io.ksmt.expr.KArray3Select
import io.ksmt.expr.KArray3Store
import io.ksmt.expr.KArrayNSelect
import io.ksmt.expr.KArrayNStore
import io.ksmt.expr.KArraySelect
import io.ksmt.expr.KArrayStore
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KExistentialQuantifier
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.KUniversalQuantifier
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort


/**
 * Сейчас массивы либо полностью переписываются, либо вообще не переписываются.
 * Можно переписывать отдельные аргументы
 */
class KBv2IntSplitter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    val dsu = DisjointSetUnion()

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> = expr.apply {
        val bvArgs = expr.args.filter {  expr ->
            val sort = expr.sort
            expr !is KInterpretedValue &&
                    (sort is KBvSort || sort is KArraySortBase<*> && (sort.range is KBvSort || sort.domainSorts.any { it is KBvSort }))
        }

        if (bvArgs.isEmpty()) return@apply

        val first = bvArgs.first()

        bvArgs.forEach { dsu.union(first, it) }
        dsu.union(expr, first)
    }

    private fun handleBitwiseExpr(expr: KExpr<*>) {
        dsu.mark(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr)
        return super.transform(expr)
    }

    override fun transform(expr: KExistentialQuantifier): KExpr<KBoolSort> {
        TODO()
    }

    override fun transform(expr: KUniversalQuantifier): KExpr<KBoolSort> {
        TODO()
    }
}