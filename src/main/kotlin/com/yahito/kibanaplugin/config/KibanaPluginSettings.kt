package com.yahito.kibanaplugin.config

class KibanaPluginSettings {
    var envs: ArrayList<Env> = ArrayList()

    class Env {
        var url: String = ""
        var name: String = ""
        var index: String = ""
        var params: LinkedHashMap<String, String> = LinkedHashMap()

        constructor(env: Env) {
            this.url = env.url
            this.name = env.name
            this.index = env.index
            this.params.putAll(env.params)
        }

        constructor(name: String) : this() {
            this.name = name
            url = "http://localhost:5601"
            params["logger"] = "%logger"
            params["level"] = "%level"
            params["requestUri"] = "%uri"
            params["requestMethod"] = "%httpMethod"
            params["interval"] = "24h"
        }

        constructor()
    }
}