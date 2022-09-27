package com.yahito.kibanaplugin.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.stream.Collectors

@State(name = "KibanaPluginProjectSettings", storages = [Storage("kibana-plugin.json")])
class KibanaPluginProjectSettings : PersistentStateComponent<KibanaPluginSettings> {

    private var settings: KibanaPluginSettings = KibanaPluginSettings()

    companion object {
        fun getInstance(): KibanaPluginProjectSettings {
            return ServiceManager.getService(KibanaPluginProjectSettings::class.java)
        }
    }

    override fun loadState(settings: KibanaPluginSettings) {
        this.settings = settings
    }

    override fun getState(): KibanaPluginSettings {
        return settings
    }

}