package com.yahito.kibanaplugin.url

class Index(private val value: String): QueryPart() {
    override fun createInternal(): String {
        return value
    }
}