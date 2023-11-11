package me.modmuss50.mpp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Nested
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
    @get:OutputFile
    abstract val result: RegularFileProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
        outputs.upToDateWhen { false }
        result.set(project.buildDir.resolve("publishMods/$name.json"))
        result.finalizeValue()
    }

    @TaskAction
    fun publish() {
        if (project.modPublishExtension.dryRun.get()) {
            val file = platform.file.get().asFile

            if (!file.exists()) {
                throw FileNotFoundException("$file not found")
            }

            project.copy {
                it.from(file)
                it.into(project.buildDir.resolve("publishMods").resolve(name))
            }

            for (additionalFile in platform.additionalFiles.files) {
                if (!additionalFile.exists()) {
                    throw FileNotFoundException("$file not found")
                }

                project.copy {
                    it.from(additionalFile)
                    it.into(project.buildDir.resolve("publishMods").resolve(name))
                }
            }

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
}
