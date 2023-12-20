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
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
)
class KZ3LazySumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.SIGNED
)
class KCvc5LazySumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
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
class KYicesLazySumUnsigned(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED
)
class KZ3LazySumUnsigned(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED
)
class KCvc5LazySumUnsigned(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.LAZY,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED
)
class KYicesEagerSumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
)
class KZ3EagerSumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.SIGNED
)
class KCvc5EagerSumSignedLazyOverflow(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
)
class KZ3EagerSumSigned(
    ctx: KContext
) : KBv2IntSolver<KZ3SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KZ3Solver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED
)
class KYicesEagerSumUnsigned(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED
)
class KCvc5EagerSumUnsigned(
    ctx: KContext
) : KBv2IntSolver<KCvc5SolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KCvc5Solver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED
)

class KYicesEagerSumSignedLazyOverflow1stRound(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
    true
)

class KYicesEagerSumSignedLazyOverflowOriginalUnsat(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx = ctx,
    solver = KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    rewriteMode = KBv2IntRewriter.RewriteMode.LAZY,
    andRewriteMode = KBv2IntRewriter.AndRewriteMode.SUM,
    signednessMode = KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    unsatSignednessMode = null,
)

class KYicesEagerSumSignedLazyOverflowOriginalUnsatSplit(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx = ctx,
    solver = KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    rewriteMode = KBv2IntRewriter.RewriteMode.EAGER,
    andRewriteMode = KBv2IntRewriter.AndRewriteMode.SUM,
    signednessMode = KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    isSplitterOn = true,
    unsatSignednessMode = null,
)

class KYicesEagerSumSignedLazyOverflow1stRoundSplit(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
    true
)

class KYicesEagerSumSignedLazyOverflowSplit(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
    isSplitterOn = true
)

class KYicesEagerSumUnsignedSplit(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    KBv2IntRewriter.RewriteMode.EAGER,
    KBv2IntRewriter.AndRewriteMode.SUM,
    KBv2IntRewriter.SignednessMode.UNSIGNED,
    isSplitterOn = true
)