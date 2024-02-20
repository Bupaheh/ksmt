package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KConstDecl
import io.ksmt.expr.KExpr
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KSort
import io.ksmt.test.GenerationParameters
import io.ksmt.utils.mkConst
import io.ksmt.utils.uncheckedCast
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RewriterTests {
    private fun KContext.testInt2BvModelConversion(
        exprs: List<KExpr<KBoolSort>>,
        rewriterConfig: KBv2IntRewriterConfig,
        equisatisfiableConfig: KBv2IntRewriterConfig,
    ) {
        var satCnt = 0
        var timeoutCnt = 0

        KSolverRunnerManager().use { solverManager ->
            exprs.forEachIndexed { i, expr ->
                println("$i/${exprs.size - 1}")

                solverManager.createSolver(this, KZ3Solver::class).use { innerSolver ->
                    val solver = KBv2IntSolver(
                        this,
                        innerSolver,
                        rewriterConfig,
                        equisatisfiableConfig,
                    )
                    solver.assert(expr)

                    val status = solver.check(2.seconds)

                    if (status == KSolverStatus.UNKNOWN) {
                        println("Timeout")
                        timeoutCnt++
                    }

                    if (status != KSolverStatus.SAT) return@forEachIndexed

                    satCnt++

                    val model = solver.model()
                    val simplified =  model.eval(expr, true)

                    assert(simplified != falseExpr)
                }
            }
        }

        println("${satCnt.toDouble() / exprs.size} SAT percentage")
        println("${timeoutCnt.toDouble() / exprs.size} timeout percentage")
    }

    private fun KContext.testBv2IntModelConversion(
        exprs: List<KExpr<KBoolSort>>,
        rewriterConfig: KBv2IntRewriterConfig,
        equisatisfiableConfig: KBv2IntRewriterConfig,
    ) {
        var satCnt = 0
        var timeoutCnt = 0

        KSolverRunnerManager().use { solverManager ->
            exprs.forEachIndexed { i, expr ->
                println("$i/${exprs.size - 1}")

                solverManager.createSolver(this, KZ3Solver::class).use { solver ->
                    solver.push()
                    solver.assert(expr)

                    var status = solver.check(3.seconds)

                    if (status == KSolverStatus.UNKNOWN) {
                        println("Timeout")
                        timeoutCnt++
                    }

                    if (status != KSolverStatus.SAT) return@forEachIndexed

                    satCnt++

                    val model = solver.model().detach()

                    solver.pop()

                    val restrictions = KExprUninterpretedDeclCollector.collectUninterpretedDeclarations(expr)
                        .filterIsInstance<KConstDecl<*>>()
                        .mapNotNull { decl ->
                            val interpretation = model.interpretation(decl) ?: return@mapNotNull null
                            val default = interpretation.default ?: return@mapNotNull null

                            if (interpretation.entries.isNotEmpty()) return@mapNotNull null

//                            println(decl.apply() to default)
                            decl.apply().uncheckedCast<_, KExpr<KSort>>() eq default.uncheckedCast()
                        }.let { mkAnd(it) }

                    KBv2IntSolver(this, solver, rewriterConfig, equisatisfiableConfig).use { bv2intSolver ->
                        bv2intSolver.assert(expr and restrictions)
                        status = bv2intSolver.check(3.seconds)
                    }

                    if (status == KSolverStatus.UNKNOWN) {
                        println("Timeout rewrite")
                        timeoutCnt++
                    }

                    if (status == KSolverStatus.UNSAT) {
                        println("Check error")
                        println(i)
                    }

                    assert(status != KSolverStatus.UNSAT)
                }
            }
        }

        println("${satCnt.toDouble() / exprs.size} SAT percentage")
        println("${timeoutCnt.toDouble() / exprs.size} timeout percentage")
    }

    fun KContext.testing(expr: KExpr<KBoolSort>) {
        val n = bv32Sort.mkConst("nhseznl")
        val f = bv32Sort.mkConst("frhjnciiz")
        val s = bv32Sort.mkConst("snssgvwzvj")

        val fValue = mkBv(1)
        val nValue = mkBv(2)
        val sValue = mkBv(31)

        val shl = mkBvShiftLeftExpr(n, s)
        val neg = mkBvNegationExpr(f)
        val ule = mkBvUnsignedLessExpr(shl, neg)
        val not = mkNot(ule)

        println(mkBvShiftLeftExpr(nValue, sValue))

        val subexpr = neg

        val constraints = mkAnd(
            f eq fValue,
            s eq sValue,
            n eq nValue
        )

        KBv2IntSolver(
            this,
            KZ3Solver(this),
            KBv2IntRewriterConfig(
                KBv2IntRewriter.RewriteMode.LAZY,
                KBv2IntRewriter.AndRewriteMode.SUM,
                KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
            ),
            KBv2IntRewriterConfig(disableRewriting = true)
        ).use { solver ->
            solver.assert(expr)
            solver.assert(constraints)
            println(solver.check())
            val model = solver.model()

            println(model.eval(subexpr))
        }

        KZ3Solver(this).use { solver ->
            solver.assert(not or constraints)

            println(solver.check())

            val model = solver.model()

            println(model.eval(subexpr))
        }
    }

    @Ignore
    @Test
    fun testInt2BvModelConversion() = with(KContext()) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 20,
            possibleIntValues = 2..64,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter(),
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableBvCmp(5.0)
            .enableBvLia(10.0)
            .enableBvShift(2.0)
            .enableBvWeird(2.0)
//            .enableBvNia(2.0)
//            .enableBvBitwise(2.0)
//            .setWeight("mkBvAndExpr", 0.4)
            .build()
        val expressions = generateRandomExpressions(
            size = 10000,
            batchSize = 300,
            params = params,
            random = Random(53),
            weights = weights,
            isVerbose = false,
        )

        testInt2BvModelConversion(
            expressions,
            KBv2IntRewriterConfig(
                KBv2IntRewriter.RewriteMode.LAZY,
                KBv2IntRewriter.AndRewriteMode.SUM,
                KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
            ),
            KBv2IntRewriterConfig(disableRewriting = true)
        )
    }

    @Ignore
    @Test
    fun testBv2IntModelConversion() = with(KContext()) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 20,
            possibleIntValues = 2..64,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter(),
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableBvCmp(5.0)
            .enableBvLia(10.0)
            .enableBvShift(2.0)
//            .enableBvWeird(2.0)
//            .enableBvNia(2.0)
            .enableBvBitwise(2.0)
//            .setWeight("mkBvAndExpr", 0.4)
            .build()
        val expressions = generateRandomExpressions(
            size = 10000,
            batchSize = 300,
            params = params,
            random = Random(53),
            weights = weights,
            isVerbose = false,
        )

        testBv2IntModelConversion(
            expressions,
            KBv2IntRewriterConfig(
                KBv2IntRewriter.RewriteMode.LAZY,
                KBv2IntRewriter.AndRewriteMode.SUM,
                KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
            ),
            KBv2IntRewriterConfig(disableRewriting = true)
        )
    }
}
