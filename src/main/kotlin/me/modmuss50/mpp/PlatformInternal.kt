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
import org.intellij.lang.annotations.Language
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
    abstract val title: String
    abstract val brandColor: Int

    companion object {
        fun fromJson(@Language("json") string: String): PublishResult {
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
    override val title: String,
) : PublishResult() {
    override val type: String
        get() = "curseforge"
    override val link: String
        get() {
            if (projectSlug == null) {
                // Thanks CF...
                throw IllegalStateException("The CurseForge projectSlug property must be set to generate a link to the uploaded file")
            }

            return "https://curseforge.com/minecraft/mc-mods/$projectSlug/files/$fileId"
        }
    override val brandColor: Int
        get() = 0xF16436
}

@Serializable
@SerialName("github")
data class GithubPublishResult(
    val repository: String,
    val releaseId: Long,
    val url: String,
    override val title: String,
) : PublishResult() {
    override val type: String
        get() = "github"
    override val link: String
        get() = url
    override val brandColor: Int
        get() = 0xF6F0FC
}

@Serializable
@SerialName("modrinth")
data class ModrinthPublishResult(
    val id: String,
    val projectId: String,
    override val title: String,
) : PublishResult() {
    override val type: String
        get() = "modrinth"
    override val link: String
        get() = "https://modrinth.com/mod/$projectId/version/$id"
    override val brandColor: Int
        get() = 0x1BD96A
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
