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
import io.ksmt.solver.util.KExprIntInternalizerBase
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.solver.yices.KYicesModel
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.solver.z3.KZ3SmtLibWriter
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.solver.z3.KZ3SolverConfiguration
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.utils.getValue
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
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
//        .shuffled(Random(1))
        .sortedBy { it.path.substringAfterLast("/").toInt() }
        .drop(begin)
        .take(end - begin + 1)
        .forEach { file ->
            val srcSerializationCtx = AstSerializationCtx().apply { initCtx(this@readSerializedFormulas) }
            val srcMarshaller = AstSerializationCtx.marshaller(srcSerializationCtx)
            val emptyRdSerializationCtx = SerializationCtx(Serializers())
            val buffer = UnsafeBuffer(file.readBytes())

            val normalizedPath = file.path.substringAfterLast("/")
            expressions.add(normalizedPath to srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
        }

    return expressions
}

fun KContext.readSerializedFormulasUsvm(dir: File, begin: Int, end: Int): List<Pair<String, List<KExpr<KBoolSort>>>> {

    val files = dir.listFiles()?.filter { !it.isDirectory } ?: error("empty folder")
    val expressions: MutableList<Pair<String, List<KExpr<KBoolSort>>>> = mutableListOf()

    files
//        .shuffled(Random(1))
        .sortedBy { it.path.substringAfterLast("/").toInt() }
        .forEach { file ->
            val srcSerializationCtx = AstSerializationCtx().apply { initCtx(this@readSerializedFormulasUsvm) }
            val srcMarshaller = AstSerializationCtx.marshaller(srcSerializationCtx)
            val emptyRdSerializationCtx = SerializationCtx(Serializers())
            val buffer = UnsafeBuffer(file.readBytes())
            val currentExpressions: MutableList<KExpr<KBoolSort>> = mutableListOf()

            while (true) {
                try {
                    currentExpressions.add(srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
                } catch (e : Exception) {
                    println(e)
                    break
                }
            }

            val normalizedPath = file.path.substringAfterLast("/")
            expressions.add(normalizedPath to currentExpressions)
        }

    return expressions.map { it.first to it.second.drop(begin).take(end - begin + 1) }
}

inline fun KContext.readFormulas(
    dirPath: String,
    begin: Int,
    end: Int,
    filterName: (String) -> Boolean = { true }
): List<Pair<String, KExpr<KBoolSort>>> {
    val files = File(dirPath).listFiles()
        ?.filter { filterName(it.path) }
//        ?.shuffled(Random(1))
        ?.drop(begin)
        ?.take(end - begin + 1) ?: return emptyList()

    assert(files.all { it.extension == "smt2" })

    val parser = KZ3SMTLibParser(this)

    return files.map { file ->
        file.path.substringAfterLast("/") to mkAnd(parser.parse(file.toPath()))
    }
}

private data class MeasureAssertTimeResult(
    val checkTime: Long,
    val assertTime: Long,
    val status: KSolverStatus,
    val roundCnt: Int
) {
    override fun toString(): String = "$status ${checkTime.nanoseconds} ${assertTime.nanoseconds} $roundCnt"
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
    timeout: Duration
): MeasureAssertTimeResult {
    var status: KSolverStatus
    val solver = solverDescription.construct(this)
    val checkTime: Long

    solver.push()

    val assertTime = try {
        measureNanoTime {
            solver.assert(expr)
            status = solver.check(timeout)

//            if (status == KSolverStatus.SAT) { solver.model() }
        }
    } catch (e: Throwable) {
        throw e
        println(e.message)
//        solver.close()
        return MeasureAssertTimeResult(timeout.inWholeNanoseconds, timeout.inWholeNanoseconds, KSolverStatus.UNKNOWN, -2)
    } finally {
        checkTime = solver.getCheckTime(timeout.inWholeNanoseconds)
        solver.pop()
    }

    val roundCnt = solver.reasonOfUnknown().substringAfterLast(';').toIntOrNull() ?: -1

    return MeasureAssertTimeResult(checkTime, assertTime, status, roundCnt)
}

private fun KContext.runBenchmark(
    outputFile: File,
    solverConfiguration: SolverConfiguration,
    expressions: List<Pair<String, KExpr<KBoolSort>>>,
    repeatNum: Int,
    timerMode: TimerMode,
    timeout: Duration,
    cleanSolver: Boolean = false
) {
    var cnt = 0
    val len = expressions.size

    expressions.forEach { (exprId, expr) ->
        cnt++

        repeat(repeatNum) { repeatIdx ->
            if (cleanSolver) solverConfiguration.initSolver()

            println("$cnt/$len")
            println("$exprId\t$repeatIdx")

            val res = measureAssertTime(expr, solverConfiguration, timeout)

            if (cleanSolver) solverConfiguration.closeSolver()
            if (res.roundCnt == -2) return@forEach

            println(res)

            outputFile.appendText("$exprId,$repeatIdx,$solverConfiguration,${res.status},${res.checkTime},${res.assertTime},${res.roundCnt}\n")

            if (res.status == KSolverStatus.UNKNOWN) return@forEach
        }

    }
}

private fun KContext.runBenchmarkUsvm(
    outputFile: File,
    solverConfiguration: SolverConfiguration,
    expressions: List<Pair<String, List<KExpr<KBoolSort>>>>,
    timeout: Duration,
) {
    expressions.forEach { (exprId, exprs) ->
        solverConfiguration.initSolver()
        exprs.forEachIndexed { idx, expr ->
            println("$exprId/${expressions.size}")
            println("$idx/${exprs.size}")

            val res = measureAssertTime(expr, solverConfiguration, timeout)

            if (res.roundCnt != -2) {
                println(res)

                outputFile.appendText("$exprId-$idx,0,$solverConfiguration,${res.status},${res.checkTime},${res.assertTime},${res.roundCnt}\n")
            }
        }
        solverConfiguration.closeSolver()
    }
}

val innerSolver = SolverConfiguration.InnerSolver.Yices
val rewriteMode = RewriteMode.EAGER
val andRewriteMode = AndRewriteMode.SUM
val signednessMode = SignednessMode.SIGNED
val unsignedMode: SignednessMode? = null

class KBv2IntCustomSolver(
    ctx: KContext,
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KBenchmarkSolverWrapper(ctx, KYicesSolver(ctx)),
    rewriteMode,
    andRewriteMode,
    signednessMode,
    unsatSignednessMode = unsignedMode,
    isSplitterOn = true,
    round1Result = false
) {
    init {
        val solverName = innerSolver.toString().removeSuffix("MCSAT")
        val configName = KBv2IntCustomSolver::class.supertypes.single().arguments.single().toString()

        require(solverName in configName)
    }
}

fun main() {
    val ctx = KContext()
    val timeout = 1.seconds
    val expressionsFileName = "usvm-owasp"
    val solvers = listOf(
//        SolverConfiguration(ctx, innerSolver),
        SolverConfiguration(ctx, innerSolver, rewriteMode, andRewriteMode, signednessMode, unsignedMode),
    )
    SolverConfiguration.manager.close()

    val expressions = ctx.readSerializedFormulasUsvm(
        File("generatedExpressions/$expressionsFileName"),
        0,
        10000
    )
        .map { (id, l) ->
            id to l.filterNot { expr -> TempVisitor(ctx).visit(expr) }.take(400).drop(6)
//                .filter { ArithFilter(ctx).filter(it) }
        }
        .drop(1)
//        .filter { it.first == "5" }
//        .map { (id, l) -> id to l.take(35023) }


//    val expressions = ctx.readFormulas(
//        "generatedExpressions/$expressionsFileName",
//        0,
//        32000
//    )


//    for (solver in solvers) {
//        ctx.runBenchmarkUsvm(
//            outputFile = File("benchmarkResults/trash.csv"),
////            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
//            solverConfiguration = solver,
//            expressions = expressions,
//            timerMode = timerMode,
//            timeout = timeout,
//        )
//    }
//    return

    for (solver in solvers) {
        ctx.runBenchmarkUsvm(
//            outputFile = File("benchmarkResults/${expressionsFileName}-UNBIT-woM.csv"),
            outputFile = File("benchmarkResults/trash.csv"),
            solverConfiguration = solver,
            expressions = expressions,
            timeout
        )
        println()
    }
}
