package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.sort.KBoolSort
import io.ksmt.test.GenerationParameters
import java.io.File
import java.nio.ByteBuffer
import kotlin.random.Random

fun KContext.generateExpressions() {
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
        .enableBvNia(0.65)
//        .enableArray(1.25)
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
        isVerbose = true,
        predicate = { expr ->
            niaDecls.any { decl ->
                KDeclCounter(this).countDeclarations(expr).getOrDefault(decl, 0) > 0
            }
        }
    )


    writeExpressionsSerialized(this, expressions, "generatedExpressions/1Snia")
}

fun writeExpressionsSerialized(ctx: KContext, expressions: List<KExpr<KBoolSort>>, path: String) {
    val serializationCtx = AstSerializationCtx().apply { initCtx(ctx) }
    val marshaller = AstSerializationCtx.marshaller(serializationCtx)
    val emptyRdSerializationCtx = SerializationCtx(Serializers())
    val buffer = UnsafeBuffer(ByteArray(1000))

    expressions.forEach { expr ->
        marshaller.write(emptyRdSerializationCtx, buffer, expr)
    }

    val xxx = ByteBuffer.wrap(buffer.getArray(), 0, buffer.position)
    File(path).writeBytes(xxx.array())
}

inline fun readFilterSmtBenchmarkData(
    ctx: KContext,
    dirPath: String,
    begin: Int = 0,
    end: Int = 5000,
    filterExpr: (String) -> Boolean
): List<KExpr<KBoolSort>> {
    val parser = KZ3SMTLibParser(ctx)
    val expressions = mutableListOf<KExpr<KBoolSort>>()
    val paths = File(dirPath).walk()
        .filter { it.extension == "smt2" }
        .toList()
        .shuffled(Random(1))
        .mapIndexed { index, path -> index to path }
        .filter { (idx, _) -> idx in begin..end && idx != 44839 && idx != 1351 }
        .filter { filterExpr(it.second.path) }

    paths.forEach { (idx, file) ->
        println("$idx\t${expressions.size}")

        val fileName = file.toPath().toString()
            .substringAfterLast("Projects/QF_BV/")
            .replace('/', '_')

        try {
            file.copyTo(File("generatedExpressions/QF_BV_BIT/$fileName"))
        } catch (_: Exception) { }

        return@forEach


        val expr = ctx.mkAnd(parser.parse(file.toPath()))



        if (TempVisitor(ctx).visit(expr) && expr !is KInterpretedValue) {
            try {
                file.copyTo(File("generatedExpressions/QF_BV_UNBIT/$fileName"))
            } catch (_: Exception) { }
        }

        return@forEach

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

        if (filterExpr(file.path)) {
            expressions.add(expr)
        }
    }

    return expressions
}