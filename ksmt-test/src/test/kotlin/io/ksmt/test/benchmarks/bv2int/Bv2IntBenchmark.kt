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
    private val checkTimeout =  24.seconds
    private val repeatNum = 3

    private val solversToBenchmark = System.getenv("bv2intSolvers")
        .orEmpty()
        .split(',')
        .map { solverName -> solverName.trim { it.isWhitespace() || it == '"' } }
        .toSet()

    private val solvers: List<Pair<KClass<KSolver<KSolverConfiguration>>, String>> = listOf(
        KYicesSolverBench::class to "Yices",
        KZ3SolverBench::class to "Z3",
        KCvc5SolverBench::class to "Cvc5",
        KYicesLazySumSignedLazyOverflowUnsignedUnsat::class to "Yices-Lazy-Sum-SignedLazyOverflow-UnsignedUnsat",
        KYicesLazySumSignedLazyOverflowSignedUnsat::class to "Yices-Lazy-Sum-SignedLazyOverflow-SignedUnsat",
        KZ3LazySumSignedLazyOverflowSignedUnsat::class to "Z3-Lazy-Sum-SignedLazyOverflow-SignedUnsat",
        KZ3LazySumSignedLazyOverflowUnsignedUnsat::class to "Z3-Lazy-Sum-SignedLazyOverflow-UnsignedUnsat",
        KCvc5LazySumSignedLazyOverflow::class to "Cvc5-Lazy-Sum-SignedLazyOverflow",
        KYicesLazySumSigned::class to "Yices-Lazy-Sum-Signed",
        KZ3LazySumSigned::class to "Z3-Lazy-Sum-Signed",
        KCvc5LazySumSigned::class to "Cvc5-Lazy-Sum-Signed",
        KYicesLazySumUnsigned::class to "Yices-Lazy-Sum-Unsigned",
        KZ3LazySumUnsigned::class to "Z3-Lazy-Sum-Unsigned",
        KCvc5LazySumUnsigned::class to "Cvc5-Lazy-Sum-Unsigned",
        KYicesEagerSumSignedLazyOverflow::class to "Yices-Eager-Sum-SignedLazyOverflow",
        KZ3EagerSumSignedLazyOverflow::class to "Z3-Eager-Sum-SignedLazyOverflow",
        KCvc5EagerSumSignedLazyOverflow::class to "Cvc5-Eager-Sum-SignedLazyOverflow",
        KZ3EagerSumSigned::class to "Z3-Eager-Sum-Signed",
        KYicesEagerSumUnsigned::class to "Yices-Eager-Sum-Unsigned",
        KCvc5EagerSumUnsigned::class to "Cvc5-Eager-Sum-Unsigned",

        KYicesEagerSumSignedLazyOverflowOriginalUnsat::class to "Yices-Eager-Sum-SignedLazyOverflow-OriginalUnsat",
        KYicesEagerSumSignedLazyOverflowOriginalUnsatSplit::class to "Yices-Eager-Sum-SignedLazyOverflow-OriginalUnsat-Split",
        KYicesEagerSumSignedLazyOverflow::class to "Yices-Eager-Sum-SignedLazyOverflow",
        KYicesEagerSumUnsigned::class to "Yices-Eager-Sum-Unsigned",
    ).filter { (_, name) -> name in solversToBenchmark }.uncheckedCast()

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

                    val status = if (res.roundCnt == -2) res.message.toString().replace(',', ' ') else res.status

                    val resultRow =
                        "$name,$repeatIdx,$configName,$status,${res.checkTime},${res.assertTime},${res.roundCnt}"

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
        } catch (e: Throwable) {
            return MeasureTimeResult(
                timeout.inWholeNanoseconds,
                timeout.inWholeNanoseconds,
                KSolverStatus.UNKNOWN,
                -2,
                e.toString().replace("\n", "").replace(",", "")
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
        val roundCnt: Int,
        val message: String? = null
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
        fun bv2intTestData() = testDataShuffled { it.startsWith("bv2int-") }
    }
}
