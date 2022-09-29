package com.yahito.kibanaplugin.url.builder

import org.apache.http.client.utils.URIBuilder
import java.net.URLDecoder

class KibanaUrlBuilder(private val url: String, private val dateCondition: DateCondition?, private var query: Query): QueryPart() {
    override fun createInternal(): String {
        val builder = URIBuilder(url)

        builder.path = "/app/kibana#/discover"

        if (dateCondition != null) {
            builder.addParameter("_g", dateCondition.create())
        }

        builder.addParameter("_a", query.create())

        val url = URLDecoder.decode(builder.build().toString().replace("%23/discover", "#/discover"), "UTF-8")
        return url
    }
}