package me.modmuss50.mpp.platforms

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.networking.HttpContext
import java.net.http.HttpClient
import java.time.Duration

object HttpProfiles {
    private val defaultJson = Json { ignoreUnknownKeys = true }
    private val defaultAgent = "modmuss50/mod-publish-plugin/${HttpProfiles::class.java.`package`.implementationVersion}"
    private val defaultClient = client(Duration.ofSeconds(30))

    val default =
        HttpContext(
            client = defaultClient,
            json = defaultJson,
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.default,
        )
    val modrinth =
        HttpContext(
            client = client(Duration.ofSeconds(60)),
            json = defaultJson,
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.modrinth,
        )
    val gitea =
        HttpContext(
            client = defaultClient,
            json = defaultJson,
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.gitea,
        )

    @OptIn(ExperimentalSerializationApi::class)
    val gitlab =
        HttpContext(
            client = defaultClient,
            json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.default,
        )

    @OptIn(ExperimentalSerializationApi::class)
    val curseforge =
        HttpContext(
            client = defaultClient,
            json =
            Json {
                ignoreUnknownKeys = true // Added on 4.16.26, may be re-evaluated later
                explicitNulls = false
            },
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.curseforge,
        )

    @OptIn(ExperimentalSerializationApi::class)
    val discord =
        HttpContext(
            client = defaultClient,
            json =
            Json {
                explicitNulls = false
                classDiscriminator = "class"
                encodeDefaults = true
            },
            userAgent = defaultAgent,
            exceptionFactory = HttpExceptionFactories.default,
        )

    private fun client(timeout: Duration) =
        HttpClient
            .newBuilder()
            .connectTimeout(timeout)
            .build()
}
