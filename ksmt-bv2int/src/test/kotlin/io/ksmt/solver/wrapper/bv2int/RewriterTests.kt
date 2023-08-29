package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KConstDecl
import io.ksmt.expr.KApp
import io.ksmt.expr.KDivArithExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpMaxExpr
import io.ksmt.expr.KFpMinExpr
import io.ksmt.expr.KFpToBvExpr
import io.ksmt.expr.KFpToRealExpr
import io.ksmt.expr.KFpValue
import io.ksmt.expr.KModIntExpr
import io.ksmt.expr.KPowerArithExpr
import io.ksmt.expr.KQuantifier
import io.ksmt.expr.KRemIntExpr
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KArithSort
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KFpSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KRealSort
import io.ksmt.sort.KSort
import io.ksmt.test.GenerationParameters
import io.ksmt.utils.FpUtils.isZero
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

    private fun KContext.testInt2BvModelConversion(
        exprs: List<KExpr<KBoolSort>>,
        rewriteMode: KBv2IntRewriter.RewriteMode,
        andRewriteMode: KBv2IntRewriter.AndRewriteMode,
        signedness: KBv2IntRewriter.SignednessMode = KBv2IntRewriter.SignednessMode.UNSIGNED
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
                        rewriteMode,
                        andRewriteMode,
                        signedness
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
        mode: KBv2IntRewriter.AndRewriteMode,
        signednessMode: KBv2IntRewriter.SignednessMode
    ) {
        var satCnt = 0
        var timeoutCnt = 0

        val bv2intContext = KBv2IntContext(this)
        val rewriter = KBv2IntRewriter(this, bv2intContext, KBv2IntRewriter.RewriteMode.EAGER, mode)

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
    fun testInt2BvModelConversion() = with(KContext()) {
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
//            .enableBvNia(0.65)
//            .enableBvWeird(2.0)
//            .enableBvBitwise(3.5)
            .build()
        val expressions = generateRandomExpressions(
            size = 5000,
            batchSize = 500,
            params = params,
            random = Random(10),
            weights = weights,
            isVerbose = true
        )
//        { expr ->
//            val decls = KDeclCounter(this).countDeclarations(expr)
//            countOperations(decls, bitwiseDecls) > 0
//        }

        testInt2BvModelConversion(
            expressions,
            KBv2IntRewriter.RewriteMode.EAGER,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.UNSIGNED
        )
    }

    fun KContext.testing(expr: KExpr<KBoolSort>) {
        KZ3Solver(this).use { solver ->
           solver.assert(expr)
           val status = solver.check(1.seconds)
           if (status != KSolverStatus.SAT) return
        }

        KBv2IntSolver(
            this,
            KZ3Solver(this),
            KBv2IntRewriter.RewriteMode.EAGER,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.UNSIGNED
        ).use { solver ->
            solver.assert(expr)
            if (solver.check(1.seconds) == KSolverStatus.UNSAT) {
                error(10)
            }
        }
    }

    @Test
    fun testBv2IntModelConversion() = with(KContext()) {
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
//            .enableBvNia(0.65)
            .enableBvWeird(2.0)
//            .enableBvBitwise(3.5)
            .build()
        val expressions = generateRandomExpressions(
            size = 15,
            batchSize = 500,
            params = params,
            random = Random(11),
            weights = weights,
            isVerbose = true
        )

        testBv2IntModelConversion(
            expressions,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.UNSIGNED
        )
    }

}
