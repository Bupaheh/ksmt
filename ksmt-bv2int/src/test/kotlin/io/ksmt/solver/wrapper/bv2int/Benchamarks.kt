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
import java.io.File
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
    override fun toString(): String = "$status ${nanoTime / 1e6} $roundCnt"
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
        status = solver.check(2.seconds)
    }

    val resTime = when (timerMode) {
        TimerMode.CHECK_TIME -> solver.getCheckTime()
        TimerMode.ASSERT_TIME -> assertTime
    }

    solver.close()

    return MeasureAssertTimeResult(resTime, status, solver.roundCount)
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

    fun construct(ctx: KContext): KSolver<*> {
        val innerSolver = solver.construct(ctx)
        if (rewriteMode == null) return innerSolver

        return KBv2IntSolver(ctx, innerSolver, rewriteMode, andRewriteMode, signednessMode)
    }

    override fun toString(): String {
        val innerSolver = solver.toString()
        if (rewriteMode == null) return innerSolver

        val prefix = when (rewriteMode) {
            RewriteMode.EAGER -> "Eager-"
            RewriteMode.LAZY -> "LazyImportantApps-"
        }

        var suffix =  when (andRewriteMode) {
            AndRewriteMode.SUM -> "-Sum"
            AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (signednessMode) {
            SignednessMode.UNSIGNED -> ""
            SignednessMode.SIGNED_LAZY_OVERFLOW -> "-Signed"
            SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedNoBounds"
            SignednessMode.SIGNED_NO_OVERFLOW -> "-SignedNoOverflow"
        }

        return prefix + innerSolver + suffix
    }
}

val exprsToCheck = mutableListOf(0, 27, 31, 50, 59, 76, 88, 89, 117, 127, 132, 134, 138, 147, 153, 166)
// lia

fun runDirBenchmark(paths: List<String>, solvers: List<Solver>, resPath: String, timerMode: TimerMode) {
    for (solver in solvers) {
        var cnt = 121

        paths.forEach { path ->
            val ctx = KContext()
            val expressions = ctx.readFormulas(File(path))
                .mapIndexed { id, expr -> id + cnt to expr }
                .filter { (id, _) -> id < 10000 && id !in listOf(94) }
            cnt += expressions.size

            ctx.runBenchmark(
                outputFile = File("benchmarkResults/$resPath.csv"),
                solver = solver,
                expressions = expressions,
                repeatNum = 1,
                timerMode = timerMode
            )

            println(exprsToCheck)
            println(path)
        }
    }
}


fun main() {
    val timerMode = TimerMode.CHECK_TIME
    val expressionsFileName = "QF_BV_05wlia"
    val solvers = listOf(
//        Solver(Solver.InnerSolver.Z3),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.UNSIGNED),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.Z3, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_NO_OVERFLOW),
        Solver(Solver.InnerSolver.Yices, RewriteMode.EAGER, signednessMode = SignednessMode.UNSIGNED)
    )

//    val prefix = "QF_BV_lia"
//    val paths = (11..46).map { "generatedExpressions/lia/${prefix}$it" }
//
//    runDirBenchmark(paths, solvers, prefix)
//    return

    val ctx = KContext()
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 0..1000 }

//    ctx.runBenchmark(
//        outputFile = File("benchmarkResults/trash.csv"),
//        solver = Solver(Solver.InnerSolver.Z3),
//        expressions = expressions.take(200),
//        repeatNum = 3,
//        timerMode = timerMode
//    )


    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/$expressionsFileName$timerMode.csv"),
            solver = solver,
            expressions = expressions,
            repeatNum = 3,
            timerMode = timerMode
        )
    }
}
