package com.yahito.kibanaplugin.url

class Sort : QueryPart() {
    override fun createInternal(): String {
        return """
            sort:!('@timestamp',desc)
        """.trimIndent().trimIndent().replace(" ", "")
    }
}