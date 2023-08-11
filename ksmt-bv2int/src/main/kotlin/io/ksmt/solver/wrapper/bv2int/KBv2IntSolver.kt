package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KAndBinaryExpr
import io.ksmt.expr.KAndExpr
import io.ksmt.expr.KAndNaryExpr
import io.ksmt.expr.KApp
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFunctionApp
import io.ksmt.expr.KIntNumExpr
import io.ksmt.expr.KIteExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.Signedness
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.utils.uncheckedCast
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class KBv2IntSolver<Config: KSolverConfiguration>(
    private val ctx: KContext,
    private val solver: KSolver<Config>,
    private val rewriteMode: RewriteMode = RewriteMode.EAGER,
    private val andRewriteMode: AndRewriteMode = AndRewriteMode.SUM,
    private val signednessMode: SignednessMode = SignednessMode.UNSIGNED
) : KSolver<Config> by solver {
    init {
        require(ctx.simplificationMode == KContext.SimplificationMode.SIMPLIFY)
        require(!(rewriteMode == RewriteMode.LAZY && signednessMode != SignednessMode.UNSIGNED)) {
            "Not supported"
        }
    }

    private var lastCheckStatus = KSolverStatus.UNKNOWN

    private val bv2IntContext = KBv2IntContext(ctx)
    private val rewriter = KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, signednessMode)
    private val unsignedRewriter by lazy {
        KBv2IntRewriter(ctx, bv2IntContext, rewriteMode, andRewriteMode, SignednessMode.UNSIGNED)
    }

    private var lastUsedRewriter = rewriter

    private val originalExpressions = hashMapOf<KExpr<KBoolSort>, KExpr<KBoolSort>>()
    private var currentLevelBvAndLemmas = mutableListOf<KExpr<KBoolSort>>()
    private var currentLevelAssertedExprs = mutableListOf<KExpr<KBoolSort>>()

    override fun assert(expr: KExpr<KBoolSort>) {
        val rewritten = rewriter.rewriteBv2Int(expr)

        if (signednessMode != SignednessMode.UNSIGNED) {
            originalExpressions[rewritten] = expr
            currentLevelAssertedExprs.add(rewritten)
        } else {
            solver.assert(rewritten)
        }

        if (rewriteMode != RewriteMode.EAGER) {
            currentLevelBvAndLemmas.addAll(rewriter.bvAndLemmas(rewritten))
        }
    }

    private fun lemmaCheck(lemma: KExpr<KBoolSort>, model: KModel): Boolean = with(ctx) {
        if (lemma == trueExpr) return true
        val application = rewriter.extractBvAndApplication(lemma) ?: error("Unexpected")

        require(application is KFunctionApp<*> && application.decl == bv2IntContext.bvAndFunc)

        if (model.eval(application) !is KIntNumExpr) return true

        return model.eval(lemma) == trueExpr
    }

    private fun lazyCheck(timeout: Duration): KSolverStatus {
        val start = Date().time
        var left = timeout
        val assumptions = mutableListOf<KExpr<KBoolSort>>()
        var lemmas: List<KExpr<KBoolSort>> = currentLevelBvAndLemmas

        while (left.isPositive()) {
            val status = solver.checkWithAssumptions(assumptions, left)
            if (status != KSolverStatus.SAT) return status

            val model = solver.model()

            val (satisfied, unsatisfied) = lemmas.partition {
                lemmaCheck(it, model)
            }

            if (unsatisfied.isEmpty()) return KSolverStatus.SAT

            lemmas = satisfied
            assumptions.addAll(unsatisfied)

            left = timeout - (Date().time - start).milliseconds
        }

        return KSolverStatus.UNKNOWN
    }

    private fun signedCheck(timeout: Duration): KSolverStatus {
        lastUsedRewriter = rewriter
        val start = Date().time
        var left = timeout

        while (left.isPositive()) {
            val status = solver.checkWithAssumptions(currentLevelAssertedExprs, left)
            left = timeout - (Date().time - start).milliseconds

            when {
                status == KSolverStatus.UNKNOWN -> return KSolverStatus.UNKNOWN
                status == KSolverStatus.SAT && signednessMode == SignednessMode.SIGNED_NO_OVERFLOW ->
                    return KSolverStatus.SAT
                status == KSolverStatus.UNSAT -> {
                    lastUsedRewriter = unsignedRewriter
                    val unsignedExpressions = currentLevelAssertedExprs.map {
                        unsignedRewriter.rewriteBv2Int(originalExpressions[it] ?: error("Unexpected"))
                    }

                    return if (left.isPositive()) {
                        solver.checkWithAssumptions(unsignedExpressions, left)
                    } else {
                        KSolverStatus.UNKNOWN
                    }
                }
            }

            val model = solver.model()

            var correctModel = true
            val exprs = currentLevelAssertedExprs.map { expr ->
                KBv2IntOverflowChecker.overflowCheck(expr, model, rewriter).also { transformed ->
                    originalExpressions[transformed] = originalExpressions[expr] ?: error("Unexpected")
                    correctModel = correctModel && transformed == expr
                }
            }

            if (correctModel) return status

            currentLevelAssertedExprs = exprs.toMutableList()
        }

        return KSolverStatus.UNKNOWN
    }

    override fun check(timeout: Duration): KSolverStatus {
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