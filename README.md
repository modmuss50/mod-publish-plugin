# Mod Publish Plugin
A modern Gradle plugin to publish mods to a range of destinations.

**Please note this plugin is still under development, breaking changes may be made at anytime!**
Specify an exact version number to prevent unwanted breakages.

## Basic usage

Run the `publishMods` task to publish to all configured destinations

Basic example to publish a jar to CurseForge and Modrinth from a Fabric project:

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
}
```

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
- Supports CurseForge and Modrinth
- Typesafe DSL to easily publish to multiple locations with minimal repetition 
- Retry on failure
- Dry run mode to try and increase confidence in your buildscript before releases.
- Built with modern Gradle features

### Future plans
- Github
- Create some detailed documentation/working examples for Fabric and Forge + add some docs to the code.