package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.sort.KBoolSort
import io.ksmt.test.GenerationParameters
import io.ksmt.utils.getValue
import io.ksmt.utils.mkConst
import io.ksmt.utils.uncheckedCast
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

val generalDecls = setOf(
    "or", "and", "not", "implies", "xor", "=", "distinct", "ite",
    "store", "select", "const",
    "bvult", "bvslt", "bvsle", "bvule", "bvuge", "bvsge", "bvugt", "bvsgt"
)

val liaDecls = setOf(
    "bvneg", "bvadd", "bvsub",
    "zero_extend", "sign_extend",
    "bvmulC",
//    "bvshlC", "bvlshrC", "bvashrC"
//    "bvmul", "bvudiv", "bvsdiv", "bvurem", "bvsrem", "bvsmod",
)

val weirdDecls = setOf(
//    "zero_extend", "sign_extend",
    "concat", "extract"
)

val niaDecls = setOf(
    "bvmul", "bvudiv", "bvsdiv", "bvurem", "bvsrem", "bvsmod"
)

val bitwiseDecls = setOf(
    "bvnot",
    "bvand", "bvor", "bvxor", "bvnor", "bvnand", "bvxnor"
)

val shiftDecls = setOf(
    "bvshlC", "bvlshrC", "bvashrC",
//    "bvshl", "bvlshr", "bvashr"
)

val allDecls = generalDecls + liaDecls + weirdDecls + shiftDecls + bitwiseDecls

fun countOperations(decls: HashMap<String, Int>, operations: Set<String>): Int {
    return operations.fold(0) { acc, el -> acc + decls.getOrDefault(el, 0) }
}

fun KContext.filterExpressions() {
    val nobitExprs = File("generatedExpressions/QF_BV_UNBIT")
        .walkBottomUp()
        .filter { it.extension == "smt2" }
        .toList()
        .map { it.path.toString().substringAfterLast('/') }
        .toSet()

    val smtExprs = File("generatedExpressions/QF_BV/SMTcomp2022Exprs.txt")
        .readLines()
        .map { path ->
            path.removePrefix("/non-incremental/")
        }.filter {
            val normalized = it.removePrefix("QF_BV/")
                .replace('/', '_')

            normalized !in nobitExprs
        }.toSet()

    for (i in 71..94) {
        val ctx = KContext()
        val step = 500
        val exprs = readFilterSmtBenchmarkData(
            ctx,
            "/home/pvl/Heh/Projects/QF_BV",
            begin = step * i,
            end = step * (i + 1) - 1,
            filterExpr = { path ->
                val normalizedPath = path.removePrefix("/home/pvl/Heh/Projects/")

                normalizedPath in smtExprs
            }
        )

//        writeExpressions(ctx, exprs, "generatedExpressions/QF_BV/QF_BV$i")
    }
}


fun main() = with(KContext()) {
    val dirPath: String = "generatedExpressions/QF_BV_BIT/"
    val begin: Int = 7001
    val end: Int = 7000

    val files = File(dirPath).listFiles()!!
        .mapIndexed { id, file -> id to file }
        .filter { (id, _) -> id in begin..end }

    val parser = KZ3SMTLibParser(this)

    files.forEach{ (id, file) ->
        println(id)

        val expr = mkAnd(parser.parse(file.toPath()))
        val splitter = KBv2IntSplitter(this)

        splitter.apply(expr)

        val roots = splitter.dsu.getRoots()

        val flag = roots.count { !splitter.dsu.isMarked(it) } > 0

        if (flag) {
            val toDir = "generatedExpressions/QF_BV_BITS/"
            val fileName = "bv2int-bits-" + file.path.removePrefix(dirPath)
            file.copyTo(File(toDir + fileName), overwrite = true)
        }
    }
}
