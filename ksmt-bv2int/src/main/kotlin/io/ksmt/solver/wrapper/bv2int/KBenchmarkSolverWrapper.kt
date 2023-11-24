package io.ksmt.solver.wrapper.bv2int

import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import kotlin.system.measureNanoTime
import kotlin.time.Duration

class KBenchmarkSolverWrapper<Config: KSolverConfiguration>(
    private val solver: KSolver<Config>,
) : KSolver<Config> by solver {
    private var checkTime: Long = 0
    private var roundCount: Int = 1

    fun resetRoundCount() {
        roundCount = 1
    }

    fun incRoundCount() {
        roundCount++
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

    override fun reasonOfUnknown(): String = "$checkTime;$roundCount"
}
