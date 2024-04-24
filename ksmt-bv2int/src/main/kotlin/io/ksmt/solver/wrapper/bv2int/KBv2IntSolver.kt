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

    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, rewriterConfig)
    private val unsatRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, splitter.dsu, equisatisfiableRewriterConfig)
    }

    private var scopes = Scopes(currentConfig)

    private var roundCnt = 0

    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)
        require(!equisatisfiableRewriterConfig.isLazyOverflow)

        if (currentConfig.isLazyOverflow) {
            solver.push()
        }
    }

    override fun configure(configurator: Config.() -> Unit) {
        solver.configure(configurator)
    }

    override fun assert(expr: KExpr<KBoolSort>) {
        if (currentConfig.enableSplitter) splitter.apply(expr)
        val rewritten = currentRewriter.rewriteBv2Int(expr)

        solver.assert(rewritten)
        scopes.assert(expr, rewritten, currentRewriter)
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>) {
        if (currentConfig.enableSplitter) splitter.apply(expr)
        val rewritten = currentRewriter.rewriteBv2Int(expr)

        solver.assertAndTrack(rewritten)
        scopes.assertAndTrack(expr, rewritten, currentRewriter)
    }

    private fun reassertExpressions() {
        solver.pop(currentScope + 1u)

        val newScope = Scopes(currentConfig)
        val assumptions = scopes.assumptions

        scopes.resetAssumptions()

        var isFirst = true

        scopes.assertions.zip(scopes.trackedAssertions).forEach { (scopeAssertions, scopeTrackedAssertions) ->
            val exprsToAssert = scopeAssertions.map { currentRewriter.rewriteBv2Int(it) }
            val exprsToAssertAndTrack = scopeTrackedAssertions.map { currentRewriter.rewriteBv2Int(it) }

            solver.push()
            if (!isFirst) {
                newScope.push()
            } else {
                isFirst = false
            }

            solver.assert(exprsToAssert)
            solver.assertAndTrack(exprsToAssertAndTrack)

            scopeAssertions.zip(exprsToAssert).forEach { newScope.assert(it.first, it.second, rewriter) }
            scopeTrackedAssertions.zip(exprsToAssertAndTrack).forEach { newScope.assertAndTrack(it.first, it.second, rewriter) }
        }

        val rewrittenAssumptions = assumptions.map { currentRewriter.rewriteBv2Int(it) }
        newScope.assume(assumptions, rewrittenAssumptions, rewriter)

        scopes = newScope
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

        return if (scopes.isEmptyAssumptions) {
            solver.check(timeout)
        } else {
            solver.checkWithAssumptions(scopes.rewrittenAssumptions, timeout)
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
            val evalResult = model.eval(ctx.mkAndNoSimplify(scopes.overflowLemmasFlatten), true)

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

        var flattenBvAndLemmas = scopes.bvLemmasFlatten
        var exprsToCheck = scopes.assertedExprs
        var prevConfig = currentConfig

        while (left.isPositive()) {
            val status = signedCheck(left)
            if (status != KSolverStatus.SAT || !currentConfig.isLazyBvAnd) return status

            if (prevConfig != currentConfig) {
                flattenBvAndLemmas = scopes.bvLemmasFlatten
                exprsToCheck = scopes.assertedExprs
                prevConfig = currentConfig
            }

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

        val rewritten = assumptions.map { currentRewriter.rewriteBv2Int(it) }

        scopes.assume(assumptions, rewritten, rewriter)

        return if (currentConfig.isLazyBvAnd) {
            lazyCheck(timeout)
        } else {
            signedCheck(timeout)
        }.also {
            scopes.resetAssumptions()
            lastCheckStatus = it
        }
    }

    override fun push() {
        currentScope++
        solver.push()
        scopes.push()
    }

    override fun pop(n: UInt) {
        solver.pop(n)
        scopes.pop(n)

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
        scopes.resetAssumptions()
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

    override fun unsatCore(): List<KExpr<KBoolSort>> = scopes.resolveUnsatCore(solver.unsatCore())

    override fun close() {
        solver.close()
    }

    private class Scopes(val config: KBv2IntRewriterConfig) {
        val assertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
        private val rewrittenAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())

        val trackedAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
        private val rewrittenTrackedAssertions = mutableListOf(mutableListOf<KExpr<KBoolSort>>())

        private val bvAndLemmas = mutableListOf(mutableListOf<KExpr<KBoolSort>>())
        private val overflowLemmas = mutableListOf(mutableListOf<KExpr<KBoolSort>>())

        private var assumptionsSize = 0
        private var assumptionsBvAndLemmasSize = 0

        private var currentScope = 0

        val assumptions: List<KExpr<KBoolSort>>
            get() {
                val currentLevelAssertions = assertions.last()
                val size = currentLevelAssertions.size

                return currentLevelAssertions.subList(size - assumptionsSize, size).toList()
            }

        val rewrittenAssumptions: List<KExpr<KBoolSort>>
            get() {
                val currentLevelAssertions = rewrittenAssertions.last()
                val size = currentLevelAssertions.size

                return currentLevelAssertions.subList(size - assumptionsSize, size)
            }

        val overflowLemmasFlatten: List<KExpr<KBoolSort>>
            get() = overflowLemmas.flatten()

        val bvLemmasFlatten: List<KExpr<KBoolSort>>
            get() = bvAndLemmas.flatten()

        val assertedExprs: List<KExpr<KBoolSort>>
            get() = rewrittenAssertions.flatten() + rewrittenTrackedAssertions.flatten()

        val isEmptyAssumptions: Boolean
            get() = assumptionsSize == 0

        fun assert(expr: KExpr<KBoolSort>, rewritten: KExpr<KBoolSort>, rewriter: KBv2IntRewriter) {
            modifyCurrentScope(
                modifyAssertions = { it.add(expr) },
                modifyRewrittenAssertions = { it.add(rewritten) },
                modifyTrackedAssertions = { },
                modifyRewrittenTrackedAssertions = { },
                modifyOverflowLemmas = { it.add(rewriter.overflowLemmas(rewritten)) },
                modifyBvAndLemmas = { it.addAll(rewriter.bvAndLemmas(rewritten)) }
            )
        }

        fun assertAndTrack(expr: KExpr<KBoolSort>, rewritten: KExpr<KBoolSort>, rewriter: KBv2IntRewriter) {
            modifyCurrentScope(
                modifyAssertions = { },
                modifyRewrittenAssertions = { },
                modifyTrackedAssertions = { it.add(expr) },
                modifyRewrittenTrackedAssertions = { it.add(rewritten) },
                modifyOverflowLemmas = { it.add(rewriter.overflowLemmas(rewritten)) },
                modifyBvAndLemmas = { it.addAll(rewriter.bvAndLemmas(rewritten)) }
            )
        }

        fun assume(assumptions: List<KExpr<KBoolSort>>, rewritten: List<KExpr<KBoolSort>>, rewriter: KBv2IntRewriter) {
            assumptionsSize = assumptions.size

            modifyCurrentScope(
                modifyAssertions = { it.addAll(assumptions) },
                modifyRewrittenAssertions = { it.addAll(rewritten) },
                modifyTrackedAssertions = { },
                modifyRewrittenTrackedAssertions = { },
                modifyOverflowLemmas = { it.addAll(rewritten.map { rewriter.overflowLemmas(it) }) },
                modifyBvAndLemmas = {
                    val assumptionBvAndLemmas = rewritten.map { rewriter.bvAndLemmas(it) }.flatten()
                    assumptionsBvAndLemmasSize = assumptionBvAndLemmas.size
                    it.addAll(assumptionBvAndLemmas)
                }
            )
        }

        fun resetAssumptions() {
            if (assumptionsSize == 0) return

            modifyCurrentScope(
                modifyAssertions = { it.removeLast(assumptionsSize) },
                modifyRewrittenAssertions = { it.removeLast(assumptionsSize) },
                modifyTrackedAssertions = { },
                modifyRewrittenTrackedAssertions = { },
                modifyOverflowLemmas = { it.removeLast(assumptionsSize) },
                modifyBvAndLemmas = { it.removeLast(assumptionsBvAndLemmasSize) }
            )

            assumptionsSize = 0
            assumptionsBvAndLemmasSize = 0
        }

        fun push() {
            currentScope++

            modifyScope(
                modifyAssertions = { it.add(mutableListOf()) },
                modifyRewrittenAssertions = { it.add(mutableListOf()) },
                modifyTrackedAssertions = { it.add(mutableListOf()) },
                modifyRewrittenTrackedAssertions = { it.add(mutableListOf()) },
                modifyOverflowLemmas = { it.add(mutableListOf()) },
                modifyBvAndLemmas = { it.add(mutableListOf()) },
            )
        }

        fun pop(n: UInt) {
            currentScope -= n.toInt()

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
        }

        fun resolveUnsatCore(unsatCore: List<KExpr<KBoolSort>>): List<KExpr<KBoolSort>> {
            val unsatCoreSet = unsatCore.toSet()

            return (assumptions.zip(rewrittenAssumptions) +
                    trackedAssertions.flatten().zip(rewrittenTrackedAssertions.flatten()))
                .mapNotNull { (expr, rewritten) ->
                    expr.takeIf { rewritten in unsatCoreSet }
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

            modifyAssertions(assertions)
            modifyRewrittenAssertions(rewrittenAssertions)

            if (config.isLazyOverflow) {
                modifyOverflowLemmas(overflowLemmas)
            }

            if (config.isLazyBvAnd) {
                modifyBvAndLemmas(bvAndLemmas)
            }
        }

        private fun <T> MutableList<T>.removeLast(n: Int) {
            repeat(n) {
                removeLast()
            }
        }
    }
}
