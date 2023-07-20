package io.ksmt.solver.wrapper.bv2int

import kotlin.reflect.KFunction

fun bv2IntBenchmarkGeneratorFilter(function: KFunction<*>): Boolean = function.name in allGenerators

private val defaultGenerators = listOf(
    "mkBoolSort",
    "mkTrue",
    "mkFalse",
    "mkOr",
    "mkAnd",
    "mkXor",
    "mkImplies",
    "mkNot",

    "bvSortDefaultValue",
    "mkBv1Sort",
    "mkBv8Sort",
    "mkBv16Sort",
    "mkBv32Sort",
    "mkBv64Sort",
    "mkBvSort",
    "mkBv",

    "mkEq",
    "mkDistinct",
    "mkIte",
    "mkConst",

    "mkBvAddExpr",
    "mkBvSubExpr",
    "mkBvNegationExpr",
    "mkBvSignedGreaterExpr",
    "mkBvSignedGreaterOrEqualExpr",
    "mkBvSignedLessExpr",
    "mkBvSignedLessOrEqualExpr",
    "mkBvUnsignedGreaterExpr",
    "mkBvUnsignedGreaterOrEqualExpr",
    "mkBvUnsignedLessExpr",
    "mkBvUnsignedLessOrEqualExpr",
)

private val bvNiaGenerators = listOf(
    "mkBvMulExpr",
    "mkBvUnsignedDivExpr",
    "mkBvUnsignedRemExpr",
    "mkBvSignedDivExpr",
    "mkBvSignedModExpr",
    "mkBvSignedRemExpr",
    "mkBvZeroExtensionExpr",
    "mkBvSignExtensionExpr",
)

private val bvShiftGenerators = listOf(
    "mkBvShiftLeftExpr",
    "mkBvArithShiftRightExpr",
    "mkBvLogicalShiftRightExpr",
)

private val bvRotateGenerators = listOf(
    "mkBvRotateLeftExpr",
    "mkBvRotateLeftIndexedExpr",
    "mkBvRotateRightExpr",
    "mkBvRotateRightIndexedExpr",
    "mkBvConcatExpr",
    "mkBvExtractExpr",
)

private val bvBitwiseGenerators = listOf(
    "mkBvNotExpr",
    "mkBvAndExpr",
    "mkBvOrExpr",
    "mkBvXorExpr",
    "mkBvXNorExpr",
    "mkBvNAndExpr",
    "mkBvNorExpr",
)

private val bvWeirdGenerators = listOf(
    "mkBvRepeatExpr",
    "mkBvAddNoOverflowExpr",
    "mkBvAddNoUnderflowExpr",
    "mkBvSubNoOverflowExpr",
    "mkBvSubNoUnderflowExpr",
    "mkBvMulNoOverflowExpr",
    "mkBvMulNoUnderflowExpr",
    "mkBvDivNoOverflowExpr",
    "mkBvNegationNoOverflowExpr",
    "mkBvReductionAndExpr",
    "mkBvReductionOrExpr",
)

private val arrayGenerators = listOf(
    "arraySortDefaultValue",
    "mkArraySort",
    "mkArrayNSort",
    "mkArrayConst",
    "mkArraySelect",
    "mkArrayNSelect",
    "mkArrayStore",
    "mkArrayNStore"
)

private val allGenerators = (defaultGenerators + bvNiaGenerators + bvShiftGenerators + bvRotateGenerators +
        bvBitwiseGenerators + bvWeirdGenerators + arrayGenerators).toSet()

class Bv2IntBenchmarkWeightBuilder(private val defaultWeight: Double = 1.0) {
    private val weights = defaultGenerators.associateWith { defaultWeight }.toMutableMap()

    fun enableBvNia(weight: Double = defaultWeight) = apply {
        bvNiaGenerators.forEach { weights[it] = weight }
    }

    fun enableBvShift(weight: Double = defaultWeight) = apply {
        bvShiftGenerators.forEach { weights[it] = weight }
    }

    fun enableBvRotate(weight: Double = defaultWeight) = apply {
        bvRotateGenerators.forEach { weights[it] = weight }
    }

    fun enableBvBitwise(weight: Double = defaultWeight) = apply {
        bvBitwiseGenerators.forEach { weights[it] = weight }
    }

    fun enableBvWeird(weight: Double = defaultWeight) = apply {
        bvWeirdGenerators.forEach { weights[it] = weight }
    }

    fun enableArray(weight: Double = defaultWeight) = apply {
        arrayGenerators.forEach { weights[it] = weight }
    }

    fun setWeight(generator: String, weight: Double = defaultWeight) = apply {
        weights[generator] = weight
    }

    fun build(): Map<String, Double> {
        (allGenerators - weights.keys).forEach { weights[it] = 0.0 }

        return weights
    }
}
