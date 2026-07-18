package me.modmuss50.mpp.platforms.hangar.dependency

import org.gradle.api.Incubating
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@Incubating
interface HangarDependency {
    val externalUrl: Property<String> // Not always present

    val name: Property<String> // Not always present

    val platform: Property<String>

    val projectId: Property<Int> // Not always present

    val required: Property<Boolean>

    @ApiStatus.Internal
    fun validate()
}
