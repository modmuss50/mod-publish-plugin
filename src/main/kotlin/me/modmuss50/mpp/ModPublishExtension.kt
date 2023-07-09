package me.modmuss50.mpp

import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.modrith.Modrith
import me.modmuss50.mpp.platforms.modrith.ModrithOptions
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class ModPublishExtension(val project: Project) : PublishOptions {
    // Removes the need to import the release type, a little gross tho?
    val BETA = PublishOptions.ReleaseType.BETA
    val ALPHA = PublishOptions.ReleaseType.ALPHA
    val STABLE = PublishOptions.ReleaseType.STABLE

    abstract val dryRun: Property<Boolean>
    val platforms: ExtensiblePolymorphicDomainObjectContainer<Platform> = project.objects.polymorphicDomainObjectContainer(Platform::class.java)

    init {
        dryRun.convention(false)
        maxRetries.convention(3)

        // Inherit the platform options from this extension.
        // TODO we may need to do this in the platform factory
        platforms.whenObjectAdded {
            it.from(this)
        }
    }

    fun curseforge(name: String = "curseforge", action: Action<Curseforge>): NamedDomainObjectProvider<Curseforge> {
        return platforms.register(name, Curseforge::class.java, action)
    }

    fun curseforgeOptions(action: Action<CurseforgeOptions>): Provider<CurseforgeOptions> {
        return configureOptions(CurseforgeOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    fun modrith(name: String = "modrith", action: Action<Modrith>): NamedDomainObjectProvider<Modrith> {
        return platforms.register(name, Modrith::class.java, action)
    }

    fun modrithOptions(action: Action<ModrithOptions>): Provider<ModrithOptions> {
        return configureOptions(ModrithOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    private fun <A : PlatformOptions, T : PlatformOptionsInternal<A>> configureOptions(klass: KClass<T>, action: Action<T>): Provider<T> {
        return project.provider {
            val options = project.objects.newInstance(klass.java)
            options.setInternalDefaults()
            action.execute(options)
            return@provider options
        }
    }
}

internal val Project.modPublishExtension: ModPublishExtension
    get() = extensions.getByType(ModPublishExtension::class.java)

internal val RegularFileProperty.path: Path
    get() = get().asFile.toPath()
