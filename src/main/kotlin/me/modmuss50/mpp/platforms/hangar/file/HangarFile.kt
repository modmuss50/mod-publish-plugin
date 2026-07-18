package me.modmuss50.mpp.platforms.hangar.file

import me.modmuss50.mpp.platforms.hangar.HangarApi.HangarPlatformType
import org.gradle.api.Incubating
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.annotations.ApiStatus

@Incubating
interface HangarFile {
    val externalUrl: Property<String> // Not always present

    val platforms: ListProperty<HangarPlatformType>

    @ApiStatus.Internal
    fun validate()
}
