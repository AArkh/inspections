package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.plugin.inspection.fix.AnnotateDangerousMethodQuickFix
import com.plugin.inspection.fix.KotlinCallExpressionTryCatchQuickFix
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid

private const val PROBLEM_NAME = "Kotlin calling explosive functions inspection"

/**
 * Инспекция, проверяющая каждый вызов котлин-методов на предмет аннотации throw у последних
 * и предлагающая добавить соответствующую аннотацию в декларацию метода, в котором оное выражение встречается,
 * либо обернуть в try/catch.
 *
 * Использует [KotlinCallExpressionTryCatchQuickFix] и [AnnotateDangerousMethodQuickFix]
 */
class KotlinThrowsMethodInspection : AbstractKotlinInspection() {
	
	private val addTryCatchQuickFix = KotlinCallExpressionTryCatchQuickFix()
	private val addThrowsAnnotationFix = AnnotateDangerousMethodQuickFix()
	
	override fun getDisplayName(): String = PROBLEM_NAME
	
	override fun getGroupDisplayName(): String = GroupNames.BUGS_GROUP_NAME
	
	override fun isEnabledByDefault(): Boolean = true
	
	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
		return object : KtVisitorVoid() {
			override fun visitCallExpression(expression: KtCallExpression) {
				super.visitCallExpression(expression)
				val calledElement: PsiElement = expression.calleeExpression
					?.mainReference
					?.resolve()
					?: return
				if (!shouldAddThrowAnnotation(calledElement, expression)) {
					return
				}
				if (expression.canBeAnnotated()) {
					holder.registerProblem(
						expression,
						PROBLEM_NAME,
						addThrowsAnnotationFix,
						addTryCatchQuickFix
					)
				} else {
					holder.registerProblem(
						expression,
						PROBLEM_NAME,
						addTryCatchQuickFix
					)
				}
			}
		}
	}
	
	private fun shouldAddThrowAnnotation(calledElement: PsiElement, expression: KtCallExpression): Boolean {
		return calledElement is KtNamedFunction
			&& calledElement.hasThrowsAnnotation()
			&& !expression.isCaught()
	}
	
	private fun KtElement.canBeAnnotated() : Boolean {
		return this.getContainingMethod() != null
	}
}