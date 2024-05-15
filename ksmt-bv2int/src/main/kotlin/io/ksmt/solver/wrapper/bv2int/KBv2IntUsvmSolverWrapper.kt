package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KExpr
import io.ksmt.expr.transformer.KNonRecursiveVisitor
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration

class KBv2IntUsvmSolverWrapper<Config: KSolverConfiguration>(
    private val bv2intSolver: KBv2IntSolver<Config>,
    private val originalSolver: KSolver<Config>,
    private val exprFilter: KNonRecursiveVisitor<Boolean>,
) : KSolver<Config> {
    private val assertions = mutableListOf<KExpr<KBoolSort>>()
    private val trackedAssertions = mutableListOf<KExpr<KBoolSort>>()
    private var currentScope = 0

    private var isRewriteSolver = true
    private val currentSolver: KSolver<Config>
        get() = if (isRewriteSolver) bv2intSolver else originalSolver

    override fun configure(configurator: Config.() -> Unit) {
        error("Unexpected call")
    }

    override fun assert(expr: KExpr<KBoolSort>) {
        require(currentScope == 1)

        if (!isRewriteSolver) {
            currentSolver.assert(expr)
        }

        if (!exprFilter.applyVisitor(expr)) {
            reassertExprs()
        }

        currentSolver.assert(expr)
        assertions.add(expr)
    }

    override fun assert(exprs: List<KExpr<KBoolSort>>) {
        require(currentScope == 1)

        if (!isRewriteSolver) {
            currentSolver.assert(exprs)
        }

        if (!exprs.all { exprFilter.applyVisitor(it) }) {
            reassertExprs()
        }

        currentSolver.assert(exprs)
        assertions.addAll(exprs)
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>) {
        require(currentScope == 1)

        if (!isRewriteSolver) {
            currentSolver.assertAndTrack(expr)
        }

        if (!exprFilter.applyVisitor(expr)) {
            reassertExprs()
        }

        currentSolver.assertAndTrack(expr)
        trackedAssertions.add(expr)
    }

    override fun assertAndTrack(exprs: List<KExpr<KBoolSort>>) {
        require(currentScope == 1)

        if (!isRewriteSolver) {
            currentSolver.assertAndTrack(exprs)
        }

        if (!exprs.all { exprFilter.applyVisitor(it) }) {
            reassertExprs()
        }

        currentSolver.assertAndTrack(exprs)
        trackedAssertions.addAll(exprs)
    }

    private fun reassertExprs() {
        currentSolver.pop()

        isRewriteSolver = false
        currentSolver.push()

        currentSolver.assert(assertions)
        currentSolver.assertAndTrack(trackedAssertions)
    }

    override fun push() {
        require(currentScope == 0)

        currentSolver.push()
    }

    override fun pop(n: UInt) {
        require(currentScope == 1 && n == 1u)

        currentSolver.pop(n)

        currentScope--
        assertions.clear()
        trackedAssertions.clear()
        isRewriteSolver = true
    }

    override fun check(timeout: Duration): KSolverStatus = currentSolver.check(timeout)

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        require(currentScope == 1)

        if (!isRewriteSolver) {
            return currentSolver.checkWithAssumptions(assumptions, timeout)
        }

        if (!assumptions.all { exprFilter.applyVisitor(it) }) {
            reassertExprs()
        }

        return currentSolver.checkWithAssumptions(assumptions, timeout)
    }

    override fun model(): KModel = currentSolver.model()

    override fun unsatCore(): List<KExpr<KBoolSort>> = currentSolver.unsatCore()

    override fun reasonOfUnknown(): String = currentSolver.reasonOfUnknown()

    override fun interrupt() = currentSolver.interrupt()

    override fun close() {
        bv2intSolver.close()
        originalSolver.close()
    }

}
