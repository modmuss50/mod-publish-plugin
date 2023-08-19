package me.modmuss50.mpp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Action
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.jetbrains.annotations.ApiStatus
import java.lang.IllegalStateException
import kotlin.reflect.KClass

@ApiStatus.Internal
interface PublishWorkParameters : WorkParameters {
    val result: RegularFileProperty
}

@ApiStatus.Internal
interface PublishWorkAction<T : PublishWorkParameters> : WorkAction<T> {
    fun publish(): PublishResult

    override fun execute() {
        val result = publish()

        parameters.result.get().asFile.writeText(
            Json.encodeToString(result),
        )
    }
}

@ApiStatus.Internal
@Serializable
sealed class PublishResult {
    abstract val type: String
    abstract val link: String

    companion object {
        fun fromJson(string: String): PublishResult {
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString(string)
        }
    }
}

@Serializable
@SerialName("curseforge")
data class CurseForgePublishResult(
    val projectId: String,
    val projectSlug: String?,
    val fileId: Int,
) : PublishResult() {
    override val type: String
        get() = "curseforge"
    override val link: String
        get() {
            if (projectSlug == null) {
                // Thanks CF...
                throw IllegalStateException("The Curseforge projectSlug property must be set to generate a link to the uploaded file")
            }

            return "https://curseforge.com/minecraft/mc-mods/$projectSlug/files/$fileId"
        }
}

@Serializable
@SerialName("github")
data class GithubPublishResult(
    val repository: String,
    val releaseId: Long,
    val url: String,
) : PublishResult() {
    override val type: String
        get() = "github"
    override val link: String
        get() = url
}

@Serializable
@SerialName("modrinth")
data class ModrinthPublishResult(
    val id: String,
    val projectId: String,
) : PublishResult() {
    override val type: String
        get() = "modrinth"
    override val link: String
        get() = "https://modrinth.com/mod/$projectId/version/$id"
}

@ApiStatus.Internal
class PublishContext(private val queue: WorkQueue, private val result: RegularFile) {
    fun <T : PublishWorkParameters> submit(workActionClass: KClass<out PublishWorkAction<T>>, parameterAction: Action<in T>) {
        queue.submit(workActionClass.java) {
            it.result.set(result)
            parameterAction.execute(it)
        }
    }
}
