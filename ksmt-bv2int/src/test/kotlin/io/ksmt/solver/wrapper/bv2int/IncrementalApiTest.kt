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
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds

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

    private fun genRandomString(len: UInt): String {
        val builder = StringBuilder()

        repeat(len.toInt()) {
            builder.append(Random.nextInt(0, 1))
        }

        return builder.toString()
    }

    @Test
    fun intBlasting() = with(KContext()) {
        val len = 1024u
        val s = mkBvSort(len)

        val a by s
        val b by s

        val aVal = mkBv(genRandomString(len), len)
        val bVal = mkBv(genRandomString(len), len)
        val result = mkBvMulExpr(aVal, bVal)

        val solver = KZ3Solver(this).apply {
            configure {
                setZ3Option("sat.smt", true)
                setZ3Option("smt.bv.solver", 2)
            }
        }
//        val solver = KBv2IntSolver(
//            this,
//            KZ3Solver(this),
//            KBv2IntRewriterConfig(signednessMode = KBv2IntRewriter.SignednessMode.UNSIGNED)
//        )

        solver.assert(mkBvMulExpr(a, b) eq result)

        val time = measureNanoTime {
            val result = solver.checkWithAssumptions(listOf(a eq aVal, b eq bVal))

            println(result)
        }

        println(time.nanoseconds)

        solver.close()
    }

    @Test
    fun benchmarkT() = with(KContext()) {
        val expressions = readSerializedFormulasUsvm(
            File("generatedExpressions/usvm-owasp2"),
            2,
            500
        ).map { it.second.filter { TempVisitor(ctx).visit(it) } }

        expressions.forEach {
            println("start iteration")
            benchmarkTest(
                expressions = it,
                random = Random(2),
                timeout = 1.hours,
                oracleSolverProvider = { KZ3Solver(this) },
                solverProvider = {
                    KBv2IntSolver(
                        this,
                        KZ3Solver(this),
                        KBv2IntRewriterConfig(signednessMode = KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW),
                        KBv2IntRewriterConfig(signednessMode = KBv2IntRewriter.SignednessMode.SIGNED)
                    )
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

        while (currentExpression.isNotEmpty()) {
            val op = random.nextInt(0, 6)

            val opStr = when (op) {
                0 -> "assert"
                1 -> "assertAndTrack"
                2 -> "check"
                3 -> "checkWithAssumptions"
                4 -> "push"
                else -> "pop"
            }
            println("--------\n$opStr\n--------")

            when (op) {
                0 -> solvers.assert(currentExpression.removeFirst())
                1 -> solvers.assertAndTrack(currentExpression.removeFirst())
                2 -> validateCheck(solvers) { solvers.check(timeout) }
                3 -> validateCheck(solvers) { solvers.checkWithAssumptions(currentExpression.removeFirst(), timeout) }
                4 -> solvers.push()
                5 -> solvers.pop()
            }
        }
    }

    private inline fun validateCheck(
        solvers: SolversWrapper,
        check: () -> Pair<KSolverStatus, KSolverStatus>,
    ) {
        val (oracleStatus, solverStatus) = check()
        println("after check")

        if (oracleStatus == KSolverStatus.UNKNOWN || solverStatus == KSolverStatus.UNKNOWN) return

        println(solverStatus)
        if (oracleStatus != solverStatus) {
            val t = 7
        }
        assertEquals(oracleStatus, solverStatus)
//        println(oracleStatus)

        if (solverStatus == KSolverStatus.SAT) return

        val (oracleUnsatCore, unsatCore) = solvers.unsatCore()

        val target = (solvers.assumptions + solvers.trackedAssertions).toSet()

        if (!target.containsAll(unsatCore)) {
            val flag = target.containsAll(oracleUnsatCore)

            error("AHAHAHAH $flag")
        }

//        solvers.pop()
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
            opWrapper { it.push() }
        }

        private inline fun <T> opWrapper(op: (KSolver<*>) -> T): Pair<T, T> {
            println("solver")
            val solverResult = op(solver)
            println("oracle")
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