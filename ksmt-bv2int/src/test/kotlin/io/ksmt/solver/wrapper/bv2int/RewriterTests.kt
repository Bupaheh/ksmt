package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KConst
import io.ksmt.expr.KExpr
import io.ksmt.expr.KIntNumExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.utils.ArithUtils.bigIntegerValue
import io.ksmt.utils.getValue
import io.ksmt.utils.uncheckedCast
import java.math.BigInteger
import kotlin.test.Test

class RewriterTests {

    private fun KExpr<*>.tryUnwrap(): KExpr<KSort> =
        if (this is KBv2IntRewriter.KBv2IntAuxExpr) {
            normalized
        } else {
            this
        }.uncheckedCast()

    private fun <T : KBvSort> KBv2IntRewriter.extractInterpretation(model: KModel, variable: KExpr<T>): BigInteger {
        require(variable is KConst<*>)

        return (model.eval(transform(variable).tryUnwrap(), isComplete = true) as KIntNumExpr).bigIntegerValue
    }

    @Test
    fun testSumMode() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val val1 by bv8Sort
        val val2 by bv8Sort
        val val3 by bv8Sort
        val val4 by bv8Sort
        val val5 by bv8Sort

        val expr =
            mkAnd(
                mkBvSubExpr(
                    mkBvShiftLeftExpr(
                        mkBvNegationExpr(
                            mkBvAndExpr(
                                mkBvMulExpr(val1, val2),
                                val4
                            )
                        ),
                        val3
                    ),
                    val5
                ) eq mkBv("01011101", 8u).uncheckedCast(),
                val4 neq mkBv(0.toByte())
            )

        val rewriter = KBv2IntRewriter(this, KBv2IntRewriter.RewriteMode.SUM)
        val transformedExpr = rewriter.rewriteBv2Int(expr)

        with(KCvc5Solver(this)) {
            push()

            assert(transformedExpr)
            assert(check() == KSolverStatus.SAT)

            val model = model().detach()

            pop()

            assert(val1 eq mkBv(rewriter.extractInterpretation(model, val1).toByte()))
            assert(val2 eq mkBv(rewriter.extractInterpretation(model, val2).toByte()))
            assert(val3 eq mkBv(rewriter.extractInterpretation(model, val3).toByte()))
            assert(val4 eq mkBv(rewriter.extractInterpretation(model, val4).toByte()))
            assert(val5 eq mkBv(rewriter.extractInterpretation(model, val5).toByte()))

            assert(expr)

            assert(check() == KSolverStatus.SAT)
        }
    }
}
