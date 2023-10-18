package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
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
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private fun writeExpressions(ctx: KContext, expressions: List<KExpr<KBoolSort>>, path: String) {
    val serializationCtx = AstSerializationCtx().apply { initCtx(ctx) }
    val marshaller = AstSerializationCtx.marshaller(serializationCtx)
    val emptyRdSerializationCtx = SerializationCtx(Serializers())
    val buffer = UnsafeBuffer(ByteArray(1000))

    expressions.forEach { expr ->
        marshaller.write(emptyRdSerializationCtx, buffer, expr)
    }

    File(path).writeBytes(buffer.getArray())
}

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

fun readSmtBenchmarkData(
    ctx: KContext,
    dirPath: String,
    begin: Int = 0,
    end: Int = 5000,
    limit: Int = 1000
): List<KExpr<KBoolSort>> {
    val parser = KZ3SMTLibParser(ctx)
    val expressions = mutableListOf<KExpr<KBoolSort>>()
    val paths = File(dirPath).walk()
        .filter { it.extension == "smt2" }
        .toList()
        .shuffled(Random(1))
        .mapIndexed { index, path -> index to path }
        .filter { (idx, _) -> idx in begin..end && idx != 44839 && idx != 1351 }

    paths.forEach { (idx, file) ->
        println("$idx\t${expressions.size}")

        val expr = ctx.mkAnd(parser.parse(file.toPath()))

//        val decls = KDeclCounter(ctx).countDeclarations(expr)
//        val liaCnt = countOperations(decls, liaDecls)
//        val niaCnt = countOperations(decls, niaDecls)
//        val weirdCnt = countOperations(decls, weirdDecls)
//        val shiftCnt = countOperations(decls, shiftDecls)
//        val bitCnt = countOperations(decls, bitwiseDecls)
//
//        val flag = decls.keys.all { it in allDecls }
//                && liaCnt > 7
//                && bitCnt > 0
//                && liaCnt * 0.5 > bitCnt
//                && liaCnt * 0.5 > weirdCnt
//                && liaCnt * 0.5 > shiftCnt

        if (true) {
            expressions.add(expr)
        }
    }

    return expressions
}

fun testYices() = with(KContext()) {
    KYicesSolver(this).use { solver ->

        val a by intSort

        solver.push()

        val expr = (a * mkPowerOfTwoExpr(2u) mod 3.expr) eq 5.expr
        solver.assert(expr)
        println(solver.check())

        solver.pop()

        solver.assert(a * mkPowerOfTwoExpr(2u) eq 5.expr)
        solver.assert(bv8Sort.mkConst("kek") eq mkBv(1, 8u).uncheckedCast())
        println(solver.check())
    }
}



fun main() = with(KContext()) {
//    val ctx = KContext()
//    val expressions = ctx.readFormulas(File("generatedExpressions/QF_BV_wliac"))
//    val rewriter = KUnsignedToSignedBvRewriter(ctx)
//
//    val signedExpressions = expressions.filter {
//        val decls = KDeclCounter(ctx).countDeclarations(it)
//        countOperations(decls, liaDecls) > 5
//    }
//
//    writeExpressions(ctx, signedExpressions, "generatedExpressions/QF_BV_wliac")

    val ids = 0..3
    val exprs = mutableListOf<KExpr<KBoolSort>>()
    val ctx = KContext()
    val rewriter = KUnsignedToSignedBvRewriter(ctx)
    for (i in ids) {
        println(i)
        val ex = ctx.readFormulas(File("generatedExpressions/bv/QF_BV$i"))
        exprs.addAll(ex)
    }

    writeExpressions(ctx, exprs, "generatedExpressions/QF_BV_2000")
    return
//    val ctx = KContext()
//    val exprs = ctx.readFormulas(File("generatedExpressions/wbslia/QF_BV_wbslia"))
//
//    val expressions = exprs.filter {
//        val decls = KDeclCounter(ctx).countDeclarations(it)
//        val liaCnt = countOperations(decls, liaDecls)
//        val bitCnt = countOperations(decls, bitwiseDecls)
//        val shiftCnt = countOperations(decls, shiftDecls)
//        val weirdCnt = countOperations(decls, weirdDecls)
//        val threshold = 0.2
//        val flag = decls.keys.all { it in allDecls } && bitCnt > 0 &&
//                liaCnt * threshold > bitCnt
////                && weirdCnt < 5
////                && liaCnt * threshold > weirdCnt
////                liaCnt * threshold > shiftCnt
//        flag
//    }
//
//    println(expressions.size)
//    writeExpressions(ctx, expressions, "generatedExpressions/QF_BV_wbslia")

//    var satCnt = 0
//
//    KYicesSolver(ctx).use { solver ->
//        expressions.forEachIndexed { idx, expr ->
//            println("$idx/${expressions.size - 1}")
//            solver.push()
//            solver.assert(expr)
//            if (solver.check(2.seconds) == KSolverStatus.SAT) satCnt++
//
//            solver.pop()
//        }
//    }
//
//    println(satCnt)
//    println(expressions.size)

//    writeExpressions(ctx, expressions, "generatedExpressions/QF_BV_bslia2")


    for (i in 2..92) {
        val ctx = KContext()
        val step = 500
        val exprs = readSmtBenchmarkData(
            ctx,
            "/home/pvl/Heh/Projects/QF_BV",
            begin = step * i,
            end = step * (i + 1),
            limit = 10000
        )

        writeExpressions(ctx, exprs, "generatedExpressions/bv/QF_BV$i")
    }

//    val params = GenerationParameters(
//        seedExpressionsPerSort = 20,
//        possibleIntValues = 2..64,
//        deepExpressionProbability = 0.3,
//        generatedListSize = 2..3,
//        astFilter = Bv2IntAstFilter()
//    )
//    val weights = Bv2IntBenchmarkWeightBuilder()
//        .enableBvCmp(3.7)
//        .setWeight("mkBvUnsignedGreaterExpr", 0.3)
//        .setWeight("mkBvUnsignedGreaterOrEqualExpr", 0.3)
//        .setWeight("mkBvUnsignedLessExpr", 0.3)
//        .setWeight("mkBvUnsignedLessOrEqualExpr", 0.3)
//        .enableBvLia(10.0)
//        .enableBvNia(0.65)
////        .enableArray(1.25)
////        .enableBvBitwise(0.25)
////        .enableBvWeird(0.875)
////        .enableBvShift(1.0)
//        .build()
//    // mkBvSort disabled
//    val expressions = generateRandomExpressions(
//        size = 5000,
//        batchSize = 1000,
//        params = params,
//        random = Random(55),
//        weights = weights,
//        isVerbose = true,
//        predicate = { expr ->
//            niaDecls.any { decl ->
//                KDeclCounter(this).countDeclarations(expr).getOrDefault(decl, 0) > 0
//            }
//        }
//    )
//
//
//    writeExpressions(this, expressions, "generatedExpressions/1Snia")
}