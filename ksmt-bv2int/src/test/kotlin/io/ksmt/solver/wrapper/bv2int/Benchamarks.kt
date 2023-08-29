package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.utils.mkConst
import java.io.File
import kotlin.math.max
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds

fun KContext.readFormulas(file: File): List<KExpr<KBoolSort>> {
    val srcSerializationCtx = AstSerializationCtx().apply { initCtx(this@readFormulas) }
    val srcMarshaller = AstSerializationCtx.marshaller(srcSerializationCtx)
    val emptyRdSerializationCtx = SerializationCtx(Serializers())
    val buffer = UnsafeBuffer(file.readBytes())
    val expressions: MutableList<KExpr<KBoolSort>> = mutableListOf()

    while (true) {
        try {
            expressions.add(srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
        } catch (_ : Exception) {
            break
        }
    }

    return expressions
}

private data class MeasureAssertTimeResult(val nanoTime: Long, val status: KSolverStatus, val roundCnt: Int) {
    override fun toString(): String = "$status ${nanoTime / 1e3} $roundCnt"
}

enum class TimerMode {
    CHECK_TIME,
    ASSERT_TIME;

    override fun toString(): String =
        when (this) {
            CHECK_TIME -> "CT"
            ASSERT_TIME -> "AT"
        }
}

private fun KContext.measureAssertTime(
    expr: KExpr<KBoolSort>,
    solverDescription: Solver,
    timerMode: TimerMode
): MeasureAssertTimeResult {
    var status: KSolverStatus
    val solver = solverDescription.construct(this)

    solver.resetCheckTime()
    val assertTime = measureNanoTime {
        solver.assert(expr)
        status = solver.check(1.seconds)
    }

    val resTime = when (timerMode) {
        TimerMode.CHECK_TIME -> solver.getCheckTime()
        TimerMode.ASSERT_TIME -> assertTime
    }

    val roundCnt = if (solver is KBv2IntSolver) solver.roundCount else 1

    solver.close()

    return MeasureAssertTimeResult(resTime, status, roundCnt)
}

private fun KContext.runBenchmark(
    outputFile: File,
    solver: Solver,
    expressions: List<Pair<Int, KExpr<KBoolSort>>>,
    repeatNum: Int,
    timerMode: TimerMode
) {
    expressions.forEach { (exprId, expr) ->
        repeat(repeatNum) { repeatIdx ->
            println("$exprId\t$repeatIdx")

            val res = measureAssertTime(expr, solver, timerMode)

            println(res)
            outputFile.appendText("$exprId,$repeatIdx,$solver,${res.status},${res.nanoTime},${res.roundCnt}\n")

            if (res.status == KSolverStatus.UNKNOWN) return@forEach
        }
    }
}

class Solver(
    private val solver: InnerSolver,
    private val rewriteMode: RewriteMode? = null,
    private val andRewriteMode: AndRewriteMode = AndRewriteMode.SUM,
    private val signednessMode: SignednessMode = SignednessMode.UNSIGNED
) {
    enum class InnerSolver {
        Z3,
        CVC5,
        Yices,
        Bitwuzla;

        fun construct(ctx: KContext) =
            when (this) {
                Z3 -> KZ3Solver(ctx)
                CVC5 -> KCvc5Solver(ctx)
                Yices -> KYicesSolver(ctx)
                Bitwuzla -> KBitwuzlaSolver(ctx)
            }

        override fun toString(): String =
            when (this) {
                Z3 -> "Z3"
                CVC5 -> "cvc5"
                Yices -> "Yices"
                Bitwuzla -> "Bitwuzla"
            }
    }

    fun construct(ctx: KContext): KSolver<*>  = with(ctx) {
        val innerSolver = solver.construct(ctx)
        if (innerSolver is KZ3Solver) {
            innerSolver.push()
            innerSolver.assert(boolSort.mkConst("a"))
            innerSolver.check()
            innerSolver.pop()
            innerSolver.resetCheckTime()
        }
        if (rewriteMode == null) return innerSolver

        KBv2IntSolver(ctx, innerSolver, rewriteMode, andRewriteMode, signednessMode)
    }

    override fun toString(): String {
        val innerSolver = solver.toString()
        if (rewriteMode == null) return innerSolver

        val prefix = when (rewriteMode) {
            RewriteMode.EAGER -> "Eager-"
            RewriteMode.LAZY -> "NewLazyImportantApps-"
        }

        var suffix =  when (andRewriteMode) {
            AndRewriteMode.SUM -> "-Sum"
            AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (signednessMode) {
            SignednessMode.UNSIGNED -> ""
            SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflowOriginalUnsat"
            SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBoundsOriginalUnsat"
            SignednessMode.SIGNED -> "-Signed"
        }

        return prefix + innerSolver + suffix
    }
}

fun main() {
    val timerMode = TimerMode.CHECK_TIME
    val expressionsFileName = "QF_BV_wliaB"
    val solvers = listOf(
//        Solver(Solver.InnerSolver.Bitwuzla),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.UNSIGNED),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.Yices),
//        Solver(Solver.InnerSolver.CVC5),
        Solver(Solver.InnerSolver.CVC5, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.Yices),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.LAZY, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.Z3),
//        Solver(Solver.InnerSolver.Yices),
//        Solver(Solver.InnerSolver.CVC5, RewriteMode.EAGER, signednessMode = SignednessMode.UNSIGNED),
//        Solver(Solver.InnerSolver.Yices, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_UNSAT_TEST),
//        Solver(Solver.InnerSolver.Yices),
//        Solver(Solver.InnerSolver.Yices)
    )

//    val prefix = "QF_BV_lia"
//    val paths = (11..46).map { "generatedExpressions/lia/${prefix}$it" }
//
//    runDirBenchmark(paths, solvers, prefix)
//    return


    val stepIdx = 0
    val step = 20000
    val ctx = KContext()
    // wliaB
//    val skipExprs = listOf(
//        211, 403, 528, 798, 1946, 2342, 2347, 2357, 2362, 2640, 2717, 2886, 2948,
//        3393, 3676, 3794, 4212, 4588, 4654, 4960, 4994, 5156, 5168, 5715, 5890, 5892,
//        6340, 6693, 6725, 6886, 7095, 7705, 7835, 8154, 8429, 8453, 8470, 8664,
//        9088, 9118, 9392, 9595, 9645, 9715, 10033, 10067, 10136, 10142, 10226
//    )
    // wliaB cvc
    val skipExprs = listOf(
        85, 95, 211, 403, 528, 798, 1946, 2342, 2347, 2357, 2362, 2640, 2717, 2886, 2948,
        3393, 3676, 3794, 4212, 4588, 4654, 4960, 4994, 5156, 5168, 5715, 5890, 5892,
        6340, 6693, 6725, 6886, 7095, 7705, 7835, 8154, 8429, 8453, 8470, 8664,
        9088, 9118, 9392, 9595, 9645, 9715, 10033, 10067, 10136, 10142, 10226
    )
//    val skipExprs = listOf<Int>(124, 1141, 1822, 2001)
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 1704..5093 }
        .filter { (id, ) -> id !in skipExprs }

//        .filter { (id, _) -> id !in listOf(319) }

//    ctx.runBenchmark(
//        outputFile = File("benchmarkResults/trash.csv"),
//        solver = Solver(Solver.InnerSolver.Z3, RewriteMode.LAZY, signednessMode = SignednessMode.UNSIGNED),
////        solver = Solver(Solver.InnerSolver.Yices),
//        expressions = expressions.take(1000),
//        repeatNum = 3,
//        timerMode = timerMode
//    )
//    return

    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/$expressionsFileName${timerMode}.csv"),
//            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
            solver = solver,
            expressions = expressions,
            repeatNum = 1,
            timerMode = timerMode
        )
    }
}
