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
import io.ksmt.test.RandomExpressionGenerator
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

    private fun KContext.testInt2BvModelConversion(exprs: List<KExpr<KBoolSort>>) {
        var satCnt = 0
        var timeoutCnt = 0

        val rewriter = KBv2IntRewriter(this, KBv2IntRewriter.RewriteMode.SUM)

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

                    assert(model.eval(expr, true) == trueExpr)
                }
            }
        }

        println("${satCnt.toDouble() / exprs.size} SAT percentage")
        println("${timeoutCnt.toDouble() / exprs.size} timeout percentage")
    }

    private fun KContext.testBv2IntModelConversion(exprs: List<KExpr<KBoolSort>>) {
        var satCnt = 0
        var timeoutCnt = 0

        val rewriter = KBv2IntRewriter(this, KBv2IntRewriter.RewriteMode.SUM)

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

    private fun KContext.generateRandomExpressions(
        size: Int,
        batchSize: Int,
        params: GenerationParameters,
        random: Random,
        weights: Map<String, Double> = mapOf()
    ): List<KExpr<KBoolSort>> = List(size) {
        RandomExpressionGenerator().generate(
            batchSize,
            this,
            params = params,
            random = random,
            generatorFilter = ::bv2IntBenchmarkGeneratorFilter,
            weights = weights
        ).last { it.sort is KBoolSort }
    }.uncheckedCast()

    private fun KExpr<*>.depth(): Int {
        val expr = this

        if (expr is KQuantifier) return 1 + expr.body.depth()

        if (expr !is KApp<*, *> || expr.args.isEmpty()) return 1

        return expr.args.map { it.depth() }.max() + 1
    }

    @Test
    fun testInt2BvModelConversion() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 10,
            possibleIntValues = 2..32,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter()
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableArray(100.0)
            .build()
        val expressions = generateRandomExpressions(
            size = 2000,
            batchSize = 400,
            params = params,
            random = Random(3),
            weights = weights
        )

        testInt2BvModelConversion(expressions)
    }

    @Test
    fun testBv2IntModelConversion() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val params = GenerationParameters(
            seedExpressionsPerSort = 10,
            possibleIntValues = 2..32,
            deepExpressionProbability = 0.2,
            generatedListSize = 2..3,
            astFilter = Bv2IntAstFilter()
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableArray(100.0)
            .build()
        val expressions = generateRandomExpressions(
            size = 5000,
            batchSize = 500,
            params = params,
            random = Random(3),
            weights = weights
        )

        testBv2IntModelConversion(expressions)
    }

}
