package me.modmuss50.mpp.platforms.curseforge

import me.modmuss50.mpp.CurseForgePublishResult
import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.MinecraftApi
import me.modmuss50.mpp.Platform
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.PlatformDependencyContainer
import me.modmuss50.mpp.PlatformOptions
import me.modmuss50.mpp.PlatformOptionsInternal
import me.modmuss50.mpp.PublishContext
import me.modmuss50.mpp.PublishOptions
import me.modmuss50.mpp.PublishResult
import me.modmuss50.mpp.PublishWorkAction
import me.modmuss50.mpp.PublishWorkParameters
import me.modmuss50.mpp.path
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import javax.inject.Inject
import kotlin.random.Random
import kotlin.reflect.KClass

interface CurseforgeOptions : PlatformOptions, PlatformOptionsInternal<CurseforgeOptions>, CurseforgeDependencyContainer {
    @get:Input
    val projectId: Property<String>

    // Project slug, used by discord webhook to link to the uploaded file.
    @get:Input
    @get:Optional
    val projectSlug: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    @get:Optional
    val clientRequired: Property<Boolean>

    @get:Input
    @get:Optional
    val serverRequired: Property<Boolean>

    @get:Input
    val javaVersions: ListProperty<JavaVersion>

    @get:Input
    val apiEndpoint: Property<String>

    // Set the type of changelog for CurseForge. If unset, defaults to MARKDOWN
    @get:Input
    val changelogType: Property<CurseforgeApi.ChangelogType>

    fun from(other: CurseforgeOptions) {
        super.from(other)
        fromDependencies(other)
        projectId.set(other.projectId)
        projectSlug.set(other.projectSlug)
        minecraftVersions.set(other.minecraftVersions)
        clientRequired.set(other.clientRequired)
        serverRequired.set(other.serverRequired)
        javaVersions.set(other.javaVersions)
        apiEndpoint.set(other.apiEndpoint)
        changelogType.set(other.changelogType)
    }

    fun from(other: Provider<CurseforgeOptions>) {
        from(other.get())
    }

    fun from(other: Provider<CurseforgeOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }

    fun minecraftVersionRange(action: Action<CurseforgeVersionRangeOptions>) {
        val options = objectFactory.newInstance(CurseforgeVersionRangeOptions::class.java)
        action.execute(options)

        val startId = options.start.get()
        val endId = options.end.get()

        minecraftVersions.addAll(
            providerFactory.provider {
                MinecraftApi().getVersionsInRange(startId, endId)
            },
        )
    }

    override fun setInternalDefaults() {
        apiEndpoint.convention("https://minecraft.curseforge.com")
        changelogType.convention(CurseforgeApi.ChangelogType.MARKDOWN)
    }

    override val platformDependencyKClass: KClass<CurseforgeDependency>
        get() = CurseforgeDependency::class
}

interface CurseforgeDependency : PlatformDependency {
    @get:Input
    val slug: Property<String>
}

interface CurseforgeVersionRangeOptions {
    /**
     * The start version of the range (inclusive)
     */
    val start: Property<String>

    /**
     * The end version of the range (exclusive)
     */
    val end: Property<String>
}

/**
 * Provides shorthand methods for adding dependencies to curseforge
 */
interface CurseforgeDependencyContainer : PlatformDependencyContainer<CurseforgeDependency> {
    fun requires(vararg slugs: String) {
        addInternal(PlatformDependency.DependencyType.REQUIRED, slugs)
    }
    fun optional(vararg slugs: String) {
        addInternal(PlatformDependency.DependencyType.OPTIONAL, slugs)
    }
    fun incompatible(vararg slugs: String) {
        addInternal(PlatformDependency.DependencyType.INCOMPATIBLE, slugs)
    }
    fun embeds(vararg slugs: String) {
        addInternal(PlatformDependency.DependencyType.EMBEDDED, slugs)
    }

    @Internal
    fun addInternal(type: PlatformDependency.DependencyType, slugs: Array<out String>) {
        slugs.forEach {
            dependencies.add(
                objectFactory.newInstance(CurseforgeDependency::class.java).apply {
                    this.slug.set(it)
                    this.type.set(type)
                },
            )
        }
    }
}

abstract class Curseforge @Inject constructor(name: String) : Platform(name), CurseforgeOptions {
    override fun publish(context: PublishContext) {
        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult {
        return CurseForgePublishResult(
            projectId = projectId.get(),
            projectSlug = projectSlug.map { "dry-run" }.orNull,
            // Use a random file ID so that the URL is different each time, this is needed because discord drops duplicate URLs
            fileId = Random.nextInt(0, 1000000),
            title = announcementTitle.getOrElse("Download from CurseForge"),
        )
    }

    override fun printDryRunInfo(logger: Logger) {
        for (dependency in dependencies.get()) {
            logger.lifecycle("Dependency(slug: ${dependency.slug.get()}, type: ${dependency.type.get()})")
        }
    }

    interface UploadParams : PublishWorkParameters, CurseforgeOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val api = CurseforgeApi(accessToken.get(), apiEndpoint.get())
                val versions = CurseforgeVersions(
                    HttpUtils.retry(maxRetries.get(), "Failed to get game version types") {
                        api.getVersionTypes()
                    },
                    HttpUtils.retry(maxRetries.get(), "Failed to get game versions") {
                        api.getGameVersions()
                    },
                )

                val gameVersions = ArrayList<Int>()
                for (version in minecraftVersions.get()) {
                    gameVersions.add(versions.getMinecraftVersion(version))
                }

                for (modLoader in modLoaders.get()) {
                    gameVersions.add(versions.getModLoaderVersion(modLoader))
                }

                if (clientRequired.isPresent && clientRequired.get()) {
                    gameVersions.add(versions.getClientVersion())
                }

                if (serverRequired.isPresent && serverRequired.get()) {
                    gameVersions.add(versions.getServerVersion())
                }

                for (javaVersion in javaVersions.get()) {
                    gameVersions.add(versions.getJavaVersion(javaVersion))
                }

                val projectRelations = dependencies.get().map {
                    CurseforgeApi.ProjectFileRelation(
                        slug = it.slug.get(),
                        type = CurseforgeApi.RelationType.valueOf(it.type.get()),
                    )
                }

                val relations = if (projectRelations.isNotEmpty()) {
                    CurseforgeApi.UploadFileRelations(
                        projects = projectRelations,
                    )
                } else {
                    null
                }

                val metadata = CurseforgeApi.UploadFileMetadata(
                    changelog = changelog.get(),
                    changelogType = changelogType.get(),
                    displayName = displayName.get(),
                    gameVersions = gameVersions,
                    releaseType = CurseforgeApi.ReleaseType.valueOf(type.get()),
                    relations = relations,
                )

                val response = HttpUtils.retry(maxRetries.get(), "Failed to upload file") {
                    api.uploadFile(projectId.get(), file.path, metadata)
                }

                for (additionalFile in additionalFiles.files) {
                    val additionalMetadata = metadata.copy(parentFileID = response.id, gameVersions = null, displayName = null)

                    HttpUtils.retry(maxRetries.get(), "Failed to upload additional file") {
                        api.uploadFile(projectId.get(), additionalFile.toPath(), additionalMetadata)
                    }
                }

                return CurseForgePublishResult(
                    projectId = projectId.get(),
                    projectSlug = projectSlug.orNull,
                    fileId = response.id,
                    title = announcementTitle.getOrElse("Download from CurseForge"),
                )
            }
        }
    }
}
