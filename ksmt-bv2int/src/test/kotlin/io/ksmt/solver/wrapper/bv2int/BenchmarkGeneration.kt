package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.sort.KBoolSort
import io.ksmt.test.GenerationParameters
import java.io.File
import kotlin.random.Random

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
    "bvneg", "bvadd", "bvsub"
)

val weirdDecls = setOf(
    "sign_extend", "zero_extend", "concat", "extract"
)

val niaDecls = setOf(
    "bvmul", "bvudiv", "bvsdiv", "bvurem", "bvsrem", "bvsmod"
)

val bitwiseDecls = setOf(
    "bvnot", "bvand", "bvor", "bvxor", "bvnor", "bvnand", "bvxnor"
)

val shiftDecls = setOf(
    "bvshl", "bvlshr", "bvashr"
)

val allDecls = generalDecls + liaDecls + weirdDecls + shiftDecls + niaDecls

fun readSmtBenchmarkData(
    ctx: KContext,
    dirPath: String,
    begin: Int = 0,
    end: Int = 5000,
    limit: Int = 1000
): List<KExpr<KBoolSort>> {
    val parser = KZ3SMTLibParser(ctx)
//    val expressions = ctx.readFormulas(File("generatedExpressions/SMTlia3")).toMutableList()
    val expressions = mutableListOf<KExpr<KBoolSort>>()
    val paths = File(dirPath).walk()
        .shuffled(Random(1))
        .mapIndexed { index, path -> index to path }
        .filter { (idx, _) -> idx in begin..end }

    paths.forEach { (idx, file) ->
        println("$idx\t${expressions.size}")

        val parsed = parser.parse(file.toPath()).filter { expr ->
            val decls = KDeclCounter(ctx).countDeclarations(expr)

            (decls.keys.all { it in allDecls }
                    && shiftDecls.fold(0) { acc, el -> acc + decls.getOrDefault(el, 0) } > 0)
        }

        expressions.addAll(parsed)

        if (expressions.size > limit) {
            return expressions
        }
    }

    return expressions
}

fun main() = with(KContext()) {
    val params = GenerationParameters(
        seedExpressionsPerSort = 20,
        possibleIntValues = 2..64,
        deepExpressionProbability = 0.3,
        generatedListSize = 2..3,
        astFilter = Bv2IntAstFilter()
    )
    val weights = Bv2IntBenchmarkWeightBuilder()
        .enableBvCmp(3.7)
        .setWeight("mkBvUnsignedGreaterExpr", 0.3)
        .setWeight("mkBvUnsignedGreaterOrEqualExpr", 0.3)
        .setWeight("mkBvUnsignedLessExpr", 0.3)
        .setWeight("mkBvUnsignedLessOrEqualExpr", 0.3)
        .enableBvLia(10.0)
//        .enableBvNia(0.65)
        .enableArray(1.25)
//        .enableBvBitwise(0.25)
//        .enableBvWeird(0.875)
//        .enableBvShift(1.0)
        .build()
    // mkBvSort disabled
    val expressions = generateRandomExpressions(
        size = 5000,
        batchSize = 1000,
        params = params,
        random = Random(55),
        weights = weights,
        isVerbose = true
    )


    writeExpressions(this, expressions, "generatedExpressions/1Salia")
}