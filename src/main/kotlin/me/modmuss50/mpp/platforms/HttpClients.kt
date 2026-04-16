package me.modmuss50.mpp.platforms

import me.modmuss50.mpp.networking.HttpApi

object HttpClients {
    val defaultClient = HttpApi(HttpProfiles.default)
    val modrinthClient = HttpApi(HttpProfiles.modrinth)
    val giteaClient = HttpApi(HttpProfiles.gitea)
    val gitlabClient = HttpApi(HttpProfiles.gitlab)
    val curseforgeClient = HttpApi(HttpProfiles.curseforge)
    val discordClient = HttpApi(HttpProfiles.discord)
}
