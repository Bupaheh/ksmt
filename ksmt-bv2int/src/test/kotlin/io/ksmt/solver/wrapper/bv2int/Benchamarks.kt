package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvArithShiftRightExpr
import io.ksmt.expr.KBvExtractExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvShiftLeftExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.rewrite.KExprUninterpretedDeclCollector
import io.ksmt.expr.rewrite.simplify.KExprSimplifier
import io.ksmt.expr.transformer.KNonRecursiveTransformer
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
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.utils.getValue
import io.ksmt.utils.mkConst
import java.io.File
import kotlin.random.Random
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
            RewriteMode.LAZY -> "Lazy-"
        }

        var suffix =  when (andRewriteMode) {
            AndRewriteMode.SUM -> "-Sum"
            AndRewriteMode.BITWISE -> "-Bitwise"
        }

        suffix += when (signednessMode) {
            SignednessMode.UNSIGNED -> ""
            SignednessMode.SIGNED_LAZY_OVERFLOW -> "-SignedLazyOverflow"
            SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS -> "-SignedLazyOverflowNoBounds"
            SignednessMode.SIGNED -> "-Signed"
        }

        return prefix + innerSolver + suffix
    }
}

class TempVisitor(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private var flag = false

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        return super.transformApp(expr)
    }

    fun <T : KSort> visit(expr: KExpr<T>): Boolean {
        apply(expr)
        return flag
    }

    private fun visitBitwiseOp(lhs: KExpr<*>, rhs: KExpr<*>) {
        val f = if (lhs is KApp<*, *>) lhs.decl.name else null
        val s = if (rhs is KApp<*, *>) rhs.decl.name else null

        if (f == null || s == null) return

        val l = listOf(lhs, rhs)

        flag = true

//        if (f == "bvshl" && s == "zero_extend") {
//            flag = false
//        }
//
//        if (flag) {
//            println("$f $s")
//        }
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): KExpr<T> {
        visitBitwiseOp(expr.arg0, expr.arg1)
        return super.transform(expr)
    }
}

fun main() {
    val ctx = KContext()
    val timerMode = TimerMode.CHECK_TIME
    val expressionsFileName = "QF_BV_2000CT"
    val solvers = listOf(
        Solver(Solver.InnerSolver.Z3, RewriteMode.LAZY, AndRewriteMode.SUM, SignednessMode.SIGNED),
//        Solver(Solver.InnerSolver.Z3)
    )

    // wliaB
//    val skipExprs = listOf(
//        211, 403, 528, 798, 1946, 2342, 2347, 2357, 2362, 2640, 2717, 2886, 2948,
//        3393, 3676, 3794, 4212, 4588, 4654, 4960, 4994, 5156, 5168, 5715, 5890, 5892,
//        6340, 6693, 6725, 6886, 7095, 7705, 7835, 8154, 8429, 8453, 8470, 8664,
//        9088, 9118, 9392, 9595, 9645, 9715, 10033, 10067, 10136, 10142, 10226
//    )
//    // wliaB cvc
//    val skipExprs = listOf(
//        85, 95, 211, 403, 528, 798, 1946, 2342, 2347, 2357, 2362, 2640, 2717, 2886, 2948,
//        3393, 3676, 3794, 4212, 4588, 4654, 4960, 4994, 5156, 5168, 5715, 5890, 5892,
//        6340, 6693, 6725, 6886, 7095, 7705, 7835, 8154, 8429, 8453, 8470, 8664,
//        9088, 9118, 9392, 9595, 9645, 9715, 10033, 10067, 10136, 10142, 10226
//    )
    // cvc bslia
//    val skipExprs = listOf<Int>(7, 73, 291, 416, 588, 634)
    // cvc bwslia z3
//    val skipExprs = listOf(258, 1108, 3493)
    // cvc wbslia cvc
//    val skipExprs = listOf(63, 69, 80, 113, 154)

//    val skipExprs = listOf(553, 645)
    val skipExprs = listOf(258)
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 387..1000 }
//        .filter { TempVisitor(ctx).visit(it.second) }
        .filter { (id, ) -> id !in skipExprs }

//    for (solver in solvers) {
//        ctx.runBenchmark(
//            outputFile = File("benchmarkResults/trash.csv"),
////            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
//            solver = solver,
//            expressions = expressions,
//            repeatNum = 4,
//            timerMode = timerMode
//        )
//    }
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
