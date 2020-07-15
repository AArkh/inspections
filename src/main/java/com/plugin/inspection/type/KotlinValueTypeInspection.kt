package com.plugin.inspection.type

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

private const val DISPLAY_NAME = "The value without type declaration"

/**
 * Инспекция для определения указания типа у переменной в тех местах, где это происходит неявно.
 *
 * В случае отстуствия указания типа подсвечивает соответствующее свойство, предлагая сделать
 * квик фикс [ValueTypeReferenceQuickFix].
 */
class KotlinValueTypeInspection : AbstractKotlinInspection() {

    private val valueTypeQuickFix = ValueTypeReferenceQuickFix()

    override fun getDisplayName(): String = DISPLAY_NAME

    override fun getGroupDisplayName(): String = GroupNames.STYLE_GROUP_NAME

    override fun isEnabledByDefault(): Boolean = true

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)

                // Указан ли тип.
                val typeReference: KtTypeReference? = property.typeReference
                if (typeReference != null) {
                    // Тип указан, больше нечего нам тут делать.
                    return
                }

                // Делегат (например, "by").
                val delegateExpression: KtExpression? = property.delegateExpression
                if (delegateExpression != null) {
                    registerProblem(holder, property)
                    return
                }

                val expressionOrInitializer: KtExpression? = property.delegateExpressionOrInitializer
                val lastChild: PsiElement? = expressionOrInitializer?.lastChild

                when (expressionOrInitializer) {
                    is KtConstantExpression -> {
                        if (lastChild?.text == "null") {
                            registerProblem(holder, property)
                            return
                        }
                    }
                    is KtCallExpression -> {
                        if (isMethodCall(expressionOrInitializer)) {
                            registerProblem(holder, property)
                            return
                        }
                    }
                    is KtReferenceExpression,
                    is KtLambdaExpression,
                    is KtBinaryExpression,
                    is KtDoWhileExpression,
                    is KtIfExpression,
                    is KtWhenExpression -> {
                        registerProblem(holder, property)
                        return
                    }
                    is KtDotQualifiedExpression -> {
                        if (isMethodCall(expressionOrInitializer)) {
                            registerProblem(holder, property)
                            return
                        }
                        if (lastChild is KtReferenceExpression && lastChild !is KtCallExpression) {
                            registerProblem(holder, property)
                            return
                        }
                    }
                }
            }
        }
    }

    private fun registerProblem(holder: ProblemsHolder, whatToFix: PsiElement) {
        holder.registerProblem(
                whatToFix,
                "There's no type reference for the property",
                valueTypeQuickFix
        )
    }

    private fun isMethodCall(expressionOrInitializer: PsiElement): Boolean {
        val lastChild: PsiElement? = expressionOrInitializer.lastChild
        val preLastChild: PsiElement? = lastChild?.prevLeaf(false)

        // Проверка на нижний регистр для понимания, метод это или создание объекта.
        return when (lastChild) {
            // Если lastChild - это KtValueArgumentList (скобочки с аргументами),
            // то проверяем на регистр предыдущего ребенка - [название метода/класса].
            is KtValueArgumentList -> {
                preLastChild?.text
                        ?.first()
                        ?.isLowerCase()
                        ?: false
            }
            // Если lastChild - это KtCallExpression, то проверяем на регистр его, т.к. KtCallExpression - это элемент,
            // представленный в формате: "[название метода/класса]()"
            is KtCallExpression -> {
                lastChild.text
                        .first()
                        .isLowerCase()
            }
            else -> false
        }
    }
}

/**
 * Фикс, проставляющий тип переменной.
 */
class ValueTypeReferenceQuickFix : LocalQuickFix {

    override fun getName(): String = "Add type reference"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val elementToFix: KtProperty = (descriptor.psiElement ?: return) as KtProperty

        try {
            val psiFactory = KtPsiFactory(elementToFix)
            val type: KtTypeReference = psiFactory.createType(elementToFix.type().toString())
            elementToFix.typeReference = type
        } catch (ignored: Exception) {
        }
    }
}