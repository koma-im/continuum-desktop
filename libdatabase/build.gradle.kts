plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.requery:requery-kotlin:1.5.1")
    implementation("io.requery:requery:1.5.1")
    implementation("com.h2database:h2:1.4.198")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.github.microutils:kotlin-logging:1.6.22")
    implementation("io.github.koma-im:koma-library:0.7.6.8")
    compile(project(":libutil"))
    kapt("io.requery:requery-processor:1.5.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
