/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatePsiInfo
import com.intellij.openapi.editor.Document
import com.intellij.openapiext.isUnitTestMode
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import org.rust.lang.core.psi.RsBlock
import org.rust.lang.core.psi.RsExpr
import org.rust.lang.core.psi.RsPsiFactory
import org.rust.lang.core.psi.ext.ancestors
import org.rust.lang.core.psi.ext.endOffset
import org.rust.lang.core.types.ty.TyBool
import org.rust.lang.core.types.type
import org.rust.lang.utils.negate

internal object RsPostfixTemplatePsiInfo : PostfixTemplatePsiInfo() {
    override fun getNegatedExpression(element: PsiElement): PsiElement =
        element.negate()

    override fun createExpression(context: PsiElement, prefix: String, suffix: String): PsiElement =
        RsPsiFactory(context.project).createExpression("$prefix${context.text}$suffix")
}

abstract class RsExprParentsSelectorBase(val pred: (RsExpr) -> Boolean) : PostfixTemplateExpressionSelector {
    override fun getRenderer(): Function<PsiElement, String> = Function { it.text }

    final override fun getExpressions(context: PsiElement, document: Document, offset: Int): List<PsiElement> {
        val expressions = getExpressionsInternal(context, document, offset)
        // `PostfixTemplateWithExpressionSelector#expand` selects only one item from this list in unit tests.
        // But in different platform versions different items are selected (of course, it's very convenient).
        // So let's return the latest item to commit tests behavior with all platform versions
        return if (isUnitTestMode) listOfNotNull(expressions.lastOrNull()) else expressions
    }

    protected abstract fun getExpressionsInternal(context: PsiElement, document: Document, offset: Int): List<PsiElement>
}

class RsTopMostInScopeSelector(pred: ((RsExpr) -> Boolean) = { true }) : RsExprParentsSelectorBase(pred) {
    override fun getExpressionsInternal(context: PsiElement, document: Document, offset: Int): List<PsiElement> =
        context
            .ancestors
            .takeWhile { it !is RsBlock && it.endOffset == context.endOffset }
            .filter { it is RsExpr && pred(it) }
            .toList()

    override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean =
        context
            .ancestors
            .takeWhile { it !is RsBlock }
            .any { it is RsExpr && pred(it) }
}

class RsAllParentsSelector(pred: ((RsExpr) -> Boolean) = { true }) : RsExprParentsSelectorBase(pred) {
    override fun getExpressionsInternal(context: PsiElement, document: Document, offset: Int): List<PsiElement> =
        context
            .ancestors
            .takeWhile { it !is RsBlock }
            .filter { it is RsExpr && pred(it) }
            .toList()

    override fun hasExpression(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean =
        context
            .ancestors
            .takeWhile { it !is RsBlock }
            .any { it is RsExpr && pred(it) }
}

fun RsExpr.isBool() = type == TyBool
