package com.plugin.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinTryCatchSurrounder
import org.jetbrains.kotlin.psi.KtThrowExpression

class KotlinThrowExpressionTryCatchQuickFix : LocalQuickFix {
	
	override fun getName(): String = "Surround throw expression with try/catch"
	
	override fun getFamilyName(): String = name
	
	override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
		try {
			val elementToBeSurrounded: PsiElement = descriptor.psiElement ?: return
			if (elementToBeSurrounded !is KtThrowExpression) {
				return
			}
			val editor: Editor = PsiUtilBase.findEditor(elementToBeSurrounded) ?: return
			KotlinTryCatchSurrounder().surroundElements(project, editor, arrayOf(elementToBeSurrounded))
		} catch (ignored: Exception) {
		}
	}
}