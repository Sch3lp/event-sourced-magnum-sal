plugins {
    id("magnumsal.kotlin.library-conventions")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.1")
}
