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
import java.io.File
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.seconds

private fun KContext.readFormulas(file: File): List<KExpr<KBoolSort>> {
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

private data class MeasureAssertTimeResult(val nanoTime: Long, val status: KSolverStatus) {
    override fun toString(): String = "$status ${nanoTime / 1e6}"
}

private fun KContext.measureAssertTime(expr: KExpr<KBoolSort>, solver: Solver): MeasureAssertTimeResult {
    val time: Long
    var status: KSolverStatus

    solver.construct(this).use { slv ->
        slv.assert(expr)

        time = measureNanoTime {
            status = slv.check(2.seconds)
        }
    }

    return MeasureAssertTimeResult(time, status)
}

private fun KContext.printDeclCount(expr: KExpr<KBoolSort>, isRewritten: Boolean = false) {
    val exprToCount = if (isRewritten) {
        KBv2IntRewriter(this, RewriteMode.EAGER, AndRewriteMode.SUM).rewriteBv2Int(expr)
    } else {
        expr
    }
    val cnt = KDeclCounter(this).countDeclarations(exprToCount)

    for ((k, v) in cnt) {
        println("$k: $v")
    }
}

private fun KContext.runBenchmark(
    outputFile: File,
    solver: Solver,
    expressions: List<Pair<Int, KExpr<KBoolSort>>>,
    repeatNum: Int
) {
    expressions.forEach { (exprId, expr) ->
        repeat(repeatNum) { repeatIdx ->
            println("$exprId\t$repeatIdx")

            val res = measureAssertTime(expr, solver)

            println(res)
            outputFile.appendText("$exprId,$repeatIdx,$solver,${res.status},${res.nanoTime}\n")

            if (res.status == KSolverStatus.UNKNOWN) return@forEach
        }
    }
}

fun KContext.readSFormulas(): List<KExpr<KBoolSort>> {
    val expressions = mutableListOf<KExpr<KBoolSort>>()

    for (i in 45500..45930) {
        expressions.addAll(readFormulas(File("generatedExpressions/formulas/f-$i.bin")))
    }

    return expressions
}

class Solver(
    private val solver: InnerSolver,
    private val rewriteMode: RewriteMode = RewriteMode.EAGER,
    private val andRewriteMode: AndRewriteMode? = null
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
        if (andRewriteMode == null) return innerSolver

        return KBv2IntSolver(ctx, innerSolver, rewriteMode, andRewriteMode)
    }

    override fun toString(): String {
        val innerSolver = solver.toString()
        if (andRewriteMode == null) return innerSolver

        val prefix = when (rewriteMode) {
            RewriteMode.EAGER -> "Eager-"
            RewriteMode.LAZY -> "Lazy-"
        }

        val suffix =  when (andRewriteMode) {
            AndRewriteMode.SUM -> "-Sum"
            AndRewriteMode.BITWISE -> "-Bitwise"
        }

        return prefix + innerSolver + suffix
    }
}

fun main() {
    val ctx = KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)
    val expressionsFileName = "1ablia"
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 0..1000 }

//    expressions.take(10).forEach {
//        ctx.printDeclCount(it.second, false)
//        println()
//    }
//
//    return

    val solvers = listOf(Solver.InnerSolver.Z3)
    val modes = listOf(RewriteMode.EAGER)
    val andModes = listOf(null)

    ctx.runBenchmark(
        outputFile = File("benchmarkResults/trash.csv"),
        solver = Solver(solvers.first(), modes.first(), andModes.first()),
        expressions = expressions.take(700),
        repeatNum = 3
    )

    for (slv in solvers) {
        for (andMode in andModes) {
            for (mode in modes) {
                ctx.runBenchmark(
                    outputFile = File("benchmarkResults/${expressionsFileName}.csv"),
                    solver = Solver(slv, mode, andMode),
                    expressions = expressions,
                    repeatNum = 3
                )

                if (andMode == null) break
            }
        }
    }
}
