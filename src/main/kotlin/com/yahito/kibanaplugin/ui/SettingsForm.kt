package com.yahito.kibanaplugin.ui

import com.google.common.base.Strings
import com.intellij.ui.table.JBTable
import com.yahito.kibanaplugin.config.KibanaPluginProjectSettings
import com.yahito.kibanaplugin.config.KibanaPluginSettings
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.table.DefaultTableModel
import javax.swing.text.PlainDocument
import kotlin.reflect.KMutableProperty0

class SettingsForm {
    private var mainPanel: JPanel? = null
    private var envListPanel: EnvAddDeleteListPanel? = null
    private var urlTextField: JTextField? = null
    private var paramsTable: JBTable? = null
    private var indexTextField: JTextField? = null

    var state = KibanaPluginSettings()
        private set
    var isModified = false

    private fun createUIComponents() {

        val envListPanel = EnvAddDeleteListPanel("environments")
        envListPanel.getList().addListSelectionListener {
            val i = envListPanel.selectedIndex()
            if (i >= 0 && i < state.envs.size) {
                initForm(i)
            } else {
                initForm(-1)
                envListPanel.focusOnFirstItem()
            }
        }
        envListPanel.focusOnFirstItem()
        this.envListPanel = envListPanel
    }

    private fun initForm(envIdx: Int) {
        val env: KibanaPluginSettings.Env

        if (envIdx < 0 || envIdx >= state.envs.size) {
            env = KibanaPluginSettings.Env()
        } else {
            env = state.envs[envIdx]
        }
        paramsTable?.let {
            val dataModel = DefaultTableModel(arrayOf("name", "value"), 0)
            for ((key, value) in env.params) {
                dataModel.addRow(arrayOf<Any>(key, value))
            }
            dataModel.addRow(arrayOf<Any>("", ""))

            dataModel.addTableModelListener {
                env.params.clear()
                val rowCount = dataModel.rowCount
                for (i in 0 until rowCount) {
                    val name = dataModel.getValueAt(i, 0)
                    if (env.params.containsKey(name)) {
                        throw IllegalArgumentException()
                    }
                    val value = dataModel.getValueAt(i, 1)
                    val notEmpty = !Strings.isNullOrEmpty(name.toString()) || !Strings.isNullOrEmpty(value.toString())
                    if (notEmpty) {
                        env.params[name.toString()] = value.toString()
                    }
                }
                isModified = true
                initForm(envIdx)
            }
            it.model = dataModel
            it.repaint()
        }
        initTextField(urlTextField, env::url)
        initTextField(indexTextField, env::index)
    }

    private fun initTextField(urlTextField: JTextField?, property: KMutableProperty0<String>) {
        urlTextField?.let {
            val doc = PlainDocument()
            it.document = doc
            it.text = property.get()
            doc.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {
                    onChange()
                }

                override fun removeUpdate(e: DocumentEvent) {
                    onChange()
                }

                override fun changedUpdate(e: DocumentEvent) {
                    onChange()
                }

                private fun onChange() {
                    property.set(it.text)
                    isModified = true
                }
            })
        }
    }

    fun getPanel(settings: KibanaPluginProjectSettings): JComponent {
        loadState(settings.state)
        return mainPanel!!
    }

    fun loadState(_state: KibanaPluginSettings) {
        this.state = KibanaPluginSettings()
        for (env in _state.envs) {
            this.state.envs.add(KibanaPluginSettings.Env(env))
        }
        isModified = false
        envListPanel!!.setModel(initEnvListModel())
        SwingUtilities.invokeLater { envListPanel!!.focusOnFirstItem() }
    }

    private fun initEnvListModel(): DefaultListModel<String?> {
        val model = DefaultListModel<String?>()
        for (env in this.state.envs) {
            model.addElement(env.name)
        }

        model.addListDataListener(object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) {
                state.envs.add(KibanaPluginSettings.Env(model.getElementAt(e.index0)!!))
                isModified = true
                initForm(e.index0)
            }

            override fun intervalRemoved(e: ListDataEvent) {
                state.envs.removeAt(e.index0)
                val selectedIndex = envListPanel!!.selectedIndex()
                if (selectedIndex >= 0 && selectedIndex < state.envs.size) {
                    initForm(selectedIndex)
                } else {
                    initForm(-1)
                    envListPanel!!.focusOnFirstItem()
                }
                isModified = true
            }

            override fun contentsChanged(e: ListDataEvent?) {}
        })
        return model
    }
}