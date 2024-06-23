import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    embeddedKotlin("jvm")
    embeddedKotlin("plugin.serialization")
    id("com.diffplug.spotless") version "6.18.0"
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "me.modmuss50"
version = "0.5.1"
description = "The Mod Publish Plugin is a plugin for the Gradle build system to help upload artifacts to a range of common destinations."

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.kohsuke:github-api:1.315")

    testImplementation(kotlin("test"))
    testImplementation("io.javalin:javalin:5.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType(JavaCompile::class.java).all {
    options.release = 8
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Version" to version))
    }
}

spotless {
    kotlin {
        ktlint()
    }
}

gradlePlugin {
    website = "https://github.com/modmuss50/mod-publish-plugin"
    vcsUrl = "https://github.com/modmuss50/mod-publish-plugin"
    testSourceSet(sourceSets["test"])

    plugins.create("mod-publish-plugin") {
        id = "me.modmuss50.mod-publish-plugin"
        implementationClass = "me.modmuss50.mpp.MppPlugin"
        displayName = "Mod Publish Plugin"
        description = project.description
        version = project.version
        tags = listOf("minecraft", )
    }
}

fun replaceVersion(path: String) {
    var content = project.file(path).readText()

    content = content.replace("(version \").*(\")".toRegex(), "version \"${project.version}\"")// project.version.toString())

    project.file(path).writeText(content)
}

replaceVersion("README.md")
replaceVersion("docs/pages/getting_started.mdx")
