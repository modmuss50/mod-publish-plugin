package me.modmuss50.mpp.platforms.modrinth

enum class ModrinthEnvironment {
    /** All functionality is on the client-side. */
    CLIENT_ONLY,

    /** All functionality is on the server-side, functionality also present in singleplayer. */
    SERVER_ONLY,

    /** Only runs on a dedicated server. No functionality when in singleplayer. */
    DEDICATED_SERVER_ONLY,

    /** Requires both the client and server to have the project installed. */
    CLIENT_AND_SERVER,

    /** Requires the server to have the project installed, but the client can optionally have it installed for extra functionality. */
    SERVER_ONLY_CLIENT_OPTIONAL,

    /** Requires the client to have the project installed, but the server can optionally have it installed for extra functionality. */
    CLIENT_ONLY_SERVER_OPTIONAL,

    /** Can run on either the client or server, but has extra functionality if both have the project installed. */
    CLIENT_OR_SERVER_PREFERS_BOTH,

    /** Can run on either the client or server, but has no extra functionality if both have the project installed. */
    CLIENT_OR_SERVER,

    /** Only runs in singleplayer. No functionality in multiplayer. */
    SINGLEPLAYER_ONLY,
}