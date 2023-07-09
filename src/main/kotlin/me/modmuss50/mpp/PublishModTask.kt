package me.modmuss50.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.FileNotFoundException
import javax.inject.Inject

@DisableCachingByDefault(because = "Re-upload mod each time")
abstract class PublishModTask @Inject constructor(@Nested val platform: Platform) : DefaultTask() {
    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
    }

    @TaskAction
    fun publish() {
        if (project.modPublishExtension.dryRun.get()) {
            val file = platform.file.get().asFile

            if (!file.exists()) {
                throw FileNotFoundException("$file not found")
            }

            for (additionalFile in platform.additionalFiles.files) {
                if (!additionalFile.exists()) {
                    throw FileNotFoundException("$file not found")
                }
            }

            return
        }

        // Ensure that we have an access token when not dry running.
        platform.accessToken.get()

        val workQueue = workerExecutor.noIsolation()
        platform.publish(workQueue)
    }
}
