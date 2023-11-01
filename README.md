# Mod Publish Plugin
A modern Gradle plugin to publish mods to a range of destinations.

**Please note this plugin is still under development, breaking changes may be made at anytime!**
Specify an exact version number to prevent unwanted breakages to your build script.

Please make sure to report all issues, and any suggestions on this Github repo!

## Basic usage
Visit the [docs site](https://modmuss50.github.io/mod-publish-plugin/) for more detailed instructions.

Add to your gradle plugins block:

```gradle
plugins {
  id "me.modmuss50.mod-publish-plugin" version "0.4.1"
}
```

Basic example to publish a jar to CurseForge, Modrinth and Github from a Fabric project:
```gradle
publishMods {
    file = remapJar.archiveFile
    changelog = "Hello!"
    type = STABLE
    modLoaders.add("fabric")

    curseforge {
        projectId = "123456"
        projectSlug = "example-project" // Required for discord webhook
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add("1.20.1")

        requires {
            slug = "fabric-api"
        }
    }
    modrinth {
        projectId = "abcdef"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add("1.20.1")
    }
    github {
        repository = "test/example"
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        commitish = "main"
    }

    discord {
        webhookUrl = providers.environmentVariable("DISCORD_WEBHOOK")
    }
}
```

Run the `publishMods` task to publish to all configured destinations.

Visit the [docs site](https://modmuss50.github.io/mod-publish-plugin/) for more detailed instructions.