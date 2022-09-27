package com.yahito.kibanaplugin.url

class TextQuery(private val text: String) : QueryPart() {
    override fun createInternal(): String {
        return """
            query:(query_string:(analyze_wildcard:!t,query:'${text.replace("'", "!'")}'))
        """.trimIndent()
    }
}