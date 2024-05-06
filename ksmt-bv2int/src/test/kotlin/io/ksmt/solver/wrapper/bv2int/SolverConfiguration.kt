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
    private val rewriterConfig: KBv2IntRewriterConfig = KBv2IntRewriterConfig(),
    private val equisatisfiableConfig: KBv2IntRewriterConfig = KBv2IntRewriterConfig(disableRewriting = true),
    private val isOriginalSolver: Boolean = false,
) {
    enum class InnerSolver {
        Z3,
        CVC5,
        Yices,
        Bitwuzla;

        fun construct(ctx: KContext) =
            when (this) {
                Z3 -> KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx))
                CVC5 -> KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx))
                Yices -> KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx))
                Bitwuzla -> KBenchmarkSolverWrapper(ctx, KBitwuzlaSolver(ctx))
            }

        override fun toString(): String =
            when (this) {
                Z3 -> "Z3"
                CVC5 -> "cvc5"
                Yices -> "Yices"
//                Yices -> "YicesMCSAT"
                Bitwuzla -> "Bitwuzla"
            }
    }

    private lateinit var constructedSolver: KSolver<*>

    fun initSolver() {
        constructedSolver = if (isOriginalSolver) {
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


        val result = if (isOriginalSolver) {
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
        if (isOriginalSolver) return innerSolver

        val prefix = when (rewriterConfig.rewriteMode) {
            KBv2IntRewriter.RewriteMode.EAGER -> "Eager-"
            KBv2IntRewriter.RewriteMode.LAZY -> "Lazy-"
        }

        var suffix =  when (rewriterConfig.andRewriteMode) {
            KBv2IntRewriter.AndRewriteMode.SUM -> "-Sum"
            KBv2IntRewriter.AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (rewriterConfig.signednessMode) {
            KBv2IntRewriter.SignednessMode.UNSIGNED -> ""
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflow"
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBounds"
            KBv2IntRewriter.SignednessMode.SIGNED -> "-Signed"
        }

        if (rewriterConfig.signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW) {
            suffix += when {
                equisatisfiableConfig.disableRewriting -> "-OriginalUnsat"
                equisatisfiableConfig.signednessMode == KBv2IntRewriter.SignednessMode.UNSIGNED -> ""
                equisatisfiableConfig.signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflow"
                equisatisfiableConfig.signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBounds"
                equisatisfiableConfig.signednessMode == KBv2IntRewriter.SignednessMode.SIGNED -> "-Signed"
                else -> error("Unexpected")
            }
        }

        return prefix + innerSolver + suffix
    }
}
