package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KIntNumExpr
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.sort.KIntSort
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriteMode: RewriteMode = RewriteMode.EAGER,
    private val andRewriteMode: AndRewriteMode = AndRewriteMode.SUM,
    private val signednessMode: SignednessMode = SignednessMode.UNSIGNED
) : KSolver<Config> by solver {
    init {
//        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)
        require(!(rewriteMode == RewriteMode.LAZY && signednessMode != SignednessMode.UNSIGNED)) {
            "Not supported"
        }
    }

    override var roundCount: Int = 0

    private var lastCheckStatus = KSolverStatus.UNKNOWN

    private val bv2IntContext = KBv2IntContext(ctx)
    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, signednessMode)
    private val unsignedRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, SignednessMode.UNSIGNED)
    }

    private var lastUsedRewriter = rewriter

    private val originalExpressions = hashMapOf<KExpr<KBoolSort>, KExpr<KBoolSort>>()
    private var currentBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()
    private var currentAssertedExprs = mutableListOf<KExpr<KBoolSort>>()

    override fun resetCheckTime() {
        solver.resetCheckTime()
    }

    override fun getCheckTime(): Long {
        return solver.getCheckTime()
    }

    override fun assert(expr: KExpr<KBoolSort>) {
        val rewritten = rewriter.rewriteBv2Int(expr)

        currentAssertedExprs.add(rewritten)

        if (signednessMode != SignednessMode.UNSIGNED) {
            originalExpressions[rewritten] = expr
            solver.push()
        }

        solver.assert(rewritten)

        if (rewriteMode != RewriteMode.EAGER) {
            currentBvAndLemmas.addAll(rewriter.bvAndLemmas(rewritten))
        }
    }

    private fun lemmaCheck(
        lemma: KExpr<KBoolSort>,
        model: KModel,
        importantApps: Set<KExpr<*>>
    ): Boolean = with(ctx) {
        if (lemma == trueExpr) return true
        val application = rewriter.extractBvAndApplication(lemma) ?: error("Unexpected")

        require(application is KFunctionApp<*> && application.decl == bv2IntContext.bvAndFunc)

        if (application !in importantApps) return true
        if (model.eval(application) !is KIntNumExpr) return true

        return model.eval(lemma) == trueExpr
    }

    private fun lazyCheck(timeout: Duration): KSolverStatus {
        roundCount = 0
        val start = Date().time
        var left = timeout
        val assumptions = mutableListOf<KExpr<KBoolSort>>()
        var lemmas: List<KExpr<KBoolSort>> = currentBvAndLemmas

        while (left.isPositive()) {
            roundCount++
            val status = solver.checkWithAssumptions(assumptions, left)
            if (status != KSolverStatus.SAT) return status

            val model = solver.model()

            val importantApps = KImportantAppCollector.collectImportantApps(
                currentAssertedExprs.first(),
                model,
                bv2IntContext
            )

            val (satisfied, unsatisfied) = lemmas.partition {
                lemmaCheck(it, model, importantApps)
            }

            if (unsatisfied.isEmpty()) return KSolverStatus.SAT

            lemmas = satisfied
            assumptions.addAll(unsatisfied)

            left = timeout - (Date().time - start).milliseconds
        }

        return KSolverStatus.UNKNOWN
    }

    private fun signedCheck(timeout: Duration): KSolverStatus {
        roundCount = 0
        lastUsedRewriter = rewriter
        val start = Date().time
        var left = timeout

        while (left.isPositive()) {
            roundCount++
            val status = solver.check(left)
//            val status = solver.checkWithAssumptions(currentLevelAssertedExprs, left)
            left = timeout - (Date().time - start).milliseconds

            when {
                status == KSolverStatus.UNKNOWN -> return KSolverStatus.UNKNOWN
                status == KSolverStatus.SAT && signednessMode == SignednessMode.SIGNED_NO_OVERFLOW ->
                    return KSolverStatus.SAT
                status == KSolverStatus.UNSAT -> {
                    lastUsedRewriter = unsignedRewriter
                    val unsignedExpressions = currentAssertedExprs.map {
                        unsignedRewriter.rewriteBv2Int(originalExpressions[it] ?: error("Unexpected"))
                    }

                    return if (left.isPositive()) {
                        solver.pop()
                        solver.push()
                        unsignedExpressions.forEach { solver.assert(it) }
                        solver.check(left)

//                        solver.checkWithAssumptions(unsignedExpressions, left)
                    } else {
                        KSolverStatus.UNKNOWN
                    }
                }
            }

            val model = solver.model()

//            val decls = KExprUninterpretedDeclCollector
//                .collectUninterpretedDeclarations(currentAssertedExprs.first())
//            val interprs = decls.map { it to model.interpretation(it) }

            var correctModel = true
            val exprs = currentAssertedExprs.map { expr ->
                val transformed = KBv2IntOverflowChecker.overflowCheck(expr, model, rewriter) ?: expr
                correctModel = correctModel && transformed == expr
                if (transformed != expr) {
                    originalExpressions[transformed] = originalExpressions[expr] ?: error("Unexpected")
                }
                transformed
            }

            if (correctModel) return status

            solver.pop()
            solver.push()
            exprs.forEach { assert(it) }

            currentAssertedExprs = exprs.toMutableList()
        }

        return KSolverStatus.UNKNOWN
    }

    override fun check(timeout: Duration): KSolverStatus {
        roundCount = 1
        return when {
            rewriteMode == RewriteMode.LAZY -> lazyCheck(timeout)
            signednessMode != SignednessMode.UNSIGNED -> signedCheck(timeout)
            else -> solver.check(timeout)
        }.also { lastCheckStatus = it }
    }

    override fun model(): KModel {
        require(lastCheckStatus == KSolverStatus.SAT) {
            "Model are only available after SAT checks, current solver status: $lastCheckStatus"
        }

        return KBv2IntModel(ctx, bv2IntContext, solver.model(), lastUsedRewriter)
    }
}