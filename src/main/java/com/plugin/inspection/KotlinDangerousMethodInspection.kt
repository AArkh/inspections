package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.plugin.inspection.fix.KotlinAddExceptionToThrowAnnotationQuickFix
import com.plugin.inspection.fix.KotlinCallExpressionTryCatchQuickFix
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import java.util.*

private const val PROBLEM_NAME = "Kotlin \"throw\" operator without corresponding annotation or try/catch block"

/**
 * Инспекция, проверяющая каждое выражение throw и предлагающая добавить соответствующую аннотацию
 * в декларацию метода, в котором оное выражение встречается, либо обернуть в try/catch.
 *
 * Использует [KotlinCallExpressionTryCatchQuickFix] и [KotlinAddExceptionToThrowAnnotationQuickFix]
 */
class KotlinDangerousMethodInspection : AbstractKotlinInspection() {
	
	private val addTryCatchQuickFix = KotlinCallExpressionTryCatchQuickFix()
	
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
				val containingMethod: KtNamedFunction = expression.getContainingMethod() ?: return
				val throwsAnnotation: KtAnnotationEntry? = containingMethod.getThrowsAnnotation()
				if (throwsAnnotation == null) {
					registerProblem(holder, expression)
					return
				}
				if (shouldAddThrownExceptionToThrowsArguments(expression, throwsAnnotation)) {
					registerProblem(holder, expression)
				}
			}
		}
	}
	
	private fun shouldAddThrownExceptionToThrowsArguments(
		expression: KtThrowExpression,
		throwsAnnotation: KtAnnotationEntry
	) : Boolean {
		val annotatedExceptions: List<String> = throwsAnnotation.getExceptionReferences()
		if (annotatedExceptions.isEmpty()) {
			return false
		}
		val thrownException: PsiClass = expression.getExceptionType() ?: return false
		val exceptionTypes: MutableList<PsiClass> = LinkedList()
		thrownException.getSuperClassTree(exceptionTypes)
		val exceptionNames: List<String> = exceptionTypes.map { psiClass: PsiClass ->
			return@map psiClass.name!!
		}
		return exceptionNames.find { annotatedExceptions.contains(it) } == null
	}
	
	private fun registerProblem(holder: ProblemsHolder, expression: KtThrowExpression) {
		val exceptionName: String = expression.thrownExpression?.firstChild?.text ?: "Exception"
		holder.registerProblem(
			expression,
			PROBLEM_NAME,
			KotlinAddExceptionToThrowAnnotationQuickFix(exceptionName),
			addTryCatchQuickFix
		)
	}
	
	/**
	 * Достаем из набора аннотации Throw список исключений, добавленных к ней в аргументы.
	 */
	private fun KtAnnotationEntry.getExceptionReferences(): List<String> {
		val exceptionReferences = LinkedList<String>()
		valueArguments.forEach { valueArg: ValueArgument ->
			(valueArg as KtValueArgument).node.children().forEach node@{ astChild: ASTNode ->
				val exceptionKotlinClassLiteral: KtClassLiteralExpression = astChild.psi as? KtClassLiteralExpression
					?: return@node
				try {
					val exceptionType: KotlinType = exceptionKotlinClassLiteral.resolveType()
					exceptionType.arguments.forEach { typeProjection: TypeProjection ->
						exceptionReferences.add(typeProjection.toString())
					}
				} catch (ignored: Exception) {
					return@node
				}
			}
		}
		return exceptionReferences
	}
	
	/**
	 * Вытаскивает собственно ссылку на класс исключения из throw-выражения.
	 */
	private fun KtThrowExpression.getExceptionType(): PsiClass? {
		val currentThrownExceptionReference: KtReferenceExpression = thrownExpression
			?.getCalleeExpressionIfAny() as? KtReferenceExpression
			?: return null
		val resolvedExceptionClass: PsiElement = currentThrownExceptionReference.resolve() ?: return null
		return when (resolvedExceptionClass) {
			is KtTypeAlias -> resolvedExceptionClass.retrievePsiClass()
			is ClsMethodImpl -> resolvedExceptionClass.containingClass
			else -> null
		}
	}
	
	/**
	 * Вытаскиваем класс, на который ссылается Type alias.
	 */
	private fun KtTypeAlias.retrievePsiClass(): PsiClass? {
		val nameReference: KtReferenceExpression = this.getTypeReference()
			?.typeElement
			?.node
			?.lastChildNode
			?.psi as? KtReferenceExpression
			?: return null
		return nameReference.resolve() as? PsiClass ?: return null
	}
	
	private fun PsiClass.getSuperClassTree(listToBeFilled: MutableList<PsiClass>): MutableList<PsiClass> {
		listToBeFilled.add(this)
		val superClass: PsiClass = this.superClass ?: return listToBeFilled
		return superClass.getSuperClassTree(listToBeFilled)
	}
}