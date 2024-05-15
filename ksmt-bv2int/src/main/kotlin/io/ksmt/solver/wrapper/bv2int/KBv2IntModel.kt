package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KExpr
import io.ksmt.expr.KUninterpretedSortValue
import io.ksmt.solver.KModel
import io.ksmt.solver.model.KFuncInterp
import io.ksmt.solver.model.KFuncInterpEntryVarsFree
import io.ksmt.solver.model.KFuncInterpEntryWithVars
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
            // It is expected that interpretations will not contain these declarations
            .filterNot { bv2IntContext.isAuxDecl(it) || it.name == "div0" || it.name == "mod0" }
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

            convertInterpretation(decl, interpretation)
        }.uncheckedCast()

    private fun <T : KSort, R : KSort> convertInterpretation(
        originalDeclaration: KDecl<T>,
        interpretation: KFuncInterp<R>,
    ) : KFuncInterp<T> {
        val convertedEntries = interpretation.entries.map { entry ->
            if (entry is KFuncInterpEntryWithVars) TODO()

            val args = entry.args.zip(originalDeclaration.argSorts).map { (arg, originalArgSort) ->
                convertExpr(arg, originalArgSort)
            }
            val value = convertExpr(entry.value, originalDeclaration.sort)

            KFuncInterpEntryVarsFree.create(args, value)
        }
        val default = interpretation.default?.let { convertExpr(it, originalDeclaration.sort) }

        return KFuncInterpVarsFree(originalDeclaration, convertedEntries, default).uncheckedCast()
    }

    private fun <T : KSort> convertExpr(expr: KExpr<*>, sort: T): KExpr<T> =
            KBv2IntConverter(ctx, bv2IntContext).convertExpr(expr, sort)

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
