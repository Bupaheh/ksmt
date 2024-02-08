package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort

@JvmInline
value class Bv2IntLemma internal constructor(private val value: Any) {
    val isLeaf: Boolean
        get() = value is KExpr<*>

    val isEmpty: Boolean
        get() = value === empty

    val isParentPair: Boolean
        get() = value is Pair<*, *>

    val isParentTriple: Boolean
        get() = value is Triple<*, *, *>

    val isParentList: Boolean
        get() = value is List<*>

    @Suppress("UNCHECKED_CAST")
    val getChildrenPair: Pair<Bv2IntLemma, Bv2IntLemma>
        get() = value as Pair<Bv2IntLemma, Bv2IntLemma>

    @Suppress("UNCHECKED_CAST")
    val getChildrenTriple: Triple<Bv2IntLemma, Bv2IntLemma, Bv2IntLemma>
        get() = value as Triple<Bv2IntLemma, Bv2IntLemma, Bv2IntLemma>

    @Suppress("UNCHECKED_CAST")
    val getChildrenList: List<Bv2IntLemma>
        get() = value as List<Bv2IntLemma>

    @Suppress("UNCHECKED_CAST")
    val getExpr: KExpr<KBoolSort>
        get() = value as KExpr<KBoolSort>

    fun merge(other: Bv2IntLemma): Bv2IntLemma {
        if (isEmpty) return other
        if (other.isEmpty) return this

        return Bv2IntLemma(this to other)
    }

    companion object {
        private val empty = Any()

        val EMPTY = Bv2IntLemma(empty)

        @JvmStatic
        fun fromExpr(expr: KExpr<KBoolSort>): Bv2IntLemma {
            if (expr == expr.ctx.trueExpr) return EMPTY

            return Bv2IntLemma(expr)
        }

        @JvmStatic
        fun from(l0: Bv2IntLemma, arg1: Bv2IntLemma): Bv2IntLemma = l0.merge(arg1)

        @JvmStatic
        fun from(arg0: Bv2IntLemma, arg1: Bv2IntLemma, arg2: Bv2IntLemma): Bv2IntLemma {
            when {
                arg0.isEmpty -> return arg1.merge(arg2)
                arg1.isEmpty -> return arg0.merge(arg2)
                arg2.isEmpty -> return arg0.merge(arg1)
            }

            return Bv2IntLemma(Triple(arg0, arg1, arg2))
        }

        @JvmStatic
        fun fromList(lemmas: List<Bv2IntLemma>): Bv2IntLemma {
//            if (lemmas.all { it.isEmpty }) return EMPTY

            return Bv2IntLemma(lemmas)
        }

        @JvmStatic
        inline fun fromListMap(list: List<KExpr<*>>, transform: (KExpr<*>) -> Bv2IntLemma): Bv2IntLemma {
            var size = 0
            var first = EMPTY
            var second = EMPTY
            var third = EMPTY
            var lemmas: MutableList<Bv2IntLemma>? = null

            list.forEach {
                val lemma = transform(it)
                if (lemma.isEmpty) return@forEach

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
                0 -> EMPTY
                1 -> first
                2 -> from(first, second)
                3 -> from(first, second, third)
                else -> fromList(lemmas!!)
            }
        }
    }
}

fun Bv2IntLemma.flatten(): List<KExpr<KBoolSort>> {
    val lemmas: MutableList<KExpr<KBoolSort>> = mutableListOf()
    val stack = ArrayList<Bv2IntLemma>()

    stack.add(this)

    while (stack.isNotEmpty()) {
        val e = stack.removeLast()

        when {
            e.isEmpty -> continue
            e.isLeaf -> lemmas.add(e.getExpr)
            e.isParentPair -> {
                val (n1, n2) = e.getChildrenPair
                stack.add(n1)
                stack.add(n2)
            }
            e.isParentTriple -> {
                val (n1, n2, n3) = e.getChildrenTriple
                stack.add(n1)
                stack.add(n2)
                stack.add(n3)
            }
            e.isParentList -> stack.addAll(e.getChildrenList)
        }
    }

    return lemmas
}