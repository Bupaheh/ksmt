package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvArithShiftRightExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvShiftLeftExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.transformer.KExprVisitResult
import io.ksmt.expr.transformer.KNonRecursiveVisitor
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort

class Bv2IntDefaultFilter(ctx: KContext) : KNonRecursiveVisitor<Boolean>(ctx) {
    override fun <T: KSort> defaultValue(expr: KExpr<T>): Boolean {
        return true
    }

    override fun mergeResults(left: Boolean, right: Boolean): Boolean = left && right

    private fun visitBitwiseOp(lhs: KExpr<*>, rhs: KExpr<*>) =
        lhs is KInterpretedValue || rhs is KInterpretedValue

    private fun visitShift(arg: KExpr<*>, shift: KExpr<*>) = shift is KInterpretedValue

    override fun <T: KBvSort> visit(expr: KBvAndExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvOrExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvXorExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvNAndExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvNorExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvXNorExpr<T>): KExprVisitResult<Boolean> {
        if (!visitBitwiseOp(expr.arg0, expr.arg1)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvShiftLeftExpr<T>): KExprVisitResult<Boolean> {
        if (!visitShift(expr.arg, expr.shift)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvLogicalShiftRightExpr<T>): KExprVisitResult<Boolean> {
        if (!visitShift(expr.arg, expr.shift)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }

    override fun <T: KBvSort> visit(expr: KBvArithShiftRightExpr<T>): KExprVisitResult<Boolean> {
        if (!visitShift(expr.arg, expr.shift)) return saveVisitResult(expr,false)
        return super.visit(expr)
    }
}