package de.cyan.fca.restore

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State
import com.intellij.openapi.util.text.stringHashCode
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection
import io.ktor.util.Hash
import kotlin.collections.HashMap

@Service
@State(name = "ChangeStorageState", storages = [Storage("change-entries.xml")])
class ChangeStorageService : PersistentStateComponent<ChangeStorageService> {

    @XCollection(elementName = "change-entries")
    private var storage = HashMap<Int/*change*/, String/*ChangeList*/>()

    override fun getState(): ChangeStorageService? {
        return this
    }

    override fun loadState(state: ChangeStorageService) {
        XmlSerializerUtil.copyBean<ChangeStorageService?>(state, this)
    }

    fun getStorage() : HashMap<Int, String> {
        return storage
    }
}