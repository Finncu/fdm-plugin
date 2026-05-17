package de.cyan.fca.restore

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.jetbrains.rd.util.getOrCreate
import com.jetbrains.rd.util.put

class ChangeListManagementService(private val project: Project) {
    private val logger = Logger.getInstance(ChangeListManagementService::class.java)
    private val manager by lazy { project.service<ChangeListManager>() }
    private val storageService by lazy { project.service<ChangeStorageService>() }

    fun computeChanges(changes: Collection<Change>) {// flashcast - check if not in an changelist or not in an
        val lg: String = storageService.getStorage().toString()
        logger.info("storage: $lg")
        val movements: HashMap<String, ArrayList<Change>> = HashMap()
        changes.forEach { change ->
            storageService.getStorage().remove(change.toString().hashCode())?.let {
                movements.getOrCreate(it) { ArrayList() }.add(change)
            }
        }
        if (movements.isNotEmpty())
            movements.entries.forEach {
                manager.moveChangesTo(manager.getOrCreateChangeList(it.key), it.value)
            }
    }

    fun computeChangesForRemoval(oldMapping: VcsDirectoryMapping) {
        storageService.getStorage().toString()
        val changes = manager.getChangesIn(LocalFilePath(oldMapping.directory, true))
        changes.forEach {
            manager.getChangeList(it)?.let { it1 -> storageService.getStorage()[it.toString().hashCode()] = it1.name }
        }
    }
}

private fun ChangeListManager.getOrCreateChangeList(
    name: String,
) : LocalChangeList {
    return this.getChangeList(name)?:this.addChangeList(name, name)
}

