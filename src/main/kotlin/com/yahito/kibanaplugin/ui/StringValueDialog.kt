package com.yahito.kibanaplugin.ui

import com.intellij.openapi.ui.DialogWrapper
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class StringValueDialog(parent: Component, canBeParent: Boolean) : DialogWrapper(parent, canBeParent) {
    private var myTextField: JTextField? = null
    private var myMainPanel: JPanel? = null

    override fun createCenterPanel(): JComponent? {
        return myMainPanel
    }

    val stringValue: String
        get() = myTextField!!.text

    override fun getPreferredFocusedComponent(): JComponent? {
        return myTextField
    }

    init {
        title = "Add New Environment"
        init()
    }
}
