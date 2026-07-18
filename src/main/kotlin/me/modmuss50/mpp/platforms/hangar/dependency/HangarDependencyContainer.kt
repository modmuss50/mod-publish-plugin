package me.modmuss50.mpp.platforms.hangar.dependency

import org.gradle.api.Action
import org.gradle.api.Incubating
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject

@Incubating
interface HangarDependencyContainer {

    @get:Input
    @get:Optional
    val pluginDependencies: MapProperty<String, List<HangarDependency>>

    fun requires(
        name: String,
        action: Action<HangarDependency>,
    ) {
        addInternal(name, true, action)
    }

    fun optional(
        name: String,
        action: Action<HangarDependency>,
    ) {
        addInternal(name, false, action)
    }

    fun fromDependencies(other: HangarDependencyContainer) {
        pluginDependencies.convention(other.pluginDependencies)
    }

    @get:ApiStatus.Internal
    @get:Inject
    val objectFactory: ObjectFactory

    @Internal
    fun addInternal(
        name: String,
        required: Boolean,
        action: Action<HangarDependency>,
    ) {
        val dep = objectFactory.newInstance(HangarDependency::class.java)

        dep.name.set(name)
        dep.name.finalizeValue()

        dep.required.set(required)
        dep.required.finalizeValue()

        action.execute(dep)

        dep.validate()

        pluginDependencies.put(
            name,
            pluginDependencies
                .getting(name)
                .map { dependencies ->
                    dependencies + dep
                },
        )
    }
}
