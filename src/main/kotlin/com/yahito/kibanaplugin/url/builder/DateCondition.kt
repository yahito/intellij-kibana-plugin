package com.yahito.kibanaplugin.url.builder

class DateCondition(private val interval: String?) : QueryPart() {

    override fun createInternal(): String {
        return  """
            (
                refreshInterval:(display:Off,pause:!f,value:0),
                time:(from:now-%s,mode:quick,to:now)
            )
        """.format(interval ?: "24h").trimIndent().replace(" ", "")
    }
}