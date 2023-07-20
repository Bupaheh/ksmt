plugins {
    id("io.ksmt.ksmt-base")
}

repositories {
    mavenCentral()
}


dependencies {
    implementation(project(":ksmt-core"))
    implementation(project(":ksmt-z3"))
    implementation(project(":ksmt-cvc5"))
    implementation(project(":ksmt-runner"))
    testImplementation(project(mapOf("path" to ":ksmt-test")))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            addKsmtPom()
            addSourcesAndJavadoc(project)
            signKsmtPublication(project)
        }
    }
}
