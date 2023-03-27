package org.ksmt.test.benchmarks

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.ksmt.solver.cvc5.KCvc5Solver
import java.nio.file.Path

class Cvc5BenchmarksBasedTest : BenchmarksBasedTest() {

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("cvc5TestData")
    fun testSolver(name: String, samplePath: Path) = testSolver(name, samplePath, KCvc5Solver::class)

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("cvc5TestData")
    fun testModelConversion(name: String, samplePath: Path) = testModelConversion(name, samplePath, KCvc5Solver::class)

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}")
    @MethodSource("cvc5TestData")
    fun testConverter(name: String, samplePath: Path) = testConverter(name, samplePath) { assertions ->
        internalizeAndConvertCvc5(assertions)
    }

    companion object {
        @JvmStatic
        fun cvc5TestData() = testData
            .filter { it.name !in KnownCvc5Issues.fp64CrashSamples }
            .ensureNotEmpty()
    }
}

object KnownCvc5Issues {
    /**
     * On these benchmarks cvc5 crashes due to fp 64 rem operation (both Windows, and Linux, and using java-api)
     * TODO: remove it after fix
     */
    val fp64CrashSamples = setOf(
        "FP_11_53_RNA_fp.rem_fp.geq0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isInfinite0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isInfinite1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNegative0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNormal1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isPositive0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isPositive1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isSubnormal1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isZero0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isZero1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isInfinite1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNormal0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNormal1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isPositive0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isPositive1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isSubnormal1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isZero0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isZero1.smt2",
        "FP_11_53_RNE_fp.rem_fp.leq0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isInfinite0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNaN0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isPositive1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isZero1.smt2",
        "FP_11_53_RTN_fp.rem_fp.leq0.smt2",
        "FP_11_53_RTP_fp.rem_fp.geq0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNaN0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNegative0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNegative1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNormal0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNormal1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isPositive0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isSubnormal1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isZero0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.geq0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isInfinite1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNaN0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNaN1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNegative0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNormal1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isPositive0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isPositive1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isSubnormal1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isZero0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isZero1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.leq0.smt2",
        "FP_3_5_RNA_fp.rem_fp.geq0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isInfinite0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNaN1.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNegative0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNegative1.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNormal1.smt2",
        "FP_3_5_RNA_fp.rem_fp.isPositive0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isPositive1.smt2",
        "FP_3_5_RNA_fp.rem_fp.isZero0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isZero1.smt2",
        "FP_3_5_RNA_fp.rem_fp.leq0.smt2",
        "FP_3_5_RNE_fp.rem_fp.geq0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isInfinite0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNaN1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNegative0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNegative1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isSubnormal1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isZero0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isZero1.smt2",
        "FP_3_5_RTN_fp.rem_fp.geq0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isInfinite0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isInfinite1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNaN0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNaN1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNegative0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNegative1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNormal1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isPositive0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isPositive1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isSubnormal0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isSubnormal1.smt2",
        "FP_3_5_RTN_fp.rem_fp.isZero0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isZero1.smt2",
        "FP_3_5_RTN_fp.rem_fp.leq0.smt2",
        "FP_3_5_RTP_fp.rem_fp.geq0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNaN1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNegative0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNegative1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNormal1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isPositive0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isPositive1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isSubnormal0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isSubnormal1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isZero0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.geq0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNaN0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNegative0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isPositive1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isSubnormal0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isSubnormal1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.leq0.smt2",
        "FP_8_24_RNA_fp.rem_fp.geq0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNaN0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNaN1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNegative0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNegative1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isPositive0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isPositive1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isSubnormal1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isZero0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RNE_fp.rem_fp.geq0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isInfinite0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isInfinite1.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNaN0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNaN1.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNegative0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNegative1.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNormal1.smt2",
        "FP_8_24_RNE_fp.rem_fp.isPositive0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isSubnormal1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isInfinite0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNaN0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNaN1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNegative0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNegative1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNormal1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isPositive0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isSubnormal0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isSubnormal1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RTN_fp.rem_fp.leq0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isInfinite1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNaN0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNaN1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNegative0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNormal1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isPositive0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isPositive1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isSubnormal0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isSubnormal1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RTP_fp.rem_fp.leq0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isInfinite0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isInfinite1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNaN1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNegative0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNegative1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNormal0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isPositive1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isSubnormal1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isZero0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.leq0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNaN0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNaN1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNegative1.smt2",
        "FP_11_53_RNA_fp.rem_fp.isNormal0.smt2",
        "FP_11_53_RNA_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RNA_fp.rem_fp.leq0.smt2",
        "FP_11_53_RNE_fp.rem_fp.geq0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isInfinite0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNaN0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNaN1.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNegative0.smt2",
        "FP_11_53_RNE_fp.rem_fp.isNegative1.smt2",
        "FP_11_53_RTN_fp.rem_fp.geq0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isInfinite1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNaN1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNegative0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNegative1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNormal0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isNormal1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isPositive0.smt2",
        "FP_11_53_RTN_fp.rem_fp.isSubnormal1.smt2",
        "FP_11_53_RTN_fp.rem_fp.isZero0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isInfinite0.smt2",
        "FP_11_53_RTP_fp.rem_fp.isInfinite1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isNaN1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isPositive1.smt2",
        "FP_11_53_RTP_fp.rem_fp.isZero1.smt2",
        "FP_11_53_RTP_fp.rem_fp.leq0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isInfinite0.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNegative1.smt2",
        "FP_11_53_RTZ_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isInfinite1.smt2",
        "FP_3_5_RNA_fp.rem_fp.isNaN0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isSubnormal0.smt2",
        "FP_3_5_RNA_fp.rem_fp.isSubnormal1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isInfinite1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNaN0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isNormal1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isPositive0.smt2",
        "FP_3_5_RNE_fp.rem_fp.isPositive1.smt2",
        "FP_3_5_RNE_fp.rem_fp.isSubnormal0.smt2",
        "FP_3_5_RNE_fp.rem_fp.leq0.smt2",
        "FP_3_5_RTN_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isInfinite0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isInfinite1.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNaN0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RTP_fp.rem_fp.isZero1.smt2",
        "FP_3_5_RTP_fp.rem_fp.leq0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isInfinite0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isInfinite1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNaN1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNegative1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNormal0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isNormal1.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isPositive0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isZero0.smt2",
        "FP_3_5_RTZ_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isInfinite0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isInfinite1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNormal0.smt2",
        "FP_8_24_RNA_fp.rem_fp.isNormal1.smt2",
        "FP_8_24_RNA_fp.rem_fp.isSubnormal0.smt2",
        "FP_8_24_RNA_fp.rem_fp.leq0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isNormal0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isPositive1.smt2",
        "FP_8_24_RNE_fp.rem_fp.isSubnormal0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isZero0.smt2",
        "FP_8_24_RNE_fp.rem_fp.isZero1.smt2",
        "FP_8_24_RNE_fp.rem_fp.leq0.smt2",
        "FP_8_24_RTN_fp.rem_fp.geq0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isInfinite1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isNormal0.smt2",
        "FP_8_24_RTN_fp.rem_fp.isPositive1.smt2",
        "FP_8_24_RTN_fp.rem_fp.isZero0.smt2",
        "FP_8_24_RTP_fp.rem_fp.geq0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isInfinite0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNegative1.smt2",
        "FP_8_24_RTP_fp.rem_fp.isNormal0.smt2",
        "FP_8_24_RTP_fp.rem_fp.isZero0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.geq0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNaN0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isNormal1.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isPositive0.smt2",
        "FP_8_24_RTZ_fp.rem_fp.isSubnormal0.smt2",
        "FP_11_53_RNA_fp.rem_=0.smt2",
        "FP_11_53_RNA_fp.rem_distinct0.smt2",
        "FP_11_53_RNA_fp.rem_distinct1.smt2",
        "FP_11_53_RNE_fp.rem_=0.smt2",
        "FP_11_53_RNE_fp.rem_distinct0.smt2",
        "FP_11_53_RNE_fp.rem_distinct1.smt2",
        "FP_11_53_RTN_fp.rem_=0.smt2",
        "FP_11_53_RTN_fp.rem_distinct0.smt2",
        "FP_11_53_RTN_fp.rem_distinct1.smt2",
        "FP_11_53_RTP_fp.rem_distinct0.smt2",
        "FP_11_53_RTZ_fp.rem_=0.smt2",
        "FP_11_53_RTZ_fp.rem_distinct0.smt2",
        "FP_11_53_RTZ_fp.rem_distinct1.smt2",
    )
}