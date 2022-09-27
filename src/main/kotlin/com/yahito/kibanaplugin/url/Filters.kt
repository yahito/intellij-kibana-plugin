package com.yahito.kibanaplugin.url

import java.util.stream.Collectors

class Filters : QueryPart() {
    val filters: ArrayList<Filter> = ArrayList()

    override fun createInternal(): String {
        return StringBuilder().append("filters:!(")
                .append(filters.stream().map { f -> f.create() }.collect(Collectors.joining(",")))
                .append(")").toString()
    }
}