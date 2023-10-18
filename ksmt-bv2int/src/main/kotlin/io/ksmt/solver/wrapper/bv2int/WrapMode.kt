package io.ksmt.solver.wrapper.bv2int

sealed interface WrapMode

data class Normalized(val signedness: Signedness) : WrapMode

object Denormalized : WrapMode

object None : WrapMode


