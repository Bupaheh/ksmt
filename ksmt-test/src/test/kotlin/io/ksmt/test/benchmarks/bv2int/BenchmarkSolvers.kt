package io.ksmt.test.benchmarks.bv2int

import io.ksmt.KContext
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.cvc5.KCvc5SolverConfiguration
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter
import io.ksmt.solver.wrapper.bv2int.KBv2IntSolver
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.solver.z3.KZ3SolverConfiguration

class KYicesSolverBench(ctx: KContext) : KBenchmarkSolverWrapper<KYicesSolverConfiguration>(ctx, KYicesSolver(ctx))
class KZ3SolverBench(ctx: KContext) : KBenchmarkSolverWrapper<KZ3SolverConfiguration>(ctx, KZ3Solver(ctx))
class KCvc5SolverBench(ctx: KContext) : KBenchmarkSolverWrapper<KCvc5SolverConfiguration>(ctx, KCvc5Solver(ctx))
class KYicesLazySumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
)
class KZ3LazySumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
)
class KCvc5LazySumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
)
class KYicesLazySumSigned(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED
)
class KZ3LazySumSigned(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED
)
class KCvc5LazySumSigned(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED
)