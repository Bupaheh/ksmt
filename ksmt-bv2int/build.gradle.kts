plugins {
    id("io.ksmt.ksmt-base")
}

repositories {
    mavenCentral()
}


dependencies {
    implementation(project(":ksmt-core"))

    testImplementation(project(":ksmt-cvc5"))
    testImplementation(project(":ksmt-z3"))
    testImplementation(project(":ksmt-yices"))
    testImplementation(project(":ksmt-bitwuzla"))
    testImplementation(project(":ksmt-runner"))
    testImplementation(project(":ksmt-test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            addKsmtPom()
            addSourcesAndJavadoc(project)
            signKsmtPublication(project)
        }
    }
}

tasks.register<JavaExec>("benchmark") {
    mainClass.set("io.ksmt.solver.wrapper.bv2int.BenchamarksKt")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.register<JavaExec>("benchmarkGeneration") {
    mainClass.set("io.ksmt.solver.wrapper.bv2int.BenchmarkGenerationKt")
    classpath = sourceSets["test"].runtimeClasspath
}
