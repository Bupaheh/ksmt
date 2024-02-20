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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

open class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriterConfig: KBv2IntRewriterConfig,
    private val equisatisfiableRewriterConfig: KBv2IntRewriterConfig = KBv2IntRewriterConfig(disableRewriting = true),
) : KSolver<Config> by solver {
    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)

        if (!rewriterConfig.isLazyOverflow) {
            solver.push()
        }
    }

    private var currentScope: UInt = 0u
    private var lastCheckStatus = KSolverStatus.UNKNOWN

    private var lastUnsatScope: UInt = UInt.MAX_VALUE
    private val currentRewriter
        get() = if (isUnsatRewriter) unsatRewriter else rewriter
    private val isUnsatRewriter: Boolean
        get() = lastUnsatScope <= currentScope

    private val bv2IntContext = KBv2IntContext(ctx)
    private val splitter = KBv2IntSplitter(ctx)
    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, rewriterConfig, )
    private val unsatRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, equisatisfiableRewriterConfig)
    }

    private var currentBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()
    private var currentOverflowLemmas = mutableListOf<KExpr<KBoolSort>>()
    private val originalExpressions = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssertedExprs = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssumptions = mutableListOf<KExpr<KBoolSort>>()
    private var originalAssumptions = listOf<KExpr<KBoolSort>>()

    private var roundCnt = 0

    override fun assert(expr: KExpr<KBoolSort>) {
        if (rewriterConfig.enableSplitter) splitter.apply(expr)

        val rewritten = currentRewriter.rewriteBv2Int(expr)
        solver.assert(rewritten)

        if (rewriterConfig.isEquisatisfiable) return

        currentAssertedExprs.add(rewritten)

        if (rewriterConfig.isLazyOverflow) {
            currentOverflowLemmas.add(currentRewriter.overflowLemmas(rewritten))
            originalExpressions.add(expr)
        }

        if (rewriterConfig.isLazyOverflow) {
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

        return if (currentAssumptions.isEmpty()) {
            solver.check(timeout)
        } else {
            solver.checkWithAssumptions(currentAssumptions, timeout)
        }
    }

    @Suppress("ComplexCondition")
    private fun signedCheck(timeout: Duration): KSolverStatus {
        val start = Date()
        val status = innerCheck(timeout)

        if (!rewriterConfig.isLazyOverflow ||
            status == KSolverStatus.UNKNOWN ||
            isUnsatRewriter
        ) {
            return status
        }

        var isCorrect = status == KSolverStatus.SAT

        if (status == KSolverStatus.SAT) {
            val model = solver.model()

            currentOverflowLemmas.forEach { overflowLemma ->
                val evalResult = model.eval(overflowLemma, true)

                isCorrect = isCorrect && (evalResult == ctx.trueExpr)
            }
        }

        if (isCorrect) return status

        roundCnt++

        lastUnsatScope = currentScope
        currentOverflowLemmas.clear()
        currentAssertedExprs = originalExpressions.map { expr ->
            currentRewriter.rewriteBv2Int(expr).also { rewritten ->
                currentBvAndLemmas = currentRewriter.bvAndLemmas(rewritten).toMutableList()
                currentOverflowLemmas.add(currentRewriter.overflowLemmas(rewritten))
            }
        }.toMutableList()
        currentAssumptions = originalAssumptions.map { currentRewriter.rewriteBv2Int(it) }.toMutableList()

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
            solver.assert(unsatisfied)

            left = timeLeft(start, timeout)

            roundCnt++
        }

        return KSolverStatus.UNKNOWN
    }

    override fun check(timeout: Duration): KSolverStatus = checkWithAssumptions(listOf(), timeout)

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        roundCnt = 1

        originalAssumptions = assumptions
        currentAssumptions = assumptions.map { currentRewriter.rewriteBv2Int(it) }.toMutableList()

        return if (rewriterConfig.isLazyBvAnd) {
            lazyCheck(timeout)
        } else {
            signedCheck(timeout)
        }.also { lastCheckStatus = it }
    }

    override fun push() {
        // TODO("incorrect implementation")

        solver.push()
    }

    override fun pop(n: UInt) {
        // TODO("incorrect implementation")

        lastUnsatScope = UInt.MAX_VALUE
        currentBvAndLemmas.clear()
        currentOverflowLemmas.clear()
        originalExpressions.clear()
        currentAssertedExprs.clear()
        currentAssumptions.clear()
        originalAssumptions = listOf()
//        splitter.dsu.clear()

        solver.pop(n)
    }

    override fun reasonOfUnknown(): String {
        val innerReason = solver.reasonOfUnknown()

        return innerReason.substringBefore(';') + ";$roundCnt"
    }

    override fun model(): KModel {
        require(lastCheckStatus == KSolverStatus.SAT) {
            "Model are only available after SAT checks, current solver status: $lastCheckStatus"
        }

        return KBv2IntModel(ctx, bv2IntContext, solver.model())
    }

    override fun close() {
        solver.close()
    }
}
