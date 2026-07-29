package me.modmuss50.mpp

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection

internal object GradleUtils {
    fun fileCollection(
        project: Project,
        file: Any,
    ): ConfigurableFileCollection =
        project.objects.fileCollection().from(
            if (file is Project) {
                project.configurations
                    .detachedConfiguration(project.dependencyFactory.create(file).setTransitive(false))
                    .elements
                    .map { it.single().asFile }
            } else {
                file
            },
        )
}
