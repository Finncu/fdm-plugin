package de.cyan.fca.restore

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListListener

class ChangeListRestoreListener(private val project: Project) : ChangeListListener {
    private val clmService by lazy { project.service<ChangeListManagementService>() }

    override fun changesAdded(changes: Collection<Change>, toList: ChangeList?) {
        clmService.computeChanges(changes)
    }
}