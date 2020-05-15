package com.plugin.inspection.archy

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor

class JavaArchyInterfaceInspection : BaseArchyInterfaceInspection() {

	override fun buildVisitor(
		holder: ProblemsHolder,
		isOnTheFly: Boolean,
		session: LocalInspectionToolSession
	): PsiElementVisitor = object : JavaElementVisitor() {
		private val rule = ArchyInterfaceRule(holder, getInterfaces())

		override fun visitClass(klass: PsiClass) {
			super.visitClass(klass)
			rule.visitClass(klass)
		}
	}
}