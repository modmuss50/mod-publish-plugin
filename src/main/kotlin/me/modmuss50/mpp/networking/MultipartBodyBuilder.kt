package me.modmuss50.mpp.networking

import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import java.io.File
import java.nio.file.Path

class MultipartBodyBuilder {
    private val parts = mutableListOf<Part>()

    fun addFormDataPart(
        name: String,
        value: String,
    ): MultipartBodyBuilder {
        parts.add(Part.Text(name, value))
        return this
    }

    fun addFormDataPart(
        name: String,
        filename: String,
        file: File,
        mediaType: String? = null,
    ): MultipartBodyBuilder {
        parts.add(Part.File(name, filename, file.toPath(), mediaType))
        return this
    }

    fun addFormDataPart(
        name: String,
        filename: String,
        path: Path,
        mediaType: String? = null,
    ): MultipartBodyBuilder {
        parts.add(Part.File(name, filename, path, mediaType))
        return this
    }

    fun build(): List<PartData> =
        parts.map { part ->
            when (part) {
                is Part.Text -> {
                    formData {
                        append(part.name, part.value)
                    }.first()
                }

                is Part.File -> {
                    formData {
                        append(
                            key = part.name,
                            value = part.path.toFile().readBytes(),
                            headers =
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${part.filename}\"")
                                    append(
                                        HttpHeaders.ContentType,
                                        part.mediaType ?: ContentType.Application.OctetStream.toString(),
                                    )
                                },
                        )
                    }.first()
                }
            }
        }

    sealed class Part {
        data class Text(
            val name: String,
            val value: String,
        ) : Part()

        data class File(
            val name: String,
            val filename: String,
            val path: Path,
            val mediaType: String?,
        ) : Part()
    }
}
