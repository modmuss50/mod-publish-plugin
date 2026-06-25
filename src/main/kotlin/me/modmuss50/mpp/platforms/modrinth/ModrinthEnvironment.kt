package me.modmuss50.mpp.platforms.modrinth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ModrinthEnvironment {
    /** All functionality is on the client-side. */
    @SerialName("client_only")
    CLIENT_ONLY,

    /** All functionality is on the server-side, functionality also present in singleplayer. */
    @SerialName("server_only")
    SERVER_ONLY,

    /** Only runs on a dedicated server. No functionality when in singleplayer. */
    @SerialName("dedicated_server_only")
    DEDICATED_SERVER_ONLY,

    /** Requires both the client and server to have the project installed. */
    @SerialName("client_and_server")
    CLIENT_AND_SERVER,

    /** Requires the server to have the project installed, but the client can optionally have it installed for extra functionality. */
    @SerialName("server_only_client_optional")
    SERVER_ONLY_CLIENT_OPTIONAL,

    /** Requires the client to have the project installed, but the server can optionally have it installed for extra functionality. */
    @SerialName("client_only_server_optional")
    CLIENT_ONLY_SERVER_OPTIONAL,

    /** Can run on either the client or server, but has extra functionality if both have the project installed. */
    @SerialName("client_or_server_prefers_both")
    CLIENT_OR_SERVER_PREFERS_BOTH,

    /** Can run on either the client or server, but has no extra functionality if both have the project installed. */
    @SerialName("client_or_server")
    CLIENT_OR_SERVER,

    /** Only runs in singleplayer. No functionality in multiplayer. */
    @SerialName("singleplayer_only")
    SINGLEPLAYER_ONLY,
}
