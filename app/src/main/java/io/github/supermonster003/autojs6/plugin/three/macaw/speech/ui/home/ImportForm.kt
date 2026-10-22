package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CatalogRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.Importer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawDialog
import java.util.Locale

/**
 * The fields of the custom model form and their checks, the same rules `Importer` applies (roadmap D23). Pure Kotlin.
 * zh-CN: 自定义模型表单的字段及其检查, 规则与 `Importer` 相同 (路线图 D23). 纯 Kotlin.
 */
internal object ImportFormPolicy {
    enum class Problem { ID, NAME, LANGUAGES }

    fun modelTypes(kind: String): List<String> = CatalogRules.MODEL_TYPES.filterValues { it.kind == kind }.keys.toList()

    /** A suggested id from a file or folder name: lowercase ASCII, other characters as `-`. zh-CN: 由文件或文件夹名建议的 ID. */
    fun suggestId(displayName: String?): String {
        val base = displayName.orEmpty().substringBefore('.').lowercase(Locale.ROOT)
            .map { if (it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_') it else '-' }.joinToString("")
            .trim('-', '_').take(48)
        val id = if (base.length >= 2) base else "imported-model"
        return if (CatalogRules.MODEL_ID.matches(id)) "user-$id".take(64) else id
    }

    fun languages(text: String): List<String> = text.split(',', ' ', '\u3001').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun problems(id: String, name: String, languages: String): Set<Problem> {
        val out = HashSet<Problem>()
        if (!ModelStore.IMPORT_ID.matches(id) || CatalogRules.MODEL_ID.matches(id)) out += Problem.ID
        if (name.isBlank() || name.length > 128) out += Problem.NAME
        val codes = languages(languages)
        if (codes.isEmpty() || codes.size > 32 || codes.any { !CatalogRules.LANGUAGE.matches(it) }) out += Problem.LANGUAGES
        return out
    }

    fun request(id: String, kind: String, modelType: String, name: String, languages: String): Importer.Request =
        Importer.Request(modelId = id, kind = kind, modelType = modelType, name = name.trim(), languages = languages(languages))
}

/**
 * The custom model form (roadmap P5.1, maintainer's choice of 2026-10-16): shown when an import matches no catalog model;
 * kind, model type, name, languages and id, checked as the importer does; confirm retries the same file or folder.
 * zh-CN: 自定义模型表单 (路线图 P5.1, 维护者 2026-10-16 的选择): 导入不匹配任何目录模型时显示; 种类, 模型类型, 名称, 语言与 ID 按导入器的规则检查;
 * 确定后用同一文件或文件夹重试.
 */
@Composable
internal fun ImportFormDialog(pending: PendingImport, onDismiss: () -> Unit, onConfirm: (Importer.Request) -> Unit) {
    var kind by rememberSaveable { mutableStateOf("asr") }
    var modelType by rememberSaveable { mutableStateOf(ImportFormPolicy.modelTypes("asr").first()) }
    var id by rememberSaveable { mutableStateOf(ImportFormPolicy.suggestId(pending.source.displayName)) }
    var name by rememberSaveable { mutableStateOf(pending.source.displayName?.substringBefore('.').orEmpty()) }
    var languages by rememberSaveable { mutableStateOf("") }
    val problems = ImportFormPolicy.problems(id, name, languages)
    MacawDialog(
        title = stringResource(R.string.import_form_title),
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.import_form_import),
        onConfirm = { if (problems.isEmpty()) onConfirm(ImportFormPolicy.request(id, kind, modelType, name, languages)) },
        dismissText = stringResource(R.string.ui_dialog_cancel),
        confirmEnabled = problems.isEmpty(),
        tag = "macaw.importForm",
    ) {
        Text(
            stringResource(R.string.import_form_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding),
        )
        Label(stringResource(R.string.import_form_kind))
        CatalogRules.CUSTOM_IMPORT_KINDS.forEach { value ->
            ChoiceRow(Choice(stringResource(HomeTexts.kindTitle(value))), selected = kind == value, onSelect = {
                kind = value
                modelType = ImportFormPolicy.modelTypes(value).first()
            }, modifier = Modifier.testTag("macaw.importForm.kind.$value"))
        }
        Label(stringResource(R.string.import_form_model_type))
        ImportFormPolicy.modelTypes(kind).forEach { value ->
            ChoiceRow(Choice(value), selected = modelType == value, onSelect = { modelType = value }, modifier = Modifier.testTag("macaw.importForm.type.$value"))
        }
        Column(modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding)) {
            Field(name, { name = it.take(128) }, stringResource(R.string.import_form_name), ImportFormPolicy.Problem.NAME in problems, stringResource(R.string.import_form_name_invalid), "name")
            Field(
                languages, { languages = it.take(200) }, stringResource(R.string.import_form_languages),
                ImportFormPolicy.Problem.LANGUAGES in problems, stringResource(R.string.import_form_languages_invalid), "languages", ltr = true,
            )
            Field(id, { id = it.take(64) }, stringResource(R.string.import_form_id), ImportFormPolicy.Problem.ID in problems, stringResource(R.string.import_form_id_invalid), "id", ltr = true)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = DialogDefaults.contentPadding, end = DialogDefaults.contentPadding, top = 16.dp),
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, error: Boolean, errorText: String, tag: String, ltr: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error,
        supportingText = if (error) ({ Text(errorText) }) else null,
        singleLine = true,
        textStyle = if (ltr) MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr) else MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("macaw.importForm.$tag"),
    )
}
