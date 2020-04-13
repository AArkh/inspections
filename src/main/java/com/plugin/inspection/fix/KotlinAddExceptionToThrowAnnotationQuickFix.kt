package com.plugin.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.plugin.inspection.getContainingMethod
import com.plugin.inspection.getThrowsAnnotation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue

/**
 * Фикс, добавляющий ссылку на выбрасываемое исключение в аргументы аннотации [Throws] и, собственно, саму
 * аннотацию, ежели таковой не имеется.
 */
class KotlinAddExceptionToThrowAnnotationQuickFix(
	private val exceptionName: String
) : LocalQuickFix {
	
	override fun getName(): String = "Add exception reference to @kotlin.jvm.Throws annotation"
	
	override fun getFamilyName(): String = name
	
	override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
		val expression: PsiElement = descriptor.psiElement ?: return
		val methodToBeFixed: KtNamedFunction = expression.getContainingMethod() ?: return
		val throwsAnnotation: KtAnnotationEntry? = methodToBeFixed.getThrowsAnnotation()
		val psiFactory = KtPsiFactory(methodToBeFixed)
		val replacementAnnotation: KtAnnotationEntry = formReplacementAnnotation(throwsAnnotation, psiFactory)
		val hasThrownAnnotation: Boolean = throwsAnnotation != null
		if (hasThrownAnnotation) {
			throwsAnnotation?.replace(replacementAnnotation)
		} else {
			methodToBeFixed.addAnnotationEntry(replacementAnnotation)
		}
	}
	
	private fun formReplacementAnnotation(
		currentThrowsAnnotation: KtAnnotationEntry?,
		psiFactory: KtPsiFactory
	): KtAnnotationEntry {
		val annotations: MutableList<KtValueArgument> = currentThrowsAnnotation
			?.getThrowAnnotationArgumentsList(psiFactory)
			?: mutableListOf()
		annotations.add(psiFactory.createArgument("$exceptionName::class"))
		val replacementAnnotation: KtAnnotationEntry = psiFactory.createAnnotationEntry("@Throws")
		val annotationArgumentList: KtValueArgumentList = formNewAnnotationArguments(psiFactory, annotations)
		replacementAnnotation.add(annotationArgumentList)
		return replacementAnnotation
	}
	
	private fun KtAnnotationEntry.getThrowAnnotationArgumentsList(
		psiFactory: KtPsiFactory
	): MutableList<KtValueArgument> {
		val bindingContext: BindingContext = this.analyze()
		val descriptor: AnnotationDescriptor? = bindingContext[BindingContext.ANNOTATION, this]
		val name: FqName = descriptor?.fqName ?: return mutableListOf()
		val argumentOutput: MutableList<KtValueArgument> = mutableListOf()
		val arguments: Collection<ConstantValue<*>> = descriptor.allValueArguments.values
		if (name != FqName("kotlin.jvm.Throws")) {
			return argumentOutput
		}
		for (arg in arguments) {
			// По какой-то причине тут еще один список, а раньше была пара о_о
			val list: ArrayList<*> = (arg.value as ArrayList<*>)
			list.forEach { classValue: Any ->
				if (classValue !is KClassValue || classValue.value !is KClassValue.Value.NormalClass) {
					return@forEach
				}
				val classId: ClassId = (classValue.value as KClassValue.Value.NormalClass).classId
				argumentOutput.add(psiFactory.createArgument("${classId.relativeClassName}::class"))
			}
		}
		return argumentOutput
	}
	
	private fun formNewAnnotationArguments(
		psiFactory: KtPsiFactory,
		annotations: MutableList<KtValueArgument>
	): KtValueArgumentList {
		return psiFactory.buildValueArgumentList {
			appendFixedText("(")
			appendExpressions(annotations.map { argument: KtValueArgument ->
				return@map argument.getArgumentExpression()
			})
			appendFixedText(")")
		}
	}
}