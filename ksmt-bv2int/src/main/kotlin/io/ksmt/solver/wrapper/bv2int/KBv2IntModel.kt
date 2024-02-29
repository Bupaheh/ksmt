package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KExpr
import io.ksmt.expr.KUninterpretedSortValue
import io.ksmt.solver.KModel
import io.ksmt.solver.model.KFuncInterp
import io.ksmt.solver.model.KFuncInterpVarsFree
import io.ksmt.solver.model.KModelEvaluator
import io.ksmt.solver.model.KModelImpl
import io.ksmt.sort.KSort
import io.ksmt.sort.KUninterpretedSort
import io.ksmt.utils.uncheckedCast

class KBv2IntModel(
    private val ctx: KContext,
    private val bv2IntContext: KBv2IntContext,
    private val model: KModel
) : KModel {
    override val declarations: Set<KDecl<*>> by lazy {
        model.declarations
            .filterNot { bv2IntContext.isAuxDecl(it) }
            .map { bv2IntContext.getOriginalDeclaration(it) ?: it }
            .toSet()
    }

    private val interpretations = hashMapOf<KDecl<*>, KFuncInterp<*>?>()
    override val uninterpretedSorts: Set<KUninterpretedSort>
        get() = model.uninterpretedSorts

    private val evaluatorWithModelCompletion by lazy { KModelEvaluator(ctx, this, isComplete = true) }
    private val evaluatorWithoutModelCompletion by lazy { KModelEvaluator(ctx, this, isComplete = false) }

    override fun <T : KSort> eval(expr: KExpr<T>, isComplete: Boolean): KExpr<T> {
        ctx.ensureContextMatch(expr)

        val evaluator = if (isComplete) evaluatorWithModelCompletion else evaluatorWithoutModelCompletion
        return evaluator.apply(expr)
    }

    override fun <T : KSort> interpretation(decl: KDecl<T>): KFuncInterp<T>? =
        interpretations.getOrPut(decl) {
            val rewrittenDecl = bv2IntContext.getRewrittenDeclaration(decl) ?: return null
            val interpretation = model.interpretation(rewrittenDecl) ?: return null

            if (rewrittenDecl == decl) return interpretation.uncheckedCast()

            if (interpretation.entries.isNotEmpty()) TODO()

            val default = interpretation.default?.let { KBv2IntConverter(ctx).convertExpr(it, decl.sort) }

            return KFuncInterpVarsFree(decl, listOf(), default)
        }.uncheckedCast()

    override fun uninterpretedSortUniverse(sort: KUninterpretedSort): Set<KUninterpretedSortValue>? =
        model.uninterpretedSortUniverse(sort)

    override fun detach(): KModel {
        val interpretations = declarations.associateWith {
            interpretation(it) ?: error("missed interpretation for $it")
        }

        val uninterpretedSortsUniverses = uninterpretedSorts.associateWith {
            uninterpretedSortUniverse(it) ?: error("missed sort universe for $it")
        }

        return KModelImpl(ctx, interpretations, uninterpretedSortsUniverses)
    }

    override fun close() {
        model.close()
    }
}
