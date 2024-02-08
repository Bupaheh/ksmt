package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KExpr

class DisjointSetUnion {
    private val parent = hashMapOf<KExpr<*>, KExpr<*>>()
    private val size = hashMapOf<KExpr<*>, UInt>()
    private val mark = hashMapOf<KExpr<*>, Boolean>()

    fun find(a: KExpr<*>): KExpr<*> {
        if (parent.getOrPut(a) { a } == a) return a

        val nodes = mutableListOf<KExpr<*>>()
        var cur = a

        while (true) {
            val next = parent.getValue(cur)
            nodes.add(cur)

            if (next == cur) break

            cur = next
        }

        nodes.forEach { parent[it] = cur }

        return cur
    }

    fun union(a: KExpr<*>, b: KExpr<*>) {
        val aRoot = find(a)
        val bRoot = find(b)

        if (aRoot == bRoot) return

        val aSize = size.getOrDefault(aRoot, 1u)
        val bSize = size.getOrDefault(bRoot, 1u)
        val aMark = mark.getOrDefault(aRoot, false)
        val bMark = mark.getOrDefault(bRoot, false)

        val (child, root) = if (aSize < bSize) aRoot to bRoot else bRoot to aRoot

        parent[child] = root
        size[root] = aSize + bSize
        mark[root] = aMark || bMark
    }

    fun mark(a: KExpr<*>) {
        mark[find(a)] = true
    }

    fun isMarked(a: KExpr<*>) =
        mark.getOrDefault(find(a), false)
    fun getRoots() = parent.keys.filter { parent[it] == it }

    fun clear() {
        parent.clear()
        size.clear()
        mark.clear()
    }
}