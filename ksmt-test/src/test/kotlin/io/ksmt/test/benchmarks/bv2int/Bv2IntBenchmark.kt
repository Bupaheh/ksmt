package io.ksmt.test.benchmarks.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.async.KAsyncSolver
import io.ksmt.solver.cvc5.KCvc5SolverUniversalConfiguration
import io.ksmt.solver.yices.KYicesSolverUniversalConfiguration
import io.ksmt.solver.z3.KZ3SolverUniversalConfiguration
import io.ksmt.sort.KBoolSort
import io.ksmt.test.benchmarks.BenchmarksBasedTest
import io.ksmt.utils.uncheckedCast
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Bv2IntBenchmark : BenchmarksBasedTest() {
    private val outputPath = "report.csv"
    private val outputFile = File(outputPath)
    private val checkTimeout =  3.seconds
    private val repeatNum = 3

    private val solvers: List<Pair<KClass<KSolver<KSolverConfiguration>>, String>> = listOf(
//        KYicesSolverBench::class to "Yices",
//        KZ3SolverBench::class to "Z3",
        KCvc5SolverBench::class to "Cvc5",
//        KYicesLazySumSignedLazyOverflow::class to "Yices-Lazy-Sum-SignedLazyOverflow",
//        KZ3LazySumSignedLazyOverflow::class to "Z3-Lazy-Sum-SignedLazyOverflow",
        KCvc5LazySumSignedLazyOverflow::class to "Cvc5-Lazy-Sum-SignedLazyOverflow",
//        KYicesLazySumSigned::class to "Yices-Lazy-Sum-Signed",
//        KZ3LazySumSigned::class to "Z3-Lazy-Sum-Signed",
        KCvc5LazySumSigned::class to "Cvc5-Lazy-Sum-Signed",
//        KYicesLazySumUnsigned::class to "Yices-Lazy-Sum-Unsigned",
//        KZ3LazySumUnsigned::class to "Z3-Lazy-Sum-Unsigned",
        KCvc5LazySumUnsigned::class to "Cvc5-Lazy-Sum-Unsigned",
    ).uncheckedCast()

    @ParameterizedTest(name = "{0}")
    @MethodSource("bv2intTestData")
    fun benchmark(name: String, samplePath: Path) = solvers.forEach { (solverClass, configName) ->
        benchmarkSolver(name, samplePath, configName) { ctx ->
            solverManager.run {
                registerSolver(
                    solverClass,
                    getSolverConfigurationClass(configName)
                )

                createSolver(ctx, solverClass)
            }
        }
    }

    private fun <C : KSolverConfiguration> benchmarkSolver(
        name: String,
        samplePath: Path,
        configName: String,
        solverProvider: (KContext) -> KAsyncSolver<C>
    ) = handleIgnoredTests("bv2int-benchmark[$name]") {
        ignoreNoTestDataStub(name)
        val ctx = KContext()

        testWorkers.withWorker(ctx) { worker ->
            worker.skipBadTestCases {
                val assertions = worker.parseFile(samplePath)
                val ksmtAssertion = ctx.mkAnd(worker.convertAssertions(assertions))

                repeat(repeatNum) { repeatIdx ->
                    val res = solverProvider(ctx).use { solver ->
                        measureAssertTime(ksmtAssertion, solver, checkTimeout)
                    }

                    val resultRow =
                        "$name,$repeatIdx,$configName,${res.status},${res.checkTime},${res.assertTime},${res.roundCnt}"

                    println(resultRow)
                    outputFile.appendText("$resultRow\n")

                    if (res.roundCnt == -2) return@skipBadTestCases

                    if (res.status == KSolverStatus.UNKNOWN) return@skipBadTestCases
                }
            }
        }
    }

    private fun getSolverConfigurationClass(
        configName: String
    ) : KClass<KSolverConfiguration> =
        when {
            "Z3" in configName -> KZ3SolverUniversalConfiguration::class
            "Yices" in configName -> KYicesSolverUniversalConfiguration::class
            "Cvc5" in configName -> KCvc5SolverUniversalConfiguration::class
            else -> error("Unexpected")
        }.uncheckedCast()

    private fun <C : KSolverConfiguration> measureAssertTime(
        expr: KExpr<KBoolSort>,
        solver: KAsyncSolver<C>,
        timeout: Duration
    ): MeasureTimeResult {
        val status: KSolverStatus

        val assertTime = try {
            measureNanoTime {
                solver.assert(expr)
                status = solver.check(timeout)
            }
        } catch (_: Throwable) {
            return MeasureTimeResult(
                timeout.inWholeNanoseconds,
                timeout.inWholeNanoseconds,
                KSolverStatus.UNKNOWN,
                -2
            )
        }

        val checkTime = solver.getCheckTime(timeout.inWholeNanoseconds)
        val roundCnt = solver.getRoundCount(-1)

        return MeasureTimeResult(checkTime, assertTime, status, roundCnt)
    }

    private data class MeasureTimeResult(
        val checkTime: Long,
        val assertTime: Long,
        val status: KSolverStatus,
        val roundCnt: Int
    )

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
