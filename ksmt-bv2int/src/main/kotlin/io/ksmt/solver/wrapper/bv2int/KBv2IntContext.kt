package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KExpr
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.uncheckedCast

class KBv2IntContext(ctx: KContext) {
    val bvAndFunc = with(ctx) { mkFreshFuncDecl("bvAnd", intSort, listOf(intSort, intSort)) }
    private val powerOfTwoFunc = with(ctx) { mkFreshFuncDecl("pow2", intSort, listOf(intSort)) }

    private val declarations = hashMapOf<KDecl<*>, KDecl<*>>()
    private val auxDecls = hashSetOf<KDecl<*>>(bvAndFunc, powerOfTwoFunc)
    private val declSignedness = hashMapOf<KDecl<*>, KBv2IntRewriter.Signedness>()

    fun saveDecl(originalDecl: KDecl<*>, rewrittenDecl: KDecl<*>, signedness: KBv2IntRewriter.Signedness) {
        declSignedness[rewrittenDecl] = signedness
        declarations[originalDecl] = rewrittenDecl
    }
    fun cachedDecl(decl: KDecl<*>): KDecl<KSort>? = declarations[decl].uncheckedCast()

    fun isAuxDecl(decl: KDecl<*>): Boolean = decl in auxDecls

    fun saveAuxDecl(decl: KDecl<*>): Boolean = auxDecls.add(decl)

    fun mkPowerOfTwoApp(power: KExpr<KIntSort>): KExpr<KIntSort> = powerOfTwoFunc.apply(listOf(power))
    fun mkBvAndApp(arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort>) = bvAndFunc.apply(listOf(arg0, arg1))

}