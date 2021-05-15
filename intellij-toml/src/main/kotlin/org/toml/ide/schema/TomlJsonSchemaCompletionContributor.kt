/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.toml.ide.schema

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.Consumer
import com.intellij.util.ThreeState
import com.jetbrains.jsonSchema.extension.JsonLikePsiWalker
import com.jetbrains.jsonSchema.extension.adapters.JsonPropertyAdapter
import com.jetbrains.jsonSchema.ide.JsonSchemaService
import com.jetbrains.jsonSchema.impl.JsonSchemaDocumentationProvider
import com.jetbrains.jsonSchema.impl.JsonSchemaObject
import com.jetbrains.jsonSchema.impl.JsonSchemaResolver

class TomlJsonSchemaCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val jsonSchemaService = JsonSchemaService.Impl.get(position.project)
        val jsonSchemaObject = jsonSchemaService.getSchemaObject(parameters.originalFile)
        if (jsonSchemaObject != null) {
            doCompletion(parameters, result, jsonSchemaObject, true)
        }
    }

    class Worker(
        val rootSchema: JsonSchemaObject,
        val position: PsiElement,
        val originalPosition: PsiElement,
        val resultConsumer: Consumer<LookupElement?>
    ) {
        val variants: MutableSet<LookupElement> = mutableSetOf()
        private val wrapInQuotes = position.parent is JsonStringLiteral
        private val insideStringLiteral = position.parent is JsonStringLiteral
        private val walker: JsonLikePsiWalker? = JsonLikePsiWalker.getWalker(position, rootSchema)
        private val project: Project = originalPosition.project

        fun work() {
            val checkable = walker?.findElementToCheck(position) ?: return
            val isName = walker.isName(checkable)
            val pointerPosition = walker.findPosition(checkable, isName == ThreeState.NO)
            if (pointerPosition == null || pointerPosition.isEmpty && isName == ThreeState.NO) return

            val schemas = JsonSchemaResolver(project, rootSchema, pointerPosition).resolve()
            val knownNames = hashSetOf<String>()

            schemas.forEach { schema ->
                if (isName != ThreeState.NO) {
                    val properties = walker.getPropertyNamesOfParentObject(originalPosition, position)
                    val adapter = walker.getParentPropertyAdapter(checkable)

                    val schemaProperties = schema.properties
                    addAllPropertyVariants(properties, adapter, schemaProperties, knownNames)
                }
            }

            for (variant in variants) {
                resultConsumer.consume(variant)
            }
        }

        fun addAllPropertyVariants(
            properties: Collection<String>,
            adapter: JsonPropertyAdapter?,
            schemaProperties: Map<String, JsonSchemaObject>,
            knownNames: MutableSet<String>
        ) {
            schemaProperties.keys
                .filter { name ->
                    !properties.contains(name) && !knownNames.contains(name) || name == adapter?.name
                }
                .forEach { name ->
                    knownNames.add(name)
                    addPropertyVariant(name, schemaProperties.get(name))
                }
        }

        fun addPropertyVariant(key: String, jsonSchemaObject: JsonSchemaObject?) {
            if (jsonSchemaObject == null) return

            val description = JsonSchemaDocumentationProvider.getBestDocumentation(true, jsonSchemaObject)

            val lookupElement = LookupElementBuilder.create(key)
                .withTypeText(description)

            variants.add(lookupElement)
        }
    }


    companion object {
        fun doCompletion(parameters: CompletionParameters, result: CompletionResultSet, rootSchema: JsonSchemaObject, stop: Boolean) {
//            JsonSchemaCompletionContributor.doCompletion(parameters, result, rootSchema, true)

            val completionPosition = parameters.originalPosition ?: parameters.position
            val worker = Worker(rootSchema, parameters.position, completionPosition, result)

            worker.work()

            if (stop && worker.variants.isNotEmpty()) {
                result.stopHere()
            }
        }
    }
}
