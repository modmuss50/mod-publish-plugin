package me.modmuss50.mpp

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.discord.DiscordWebhookTask
import me.modmuss50.mpp.platforms.gitea.Codeberg
import me.modmuss50.mpp.platforms.gitea.SelfHostedGitea
import me.modmuss50.mpp.platforms.gitea.base.GiteaCompatibleOptions
import me.modmuss50.mpp.platforms.gitea.base.GiteaCompatiblePlatform
import me.modmuss50.mpp.platforms.github.Github
import me.modmuss50.mpp.platforms.github.GithubOptions
import me.modmuss50.mpp.platforms.gitlab.Gitlab
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

    // CurseForge

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

    // Modrinth

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

    // GitHub

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

    // Gitea

    fun gitea(@DelegatesTo(value = SelfHostedGitea ::class) closure: Closure<*>): NamedDomainObjectProvider<SelfHostedGitea> {
        return gitea {
            project.configure(it, closure)
        }
    }

    fun gitea(action: Action<SelfHostedGitea>): NamedDomainObjectProvider<SelfHostedGitea> {
        return gitea("gitea", action)
    }

    fun gitea(name: String, @DelegatesTo(value = SelfHostedGitea::class) closure: Closure<*>): NamedDomainObjectProvider<SelfHostedGitea> {
        return gitea(name) {
            project.configure(it, closure)
        }
    }

    fun gitea(name: String, action: Action<SelfHostedGitea>): NamedDomainObjectProvider<SelfHostedGitea> {
        return platforms.maybeRegister(name) { it ->
            it.hostType.set(GiteaCompatiblePlatform.GITEA)
            action.execute(it)
        }
    }

    fun giteaOptions(@DelegatesTo(value = SelfHostedGitea::class) closure: Closure<*>): Provider<GiteaCompatibleOptions> {
        return giteaOptions {
            project.configure(it, closure)
        }
    }

    fun giteaOptions(action: Action<GiteaCompatibleOptions>): Provider<GiteaCompatibleOptions> {
        return configureOptions(GiteaCompatibleOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // Forgejo

    fun forgejo(@DelegatesTo(value = SelfHostedGitea::class) closure: Closure<*>): NamedDomainObjectProvider<SelfHostedGitea> {
        return forgejo {
            project.configure(it, closure)
        }
    }

    fun forgejo(action: Action<SelfHostedGitea>): NamedDomainObjectProvider<SelfHostedGitea> {
        return forgejo("forgejo", action)
    }

    fun forgejo(name: String, @DelegatesTo(value = SelfHostedGitea::class) closure: Closure<*>): NamedDomainObjectProvider<SelfHostedGitea> {
        return forgejo(name) {
            project.configure(it, closure)
        }
    }

    fun forgejo(name: String, action: Action<SelfHostedGitea>): NamedDomainObjectProvider<SelfHostedGitea> {
        return platforms.maybeRegister(name) { it ->
            it.hostType.set(GiteaCompatiblePlatform.FORGEJO)
            action.execute(it)
        }
    }

    fun forgejoOptions(@DelegatesTo(value = SelfHostedGitea::class) closure: Closure<*>): Provider<GiteaCompatibleOptions> {
        return forgejoOptions {
            project.configure(it, closure)
        }
    }

    fun forgejoOptions(action: Action<GiteaCompatibleOptions>): Provider<GiteaCompatibleOptions> {
        return configureOptions(GiteaCompatibleOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // Codeberg

    fun codeberg(@DelegatesTo(value = Codeberg ::class) closure: Closure<*>): NamedDomainObjectProvider<Codeberg> {
        return codeberg {
            project.configure(it, closure)
        }
    }

    fun codeberg(action: Action<Codeberg>): NamedDomainObjectProvider<Codeberg> {
        return codeberg("codeberg", action)
    }

    fun codeberg(name: String, @DelegatesTo(value = Codeberg::class) closure: Closure<*>): NamedDomainObjectProvider<Codeberg> {
        return codeberg(name) {
            project.configure(it, closure)
        }
    }

    fun codeberg(name: String, action: Action<Codeberg>): NamedDomainObjectProvider<Codeberg> {
        return platforms.maybeRegister(name, action)
    }

    fun codebergOptions(@DelegatesTo(value = Codeberg::class) closure: Closure<*>): Provider<GiteaCompatibleOptions> {
        return codebergOptions {
            project.configure(it, closure)
        }
    }

    fun codebergOptions(action: Action<GiteaCompatibleOptions>): Provider<GiteaCompatibleOptions> {
        return configureOptions(GiteaCompatibleOptions::class) {
            it.from(this)
            action.execute(it)
        }
    }

    // GitLab

    fun gitlab(@DelegatesTo(value = Gitlab::class) closure: Closure<*>): NamedDomainObjectProvider<Gitlab> {
        return gitlab("gitlab") {
            project.configure(it, closure)
        }
    }

    fun gitlab(action: Action<Gitlab>): NamedDomainObjectProvider<Gitlab> {
        return gitlab("gitlab", action)
    }

    fun gitlab(name: String, @DelegatesTo(value = Gitlab::class) closure: Closure<*>): NamedDomainObjectProvider<Gitlab> {
        return gitlab(name) {
            project.configure(it, closure)
        }
    }

    fun gitlab(name: String, action: Action<Gitlab>): NamedDomainObjectProvider<Gitlab> {
        return platforms.maybeRegister(name, action)
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

    private inline fun <reified T : Any> PolymorphicDomainObjectContainer<in T>.maybeRegister(name: String, action: Action<T>): NamedDomainObjectProvider<T> {
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
