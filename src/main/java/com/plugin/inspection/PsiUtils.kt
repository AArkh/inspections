package com.plugin.inspection

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun PsiElement?.getContainingMethod(): KtNamedFunction? {
	var element: PsiElement? = this
	do {
		if (element is KtNamedFunction) {
			return element
		}
		element = element?.parent
	} while (element != null)
	return null
}

fun KtElement.isCaught(): Boolean {
	var element: PsiElement? = this.parent
	do {
		when (element) {
			is KtCatchClause -> return false // ибо KtCatchClause дочерний у KtTryExpression
			is KtTryExpression -> return true
			is KtNamedFunction, is KtClassInitializer -> return false // Дальше не интересно, дропаем проход.
		}
		element = element?.parent
	} while (element != null)
	return false
}

fun KtNamedFunction.hasThrowsAnnotation(): Boolean {
	return this.getThrowsAnnotation() != null
}

fun KtNamedFunction.getThrowsAnnotation(): KtAnnotationEntry? {
	return annotationEntries
		.filterIsInstance<KtAnnotationEntry>()
		.find { it.getAnnotationClassDescriptor().classId?.asString() == "kotlin/jvm/Throws" }
}

/**
 * Из [KtAnnotationEntry] невозможно вытащить аннотацию, поэтому пришлось искать решение в сети.
 * Какая-то магия из https://github.com/JetBrains/kotlin/blob/master/idea/src/org/jetbrains/kotlin/idea/inspections/RemoveEmptyParenthesesFromAnnotationEntryInspection.kt
 */
fun KtAnnotationEntry.getAnnotationClassDescriptor(): ClassDescriptor? {
	val context: BindingContext = analyze(BodyResolveMode.PARTIAL)
	return context[BindingContext.ANNOTATION, this]?.annotationClass
}