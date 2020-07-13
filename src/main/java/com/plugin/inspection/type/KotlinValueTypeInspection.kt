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

private const val DISPLAY_NAME = "The value without type declaration"

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

                val expressionOrInitializer: KtExpression? = property.delegateExpressionOrInitializer
                // Делегат (например, "by").
                val delegateExpression: KtExpression? = property.delegateExpression

                val firstChild: PsiElement? = expressionOrInitializer?.firstChild
                val lastChild: PsiElement? = expressionOrInitializer?.lastChild

                if (delegateExpression != null) {
                    registerProblem(holder, property)
                    return
                }
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
                    is KtLambdaExpression -> {
                        registerProblem(holder, property)
                        return
                    }
                    is KtDotQualifiedExpression -> {
                        if (lastChild == firstChild && lastChild is KtLambdaArgument) {
                            registerProblem(holder, property)
                            return
                        }
                        if (isMethodCall(expressionOrInitializer)) {
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
        val children: Array<PsiElement> = expressionOrInitializer.children
        var lastChild: PsiElement? = expressionOrInitializer.lastChild
        var preLastChild: PsiElement? = if (children.size > 1) children[children.size - 2] else null

        // Разбираем lastChild до деталей.
        while (lastChild is KtCallExpression) {
            val lastChildChildren: Array<PsiElement> = lastChild.children
            preLastChild = lastChildChildren[lastChildChildren.size - 2]
            lastChild = lastChild.lastChild
        }

        return if (lastChild is KtValueArgumentList) {
            // Проверка на нижний регистр для понимания, метод это или конструктор.
            preLastChild?.text
                    ?.first()
                    ?.isLowerCase()
                    ?: false
        } else {
            false
        }
    }
}

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