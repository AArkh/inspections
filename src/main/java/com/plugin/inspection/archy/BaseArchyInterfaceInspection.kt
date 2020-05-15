package com.plugin.inspection.archy

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.ui.ListTable
import com.intellij.codeInspection.ui.ListWrappingTableModel
import com.intellij.psi.CommonClassNames
import com.intellij.util.containers.OrderedSet
import com.siyeh.ig.ui.UiUtils
import org.jdom.Element
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import javax.swing.JComponent

private const val SETTING_INTERFACE_NAME = "architecturalInterfaces"

abstract class BaseArchyInterfaceInspection : AbstractKotlinInspection() {
	private val defInterfaces = listOf(
		"com.farpost.android.archy.controller.Controller",
		"com.farpost.android.archy.widget.Widget",
		"com.farpost.android.archy.interact.Interactor",
		"com.farpost.android.archy.router.Router"
	)
	private val architecturalInterfaces = OrderedSet<String>(defInterfaces)

	override fun getDisplayName(): String = "Should implement an architectural interface"

	override fun getGroupDisplayName(): String = GroupNames.INHERITANCE_GROUP_NAME

	override fun isEnabledByDefault(): Boolean = true

	override fun createOptionsPanel(): JComponent {
		val interfacesModel = NotEditableListWrappingTableModel(
			architecturalInterfaces,
			"Architectural interfaces"
		)
		return UiUtils.createAddRemoveTreeClassChooserPanel(
			ListTable(interfacesModel),
			"Architectural interfaces",
			CommonClassNames.JAVA_LANG_OBJECT
		)
	}

	override fun readSettings(node: Element) {
		super.readSettings(node)

		architecturalInterfaces.clear()
		val elements: List<Element>? = node.getChildren(SETTING_INTERFACE_NAME)
		val text: String? = elements?.firstOrNull()?.content?.firstOrNull()?.value
		if (text.isNullOrEmpty()) {
			architecturalInterfaces.addAll(defInterfaces)
		} else {
			architecturalInterfaces.addAll(text.split(","))
		}
	}

	override fun writeSettings(node: Element) {
		super.writeSettings(node)

		val element = Element(SETTING_INTERFACE_NAME)
		if (architecturalInterfaces.isEmpty()) {
			element.addContent(defInterfaces.joinToString(","))
		} else {
			element.addContent(architecturalInterfaces.joinToString(","))
		}
		node.addContent(element)
	}

	fun getInterfaces(): Map<String, String> {
		return architecturalInterfaces.associateBy { it.split(".").last() }
	}

	class NotEditableListWrappingTableModel(
		list: List<String>,
		columnName: String
	) : ListWrappingTableModel(list, columnName) {

		override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
			return false
		}
	}
}