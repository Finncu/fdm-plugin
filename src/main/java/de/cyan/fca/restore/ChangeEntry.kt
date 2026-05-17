package de.cyan.fca.restore

import com.intellij.util.lang.Hash
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

@Tag("change-entry")
data class ChangeEntry(@Attribute val id: String,@Attribute var target: String)
