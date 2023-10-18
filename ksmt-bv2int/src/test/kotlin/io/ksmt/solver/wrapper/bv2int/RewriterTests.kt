package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KConstDecl
import io.ksmt.expr.KApp
import io.ksmt.expr.KBvSignedLessOrEqualExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KQuantifier
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KSort
import io.ksmt.test.GenerationParameters
import io.ksmt.utils.getValue
import io.ksmt.utils.mkConst
import io.ksmt.utils.uncheckedCast
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RewriterTests {

    private fun KExpr<*>.tryUnwrap(signedness: Signedness): KExpr<KSort> =
        if (this is KBv2IntRewriter.KBv2IntAuxExpr) {
            normalized(signedness)
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
        rewriteMode: KBv2IntRewriter.RewriteMode,
        andRewriteMode: KBv2IntRewriter.AndRewriteMode,
        signednessMode: KBv2IntRewriter.SignednessMode = KBv2IntRewriter.SignednessMode.UNSIGNED
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

                    KBv2IntSolver(this, solver, rewriteMode, andRewriteMode, signednessMode).use { bv2intSolver ->
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
            astFilter = Bv2IntAstFilter(),
        )
        val weights = Bv2IntBenchmarkWeightBuilder()
            .enableBvCmp(5.0)
            .enableBvLia(10.0)
            .enableBvShift(2.0)
//            .enableBvWeird(2.0)
            .enableBvNia(2.0)
            .enableBvBitwise(2.0)
            .setWeight("mkBvAndExpr", 0.4)
            .build()
        val expressions = generateRandomExpressions(
            size = 10000,
            batchSize = 200,
            params = params,
            random = Random(49),
            weights = weights,
            isVerbose = false,
        )

        testInt2BvModelConversion(
            expressions,
            KBv2IntRewriter.RewriteMode.LAZY,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
        )
    }

    fun KContext.testing(expr: KExpr<KBoolSort>) {

        val kVal = mkBv(-1, 64u)
        val sVal = mkBv(2199023255554, 64u)
        val yVal = mkBv(-4398046511105, 64u)

        val k = bv64Sort.mkConst("kjmfxzw")
        val s = bv64Sort.mkConst("sldjlemuku")
        val y = bv64Sort.mkConst("yybilyil")

        val restrictions = mkAnd(
            kVal eq k.uncheckedCast(),
            sVal eq s.uncheckedCast(),
            yVal eq y.uncheckedCast(),
        )

        /**
         * (sldjlemuku, #x0000020000000002)
         * (kjmfxzw, #xffffffffffffffff)
         * (yybilyil, #xfffffbffffffffff)
         */

        KBv2IntSolver(
            this,
            KZ3Solver(this),
            KBv2IntRewriter.RewriteMode.EAGER,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
        ).use { solver ->
            solver.assert(expr)
            solver.assert(restrictions)
            println(solver.check())
//            val model = solver.model()
//
//            println(model.eval(subExpr))
        }


        KZ3Solver(this).use { solver ->
            solver.assert(expr)
            solver.assert(restrictions)
            println(solver.check())
            val model = solver.model()

//            println(model.eval(subExpr))
//            println(mkBv2IntExpr(model.eval(ashr), true))
        }
    }

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
            .enableBvNia(2.0)
            .enableBvBitwise(2.0)
            .setWeight("mkBvAndExpr", 0.4)
            .build()
        val expressions = generateRandomExpressions(
            size = 10000,
            batchSize = 200,
            params = params,
            random = Random(47),
            weights = weights,
            isVerbose = false,
        )

//        return testing(sublist.single())

        testBv2IntModelConversion(
            expressions,
            KBv2IntRewriter.RewriteMode.LAZY,
            KBv2IntRewriter.AndRewriteMode.SUM,
            KBv2IntRewriter.SignednessMode.UNSIGNED
        )
    }
}
