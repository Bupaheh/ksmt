package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.getValue
import io.ksmt.utils.mkConst
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class IncrementalApiTest {
    private val ctx = KContext()
    private val solver = KBv2IntSolver(
        ctx,
        KZ3Solver(ctx),
        KBv2IntRewriterConfig(signednessMode = KBv2IntRewriter.SignednessMode.SIGNED)
    )

    @Test
    fun testUnsatCoreGeneration(): Unit = with(ctx) {
        val a = boolSort.mkConst("a")
        val b = boolSort.mkConst("b")
        val c = boolSort.mkConst("c")

        val e1 = (a and b) or c
        val e2 = !(a and b)
        val e3 = !c

        solver.assert(e1)
        solver.assertAndTrack(e2)
        val status = solver.checkWithAssumptions(listOf(e3))
        assertEquals(KSolverStatus.UNSAT, status)
        val core = solver.unsatCore()
        assertEquals(2, core.size)
        assertTrue(e2 in core)
        assertTrue(e3 in core)
    }

    @Test
    fun testUnsatCoreGenerationNoAssumptions(): Unit = with(ctx) {
        val a = boolSort.mkConst("a")
        val b = boolSort.mkConst("b")

        val e1 = (a and b)
        val e2 = !(a and b)

        solver.assert(e1)
        solver.assertAndTrack(e2)
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
        val core = solver.unsatCore()
        assertEquals(1, core.size)
        assertTrue(e2 in core)
    }

    @Test
    fun testPushPop(): Unit = with(ctx) {
        val a = boolSort.mkConst("a")
        solver.assert(a)
        solver.push()
        solver.assertAndTrack(!a)
        var status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
        val core = solver.unsatCore()
        assertEquals(1, core.size)
        assertTrue(!a in core)
        solver.pop()
        status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun breakZ3(): Unit = with(KContext()) {
        KZ3Solver(this).use { solver ->
            val s = mkUninterpretedSort("temp")
            val a = mkUninterpretedSortValue(s, 0)
            val b = s.mkConst("b")

            solver.push()
            solver.assert(a eq b)
            solver.pop()
            solver.push()
            solver.push()
            solver.checkWithAssumptions(listOf(a eq b))
        }
    }

    @Test
    fun breakZ32(): Unit = with(KContext()) {
        KZ3Solver(this).use { solver ->
            val s = mkUninterpretedSort("temp")
            val a = mkUninterpretedSortValue(s, 0)
            val b = s.mkConst("b")

            solver.push()
            solver.checkWithAssumptions(listOf(a eq b))
            solver.checkWithAssumptions(listOf(a eq b))
            solver.push()
            solver.checkWithAssumptions(listOf(a eq b))
            solver.checkWithAssumptions(listOf(a eq b))
            solver.pop()
            solver.checkWithAssumptions(listOf(a eq b))
        }
    }

    @Test
    fun breakYices(): Unit = with(KContext()) {
        KYicesSolver(this).use { solver ->
            val s = mkUninterpretedSort("temp")
            val a = mkUninterpretedSortValue(s, 0)
            val b = s.mkConst("b")

            solver.checkWithAssumptions(listOf(a eq b))
        }
    }

    @Test
    fun benchmarkT() = with(KContext()) {
        val expressions = readSerializedFormulasUsvm(
            File("generatedExpressions/usvm-owasp2"),
            3,
            5
        ).map { it.second.filter { TempVisitor(ctx).visit(it) } }

        expressions.forEach {
            benchmarkTest(
                expressions = it,
                random = Random(1),
                timeout = 1.seconds,
                oracleSolverProvider = { KZ3Solver(this) },
                solverProvider = {
                    KZ3Solver(this)
//                    KBv2IntSolver(
//                        this,
//                        KZ3Solver(this),
//                        KBv2IntRewriterConfig(signednessMode = KBv2IntRewriter.SignednessMode.SIGNED)
//                    )
                }
            )
        }

    }

    private fun KContext.benchmarkTest(
        expressions: List<KExpr<KBoolSort>>,
        random: Random,
        timeout: Duration,
        oracleSolverProvider: (KContext) -> KSolver<*>,
        solverProvider: (KContext) -> KSolver<*>,
    ) = SolversWrapper(this, oracleSolverProvider, solverProvider).use { solvers ->
        val currentExpression: MutableList<KExpr<KBoolSort>> = expressions.toMutableList()

        val ops = mutableListOf(3, 1, 3)

        while (currentExpression.isNotEmpty()) {
            val op = random.nextInt(0, 4)

            println(op)

            when (op) {
//                0 -> solvers.assert(currentExpression.first())
                0 -> solvers.assertAndTrack(currentExpression.removeFirst())
//                2 -> validateCheck(solvers) { solvers.check(timeout) }
                3 -> validateCheck(solvers) { solvers.checkWithAssumptions(currentExpression.first(), timeout) }
                1 -> {
                    solvers.push()
                    validateCheck(solvers) { solvers.checkWithAssumptions(trueExpr, timeout) }
                }
                2 -> {
                    solvers.pop()
                    validateCheck(solvers) { solvers.checkWithAssumptions(trueExpr, timeout) }
                }
            }
        }
    }

    private inline fun validateCheck(
        solvers: SolversWrapper,
        check: () -> Pair<KSolverStatus, KSolverStatus>,
    ) {
        val (oracleStatus, solverStatus) = try {
            check()
        } catch (e: Throwable) {
            println(e)
            throw e
            return
        }

        if (oracleStatus == KSolverStatus.UNKNOWN || solverStatus == KSolverStatus.UNKNOWN) return

        assertEquals(oracleStatus, solverStatus)

        if (solverStatus == KSolverStatus.SAT) return

        val (oracleUnsatCore, unsatCore) = solvers.unsatCore()

        assertEquals(oracleUnsatCore.toSet(), unsatCore.toSet())

        solvers.pop()
    }

    private class SolversWrapper(
        ctx: KContext,
        oracleSolverProvider: (KContext) -> KSolver<*>,
        solverProvider: (KContext) -> KSolver<*>,
    ) : AutoCloseable {
        private val oracle = oracleSolverProvider(ctx)
        private val solver = solverProvider(ctx)

        private var currentLevel = 1
        private val trackedAssertionsStack: MutableList<MutableList<KExpr<KBoolSort>>> = mutableListOf(mutableListOf())
        private var lastAssumptions: List<KExpr<KBoolSort>> = emptyList()

        val assumptions
            get() = lastAssumptions

        val trackedAssertions
            get() = trackedAssertionsStack.flatten()

        init {
            opWrapper { it.checkWithAssumptions(listOf(ctx.trueExpr)) }
            opWrapper { it.push() }
            opWrapper { it.checkWithAssumptions(listOf(ctx.trueExpr)) }
        }

        private inline fun <T> opWrapper(op: (KSolver<*>) -> T): Pair<T, T> {
            val solverResult = op(solver)
            val oracleResult = op(oracle)

            return oracleResult to solverResult
        }

        fun assert(expr: KExpr<KBoolSort>) = opWrapper { it.assert(expr) }
        fun assertAndTrack(expr: KExpr<KBoolSort>) {
            trackedAssertionsStack.last().add(expr)
            opWrapper { it.assertAndTrack(expr) }
        }
        fun check(timeout: Duration) = checkWithAssumptions(emptyList(), timeout)
        fun checkWithAssumptions(
            exprs: List<KExpr<KBoolSort>>,
            timeout: Duration
        ): Pair<KSolverStatus, KSolverStatus> {
            lastAssumptions = exprs
            return opWrapper { it.checkWithAssumptions(exprs, timeout) }
        }
        fun checkWithAssumptions(
            expr: KExpr<KBoolSort>,
            timeout: Duration
        ): Pair<KSolverStatus, KSolverStatus> {
            return checkWithAssumptions(listOf(expr), timeout)
        }
        fun push() {
            currentLevel++
            trackedAssertionsStack.add(mutableListOf())
            opWrapper { it.push() }
        }

        fun pop() {
            currentLevel--
            opWrapper { it.pop() }
            trackedAssertionsStack.removeLast()

            if (currentLevel == 0) {
                push()
            }
        }

        fun unsatCore() = opWrapper { it.unsatCore() }

        override fun close() { opWrapper { it.close() } }
    }
}