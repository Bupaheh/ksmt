package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KConstDecl
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.expr.KQuantifier
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KSort
import io.ksmt.test.GenerationParameters
import io.ksmt.utils.uncheckedCast
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RewriterTests {

    private fun KExpr<*>.tryUnwrap(): KExpr<KSort> =
        if (this is KBv2IntRewriter.KBv2IntAuxExpr) {
            normalized
        } else {
            this
        }.uncheckedCast()

    private fun KContext.testInt2BvModelConversion(exprs: List<KExpr<KBoolSort>>, mode: KBv2IntRewriter.AndRewriteMode) {
        var satCnt = 0
        var timeoutCnt = 0

        val rewriter = KBv2IntRewriter(this, KBv2IntRewriter.RewriteMode.EAGER, mode)

        KSolverRunnerManager().use { solverManager ->
            exprs.forEachIndexed { i, expr ->
                println("$i/${exprs.size - 1}")

                val transformedExpr = rewriter.rewriteBv2Int(expr.uncheckedCast())

                solverManager.createSolver(this, KZ3Solver::class).use { solver ->
                    solver.assert(transformedExpr)

                    val status = solver.check(3.seconds)

                    if (status == KSolverStatus.UNKNOWN) {
                        println("Timeout")
                        timeoutCnt++
                    }

                    if (status != KSolverStatus.SAT) return@forEachIndexed

                    satCnt++

                    val model = KBv2IntModel(this, solver.model(), rewriter)
                    val simplified =  model.eval(expr, true)

                    assert(simplified == trueExpr)
                }
            }
        }

        println("${satCnt.toDouble() / exprs.size} SAT percentage")
        println("${timeoutCnt.toDouble() / exprs.size} timeout percentage")
    }

    private fun KContext.testBv2IntModelConversion(exprs: List<KExpr<KBoolSort>>, mode: KBv2IntRewriter.AndRewriteMode) {
        var satCnt = 0
        var timeoutCnt = 0

        val rewriter = KBv2IntRewriter(this, KBv2IntRewriter.RewriteMode.EAGER, mode)

        KSolverRunnerManager().use { solverManager ->
            exprs.forEachIndexed { i, expr ->
                println("$i/${exprs.size - 1}")

                val transformedExpr = rewriter.rewriteBv2Int(expr)

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

                    KExprUninterpretedDeclCollector.collectUninterpretedDeclarations(expr)
                        .filterIsInstance<KConstDecl<*>>()
                        .forEach { decl ->
                            val interpretation = model.interpretation(decl) ?: return@forEach
                            val default = interpretation.default ?: return@forEach
                            val intConst = rewriter.rewriteDecl(decl).apply(listOf())

                            if (interpretation.entries.isNotEmpty()) return@forEach

                            solver.assert(intConst eq rewriter.apply(default).tryUnwrap())
                        }

                    solver.assert(transformedExpr)

                    status = solver.check(3.seconds)

                    if (status == KSolverStatus.UNKNOWN) {
                        println("Timeout")
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

    private fun KExpr<*>.depth(): Int {
        val expr = this

        if (expr is KQuantifier) return 1 + expr.body.depth()

        if (expr !is KApp<*, *> || expr.args.isEmpty()) return 1

        return expr.args.map { it.depth() }.max() + 1
    }

    @Test
    fun testInt2BvModelConversion() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 20,
            possibleIntValues = 2..64,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter()
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableBvCmp(5.0)
            .enableBvLia(10.0)
            .enableBvBitwise(2.0)
            .build()
        val expressions = generateRandomExpressions(
            size = 1000,
            batchSize = 500,
            params = params,
            random = Random(1),
            weights = weights,
            isVerbose = true
        )

        testInt2BvModelConversion(expressions, KBv2IntRewriter.AndRewriteMode.BITWISE)
    }

    @Test
    fun testBv2IntModelConversion() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 20,
            possibleIntValues = 2..64,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter()
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableBvCmp(5.0)
            .enableBvLia(10.0)
            .enableBvBitwise(2.0)
            .build()
        val expressions = generateRandomExpressions(
            size = 1000,
            batchSize = 500,
            params = params,
            random = Random(22),
            weights = weights,
            isVerbose = true
        )

        testBv2IntModelConversion(expressions, KBv2IntRewriter.AndRewriteMode.BITWISE)
    }

}
