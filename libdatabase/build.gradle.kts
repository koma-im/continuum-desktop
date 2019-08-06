plugins {
    id("org.jetbrains.kotlin.kapt")
}

dependencies {
    implementation("io.requery:requery-kotlin:1.6.1")
    implementation("io.requery:requery:1.6.1")
    implementation("com.h2database:h2:1.4.198")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    compile(project(":libutil"))
    kapt("io.requery:requery-processor:1.6.1")
}
