package com.yahito.kibanaplugin.url

import java.util.stream.Collectors

class Query(private val sort: Sort?, private val filters: Filters?, private val index : Index, private  val textQuery: TextQuery?): QueryPart() {
    override fun createInternal(): String {
        val l = ArrayList<String>()
        l.add("columns:!(_source)")
        if (filters != null) {
            l.add(filters.create())
        }
        l.add("index:'" + index.create() + "'")
        l.add("interval:auto")
        if (textQuery != null) {
            l.add(textQuery.create())
        }
        if (sort != null) {
            l.add(sort.create())
        }
        return "(" + l.stream().collect(Collectors.joining(",")) + ")"
    }
}