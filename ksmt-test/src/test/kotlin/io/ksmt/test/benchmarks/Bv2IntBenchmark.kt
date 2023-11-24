package io.ksmt.test.benchmarks

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.async.KAsyncSolver
import io.ksmt.solver.wrapper.bv2int.KBenchmarkSolverWrapper
import io.ksmt.solver.wrapper.bv2int.KBv2IntRewriter
import io.ksmt.solver.wrapper.bv2int.KBv2IntSolver
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.yices.KYicesSolverConfiguration
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.sort.KBoolSort
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Bv2IntBenchmark : BenchmarksBasedTest() {
    private val outputPath = "report.csv"
    private val outputFile = File(outputPath)
    private val checkTimeout =  3.seconds
    private val repeatNum = 3

    class KBv2IntCustomSolver(
        ctx: KContext
    ) : KBv2IntSolver<KYicesSolverConfiguration>(
        ctx,
        KBenchmarkSolverWrapper(KYicesSolver(ctx)),
        KBv2IntRewriter.RewriteMode.LAZY,
        KBv2IntRewriter.AndRewriteMode.SUM,
        KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW
    )

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

                repeat(repeatNum) { repeatIdx ->
                    val res = measureAssertTime(ctx, ksmtAssertion, solverProvider, checkTimeout)

                    println(name)
                    println(res)

                    if (res.roundCnt == -2) return@repeat

                    outputFile.appendText(
                        "$name,$repeatIdx,Yices,${res.status},${res.checkTime},${res.assertTime},${res.roundCnt}\n"
                    )

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
    ): MeasureTimeResult {
        val solver = solverProvider(ctx)
        val status: KSolverStatus

        val assertTime = try {
            measureNanoTime {
                solver.assert(expr)
                status = solver.check(timeout)
            }
        } catch (_: Throwable) {
            solver.close()

            return MeasureTimeResult(
                timeout.inWholeNanoseconds,
                timeout.inWholeNanoseconds,
                KSolverStatus.UNKNOWN,
                -2
            )
        }

        val checkTime = solver.getCheckTime(timeout.inWholeNanoseconds)
        val roundCnt = solver.getRoundCount(-1)

        solver.close()

        return MeasureTimeResult(checkTime, assertTime, status, roundCnt)
    }

    private data class MeasureTimeResult(
        val checkTime: Long,
        val assertTime: Long,
        val status: KSolverStatus,
        val roundCnt: Int
    ) {
        override fun toString(): String = "$status ${checkTime / 1e3} $roundCnt"
    }

    private fun KSolver<*>.getCheckTime(default: Long): Long {
        val reason = reasonOfUnknown().replaceAfterLast(';', "").removeSuffix(";")
        val result = reason.toLongOrNull() ?: default

        assert(result > 0L)

        return result
    }

    private fun KSolver<*>.getRoundCount(default: Int): Int =
        reasonOfUnknown().substringAfterLast(';').toIntOrNull() ?: default

    companion object {
        @JvmStatic
        fun bv2intTestData() = testData { it.startsWith("bv2int-") }
    }
}
