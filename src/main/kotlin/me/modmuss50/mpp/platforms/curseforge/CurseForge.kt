package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformOptions
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import javax.inject.Inject

interface CurseForgeOptions : PlatformOptions {
    @get:Input
    val minecraftVersions: ListProperty<String>

    fun from(other: CurseForgeOptions) {
        super.from(other)
        minecraftVersions.set(other.minecraftVersions)
    }
}

abstract class CurseForge @Inject constructor(name: String) : Platform(name), CurseForgeOptions {
    override fun publish(queue: WorkQueue) {
        queue.submit(UploadWorkAction::class.java) {
            it.from(this)
        }
    }

    interface UploadParams : WorkParameters, CurseForgeOptions

    abstract class UploadWorkAction : WorkAction<UploadParams> {
        override fun execute() {
            TODO("Not yet implemented")
        }
    }
}
