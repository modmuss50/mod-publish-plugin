package me.modmuss50.mpp.networking

import java.io.File
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Helper class to build multipart/form-data requests for Java HttpClient
 */
class MultipartBodyBuilder {
    private val boundary = UUID.randomUUID().toString().replace("-", "")
    private val parts = mutableListOf<Part>()

    fun addFormDataPart(name: String, value: String): MultipartBodyBuilder {
        parts.add(TextPart(name, value))
        return this
    }

    fun addFormDataPart(name: String, filename: String, file: File, mediaType: String? = null): MultipartBodyBuilder {
        parts.add(FilePart(name, filename, file.toPath(), mediaType))
        return this
    }

    fun addFormDataPart(name: String, filename: String, path: Path, mediaType: String? = null): MultipartBodyBuilder {
        parts.add(FilePart(name, filename, path, mediaType))
        return this
    }

    fun build(): HttpRequest.BodyPublisher {
        val byteArrays = mutableListOf<ByteArray>()

        for (part in parts) {
            byteArrays.add("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
            byteArrays.addAll(part.getBytes())
        }

        byteArrays.add("--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))

        val totalBytes = byteArrays.sumOf { it.size }
        val result = ByteArray(totalBytes)
        var offset = 0

        for (bytes in byteArrays) {
            System.arraycopy(bytes, 0, result, offset, bytes.size)
            offset += bytes.size
        }

        return HttpRequest.BodyPublishers.ofByteArray(result)
    }

    fun getContentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    private sealed class Part {
        abstract fun getBytes(): List<ByteArray>
    }

    private class TextPart(
        val name: String,
        val value: String,
    ) : Part() {
        override fun getBytes(): List<ByteArray> {
            val result = mutableListOf<ByteArray>()
            result.add("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
            result.add(value.toByteArray(StandardCharsets.UTF_8))
            result.add("\r\n".toByteArray(StandardCharsets.UTF_8))
            return result
        }
    }

    private class FilePart(
        val name: String,
        val filename: String,
        val path: Path,
        val mediaType: String?,
    ) : Part() {
        override fun getBytes(): List<ByteArray> {
            val result = mutableListOf<ByteArray>()
            val contentType = mediaType ?: Files.probeContentType(path) ?: "application/octet-stream"

            result.add(
                "Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n".toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            result.add("Content-Type: $contentType\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
            result.add(Files.readAllBytes(path))
            result.add("\r\n".toByteArray(StandardCharsets.UTF_8))

            return result
        }
    }
}
