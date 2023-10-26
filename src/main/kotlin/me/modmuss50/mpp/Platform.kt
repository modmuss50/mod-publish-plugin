package me.modmuss50.mpp

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.annotations.ApiStatus
import javax.inject.Inject
import kotlin.reflect.KClass

interface PlatformOptions : PublishOptions {
    @get:Optional
    @get:Input
    val accessToken: Property<String>

    @get:Optional
    @get:Input
    val announcementTitle: Property<String>

    fun from(other: PlatformOptions) {
        super.from(other)
        accessToken.set(other.accessToken)
        announcementTitle.set(other.announcementTitle)
    }
}

@ApiStatus.Internal
interface PlatformOptionsInternal<T : PlatformOptions> {
    fun setInternalDefaults()
}

interface PlatformDependencyContainer<T : PlatformDependency> {
    @get:Input
    val dependencies: ListProperty<T>

    fun requires(action: Action<T>) {
        addInternal(PlatformDependency.DependencyType.REQUIRED, action)
    }

    fun optional(action: Action<T>) {
        addInternal(PlatformDependency.DependencyType.OPTIONAL, action)
    }

    fun incompatible(action: Action<T>) {
        addInternal(PlatformDependency.DependencyType.INCOMPATIBLE, action)
    }

    fun embeds(action: Action<T>) {
        addInternal(PlatformDependency.DependencyType.EMBEDDED, action)
    }

    fun fromDependencies(other: PlatformDependencyContainer<T>) {
        dependencies.set(other.dependencies)
    }

    @get:ApiStatus.Internal
    @get:Inject
    val objectFactory: ObjectFactory

    @get:ApiStatus.Internal
    @get:Inject
    val providerFactory: ProviderFactory

    @get:ApiStatus.OverrideOnly
    @get:Internal
    val platformDependencyKClass: KClass<T>

    @Internal
    fun addInternal(type: PlatformDependency.DependencyType, action: Action<T>) {
        val dep = objectFactory.newInstance(platformDependencyKClass.java)
        dep.type.set(type)
        dep.type.finalizeValue()
        action.execute(dep)
        dependencies.add(dep)
    }
}

interface PlatformDependency {
    val type: Property<DependencyType>

    enum class DependencyType {
        REQUIRED,
        OPTIONAL,
        INCOMPATIBLE,
        EMBEDDED,
    }
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

abstract class Platform @Inject constructor(private val name: String) : Named, PlatformOptions {
    @ApiStatus.Internal
    abstract fun publish(context: PublishContext)

    @get:ApiStatus.Internal
    @get:Internal
    val taskName: String
        get() = "publish" + name.capitalized()

    init {
        (this as PlatformOptionsInternal<*>).setInternalDefaults()
    }

    @Input
    override fun getName(): String {
        return name
    }
}
