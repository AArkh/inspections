package com.plugin.inspection.archy

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitorVoid

class KotlinArchyInterfaceInspection : BaseArchyInterfaceInspection() {

	override fun buildVisitor(
		holder: ProblemsHolder,
		isOnTheFly: Boolean,
		session: LocalInspectionToolSession
	): PsiElementVisitor = object : KtVisitorVoid() {
		private val rule = ArchyInterfaceRule(holder, getInterfaces())

		override fun visitClass(klass: KtClass) {
			super.visitClass(klass)
			rule.visitClass(klass)
		}
	}
}