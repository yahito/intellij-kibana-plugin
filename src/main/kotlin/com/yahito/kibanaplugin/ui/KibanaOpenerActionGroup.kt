package com.yahito.kibanaplugin.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.ServiceManager
import com.yahito.kibanaplugin.config.KibanaPluginProjectSettings
import com.yahito.kibanaplugin.config.KibanaPluginSettings
import com.yahito.kibanaplugin.url.opener.KibanaUrlOpener

class KibanaOpenerActionGroup : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val envs = ServiceManager.getService(KibanaPluginProjectSettings::class.java).state.envs
        return Array(envs.size) { i -> EnvAction(envs[i]) }
    }

}

internal class EnvAction(private val env: KibanaPluginSettings.Env) : AnAction(env.name) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = try {
            DataManager.getInstance().dataContextFromFocusAsync.blockingGet(1000)!!.getData(PlatformDataKeys.EDITOR)!!
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        val editorWriteActionHandler = KibanaUrlOpener()
        editorWriteActionHandler.openKibana(editor, e.dataContext, env.url, env.index, env.params)
    }

}