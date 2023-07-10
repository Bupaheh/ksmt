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
