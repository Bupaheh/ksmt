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
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


open class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriterConfig: KBv2IntRewriterConfig,
    equisatisfiableRewriterConfig: KBv2IntRewriterConfig = KBv2IntRewriterConfig(disableRewriting = true),
) : KSolver<Config> {
    private val bv2IntContext = KBv2IntContext(ctx)
    private val splitter = KBv2IntSplitter(ctx)

    private var currentScope: UInt = 0u
    private var lastCheckStatus = KSolverStatus.UNKNOWN

    private var lastUnsatScope: UInt = UInt.MAX_VALUE

    private val isUnsatRewriter: Boolean
        get() = lastUnsatScope <= currentScope
    private val currentRewriter
        get() = if (isUnsatRewriter) unsatRewriter else rewriter
    private val currentConfig
        get() = if (isUnsatRewriter) unsatRewriter.config else rewriter.config

    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, rewriterConfig, )
    private val unsatRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, equisatisfiableRewriterConfig)
    }

    private val assertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
    private val rewrittenAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
    
    private val trackedAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
    private val rewrittenTrackedAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())

    private var assumptions = listOf<KExpr<KBoolSort>>()
    private var rewrittenAssumptions = listOf<KExpr<KBoolSort>>()

    private val bvAndLemmas = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
    private val overflowLemmas = mutableListOf(mutableListOf<KExpr<KBoolSort>>())

    private var roundCnt = 0

    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)
        require(!equisatisfiableRewriterConfig.isLazyOverflow)

        if (currentConfig.isLazyOverflow) {
            solver.push()
        }
    }

    private inline fun modifyCurrentScope(
        modifyAssertions: (MutableList<KExpr<KBoolSort>>) -> Unit,
        modifyRewrittenAssertions: (MutableList<KExpr<KBoolSort>>) -> Unit,
        modifyTrackedAssertions: (MutableList<KExpr<KBoolSort>>) -> Unit,
        modifyRewrittenTrackedAssertions: (MutableList<KExpr<KBoolSort>>) -> Unit,
        modifyBvAndLemmas: (MutableList<KExpr<KBoolSort>>) -> Unit,
        modifyOverflowLemmas: (MutableList<KExpr<KBoolSort>>) -> Unit,
    ) = modifyScope(
        modifyAssertions = { modifyAssertions(it.last()) },
        modifyRewrittenAssertions = { modifyRewrittenAssertions(it.last()) },
        modifyTrackedAssertions = { modifyTrackedAssertions(it.last()) },
        modifyRewrittenTrackedAssertions = { modifyRewrittenTrackedAssertions(it.last()) },
        modifyBvAndLemmas = { modifyBvAndLemmas(it.last()) },
        modifyOverflowLemmas = { modifyOverflowLemmas(it.last()) }
    )

    private inline fun modifyScope(
        modifyAssertions: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
        modifyRewrittenAssertions: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
        modifyTrackedAssertions: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
        modifyRewrittenTrackedAssertions: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
        modifyBvAndLemmas: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
        modifyOverflowLemmas: (MutableList<MutableList<KExpr<KBoolSort>>>) -> Unit,
    ) {
        modifyTrackedAssertions(trackedAssertions)
        modifyRewrittenTrackedAssertions(rewrittenTrackedAssertions)

        if (rewriterConfig.isLazyOverflow) {
            modifyAssertions(assertions)
        }

        if (currentConfig.isLazyOverflow) {
            modifyOverflowLemmas(overflowLemmas)
        }

        if (currentConfig.isLazyBvAnd) {
            modifyBvAndLemmas(bvAndLemmas)
            modifyRewrittenAssertions(rewrittenAssertions)
        }
    }

    override fun configure(configurator: Config.() -> Unit) {
        solver.configure(configurator)
    }

    private fun assertBase(expr: KExpr<KBoolSort>, trackFlag: Boolean) {
        if (currentConfig.enableSplitter) splitter.apply(expr)

        val rewritten = currentRewriter.rewriteBv2Int(expr)

        if (trackFlag) {
            solver.assertAndTrack(rewritten)
        } else {
            solver.assert(rewritten)
        }

        modifyCurrentScope(
            modifyAssertions = { if (!trackFlag) it.add(expr) },
            modifyRewrittenAssertions = { if (!trackFlag) it.add(rewritten) },
            modifyTrackedAssertions = { if (trackFlag) it.add(expr) },
            modifyRewrittenTrackedAssertions = { if (trackFlag) it.add(rewritten) },
            modifyOverflowLemmas = { it.add(currentRewriter.overflowLemmas(rewritten)) },
            modifyBvAndLemmas = { it.addAll(currentRewriter.bvAndLemmas(rewritten)) }
        )
    }

    override fun assert(expr: KExpr<KBoolSort>) = assertBase(expr, false)
    override fun assertAndTrack(expr: KExpr<KBoolSort>) = assertBase(expr, true)

    private fun reassertExpressions() {
        solver.pop(currentScope + 1u)

        overflowLemmas.clear()
        bvAndLemmas.clear()
        rewrittenAssertions.clear()
        rewrittenTrackedAssertions.clear()

        assertions.zip(trackedAssertions).forEach { (scopeAssertions, scopeTrackedAssertions) ->
            val exprsToAssert = scopeAssertions.map { currentRewriter.rewriteBv2Int(it) }
            val exprsToAssertAndTrack = scopeTrackedAssertions.map { currentRewriter.rewriteBv2Int(it) }

            val rewrittenExprs = exprsToAssert + exprsToAssertAndTrack

            solver.push()
            solver.assert(exprsToAssert)
            solver.assertAndTrack(exprsToAssertAndTrack)

            modifyScope(
                modifyAssertions = {},
                modifyRewrittenAssertions = { it.add(exprsToAssert.toMutableList()) },
                modifyTrackedAssertions = {},
                modifyRewrittenTrackedAssertions = { it.add(exprsToAssertAndTrack.toMutableList()) },
                modifyOverflowLemmas = { it.add(rewrittenExprs.map { currentRewriter.overflowLemmas(it) }.toMutableList()) },
                modifyBvAndLemmas = { it.add(rewrittenExprs.map { currentRewriter.bvAndLemmas(it) }.flatten().toMutableList()) },
            )
        }

        rewrittenAssumptions = assumptions.map { currentRewriter.rewriteBv2Int(it) }

        /**
         * ASSUMPTIONS LEMMAS !!!!!!
         */
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

        println(currentConfig)

        return if (rewrittenAssumptions.isEmpty()) {
            solver.check(timeout)
        } else {
            solver.checkWithAssumptions(rewrittenAssumptions, timeout)
        }
    }

    private fun signedCheck(timeout: Duration): KSolverStatus {
        val start = Date()
        val status = innerCheck(timeout)

        if (!currentConfig.isLazyOverflow || status == KSolverStatus.UNKNOWN) {
            return status
        }

        var isCorrect = status == KSolverStatus.SAT

        if (status == KSolverStatus.SAT) {
            val model = solver.model()
            val evalResult = model.eval(ctx.mkAndNoSimplify(overflowLemmas.flatten()), true)

            isCorrect = evalResult == ctx.trueExpr
        }

        if (isCorrect) return status

        roundCnt++
        lastUnsatScope = currentScope

        reassertExpressions()

        return innerCheck(timeLeft(start, timeout))
    }

    private fun lazyCheck(timeout: Duration): KSolverStatus {
        val start = Date()
        var left = timeout

        val flattenBvAndLemmas = bvAndLemmas.flatten()
        val exprsToCheck = rewrittenAssertions.flatten() + rewrittenTrackedAssertions.flatten() + rewrittenAssumptions

        while (left.isPositive()) {
            val status = signedCheck(left)
            if (status != KSolverStatus.SAT || !currentConfig.isLazyBvAnd) return status

            val model = solver.model()

            val importantApps = KImportantAppCollector.collectImportantApps(
                exprsToCheck,
                model,
                bv2IntContext
            )

            val unsatisfied = flattenBvAndLemmas.filterNot {
                checkBvAndLemma(it, model, importantApps)
            }

            if (unsatisfied.isEmpty()) return KSolverStatus.SAT

            solver.assert(unsatisfied)

            left = timeLeft(start, timeout)

            roundCnt++
        }

        return KSolverStatus.UNKNOWN
    }

    override fun check(timeout: Duration): KSolverStatus = checkWithAssumptions(emptyList(), timeout)

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        roundCnt = 1

        this.assumptions = assumptions
        rewrittenAssumptions = assumptions.map { currentRewriter.rewriteBv2Int(it) }

        return if (currentConfig.isLazyBvAnd) {
            lazyCheck(timeout)
        } else {
            signedCheck(timeout)
        }.also { lastCheckStatus = it }
    }

    override fun push() {
        currentScope++
        solver.push()

        modifyScope(
            modifyAssertions = { it.add(mutableListOf()) },
            modifyRewrittenAssertions = { it.add(mutableListOf()) },
            modifyTrackedAssertions = { it.add(mutableListOf()) },
            modifyRewrittenTrackedAssertions = { it.add(mutableListOf()) },
            modifyOverflowLemmas = { it.add(mutableListOf()) },
            modifyBvAndLemmas = { it.add(mutableListOf()) },
        )
    }

    override fun pop(n: UInt) {
        solver.pop(n)

        repeat(n.toInt()) {
            modifyScope(
                modifyAssertions = { it.removeLast() },
                modifyRewrittenAssertions = { it.removeLast() },
                modifyTrackedAssertions = { it.removeLast() },
                modifyRewrittenTrackedAssertions = { it.removeLast() },
                modifyOverflowLemmas = { it.removeLast() },
                modifyBvAndLemmas = { it.removeLast() },
            )
        }

        currentScope -= n
        if (lastUnsatScope <= currentScope != lastUnsatScope <= currentScope + n) {
            lastUnsatScope = UInt.MAX_VALUE
            reassertExpressions()
        }
    }

    override fun reasonOfUnknown(): String {
        val innerReason = solver.reasonOfUnknown()

        return innerReason.substringBefore(';') + ";$roundCnt"
    }

    override fun interrupt() {
        solver.interrupt()
    }

    override fun model(): KModel {
        require(lastCheckStatus == KSolverStatus.SAT) {
            "Model are only available after SAT checks, current solver status: $lastCheckStatus"
        }

        return if (currentConfig.disableRewriting) {
            solver.model()
        } else {
            KBv2IntModel(ctx, bv2IntContext, solver.model())
        }
    }

    override fun unsatCore(): List<KExpr<KBoolSort>> {
        val innerUnsatCore = solver.unsatCore().toSet()

        return (assumptions.zip(rewrittenAssumptions) + trackedAssertions.flatten().zip(rewrittenTrackedAssertions))
            .mapNotNull { (expr, rewritten) ->
                expr.takeIf { it in innerUnsatCore }
            }
    }

    override fun close() {
        solver.close()
    }
}
