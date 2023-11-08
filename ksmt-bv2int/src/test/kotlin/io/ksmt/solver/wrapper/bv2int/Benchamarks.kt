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
import io.ksmt.expr.KBvSignedGreaterExpr
import io.ksmt.expr.KBvSignedLessExpr
import io.ksmt.expr.KBvUnsignedGreaterExpr
import io.ksmt.expr.KBvUnsignedGreaterOrEqualExpr
import io.ksmt.expr.KBvUnsignedLessExpr
import io.ksmt.expr.KBvUnsignedLessOrEqualExpr
import io.ksmt.expr.KBvXNorExpr
import io.ksmt.expr.KBvXorExpr
import io.ksmt.expr.KEqExpr
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
import io.ksmt.solver.cvc5.KCvc5SolverConfiguration
import io.ksmt.solver.cvc5.KCvc5SolverUniversalConfiguration
import io.ksmt.solver.runner.KSolverRunnerManager
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.RewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.AndRewriteMode
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter.SignednessMode
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.yices.KYicesSolverConfigurationImpl
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.solver.z3.KZ3SolverConfiguration
import io.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import io.ksmt.utils.getValue
import io.ksmt.utils.mkConst
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TempVisitor(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private var cmp = false
    private var bit = true

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        if (expr.args.any { it is KBvLogicalShiftRightExpr } &&
            expr.args.any { it is KInterpretedValue } &&
            (expr is KBvUnsignedLessExpr || expr is KBvUnsignedGreaterExpr ||
                    expr is KBvUnsignedGreaterOrEqualExpr || expr is KBvUnsignedLessOrEqualExpr)
        ) {
            cmp = true
        }
        return super.transformApp(expr)
    }

    fun <T : KSort> visit(expr: KExpr<T>): Boolean {
        apply(expr)
        return cmp
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
    solver: Solver,
    expressions: List<Pair<Int, KExpr<KBoolSort>>>,
    repeatNum: Int,
    timerMode: TimerMode,
    timeout: Duration
) {
    expressions.forEach { (exprId, expr) ->
        repeat(repeatNum) { repeatIdx ->
            println("$exprId\t$repeatIdx")

            val res = measureAssertTime(expr, solver, timerMode, timeout)
            if (res.roundCnt == -2) return@forEach

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
                Z3 -> manager.createSolver(ctx, KZ3Solver::class)
                CVC5 -> manager.createSolver(ctx, KCvc5Solver::class)
                Yices -> manager.createSolver(ctx, KYicesSolver::class)
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
        val result = if (rewriteMode == null) {
            solver.construct(ctx)
        } else {
            manager.createSolver(ctx, KBv2IntCustomSolver::class)
        }

        if (solver == InnerSolver.Z3) {
            result.push()
            result.assert(boolSort.mkConst("a"))
            result.check()
            result.pop()
        }

        result
    }

    override fun toString(): String {
        val innerSolver = solver.toString()
        if (rewriteMode == null) return innerSolver

        val prefix = when (rewriteMode) {
            RewriteMode.EAGER -> "Eager-"
            RewriteMode.LAZY -> "LazyULshr-"
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

    companion object {
        val manager = KSolverRunnerManager(hardTimeout = 5.seconds, workerProcessIdleTimeout = 40.seconds)

        init {
            manager.registerSolver(KBv2IntCustomSolver::class, KYicesSolverUniversalConfiguration::class)
        }
    }
}

val innerSolver = Solver.InnerSolver.Yices
val rewriteMode = RewriteMode.LAZY
val andRewriteMode = AndRewriteMode.SUM
val signednessMode = SignednessMode.SIGNED_LAZY_OVERFLOW

class KBv2IntCustomSolver(
    ctx: KContext
) : KBv2IntSolver<KYicesSolverConfiguration>(
    ctx,
    KYicesSolver(ctx),
    rewriteMode,
    andRewriteMode,
    signednessMode
)

fun main() {
//    TODO("check 247, 817, 1093")
    val ctx = KContext()
    val timerMode = TimerMode.CHECK_TIME
    val timeout = 2.seconds
    val expressionsFileName = "QF_BV0"
    val solvers = listOf(
//        Solver(Solver.InnerSolver.Yices),
        Solver(innerSolver, rewriteMode, andRewriteMode, signednessMode),
    )

//    val skipExprs = listOf(133, 237, 325, 416, 471, 553, 687, 701, 710, 826, 852, 984, 994, 1064, 1108)
    val skipExprs = listOf(237, 471, 826, 984, 994, 1064, 1108)

    val exprToCheckSatUnbit = listOf(
        2,
        33,
        88,
        109,
        134,
        148,
        150,
        174,
        189,
        199,
        278,
        406,
        409,
        531,
        556,
        624,
        640,
        649,
        669,
        713,
        810,
        899,
        903,
        990,
        1003,
        1029,
        1040,
        1067,
        1104
    )
    val expressions = ctx.readFormulas(File("generatedExpressions/$expressionsFileName"))
        .mapIndexed { id, expr -> id to expr }
        .filter { (id, _) -> id in 0..1300 }
        .filter { (id, _) -> id in listOf(1093)}
        .filter { TempVisitor(ctx).visit(it.second) }
        .onEach {
            println(KDeclCounter(ctx).countDeclarations(it.second))
        }


//        .filter { (id, _) -> id in exprToCheck }
//        .filter { (id, ) -> id !in skipExprs }

//    expressions.forEach { (id, expr) ->
//        val t = KDeclCounter(ctx).countDeclarations(expr)
//        println("$id: $t")
//    }
//    return
//
    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/trash.csv"),
//            outputFile = File("benchmarkResults/${expressionsFileName}Test.csv"),
            solver = solver,
            expressions = expressions,
            repeatNum = 1,
            timerMode = timerMode,
            timeout = timeout
        )
    }
    Solver.manager.close()
    return


    for (solver in solvers) {
        ctx.runBenchmark(
            outputFile = File("benchmarkResults/$expressionsFileName${timerMode}LshrTest.csv"),
            solver = solver,
            expressions = expressions,
            repeatNum = 1,
            timerMode = timerMode,
            timeout
        )
    }

    Solver.manager.close()
}
