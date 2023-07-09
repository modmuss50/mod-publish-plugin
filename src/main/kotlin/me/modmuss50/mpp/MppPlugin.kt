package me.modmuss50.mpp

import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.github.Github
import me.modmuss50.mpp.platforms.modrith.Modrith
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

@Suppress("unused")
class MppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("publishMods", ModPublishExtension::class.java, project)

        extension.platforms.registerFactory(Curseforge::class.java) {
            project.objects.newInstance(Curseforge::class.java, it)
        }
        extension.platforms.registerFactory(Github::class.java) {
            project.objects.newInstance(Github::class.java, it)
        }
        extension.platforms.registerFactory(Modrith::class.java) {
            project.objects.newInstance(Modrith::class.java, it)
        }

        val publishModsTask = project.tasks.register("publishMods") {
            it.group = "publishing"
        }

        extension.platforms.whenObjectAdded { platform ->
            val publishPlatformTask = configureTask(project, platform)

            publishModsTask.configure { task ->
                task.dependsOn(publishPlatformTask)
            }
        }
    }

    private fun configureTask(project: Project, platform: Platform): TaskProvider<PublishModTask> {
        return project.tasks.register("publish" + platform.name.capitalized(), PublishModTask::class.java, platform)
    }
}
