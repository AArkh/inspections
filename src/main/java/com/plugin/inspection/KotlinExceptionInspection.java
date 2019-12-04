package com.plugin.inspection;

import com.intellij.codeInspection.*;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

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
			public void visitMethodCallExpression(PsiMethodCallExpression expression) {
				super.visitMethodCallExpression(expression);
				PsiExpressionList expressionList = expression.getArgumentList();
				
				PsiMethod psiMethod = expression.resolveMethod();
				PsiModifierList psiModifierList = psiMethod.getModifierList();
				PsiAnnotation[] annotations = psiModifierList.getAnnotations();
				
				for (PsiAnnotation annotation : annotations) {
					if (annotation.getQualifiedName().equals("kotlin.jvm.Throws")) {
						holder.registerProblem(
							expression.getOriginalElement(),
							"This shit is dangerous, man!",
							myQuickFix
						);
						return;
					}
				}
			}
			
			//@Override
			//public void visitBinaryExpression(PsiBinaryExpression expression) {
			//	super.visitBinaryExpression(expression);
			//	LOG.info("visitBinaryExpression, " + expression.getText());
			//	IElementType opSign = expression.getOperationTokenType();
			//	if (opSign == JavaTokenType.EQEQ || opSign == JavaTokenType.NE) {
			//		// The binary expression is the correct type for this inspection
			//		PsiExpression lOperand = expression.getLOperand();
			//		PsiExpression rOperand = expression.getROperand();
			//		if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) {
			//			return;
			//		}
			//		// Nothing is compared to null, now check the types being compared
			//		PsiType lType = lOperand.getType();
			//		PsiType rType = rOperand.getType();
			//		if (isCheckedType(lType) || isCheckedType(rType)) {
			//			// Identified an expression with potential problems, add to list with fix object.
			//			holder.registerProblem(
			//				expression,
			//				"Short message definin that smthng wrong here",
			//				myQuickFix
			//			);
			//		}
			//	}
			//}
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
		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			try {
				
				PsiElement codeBlockToBeFixed = descriptor.getPsiElement();
				
				String tryBlock = "try { ";
				String codeBlockToBeSurrounded = codeBlockToBeFixed.getText();
				String tryBlockEnd = " } ";
				String catchBlock = "catch(Exception e) { ";
				String catchBlockEnd = " }";
				
				PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
				
				//codeBlockToBeFixed.addBefore()
				
				PsiExpression surroundedCodeBlock = elementFactory.createExpressionFromText(
					tryBlock + codeBlockToBeSurrounded + tryBlockEnd + catchBlock + catchBlockEnd,
					null
				);
				
				surroundedCodeBlock.replace(codeBlockToBeFixed);
				
				
				//PsiBinaryExpression binaryExpression = (PsiBinaryExpression) descriptor.getPsiElement();
				//IElementType opSign = binaryExpression.getOperationTokenType();
				//PsiExpression lExpr = binaryExpression.getLOperand();
				//PsiExpression rExpr = binaryExpression.getROperand();
				//if (rExpr == null) {
				//	return;
				//}
				//
				//PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
				//PsiMethodCallExpression equalsCall =
				//	(PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);
				//
				//equalsCall.getMethodExpression().getQualifierExpression().replace(lExpr);
				//equalsCall.getArgumentList().getExpressions()[0].replace(rExpr);
				//
				//PsiExpression result = (PsiExpression) binaryExpression.replace(equalsCall);
				//
				//if (opSign == JavaTokenType.NE) {
				//	PsiPrefixExpression negation = (PsiPrefixExpression) factory.createExpressionFromText("!a", null);
				//	negation.getOperand().replace(result);
				//	result.replace(negation);
				//}
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