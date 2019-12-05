package com.plugin.inspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInsight.daemon.impl.quickfix.SurroundWithTryCatchFix;
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class JavaCallingKotlinExceptionInspection extends AbstractBaseUastLocalInspectionTool {
	
	public static final String QUICK_FIX_NAME = "Surround expression with try/catch";
	
	private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.JavaCallingKotlinExceptionInspection");
	
	private final LocalQuickFix myQuickFix = new CriQuickFix();
	
	@Nls
	@NotNull
	@Override
	public String getDisplayName() {
		return "Java calling Kotlin inspection";
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
				PsiElement element = descriptor.getPsiElement();
				Editor editor = PsiUtilBase.findEditor(element);
				new SurroundWithTryCatchFix(element).invoke(project, editor, null);
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