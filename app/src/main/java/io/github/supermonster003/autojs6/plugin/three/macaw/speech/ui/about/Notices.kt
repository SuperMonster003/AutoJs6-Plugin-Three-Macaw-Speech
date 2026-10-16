package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.StandaloneActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.EmptyState
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.Hairline
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.MacawScreen
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SectionHeader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.components.SettingsRow
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.update.AppUpdateChecker

/** One block of a legal text. zh-CN: 法律文本的一个块. */
internal sealed interface LegalBlock {
    data class Heading(val level: Int, val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
    data class Bullet(val text: String) : LegalBlock

    /** A table row as "column: value" pairs, the header giving the columns. zh-CN: 以 "列: 值" 表示的表格行. */
    data class Row(val cells: List<Pair<String, String>>) : LegalBlock
}

/**
 * Turns the legal texts into readable blocks (roadmap P5.2): the Markdown of `THIRD_PARTY_NOTICES.md` (headings,
 * paragraphs, bullets, tables shown row by row) and the plain license texts, whose hard-wrapped lines are joined into
 * paragraphs; the words stay as written. Pure Kotlin.
 * zh-CN: 把法律文本转为易读的块 (路线图 P5.2): `THIRD_PARTY_NOTICES.md` 的 Markdown (标题, 段落, 列表, 表格逐行显示) 与纯文本许可, 后者的硬换行
 * 合并为段落; 文字保持原样. 纯 Kotlin.
 */
internal object LegalText {

    fun markdown(text: String): List<LegalBlock> {
        val blocks = ArrayList<LegalBlock>()
        val paragraph = ArrayList<String>()
        var header: List<String>? = null
        fun flush() {
            if (paragraph.isNotEmpty()) blocks += LegalBlock.Paragraph(paragraph.joinToString(" ").let(::plain))
            paragraph.clear()
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {
                    flush()
                    header = null
                }
                line.startsWith("#") -> {
                    flush()
                    val level = line.takeWhile { it == '#' }.length
                    blocks += LegalBlock.Heading(level, plain(line.drop(level).trim()))
                }
                line.startsWith("|") -> {
                    flush()
                    val cells = line.trim('|').split('|').map { plain(it.trim()) }
                    when {
                        header == null -> header = cells
                        cells.all { cell -> cell.isNotEmpty() && cell.all { it == '-' || it == ':' } } -> Unit
                        else -> blocks += LegalBlock.Row(header.zip(cells))
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    flush()
                    blocks += LegalBlock.Bullet(plain(line.drop(2)))
                }
                // A continuation line of the previous bullet. zh-CN: 上一个列表项的续行.
                raw.startsWith("  ") && blocks.lastOrNull() is LegalBlock.Bullet && paragraph.isEmpty() -> {
                    val last = blocks.removeAt(blocks.lastIndex) as LegalBlock.Bullet
                    blocks += LegalBlock.Bullet(last.text + " " + plain(line))
                }
                else -> paragraph += line
            }
        }
        flush()
        return blocks
    }

    fun plainText(text: String): List<LegalBlock> = text.replace("\r\n", "\n").split(Regex("\n\\s*\n")).mapNotNull { block ->
        val joined = block.lines().joinToString(" ") { it.trim() }.trim()
        if (joined.isEmpty()) null else LegalBlock.Paragraph(joined)
    }

    /** Drops the Markdown code marks; the text stays. zh-CN: 去掉 Markdown 代码标记, 文字不变. */
    private fun plain(text: String): String = text.replace("`", "")
}

/** A legal document of `assets/legal/`. zh-CN: `assets/legal/` 中的法律文档. */
internal enum class LegalDocument(val asset: String, val markdown: Boolean) {
    NOTICES("legal/THIRD_PARTY_NOTICES.md", true),
    GPL("legal/GPL-3.0.txt", false),
    MPL("legal/LICENSE.txt", false),
    UNICODE("legal/COPYING.UCD", false),
}

/**
 * Licenses and notices (roadmap D32, P5.2): the license of the APK as a whole (GPL-3.0-or-later, no warranty), the
 * source license (MPL-2.0), the third-party notices, the Unicode Terms of Use and where the corresponding source is,
 * all inside the APK (GPL-3.0 sections 4 and 5(d): appropriate legal notices in the interactive user interface).
 * zh-CN: 许可与声明 (路线图 D32, P5.2): APK 整体的许可 (GPL-3.0-or-later, 不提供担保), 源码许可 (MPL-2.0), 第三方声明, Unicode 使用条款以及对应源码的位置,
 * 全部内置于 APK (GPL-3.0 第 4 条与第 5(d) 条: 交互界面中适当的法律声明).
 */
class NoticesActivity : StandaloneActivity() {

    @Composable
    override fun Content() {
        MacawScreen(title = stringResource(R.string.notices_title), onBack = ::finish) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("macaw.notices")) {
                Text(
                    stringResource(R.string.notices_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("macaw.notices.statement"),
                )
                SectionHeader(stringResource(R.string.notices_section_documents))
                DocumentRow(R.string.notices_third_party, R.string.notices_third_party_summary, LegalDocument.NOTICES, "notices")
                Hairline()
                DocumentRow(R.string.notices_gpl, R.string.notices_gpl_summary, LegalDocument.GPL, "gpl")
                Hairline()
                DocumentRow(R.string.notices_mpl, R.string.notices_mpl_summary, LegalDocument.MPL, "mpl")
                Hairline()
                DocumentRow(R.string.notices_unicode, R.string.notices_unicode_summary, LegalDocument.UNICODE, "unicode")
                SectionHeader(stringResource(R.string.notices_section_source))
                SettingsRow(
                    title = stringResource(R.string.notices_corresponding_source),
                    summary = stringResource(R.string.notices_corresponding_source_summary),
                    icon = R.drawable.ic_code_24,
                    onClick = { open(AppUpdateChecker.RELEASES_PAGE) },
                    modifier = Modifier.testTag("macaw.notices.source"),
                )
                Hairline()
                SettingsRow(
                    title = stringResource(R.string.notices_plugin_source),
                    summary = REPOSITORY,
                    icon = R.drawable.ic_open_in_new_24,
                    onClick = { open(REPOSITORY) },
                    modifier = Modifier.testTag("macaw.notices.repository"),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun DocumentRow(title: Int, summary: Int, document: LegalDocument, tag: String) {
        val name = stringResource(title)
        SettingsRow(
            title = name,
            summary = stringResource(summary),
            icon = R.drawable.ic_license_24,
            onClick = { startActivity(LegalDocumentActivity.intent(this, document, name)) },
            modifier = Modifier.testTag("macaw.notices.$tag"),
        )
    }

    private fun open(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    internal companion object {
        const val REPOSITORY = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Macaw-Speech"
    }
}

/** Shows one document of `assets/legal/`, selectable. zh-CN: 显示 `assets/legal/` 中的一个文档, 文字可选择. */
class LegalDocumentActivity : StandaloneActivity() {

    private val document: LegalDocument by lazy {
        runCatching { LegalDocument.valueOf(intent.getStringExtra(EXTRA_DOCUMENT).orEmpty()) }.getOrDefault(LegalDocument.NOTICES)
    }

    private val blocks: List<LegalBlock> by lazy {
        runCatching { assets.open(document.asset).use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()?.let { text ->
            if (document.markdown) LegalText.markdown(text) else LegalText.plainText(text)
        }.orEmpty()
    }

    @Composable
    override fun Content() {
        MacawScreen(title = intent.getStringExtra(EXTRA_TITLE) ?: stringResource(R.string.notices_title), onBack = ::finish) { padding ->
            if (blocks.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(title = stringResource(R.string.notices_unavailable), icon = R.drawable.ic_warning_24)
                }
                return@MacawScreen
            }
            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("macaw.legal.${document.name.lowercase()}")) {
                    items(blocks) { block -> Block(block) }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    @Composable
    private fun Block(block: LegalBlock) {
        val horizontal = Modifier.padding(horizontal = 24.dp)
        when (block) {
            is LegalBlock.Heading -> Text(
                block.text,
                style = if (block.level <= 2) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = horizontal.padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
            )
            is LegalBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = horizontal.padding(vertical = 6.dp))
            is LegalBlock.Bullet -> Text("\u2022 " + block.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = horizontal.padding(vertical = 4.dp))
            is LegalBlock.Row -> Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                block.cells.forEachIndexed { index, (column, value) ->
                    if (index == 0) {
                        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        Text("$column: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT = "document"
        private const val EXTRA_TITLE = "title"

        internal fun intent(context: Context, document: LegalDocument, title: String): Intent =
            Intent(context, LegalDocumentActivity::class.java).putExtra(EXTRA_DOCUMENT, document.name).putExtra(EXTRA_TITLE, title)
    }
}
