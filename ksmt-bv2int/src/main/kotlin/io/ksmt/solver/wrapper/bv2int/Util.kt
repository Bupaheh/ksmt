package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KIntSort
import io.ksmt.utils.powerOfTwo

fun KContext.mkPowerOfTwoExpr(power: UInt): KExpr<KIntSort> = powerOfTwo(power).expr
