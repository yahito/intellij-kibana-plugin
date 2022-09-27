package com.yahito.kibanaplugin.ui

import com.intellij.ui.AddDeleteListPanel
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel

class EnvAddDeleteListPanel(title: String?) : AddDeleteListPanel<String>(title, emptyList<String>()) {

    fun setModel(model: DefaultListModel<String?>) {
        this.myListModel = model
        this.myList.model = model
        this.myList.repaint()
    }

    override fun findItemToAdd(): String? {
        val dialog = StringValueDialog(this, false)
        dialog.show()
        if (!dialog.isOK) {
            return null
        }
        val stringValue = dialog.stringValue
        return stringValue.ifEmpty { null }
    }

    fun selectedIndex(): Int {
        return myList.selectedIndex
    }

    fun getList(): JBList<String> {
        return myList
    }

    fun focusOnFirstItem() {
        myList.requestFocus()
        if (myList.model.size > 0) {
            myList.setSelectedValue(myList.model.getElementAt(0), false)
        }
    }
}