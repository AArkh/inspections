package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.plugin.inspection.fix.AnnotateDangerousMethodQuickFix
import com.plugin.inspection.fix.KotlinCallExpressionTryCatchQuickFix
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid

private const val PROBLEM_NAME = "Kotlin calling explosive functions inspection"

/**
 * https://github.com/JetBrains/kotlin/tree/master/idea/src/org/jetbrains/kotlin/idea/inspections
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
				val reference: KtReference = expression.calleeExpression?.mainReference ?: return
				val calledElement: PsiElement = reference.resolve() ?: return
				if (calledElement !is KtNamedFunction
					|| !calledElement.hasThrowsAnnotation()
					|| expression.isCaught()
				) {
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
	
	private fun KtElement.canBeAnnotated() : Boolean {
		return this.getContainingMethod() != null
	}
}