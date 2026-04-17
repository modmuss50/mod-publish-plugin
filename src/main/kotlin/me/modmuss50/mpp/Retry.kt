package me.modmuss50.mpp

import org.slf4j.LoggerFactory

object Retry {
    private val LOGGER = LoggerFactory.getLogger(Retry::class.java)

    /**
     * Retry on exceptions
     */
    fun <T> run(
        maxRetries: Int,
        message: String,
        closure: () -> T,
    ): T {
        require(0 != maxRetries) { "maxRetries cannot be 0" }
        var exception: RuntimeException? = null // Potentially remove this nullable sentinel later
        var count = 0

        while (count < maxRetries) {
            try {
                return closure()
            } catch (e: Exception) {
                count++
                exception = exception ?: RuntimeException("$message after $maxRetries attempts with error: ${e.message}")
                exception.addSuppressed(e)
            }
        }

        LOGGER.error("$message failed after $maxRetries retries", exception)
        throw exception ?: error("Retry logic bug: no exception captured despite failures")
    }
}
