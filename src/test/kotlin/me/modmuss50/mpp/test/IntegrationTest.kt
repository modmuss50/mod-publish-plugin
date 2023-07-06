package me.modmuss50.mpp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File

interface IntegrationTest {
    fun gradleTest(): TestBuilder {
        return TestBuilder()
    }

    class TestBuilder {
        private val runner = GradleRunner.create()
            .withPluginClasspath()
            .forwardOutput()
            .withDebug(true)

        private val gradleHome: File
        private val projectDir: File
        private val buildScript: File
        private var arguments = ArrayList<String>()

        init {
            val testDir = File("build/intergation_test")
            gradleHome = File(testDir, "home")
            projectDir = File(testDir, "project")
            buildScript = File(projectDir, "build.gradle.kts")

            projectDir.mkdirs()
            buildScript.writeText("") // Clear
            buildScript(
                """
                plugins {
                    java
                    id("me.modmuss50.mod-publish-plugin") version "0.0.1"
                }
                """.trimIndent(),
            )

            File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"mpp-example\"")

            runner.withProjectDir(projectDir)
            argument("--gradle-user-home", gradleHome.absolutePath)
        }

        // Appends to an existing buildscript
        fun buildScript(@Language("gradle") script: String): TestBuilder {
            buildScript.appendText(script)
            return this
        }

        fun argument(vararg args: String) {
            arguments.addAll(args)
        }

        fun run(task: String): BuildResult {
            argument(task)
            runner.withArguments(arguments)
            return runner.build()
        }
    }
}
