package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtilBase
import com.intellij.uast.UastVisitorAdapter
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinTryCatchSurrounder
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.util.isMethodCall
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class KotlinThrowsMethodInspection : AbstractBaseUastLocalInspectionTool() {

    @Nls
    override fun getDisplayName(): String {
        return "Java calling Kotlin inspection"
    }

    @Nls
    override fun getGroupDisplayName(): String {
        return GroupNames.STYLE_GROUP_NAME
    }

    override fun isEnabledByDefault(): Boolean = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {


            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.isMethodCall()) {
                    val psiMethod: PsiMethod? = node.resolve()
                    val containsThrowsAnnotation = psiMethod?.annotations?.find { it.qualifiedName == "kotlin.jvm.Throws" } != null
                    if (containsThrowsAnnotation) {
                        var parent = node.sourcePsi
                        while (true) {
                            if (parent?.node?.elementType?.toString() == "TRY") {
                                return false
                            }
                            if (parent == null) {
                                break
                            }
                            parent = parent.parent
                        }
                        val pp = node.sourcePsi
                        val pslpapa = if (pp?.parent?.node?.elementType?.toString() == "DOT_QUALIFIED_EXPRESSION") {
                            pp.parent.parent
                        } else {
                            pp
                        }
                        holder.registerProblem(
                                pslpapa!!,
                                "Surround expression with try/catch",
                                CriQuickFix()
                        )
                    }
                }
                return super.visitCallExpression(node)
            }
        },
            true
        )
    }
}

/**
 * This class provides a solution to inspection problem expressions by manipulating
 * the PSI tree to use a.equals(b) instead of '==' or '!='
 */
private class CriQuickFix : LocalQuickFix {

    /**
     * Returns a partially localized string for the quick fix intention.
     * Used by the test code for this plugin.
     *
     * @return Quick fix short name.
     */
    override fun getName(): String {
        return "Surround with try / catch"
    }

    /**
     * This method manipulates the PSI tree to replace 'a==b' with 'a.equals(b)
     * or 'a!=b' with '!a.equals(b)'
     *
     * @param project    The project that contains the file being edited.
     * @param descriptor A problem found by this inspection.
     */
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        try {
            val element = descriptor.psiElement
            val editor = PsiUtilBase.findEditor(element)!!
            KotlinTryCatchSurrounder().surroundElements(project, editor, arrayOf(element))
        } catch (e: IncorrectOperationException) {
            e.printStackTrace()
        }

    }

    override fun getFamilyName(): String {
        return name
    }
}