package com.enjiws.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LrcLine(val timeMs: Long, val text: String)

data class LyricsResult(val synced: List<LrcLine>, val plain: String) {
    fun hasContent() = synced.isNotEmpty() || plain.isNotBlank()
}

object LyricsRepo {
    private val cache = HashMap<String, LyricsResult>()
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    suspend fun get(title: String, artist: String, durationMs: Long): LyricsResult = withContext(Dispatchers.IO) {
        val t = cleanTitle(title)
        val a = cleanArtist(artist)
        val key = "$t|$a".lowercase()
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        var result = LyricsResult(emptyList(), "")
        val dur = if (durationMs > 0) "&duration=${durationMs / 1000}" else ""
        val base = "https://lrclib.net/api/get?track_name=${enc(t)}&artist_name=${enc(a)}"
        for (u in listOf(base + dur, base).distinct()) {
            val body = runCatching { httpText(u) }.getOrNull() ?: continue
            val r = runCatching { fromObject(JSONObject(body)) }.getOrNull()
            if (r != null && r.hasContent()) { result = r; break }
        }
        if (!result.hasContent()) {
            val body = runCatching { httpText("https://lrclib.net/api/search?q=${enc("$t $a")}") }.getOrNull()
            val arr = body?.let { runCatching { JSONArray(it) }.getOrNull() }
            if (arr != null) {
                var best: LyricsResult? = null
                for (i in 0 until minOf(arr.length(), 8)) {
                    val r = fromObject(arr.optJSONObject(i) ?: continue)
                    if (r.synced.isNotEmpty()) { best = r; break }
                    if (best == null && r.plain.isNotBlank()) best = r
                }
                if (best != null) result = best
            }
        }
        if (result.hasContent()) synchronized(cache) { cache[key] = result }
        result
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun JSONObject.str(k: String): String = optString(k).let { if (it == "null") "" else it }

    private fun fromObject(o: JSONObject) = LyricsResult(parseLrc(o.str("syncedLyrics")), o.str("plainLyrics"))

    private fun parseLrc(raw: String): List<LrcLine> {
        val out = ArrayList<LrcLine>()
        for (line in raw.lineSequence()) {
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = line.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLong()
                out.add(LrcLine(min * 60_000L + sec * 1_000L + frac, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun cleanTitle(s: String): String =
        s.replace(Regex("""\s*[\(\[][^\)\]]*[\)\]]"""), "")
            .replace(Regex("""(?i)\s*(official\s+)?(music\s+)?(video|audio|lyrics?)\s*$"""), "")
            .trim().ifBlank { s }

    private fun cleanArtist(s: String): String =
        s.replace(Regex("""(?i)\s*-\s*topic$"""), "").replace(Regex("""(?i)vevo$"""), "").trim()

    private fun httpText(address: String): String? {
        val c = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("User-Agent", "EnjiwsMusic/1.8 (https://github.com/enjiws/Enjiws-music)")
        }
        return try {
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }
}

@Composable
fun LyricsView(
    result: LyricsResult?,
    loading: Boolean,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        loading || result == null -> CenterMessage("Mencari lirik…", modifier)
        result.synced.isNotEmpty() -> SyncedLyrics(result.synced, positionMs, onSeek, modifier)
        result.plain.isNotBlank() -> Box(modifier.verticalScroll(rememberScrollState())) {
            Text(
                result.plain,
                style = TextStyle(color = Color(0xFFF7FAF7), fontSize = 18.sp, lineHeight = 30.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }
        else -> CenterMessage("Lirik belum tersedia untuk lagu ini.", modifier)
    }
}

@Composable
private fun CenterMessage(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFFE3EAE4), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SyncedLyrics(lines: List<LrcLine>, positionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    val active = remember(lines, positionMs) { lines.indexOfLast { it.timeMs <= positionMs + 300L } }

    LaunchedEffect(lines) { listState.scrollToItem(0) }
    LaunchedEffect(active) {
        if (active >= 0 && !listState.isScrollInProgress) listState.animateScrollToItem(active)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 80.dp, bottom = 220.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            LyricRow(
                text = line.text.ifBlank { "♪" },
                distance = if (active < 0) 3 else abs(index - active),
                onClick = { onSeek(line.timeMs) }
            )
        }
    }
}

@Composable
private fun LyricRow(text: String, distance: Int, onClick: () -> Unit) {
    val isActive = distance == 0
    val size by animateFloatAsState(if (isActive) 26f else 20f, tween(250), label = "lyricSize")
    val alpha by animateFloatAsState(
        when (distance) { 0 -> 1f; 1 -> 0.72f; 2 -> 0.6f; else -> 0.5f }, tween(250), label = "lyricAlpha"
    )
    Text(
        text,
        style = TextStyle(
            color = Color.White.copy(alpha = alpha),
            fontSize = size.sp,
            lineHeight = (size * 1.3f).sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            shadow = Shadow(Color.Black.copy(alpha = 0.55f), blurRadius = 8f)
        ),
        modifier = Modifier.fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
