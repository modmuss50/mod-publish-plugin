package me.modmuss50.mpp.networking

import java.io.File
import java.net.http.HttpRequest
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Utility for constructing `multipart/form-data` request bodies compatible with
 * Java's [HttpRequest] API.
 *
 * This builder accumulates form parts (text and files) and produces a single
 * [HttpRequest.BodyPublisher] when [build] is called.
 *
 * A unique boundary is generated per instance and **must** be included in the
 * `Content-Type` header via [getContentType].
 */
class MultipartBodyBuilder {
    /**
     * Unique boundary string separating parts in the multipart payload.
     *
     * This value is randomly generated and stripped of dashes to ensure it is
     * safe for use in HTTP headers and body formatting.
     */
    private val boundary = UUID.randomUUID().toString().replace("-", "")

    /**
     * Internal list of parts that will be serialized into the final request body.
     */
    private val parts = mutableListOf<Part>()

    /**
     * Adds a simple text form field.
     *
     * @param name the form field name (used in the `Content-Disposition` header).
     * @param value the string value of the field, encoded as UTF-8.
     *
     * @return this builder instance for chaining.
     */
    fun addFormDataPart(
        name: String,
        value: String,
    ): MultipartBodyBuilder {
        parts.add(TextPart(name, value))
        return this
    }

    /**
     * Adds a file form field using a [File].
     *
     * @param name the form field name (used in the `Content-Disposition` header).
     * @param filename the filename reported in the multipart payload (does not have to match the actual file name).
     * @param file the file to upload.
     * @param mediaType optional MIME type; if null, it is resolved using [Files.probeContentType], or falls back to `application/octet-stream` if detection fails.
     *
     * @return this builder instance for chaining.
     */
    fun addFormDataPart(
        name: String,
        filename: String,
        file: File,
        mediaType: String? = null,
    ): MultipartBodyBuilder {
        parts.add(FilePart(name, filename, file.toPath(), mediaType))
        return this
    }

    /**
     * Adds a file form field using a [Path].
     *
     * @param name the form field name (used in the `Content-Disposition` header).
     * @param filename the filename reported in the multipart payload (does not have to match the actual file name).
     * @param path the file path to upload.
     * @param mediaType optional MIME type; if null, it is resolved using [Files.probeContentType], or falls back to `application/octet-stream` if detection fails.
     *
     * @return this builder instance for chaining.
     */
    fun addFormDataPart(
        name: String,
        filename: String,
        path: Path,
        mediaType: String? = null,
    ): MultipartBodyBuilder {
        parts.add(FilePart(name, filename, path, mediaType))
        return this
    }

    /**
     * Builds the multipart body as a single [HttpRequest.BodyPublisher].
     *
     * Each part is serialized with:
     * - a leading boundary (`--boundary`)
     * - appropriate headers
     * - its content
     * - a trailing CRLF
     * - a closing boundary (`--boundary--`)
     *
     * @return a [HttpRequest.BodyPublisher] containing the complete multipart payload.
     */
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

    /**
     * Returns the `Content-Type` header value for this multipart request.
     *
     * This value must be set on the HTTP request so the server can correctly
     * parse the multipart body using the same boundary.
     *
     * @return a `multipart/form-data` content type string including the boundary parameter.
     */
    fun getContentType(): String = "multipart/form-data; boundary=$boundary"

    /**
     * Represents a single multipart section.
     */
    private sealed class Part {
        /**
         * Serializes this part into a list of byte arrays representing:
         * - headers
         * - content
         * - trailing CRLF
         *
         * These byte arrays are later concatenated into the final payload.
         *
         * @return a list of byte arrays representing this part.
         */
        abstract fun getBytes(): List<ByteArray>
    }

    /**
     * A simple text-based form field.
     *
     * @property name the form field name.
     * @property value the UTF-8 encoded value of the field.
     */
    private class TextPart(
        val name: String,
        val value: String,
    ) : Part() {
        /**
         * Encodes the text part including its `Content-Disposition` header and value.
         *
         * @return a list of byte arrays containing headers, value, and trailing CRLF.
         */
        override fun getBytes(): List<ByteArray> {
            val result = mutableListOf<ByteArray>()
            result.add("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
            result.add(value.toByteArray(StandardCharsets.UTF_8))
            result.add("\r\n".toByteArray(StandardCharsets.UTF_8))
            return result
        }
    }

    /**
     * A file-based form field.
     *
     * @property name the form field name.
     * @property filename the filename reported in the multipart payload.
     * @property path the path to the file being uploaded.
     * @property mediaType optional MIME type override.
     */
    private class FilePart(
        val name: String,
        val filename: String,
        val path: Path,
        val mediaType: String?,
    ) : Part() {
        /**
         * Encodes the file part including headers and raw file contents.
         *
         * The content type is resolved in the following order:
         * 1. Explicit [mediaType] if provided
         * 2. Auto-detected via [Files.probeContentType]
         * 3. Fallback to `application/octet-stream`
         *
         * @return a list of byte arrays containing headers, file bytes, and trailing CRLF.
         */
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
