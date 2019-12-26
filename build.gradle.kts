import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.9.32"

plugins {
    kotlin("jvm") version "1.3.60"
    kotlin("kapt") version "1.3.60"
    id("com.github.johnrengelman.shadow").version("4.0.4")
    id("java")
    id("maven")
    id("application")
    id("org.beryx.runtime") version "1.3.0"
    id("org.openjfx.javafxplugin").version("0.0.5")
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://dl.bintray.com/jerady/maven")
        maven("https://jitpack.io")
        maven("https://repo.maven.apache.org/maven2")
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("io.github.microutils:kotlin-logging:1.6.22")

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    }
}

javafx {
    version = "12"
    modules = listOf("javafx.controls", "javafx.media", "javafx.fxml")
}

dependencies {
    val ktorVersion = "1.2.6"
    implementation("io.github.koma-im:koma-library:0.9.18")
    implementation("org.cache2k:cache2k-core:1.2.3.Final")
    implementation("de.jensd:fontawesomefx-fontawesome:4.7.0-9.1.2")
    implementation("de.jensd:fontawesomefx-materialicons:2.2.0-9.1.2")
    implementation("de.jensd:fontawesomefx-commons:9.1.2")
    implementation("org.controlsfx:controlsfx:11.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.3.3")
    implementation("com.vdurmont:emoji-java:4.0.0")
    implementation("org.slf4j:slf4j-api:1.8.0-beta2")
    implementation("org.slf4j:slf4j-simple:1.8.0-beta2")
    implementation("io.requery:requery-kotlin:1.6.1")
    implementation("io.requery:requery:1.6.1")
    implementation("com.h2database:h2:1.4.199")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.14.0")
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-core", ktorVersion)

    kapt("io.requery:requery-processor:1.6.1")
}

group = "link.continuum"
description = "continuum-desktop"
application {
    mainClassName = "koma.koma_app.MainKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.apply {
        jvmTarget = "9"
        freeCompilerArgs = listOf(
                "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi"
                , "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
                , "-XXLanguage:+InlineClasses"
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}


tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Jar> {
    archiveClassifier.set("without-dependencies")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("bundled")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf(
            "java.compiler",
            "java.desktop",
            "java.instrument",
            "java.logging",
            "java.management",
            "java.naming",
            "java.scripting",
            "java.sql",
            "java.sql.rowset",
            "java.transaction.xa",
            "java.xml",
            "jdk.crypto.ec",
            "jdk.jfr",
            "jdk.unsupported"
            ))
}
