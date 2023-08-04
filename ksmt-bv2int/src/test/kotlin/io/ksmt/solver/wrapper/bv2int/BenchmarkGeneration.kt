package io.ksmt.solver.wrapper.bv2int

import com.jetbrains.rd.framework.SerializationCtx
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.UnsafeBuffer
import io.ksmt.KContext
import io.ksmt.runner.serializer.AstSerializationCtx
import io.ksmt.test.GenerationParameters
import java.io.File
import kotlin.random.Random

fun main() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
    val params = GenerationParameters(
        seedExpressionsPerSort = 20,
        possibleIntValues = 2..64,
        deepExpressionProbability = 0.3,
        generatedListSize = 2..3,
        astFilter = Bv2IntAstFilter()
    )
    val weights = Bv2IntBenchmarkWeightBuilder()
        .enableBvCmp(2.0)
        .enableBvLia(10.0)
//        .enableBvNia(0.65)
        .enableArray(1.25)
//        .enableBvBitwise(0.25)
        .enableBvWeird(0.875)
//        .enableBvShift(1.0)
        .build()
    // mkBvSort disabled
    val expressions = generateRandomExpressions(
        size = 10000,
        batchSize = 1000,
        params = params,
        random = Random(48),
        weights = weights,
        isVerbose = true
    )

    val serializationCtx = AstSerializationCtx().apply { initCtx(this@with) }
    val marshaller = AstSerializationCtx.marshaller(serializationCtx)
    val emptyRdSerializationCtx = SerializationCtx(Serializers())
    val buffer = UnsafeBuffer(ByteArray(1000))

    expressions.forEach { expr ->
        marshaller.write(emptyRdSerializationCtx, buffer, expr)
    }

    File("generatedExpressions/1awlia").writeBytes(buffer.getArray())
}