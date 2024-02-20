package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast

typealias Bv2IntLemma = Any

val EMPTY_LEMMA = Any()

fun isEmptyLemma(lemma: Bv2IntLemma) = lemma === EMPTY_LEMMA

fun toLemma(expr: KExpr<KBoolSort>): Bv2IntLemma =
    if (expr == expr.ctx.trueExpr) {
        EMPTY_LEMMA
    } else {
        expr
    }

fun mergeLemmas(arg0: Bv2IntLemma, arg1: Bv2IntLemma): Bv2IntLemma =
    when {
        isEmptyLemma(arg0) -> arg1
        isEmptyLemma(arg1) -> arg0
        else -> arg0 to arg1
    }

fun mergeLemmas(arg0: Bv2IntLemma, arg1: Bv2IntLemma, arg2: Bv2IntLemma): Bv2IntLemma =
    when {
        isEmptyLemma(arg0) -> mergeLemmas(arg1, arg2)
        isEmptyLemma(arg1) -> mergeLemmas(arg0, arg2)
        isEmptyLemma(arg2) -> mergeLemmas(arg0, arg1)
        else -> Triple(arg0, arg1, arg2)
    }

inline fun mergeLemmas(list: List<KExpr<*>>, transform: (KExpr<*>) -> Bv2IntLemma): Bv2IntLemma {
    var size = 0
    var first = EMPTY_LEMMA
    var second = EMPTY_LEMMA
    var third = EMPTY_LEMMA
    var lemmas: MutableList<Bv2IntLemma>? = null

    list.forEach {
        val lemma = transform(it)
        if (isEmptyLemma(lemma)) return@forEach

        size++

        when (size) {
            1 -> first = lemma
            2 -> second = lemma
            3 -> third = lemma
            4 -> lemmas = mutableListOf(first, second, third, lemma)
            else -> lemmas!!.add(lemma)
        }
    }

    return when (size) {
        0 -> EMPTY_LEMMA
        1 -> first
        2 -> first to second
        3 -> Triple(first, second, third)
        else -> lemmas!!
    }
}

fun lemmaFlatten(root: Bv2IntLemma): List<KExpr<KBoolSort>> {
    val lemmas: MutableList<KExpr<*>> = mutableListOf()
    val stack = ArrayList<Bv2IntLemma>()

    stack.add(root)

    while (stack.isNotEmpty()) {
        when (val lemma = stack.removeLast()) {
            is KExpr<*> -> lemmas.add(lemma)
            is Pair<*, *> -> {
                stack.add(lemma.first.uncheckedCast())
                stack.add(lemma.second.uncheckedCast())
            }

            is Triple<*, *, *> -> {
                stack.add(lemma.first.uncheckedCast())
                stack.add(lemma.second.uncheckedCast())
                stack.add(lemma.third.uncheckedCast())
            }

            is List<*> -> stack.addAll(lemma.uncheckedCast())
        }
    }

    return lemmas.uncheckedCast()
}
