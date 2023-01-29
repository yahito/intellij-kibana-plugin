package com.yahito.kibanaplugin.ui

import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class StringValueDialog(parent: Component, canBeParent: Boolean) : DialogWrapper(parent, canBeParent) {
    private var textField: JTextField? = null
    private var panel: JPanel? = null

    override fun createCenterPanel(): JComponent? {
        return panel
    }

    val stringValue: String
        get() = textField!!.text

    override fun getPreferredFocusedComponent(): JComponent? {
        return textField
    }

    init {
        title = "Add New Environment"
        init()
    }
}
