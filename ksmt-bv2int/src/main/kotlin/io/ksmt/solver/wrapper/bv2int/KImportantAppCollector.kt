package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KAndBinaryExpr
import io.ksmt.expr.KAndExpr
import io.ksmt.expr.KApp
import io.ksmt.expr.KArray2Lambda
import io.ksmt.expr.KArray3Lambda
import io.ksmt.expr.KArrayLambda
import io.ksmt.expr.KArrayNLambda
import io.ksmt.expr.KExistentialQuantifier
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KFunctionAsArray
import io.ksmt.expr.KImpliesExpr
import io.ksmt.expr.KIteExpr
import io.ksmt.expr.KOrBinaryExpr
import io.ksmt.expr.KOrExpr
import io.ksmt.expr.KUniversalQuantifier
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.solver.KModel
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KSort

class KImportantAppCollector private constructor(
    ctx: KContext,
    private val model: KModel,
    private val bv2intContext: KBv2IntContext
) : KNonRecursiveTransformer(ctx) {
    private val importantApps = mutableSetOf<KExpr<*>>()
    private val importantExprs = mutableSetOf<KExpr<*>>()

    override fun <T : KSort> exprTransformationRequired(expr: KExpr<T>): Boolean {
        return expr in importantExprs
    }

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        expr.args.forEach { importantExprs.add(it) }
        return visitImportantDependencies(expr, expr.args)
    }

    override fun <T : KSort> transform(expr: KFunctionApp<T>): KExpr<T> {
        if (expr.decl == bv2intContext.bvAndFunc) importantApps.add(expr)
        expr.args.forEach { importantExprs.add(it) }

        return visitImportantDependencies(expr, expr.args)
    }

    private fun <A : KSort> visitAndOr(
        expr: KExpr<KBoolSort>,
        args: List<KExpr<A>>,
        zeroElement: KExpr<KBoolSort>
    ): KExpr<KBoolSort> {
        val arg = args.firstOrNull { model.eval(it) == zeroElement }
        if (arg != null) {
            importantExprs.add(arg)
        } else {
            args.forEach { importantExprs.add(it) }
        }

        return visitImportantDependencies(expr, args)
    }

    override fun transform(expr: KAndBinaryExpr): KExpr<KBoolSort> = with(ctx) {
        visitAndOr(expr, expr.args, falseExpr)
    }

    override fun transform(expr: KAndExpr): KExpr<KBoolSort> = with(ctx) {
        visitAndOr(expr, expr.args, falseExpr)
    }

    override fun transform(expr: KOrExpr): KExpr<KBoolSort> = with(ctx) {
        visitAndOr(expr, expr.args, trueExpr)
    }

    override fun transform(expr: KOrBinaryExpr): KExpr<KBoolSort> = with(ctx) {
        visitAndOr(expr, expr.args, trueExpr)
    }

    override fun transform(expr: KImpliesExpr): KExpr<KBoolSort> = with(ctx) {
        when {
            model.eval(expr.p) == falseExpr -> importantExprs.add(expr.p)
            model.eval(expr.q) == trueExpr -> importantExprs.add(expr.q)
            else -> expr.args.forEach { importantExprs.add(it) }
        }

        visitImportantDependencies(expr, expr.p, expr.q)
    }

    override fun <T : KSort> transform(expr: KIteExpr<T>): KExpr<T> = with(ctx) {
        importantExprs.add(expr.condition)
        when {
            model.eval(expr.condition) == falseExpr -> importantExprs.add(expr.falseBranch)
            model.eval(expr.condition) == trueExpr -> importantExprs.add(expr.trueBranch)
            else -> expr.args.drop(1).forEach { importantExprs.add(it) }
        }
        visitImportantDependencies(expr, expr.condition, expr.trueBranch, expr.falseBranch)
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayLambda<D, R>): KExpr<KArraySort<D, R>> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(expr: KArray2Lambda<D0, D1, R>): KExpr<KArray2Sort<D0, D1, R>> {
        TODO()
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(expr: KArray3Lambda<D0, D1, D2, R>): KExpr<KArray3Sort<D0, D1, D2, R>> {
        TODO()
    }

    override fun <R : KSort> transform(expr: KArrayNLambda<R>): KExpr<KArrayNSort<R>> {
        TODO()
    }

    override fun transform(expr: KExistentialQuantifier): KExpr<KBoolSort> {
        TODO()
    }

    override fun transform(expr: KUniversalQuantifier): KExpr<KBoolSort> {
        TODO()
    }

    override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KFunctionAsArray<A, R>): KExpr<A> {
        TODO()
    }

    private fun <T : KSort> visitImportantDependencies(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1) { _, _ ->
        expr
    }

    private fun <T : KSort> visitImportantDependencies(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { _, _, _->
        expr
    }

    private fun <T : KSort, A : KSort> visitImportantDependencies(
        expr: KExpr<T>,
        dependencies: List<KExpr<A>>
    ): KExpr<T> = transformExprAfterTransformed(expr, dependencies) { _ ->
        expr
    }

    companion object {
        fun collectImportantApps(
            exprs: List<KExpr<KBoolSort>>,
            model: KModel,
            bv2intContext: KBv2IntContext
        ): Set<KExpr<*>> {
            if (exprs.isEmpty()) return emptySet()

            val collector = KImportantAppCollector(exprs.first().ctx, model, bv2intContext)

            exprs.forEach { expr ->
                collector.importantExprs.add(expr)
                collector.apply(expr)
            }
            return collector.importantApps
        }
    }
}