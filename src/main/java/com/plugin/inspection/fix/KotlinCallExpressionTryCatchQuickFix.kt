package com.plugin.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinTryCatchSurrounder
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtThrowExpression

class KotlinCallExpressionTryCatchQuickFix : LocalQuickFix {
	
	override fun getName(): String = "Surround call expression with try/catch"
	
	override fun getFamilyName(): String = name
	
	override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
		try {
			val elementToBeFixed: PsiElement = descriptor.psiElement ?: return
			val elementsToBeSurrounded = when(elementToBeFixed) {
				is KtCallExpression -> getElementsToBeSurrounded(elementToBeFixed)
				is KtThrowExpression -> arrayOf(elementToBeFixed)
				else -> throw IllegalStateException("Can't use this stuff =(")
			}
			val editor: Editor = PsiUtilBase.findEditor(elementToBeFixed) ?: return
			KotlinTryCatchSurrounder().surroundElements(project, editor, elementsToBeSurrounded)
		} catch (ignored: Exception) {
		}
	}
	
	private fun getElementsToBeSurrounded(callExpression: KtCallExpression): Array<PsiElement> {
		if (callExpression.parent !is KtDotQualifiedExpression) {
			return arrayOf(callExpression)
		}
		return arrayOf((callExpression.parent as KtDotQualifiedExpression).retrieveParentKtDotExpression())
	}
	
	/**
	 * Каждый следующий вызов функции в рамках одного выражения порождает дополнительную вложенность по типу:
	 * [[[[[Builder().a()].b()].c()].d()].build()]. Этот метод достает верхний [KtDotQualifiedExpression] дерева.
	 */
	private fun KtDotQualifiedExpression.retrieveParentKtDotExpression(): KtDotQualifiedExpression {
		var dotQualifiedExpression = this
		while (dotQualifiedExpression.parent is KtDotQualifiedExpression) {
			dotQualifiedExpression = dotQualifiedExpression.parent as KtDotQualifiedExpression
		}
		return dotQualifiedExpression
	}
}