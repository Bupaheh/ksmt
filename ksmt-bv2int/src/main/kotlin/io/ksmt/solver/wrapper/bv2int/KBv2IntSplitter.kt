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
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort

/**
 * TODO() из-за упрощения при apply мы можем получать уже другое выражение, надо переписать на визиторах
 * вроде неправда, т.к. super.transform
 */
class KBv2IntSplitter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    val dsu = DisjointSetUnion()

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> = expr.apply {
        val bvArgs = expr.args.filter { it.sort is KBvSort && it !is KInterpretedValue }

        if (bvArgs.isEmpty()) return@apply

        val first = bvArgs.first()

        bvArgs.forEach { dsu.union(first, it) }

        if (expr.sort is KBvSort) {
            dsu.union(expr, first)
        }
    }

    private fun handleBitwiseExpr(lhs: KExpr<*>, rhs: KExpr<*>) {
        dsu.mark(listOf(lhs, rhs).first { it !is KInterpretedValue })
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> {
        handleBitwiseExpr(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayStore<D, R>): KExpr<KArraySort<D, R>> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(expr: KArray2Store<D0, D1, R>): KExpr<KArray2Sort<D0, D1, R>> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(expr: KArray3Store<D0, D1, D2, R>): KExpr<KArray3Sort<D0, D1, D2, R>> {
        TODO()
    }

    override fun <R : KSort> transform(expr: KArrayNStore<R>): KExpr<KArrayNSort<R>> {
        TODO()
    }

    override fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): KExpr<R> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(expr: KArray2Select<D0, D1, R>): KExpr<R> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(expr: KArray3Select<D0, D1, D2, R>): KExpr<R> {
        TODO()
    }

    override fun <R : KSort> transform(expr: KArrayNSelect<R>): KExpr<R> {
        TODO()
    }

    override fun <T : KSort> transform(expr: KFunctionApp<T>): KExpr<T> {
        TODO()
    }

    override fun transform(expr: KExistentialQuantifier): KExpr<KBoolSort> {
        TODO()
    }

    override fun transform(expr: KUniversalQuantifier): KExpr<KBoolSort> {
        TODO()
    }
}