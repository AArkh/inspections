package com.plugin.inspection

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.annotations.Nls
import org.jetbrains.uast.UThrowExpression
import org.jetbrains.uast.getContainingMethod
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

            override fun visitThrowExpression(node: UThrowExpression): Boolean {
                super.visitThrowExpression(node)

                val papaMethod = node.getContainingMethod()
                val papaMethodAnnotations: Array<PsiAnnotation> = papaMethod?.annotations ?: return false

                if (papaMethodAnnotations.any { it.qualifiedName == "kotlin.jvm.Throws" }) {
                    return false
                }

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