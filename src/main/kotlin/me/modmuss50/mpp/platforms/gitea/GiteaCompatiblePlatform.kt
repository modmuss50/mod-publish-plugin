package me.modmuss50.mpp.platforms.gitea

enum class GiteaCompatiblePlatform(
    val friendlyString: String,
    val defaultBrandColor: Int,
) {
    GITEA("Gitea", 0x1d8f4a),
    FORGEJO("Forgejo", 0xff5500),
    CODEBERG("Codeberg", 0xff2185D0.toInt()),
}
