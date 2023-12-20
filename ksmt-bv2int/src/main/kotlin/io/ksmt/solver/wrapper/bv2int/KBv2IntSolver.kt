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
    private val signednessMode: SignednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW,
    private val unsatSignednessMode: SignednessMode? = SignednessMode.SIGNED,
    private val round1Result: Boolean = false,
    private val isSplitterOn: Boolean = false
) : KSolver<Config> by solver {
    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)

        if (signednessMode != SignednessMode.UNSIGNED) {
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
    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, signednessMode, splitter.dsu, isSplitterOn)
    private val unsatRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, unsatSignednessMode!!, splitter.dsu, isSplitterOn)
    }

    private var currentBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()
    private var currentOverflowLemmas = mutableListOf<KExpr<KBoolSort>>()
    private val originalExpressions = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssertedExprs = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssumptions = mutableListOf<KExpr<KBoolSort>>()
    private var originalAssumptions = listOf<KExpr<KBoolSort>>()

    private var roundCnt = 0

    override fun assert(expr: KExpr<KBoolSort>) {
        splitter.apply(expr)

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

        measureNanoTime {
            status = if (currentAssumptions.isEmpty()) {
                solver.check(timeout)
            } else {
                solver.checkWithAssumptions(currentAssumptions, timeout)
            }
        }

        return status
    }

    @Suppress("ComplexCondition")
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

        if (round1Result) {
            return status
        }

        if (status == KSolverStatus.SAT) {
            val model = solver.model()

            currentOverflowLemmas.forEach { overflowLemma ->
                LemmaFlatter.flatLemma(overflowLemma).forEach {
                    val evalResult = model.eval(it, true)

                    isCorrect = isCorrect && (evalResult == ctx.trueExpr)
                }
            }
        }

        if (isCorrect) return status

        roundCnt++

        lastUnsatScope = currentScope
        currentOverflowLemmas.clear()
        currentAssertedExprs = originalExpressions.map { expr ->
            if (unsatSignednessMode == null) {
                return@map ctx.mkAndNoSimplify(ctx.trueExpr, expr).also { currentBvAndLemmas.clear() }
            }

            unsatRewriter.rewriteBv2Int(expr).also { rewritten ->
                currentBvAndLemmas = unsatRewriter.bvAndLemmas(rewritten).toMutableList()
                currentOverflowLemmas.add(currentRewriter.overflowLemmas(rewritten))
            }
        }.toMutableList()
        currentAssumptions = originalAssumptions.map {
            if (unsatSignednessMode == null) {
                return@map ctx.mkAndNoSimplify(ctx.trueExpr, it)
            }

            unsatRewriter.rewriteBv2Int(it)
        }.toMutableList()

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

        return if (rewriteMode == RewriteMode.LAZY) {
            lazyCheck(timeout)
        } else {
            signedCheck(timeout)
        }.also { lastCheckStatus = it }
    }

    override fun pop(n: UInt) {
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
}
