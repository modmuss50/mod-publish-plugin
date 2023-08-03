package me.modmuss50.mpp

import java.lang.IllegalArgumentException

enum class ReleaseType {
    STABLE,
    BETA,
    ALPHA, ;

    companion object {
        @JvmStatic
        fun of(value: String): ReleaseType {
            val upper = value.uppercase()
            try {
                return ReleaseType.valueOf(upper)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid release type: $upper. Must be one of: STABLE, BETA, ALPHA")
            }
        }
    }
}
