package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.rewrite.KExprSubstitutor
import io.ksmt.expr.rewrite.simplify.KExprSimplifier
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KIntSort
import io.ksmt.utils.mkConst
import io.ksmt.utils.powerOfTwo
import io.ksmt.utils.uncheckedCast

enum class Signedness {
    UNSIGNED,
    SIGNED,
}

fun KContext.getBounds(
    sizeBits: UInt,
    boundSignedness: Signedness
): Pair<KExpr<KIntSort>, KExpr<KIntSort>> {
    val lowerBound = 0.expr
    val upperBound = mkPowerOfTwoExpr(sizeBits) - 1.expr

    val diff = if (boundSignedness == Signedness.SIGNED) {
        -mkPowerOfTwoExpr(sizeBits - 1u)
    } else {
        0.expr
    }

    return Pair(lowerBound + diff, upperBound + diff)
}

fun KContext.toSignedness(
    value: KExpr<KIntSort>,
    sizeBits: UInt,
    valueSignedness: Signedness,
    signedness: Signedness
): KExpr<KIntSort> =
    when (signedness) {
        Signedness.UNSIGNED -> toUnsigned(value, sizeBits, valueSignedness)
        Signedness.SIGNED -> toSigned(value, sizeBits, valueSignedness)
    }

fun KContext.normalizeExpr(expr: KExpr<KIntSort>, signedness: Signedness, sizeBits: UInt): KExpr<KIntSort> =
    toSignedness(expr mod mkPowerOfTwoExpr(sizeBits), sizeBits, Signedness.UNSIGNED, signedness)

fun KContext.toSigned(
    value: KExpr<KIntSort>,
    sizeBits: UInt,
    valueSignedness: Signedness
): KExpr<KIntSort> =
    if (valueSignedness == Signedness.SIGNED) {
        value
    } else {
        unsignedToSigned(value, sizeBits)
    }

fun KContext.toUnsigned(
    value: KExpr<KIntSort>,
    sizeBits: UInt,
    valueSignedness: Signedness
): KExpr<KIntSort> =
    if (valueSignedness == Signedness.UNSIGNED) {
        value
    } else {
        signedToUnsigned(value, sizeBits)
    }

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

fun <T : KBvSort> KContext.evalBv(expr: KExpr<T>): KExpr<T> {
    val kVal = mkBv(-1, 64u)
    val sVal = mkBv(2199023255554, 64u)
    val yVal = mkBv(-4398046511105, 64u)

    val k = bv64Sort.mkConst("kjmfxzw")
    val s = bv64Sort.mkConst("sldjlemuku")
    val y = bv64Sort.mkConst("yybilyil")

    val consts = listOf(
        k to kVal,
        s to sVal,
        y to yVal,
    )


    val substituted = KExprSubstitutor(this).apply {
        consts.forEach { (c, v) ->
            substitute(c, v.uncheckedCast())
        }
    }.apply(expr)

    return KExprSimplifier(this).apply(substituted)
}

fun KContext.evalInt(expr: KExpr<KIntSort>): KExpr<KIntSort> {
    val kVal = mkBv(-1, 64u)
    val sVal = mkBv(2199023255554, 64u)
    val yVal = mkBv(-4398046511105, 64u)

    val k = bv64Sort.mkConst("kjmfxzw")
    val s = bv64Sort.mkConst("sldjlemuku")
    val y = bv64Sort.mkConst("yybilyil")

    val consts = listOf(
        k to kVal,
        s to sVal,
        y to yVal,
    )

    val substituted = KExprSubstitutor(this).apply {
        consts.forEach { (c, v) ->
//            substitute(c, mkBv2IntExpr(v, true))
        }
    }.apply(expr)

    return KExprSimplifier(this).apply(substituted)
}