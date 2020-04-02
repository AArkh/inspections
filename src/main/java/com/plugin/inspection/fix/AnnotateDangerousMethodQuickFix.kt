package com.plugin.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.plugin.inspection.getContainingMethod
import org.jetbrains.kotlin.psi.KtPsiFactory

class AnnotateDangerousMethodQuickFix : LocalQuickFix {
	
	override fun getName(): String = "Add @kotlin.jvm.Throws annotation to method signature"
	
	override fun getFamilyName(): String = name
	
	override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
		val expression: PsiElement = descriptor.psiElement ?: return
		
		try {
			val methodToBeFixed = expression.getContainingMethod() ?: return
			val annotation = KtPsiFactory(methodToBeFixed).createAnnotationEntry("@Throws")
			methodToBeFixed.addAnnotationEntry(annotation)
		} catch (ignored: Exception) {
		}
	}
}