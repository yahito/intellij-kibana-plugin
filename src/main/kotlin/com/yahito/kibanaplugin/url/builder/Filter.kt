package com.yahito.kibanaplugin.url.builder

class Filter(private val index: Index, private val key: String, private val value: CharSequence): QueryPart() {
    override fun createInternal(): String {
        return """
        ('${'$'}state':(store:appState),meta:(alias:!n,disabled:!f,index:'${index.create()}',
        key:$key,negate:!f,type:phrase,value:$value),query:(match:($key:(query:$value,type:phrase))))
        """.trimIndent()

    }
}
