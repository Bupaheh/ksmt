package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KArray2Store
import io.ksmt.expr.KArray3Store
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KArrayNStore
import io.ksmt.expr.KArrayStore
import io.ksmt.expr.KExpr
import io.ksmt.expr.KIntNumExpr
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KArray2Sort
import io.ksmt.sort.KArray3Sort
import io.ksmt.sort.KArrayNSort
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.ArithUtils.bigIntegerValue
import io.ksmt.utils.uncheckedCast

class KBv2IntConverter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private val expectedSort = hashMapOf<KExpr<*>, KSort>()

    fun <T : KSort> convertExpr(expr: KExpr<*>, sort: T): KExpr<T> {
        expectedSort[expr] = sort

        val converted = apply(expr)

        if (converted.sort != sort) error("Conversion failed")

        return converted.uncheckedCast()
    }

    override fun <T : KSort> exprTransformationRequired(expr: KExpr<T>): Boolean =
        expectedSort.containsKey(expr) && expectedSort[expr] != expr.sort

    override fun transformIntNum(expr: KIntNumExpr): KExpr<KIntSort> = with(ctx) {
        val sort = expectedSort[expr] ?: error("Unexpected ...")
        require(sort is KBvSort)

        mkBv(expr.bigIntegerValue, sort.sizeBits).uncheckedCast()
    }

    override fun <A : KArraySortBase<R>, R : KSort> transform(expr: KArrayConst<A, R>): KExpr<A> = with(ctx) {
        transformExprAfterTransformedWithSorts(
            expr = expr,
            dependency = expr.value,
            dependencySort = { it.range }
        ) { mkArrayConst(expectedSort[expr]!!.uncheckedCast(), it) }
    }

    override fun <D : KSort, R : KSort> transform(
        expr: KArrayStore<D, R>
    ): KExpr<KArraySort<D, R>> = with(ctx) {
        transformExprAfterTransformedWithSorts(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index,
            dependency2 = expr.value,
            dependency0Sort = { it },
            dependency1Sort = { it.domain },
            dependency2Sort = { it.range },
            transformer = ::mkArrayStore
        )
    }

    override fun <D0 : KSort, D1 : KSort, R : KSort> transform(
        expr: KArray2Store<D0, D1, R>
    ): KExpr<KArray2Sort<D0, D1, R>> = with(ctx) {
        transformExprAfterTransformedWithSorts(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index0,
            dependency2 = expr.index1,
            dependency3 = expr.value,
            dependency0Sort = { it },
            dependency1Sort = { it.domain0 },
            dependency2Sort = { it.domain1 },
            dependency3Sort = { it.range },
            transformer = ::mkArrayStore
        )
    }

    override fun <D0 : KSort, D1 : KSort, D2 : KSort, R : KSort> transform(
        expr: KArray3Store<D0, D1, D2, R>
    ): KExpr<KArray3Sort<D0, D1, D2, R>> = with(ctx) {
        transformExprAfterTransformedWithSorts(
            expr = expr,
            dependency0 = expr.array,
            dependency1 = expr.index0,
            dependency2 = expr.index1,
            dependency3 = expr.index2,
            dependency4 = expr.value,
            dependency0Sort = { it },
            dependency1Sort = { it.domain0 },
            dependency2Sort = { it.domain1 },
            dependency3Sort = { it.domain2 },
            dependency4Sort = { it.range },
            transformer = ::mkArrayStore
        )
    }

    override fun <R : KSort> transform(
        expr: KArrayNStore<R>
    ): KExpr<KArrayNSort<R>> = transformExprAfterTransformedWithSorts(
        expr = expr,
        dependencies = expr.args,
        dependencySort = { sort, idx ->
            when (idx) {
                0 -> sort
                expr.args.size - 1 -> sort.range
                else -> sort.domainSorts[idx - 1]
            }
        }
    ) { args ->
        val array = args.first()
        val indices = args.subList(fromIndex = 1, toIndex = args.size - 1)
        val value = args.last()

        ctx.mkArrayNStore(array.uncheckedCast(), indices, value)
    }

    private inline fun <T : KSort, A : KSort> transformExprAfterTransformedWithSorts(
        expr: KExpr<T>,
        dependency: KExpr<*>,
        dependencySort: (T) -> A,
        transformer: (KExpr<A>) -> KExpr<*>
    ): KExpr<T> {
        val sort = expectedSort[expr] ?: error("Unexpected ...")

        expectedSort[dependency] = dependencySort(sort.uncheckedCast())

        return transformExprAfterTransformed(expr, dependency) { arg ->
            transformer(arg.uncheckedCast())
                .also { require(it.sort == sort) }
                .uncheckedCast()
        }
    }

    @Suppress("LongParameterList")
    private inline fun <
        T : KSort,
        A0 : KSort,
        A1 : KSort,
        A2 : KSort
    > transformExprAfterTransformedWithSorts(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        dependency0Sort: (T) -> A0,
        dependency1Sort: (T) -> A1,
        dependency2Sort: (T) -> A2,
        transformer: (KExpr<A0>, KExpr<A1>, KExpr<A2>) -> KExpr<*>
    ): KExpr<T> {
        val sort = expectedSort[expr] ?: error("Unexpected ...")

        expectedSort[dependency0] = dependency0Sort(sort.uncheckedCast())
        expectedSort[dependency1] = dependency1Sort(sort.uncheckedCast())
        expectedSort[dependency2] = dependency2Sort(sort.uncheckedCast())

        return transformExprAfterTransformed(expr, dependency0, dependency1, dependency2) { arg0, arg1, arg2 ->
            transformer(arg0.uncheckedCast(), arg1.uncheckedCast(), arg2.uncheckedCast())
                .also { require(it.sort == sort) }
                .uncheckedCast()
        }
    }

    @Suppress("LongParameterList")
    private inline fun <
        T : KSort,
        A0 : KSort,
        A1 : KSort,
        A2 : KSort,
        A3 : KSort
    > transformExprAfterTransformedWithSorts(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        dependency3: KExpr<*>,
        dependency0Sort: (T) -> A0,
        dependency1Sort: (T) -> A1,
        dependency2Sort: (T) -> A2,
        dependency3Sort: (T) -> A3,
        transformer: (KExpr<A0>, KExpr<A1>, KExpr<A2>, KExpr<A3>) -> KExpr<*>
    ): KExpr<T> {
        val sort = expectedSort[expr] ?: error("Unexpected ...")

        expectedSort[dependency0] = dependency0Sort(sort.uncheckedCast())
        expectedSort[dependency1] = dependency1Sort(sort.uncheckedCast())
        expectedSort[dependency2] = dependency2Sort(sort.uncheckedCast())
        expectedSort[dependency3] = dependency3Sort(sort.uncheckedCast())

        return transformExprAfterTransformed(
            expr,
            dependency0,
            dependency1,
            dependency2,
            dependency3
        ) { arg0, arg1, arg2, arg3 ->
            transformer(arg0.uncheckedCast(), arg1.uncheckedCast(), arg2.uncheckedCast(), arg3.uncheckedCast())
                .also { require(it.sort == sort) }
                .uncheckedCast()
        }
    }

    @Suppress("LongParameterList")
    private inline fun <
        T : KSort,
        A0 : KSort,
        A1 : KSort,
        A2 : KSort,
        A3 : KSort,
        A4 : KSort
    > transformExprAfterTransformedWithSorts(
        expr: KExpr<T>,
        dependency0: KExpr<*>,
        dependency1: KExpr<*>,
        dependency2: KExpr<*>,
        dependency3: KExpr<*>,
        dependency4: KExpr<*>,
        dependency0Sort: (T) -> A0,
        dependency1Sort: (T) -> A1,
        dependency2Sort: (T) -> A2,
        dependency3Sort: (T) -> A3,
        dependency4Sort: (T) -> A4,
        transformer: (KExpr<A0>, KExpr<A1>, KExpr<A2>, KExpr<A3>, KExpr<A4>) -> KExpr<*>
    ): KExpr<T> {
        val sort = expectedSort[expr] ?: error("Unexpected ...")

        expectedSort[dependency0] = dependency0Sort(sort.uncheckedCast())
        expectedSort[dependency1] = dependency1Sort(sort.uncheckedCast())
        expectedSort[dependency2] = dependency2Sort(sort.uncheckedCast())
        expectedSort[dependency3] = dependency3Sort(sort.uncheckedCast())
        expectedSort[dependency4] = dependency4Sort(sort.uncheckedCast())

        return transformExprAfterTransformed(
            expr,
            dependency0,
            dependency1,
            dependency2,
            dependency3,
            dependency4
        ) { arg0, arg1, arg2, arg3, arg4 ->
            transformer(
                arg0.uncheckedCast(),
                arg1.uncheckedCast(),
                arg2.uncheckedCast(),
                arg3.uncheckedCast(),
                arg4.uncheckedCast()
            ).also { require(it.sort == sort) }.uncheckedCast()
        }
    }

    private inline fun <T : KSort> transformExprAfterTransformedWithSorts(
        expr: KExpr<T>,
        dependencies: List<KExpr<KSort>>,
        dependencySort: (T, Int) -> KSort,
        transformer: (List<KExpr<KSort>>) -> KExpr<*>
    ): KExpr<T> {
        val sort = expectedSort[expr] ?: error("Unexpected ...")

        dependencies.forEachIndexed { idx, d ->
            expectedSort[d] = dependencySort(sort.uncheckedCast(), idx)
        }

        return transformExprAfterTransformed(expr, dependencies) { args ->
            transformer(args).also { require(it.sort == sort) }.uncheckedCast()
        }
    }
}
