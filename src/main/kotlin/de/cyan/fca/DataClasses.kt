package de.cyan.fca

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.log

data class GitVcsItem(val path: String, var enabled: Boolean)
data class TSaveEntry(var idle: Boolean, var value: Collection<GitVcsItem>) {
    fun value(value: () -> Collection<GitVcsItem>) {
        if (idle) {
            idle = false
            this.value = value.invoke()
            idle = true
        }
    }

    constructor(value: Collection<GitVcsItem>) : this(true, value)
}