package me.modmuss50.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@DisableCachingByDefault(because = "Re-upload mod each time")
abstract class PublishModTask @Inject constructor(@Nested val platform: Platform): DefaultTask() {
    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    init {
        group = "publishing"
    }

    @TaskAction
    fun publish() {
        val workQueue = workerExecutor.noIsolation()
        platform.publish(workQueue)
    }
}