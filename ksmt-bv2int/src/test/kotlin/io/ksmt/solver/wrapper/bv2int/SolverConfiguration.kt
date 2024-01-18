package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.solver.KSolver
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.cvc5.KCvc5SolverUniversalConfiguration
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.utils.mkConst
import kotlin.time.Duration.Companion.seconds

class SolverConfiguration(
    private val ctx: KContext,
    private val solver: InnerSolver,
    private val rewriteMode: KBv2IntRewriter.RewriteMode? = null,
    private val andRewriteMode: KBv2IntRewriter.AndRewriteMode = KBv2IntRewriter.AndRewriteMode.SUM,
    private val signednessMode: KBv2IntRewriter.SignednessMode = KBv2IntRewriter.SignednessMode.UNSIGNED,
    private val unsignedMode: KBv2IntRewriter.SignednessMode? = KBv2IntRewriter.SignednessMode.UNSIGNED,
) {
    enum class InnerSolver {
        Z3,
        CVC5,
        Yices,
        Bitwuzla;

        fun construct(ctx: KContext) =
            when (this) {
                Z3 -> KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx))
                CVC5 -> manager.createSolver(ctx, KCvc5Solver::class)
                Yices -> KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx))
                Yices -> manager.createSolver(ctx, KYicesSolver::class)
                Bitwuzla -> KBitwuzlaSolver(ctx)
            }

        override fun toString(): String =
            when (this) {
                Z3 -> "Z3"
                CVC5 -> "cvc5"
                Yices -> "Yices"
                Bitwuzla -> "Bitwuzla"
            }
    }

    private lateinit var constructedSolver: KSolver<*>

    fun initSolver() {
        constructedSolver = if (rewriteMode == null) {
            solver.construct(ctx)
        } else {
            KBv2IntCustomSolver(ctx)
        }
    }

    fun closeSolver() {
        constructedSolver.close()
    }

    fun construct(ctx: KContext): KSolver<*> = with(ctx) {
        return@with constructedSolver


        val result = if (rewriteMode == null) {
            solver.construct(ctx)
        } else {
//            return@with
            KBv2IntCustomSolver(ctx)
//            manager.createSolver(ctx, KBv2IntCustomSolver::class)
        }

        result
    }

    override fun toString(): String {
        val innerSolver = solver.toString()
        if (rewriteMode == null) return innerSolver

        val prefix = when (rewriteMode) {
            KBv2IntRewriter.RewriteMode.EAGER -> "Eager-"
            KBv2IntRewriter.RewriteMode.LAZY -> "Lazy-"
        }

        var suffix =  when (andRewriteMode) {
            KBv2IntRewriter.AndRewriteMode.SUM -> "-Sum"
            KBv2IntRewriter.AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (signednessMode) {
            KBv2IntRewriter.SignednessMode.UNSIGNED -> ""
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflow"
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBounds"
            KBv2IntRewriter.SignednessMode.SIGNED -> "-Signed"
        }

        if (signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW) {
            suffix += when (unsignedMode) {
                KBv2IntRewriter.SignednessMode.UNSIGNED -> ""
                KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflow"
                KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBounds"
                KBv2IntRewriter.SignednessMode.SIGNED -> "-Signed"
                null -> "-OriginalUnsat"
            }
        }

        return prefix + innerSolver + suffix
    }

    companion object {
        val manager = KSolverRunnerManager(hardTimeout = 5.seconds, workerProcessIdleTimeout = 40.seconds)

        init {
//            manager.registerSolver(KBv2IntCustomSolver::class, KYicesSolverUniversalConfiguration::class)
        }
    }
}
