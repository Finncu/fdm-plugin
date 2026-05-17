package de.cyan.fca.restore

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListListener
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.annotations.NotNull

@Service(Service.Level.PROJECT)
@State(name = "FDMRestoreStorage", storages = [Storage("fdmRestore.json")])
class RestoreVcsChangeControllerService(private val project: Project) : ChangeListListener,
    PersistentStateComponent<RestoreVcsChangeControllerService> {
    private val logger = Logger.getInstance(this::class.java)
    private val manager by lazy { project.service<ChangeListManager>() }

    @XCollection(elementName = "changes")
    private val storage: HashMap<String/*change*/, String/*ChangeList*/> = HashMap()

    override fun getState(): RestoreVcsChangeControllerService {
        return this
    }

    override fun loadState(p0: RestoreVcsChangeControllerService) {
        XmlSerializerUtil.copyBean<RestoreVcsChangeControllerService?>(state, this)
    }

    override fun changesAdded(changes: Collection<Change?>, toList: ChangeList) {
        val movements: HashMap<String, List<Change>> = HashMap()
        changes.stream()
            .forEach { change ->
                storage.remove(change.toString())?.let { movements.getOrPut(it) { listOf(change!!) } }
            }
        for (entry in movements.entries)
            manager.moveChangesTo(getOrCreateChangeList(entry.key), entry.value)
    }

    private fun getOrCreateChangeList(changeList: String): LocalChangeList {
        return manager.getChangeList(changeList) ?: manager.addChangeList(changeList, changeList)
    }

    fun storeChanges(changes: Collection<Change>) {
        changes.forEach(this::storeChange)
    }

    fun storeChange(change: Change?) {
//        manager.getChangeList(change)?.name?.let { changes[change.toString()] = it }
        storage[change.toString()] = manager.getChangeList(change!!)?.name!!
    }

    fun store(change: Change, changeList: LocalChangeList) {
        storage[change.toString()] = changeList.name
    }

    fun storeChanges(mapping: VcsDirectoryMapping) {
        val changes = manager.getChangesIn(LocalFilePath(mapping.directory, true))
        storeChanges(changes)
    }
}