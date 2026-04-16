package me.modmuss50.mpp.platforms.context

import org.slf4j.LoggerFactory

object Retry {
    private val LOGGER = LoggerFactory.getLogger(Retry::class.java)

    /**
     * Retry server errors
     */
    fun <T> run(
        maxRetries: Int,
        message: String,
        closure: () -> T,
    ): T {
        var exception: RuntimeException? = null
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
        throw exception!!
    }
}
