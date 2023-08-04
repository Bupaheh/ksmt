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
            addKsmtPom()
            addSourcesAndJavadoc(project)
            signKsmtPublication(project)
        }
    }
}