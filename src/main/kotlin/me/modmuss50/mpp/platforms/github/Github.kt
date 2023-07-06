package me.modmuss50.mpp.platforms.github

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import javax.inject.Inject

interface GithubOptions: PlatformOptions {
    fun from(other: GithubOptions) {
        super.from(other)
    }
}

abstract class Github @Inject constructor(name: String): Platform(name), GithubOptions {
    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams: WorkParameters, GithubOptions {
    }

    abstract class UploadWorkAction: WorkAction<UploadParams> {
        override fun execute() {
            TODO("Not yet implemented")
        }
    }
}