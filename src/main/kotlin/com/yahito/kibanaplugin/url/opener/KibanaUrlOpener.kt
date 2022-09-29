package com.yahito.kibanaplugin.url.opener

import com.google.common.collect.ImmutableMap
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.util.PsiTreeUtil
import com.yahito.kibanaplugin.url.*
import com.yahito.kibanaplugin.url.builder.*
import org.jetbrains.kotlin.psi.KtFile
import java.util.regex.Pattern
import java.util.stream.Collectors

class KibanaUrlOpener {

    fun eval(parentExpression: PsiExpression): String {
        val computedValue = StringBuilder()
        if (parentExpression is PsiPolyadicExpression) {
            for (operand in parentExpression.getOperands()) {
                if (operand is PsiReference) {
                    val probableDefinition = (operand as PsiReference).resolve()
                    if (probableDefinition is PsiVariable) {
                        val initializer = probableDefinition.initializer
                        if (initializer != null) {
                            val o = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, true)
                            if (o is String) {
                                computedValue.append(o.toString())
                            }
                        }
                    }
                } else {
                    val value = JavaConstantExpressionEvaluator.computeConstantExpression(operand, true)
                    if (value is String) {
                        computedValue.append(value);
                    }
                }
            }
        } else if (parentExpression is PsiReference) {
            val probableDefinition = parentExpression.resolve();
            if (probableDefinition is PsiVariable) {
                val initializer = probableDefinition.initializer
                if (initializer != null) {
                    val o = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, true)
                    if (o is String) {
                        computedValue.append(o.toString())
                    }
                }
            }
        } else {
            return parentExpression.text
        }
        return computedValue.toString()

    }

    fun openKibana(editor: Editor, dataContext: DataContext?, url: String, indexValue: String, params: Map<String, String>) {
        val file = dataContext?.getData("psi.File")
        var className: String?
        val findReferenceAt: PsiReference?
        val caretOffset: Int = editor.caretModel.offset

        when (file) {
            is PsiJavaFile -> {
                className =
                    file.packageName + "." + file.name.substring(0, file.name.indexOf("."))

                if (className.startsWith(".")) {
                    className = className.substring(1, className.length)
                }
                findReferenceAt = file.findReferenceAt(caretOffset)
            }
            is KtFile -> {
                className = file.packageFqName.asString()  + "." + file.name.substring(0, file.name.indexOf("."))
                findReferenceAt = file.findReferenceAt(caretOffset)
            }
            else -> {
                throw java.lang.IllegalArgumentException("Unsupported file type")
            }
        }


        val currentElement = findReferenceAt?.element
        val call = PsiTreeUtil.getParentOfType(currentElement, PsiMethodCallExpression::class.java)

        val expression: PsiReferenceExpression? = PsiTreeUtil.getChildOfType(call, PsiReferenceExpression::class.java)

        PsiTreeUtil.getParentOfType(currentElement, PsiAnnotation::class.java)?.nameReferenceElement?.qualifiedName
        val annotation = PsiTreeUtil.getParentOfType(currentElement, PsiAnnotation::class.java)

        val (uri: String?, method: String?) = parseHttpMethod(annotation, currentElement)

        val methodCallExpression: PsiExpression? = PsiTreeUtil.getChildOfAnyType(expression, PsiMethodCallExpression::class.java, PsiReferenceExpression::class.java)

        if (methodCallExpression?.type?.canonicalText?.endsWith(".Logger") == true) {
            val paramList: PsiExpressionList? = PsiTreeUtil.getChildOfType(call, PsiExpressionList::class.java)
            val text: String? = getText(paramList)
            val psiIdentifier: PsiIdentifier? = PsiTreeUtil.getChildOfType(expression, PsiIdentifier::class.java)
            val level = psiIdentifier?.text
            if (level != null && (level == "info" || level == "debug" || level == "error" || level == "warn")) {
                openKibana(text, level.toUpperCase(), className, uri, method, url, indexValue, params)
            }
        } else {
            openKibana(getText(currentElement), null, className, uri, method, url, indexValue, params)
        }
    }


    private fun getText(paramList: PsiElement?): String? {
        var literal: PsiLiteralExpression? = null
        paramList?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression?) {
                if (literal == null && expression!!.type?.canonicalText == "java.lang.String") {
                    literal = expression
                }
            }
        })

        if (literal != null && literal!!.type?.canonicalText == "java.lang.String") {
            return literal!!.value.toString()
        }

        return null
    }

    private fun openKibana(text: String?, level: String?, loggerName: String?, uri: String?, method: String?, url: String, indexValue: String, params: Map<String, String>) {
        val mutableParams : MutableMap<String, String> = HashMap(params)

        val index = Index(indexValue)
        val filters = Filters()

        addFilterForPlaceholder(mutableParams, "%level", level, filters, index)
        addFilterForPlaceholder(mutableParams, "%logger", loggerName, filters, index)
        addFilterForPlaceholder(mutableParams, "%uri", uri, filters, index)
        addFilterForPlaceholder(mutableParams, "%httpMethod", method, filters, index)

        val dateCondition = DateCondition(mutableParams.remove("interval"))

        for (param in mutableParams) {
            filters.filters.add(Filter(index, param.key, param.value))
        }

        BrowserUtil.browse(KibanaUrlBuilder(url, dateCondition, Query(Sort(), filters, index, createTextQuery(text))).create())
    }

    private fun createTextQuery(text: String?): TextQuery? {
        val p: Pattern = Pattern.compile("\\{}|%s")

        val textQuery: TextQuery? = if (text != null) {
            val tokens = text.split(p)
            val newValue = tokens.stream().filter { x -> x.trim().length > 2 }.map { x -> "\"" + x + "\"" }
                .collect(Collectors.joining("AND"))
            TextQuery(newValue)
        } else {
            null
        }
        return textQuery
    }

    private fun addFilterForPlaceholder(
        params: MutableMap<String, String>,
        placeholder: String,
        resolvedValue: String?,
        filters: Filters,
        index: Index
    ) {
        params.filterValues { v -> v == placeholder }.forEach { (k, _) ->
            if (resolvedValue != null) {
                filters.filters.add(Filter(index, k, resolvedValue))
            }
            params.remove(k)
        }
    }

    private fun parseHttpMethod(annotation: PsiAnnotation?, currentElement: PsiElement?): Pair<String?, String?> {
        val supportedAnnotations: Map<String, String> = ImmutableMap.of(
                "org.springframework.web.bind.annotation.GetMapping", "GET",
                "org.springframework.web.bind.annotation.PostMapping", "POST",
                "org.springframework.web.bind. annotation.DeleteMapping", "DELETE",
                "ora.springframework.web.bind.annotation.PatchMapping", "PATCH")

        var uri: String? = null
        var method: String? = null

        if (annotation != null && supportedAnnotations.containsKey(annotation.nameReferenceElement?.qualifiedName)) {
            method = supportedAnnotations[annotation.nameReferenceElement?.qualifiedName]
            val parentClass = PsiTreeUtil.getParentOfType(currentElement, PsiClass::class.java)
            val modifiers = PsiTreeUtil.findChildOfType(parentClass, PsiModifierList::class.java)
            if (modifiers != null) {
                val classAnnotations = PsiTreeUtil.findChildrenOfAnyType(modifiers, PsiAnnotation::class.java)
                for (classAnnotation in classAnnotations) {
                    if (classAnnotation.nameReferenceElement?.qualifiedName.equals("org.springframework.web.bind.annotation.RequestMapping")) {
                        val annotationParams = PsiTreeUtil.findChildrenOfAnyType(classAnnotation, PsiNameValuePair::class.java)

                        for (annotationParam in annotationParams) {
                            val p: PsiIdentifier? = PsiTreeUtil.getChildOfType(annotationParam, PsiIdentifier::class.java)
                            if (p == null || p.text.equals("value")) {
                                val le = PsiTreeUtil.getChildOfType(annotationParam, PsiExpression::class.java)
                                uri = le?.let { eval(it) }
                                print(uri)
                            }
                        }
                    }
                }
            }

            val annotationParams = PsiTreeUtil.findChildrenOfAnyType(annotation, PsiNameValuePair::class.java)
            for (annotationParam in annotationParams) {
                val p: PsiIdentifier? = PsiTreeUtil.getChildOfType(annotationParam, PsiIdentifier::class.java)
                if (p == null || p.text.equals("value")) {
                    if (uri == null) {
                        uri = ""
                    }
                    uri += PsiTreeUtil.getChildOfType(annotationParam, PsiLiteralExpression::class.java)?.text
                }
            }
        }

        uri = uri?.replace("\"", "")?.replace(" \\\\", "\\")
        return Pair(uri, method)
    }
}
