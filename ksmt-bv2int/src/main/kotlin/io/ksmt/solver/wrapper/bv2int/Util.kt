package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KIntSort
import io.ksmt.utils.powerOfTwo

fun KContext.mkPowerOfTwoExpr(power: UInt): KExpr<KIntSort> = powerOfTwo(power).expr

fun KContext.mkIntExtractBit(arg: KExpr<KIntSort>, bit: UInt) = arg / mkPowerOfTwoExpr(bit) mod 2.expr

fun KContext.mkIntExtractBits(arg: KExpr<KIntSort>, high: UInt, low: UInt) =
    if (low > high) {
        0.expr
    } else {
        (arg mod mkPowerOfTwoExpr(high + 1u)) / mkPowerOfTwoExpr(low)
    }

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