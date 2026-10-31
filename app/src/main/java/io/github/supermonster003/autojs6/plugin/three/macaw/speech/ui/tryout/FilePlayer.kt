package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout

import android.os.SystemClock
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAnalysis
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackCue
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackCues
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.PlaybackRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** How long the page stays where a manual scroll left it (roadmap D172). zh-CN: 手动滚动后页面停留的时长 (路线图 D172). */
internal const val FOLLOW_HOLD_MILLIS = 4000L

/**
 * Keeps the sounding line of a long result in view (roadmap D164): the page scrolls with the playback until the user
 * drags it, and goes on [FOLLOW_HOLD_MILLIS] after that scroll, fling included, has stopped; [serial] grows then, so the
 * sounding line asks again. A scroll of the page itself leaves no drag.
 * zh-CN: 让长结果中正在发声的行保持可见 (路线图 D164): 页面随播放滚动, 直到用户拖动; 那次滚动 (含惯性) 停止 [FOLLOW_HOLD_MILLIS] 后继续, 此时 [serial]
 * 增加, 正在发声的行再次请求. 页面自身的滚动不产生拖动.
 */
@Stable
internal class PlaybackFollow(private val scroll: ScrollState) {
    private var dragged = false
    private var heldUntil = 0L

    /** The height the pinned bar covers. zh-CN: 固定的播放条遮住的高度. */
    var barHeight = 0

    var serial by mutableIntStateOf(0)
        private set

    val following: Boolean get() = !dragged && SystemClock.uptimeMillis() >= heldUntil

    suspend fun watch() = coroutineScope {
        launch { scroll.interactionSource.interactions.collect { if (it is DragInteraction.Start) dragged = true } }
        snapshotFlow { scroll.isScrollInProgress }.collectLatest { moving ->
            if (!moving && dragged) {
                dragged = false
                heldUntil = SystemClock.uptimeMillis() + FOLLOW_HOLD_MILLIS
                delay(FOLLOW_HOLD_MILLIS)
                serial += 1
            }
        }
    }
}

/**
 * What the rows of a shown result need from the player; [current] changes only when another line starts sounding.
 * zh-CN: 显示中的结果各行需要的播放信息; [current] 只在另一行开始发声时变化.
 */
@Stable
internal class ResultPlayer(val playback: FilePlayback, val follow: PlaybackFollow, private val cues: PlaybackCues) {
    val ready by derivedStateOf { playback.state == FilePlayback.State.READY }
    val current by derivedStateOf { if (ready) cues.at(playback.positionMillis) else null }
}

/**
 * The compact bar above the result (roadmap D164, D171): position, length and a slider, 5 s back and forward, play and
 * pause, the speed and the lyrics view switch; while the file opens a progress line, and the explanation of D170 when it
 * cannot be played. The background hides the list that scrolls under the pinned bar.
 * zh-CN: 结果上方的紧凑播放条 (路线图 D164, D171): 位置, 总时长与进度条, 前后 5 秒, 播放与暂停, 倍速与歌词式视图开关; 打开文件时为进度行, 不能播放时为
 * D170 的说明. 背景遮住从固定的播放条下方滚过的列表.
 */
@Composable
internal fun PlayerBar(playback: FilePlayback, lyrics: Boolean, lyricsEnabled: Boolean, onLyrics: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 4.dp).testTag("macaw.tryout.file.player")) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            when (playback.state) {
                FilePlayback.State.READY -> Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Timeline(playback)
                    Transport(playback, lyrics, lyricsEnabled, onLyrics)
                }
                FilePlayback.State.UNAVAILABLE -> Text(
                    stringResource(R.string.tryout_file_player_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp).testTag("macaw.tryout.file.player.unavailable"),
                )
                else -> Row(modifier = Modifier.padding(16.dp).testTag("macaw.tryout.file.player.preparing"), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.tryout_file_player_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Timeline(playback: FilePlayback) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val total = playback.durationMillis.coerceAtLeast(1).toFloat()
    val shown = dragged ?: playback.positionMillis.toFloat()
    val label = stringResource(R.string.tryout_file_player_position)
    val position = TranscriptExport.clock(shown.toLong())
    val length = TranscriptExport.clock(playback.durationMillis)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Time(position, "macaw.tryout.file.player.position")
        Slider(
            value = shown.coerceIn(0f, total),
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let { playback.seekTo(it.toLong()) }
                dragged = null
            },
            valueRange = 0f..total,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics {
                contentDescription = label
                stateDescription = "$position / $length"
            }.testTag("macaw.tryout.file.player.seek"),
        )
        Time(length, "macaw.tryout.file.player.duration")
    }
}

@Composable
private fun Time(text: String, tag: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(min = 40.dp).testTag(tag),
    )
}

@Composable
private fun Transport(playback: FilePlayback, lyrics: Boolean, lyricsEnabled: Boolean, onLyrics: (Boolean) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { playback.skip(-PlaybackRules.SKIP_MILLIS) }, modifier = Modifier.testTag("macaw.tryout.file.player.back")) {
            Icon(painterResource(R.drawable.ic_replay_5_24), contentDescription = stringResource(R.string.tryout_file_player_back))
        }
        FilledIconButton(onClick = playback::toggle, modifier = Modifier.testTag("macaw.tryout.file.player.toggle")) {
            Icon(
                painterResource(if (playback.playing) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24),
                contentDescription = stringResource(if (playback.playing) R.string.tryout_file_player_pause else R.string.tryout_file_player_play),
            )
        }
        IconButton(onClick = { playback.skip(PlaybackRules.SKIP_MILLIS) }, modifier = Modifier.testTag("macaw.tryout.file.player.forward")) {
            Icon(painterResource(R.drawable.ic_forward_5_24), contentDescription = stringResource(R.string.tryout_file_player_forward))
        }
        Spacer(Modifier.weight(1f))
        Box {
            val speed = PlaybackRules.speedLabel(playback.speed)
            val description = stringResource(R.string.tryout_file_player_speed, speed)
            TextButton(onClick = { choosing = true }, modifier = Modifier.semantics { contentDescription = description }.testTag("macaw.tryout.file.player.speed")) {
                Text(speed, modifier = Modifier.testTag("macaw.tryout.file.player.speed.value"))
            }
            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                PlaybackRules.SPEEDS.forEachIndexed { index, value ->
                    DropdownMenuItem(
                        text = { Text(PlaybackRules.speedLabel(value)) },
                        onClick = {
                            choosing = false
                            playback.changeSpeed(value)
                        },
                        modifier = Modifier.testTag("macaw.tryout.file.player.speed.$index"),
                    )
                }
            }
        }
        IconToggleButton(checked = lyrics, onCheckedChange = onLyrics, enabled = lyricsEnabled, modifier = Modifier.testTag("macaw.tryout.file.player.lyrics")) {
            Icon(painterResource(R.drawable.ic_subtitles_24), contentDescription = stringResource(R.string.tryout_file_player_lyrics))
        }
    }
}

/**
 * One row of a result under a player (roadmap D164): the sounding row is tinted in [accent] and kept in view below the
 * pinned bar with room for the lines after it, and a tap plays on from [startMillis]. Without a player it is a plain row.
 * zh-CN: 播放器下的结果行 (路线图 D164): 正在发声的行以 [accent] 着色, 保持在固定播放条下方可见并为后面几行留出空间, 点击从 [startMillis] 继续播放. 没有
 * 播放器时为普通行.
 */
@Composable
internal fun CueRow(player: ResultPlayer?, index: Int, startMillis: Long, accent: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val current = player?.current?.index == index
    val requester = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val below = with(LocalDensity.current) { FOLLOW_ROOM_BELOW.toPx() }
    if (current && player.playback.playing) {
        LaunchedEffect(player.follow.serial) {
            val follow = player.follow
            if (follow.following) requester.bringIntoView(Rect(0f, -follow.barHeight.toFloat(), size.width.toFloat(), size.height + below))
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .onSizeChanged { size = it }
            .clip(RoundedCornerShape(8.dp))
            .background(if (current) accent.copy(alpha = HIGHLIGHT_ALPHA) else Color.Transparent)
            .clickable(enabled = player?.ready == true, onClickLabel = stringResource(R.string.tryout_file_player_play)) { player?.playback?.jump(startMillis) }
            .then(if (current) Modifier.semantics { selected = true } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        content = content,
    )
}

private val FOLLOW_ROOM_BELOW = 96.dp
private const val HIGHLIGHT_ALPHA = 0.16f
private const val SIDE_ALPHA = 0.6f

/**
 * The lyrics view (roadmap D164): the sounding line large in the middle, the lines around it faint; a tap on a line plays
 * on from its start. In the silence between two lines the one that ended stays, faint too. The view moves into sight
 * when the user turns it on ([shown] grows) or the sound starts.
 * zh-CN: 歌词式视图 (路线图 D164): 正在发声的行大字居中, 上下两行淡显; 点击某行从其开头继续播放. 两行之间的空白里刚结束的行留在中间, 同样淡显. 用户打开视图
 * ([shown] 增加) 或开始播放时, 视图移入可见区域.
 */
@Composable
internal fun Lyrics(cues: PlaybackCues, playback: FilePlayback, follow: PlaybackFollow, shown: Int, speakers: FileAnalysis.Speakers?, label: @Composable (PlaybackCue) -> String) {
    val line by remember(cues) { derivedStateOf { cues.lyric(playback.positionMillis) } }
    val sounding by remember(cues) { derivedStateOf { cues.at(playback.positionMillis) == line } }
    val middle = line ?: return
    val accent = middle.speaker?.let(::speakerColor) ?: MaterialTheme.colorScheme.primary
    val requester = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val playing = playback.playing
    LaunchedEffect(shown, playing, follow.serial) {
        if ((shown > 0 || playing) && follow.following) requester.bringIntoView(Rect(0f, -follow.barHeight.toFloat(), size.width.toFloat(), size.height.toFloat()))
    }
    Column(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester).onSizeChanged { size = it }.testTag("macaw.tryout.file.lyrics"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LyricSide(cues.before(middle), playback, Alignment.BottomCenter, "macaw.tryout.file.lyrics.previous", label)
        if (middle.speaker != null && middle.text.isNotEmpty()) {
            Box(modifier = Modifier.padding(top = 12.dp)) { SpeakerLabel(middle.speaker, speakers) }
        }
        Text(
            label(middle),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            color = if (sounding) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = stringResource(R.string.tryout_file_player_play)) { playback.jump(middle.startMillis) }
                .then(if (sounding) Modifier.semantics { selected = true } else Modifier)
                .testTag("macaw.tryout.file.lyrics.current"),
        )
        LyricSide(cues.after(middle), playback, Alignment.TopCenter, "macaw.tryout.file.lyrics.next", label)
    }
}

@Composable
private fun LyricSide(cue: PlaybackCue?, playback: FilePlayback, alignment: Alignment, tag: String, label: @Composable (PlaybackCue) -> String) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = LYRIC_SIDE_HEIGHT), contentAlignment = alignment) {
        if (cue != null) {
            Text(
                label(cue),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SIDE_ALPHA),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = stringResource(R.string.tryout_file_player_play)) { playback.jump(cue.startMillis) }.padding(8.dp).testTag(tag),
            )
        }
    }
}

private val LYRIC_SIDE_HEIGHT = 72.dp
