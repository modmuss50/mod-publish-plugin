# Mod Publish Plugin
A modern Gradle plugin to publish mods to a range of destinations.

**Please note this plugin is still under development, breaking changes may be made at anytime!**
Specify an exact version number to prevent unwanted breakages to your build script.

Please make sure to report all issues, and any suggestions on this Github repo!

## Basic usage
Add to your gradle plugins block:
```gradle
plugins {
  id "me.modmuss50.mod-publish-plugin" version "0.1.1"
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
}
```

Run the `publishMods` task to publish to all configured destinations

Example showing multiple CurseForge destinations sharing common options. 

```gradle
publishMods {
    changelog = file("changelog.md").text

    def options = curseforgeOptions {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add("1.20.1")
    }

    curseforge("curseforgeFabric") {
        from options
        projectId = "123456"
        type = STABLE
        file = fabricJar.archiveFile
    }

    curseforge("curseforgeForge") {
        from options
        projectId = "7890123"
        type = BETA
        file = forgeJar.archiveFile
    }
}
```

Full reference of all options

### Features
- Supports CurseForge, Modrinth and Github
- Typesafe DSL to easily publish to multiple locations with minimal repetition 
- Retry on failure
- Dry run mode to try and increase confidence in your buildscript before releases
- Built with modern Gradle features
- Mod loader independent 

### Future plans
- Create some detailed documentation/working examples for Fabric and Forge + add some docs to the code. (Waiting on the project to be a bit more stable first)

### FAQ
- Why use Kotlin?
  - I wanted to explore using Kotlin for a larger project, as Gradle already includes Kotlin there should be little downside to the end user.
- Why do I need to specify the minecraft versions for both CurseForge and Modrinth?
  - Curseforge and Modrinth use different versioning for snapshots, thus they must be defined for each platform.
- Feature x or platform y is not supported
  - Please open an issue on this repo!