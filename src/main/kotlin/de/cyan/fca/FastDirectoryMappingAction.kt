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
import kotlin.collections.HashMap

class FastDirectoryMappingAction : AnAction() {

    companion object {
        private val POPUP_HOLDER = HashMap<Project, JBPopup>()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (POPUP_HOLDER.containsKey(project)) {
            POPUP_HOLDER.remove(project)!!.dispose()
            return
        }
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
                if (value.enabled) label.icon = AllIcons.RunConfigurations.TestPassed else label.icon =
                    AllIcons.Actions.Close
            })
        builder.setItemChosenCallback { item ->
            item.enabled = !item.enabled
            POPUP_HOLDER[project]?.content?.repaint()
        }
        POPUP_HOLDER[project] = builder.createPopup()
        POPUP_HOLDER[project]!!.showInBestPositionFor(e.dataContext)
    }

    private fun applyChanges(project: Project, items: List<GitVcsItem>): Boolean {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        vcsManager.directoryMappings = items.map { VcsDirectoryMapping(it.path, if (it.enabled) VcsDirectoryMappingManager.GIT else "") }
        POPUP_HOLDER.remove(project)
        return true
    }
}
