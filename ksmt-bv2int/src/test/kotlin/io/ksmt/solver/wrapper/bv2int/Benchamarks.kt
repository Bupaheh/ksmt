package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KBvAndExpr
import io.ksmt.expr.KBvNAndExpr
import io.ksmt.expr.KBvNorExpr
import io.ksmt.expr.KBvOrExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.rewrite.simplify.KExprSimplifier
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.solver.z3.KZ3SmtLibWriter
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


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

fun KContext.readSerializedFormulas(dir: File, begin: Int, end: Int): List<Pair<String, KExpr<KBoolSort>>> {

    val files = dir.listFiles()?.filter { !it.isDirectory } ?: error("empty folder")
    val expressions: MutableList<Pair<String, KExpr<KBoolSort>>> = mutableListOf()

    files
        .shuffled(Random(1))
        .drop(begin)
        .take(end - begin + 1)
        .forEach { file ->
            val srcSerializationCtx = AstSerializationCtx().apply { initCtx(this@readSerializedFormulas) }
            val srcMarshaller = AstSerializationCtx.marshaller(srcSerializationCtx)
            val emptyRdSerializationCtx = SerializationCtx(Serializers())
            val buffer = UnsafeBuffer(file.readBytes())
//            val currentExpressions: MutableList<KExpr<KBoolSort>> = mutableListOf()
//
//            while (true) {
//                try {
//                    currentExpressions.add(srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
//                } catch (_ : Exception) {
//                    break
//                }
//            }

            val normalizedPath = file.path.substringAfterLast("/")
            expressions.add(normalizedPath to srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
        }

    return expressions
}

inline fun KContext.readFormulas(
    dirPath: String,
    begin: Int,
    end: Int,
    filterName: (String) -> Boolean
): List<Pair<String, KExpr<KBoolSort>>> {
    val files = File(dirPath).listFiles()
        ?.filter { filterName(it.path) }
        ?.shuffled(Random(1))
        ?.drop(begin)
        ?.take(end - begin + 1) ?: return emptyList()

    assert(files.all { it.extension == "smt2" })

    val parser = KZ3SMTLibParser(this)

    return files.map { file ->
        file.path.substringAfterLast("/") to mkAnd(parser.parse(file.toPath()))
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
    } catch (e: Throwable) {
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
    expressions: List<Pair<String, KExpr<KBoolSort>>>,
    repeatNum: Int,
    timerMode: TimerMode,
    timeout: Duration
) {
    var cnt = 0
    val len = expressions.size

    expressions.forEach { (exprId, expr) ->
        cnt++

        repeat(repeatNum) { repeatIdx ->
            println("$cnt/$len")
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
val rewriteMode = RewriteMode.EAGER
val andRewriteMode = AndRewriteMode.SUM
val signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW
val unsignedMode: SignednessMode? = null

class KBv2IntCustomSolver(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    rewriteMode,
    andRewriteMode,
    signednessMode,
    unsatSignednessMode = unsignedMode,
    isSplitterOn = false,
)

fun main() {
    val ctx = KContext()
    val timerMode = TimerMode.CHECK_TIME
    val timeout = 25.seconds
    val expressionsFileName = "usvm-unbit"
    val solvers = listOf(
//        SolverConfiguration(SolverConfiguration.InnerSolver.Yices),
        SolverConfiguration(innerSolver, rewriteMode, andRewriteMode, signednessMode, unsignedMode),
    )

//    val expressions = ctx.readSerializedFormulas(
//        File("generatedExpressions/$expressionsFileName"),
//        0,
//        1000
//    ).filter { (id, expr) ->
//        TempVisitor(ctx).visit(expr) && id == "21767"
//    }.map { (id, expr) ->
//        id to KExprSimplifier(ctx).apply(expr)
//    }

    val expressions = ctx.readFormulas(
        "generatedExpressions/$expressionsFileName",
        0,
        10
    ) { name ->
        val normalized = name.substringAfterLast('/')

//        normalized in exprsToFilter
        normalized != "2019-Mann_ridecore-qf_bv-bug.smt2"
    }.filter {
        return@filter true
        println(it.first)

        val context = KBv2IntContext(ctx)
        val rewriter = KBv2IntRewriter(
            ctx,
            context,
            RewriteMode.LAZY,
            AndRewriteMode.SUM,
            SignednessMode.SIGNED_LAZY_OVERFLOW,
            DisjointSetUnion()
        )

        val rewritten = rewriter.rewriteBv2Int(it.second)

        rewriter.flag && it.second !is KInterpretedValue
    }

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
        println()
    }
    SolverConfiguration.manager.close()
    return


    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/${expressionsFileName}-UNBIT.csv"),
            solverConfiguration = solver,
            expressions = expressions,
            repeatNum = 5,
            timerMode = timerMode,
            timeout
        )
        println()
    }

    SolverConfiguration.manager.close()
}
