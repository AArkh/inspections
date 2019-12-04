package com.plugin.inspection;

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import java.util.StringTokenizer;

import static com.siyeh.ig.psiutils.ExpressionUtils.isNullLiteral;

public class KotlinExceptionInspection extends AbstractBaseUastLocalInspectionTool {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Kotlin calls inspection";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return GroupNames.STYLE_GROUP_NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return super.isEnabledByDefault();
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {



        return new JavaElementVisitor() {

            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
            }
            @Override
            public void visitBinaryExpression(PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                IElementType opSign = expression.getOperationTokenType();
                if (opSign == JavaTokenType.EQEQ || opSign == JavaTokenType.NE) {
                    // The binary expression is the correct type for this inspection
                    PsiExpression lOperand = expression.getLOperand();
                    PsiExpression rOperand = expression.getROperand();
                    if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
                        return;
                    }
                    // Nothing is compared to null, now check the types being compared
                    PsiType lType = lOperand.getType();
                    PsiType rType = rOperand.getType();
                    if (isCheckedType(lType) || isCheckedType(rType)) {
                        // Identified an expression with potential problems, add to list with fix object.
                        holder.registerProblem(
                                expression,
                                "short message definin that smthng wrong here"
                                //myQuickFix
                        );
                    }
                }
            }
            /**
             * Verifies the input is the correct {@code PsiType} for this inspection.
             *
             * @param type  The {@code PsiType} to be examined for a match
             * @return      {@code true} if input is {@code PsiClassType} and matches
             *                 one of the classes in the CHECKED_CLASSES list.
             */
            private boolean isCheckedType(PsiType type) {
                if (!(type instanceof PsiClassType)) {
                    return false;
                }
                StringTokenizer tokenizer = new StringTokenizer("java.lang.String;java.util.Date", ";");
                while (tokenizer.hasMoreTokens()) {
                    String className = tokenizer.nextToken();
                    if (type.equalsToText(className)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}