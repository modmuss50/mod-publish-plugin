package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.Platform
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class CurseForge @Inject constructor(name: String): Platform(name) {
    @get:Input
    abstract val minecraftVersions: ListProperty<String>
}