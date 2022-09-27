package com.yahito.kibanaplugin.config

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.yahito.kibanaplugin.ui.SettingsForm
import javax.swing.JComponent

class Config(val project: Project) : SearchableConfigurable {

    private var settingsForm: SettingsForm? = null

    override fun getId(): String {
        return "com.github.yahito.kibanaplugin.config.Config"
    }

    override  fun getDisplayName(): String {
        return "Kibana Opener"
    }

    override fun createComponent(): JComponent {
        if (settingsForm == null) {
            settingsForm = SettingsForm()
        }
        return settingsForm!!.getPanel(KibanaPluginProjectSettings.getInstance())
    }

    override fun isModified(): Boolean {
        return settingsForm!!.isModified
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        if (settingsForm != null && isModified) {
            KibanaPluginProjectSettings.getInstance().loadState(settingsForm!!.state)
            settingsForm!!.isModified = false
        }
    }

    override fun reset() {
        settingsForm!!.loadState(getSettings().state)
    }

    private fun getSettings(): KibanaPluginProjectSettings {
        return KibanaPluginProjectSettings.getInstance()
    }

    override fun disposeUIResources() {
        settingsForm = null
    }

    override fun enableSearch(option: String?): Runnable? {
        return null
    }
}