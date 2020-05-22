package com.plugin.inspection.archy

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtClass

/**
 * Отвечает за проверку классов (интерфейсы не проверяются) на наличие реализации
 * архитектурных интерфейсов.
 *
 * Находит классы, название которых заканчивается на ключ [interfaces] и не реализуют
 * интерфейс, который указан в значении [interfaces].
 * Если интерфейс не доступен (например не подключена библиотека), то проверка опускается.
 *
 * @param interfaces Интерфейсы <Окончание класса, Полные название необходимого интерфейса>.
 */
class ArchyInterfaceRule(
	private val holder: ProblemsHolder,
	private val interfaces: Map<String, String>
) {

	fun visitClass(clazz: KtClass) {
		visitClass(clazz.getPsiClass())
	}

	fun visitClass(clazz: PsiClass) {
		if (clazz.isInterface) {
			return
		}
		val className: PsiElement = clazz.nameIdentifier ?: return
		val qualifiedInterfaceName: String = interfaces.entries.find({
			className.text.endsWith(it.key)
		})?.value ?: return

		if (!isValidInterface(clazz.project, qualifiedInterfaceName)) {
			return
		}
		if (!clazz.hasInterface(qualifiedInterfaceName)) {
			holder.registerProblem(className, "Should implement '$qualifiedInterfaceName'")
		}
	}

	private fun isValidInterface(project: Project, qualifiedInterfaceName: String): Boolean {
		return findPsiClass(project, qualifiedInterfaceName)?.isInterface ?: false
	}

	private fun findPsiClass(project: Project, qualifiedInterfaceName: String): PsiClass? {
		val scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
		return JavaPsiFacade.getInstance(project).findClass(qualifiedInterfaceName, scope)
	}

	private fun PsiClass.hasInterface(qualifiedName: String): Boolean {
		val interfaces: Set<PsiClass> = getAllInterfaces()
		return interfaces.any { it.qualifiedName == qualifiedName }
	}

	private fun PsiClass.getAllInterfaces(): Set<PsiClass> {
		val result = HashSet<PsiClass>()
		getSuperTypeListEntries().forEach { superType: PsiClass ->
			if (superType.isInterface) {
				result.add(superType)
			}
			result.addAll(superType.getAllInterfaces())
		}
		return result
	}

	private fun PsiClass.getSuperTypeListEntries(): List<PsiClass> {
		val result: MutableList<PsiClass> = interfaces.toMutableList()
		superClass?.let { psiClass: PsiClass -> result.add(psiClass) }
		return result
	}

	private fun KtClass.getQualifiedName(): String? {
		val className: String = name ?: return null
		val packageName: String = containingKtFile.packageFqName.asString()
		return "$packageName.$className"
	}

	private fun KtClass.getPsiClass(): PsiClass {
		return findPsiClass(project, getQualifiedName()!!)!!
	}
}