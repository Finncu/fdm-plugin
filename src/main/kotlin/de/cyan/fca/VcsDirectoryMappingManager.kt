package de.cyan.fca

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.roots.VcsRootDetector
import org.jetbrains.annotations.SystemIndependent
import org.jetbrains.concurrency.runAsync

class VcsDirectoryMappingManager() {

    private lateinit var allMappings : Map<String, GitVcsItem>
    private lateinit var project: Project

    companion object {
        const val GIT_VCS_NAME = "Git"
        private val ALL_ROOTS = HashMap<Project, TSaveEntry>()
    }

    constructor(project: Project) : this() {
        this.project = project
        allMappings = getActiveDirectoryMappings();
    }

    fun readAllDirectoryMappings(): Collection<GitVcsItem> {
        if (!ALL_ROOTS.containsKey(project))
            ALL_ROOTS[project] = TSaveEntry(allMappings.values)

        if (ALL_ROOTS[project]!!.idle)
            detectAllRootsAsync()

        return ALL_ROOTS[project]!!.value
    }

    private fun getActiveDirectoryMappings(): Map<@SystemIndependent String, GitVcsItem> {
        return ProjectLevelVcsManager.getInstance(project).directoryMappings.associate {
            it.directory to GitVcsItem(
                it.directory,
                true
            )
        }
    }

    fun detectAllRootsAsync() {
        runAsync {
//            NotificationsManager.getNotificationsManager().showNotification(Notification("detect", "Detecting ...", NotificationType.INFORMATION), project)
            ALL_ROOTS.getOrPut(project) { TSaveEntry(allMappings.values) }.value {
                    VcsRootDetector.getInstance(project).detect()
                        .map { root ->
                            allMappings.getOrDefault(
                                root.path.path,
                                GitVcsItem(root.path.path, false)
                            )
                        }}
//            NotificationsManager.getNotificationsManager().showNotification(Notification("detect", "Detected!", NotificationType.INFORMATION), project)
        }
    }
}