package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KBv2IntExpr
import io.ksmt.expr.KBvAddExpr
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvArithShiftRightExpr
import io.ksmt.expr.KBvConcatExpr
import io.ksmt.expr.KBvExtractExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvMulExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNegationExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvNotExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvShiftLeftExpr
import io.ksmt.expr.KBvSignExtensionExpr
import io.ksmt.expr.KBvSignedDivExpr
import io.ksmt.expr.KBvSignedGreaterExpr
import io.ksmt.expr.KBvSignedGreaterOrEqualExpr
import io.ksmt.expr.KBvSignedLessExpr
import io.ksmt.expr.KBvSignedLessOrEqualExpr
import io.ksmt.expr.KBvSignedModExpr
import io.ksmt.expr.KBvSignedRemExpr
import io.ksmt.expr.KBvSubExpr
import io.ksmt.expr.KBvUnsignedDivExpr
import io.ksmt.expr.KBvUnsignedGreaterExpr
import io.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import io.ksmt.expr.KBvUnsignedLessExpr
import io.ksmt.expr.KBvUnsignedLessOrEqualExpr
import io.ksmt.expr.KBvUnsignedRemExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KBvZeroExtensionExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort

class ArithFilter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private var hasArith = false
    private var hasShift = false

    fun <T : KSort> filter(expr: KExpr<T>): Boolean {
        apply(expr)
        return hasArith
    }


    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): KExpr<KBoolSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun transform(expr: KBvConcatExpr): KExpr<KBvSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun transform(expr: KBvExtractExpr): KExpr<KBvSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun transform(expr: KBvSignExtensionExpr): KExpr<KBvSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun transform(expr: KBvZeroExtensionExpr): KExpr<KBvSort> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): KExpr<T> {
        hasArith = true
        hasShift = true
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): KExpr<T> {
        hasArith = true
        return super.transform(expr)
    }

    override fun transform(expr: KBv2IntExpr): KExpr<KIntSort> {
        hasArith = true
        return super.transform(expr)
    }
}
