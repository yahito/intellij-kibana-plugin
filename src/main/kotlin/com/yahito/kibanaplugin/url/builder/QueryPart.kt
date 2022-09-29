package com.yahito.kibanaplugin.url.builder

abstract class QueryPart {
    fun create(): String {
        return createInternal().replace("\n", "").replace("\t", "")
    }

    protected abstract fun createInternal(): String
}