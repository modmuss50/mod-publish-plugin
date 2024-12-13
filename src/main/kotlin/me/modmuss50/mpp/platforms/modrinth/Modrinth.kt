package me.modmuss50.mpp.platforms.modrinth

import me.modmuss50.mpp.HttpUtils
import me.modmuss50.mpp.MinecraftApi
import me.modmuss50.mpp.ModrinthPublishResult
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
import me.modmuss50.mpp.Validators
import me.modmuss50.mpp.path
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import javax.inject.Inject
import kotlin.random.Random
import kotlin.reflect.KClass

interface ModrinthOptions : PlatformOptions, PlatformOptionsInternal<ModrinthOptions>, ModrinthDependencyContainer {
    companion object {
        // https://github.com/modrinth/labrinth/blob/ae1c5342f2017c1c93008d1e87f1a29549dca92f/src/scheduler.rs#L112
        @JvmStatic
        val WALL_OF_SHAME = mapOf(
            "1.14.2 Pre-Release 4" to "1.14.2-pre4",
            "1.14.2 Pre-Release 3" to "1.14.2-pre3",
            "1.14.2 Pre-Release 2" to "1.14.2-pre2",
            "1.14.2 Pre-Release 1" to "1.14.2-pre1",
            "1.14.1 Pre-Release 2" to "1.14.1-pre2",
            "1.14.1 Pre-Release 1" to "1.14.1-pre1",
            "1.14 Pre-Release 5" to "1.14-pre5",
            "1.14 Pre-Release 4" to "1.14-pre4",
            "1.14 Pre-Release 3" to "1.14-pre3",
            "1.14 Pre-Release 2" to "1.14-pre2",
            "1.14 Pre-Release 1" to "1.14-pre1",
            "3D Shareware v1.34" to "3D-Shareware-v1.34",
        )
    }

    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val featured: Property<Boolean>

    /**
     * When set, this will update the project description to the provided value.
     */
    @get:Input
    @get:Optional
    val projectDescription: Property<String>

    @get:Input
    val apiEndpoint: Property<String>

    @ApiStatus.Internal
    override fun setInternalDefaults() {
        featured.convention(false)
        apiEndpoint.convention("https://api.modrinth.com/v2")
    }

    fun minecraftVersionRange(action: Action<ModrinthVersionRangeOptions>) {
        val options = objectFactory.newInstance(ModrinthVersionRangeOptions::class.java)
        options.includeSnapshots.convention(false)
        action.execute(options)

        val startId = options.start.get()
        val endId = options.end.get()
        val includeSnapshots = options.includeSnapshots.get()

        minecraftVersions.addAll(
            providerFactory.provider {
                MinecraftApi().getVersionsInRange(startId, endId, includeSnapshots).map { WALL_OF_SHAME.getOrDefault(it, it) }
            },
        )
    }

    fun from(other: ModrinthOptions) {
        super.from(other)
        fromDependencies(other)
        projectId.convention(other.projectId)
        minecraftVersions.convention(other.minecraftVersions)
        featured.convention(other.featured)
        projectDescription.convention(other.projectDescription)
        apiEndpoint.convention(other.apiEndpoint)
    }

    fun from(other: Provider<ModrinthOptions>) {
        from(other.get())
    }

    fun from(other: Provider<ModrinthOptions>, publishOptions: Provider<PublishOptions>) {
        from(other)
        from(publishOptions.get())
    }

    override val platformDependencyKClass: KClass<ModrinthDependency>
        get() = ModrinthDependency::class
}

/**
 * Provides shorthand methods for adding dependencies to modrinth
 */
interface ModrinthDependencyContainer : PlatformDependencyContainer<ModrinthDependency> {
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
                objectFactory.newInstance(ModrinthDependency::class.java).apply {
                    this.slug.set(it)
                    this.type.set(type)
                },
            )
        }
    }
}

interface ModrinthDependency : PlatformDependency {
    @get:Input
    @get:Optional
    val id: Property<String>

    @get:Input
    @get:Optional
    val slug: Property<String>

    @get:Input
    @get:Optional
    val version: Property<String>

    @Deprecated("For removal", ReplaceWith("id"))
    val projectId: Property<String> get() = id
}

interface ModrinthVersionRangeOptions {
    /**
     * The start version of the range (inclusive)
     */
    val start: Property<String>

    /**
     * The end version of the range (exclusive)
     */
    val end: Property<String>

    /**
     * Whether to include snapshot versions in the range
     */
    val includeSnapshots: Property<Boolean>
}

abstract class Modrinth @Inject constructor(name: String) : Platform(name), ModrinthOptions {
    override fun validateInputs() {
        super.validateInputs()
        Validators.validateUnique("minecraftVersions", minecraftVersions)
    }

    override fun publish(context: PublishContext) {
        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    override fun dryRunPublishResult(): PublishResult {
        return ModrinthPublishResult(
            // Use a random file ID so that the URL is different each time, this is needed because discord drops duplicate URLs
            id = "${Random.nextInt(0, 1000000)}",
            projectId = "dry-run",
            title = announcementTitle.getOrElse("Download from Modrinth"),
        )
    }

    override fun printDryRunInfo(logger: Logger) {
        for (dependency in dependencies.get()) {
            val idOrSlug = dependency.id.orNull ?: dependency.slug.get()
            logger.lifecycle("Dependency(id/slug: $idOrSlug, version: ${dependency.version.orNull})")
        }
    }

    interface UploadParams : PublishWorkParameters, ModrinthOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val api = ModrinthApi(accessToken.get(), apiEndpoint.get())

                val primaryFileKey = "primaryFile"
                val files = HashMap<String, Path>()
                files[primaryFileKey] = file.path

                additionalFiles.files.forEachIndexed { index, additionalFile ->
                    files["file_$index"] = additionalFile.toPath()
                }

                val dependencies = dependencies.get().map { toApiDependency(it, api) }

                val metadata = ModrinthApi.CreateVersion(
                    name = displayName.get(),
                    versionNumber = version.get(),
                    changelog = changelog.orNull,
                    dependencies = dependencies,
                    gameVersions = minecraftVersions.get(),
                    versionType = ModrinthApi.VersionType.valueOf(type.get()),
                    loaders = modLoaders.get().map { it.lowercase() },
                    featured = featured.get(),
                    projectId = projectId.get().modrinthId,
                    fileParts = files.keys.toList(),
                    primaryFile = primaryFileKey,
                )

                val response = HttpUtils.retry(maxRetries.get(), "Failed to create version") {
                    api.createVersion(metadata, files)
                }

                if (projectDescription.isPresent) {
                    HttpUtils.retry(maxRetries.get(), "Failed to update project description") {
                        api.modifyProject(projectId.get().modrinthId, ModrinthApi.ModifyProject(body = projectDescription.get()))
                    }
                }

                return ModrinthPublishResult(
                    id = response.id,
                    projectId = response.projectId,
                    title = announcementTitle.getOrElse("Download from Modrinth"),
                )
            }
        }

        private fun toApiDependency(dependency: ModrinthDependency, api: ModrinthApi): ModrinthApi.Dependency {
            with(dependency) {
                var projectId: String? = null
                var versionId: String? = null

                // Use the project ID if we have it
                if (id.isPresent) {
                    projectId = id.get().modrinthId
                }

                // Lookup the project ID from the slug
                if (slug.isPresent) {
                    // Don't allow a slug and id to both be specified
                    if (projectId != null) {
                        throw IllegalStateException("Modrinth dependency cannot specify both projectId and projectSlug")
                    }

                    projectId = HttpUtils.retry(parameters.maxRetries.get(), "Failed to lookup project id from slug: ${slug.get()}") {
                        api.checkProject(slug.get())
                    }.id
                }

                // Ensure we have an id
                if (projectId == null) {
                    throw IllegalStateException("Modrinth dependency has no configured projectId or projectSlug value")
                }

                if (version.isPresent) {
                    val response = HttpUtils.retry(parameters.maxRetries.get(), "Failed to list versions from slug/id: ${version.get()}") {
                        api.listVersions(projectId)
                    }

                    val versions = response.filter {
                        it.id == version.get() || it.versionNumber == version.get()
                    }

                    versionId = when (versions.size) {
                        0 -> throw IllegalStateException("Modrinth dependency has a version configured but no matches found for version: ${version.get()}")
                        1 -> versions.first().id
                        else -> throw IllegalStateException("Modrinth dependency has a version configured but multiple matches found for version: ${version.get()}")
                    }
                }

                return ModrinthApi.Dependency(
                    projectId = projectId,
                    versionId = versionId,
                    dependencyType = ModrinthApi.DependencyType.valueOf(type.get()),
                )
            }
        }
    }
}

private val ID_REGEX = Regex("[0-9a-zA-Z]{8}")

// Returns a validated ModrithID
private val String.modrinthId: String
    get() {
        if (!this.matches(ID_REGEX)) {
            throw IllegalArgumentException("$this is not a valid Modrinth ID")
        }

        return this
    }
