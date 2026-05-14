package com.localvault.app.autofill

import android.app.assist.AssistStructure
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.localvault.app.LocalVaultApplication
import com.localvault.app.R
import com.localvault.app.data.PasswordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale

class LocalVaultAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: android.os.CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }
        val fields = AutofillParser.parse(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val entries = runBlocking {
            withContext(Dispatchers.IO) {
                val db = (application as LocalVaultApplication).vaultSession.database.value
                db?.entryDao()?.getAll().orEmpty()
            }
        }
        if (entries.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val matches = entries
            .sortedByDescending { scoreEntry(it, fields) }
            .filter { scoreEntry(it, fields) > 0 }
            .take(MAX_DATASETS)
            .ifEmpty { entries.take(MAX_DATASETS) }

        val response = FillResponse.Builder()
        matches.forEach { entry ->
            val presentation = presentation("${entry.title.ifBlank { getString(R.string.app_name) }} - ${entry.username}")
            val dataset = Dataset.Builder(presentation).apply {
                fields.usernameId?.let { id ->
                    setValue(id, AutofillValue.forText(entry.username), presentation)
                }
                fields.passwordId?.let { id ->
                    setValue(id, AutofillValue.forText(entry.password), presentation)
                }
            }.build()
            response.addDataset(dataset)
        }
        callback.onSuccess(response.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun presentation(text: String): RemoteViews =
        RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, text)
        }

    private fun scoreEntry(entry: PasswordEntry, fields: AutofillFields): Int {
        val haystack = listOfNotNull(entry.title, entry.username, entry.url)
            .joinToString(" ")
            .lowercase(Locale.US)
        return fields.matchTerms.sumOf { term ->
            val score: Int = when {
                term.isBlank() -> 0
                haystack.contains(term) -> 3
                term.contains(entry.title.lowercase(Locale.US)) && entry.title.isNotBlank() -> 2
                else -> 0
            }
            score
        }
    }

    private object AutofillParser {
        fun parse(structure: AssistStructure): AutofillFields {
            val result = MutableAutofillFields()
            for (i in 0 until structure.windowNodeCount) {
                visit(structure.getWindowNodeAt(i).rootViewNode, result)
            }
            return result.toFields()
        }

        private fun visit(node: AssistStructure.ViewNode, result: MutableAutofillFields) {
            val id = node.autofillId
            val hints = node.autofillHints.orEmpty().map { it.lowercase(Locale.US) }
            val hintText = node.hint?.lowercase(Locale.US).orEmpty()
            val idText = node.idEntry?.lowercase(Locale.US).orEmpty()
            val classText = node.className?.toString()?.lowercase(Locale.US).orEmpty()
            val webDomain = node.webDomain?.lowercase(Locale.US)
            val text = node.text?.toString()?.lowercase(Locale.US).orEmpty()
            val searchable = listOf(hintText, idText, classText, text, hints.joinToString(" "))
                .joinToString(" ")

            if (webDomain != null) result.matchTerms += webDomain
            node.idPackage?.lowercase(Locale.US)?.let { result.matchTerms += it }

            if (id != null) {
                when {
                    hints.any { it.contains("password") } ||
                        searchable.contains("password") ||
                        searchable.contains("passcode") -> result.passwordId = result.passwordId ?: id

                    hints.any { it.contains("username") || it.contains("email") } ||
                        searchable.contains("username") ||
                        searchable.contains("email") ||
                        searchable.contains("login") ||
                        searchable.contains("user") -> result.usernameId = result.usernameId ?: id
                }
            }

            for (i in 0 until node.childCount) {
                visit(node.getChildAt(i), result)
            }
        }
    }

    private data class AutofillFields(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?,
        val matchTerms: List<String>,
    )

    private class MutableAutofillFields {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        val matchTerms = mutableListOf<String>()

        fun toFields(): AutofillFields =
            AutofillFields(
                usernameId = usernameId,
                passwordId = passwordId,
                matchTerms = matchTerms.distinct(),
            )
    }

    companion object {
        private const val MAX_DATASETS = 5
    }
}
