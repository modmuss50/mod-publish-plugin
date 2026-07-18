package me.modmuss50.mpp.platforms.hangar.file

import org.gradle.api.Action
import org.gradle.api.Incubating
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

@Incubating
interface HangarFileContainer {

    @get:Input
    @get:Optional
    val files: ListProperty<HangarFile>

    fun file(action: Action<HangarFile>) {
        addInternal(null, action)
    }

    fun external(
        externalUrl: String,
        action: Action<HangarFile>,
    ) {
        addInternal(externalUrl, action)
    }

    fun fromFiles(other: HangarFileContainer) {
        files.convention(other.files)
    }

    @get:ApiStatus.Internal
    @get:Inject
    val objectFactory: ObjectFactory

    @ApiStatus.Internal
    fun addInternal(
        externalUrl: String?,
        action: Action<HangarFile>,
    ) {
        val file = objectFactory.newInstance(HangarFile::class.java)

        externalUrl?.let {
            file.externalUrl.set(it)
            file.externalUrl.finalizeValue()
        }

        action.execute(file)

        file.validate()

        files.add(file)
    }
}
