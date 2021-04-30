/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.toml.ide.schema

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.jetbrains.jsonSchema.ide.JsonSchemaService
import com.jetbrains.jsonSchema.impl.JsonSchemaCompletionContributor

class TomlJsonSchemaCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val jsonSchemaService = JsonSchemaService.Impl.get(position.project)
        val jsonSchemaObject = jsonSchemaService.getSchemaObject(parameters.originalFile)
        if (jsonSchemaObject != null) {
            JsonSchemaCompletionContributor.doCompletion(parameters, result, jsonSchemaObject, true)
        }
    }
}
