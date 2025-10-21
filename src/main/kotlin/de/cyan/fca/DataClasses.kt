package de.cyan.fca

data class GitVcsItem(val path: String, var isEnabled: Boolean)
data class TSaveEntry(var idle: Boolean, var value: Collection<GitVcsItem>) {
    fun value(value: () -> Collection<GitVcsItem>) {
        idle = false
        this.value = value.invoke()
        idle = true
    }

    constructor(value: Collection<GitVcsItem>) : this(true, value)
}