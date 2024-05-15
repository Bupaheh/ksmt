package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
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
import io.ksmt.expr.rewrite.simplify.KExprSimplifier
import io.ksmt.expr.rewrite.simplify.KExprSimplifierBase
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBv1Sort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KIntSort

class KBv2IntSimplifier(ctx : KContext) : KExprSimplifier(ctx) {
    override fun <T : KBvSort> KContext.preprocess(expr: KBvShiftLeftExpr<T>): KExpr<T> {
        val arg = expr.arg
        val shift = expr.shift

        return when (arg) {
            is KBvAddExpr -> mkBvAddExpr(mkBvShiftLeftExpr(arg.arg0, shift), mkBvShiftLeftExpr(arg.arg1, shift))
            is KBvSubExpr -> mkBvSubExpr(mkBvShiftLeftExpr(arg.arg0, shift), mkBvShiftLeftExpr(arg.arg1, shift))
            is KBvShiftLeftExpr -> mkBvShiftLeftExpr(arg.arg, mkBvAddExpr(arg.shift, shift))
            is KBvOrExpr -> mkBvOrExpr(mkBvShiftLeftExpr(arg.arg0, shift), mkBvShiftLeftExpr(arg.arg1, shift))
            else -> expr
        }
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedLessOrEqualExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedLessOrEqualExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvUnsignedLessOrEqualExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedLessOrEqualExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedLessOrEqualExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvSignedLessOrEqualExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedGreaterOrEqualExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvUnsignedGreaterOrEqualExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedLessExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedLessExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<KBoolSort> =
        mkBvUnsignedLessExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedGreaterExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedGreaterExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvUnsignedGreaterExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedGreaterOrEqualExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedGreaterOrEqualExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvSignedLessOrEqualExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedLessExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedLessExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<KBoolSort> =
        mkBvSignedLessExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedGreaterExpr<T>): KExpr<KBoolSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedGreaterExpr(
        lhs: KExpr<T>,
        rhs: KExpr<T>
    ): KExpr<KBoolSort> = mkBvSignedGreaterExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvAddExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvAddExpr(args: List<KExpr<T>>): KExpr<T> {
        require(args.size == 2)

        return mkBvAddExpr(args[0], args[1])
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSubExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSubExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvSubExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvMulExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvMulExpr(args: List<KExpr<T>>): KExpr<T> {
        require(args.size == 2)

        return mkBvMulExpr(args[0], args[1])
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvNegationExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvNegationExpr(arg: KExpr<T>): KExpr<T> = mkBvNegationExpr(arg)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedDivExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedDivExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvSignedDivExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedDivExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedDivExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvUnsignedDivExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedRemExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedRemExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvSignedRemExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvUnsignedRemExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvUnsignedRemExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvUnsignedRemExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvSignedModExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignedModExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvSignedModExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvNotExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvNotExpr(arg: KExpr<T>): KExpr<T> = mkBvNotExpr(arg)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvOrExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvOrExpr(args: List<KExpr<T>>): KExpr<T> {
        require(args.size == 2)

        return mkBvOrExpr(args[0], args[1])
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvXorExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvXorExpr(args: List<KExpr<T>>): KExpr<T> {
        require(args.size == 2)

        return mkBvXorExpr(args[0], args[1])
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvAndExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvAndExpr(args: List<KExpr<T>>): KExpr<T> {
        require(args.size == 2)

        return mkBvAndExpr(args[0], args[1])
    }

    override fun <T : KBvSort> KContext.preprocess(expr: KBvNAndExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvNAndExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvNAndExpr(lhs, rhs)
    override fun <T : KBvSort> KContext.preprocess(expr: KBvNorExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvNorExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvNorExpr(lhs, rhs)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvXNorExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvXNorExpr(lhs: KExpr<T>, rhs: KExpr<T>): KExpr<T> =
        mkBvXNorExpr(lhs, rhs)

    override fun KContext.preprocess(expr: KBvConcatExpr): KExpr<KBvSort> = expr

    override fun KContext.postRewriteBvConcatExpr(args: List<KExpr<KBvSort>>): KExpr<KBvSort> {
        require(args.size == 2)

        return mkBvConcatExpr(args[0], args[1])
    }

    override fun KContext.preprocess(expr: KBvExtractExpr): KExpr<KBvSort> = expr

    override fun KContext.postRewriteBvExtractExpr(high: Int, low: Int, value: KExpr<KBvSort>): KExpr<KBvSort> =
        mkBvExtractExpr(high, low, value)

    override fun <T : KBvSort> KContext.postRewriteBvShiftLeftExpr(arg: KExpr<T>, shift: KExpr<T>): KExpr<T> =
        mkBvShiftLeftExpr(arg, shift)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvLogicalShiftRightExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvLogicalShiftRightExpr(arg: KExpr<T>, shift: KExpr<T>): KExpr<T> =
        mkBvLogicalShiftRightExpr(arg, shift)

    override fun <T : KBvSort> KContext.preprocess(expr: KBvArithShiftRightExpr<T>): KExpr<T> = expr

    override fun <T : KBvSort> KContext.postRewriteBvArithShiftRightExpr(arg: KExpr<T>, shift: KExpr<T>): KExpr<T> =
        mkBvArithShiftRightExpr(arg, shift)

    override fun KContext.preprocess(expr: KBvZeroExtensionExpr): KExpr<KBvSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvZeroExtensionExpr(
        extensionSize: Int,
        value: KExpr<T>
    ): KExpr<KBvSort> = mkBvZeroExtensionExpr(extensionSize, value)

    override fun KContext.preprocess(expr: KBvSignExtensionExpr): KExpr<KBvSort> = expr

    override fun <T : KBvSort> KContext.postRewriteBvSignExtensionExpr(
        extensionSize: Int,
        value: KExpr<T>
    ): KExpr<KBvSort> = mkBvSignExtensionExpr(extensionSize, value)
}
