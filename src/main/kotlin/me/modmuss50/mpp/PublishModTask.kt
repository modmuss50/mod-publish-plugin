package me.modmuss50.mpp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.platforms.github.GithubOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.annotations.ApiStatus
import java.io.FileNotFoundException
import javax.inject.Inject

@DisableCachingByDefault(because = "Re-upload mod each time")
abstract class PublishModTask @Inject constructor(@Nested val platform: Platform) : DefaultTask() {
    @get:ApiStatus.Internal
    @get:Input
    abstract val dryRun: Property<Boolean>

    @get:ApiStatus.Internal
    @get:OutputFile
    abstract val result: RegularFileProperty

    @get:ApiStatus.Internal
    @get:OutputDirectory
    abstract val dryRunDirectory: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
        outputs.upToDateWhen { false }
        dryRun.set(project.modPublishExtension.dryRun)
        dryRun.finalizeValue()
        result.set(project.layout.buildDirectory.file("publishMods/$name.json"))
        result.finalizeValue()
        dryRunDirectory.set(project.layout.buildDirectory.dir("publishMods/$name"))
        dryRunDirectory.finalizeValue()
    }

    @TaskAction
    fun publish() {
        if (dryRun.get()) {
            logger.lifecycle("Dry run $name:")
            platform.printDryRunInfo(logger)

            dryRunCopyMainFile()

            for (additionalFile in platform.additionalFiles.files) {
                if (!additionalFile.exists()) {
                    throw FileNotFoundException("$additionalFile not found")
                }

                additionalFile.copyTo(dryRunDirectory.get().asFile.resolve(additionalFile.name), overwrite = true)
                logger.lifecycle("Additional file: ${additionalFile.name}")
            }

            logger.lifecycle("Display name: ${platform.displayName.get()}")
            logger.lifecycle("Version: ${platform.version.get()}")
            logger.lifecycle("Changelog: ${platform.changelog.get()}")

            result.get().asFile.writeText(
                Json.encodeToString(platform.dryRunPublishResult()),
            )

            return
        }

        // Ensure that we have an access token when not dry running.
        platform.accessToken.get()

        val workQueue = workerExecutor.noIsolation()
        val context = PublishContext(queue = workQueue, result = result.get())
        platform.publish(context)
    }

    private fun dryRunCopyMainFile() {
        // A bit of a hack to handle the optional main file for Github.
        if (platform is GithubOptions) {
            if (!platform.file.isPresent && platform.allowEmptyFiles.get()) {
                return
            }
        }

        val file = platform.file.get().asFile

        if (!file.exists()) {
            throw FileNotFoundException("$file not found")
        }

        file.copyTo(dryRunDirectory.get().asFile.resolve(file.name), overwrite = true)
        logger.lifecycle("Main file: ${file.name}")
    }
}
