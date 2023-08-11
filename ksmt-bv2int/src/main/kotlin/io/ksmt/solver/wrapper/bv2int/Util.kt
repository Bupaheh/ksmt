package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KIntSort
import io.ksmt.utils.powerOfTwo

fun KContext.mkPowerOfTwoExpr(power: UInt): KExpr<KIntSort> = powerOfTwo(power).expr

fun KContext.unsignedToSigned(value: KExpr<KIntSort>, sizeBits: UInt): KExpr<KIntSort> =
    mkIte(
        value lt mkPowerOfTwoExpr(sizeBits - 1u),
        value,
        value - mkPowerOfTwoExpr(sizeBits)
    )

fun KContext.signedToUnsigned(value: KExpr<KIntSort>, sizeBits: UInt): KExpr<KIntSort> =
    mkIte(
        value lt 0.expr,
        mkPowerOfTwoExpr(sizeBits) + value,
        value
    )