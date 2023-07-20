package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KDistinctExpr
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExpr
import io.ksmt.sort.KArraySortBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.test.RandomExpressionGenerator.Companion.AstFilter

class Bv2IntAstFilter : AstFilter() {
    override fun filterSort(sort: KSort): Boolean {
        val sorts = if (sort is KArraySortBase<*>) {
            sort.domainSorts + sort.range
        } else {
            listOf(sort)
        }

        return sorts.all { it is KBvSort || it is KBoolSort }
    }

    override fun filterExpr(expr: KExpr<*>): Boolean {
        if (expr is KArrayConst<*, *> && expr.value.sort != expr.sort.range) {
            println("oops")
            return false
        }
        if (expr is KEqExpr<*> && expr.lhs.sort is KArraySortBase<*>) return false
        if (expr is KDistinctExpr<*> && expr.args.first().sort is KArraySortBase<*>) return false

        return true
    }
}
