package io.ksmt.solver.wrapper.bv2int

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.utils.mkConst
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

open class KBenchmarkSolverWrapper<Config: KSolverConfiguration>(
    ctx: KContext,
    private val solver: KSolver<Config>,
) : KSolver<Config> by solver {
    private var checkTime: Long = 0

    init {
        if (solver is KZ3Solver) {
            solver.push()
            solver.assert(ctx.boolSort.mkConst("a"))
            solver.check()
            solver.pop()
        }
    }

    override fun check(timeout: Duration): KSolverStatus {
        val status: KSolverStatus

        val time = measureNanoTime {
            status = solver.check(timeout)
        }

        checkTime += time

        return status
    }

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        val status: KSolverStatus

        val time = measureNanoTime {
            status = solver.checkWithAssumptions(assumptions, timeout)
        }

        checkTime += time

        return status
    }

    override fun pop(n: UInt) {
        checkTime = 0
        solver.pop(n)
    }

    override fun model(): KModel {
        val model: KModel
        measureNanoTime {
            model = solver.model()
        }
//            .also { println("MODEL: ${it.nanoseconds} | ${model.declarations.size}") }
        return model
    }

    override fun reasonOfUnknown(): String = "$checkTime;1"
}