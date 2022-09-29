package com.yahito.kibanaplugin.url.builder

class Sort : QueryPart() {
    override fun createInternal(): String {
        return """
            sort:!('@timestamp',desc)
        """.trimIndent().trimIndent().replace(" ", "")
    }
}