package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriteMode: RewriteMode,
    andRewriteMode: AndRewriteMode
) : KSolver<Config> by solver {
    private val rewriter = KBv2IntRewriter(ctx, rewriteMode, andRewriteMode)

    private var currentLevelBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()

    private val solverHelper = solver

    override fun assert(expr: KExpr<KBoolSort>) {
        solver.assert(rewriter.rewriteBv2Int(expr))

        if (rewriteMode != RewriteMode.EAGER) {
            currentLevelBvAndLemmas.addAll(rewriter.bvAndLemmas)
        }
    }

    override fun check(timeout: Duration): KSolverStatus {
        if (rewriteMode == RewriteMode.EAGER) return solver.check(timeout)

        val start = Date().time
        var left = timeout
        val assumptions = mutableListOf<KExpr<KBoolSort>>()
        var lemmas: List<KExpr<KBoolSort>> = currentLevelBvAndLemmas

        while (left.isPositive()) {
            var status = solver.checkWithAssumptions(assumptions, left)
            if (status != KSolverStatus.SAT) return status

            val model = solver.model()

            val (satisfied, unsatisfied) = lemmas.partition {
                model.eval(it, false) == ctx.trueExpr
            }

            if (unsatisfied.isEmpty()) return KSolverStatus.SAT

            lemmas = satisfied
            assumptions.addAll(unsatisfied)

            left = timeout - (Date().time - start).milliseconds
        }

        return KSolverStatus.UNKNOWN
    }

    override fun close() {
        solverHelper.close()
        solver.close()
    }

    override fun model(): KModel = KBv2IntModel(ctx, solver.model(), rewriter)
}