package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.annotations.Nls
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
                        holder.registerProblem(
                            node.sourcePsi!!,
                            "Surround expression with try/catch"
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