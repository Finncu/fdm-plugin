package de.cyan.fca.restore

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.jetbrains.rd.util.put

class ChangeListManagementService(private val project: Project) {
    private val logger = Logger.getInstance(ChangeListManagementService::class.java)
    private val manager by lazy { project.service<ChangeListManager>() }
    private val storageService by lazy { project.service<ChangeStorageService>() }

    fun computeChanges(changes: Collection<Change?>?) {
        val lg:String = storageService.getStorage().toString()
        logger.info("storage: $lg")
        TODO("Not yet implemented")
    }

    fun computeChangesForRemoval(oldMapping: VcsDirectoryMapping) {
        val lg:String = storageService.getStorage().toString()
        val changes = manager.getChangesIn(LocalFilePath(oldMapping.directory, true))
        changes.forEach {
            manager.getChangeList(it)?.let { it1 -> storageService.getStorage()[it.toString().hashCode()] = it1.name }
        }
    }
}