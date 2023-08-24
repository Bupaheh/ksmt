package io.ksmt.solver.wrapper.bv2int

class Bv2IntBenchmarkWeightBuilder(private val defaultWeight: Double = 1.0) {
    private val weights = defaultGenerators.associateWith { defaultWeight }.toMutableMap()

    fun enableBvCmp(weight: Double = defaultWeight) = apply {
        bvCmpGenerators.forEach { weights[it] = weight }
    }

    fun enableBvLia(weight: Double = defaultWeight) = apply {
        bvLiaGenerators.forEach { weights[it] = weight }
    }

    fun enableBvNia(weight: Double = defaultWeight) = apply {
        bvNiaGenerators.forEach { weights[it] = weight }
    }

    fun enableBvBitwise(weight: Double = defaultWeight) = apply {
        bvBitwiseGenerators.forEach { weights[it] = weight }
    }

    fun enableBvShift(weight: Double = defaultWeight) = apply {
        bvShiftGenerators.forEach { weights[it] = weight }
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

    companion object {
        private val initialSeedGenerators = listOf(
            "mkBoolSort",
//            "mkTrue",
//            "mkFalse",
//            "mkBv1Sort",
//            "mkBv8Sort",
//            "mkBv16Sort",
            "mkBv32Sort",
            "mkBv64Sort",
            "mkBv100Sort"
        )

        private val defaultGenerators = listOf(
            "mkOr",
            "mkAnd",
            "mkXor",
            "mkImplies",
            "mkNot",

//            "mkBvSort",
            "mkBv",

            "mkEq",
            "mkDistinct",
            "mkIte",
            "mkConst",
        )

        private val bvCmpGenerators = listOf(
            "mkBvSignedGreaterExpr",
            "mkBvSignedGreaterOrEqualExpr",
            "mkBvSignedLessExpr",
            "mkBvSignedLessOrEqualExpr",
            "mkBvUnsignedGreaterExpr",
            "mkBvUnsignedGreaterOrEqualExpr",
            "mkBvUnsignedLessExpr",
            "mkBvUnsignedLessOrEqualExpr",
        )

        private val bvLiaGenerators = listOf(
            "mkBvAddExpr",
            "mkBvSubExpr",
            "mkBvNegationExpr",
        )

        private val bvNiaGenerators = listOf(
            "mkBvMulExpr",
            "mkBvUnsignedDivExpr",
            "mkBvUnsignedRemExpr",
            "mkBvSignedDivExpr",
            "mkBvSignedModExpr",
            "mkBvSignedRemExpr"
        )

        private val bvBitwiseGenerators = listOf(
//            "mkBvNotExpr",
            "mkBvAndExpr",
//            "mkBvOrExpr",
//            "mkBvXorExpr",
//            "mkBvXNorExpr",
//            "mkBvNAndExpr",
//            "mkBvNorExpr",
        )

        private val bvShiftGenerators = listOf(
            "mkBvShiftLeftExpr",
            "mkBvArithShiftRightExpr",
            "mkBvLogicalShiftRightExpr",
        )

        private val bvWeirdGenerators = listOf(
            "mkBvZeroExtensionExpr",
            "mkBvSignExtensionExpr",
            "mkBvConcatExpr",
            "mkBvExtractExpr",
        )

//        private val bvRotateGenerators = listOf(
//            "mkBvRotateLeftExpr",
//            "mkBvRotateLeftIndexedExpr",
//            "mkBvRotateRightExpr",
//            "mkBvRotateRightIndexedExpr",
//        )
//


//        private val bvWeirdGenerators = listOf(
//            "mkBvRepeatExpr",
//            "mkBvAddNoOverflowExpr",
//            "mkBvAddNoUnderflowExpr",
//            "mkBvSubNoOverflowExpr",
//            "mkBvSubNoUnderflowExpr",
//            "mkBvMulNoOverflowExpr",
//            "mkBvMulNoUnderflowExpr",
//            "mkBvDivNoOverflowExpr",
//            "mkBvNegationNoOverflowExpr",
//            "mkBvReductionAndExpr",
//            "mkBvReductionOrExpr",
//        )

        private val arrayGenerators = listOf(
//            "mkArrayConst",
            "mkArraySort",
            "mkArraySelect",
            "mkArrayStore",
//            "mkArrayNSort",
//            "mkArrayNSelect",
//            "mkArrayNStore"
        )

        val allGenerators = (initialSeedGenerators + defaultGenerators + bvCmpGenerators + bvLiaGenerators +
                bvNiaGenerators + bvShiftGenerators + bvBitwiseGenerators + bvWeirdGenerators + arrayGenerators).toSet()
    }
}