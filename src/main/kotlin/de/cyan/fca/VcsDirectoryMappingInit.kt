package de.cyan.fca

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class VcsDirectoryMappingInit : ProjectActivity {
    override suspend fun execute(project: Project) {
        VcsDirectoryMappingManager(project).detectAllRootsAsync()
    }
}