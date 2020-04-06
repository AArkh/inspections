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
import org.jetbrains.kotlin.resolve.constants.KClassValue

class KotlinAddExceptionToThrowAnnotationQuickFix(
	private val exceptionName: String
) : LocalQuickFix {
	
	override fun getName(): String = "Add exception reference to @kotlin.jvm.Throws annotation"
	
	override fun getFamilyName(): String = name
	
	override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
		val expression: PsiElement = descriptor.psiElement ?: return
		try {
			val methodToBeFixed: KtNamedFunction = expression.getContainingMethod() ?: return
			val throwsAnnotation: KtAnnotationEntry? = methodToBeFixed.getThrowsAnnotation()
			val hasThrownAnnotation = throwsAnnotation != null
			val psiFactory = KtPsiFactory(methodToBeFixed)
			val annotations: MutableList<KtValueArgument> = throwsAnnotation
				?.getThrowAnnotationArgumentsList(psiFactory)
				?: mutableListOf()
			annotations.add(psiFactory.createArgument("$exceptionName::class"))
			val replacementAnnotation = psiFactory.createAnnotationEntry("@Throws")
			val valueArgumentList = psiFactory.buildValueArgumentList {
				appendFixedText("(")
				appendExpressions(annotations.map { argument: KtValueArgument ->
					return@map argument.getArgumentExpression()
				})
				appendFixedText(")")
			}
			replacementAnnotation.add(valueArgumentList)
			if (hasThrownAnnotation) {
				throwsAnnotation?.replace(replacementAnnotation)
			} else {
				methodToBeFixed.addAnnotationEntry(replacementAnnotation)
			}
		} catch (ignored: Exception) {
		}
	}
	
	private fun KtAnnotationEntry.getThrowAnnotationArgumentsList(
		psiFactory: KtPsiFactory
	): MutableList<KtValueArgument> {
		val bindingContext: BindingContext = this.analyze()
		val descriptor: AnnotationDescriptor? = bindingContext[BindingContext.ANNOTATION, this]
		val name = descriptor?.fqName ?: return mutableListOf()
		val argumentOutput: MutableList<KtValueArgument> = mutableListOf()
		val arguments = descriptor.allValueArguments.values
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
}