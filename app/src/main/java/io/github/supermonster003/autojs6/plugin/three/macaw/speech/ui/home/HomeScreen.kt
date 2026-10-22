package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LicenseNotices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.LocalMacawPalette
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.CheckRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Choice
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.ChoiceDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.DialogDefaults
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MessageDialog
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.TopBarAction

/** What the home screen asks its Activity to do. zh-CN: 首页请求 Activity 执行的动作. */
internal class HomeActions(
    val onTryout: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onRequestMicrophone: () -> Unit = {},
    val onOpenTtsSettings: () -> Unit = {},
    val onPickArchive: () -> Unit = {},
    val onPickFolder: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
)

/**
 * The home screen (roadmap D26, P5.1): status card, the models grouped by kind with live download progress, the import
 * entry and the dialogs of [HomeController]. [scrollToKind] opens at a group (the TTS settings ask for the synthesis
 * models).
 * zh-CN: 首页 (路线图 D26, P5.1): 状态卡, 按种类分组且带实时下载进度的模型, 导入入口以及 [HomeController] 的对话框. [scrollToKind] 打开时定位到
 * 某组 (TTS 设置请求合成模型时).
 */
@Composable
internal fun HomeScreen(controller: HomeController, actions: HomeActions, scrollToKind: String? = null) {
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val message = controller.message
    LaunchedEffect(message?.first) {
        val text = message?.second?.let { describe(resources, it) } ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
    }
    MacawScreen(
        title = stringResource(R.string.app_name),
        snackbarHostState = snackbar,
        actions = {
            TopBarAction(R.drawable.ic_mic_24, stringResource(R.string.tryout_title), "macaw.home.tryoutAction", actions.onTryout)
            TopBarAction(R.drawable.ic_settings_24, stringResource(R.string.settings_title), "macaw.home.settings", actions.onSettings)
        },
    ) { padding ->
        val groups = controller.groups
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).testTag("macaw.home.list")) {
            item(key = "status") { StatusCard(controller, actions) }
            if (!controller.networkAvailable) {
                item(key = "offline") { Notice(stringResource(R.string.home_offline_notice), Modifier.testTag("macaw.home.offline")) }
            }
            if (controller.loaded && groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.home_empty_body),
                        icon = R.drawable.ic_model_24,
                    )
                }
            }
            groups.forEach { group ->
                item(key = "header:${group.kind}") {
                    SectionHeader(
                        stringResource(R.string.home_group_title, stringResource(HomeTexts.kindTitle(group.kind)), group.installed, group.rows.size),
                        Modifier.testTag("macaw.home.group.${group.kind}"),
                    )
                }
                HomeTexts.kindAbout(group.kind)?.let { about ->
                    item(key = "about:${group.kind}") {
                        Text(
                            stringResource(about),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp).testTag("macaw.home.about.${group.kind}"),
                        )
                    }
                }
                items(group.rows, key = { "model:${it.id}" }) { row ->
                    ModelRowItem(
                        row = row,
                        online = controller.networkAvailable,
                        onClick = { controller.dialog = HomeDialog.Details(row.id) },
                        onDownload = { controller.requestDownload(row.id) },
                        onPause = { controller.pause(row.id) },
                        onCancel = { controller.cancelDownload(row.id) },
                    )
                    if (row != group.rows.last()) Hairline()
                }
            }
            item(key = "import") {
                SectionHeader(stringResource(R.string.home_section_import))
                SettingsRow(
                    title = stringResource(R.string.home_import_title),
                    summary = stringResource(if (controller.importRunning) R.string.home_import_running else R.string.home_import_summary),
                    icon = R.drawable.ic_upload_24,
                    enabled = !controller.importRunning,
                    onClick = { controller.dialog = HomeDialog.ImportChoice },
                    modifier = Modifier.testTag("macaw.home.import"),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    ScrollTo(listState, controller, scrollToKind)
    HomeDialogs(controller, actions)
}

@Composable
private fun ScrollTo(listState: LazyListState, controller: HomeController, kind: String?) {
    var done by rememberSaveable { mutableStateOf(false) }
    val groups = controller.groups
    LaunchedEffect(kind, groups.isNotEmpty()) {
        if (kind == null || done || groups.isEmpty()) return@LaunchedEffect
        // Items: the status card, the offline notice when shown, then one header and the rows of every group.
        var index = if (controller.networkAvailable) 1 else 2
        for (group in groups) {
            if (group.kind == kind) break
            index += 1 + group.rows.size
        }
        listState.scrollToItem(index)
        done = true
    }
}

@Composable
private fun StatusCard(controller: HomeController, actions: HomeActions) {
    val palette = LocalMacawPalette.current
    val status = controller.status
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("macaw.home.status"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            StatusLine(stringResource(R.string.home_status_runtime), stringResource(R.string.home_status_runtime_value, status.runtimeVersion), "macaw.home.status.runtime")
            StatusLine(
                stringResource(R.string.home_status_models),
                stringResource(R.string.home_status_models_value, controller.installedCount, controller.catalogVersion),
                "macaw.home.status.models",
            )
            StatusLine(
                title = stringResource(R.string.home_status_microphone),
                value = stringResource(if (status.microphoneGranted) R.string.home_status_granted else R.string.home_status_not_granted),
                tag = "macaw.home.status.microphone",
                valueColor = Color(if (status.microphoneGranted) palette.success else palette.warning),
                action = if (status.microphoneGranted) null else stringResource(R.string.home_status_grant) to actions.onRequestMicrophone,
            )
            StatusLine(
                title = stringResource(R.string.home_status_tts_engine),
                value = stringResource(if (status.ttsEngineSelected) R.string.home_status_tts_selected else R.string.home_status_tts_not_selected),
                tag = "macaw.home.status.tts",
                action = stringResource(R.string.home_status_open_settings) to actions.onOpenTtsSettings,
            )
            Button(
                onClick = actions.onTryout,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).defaultMinSize(minHeight = 48.dp).testTag("macaw.home.tryout"),
            ) {
                Icon(painterResource(R.drawable.ic_mic_24), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tryout_title))
            }
        }
    }
}

@Composable
private fun StatusLine(title: String, value: String, tag: String, valueColor: Color = Color.Unspecified, action: Pair<String, () -> Unit>? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(horizontal = 16.dp, vertical = 4.dp).semantics(mergeDescendants = true) {}.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor)
        }
        if (action != null) {
            TextButton(onClick = action.second, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("$tag.action")) { Text(action.first) }
        }
    }
}

@Composable
private fun Notice(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(LocalMacawPalette.current.warning),
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelRowItem(row: ModelRow, online: Boolean, onClick: () -> Unit, onDownload: () -> Unit, onPause: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 72.dp)
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .testTag("macaw.model.${row.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_model_24), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (row.isDefault && row.state == RowState.Installed) Badge(stringResource(R.string.model_badge_default), accent = true, tag = "macaw.model.${row.id}.default")
                if (row.recommended && !row.builtin) Badge(stringResource(R.string.model_badge_recommended))
                if (row.builtin) Badge(stringResource(R.string.model_badge_builtin))
                if (row.imported) Badge(stringResource(R.string.model_badge_imported))
                if (row.license != LicenseTag.COMMERCIAL) Badge(stringResource(HomeTexts.license(row.license)), warning = true, tag = "macaw.model.${row.id}.license")
            }
            Text(
                HomeTexts.summary(context, row),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            when (val state = row.state) {
                is RowState.Running -> {
                    val fraction = if (state.total > 0) (state.done.toFloat() / state.total).coerceIn(0f, 1f) else null
                    if (fraction != null) {
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("macaw.model.${row.id}.progress"))
                        Text(
                            stringResource(R.string.model_progress, HomeTexts.size(context, state.done), HomeTexts.size(context, state.total), (fraction * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("macaw.model.${row.id}.progress"))
                    }
                }
                is RowState.Queued -> StateText(stringResource(R.string.model_state_queued))
                is RowState.Paused -> StateText(stringResource(R.string.model_state_paused, HomeTexts.size(context, state.downloadedBytes)))
                else -> Unit
            }
        }
        Spacer(Modifier.width(4.dp))
        when (row.state) {
            RowState.Installed -> Icon(
                painterResource(R.drawable.ic_check_circle_24),
                contentDescription = stringResource(R.string.model_state_installed),
                tint = Color(LocalMacawPalette.current.success),
                modifier = Modifier.padding(12.dp).size(24.dp).testTag("macaw.model.${row.id}.installed"),
            )
            RowState.Available -> RowButton(R.drawable.ic_download_24, stringResource(R.string.model_action_download_named, row.name), "macaw.model.${row.id}.download", online, onDownload)
            is RowState.Paused -> {
                RowButton(R.drawable.ic_play_arrow_24, stringResource(R.string.model_action_resume_named, row.name), "macaw.model.${row.id}.resume", online, onDownload)
                RowButton(R.drawable.ic_close_24, stringResource(R.string.model_action_cancel_named, row.name), "macaw.model.${row.id}.cancel", true, onCancel)
            }
            is RowState.Running -> {
                RowButton(R.drawable.ic_pause_24, stringResource(R.string.model_action_pause_named, row.name), "macaw.model.${row.id}.pause", true, onPause)
                RowButton(R.drawable.ic_close_24, stringResource(R.string.model_action_cancel_named, row.name), "macaw.model.${row.id}.cancel", true, onCancel)
            }
            is RowState.Queued -> RowButton(R.drawable.ic_close_24, stringResource(R.string.model_action_cancel_named, row.name), "macaw.model.${row.id}.cancel", true, onCancel)
        }
    }
}

@Composable
private fun StateText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun RowButton(icon: Int, description: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
        Icon(painterResource(icon), contentDescription = description)
    }
}

@Composable
private fun Badge(text: String, accent: Boolean = false, warning: Boolean = false, tag: String? = null) {
    val palette = LocalMacawPalette.current
    val color = when {
        accent -> MaterialTheme.colorScheme.primary
        warning -> Color(palette.warning)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

@Composable
private fun HomeDialogs(controller: HomeController, actions: HomeActions) {
    val context = LocalContext.current
    when (val dialog = controller.dialog) {
        null -> Unit
        is HomeDialog.Details -> {
            val row = controller.row(dialog.modelId)
            if (row != null) DetailsDialog(row, controller, actions) else SideEffect { controller.dialog = null }
        }
        is HomeDialog.License -> {
            val row = controller.row(dialog.modelId)
            val name = row?.name ?: dialog.modelId.ifEmpty { stringResource(R.string.home_import_title) }
            val notice = stringResource(LicenseNotices.resource(row?.noticeKey))
            val expression = row?.let { stringResource(R.string.license_dialog_expression, it.licenseSpdx) }
            MessageDialog(
                title = stringResource(R.string.license_dialog_title, name),
                message = if (expression == null) notice else notice + "\n\n" + expression,
                onDismissRequest = { controller.dialog = null },
                onConfirm = { controller.acceptLicense(dialog) },
                confirmText = stringResource(R.string.license_dialog_accept),
                tag = "macaw.license",
                neutralText = row?.noticeUrl?.let { stringResource(R.string.license_dialog_source) },
                onNeutral = row?.noticeUrl?.let { url -> { actions.onOpenUrl(url) } },
            )
        }
        is HomeDialog.Metered -> {
            var remember by rememberSaveable { mutableStateOf(false) }
            val size = controller.row(dialog.modelId)?.sizeBytes ?: 0L
            MessageDialog(
                title = stringResource(R.string.metered_dialog_title),
                message = stringResource(R.string.metered_dialog_message, HomeTexts.size(context, size)),
                onDismissRequest = { controller.dialog = null },
                onConfirm = { controller.acceptMetered(dialog, remember) },
                confirmText = stringResource(R.string.metered_dialog_download),
                tag = "macaw.metered",
            ) {
                CheckRow(Choice(stringResource(R.string.metered_dialog_remember)), remember, { remember = it }, Modifier.testTag("macaw.metered.remember"))
            }
        }
        is HomeDialog.Delete -> {
            val row = controller.row(dialog.modelId)
            MessageDialog(
                title = stringResource(R.string.delete_dialog_title, row?.name ?: dialog.modelId),
                message = stringResource(if (row?.isDefault == true) R.string.delete_dialog_default_message else R.string.delete_dialog_message),
                onDismissRequest = { controller.dialog = null },
                onConfirm = { controller.confirmDelete(dialog.modelId) },
                confirmText = stringResource(R.string.model_action_delete),
                tag = "macaw.delete",
            )
        }
        HomeDialog.ImportChoice -> ChoiceDialog(
            title = stringResource(R.string.home_import_title),
            options = listOf(
                Choice(stringResource(R.string.import_choice_archive), stringResource(R.string.import_choice_archive_note)),
                Choice(stringResource(R.string.import_choice_folder), stringResource(R.string.import_choice_folder_note)),
            ),
            selectedIndex = 0,
            onDismissRequest = { controller.dialog = null },
            onConfirm = { index ->
                controller.dialog = null
                if (index == 0) actions.onPickArchive() else actions.onPickFolder()
            },
            tag = "macaw.importChoice",
        )
        is HomeDialog.ImportForm -> ImportFormDialog(dialog.import, onDismiss = { controller.dialog = null }) { request -> controller.submitImportForm(dialog.import, request) }
    }
}

@Composable
private fun DetailsDialog(row: ModelRow, controller: HomeController, actions: HomeActions) {
    val context = LocalContext.current
    MacawDialog(
        title = row.name,
        onDismissRequest = { controller.dialog = null },
        confirmText = stringResource(R.string.ui_dialog_close),
        onConfirm = { controller.dialog = null },
        dismissText = null,
        tag = "macaw.details",
    ) {
        // The actions come first, so they stay in view when the facts are taller than the dialog.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (row.state) {
                RowState.Installed -> {
                    if (!row.isDefault && !row.builtin) DetailButton(stringResource(R.string.model_action_set_default), "macaw.details.default") { controller.setDefault(row.id) }
                    if (!row.builtin) DetailButton(stringResource(R.string.model_action_delete), "macaw.details.delete") { controller.requestDelete(row.id) }
                }
                RowState.Available -> DetailButton(stringResource(R.string.model_action_download), "macaw.details.download", controller.networkAvailable) {
                    controller.dialog = null
                    controller.requestDownload(row.id)
                }
                is RowState.Paused -> {
                    DetailButton(stringResource(R.string.model_action_resume), "macaw.details.resume", controller.networkAvailable) {
                        controller.dialog = null
                        controller.requestDownload(row.id)
                    }
                    DetailButton(stringResource(R.string.model_action_cancel), "macaw.details.cancel") { controller.cancelDownload(row.id) }
                }
                is RowState.Running, is RowState.Queued -> {
                    if (row.state is RowState.Running) DetailButton(stringResource(R.string.model_action_pause), "macaw.details.pause") { controller.pause(row.id) }
                    DetailButton(stringResource(R.string.model_action_cancel), "macaw.details.cancel") { controller.cancelDownload(row.id) }
                }
            }
            row.noticeUrl?.let { url -> DetailButton(stringResource(R.string.license_dialog_source), "macaw.details.source") { actions.onOpenUrl(url) } }
        }
        Column(modifier = Modifier.padding(horizontal = DialogDefaults.contentPadding)) {
            Fact(stringResource(R.string.model_detail_id), row.id)
            Fact(stringResource(R.string.model_detail_kind), stringResource(HomeTexts.kindTitle(row.kind)) + " \u00b7 " + row.modelType + " \u00b7 v" + row.version)
            HomeTexts.kindAbout(row.kind)?.let { about ->
                Text(
                    stringResource(about),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("macaw.details.about"),
                )
            }
            if (row.languages.isNotEmpty()) Fact(stringResource(R.string.model_detail_languages), HomeTexts.languages(context, row.languages))
            if (row.sizeBytes > 0) Fact(stringResource(R.string.model_detail_download_size), HomeTexts.size(context, row.sizeBytes))
            row.installedBytes?.takeIf { row.state == RowState.Installed }?.let { Fact(stringResource(R.string.model_detail_installed_size), HomeTexts.size(context, it)) }
            row.speakers?.takeIf { row.kind == "tts" }?.let { Fact(stringResource(R.string.model_detail_speakers), it.toString()) }
            row.sampleRate?.let { Fact(stringResource(R.string.model_detail_sample_rate), stringResource(R.string.model_detail_sample_rate_value, it)) }
            Fact(stringResource(R.string.model_detail_license), row.licenseSpdx + " \u00b7 " + stringResource(HomeTexts.license(row.license)))
            Text(
                stringResource(LicenseNotices.resource(row.noticeKey)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DetailButton(text: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag(tag)) { Text(text) }
}

/** Snackbar text of a [HomeMessage]. zh-CN: [HomeMessage] 的 snackbar 文字. */
internal fun describe(context: android.content.res.Resources, message: HomeMessage): String = when (message) {
    HomeMessage.Offline -> context.getString(R.string.home_offline_message)
    is HomeMessage.Installed -> context.getString(R.string.model_message_installed, message.name)
    is HomeMessage.Imported -> context.getString(R.string.model_message_imported, message.name)
    is HomeMessage.Removed -> context.getString(R.string.model_message_removed, message.name)
    is HomeMessage.DefaultSet -> context.getString(R.string.model_message_default, message.name)
    is HomeMessage.Failed -> context.getString(HomeTexts.error(message.code, message.insufficientSpace))
}
