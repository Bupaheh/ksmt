package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KIntNumExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import java.util.Date
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

open class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriteMode: RewriteMode = RewriteMode.EAGER,
    private val andRewriteMode: AndRewriteMode = AndRewriteMode.SUM,
    private val signednessMode: SignednessMode = SignednessMode.UNSIGNED
) : KSolver<Config> by solver {
    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)

        if (signednessMode != SignednessMode.UNSIGNED) {
            solver.push()
        }
    }

    private var roundCount: Int = 0
    private var checkTime: Long = 0

    private var currentScope: UInt = 0u
    private var lastCheckStatus = KSolverStatus.UNKNOWN

    private var lastUnsatScope: UInt = UInt.MAX_VALUE
    private val currentRewriter
        get() = if (isUnsatRewriter) unsatRewriter else rewriter
    private val isUnsatRewriter: Boolean
        get() = lastUnsatScope <= currentScope

    private val bv2IntContext = KBv2IntContext(ctx)
    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, signednessMode)
    private val unsatRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, SignednessMode.SIGNED)
    }

    private var currentBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()
    private var currentOverflowLemmas = mutableListOf<KExpr<KBoolSort>>()
    private val originalExpressions = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssertedExprs = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssumptions = mutableListOf<KExpr<KBoolSort>>()
    private var originalAssumptions = listOf<KExpr<KBoolSort>>()

    override fun assert(expr: KExpr<KBoolSort>) {
        val rewritten = currentRewriter.rewriteBv2Int(expr)

        currentAssertedExprs.add(rewritten)
        currentOverflowLemmas.add(currentRewriter.overflowLemmas(rewritten))

        if (signednessMode != SignednessMode.UNSIGNED && signednessMode != SignednessMode.SIGNED) {
            originalExpressions.add(expr)
        }

        solver.assert(rewritten)

        if (rewriteMode == RewriteMode.LAZY) {
            currentBvAndLemmas.addAll(currentRewriter.bvAndLemmas(rewritten))
        }
    }

    override fun assert(exprs: List<KExpr<KBoolSort>>) {
        exprs.forEach { assert(it) }
    }

    private fun reassertExpressions() {
        solver.pop()
        solver.push()
        currentAssertedExprs.forEach { solver.assert(it) }
    }

    private fun checkBvAndLemma(
        lemma: KExpr<KBoolSort>,
        model: KModel,
        importantApps: Set<KExpr<*>>
    ): Boolean = with(ctx) {
        if (lemma == trueExpr) return true
        val application = bv2IntContext.extractBvAndApplication(lemma) ?: error("Unexpected")

        require(application is KFunctionApp<*> && application.decl == bv2IntContext.bvAndFunc)

        if (application !in importantApps) return true
        if (model.eval(application) !is KIntNumExpr) return true

        return model.eval(lemma) == trueExpr
    }

    private fun timeLeft(start: Date, timeout: Duration) = timeout - (Date().time - start.time).milliseconds

    private fun innerCheck(timeout: Duration): KSolverStatus {
        if (timeout.isNegative()) return KSolverStatus.UNKNOWN

        val status: KSolverStatus

        val time = measureNanoTime {
            status = if (currentAssumptions.isEmpty()) {
                solver.check(timeout)
            } else {
                solver.checkWithAssumptions(currentAssumptions, timeout)
            }
        }

        checkTime += time
        return status
    }

    private fun signedCheck(timeout: Duration): KSolverStatus {
        val start = Date()
        val status = innerCheck(timeout)

        if (signednessMode == SignednessMode.UNSIGNED ||
            signednessMode == SignednessMode.SIGNED ||
            status == KSolverStatus.UNKNOWN ||
            isUnsatRewriter
        ) {
            return status
        }

        var isCorrect = status == KSolverStatus.SAT

        if (status == KSolverStatus.SAT) {
            val model = solver.model()

            currentOverflowLemmas.forEach { overflowLemma ->
                LemmaFlatter.flatLemma(overflowLemma).forEach {
                    isCorrect = isCorrect && (model.eval(it, true) == ctx.trueExpr)
                }
            }
        }

        if (isCorrect) return status

        roundCount++
        lastUnsatScope = currentScope
        currentOverflowLemmas.clear()
        currentAssertedExprs = originalExpressions.map { expr ->
//                ctx.mkAndNoSimplify(ctx.trueExpr, expr).also { currentBvAndLemmas.clear() }
            unsatRewriter.rewriteBv2Int(expr).also { rewritten ->
                currentBvAndLemmas = unsatRewriter.bvAndLemmas(rewritten).toMutableList()
                currentOverflowLemmas.add(currentRewriter.overflowLemmas(rewritten))
            }
        }.toMutableList()
        currentAssumptions = originalAssumptions.map { unsatRewriter.rewriteBv2Int(it) }.toMutableList()

        reassertExpressions()
        return innerCheck(timeLeft(start, timeout))
    }

    private fun lazyCheck(timeout: Duration): KSolverStatus {
        val start = Date()
        var left = timeout

        while (left.isPositive()) {
            val status = signedCheck(left)
            if (status != KSolverStatus.SAT) return status

            val model = solver.model()

            val importantApps = KImportantAppCollector.collectImportantApps(
                currentAssertedExprs + currentAssumptions,
                model,
                bv2IntContext
            )

            val unsatisfied = currentBvAndLemmas.filterNot {
                checkBvAndLemma(it, model, importantApps)
            }

            if (unsatisfied.isEmpty()) return KSolverStatus.SAT

            currentAssertedExprs.addAll(unsatisfied.map { ctx.mkAndNoSimplify(ctx.trueExpr, it) })
            unsatisfied.forEach { solver.assert(it) }

            left = timeLeft(start, timeout)
            roundCount++
        }

        return KSolverStatus.UNKNOWN
    }

    override fun check(timeout: Duration): KSolverStatus = checkWithAssumptions(listOf(), timeout)

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        roundCount = 1

        originalAssumptions = assumptions
        currentAssumptions = assumptions.map { currentRewriter.rewriteBv2Int(it) }.toMutableList()

        return if (rewriteMode == RewriteMode.LAZY) {
            lazyCheck(timeout)
        } else {
            signedCheck(timeout)
        }.also { lastCheckStatus = it }
    }

    override fun pop(n: UInt) {
        checkTime = 0
        solver.pop(n)
    }

    override fun reasonOfUnknown(): String = "$checkTime;$roundCount"

    override fun model(): KModel {
        require(lastCheckStatus == KSolverStatus.SAT) {
            "Model are only available after SAT checks, current solver status: $lastCheckStatus"
        }

        return KBv2IntModel(ctx, bv2IntContext, solver.model())
    }
}