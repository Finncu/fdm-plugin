package de.cyan.fca

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.roots.VcsRootDetector
import com.intellij.ui.SimpleListCellRenderer
import org.jetbrains.concurrency.runAsync
import java.util.*
import javax.swing.ListSelectionModel
import kotlin.collections.HashMap

class ToggleGitVcsAction : AnAction( ) {

    private companion object {
        const val GIT_VCS_NAME = "Git"
        val ALL_ROOTS = HashMap<Project, TSaveEntry>()
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val vcsManager = ProjectLevelVcsManager.getInstance(project)

        val allMappings = vcsManager.directoryMappings.associate { it.directory to GitVcsItem(it.directory, true) }

        if (!ALL_ROOTS.containsKey(project))
            ALL_ROOTS[project] = TSaveEntry(VcsRootDetector.getInstance(project).detect())

        if (ALL_ROOTS[project]!!.idle)
            runAsync { ALL_ROOTS[project]!!.value { VcsRootDetector.getInstance(project).detect() } }

        val items = ALL_ROOTS[project]!!.value.map { root ->
            allMappings.getOrDefault(root.path.path, GitVcsItem(root.path.path, false))
        }

        val popup = JBPopupFactory.getInstance().createPopupChooserBuilder(items)
            .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            .setCloseOnEnter(false)
            .setItemChosenCallback { item -> item.isEnabled = !item.isEnabled }
            .setCancelOnClickOutside(true)
            .setFilterAlwaysVisible(true)
            .setNamerForFiltering { action -> action.path }
            .setCancelCallback { applyChanges(project, items) }
            .setRenderer(SimpleListCellRenderer.create { label, value:GitVcsItem, _ -> label.text = value.path; if (value.isEnabled) label.icon = AllIcons.RunConfigurations.TestPassed else label.icon = AllIcons.General.Add })
            .createPopup()
        popup.showInBestPositionFor(e.dataContext)
    }

    private fun applyChanges(project: Project, items: List<GitVcsItem>): Boolean? {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)

        val newMappings = ArrayList<VcsDirectoryMapping>()
        // Process existing mappings and toggled items
        for (item in items) {
            if (item.isEnabled)
                newMappings.add(VcsDirectoryMapping(item.path, GIT_VCS_NAME))
        }

        vcsManager.setDirectoryMappings(newMappings)
        vcsManager.updateActiveVcss()
        return true
    }

    data class GitVcsItem(val path: String, var isEnabled: Boolean)
    data class TSaveEntry(var idle: Boolean, var value: Collection<VcsRoot>) {
        fun value(value: () -> Collection<VcsRoot>) {
            idle = false
            this.value = value.invoke()
            idle = true
        }
        constructor(value: Collection<VcsRoot>) : this(true, value)
    }
}
