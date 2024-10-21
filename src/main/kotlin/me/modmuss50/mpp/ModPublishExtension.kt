package me.modmuss50.mpp

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.discord.DiscordWebhookTask
import me.modmuss50.mpp.platforms.github.Github
import me.modmuss50.mpp.platforms.github.GithubOptions
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import me.modmuss50.mpp.platforms.modrinth.ModrinthOptions
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class ModPublishExtension(val project: Project) : PublishOptions {
    // Removes the need to import the release type, a little gross tho?
    val BETA = ReleaseType.BETA
    val ALPHA = ReleaseType.ALPHA
    val STABLE = ReleaseType.STABLE

    abstract val dryRun: Property<Boolean>
    val platforms: ExtensiblePolymorphicDomainObjectContainer<Platform> = project.objects.polymorphicDomainObjectContainer(Platform::class.java)

    init {
        dryRun.convention(false)
        maxRetries.convention(3)
        version.convention(project.provider(this::getProjectVersion))
        displayName.convention(version.map { "${project.name} $it" })

        // Inherit the platform options from this extension.
        platforms.whenObjectAdded {
            it.from(this)
        }
    }

    fun publishOptions(@DelegatesTo(PublishOptions::class) closure: Closure<*>): Provider<PublishOptions> {
        return publishOptions {
            project.configure(it, closure)
        }
    }

    fun publishOptions(action: Action<PublishOptions>): Provider<PublishOptions> {
        return project.provider {
            val options = project.objects.newInstance(PublishOptions::class.java)
            options.from(this)
            action.execute(options)
            return@provider options
        }
    }

    // Curseforge

    fun curseforge(@DelegatesTo(value = Curseforge::class) closure: Closure<*>): NamedDomainObjectProvider<Curseforge> {
        return curseforge {
            project.configure(it, closure)
        }
    }

    fun curseforge(action: Action<Curseforge>): NamedDomainObjectProvider<Curseforge> {
        return curseforge("curseforge", action)
    }

    fun curseforge(name: String, @DelegatesTo(value = Curseforge::class) closure: Closure<*>): NamedDomainObjectProvider<Curseforge> {
        return curseforge(name) {
            project.configure(it, closure)
        }
    }

    fun curseforge(name: String, action: Action<Curseforge>): NamedDomainObjectProvider<Curseforge> {
        return platforms.maybeRegister(name, action)
    }

    fun curseforgeOptions(@DelegatesTo(value = Curseforge::class) closure: Closure<*>): Provider<CurseforgeOptions> {
        return curseforgeOptions {
            project.configure(it, closure)
        }
    }

    fun curseforgeOptions(action: Action<CurseforgeOptions>): Provider<CurseforgeOptions> {
        return configureOptions(CurseforgeOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // Modirth

    fun modrinth(@DelegatesTo(value = Modrinth::class) closure: Closure<*>): NamedDomainObjectProvider<Modrinth> {
        return modrinth {
            project.configure(it, closure)
        }
    }

    fun modrinth(action: Action<Modrinth>): NamedDomainObjectProvider<Modrinth> {
        return modrinth("modrinth", action)
    }

    fun modrinth(name: String, @DelegatesTo(value = Modrinth::class) closure: Closure<*>): NamedDomainObjectProvider<Modrinth> {
        return modrinth(name) {
            project.configure(it, closure)
        }
    }

    fun modrinth(name: String, action: Action<Modrinth>): NamedDomainObjectProvider<Modrinth> {
        return platforms.maybeRegister(name, action)
    }

    fun modrinthOptions(@DelegatesTo(value = Modrinth::class) closure: Closure<*>): Provider<ModrinthOptions> {
        return modrinthOptions {
            project.configure(it, closure)
        }
    }

    fun modrinthOptions(action: Action<ModrinthOptions>): Provider<ModrinthOptions> {
        return configureOptions(ModrinthOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // Github

    fun github(@DelegatesTo(value = Github::class) closure: Closure<*>): NamedDomainObjectProvider<Github> {
        return github {
            project.configure(it, closure)
        }
    }

    fun github(action: Action<Github>): NamedDomainObjectProvider<Github> {
        return github("github", action)
    }

    fun github(name: String, @DelegatesTo(value = Github::class) closure: Closure<*>): NamedDomainObjectProvider<Github> {
        return github(name) {
            project.configure(it, closure)
        }
    }

    fun github(name: String, action: Action<Github>): NamedDomainObjectProvider<Github> {
        return platforms.maybeRegister(name, action)
    }

    fun githubOptions(@DelegatesTo(value = Github::class) closure: Closure<*>): Provider<GithubOptions> {
        return githubOptions {
            project.configure(it, closure)
        }
    }

    fun githubOptions(action: Action<GithubOptions>): Provider<GithubOptions> {
        return configureOptions(GithubOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // Discord

    fun discord(@DelegatesTo(value = DiscordWebhookTask::class) closure: Closure<*>): TaskProvider<DiscordWebhookTask> {
        return discord("announceDiscord", closure)
    }

    fun discord(action: Action<DiscordWebhookTask>): TaskProvider<DiscordWebhookTask> {
        return discord("announceDiscord", action)
    }

    fun discord(name: String, @DelegatesTo(value = DiscordWebhookTask::class) closure: Closure<*>): TaskProvider<DiscordWebhookTask> {
        return discord(name) {
            project.configure(it, closure)
        }
    }

    fun discord(name: String, action: Action<DiscordWebhookTask>): TaskProvider<DiscordWebhookTask> {
        val task = project.tasks.register(name, DiscordWebhookTask::class.java) {
            action.execute(it)
        }

        project.tasks.named("publishMods").configure {
            it.dependsOn(task.get())
        }

        return task
    }

    // Misc

    private inline fun <reified T> PolymorphicDomainObjectContainer<in T>.maybeRegister(name: String, action: Action<T>): NamedDomainObjectProvider<T> {
        return if (name in platforms.names) {
            named(name, T::class.java, action)
        } else {
            register(name, T::class.java, action)
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

    // Returns the project version as a string, or throws if not set.
    private fun getProjectVersion(): String {
        val version = project.version

        if (version == Project.DEFAULT_VERSION) {
            throw IllegalStateException("Gradle version is unspecified")
        }

        return version.toString()
    }
}

internal val Project.modPublishExtension: ModPublishExtension
    get() = extensions.getByType(ModPublishExtension::class.java)

internal val RegularFileProperty.path: Path
    get() = get().asFile.toPath()
