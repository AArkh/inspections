package com.plugin.inspection;

import com.intellij.codeInspection.*;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;

import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.StringTokenizer;

import static com.siyeh.ig.psiutils.ExpressionUtils.isNullLiteral;

public class KotlinExceptionInspection extends AbstractBaseUastLocalInspectionTool {
	
	public static final String QUICK_FIX_NAME = "SDK: " + InspectionsBundle.message("inspection.comparing.references.use.quickfix");
	
	private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.KotlinExceptionInspection");
	
	private final CriQuickFix myQuickFix = new CriQuickFix();
	
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
							"Short message definin that smthng wrong here",
							myQuickFix
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
	
	
	/**
	 * This class provides a solution to inspection problem expressions by manipulating
	 * the PSI tree to use a.equals(b) instead of '==' or '!='
	 */
	private static class CriQuickFix implements LocalQuickFix {
		
		/**
		 * Returns a partially localized string for the quick fix intention.
		 * Used by the test code for this plugin.
		 *
		 * @return Quick fix short name.
		 */
		@NotNull
		@Override
		public String getName() {
			return QUICK_FIX_NAME;
		}
		
		/**
		 * This method manipulates the PSI tree to replace 'a==b' with 'a.equals(b)
		 * or 'a!=b' with '!a.equals(b)'
		 *
		 * @param project    The project that contains the file being edited.
		 * @param descriptor A problem found by this inspection.
		 */
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			try {
				PsiBinaryExpression binaryExpression = (PsiBinaryExpression) descriptor.getPsiElement();
				IElementType opSign = binaryExpression.getOperationTokenType();
				PsiExpression lExpr = binaryExpression.getLOperand();
				PsiExpression rExpr = binaryExpression.getROperand();
				if (rExpr == null) {
					return;
				}
				
				PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
				PsiMethodCallExpression equalsCall =
					(PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);
				
				equalsCall.getMethodExpression().getQualifierExpression().replace(lExpr);
				equalsCall.getArgumentList().getExpressions()[0].replace(rExpr);
				
				PsiExpression result = (PsiExpression) binaryExpression.replace(equalsCall);
				
				if (opSign == JavaTokenType.NE) {
					PsiPrefixExpression negation = (PsiPrefixExpression) factory.createExpressionFromText("!a", null);
					negation.getOperand().replace(result);
					result.replace(negation);
				}
			} catch (IncorrectOperationException e) {
				LOG.error(e);
			}
		}
		
		@NotNull
		public String getFamilyName() {
			return getName();
		}
	}
}