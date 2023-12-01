package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvArithShiftRightExpr
import io.ksmt.expr.KBvLogicalShiftRightExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvShiftLeftExpr
import io.ksmt.expr.KBvSignedGreaterExpr
import io.ksmt.expr.KBvSignedGreaterOrEqualExpr
import io.ksmt.expr.KBvSignedLessExpr
import io.ksmt.expr.KBvSignedLessOrEqualExpr
import io.ksmt.expr.KBvUnsignedGreaterExpr
import io.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import io.ksmt.expr.KBvUnsignedLessExpr
import io.ksmt.expr.KBvUnsignedLessOrEqualExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.cvc5.KCvc5SolverConfiguration
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.utils.mkConst
import java.io.File
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.ksmt.solver.wrapper.bv2int.KBenchmarkSolverWrapper


class TempVisitor(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private var cmp = false
    private var bit = true

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        return super.transformApp(expr)
    }

    fun <T : KSort> visit(expr: KExpr<T>): Boolean {
        apply(expr)
        return bit
    }

    private fun visitBitwiseOp(lhs: KExpr<*>, rhs: KExpr<*>) {

        bit = false

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

fun KSolver<*>.getCheckTime(default: Long): Long {
    val reason = reasonOfUnknown().replaceAfterLast(';', "").removeSuffix(";")
    val result = reason.toLongOrNull() ?: default

    return if (result == 0L) {
        default
    } else {
        result
    }
}

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

inline fun KContext.readFormulas(
    dirPath: String,
    begin: Int,
    end: Int,
    filterName: (String) -> Boolean
): List<Pair<Int, KExpr<KBoolSort>>> {
    val files = File(dirPath).listFiles()
        ?.mapIndexed { id, file -> id to file }
        ?.filter { filterName(it.second.path) }
        ?.filter { (id, _) -> id in begin..end } ?: return emptyList()

    assert(files.all { it.second.extension == "smt2" })

    val parser = KZ3SMTLibParser(this)

    return files.map { (id, file) ->
        id to mkAnd(parser.parse(file.toPath()))
    }
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
    solverDescription: SolverConfiguration,
    timerMode: TimerMode,
    timeout: Duration
): MeasureAssertTimeResult {
    var status: KSolverStatus
    val solver = solverDescription.construct(this)

    val assertTime = try {
        measureNanoTime {
            solver.assert(expr)
            status = solver.check(timeout)
        }
    } catch (e: Exception) {
        println(e.message)
        solver.close()
        return MeasureAssertTimeResult(timeout.inWholeNanoseconds, KSolverStatus.UNKNOWN, -2)
    }



    val resTime = when (timerMode) {
        TimerMode.CHECK_TIME -> solver.getCheckTime(timeout.inWholeNanoseconds)
        TimerMode.ASSERT_TIME -> assertTime
    }

    val roundCnt = solver.reasonOfUnknown().substringAfterLast(';').toIntOrNull() ?: -1

    solver.close()

    return MeasureAssertTimeResult(resTime, status, roundCnt)
}

private fun KContext.runBenchmark(
    outputFile: File,
    solverConfiguration: SolverConfiguration,
    expressions: List<Pair<Int, KExpr<KBoolSort>>>,
    repeatNum: Int,
    timerMode: TimerMode,
    timeout: Duration
) {
    expressions.forEach { (exprId, expr) ->
        repeat(repeatNum) { repeatIdx ->
            println("$exprId\t$repeatIdx")

            val res = measureAssertTime(expr, solverConfiguration, timerMode, timeout)
            if (res.roundCnt == -2) return@forEach

            println(res)

            outputFile.appendText("$exprId,$repeatIdx,$solverConfiguration,${res.status},${res.nanoTime},${res.roundCnt}\n")

            if (res.status == KSolverStatus.UNKNOWN) return@forEach
        }
    }
}

val innerSolver = SolverConfiguration.InnerSolver.Yices
val rewriteMode = RewriteMode.LAZY
val andRewriteMode = AndRewriteMode.SUM
val signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW

class KBv2IntCustomSolver(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    rewriteMode,
    andRewriteMode,
    signednessMode,
    unsatSignednessMode = SignednessMode.UNSIGNED,
    testFlag = true
)

fun main() {
    val ctx = KContext()
    val timerMode = TimerMode.CHECK_TIME
    val timeout = 2.seconds
    val expressionsFileName = "QF_BV_UNBIT"
    val solvers = listOf(
//        SolverConfiguration(SolverConfiguration.InnerSolver.Yices),
        SolverConfiguration(innerSolver, rewriteMode, andRewriteMode, signednessMode),
    )

    val expressions = ctx.readFormulas(
        "generatedExpressions/$expressionsFileName",
        0,
        100000
    ) { name ->
        val normalized = name.substringAfterLast('/')
        normalized == "sage_app7_bench_854.smt2"
    }

    expressions.forEach {
        println(KDeclCounter(ctx).countDeclarations(it.second))
    }

//    return

    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/trash.csv"),
//            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
            solverConfiguration = solver,
            expressions = expressions,
            repeatNum = 1,
            timerMode = timerMode,
            timeout = timeout
        )
    }
    SolverConfiguration.manager.close()
    return


    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/$expressionsFileName${timerMode}.csv"),
            solverConfiguration = solver,
            expressions = expressions,
            repeatNum = 1,
            timerMode = timerMode,
            timeout
        )
    }

    SolverConfiguration.manager.close()
}
