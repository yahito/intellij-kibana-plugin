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

/**
 * Opens Kibana URLs based on the current context in the editor.
 * Handles Java and Kotlin files to extract relevant information for Kibana queries.
 */
class KibanaUrlOpener {

    companion object {
        private val HTTP_METHOD_ANNOTATIONS = mapOf(
            "org.springframework.web.bind.annotation.GetMapping" to "GET",
            "org.springframework.web.bind.annotation.PostMapping" to "POST",
            "org.springframework.web.bind.annotation.DeleteMapping" to "DELETE",
            "org.springframework.web.bind.annotation.PatchMapping" to "PATCH"
        )

        private const val REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping"
        private const val STRING_TYPE = "java.lang.String"
        private const val LOGGER_SUFFIX = ".Logger"
    }

    /**
     * Evaluates a PsiExpression to extract its string value.
     */
    private fun eval(parentExpression: PsiExpression): String {
        return when (parentExpression) {
            is PsiPolyadicExpression -> evaluatePolyadicExpression(parentExpression)
            is PsiReference -> evaluateReference(parentExpression)
            else -> parentExpression.text
        }
    }

    /**
     * Evaluates a polyadic expression (e.g., string concatenation).
     */
    private fun evaluatePolyadicExpression(expression: PsiPolyadicExpression): String {
        val computedValue = StringBuilder()

        for (operand in expression.operands) {
            when (operand) {
                is PsiReference -> {
                    val resolvedValue = evaluateReference(operand)
                    if (resolvedValue.isNotEmpty()) {
                        computedValue.append(resolvedValue)
                    }
                }
                else -> {
                    val value = JavaConstantExpressionEvaluator.computeConstantExpression(operand, true)
                    if (value is String) {
                        computedValue.append(value)
                    }
                }
            }
        }

        return computedValue.toString()
    }

    /**
     * Evaluates a reference expression to extract its string value.
     */
    private fun evaluateReference(reference: PsiReference): String {
        val probableDefinition = reference.resolve()

        if (probableDefinition is PsiVariable) {
            val initializer = probableDefinition.initializer ?: return ""

            val value = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, true)
            if (value is String) {
                return value
            }
        }

        return ""
    }

    /**
     * Main entry point to open Kibana with the context from the editor.
     */
    fun openKibana(editor: Editor, dataContext: DataContext?, url: String, indexValue: String, params: Map<String, String>) {
        val file = dataContext?.getData("psi.File") ?: throw IllegalArgumentException("File not found")
        val caretOffset = editor.caretModel.offset

        // Extract class information and reference at caret
        val (className, findReferenceAt) = when (file) {
            is PsiJavaFile -> extractJavaFileInfo(file, caretOffset)
            is KtFile -> extractKotlinFileInfo(file, caretOffset)
            else -> throw IllegalArgumentException("Unsupported file type: ${file.javaClass.name}")
        }

        val currentElement = findReferenceAt?.element ?: return
        val call = PsiTreeUtil.getParentOfType(currentElement, PsiMethodCallExpression::class.java)
        val expression = PsiTreeUtil.getChildOfType(call, PsiReferenceExpression::class.java)
        val annotation = PsiTreeUtil.getParentOfType(currentElement, PsiAnnotation::class.java)

        val (uri, method) = parseHttpMethod(annotation, currentElement)
        val methodCallExpression = PsiTreeUtil.getChildOfAnyType(
            expression,
            PsiMethodCallExpression::class.java,
            PsiReferenceExpression::class.java
        )

        if (isLoggerCall(methodCallExpression)) {
            handleLoggerCall(call, expression, className, uri, method, url, indexValue, params)
        } else {
            openKibana(getText(currentElement), null, className, uri, method, url, indexValue, params)
        }
    }

    /**
     * Extract information from a Java file.
     */
    private fun extractJavaFileInfo(file: PsiJavaFile, caretOffset: Int): Pair<String, PsiReference?> {
        var className = file.packageName + "." + file.name.substringBefore(".")
        if (className.startsWith(".")) {
            className = className.substring(1)
        }
        return Pair(className, file.findReferenceAt(caretOffset))
    }

    /**
     * Extract information from a Kotlin file.
     */
    private fun extractKotlinFileInfo(file: KtFile, caretOffset: Int): Pair<String, PsiReference?> {
        val className = file.packageFqName.asString() + "." + file.name.substringBefore(".")
        return Pair(className, file.findReferenceAt(caretOffset))
    }

    /**
     * Checks if the expression is a logger call.
     */
    private fun isLoggerCall(expression: PsiExpression?): Boolean {
        return expression?.type?.canonicalText?.endsWith(LOGGER_SUFFIX) == true
    }

    /**
     * Handle logger method calls like logger.info(), logger.debug(), etc.
     */
    private fun handleLoggerCall(
        call: PsiMethodCallExpression?,
        expression: PsiReferenceExpression?,
        className: String,
        uri: String?,
        method: String?,
        url: String,
        indexValue: String,
        params: Map<String, String>
    ) {
        val paramList = PsiTreeUtil.getChildOfType(call, PsiExpressionList::class.java)
        val text = getText(paramList)

        val psiIdentifier = PsiTreeUtil.getChildOfType(expression, PsiIdentifier::class.java)
        val level = psiIdentifier?.text

        if (level != null && level in listOf("info", "debug", "error", "warn")) {
            openKibana(text, level.uppercase(), className, uri, method, url, indexValue, params)
        }
    }

    /**
     * Extract text from the element.
     */
    private fun getText(element: PsiElement?): String? {
        var literal: PsiLiteralExpression? = null

        element?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                if (literal == null && expression.type?.canonicalText == STRING_TYPE) {
                    literal = expression
                }
            }
        })

        return literal?.value?.toString()
    }

    /**
     * Open Kibana with the provided parameters.
     */
    private fun openKibana(
        text: String?,
        level: String?,
        loggerName: String?,
        uri: String?,
        method: String?,
        url: String,
        indexValue: String,
        params: Map<String, String>
    ) {
        val mutableParams = HashMap(params)
        val index = Index(indexValue)
        val filters = Filters()

        // Add filters for placeholders
        addFilterForPlaceholder(mutableParams, "%level", level, filters, index)
        addFilterForPlaceholder(mutableParams, "%logger", loggerName, filters, index)
        addFilterForPlaceholder(mutableParams, "%uri", uri, filters, index)
        addFilterForPlaceholder(mutableParams, "%httpMethod", method, filters, index)

        // Handle date condition
        val dateCondition = DateCondition(mutableParams.remove("interval"))

        // Add remaining parameters as filters
        mutableParams.forEach { (key, value) ->
            filters.filters.add(Filter(index, key, value))
        }

        // Build and open the URL
        val textQuery = createTextQuery(text)
        val kibanaUrl = KibanaUrlBuilder(url, dateCondition, Query(Sort(), filters, index, textQuery)).create()
        BrowserUtil.browse(kibanaUrl)
    }

    /**
     * Create a text query from the given text.
     */
    private fun createTextQuery(text: String?): TextQuery? {
        if (text == null) return null

        val pattern = Pattern.compile("\\{}|%s")
        val tokens = text.split(pattern)

        val queryText = tokens.asSequence()
            .filter { it.trim().length > 2 }
            .map { "\"$it\"" }
            .joinToString("AND")

        return if (queryText.isNotEmpty()) TextQuery(queryText) else null
    }

    /**
     * Add a filter for a placeholder if it exists in the params.
     */
    private fun addFilterForPlaceholder(
        params: MutableMap<String, String>,
        placeholder: String,
        resolvedValue: String?,
        filters: Filters,
        index: Index
    ) {
        val keysToRemove = mutableListOf<String>()

        params.forEach { (key, value) ->
            if (value == placeholder && resolvedValue != null) {
                filters.filters.add(Filter(index, key, resolvedValue))
                keysToRemove.add(key)
            }
        }

        keysToRemove.forEach { params.remove(it) }
    }

    /**
     * Parse HTTP method from Spring annotations.
     */
    private fun parseHttpMethod(annotation: PsiAnnotation?, currentElement: PsiElement?): Pair<String?, String?> {
        if (annotation == null || annotation.nameReferenceElement?.qualifiedName !in HTTP_METHOD_ANNOTATIONS) {
            return Pair(null, null)
        }

        val method = HTTP_METHOD_ANNOTATIONS[annotation.nameReferenceElement?.qualifiedName]
        var uri = extractRequestMappingFromClass(currentElement)

        // Extract URI from the method annotation
        val methodUri = extractValueFromAnnotation(annotation)
        uri = if (uri.isNullOrEmpty()) methodUri else "$uri$methodUri"

        return Pair(uri?.replace("\"", "")?.replace(" \\\\", "\\"), method)
    }

    /**
     * Extract the RequestMapping value from the class.
     */
    private fun extractRequestMappingFromClass(element: PsiElement?): String? {
        val parentClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        val modifiers = PsiTreeUtil.findChildOfType(parentClass, PsiModifierList::class.java) ?: return null

        val classAnnotations = PsiTreeUtil.findChildrenOfAnyType(modifiers, PsiAnnotation::class.java)

        for (classAnnotation in classAnnotations) {
            if (classAnnotation.nameReferenceElement?.qualifiedName == REQUEST_MAPPING_ANNOTATION) {
                return extractValueFromAnnotation(classAnnotation)
            }
        }

        return null
    }

    /**
     * Extract the "value" parameter from an annotation.
     */
    private fun extractValueFromAnnotation(annotation: PsiAnnotation): String? {
        val annotationParams = PsiTreeUtil.findChildrenOfAnyType(annotation, PsiNameValuePair::class.java)

        for (param in annotationParams) {
            val identifier = PsiTreeUtil.getChildOfType(param, PsiIdentifier::class.java)

            if (identifier == null || identifier.text == "value") {
                val expression = PsiTreeUtil.getChildOfType(param, PsiExpression::class.java)
                if (expression != null) {
                    return eval(expression)
                }
            }
        }

        return null
    }
}