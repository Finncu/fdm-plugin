package de.cyan.fca

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.roots.VcsRootDetector
import org.jetbrains.annotations.SystemIndependent
import org.jetbrains.concurrency.runAsync

val ALL_ROOTS = HashMap<Project, TSaveEntry>()
const val GIT_VCS_NAME = "Git"

fun readAllDirectoryMappings(project: Project): Collection<GitVcsItem> {
    val allMappings = getActiveDirectoryMappings(project)

    if (!ALL_ROOTS.containsKey(project))
        ALL_ROOTS[project] = TSaveEntry(allMappings.values)

    if (ALL_ROOTS[project]!!.idle)
        detectAllRootsAsync(project, allMappings)

    return ALL_ROOTS[project]!!.value
}

fun getActiveDirectoryMappings(project: Project): Map<@SystemIndependent String, GitVcsItem> {
    return ProjectLevelVcsManager.getInstance(project).directoryMappings.associate {
        it.directory to GitVcsItem(
            it.directory,
            true
        )
    }
}

fun detectAllRootsAsync(project: Project, allMappings: Map<String, GitVcsItem>) {
    runAsync {
        ALL_ROOTS[project] =
            TSaveEntry(
                VcsRootDetector.getInstance(project).detect()
                    .map { root -> allMappings.getOrDefault(root.path.path, GitVcsItem(root.path.path, false)) })
    }
}

class VcsDirectoryMappingManager : ProjectActivity {
    override suspend fun execute(project: Project) {
        detectAllRootsAsync(project, getActiveDirectoryMappings(project))
    }
}

data class GitVcsItem(val path: String, var isEnabled: Boolean)
data class TSaveEntry(var idle: Boolean, var value: Collection<GitVcsItem>) {
    fun value(value: () -> Collection<GitVcsItem>) {
        idle = false
        this.value = value.invoke()
        idle = true
    }

    constructor(value: Collection<GitVcsItem>) : this(true, value)
}