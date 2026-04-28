import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    `maven-publish`
    embeddedKotlin("jvm")
    embeddedKotlin("plugin.serialization")
    id("com.diffplug.spotless") version "6.18.0"
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "me.modmuss50"
version = "2.0.0-beta.1"
description = "The Mod Publish Plugin is a plugin for the Gradle build system to help upload artifacts to a range of common destinations."

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation("io.javalin:javalin:7.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

// Workaround https://github.com/gradle/gradle/issues/25898
tasks.withType(Test::class.java).configureEach {
    jvmArgs = listOf(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED"
    )
}

tasks.withType(JavaCompile::class.java).all {
    options.release = 17
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
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
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
        compatibility {
            features {
                configurationCache = true
            }
        }
    }
}

fun replaceVersion(path: String) {
    var content = project.file(path).readText()

    content = content.replace("(version \").*(\")".toRegex(), "version \"${project.version}\"")// project.version.toString())

    project.file(path).writeText(content)
}

replaceVersion("README.md")
replaceVersion("docs/pages/getting_started.mdx")
