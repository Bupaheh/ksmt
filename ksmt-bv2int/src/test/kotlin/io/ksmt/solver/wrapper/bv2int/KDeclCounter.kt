package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.decl.KDecl
import io.ksmt.expr.KApp
import io.ksmt.expr.KConst
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.expr.transformer.KNonRecursiveTransformer
import io.ksmt.sort.KSort

@Suppress("ComplexCondition")
class KDeclCounter(ctx: KContext) : KNonRecursiveTransformer(ctx) {
    private val declCount: HashMap<String, Int> = hashMapOf()
    private val visited = hashSetOf<KExpr<*>>()
    private val constDecls = setOf("bvmul", "bvshl", "bvlshr", "bvashr")

    override fun <T : KSort, A : KSort> transformApp(expr: KApp<T, A>): KExpr<T> {
        if (expr is KConst<*>) return expr
        if (expr is KInterpretedValue<*>) return expr
        if (!visited.add(expr)) return expr

        var name = expr.decl.name

        if (name in constDecls && expr.args.take(2).any { it is KInterpretedValue<*> }
            && ("sh" !in name || expr.args[1] is KInterpretedValue)
        ) {
            name += "C"
        }

        declCount[name] = declCount.getOrDefault(name, 0) + 1

        return expr
    }

    fun countDeclarations(expr: KExpr<*>): HashMap<String, Int> {
        apply(expr)

        return declCount
    }
}