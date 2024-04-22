package io.ksmt.test

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.uncheckedCast
import java.io.File
import kotlin.test.Test

class tempClass {
    fun KContext.readFormulas(dir: File): List<KExpr<KBoolSort>> {
        val files = dir.listFiles()?.filter { !it.isDirectory } ?: error("empty folder")
        val file = files.first()

        val srcSerializationCtx = AstSerializationCtx().apply { initCtx(this@readFormulas) }
        val srcMarshaller = AstSerializationCtx.marshaller(srcSerializationCtx)
        val emptyRdSerializationCtx = SerializationCtx(Serializers())
        val buffer = UnsafeBuffer(file.readBytes())
        val expressions: MutableList<KExpr<KBoolSort>> = mutableListOf()

        while (true) {
            try {
                expressions.add(srcMarshaller.read(emptyRdSerializationCtx, buffer).uncheckedCast())
            } catch (e : Exception) {
                break
            }
        }

        return expressions
    }

    @Test
    fun test(): Unit = with(KContext()) {
        val exprs = readFormulas(File("testExprs"))
        val expr1 = exprs[0]
        val expr2 = exprs[1]

        KZ3Solver(this).use { solver ->
            solver.assertAndTrack(expr1)
            solver.assert(expr2)
            solver.checkWithAssumptions(emptyList()).also { println(it) }
        }

        KZ3Solver(this).use { solver ->
            solver.assert(expr2)
            solver.assertAndTrack(expr1)
            solver.checkWithAssumptions(emptyList()).also { println(it) }
        }
    }
}