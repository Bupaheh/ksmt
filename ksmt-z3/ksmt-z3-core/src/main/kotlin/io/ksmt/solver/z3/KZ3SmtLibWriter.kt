package io.ksmt.solver.z3

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Native
import com.microsoft.z3.Z3Exception
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.parser.KSMTLibParseException
import io.ksmt.sort.KBoolSort
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class KZ3SmtLibWriter(private val ctx: KContext) {
    fun write(expr: KExpr<KBoolSort>): String =
        KZ3Context(ctx).use {
            Native.benchmarkToSmtlibString(
                it.nativeContext.nCtx(),
                "",
                "QF_ABV",
                "unknown",
                "",
                0,
                LongArray(0),
                it.internalizeAssertion(expr)
            )
        }

    private fun KZ3Context.internalizeAssertion(expr: KExpr<KBoolSort>): Long {
        val internalizer = KZ3ExprInternalizer(ctx, this)

        return with(internalizer) { expr.internalizeExpr() }
    }

    companion object {
        init {
            // ensure z3 native library is loaded
            KZ3Solver(KContext()).close()
        }
    }
}