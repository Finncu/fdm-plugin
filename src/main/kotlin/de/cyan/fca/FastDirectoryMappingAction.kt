package de.cyan.fca

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.ui.SimpleListCellRenderer
import java.util.*
import javax.swing.ListSelectionModel

class FastDirectoryMappingAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val items = VcsDirectoryMappingManager(project).readAllDirectoryMappings().toList()
        val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(items)

        builder.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            .setCloseOnEnter(false)
            .setCancelOnClickOutside(true)
            .setFilterAlwaysVisible(true)
            .setNamerForFiltering { action -> action.path }
            .setCancelCallback { applyChanges(project, items) }
            .setRenderer(SimpleListCellRenderer.create { label, value: GitVcsItem, _ ->
                label.text = value.path;
                if (value.isEnabled) label.icon = AllIcons.RunConfigurations.TestPassed else label.icon = AllIcons.General.Close
            })
        var popupRef: JBPopup? = null
        builder.setItemChosenCallback { item ->
            item.isEnabled = !item.isEnabled
            popupRef?.content?.repaint()
        }
        val popup = builder.createPopup()
        popupRef = popup
        popup.showInBestPositionFor(e.dataContext)
    }

    private fun applyChanges(project: Project, items: List<GitVcsItem>): Boolean {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)

        val newMappings = ArrayList<VcsDirectoryMapping>()
        // Process existing mappings and toggled items
        for (item in items) {
            if (item.isEnabled)
                newMappings.add(VcsDirectoryMapping(item.path, VcsDirectoryMappingManager.GIT_VCS_NAME))
        }

        vcsManager.directoryMappings = newMappings
        return true
    }
}
