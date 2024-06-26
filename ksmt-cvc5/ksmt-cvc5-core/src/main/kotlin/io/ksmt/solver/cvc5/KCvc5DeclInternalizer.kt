package io.ksmt.solver.cvc5

import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.ksmt.decl.KConstDecl
import io.ksmt.decl.KDeclVisitor
import io.ksmt.decl.KFuncDecl
import io.ksmt.sort.KSort

open class KCvc5DeclInternalizer(
    private val cvc5Ctx: KCvc5Context,
    private val solver: Solver,
    private val sortInternalizer: KCvc5SortInternalizer
) : KDeclVisitor<Term> {
    private val tm = cvc5Ctx.termManager

    override fun <S : KSort> visit(decl: KFuncDecl<S>): Term = cvc5Ctx.internalizeDecl(decl) {
        // declarations incremental collection optimization
        cvc5Ctx.addDeclaration(decl)

        val domainSorts = decl.argSorts.map { it.accept(sortInternalizer) }
        val rangeSort = decl.sort.accept(sortInternalizer)

        solver.declareFun(
            decl.name,
            domainSorts.toTypedArray(),
            rangeSort
        ).also { tm.registerPointer(it) }
    }

    override fun <S : KSort> visit(decl: KConstDecl<S>): Term = cvc5Ctx.internalizeDecl(decl) {
        cvc5Ctx.addDeclaration(decl)

        val sort = decl.sort.accept(sortInternalizer)
        tm.builder { mkConst(sort, decl.name) }
    }
}
