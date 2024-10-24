package io.ksmt.solver.runner

import io.ksmt.runner.generated.models.ConfigurationParamKind
import io.ksmt.runner.generated.models.SolverConfigurationParam
import io.ksmt.runner.generated.models.SolverConfigurationTheories
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverUniversalConfigurationBuilder
import io.ksmt.solver.KTheory

class KSolverRunnerUniversalConfigurator : KSolverUniversalConfigurationBuilder {
    val config = mutableListOf<SolverConfigurationParam>()
    var theories: SolverConfigurationTheories? = null

    override fun buildOptimizeForTheories(theories: Set<KTheory>?, quantifiersAllowed: Boolean) {
        this.theories = SolverConfigurationTheories(quantifiersAllowed, theories?.map { it.name })
    }

    override fun buildBoolParameter(param: String, value: Boolean) {
        config += SolverConfigurationParam(ConfigurationParamKind.Bool, param, "$value")
    }

    override fun buildIntParameter(param: String, value: Int) {
        config += SolverConfigurationParam(ConfigurationParamKind.Int, param, "$value")
    }

    override fun buildDoubleParameter(param: String, value: Double) {
        config += SolverConfigurationParam(ConfigurationParamKind.Double, param, "$value")
    }

    override fun buildStringParameter(param: String, value: String) {
        config += SolverConfigurationParam(ConfigurationParamKind.String, param, value)
    }
}

fun KSolverConfiguration.setUniversalOptimizeForTheories(theoriesConfig: SolverConfigurationTheories) {
    val theories = theoriesConfig.theories?.mapTo(hashSetOf()) { KTheory.valueOf(it) }
    optimizeForTheories(theories, theoriesConfig.quantifiersAllowed)
}

fun KSolverConfiguration.addUniversalParam(param: SolverConfigurationParam): Unit = with(param) {
    when (kind) {
        ConfigurationParamKind.String -> setStringParameter(name, value)
        ConfigurationParamKind.Bool -> setBoolParameter(name, value.toBooleanStrict())
        ConfigurationParamKind.Int -> setIntParameter(name, value.toInt())
        ConfigurationParamKind.Double -> setDoubleParameter(name, value.toDouble())
    }
}
