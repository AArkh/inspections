package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.plugin.inspection.fix.AnnotateDangerousMethodQuickFix
import com.plugin.inspection.fix.KotlinThrowExpressionTryCatchQuickFix
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import java.util.*

private const val PROBLEM_NAME = "Kotlin \"throw\" operator without corresponding annotation or try/catch block"

class KotlinDangerousMethodInspection : AbstractKotlinInspection() {
	
	private val addTryCatchQuickFix = KotlinThrowExpressionTryCatchQuickFix()
	private val addThrowsAnnotationFix = AnnotateDangerousMethodQuickFix()
	
	override fun getDisplayName(): String = PROBLEM_NAME
	
	override fun getGroupDisplayName(): String = GroupNames.BUGS_GROUP_NAME
	
	override fun isEnabledByDefault(): Boolean = true
	
	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
		return object : KtVisitorVoid() {
			override fun visitThrowExpression(expression: KtThrowExpression) {
				super.visitThrowExpression(expression)
				if (expression.isCaught()) {
					return
				}
				val method = expression.getContainingMethod() ?: return
				val throwsAnnotation: KtAnnotationEntry? = method.getThrowsAnnotation()
				if (throwsAnnotation == null) {
					holder.registerProblem(
						expression,
						PROBLEM_NAME,
						addThrowsAnnotationFix,
						addTryCatchQuickFix
					)
					return
				}
				
				val exceptionReferences: List<KtNameReferenceExpression> = throwsAnnotation.getExceptionReferences()
				if (exceptionReferences.isEmpty()) {
					return
				}
				val currentThrownExceptionReference: KtNameReferenceExpression? = expression.getCurrentThrownException()
				val exceptionAnnotated = exceptionReferences.any { reference: KtNameReferenceExpression ->
					currentThrownExceptionReference?.textMatches(reference) ?: false
				}
				if (exceptionAnnotated) {
					return
				}
				holder.registerProblem(
					expression,
					PROBLEM_NAME,
					addThrowsAnnotationFix,
					addTryCatchQuickFix
				)
			}
		}
	}
	
	private fun KtAnnotationEntry.getExceptionReferences() : List<KtNameReferenceExpression> {
		val list = LinkedList<KtNameReferenceExpression>()
		(this.valueArguments.firstOrNull() as KtValueArgument?)
			?.node
			?.children()
			?.forEach { child: ASTNode ->
				val ktNameReferenceExpression: PsiElement? = child.firstChildNode?.psi
				if (ktNameReferenceExpression != null && ktNameReferenceExpression is KtNameReferenceExpression) {
					// тут можно получить ссылку на собственно java класс, но я так и не понял, как
					// достать super-классы из этой ссылки.
					//val reference: KtTypeReference? = (ktNameReferenceExpression.resolve() as KtTypeAlias).getTypeReference()
					list.add(ktNameReferenceExpression)
				}
			}
		return list
	}
	
	private fun KtThrowExpression.getCurrentThrownException() : KtNameReferenceExpression? {
		val currentThrownExceptionReference: KtExpression? = thrownExpression
			?.getCalleeExpressionIfAny()
		if (currentThrownExceptionReference == null
			|| currentThrownExceptionReference !is KtNameReferenceExpression
		) {
			return null
		}
		return currentThrownExceptionReference
	}
}