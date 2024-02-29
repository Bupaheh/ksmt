package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KAndBinaryExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KIntNumExpr
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KSort
import io.ksmt.utils.uncheckedCast
import java.util.IdentityHashMap

class KBv2IntContext(val ctx: KContext) {
    val bvAndFunc = with(ctx) { mkFreshFuncDecl("bvAnd", intSort, listOf(intSort, intSort)) }
    val powerOfTwoFunc = with(ctx) { mkFreshFuncDecl("pow2", intSort, listOf(intSort)) }

    private val originalDeclarations = IdentityHashMap<KDecl<*>, KDecl<*>>()
    private val rewrittenDeclarations = IdentityHashMap<KDecl<*>, KDecl<*>>()
    private val auxDecls = hashSetOf<KDecl<*>>(bvAndFunc, powerOfTwoFunc)
    private val bvAndLemmaApplication = IdentityHashMap<KExpr<KBoolSort>, KExpr<KIntSort>>()

    val zero = ctx.mkIntNum(0)
    val one = ctx.mkIntNum(1)
    val minusOne = ctx.mkIntNum(-1)
    val two = ctx.mkIntNum(2)

    fun saveDecl(originalDecl: KDecl<*>, rewrittenDecl: KDecl<*>) {
        originalDeclarations[originalDecl] = rewrittenDecl
        rewrittenDeclarations[rewrittenDecl] = originalDecl
    }
    fun getRewrittenDeclaration(decl: KDecl<*>): KDecl<KSort>? = originalDeclarations[decl].uncheckedCast()
    fun getOriginalDeclaration(decl: KDecl<*>): KDecl<KSort>? = rewrittenDeclarations[decl].uncheckedCast()

    fun isAuxDecl(decl: KDecl<*>): Boolean = decl in auxDecls

    fun saveAuxDecl(decl: KDecl<*>): Boolean = auxDecls.add(decl)

    fun mkPowerOfTwoApp(power: KExpr<KIntSort>): KExpr<KIntSort> = with(ctx) {
        if (power is KIntNumExpr) {
            mkArithPower(2.expr, power)
        } else {
            powerOfTwoFunc.apply(listOf(power))
        }
    }

    fun mkBvAndApp(arg0: KExpr<KIntSort>, arg1: KExpr<KIntSort>) = bvAndFunc.apply(listOf(arg0, arg1))
    fun extractBvAndApplication(expr: KExpr<KBoolSort>) = bvAndLemmaApplication[expr]

    fun registerApplication(lemma: KExpr<KBoolSort>, application: KExpr<KIntSort>) {
        if (lemma !is KAndBinaryExpr) {
            bvAndLemmaApplication[lemma] = application
        } else {
            registerApplication(lemma.lhs, application)
            registerApplication(lemma.rhs, application)
        }
    }
}
