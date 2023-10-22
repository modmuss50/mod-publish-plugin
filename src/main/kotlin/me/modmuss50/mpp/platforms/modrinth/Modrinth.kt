package me.modmuss50.mpp.platforms.modrinth

import me.modmuss50.mpp.HttpUtils
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
import me.modmuss50.mpp.path
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import javax.inject.Inject
import kotlin.reflect.KClass

interface ModrinthOptions : PlatformOptions, PlatformOptionsInternal<ModrinthOptions>, PlatformDependencyContainer<ModrinthDependency> {
    @get:Input
    val projectId: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val featured: Property<Boolean>

    @get:Input
    val apiEndpoint: Property<String>

    @ApiStatus.Internal
    override fun setInternalDefaults() {
        featured.convention(false)
        apiEndpoint.convention("https://api.modrinth.com/v2")
    }

    fun minecraftVersionRange(project: Project, action: Action<VersionRangeOptions>) {
        val options = project.objects.newInstance(VersionRangeOptions::class.java)
        options.includeSnapshots.convention(false)
        action.execute(options)

        val startId = options.start.get()
        val endId = options.end.get()

        minecraftVersions.addAll(
            project.provider {
                val versions = MinecraftApi().getVersions()
                    .filter { it.type == "release" || options.includeSnapshots.get() }
                    .map { it.id }
                    .reversed()
                val startIndex = versions.indexOf(startId)
                val endIndex = versions.indexOf(endId)

                if (startIndex == -1) throw IllegalArgumentException("Invalid start version $startId")
                if (endIndex == -1) throw IllegalArgumentException("Invalid end version $endId")
                if (startIndex > endIndex) throw IllegalArgumentException("Start version $startId must be before end version $endId")
                if (startIndex == endIndex) throw IllegalArgumentException("Start version $startId cannot be the same as end version $endId")

                versions.subList(startIndex, endIndex + 1)
            },
        )
    }

    fun from(other: ModrinthOptions) {
        super.from(other)
        fromDependencies(other)
        projectId.set(other.projectId)
        minecraftVersions.set(other.minecraftVersions)
        featured.set(other.featured)
        apiEndpoint.set(other.apiEndpoint)
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

interface VersionRangeOptions {
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
    override fun publish(context: PublishContext) {
        context.submit(UploadWorkAction::class) {
            it.from(this)
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

                // Use the project id if we have it
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

                return ModrinthApi.Dependency(
                    projectId = projectId,
                    versionId = version.orNull,
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
