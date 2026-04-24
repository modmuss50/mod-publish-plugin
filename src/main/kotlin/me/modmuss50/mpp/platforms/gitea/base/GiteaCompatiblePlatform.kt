package me.modmuss50.mpp.platforms.gitea.base

enum class GiteaCompatiblePlatform(
    val friendlyString: String,
    val defaultBrandColor: Int,
) {
    GITEA("Gitea", 0x1d8f4a),
    FORGEJO("Forgejo", 0xff5500),
    CODEBERG("Codeberg", 0x2185d0),
}
