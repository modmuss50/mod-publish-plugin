package me.modmuss50.mpp.platforms.github

import me.modmuss50.mpp.Platform
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class Github @Inject constructor(name: String): Platform(name) {

}