package io.ksmt.test.benchmarks

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.async.KAsyncSolver
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter
import io.ksmt.solver.wrapper.bv2int.KBv2IntSolver
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import io.ksmt.sort.KBoolSort
import io.ksmt.test.TestRunner
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Bv2IntBenchmark : BenchmarksBasedTest() {

    class KBv2IntCustomSolver(
        ctx: KContext
    ) : KBv2IntSolver<KYicesSolverConfiguration>(
        ctx,
        KYicesSolver(ctx),
        KBv2IntRewriter.RewriteMode.LAZY,
        KBv2IntRewriter.AndRewriteMode.SUM,
        KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
    )

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("bv2intTestData")
    fun benchmark(name: String, samplePath: Path) = benchmarkSolver(name, samplePath) { ctx ->
        solverManager.run {
            registerSolver(KBv2IntCustomSolver::class, KYicesSolverUniversalConfiguration::class)
            createSolver(ctx, KBv2IntCustomSolver::class)
        }
    }

    private fun <C : KSolverConfiguration> benchmarkSolver(
        name: String,
        samplePath: Path,
        solverProvider: (KContext) -> KAsyncSolver<C>
    ) = handleIgnoredTests("bv2int-benchmark[$name]") {
        ignoreNoTestDataStub(name)
        val ctx = KContext()
        testWorkers.withWorker(ctx) { worker ->
            worker.skipBadTestCases {
                val assertions = worker.parseFile(samplePath)
                val ksmtAssertion = ctx.mkAnd(worker.convertAssertions(assertions))

                repeat(3) { repeatIdx ->
                    val res = measureAssertTime(ctx, ksmtAssertion, solverProvider, 2.seconds)

                    println(name)
                    println(res)

                    if (res.roundCnt == -2) return@repeat

                    if (res.status == KSolverStatus.UNKNOWN) return@repeat
                }
            }
        }
    }

    private fun <C : KSolverConfiguration> measureAssertTime(
        ctx: KContext,
        expr: KExpr<KBoolSort>,
        solverProvider: (KContext) -> KAsyncSolver<C>,
        timeout: Duration
    ): MeasureAssertTimeResult {
        val status: KSolverStatus
        val solver = solverProvider(ctx)

        try {
            solver.assert(expr)
            status = solver.check(timeout)
        } catch (e: Exception) {
            println(e.message)
            solver.close()
            return MeasureAssertTimeResult(timeout.inWholeNanoseconds, KSolverStatus.UNKNOWN, -2)
        }

        val resTime = solver.getCheckTime(timeout.inWholeNanoseconds)

        val roundCnt = solver.reasonOfUnknown().substringAfterLast(';').toIntOrNull() ?: -1

        solver.close()

        return MeasureAssertTimeResult(resTime, status, roundCnt)
    }

    private data class MeasureAssertTimeResult(val nanoTime: Long, val status: KSolverStatus, val roundCnt: Int) {
        override fun toString(): String = "$status ${nanoTime / 1e3} $roundCnt"
    }

    private fun KSolver<*>.getCheckTime(default: Long): Long {
        val reason = reasonOfUnknown().replaceAfterLast(';', "").removeSuffix(";")
        val result = reason.toLongOrNull() ?: default

        return if (result == 0L) {
            default
        } else {
            result
        }
    }

    companion object {
        @JvmStatic
        fun bv2intTestData() = testData { it.startsWith("bv2int-") }
    }
}