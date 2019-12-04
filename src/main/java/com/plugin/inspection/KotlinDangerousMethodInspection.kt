package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class KotlinDangerousMethodInspection : AbstractBaseUastLocalInspectionTool() {

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

            var caughtThrowsForMethod: MutableMap<PsiMethod, List<PsiElement>> = HashMap()

            override fun visitTryExpression(node: UTryExpression): Boolean {
                super.visitTryExpression(node)

                val method = node.getContainingMethod() ?: return false

                val children = node.tryClause.sourcePsi?.children ?: return false

                val caughtThrows = children.mapNotNull { element: PsiElement ->
                    if (element.text.contains("exception", true)) {
                        return@mapNotNull element
                    }
                    return@mapNotNull null
                }

                caughtThrowsForMethod.remove(method)
                caughtThrowsForMethod[method] = caughtThrows

                return false
            }

            override fun visitThrowExpression(node: UThrowExpression): Boolean {
                super.visitThrowExpression(node)

                val papaMethod = node.getContainingMethod()
                val papaMethodAnnotations: Array<PsiAnnotation> = papaMethod?.annotations ?: return false

                val psi = node.thrownExpression
                val text = psi.sourcePsi?.text

                val ktExc = node.uastParent?.psi

                val ps = node.sourcePsi ?: return false
                val psText = ps.text
                psText.replace("throw ", "")

                val p = caughtThrowsForMethod[papaMethod]?.firstOrNull() ?: return false
                val pText = p.text


                val exceptionType: PsiType = node.thrownExpression.getExpressionType() ?: return false

                val s = ""

                if (papaMethodAnnotations.any { it.qualifiedName == "kotlin.jvm.Throws" }) {
                    return false
                }


                holder.registerProblem(
                    node.sourcePsi!!,
                    "Please, bud' lapkoy, add kotlin.jvm.Throws annotation to method signature!"
                )
                return false
            }
        },
            true
        )
    }
}