# Mod Publish Plugin
A modern Gradle plugin to publish mods to a range of destinations.

Please make sure to report all issues, and any suggestions on this GitHub repo!

## Basic usage
Visit the [docs site](https://modmuss50.github.io/mod-publish-plugin/) for more detailed instructions.

Add to your Gradle plugins block:

#### Groovy
```groovy
plugins {
  id "me.modmuss50.mod-publish-plugin" version "2.0.0-beta.1"
}
```
#### Kotlin
```kotlin
plugins {
  id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
}
```

It is recommended to specify an exact version number to prevent unwanted breakages to your build script.

Basic example to publish a jar to CurseForge, Modrinth and GitHub from a Fabric project:
#### Groovy
```groovy
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
        requires("fabric-api")
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
#### Kotlin
```kotlin
publishMods {
    file.set(remapJar.archiveFile)
    changelog.set("Hello!")
    type.set(STABLE)
    modLoaders.add("fabric")

    curseforge {
        projectId.set("123456")
        projectSlug.set("example-project") // Required for discord webhook
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.add("1.20.1")
        requires("fabric-api")
    }
    modrinth {
        projectId.set("abcdef")
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.add("1.20.1")
    }
    github {
        repository.set("test/example")
        accessToken.set(providers.environmentVariable("GITHUB_TOKEN"))
        commitish.set("main")
    }
    discord {
        webhookUrl.set(providers.environmentVariable("DISCORD_WEBHOOK"))
    }
}
```

Run the `publishMods` task to publish to all configured destinations.

Visit the [docs site](https://modmuss50.github.io/mod-publish-plugin/) for more detailed instructions.