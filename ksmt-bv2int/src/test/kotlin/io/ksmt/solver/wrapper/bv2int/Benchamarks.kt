package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import com.sri.yices.Yices
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
            RewriteMode.LAZY -> "NewLazy-"
        }

        var suffix =  when (andRewriteMode) {
            AndRewriteMode.SUM -> "-Sum"
            AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (signednessMode) {
            SignednessMode.UNSIGNED -> ""
            SignednessMode.SIGNED_LAZY_OVERFLOW -> "-Signed"
            SignednessMode.SIGNED_NO_OVERFLOW -> "-SignedNoOverflow"
        }

        return prefix + innerSolver + suffix
    }
}

fun KContext.testing(expr: KExpr<KBoolSort>) {
    KBv2IntSolver(this, KZ3Solver(this), RewriteMode.LAZY).use { solver ->
        solver.assert(expr)
        println(solver.check())
    }
}

fun main() {
    val ctx = KContext()
    val expressionsFileName = "1ablia"
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 0..1000 }

    ctx.testing(expressions[918].second)

    return

    val solvers = listOf(
        Solver(Solver.InnerSolver.Z3),
        Solver(Solver.InnerSolver.CVC5),
        Solver(Solver.InnerSolver.Z3, RewriteMode.LAZY, andRewriteMode = AndRewriteMode.BITWISE),
        Solver(Solver.InnerSolver.CVC5, RewriteMode.LAZY, andRewriteMode = AndRewriteMode.BITWISE),
        Solver(Solver.InnerSolver.Z3, RewriteMode.LAZY, andRewriteMode = AndRewriteMode.SUM),
        Solver(Solver.InnerSolver.CVC5, RewriteMode.LAZY, andRewriteMode = AndRewriteMode.SUM),
        Solver(Solver.InnerSolver.Yices),
//        Solver(Solver.InnerSolver.CVC5, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
//        Solver(Solver.InnerSolver.CVC5, RewriteMode.EAGER, signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW),
    )

    ctx.runBenchmark(
        outputFile = File("benchmarkResults/trash.csv"),
        solver = Solver(Solver.InnerSolver.Z3),
        expressions = expressions.take(700),
        repeatNum = 3
    )

    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
            solver = solver,
            expressions = expressions,
            repeatNum = 3
        )
    }
}
