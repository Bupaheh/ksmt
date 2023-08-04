package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KApp
import io.ksmt.expr.KConst
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KSort

class KDeclCounter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private val declCount: HashMap<String, Int> = hashMapOf()
    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        if (expr is KConst<*>) return expr
        if (expr is KInterpretedValue<*>) return expr

        val name = if (expr.decl.name == "select" || expr.decl.name == "store") {
            expr.decl.name + expr.args.size
        } else {
            expr.decl.name
        }

        declCount[name] = declCount.getOrDefault(name, 0) + 1

        return expr
    }

    fun countDeclarations(expr: KExpr<*>): HashMap<String, Int> {
        apply(expr)

        return declCount
    }

}