package com.enjiws.music

import android.net.Uri\nimport android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class OnlineTrack(
    val title: String,
    val artist: String,
    val audioUrl: String,
    val durationSeconds: Int,
    val source: String,
    val storeUrl: String = ""
)

private data class LyricLine(val timeMs: Long, val text: String)

@Composable
fun OnlineMusicScreen(
    controllerProvider: () -> MediaController?,
    currentTitle: String,
    currentArtist: String,
    duration: Long,
    position: Long,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var catalogQuery by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("415f93ab") }
    var streamUrl by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var plainLyrics by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Cari katalog global: Apple Music, Jamendo, dan Audius. Putar sumber yang menyediakan stream.") }
    var lyricsStatus by remember { mutableStateOf("Lirik belum dicari.") }
    var busy by remember { mutableStateOf(false) }
    var lyricsBusy by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val activeLine = remember(lyrics, position) {
        lyrics.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
    }

    fun playUrl(url: String, title: String, artist: String) {
        val player = controllerProvider()
        if (player == null) {
            status = "Pemutar belum siap. Coba lagi sebentar."
            return
        }
        try {
            val item = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title.ifBlank { "Online audio" })
                        .setArtist(artist.ifBlank { "ENJIWS ONLINE" })
                        .build()
                ).build()
            player.setMediaItem(item)
            player.prepare()
            player.play()
            status = "Memutar: $title"
        } catch (e: Exception) {
            status = "URL tidak dapat diputar: ${e.message ?: "format tidak didukung"}"
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0D120F)).padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹ KEMBALI", color = Color(0xFF91D6A0)) }
            Spacer(Modifier.weight(1f))
            Text("ENJIWS / ONLINE", color = Color(0xFF91D6A0), fontSize = 11.sp, letterSpacing = 1.8.sp)
        }
        Text("Music beyond.", color = Color(0xFFF1F5F1), fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
        Text("Streaming dan lirik, dalam satu ruang.", color = Color(0xFF9AA99D), fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            item {
                Text("STREAM DARI URL", color = Color(0xFF91D6A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
                InputPanel(value = streamUrl, onValueChange = { streamUrl = it }, hint = "https://.../audio.mp3")
                Spacer(Modifier.height(7.dp))
                Button(
                    onClick = {
                        val clean = streamUrl.trim()
                        if (clean.startsWith("https://") || clean.startsWith("http://")) {
                            playUrl(clean, "Online stream", "URL AUDIO")
                        } else status = "Masukkan URL audio yang diawali http:// atau https://."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF91D6A0)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("PUTAR URL", color = Color(0xFF0D120F), fontWeight = FontWeight.Bold) }
                Text("Gunakan direct link audio yang dapat diakses publik (MP3/AAC atau stream yang didukung).", color = Color(0xFF9AA99D), fontSize = 10.sp)
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF253129))
                Spacer(Modifier.height(16.dp))
                Text("GLOBAL MUSIC DISCOVERY", color = Color(0xFF91D6A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(7.dp))
                InputPanel(value = catalogQuery, onValueChange = { catalogQuery = it }, hint = "Cari judul, artis, atau genre")
                Spacer(Modifier.height(7.dp))
                InputPanel(value = clientId, onValueChange = { clientId = it }, hint = "Jamendo client_id")
                Text("Apple Music untuk penemuan lagu mainstream; Jamendo + Audius untuk stream yang tersedia. ID Jamendo bisa diganti.", color = Color(0xFF9AA99D), fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Button(
                    enabled = !busy && catalogQuery.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Mencari Apple Music, Jamendo, dan Audius..."
                            try {
                                val appleResult = runCatching { searchAppleCatalog(catalogQuery.trim()) }
                                val jamendoResult = runCatching { if (clientId.isBlank()) emptyList() else searchJamendo(catalogQuery.trim(), clientId.trim()) }
                                val audiusResult = runCatching { searchAudius(catalogQuery.trim()) }
                                tracks = (appleResult.getOrDefault(emptyList()) + jamendoResult.getOrDefault(emptyList()) + audiusResult.getOrDefault(emptyList()))
                                    .distinctBy { it.title.lowercase() + ":" + it.artist.lowercase() + ":" + it.source }
                                status = when {
                                    tracks.isNotEmpty() -> "Ditemukan ${tracks.size} hasil. Apple Music membuka halaman lagu; Jamendo/Audius dapat diputar jika stream tersedia."
                                    appleResult.isFailure && jamendoResult.isFailure && audiusResult.isFailure -> "Semua katalog gagal diakses. Periksa koneksi internet."
                                    else -> "Belum ada hasil. Coba kata kunci lain."
                                }
                            } finally { busy = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20382A)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "MENCARI..." else "CARI DI SEMUA KATALOG", color = Color(0xFF91D6A0)) }
                Text(status, color = Color(0xFF9AA99D), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(tracks) { track ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        if (track.audioUrl.isNotBlank()) {
                            playUrl(track.audioUrl, track.title, track.artist)
                        } else if (track.storeUrl.startsWith("https://")) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.storeUrl)))
                            }.onFailure {
                                status = "Tidak dapat membuka halaman lagu."
                            }
                        }
                    }.padding(vertical = 10.dp)
                ) {
                    Text(track.title, color = Color(0xFFF1F5F1), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${track.artist}  ·  ${if (track.durationSeconds > 0) "${track.durationSeconds / 60}:${(track.durationSeconds % 60).toString().padStart(2, '0')}  ·  " else ""}${track.source}${if (track.audioUrl.isBlank()) " · BUKA LAGU" else " · PUTAR"}",
                        color = Color(0xFF9AA99D), fontSize = 11.sp
                    )
                }
                HorizontalDivider(color = Color(0xFF253129))
            }
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF253129))
                Spacer(Modifier.height(16.dp))
                Text("LIRIK ONLINE / LRC", color = Color(0xFF91D6A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("Lagu sekarang: $currentTitle", color = Color(0xFFF1F5F1), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(currentArtist, color = Color(0xFF9AA99D), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !lyricsBusy && currentTitle != "Pilih lagu untuk memulai",
                    onClick = {
                        scope.launch {
                            lyricsBusy = true
                            showLyrics = true
                            lyricsStatus = "Mencari lirik..."
                            lyrics = emptyList()
                            plainLyrics = ""
                            try {
                                val result = findLyrics(currentTitle, currentArtist, duration)
                                lyrics = result.first
                                plainLyrics = result.second
                                lyricsStatus = when {
                                    lyrics.isNotEmpty() -> "Lirik tersinkronisasi ditemukan."
                                    plainLyrics.isNotBlank() -> "Lirik ditemukan, tetapi tidak memiliki timestamp."
                                    else -> "Lirik belum tersedia untuk lagu ini."
                                }
                            } catch (e: Exception) {
                                lyricsStatus = "Pencarian lirik gagal: ${e.message ?: "periksa koneksi"}"
                            } finally { lyricsBusy = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20382A)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (lyricsBusy) "MENCARI LIRIK..." else "CARI LIRIK LAGU SEKARANG", color = Color(0xFF91D6A0)) }
                Text(lyricsStatus, color = Color(0xFF9AA99D), fontSize = 11.sp)
                if (showLyrics && lyrics.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    lyrics.forEachIndexed { index, line ->
                        Text(
                            line.text.ifBlank { "♪" },
                            color = if (index == activeLine) Color(0xFF91D6A0) else Color(0xFF9AA99D),
                            fontSize = if (index == activeLine) 17.sp else 14.sp,
                            fontWeight = if (index == activeLine) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        )
                    }
                } else if (showLyrics && plainLyrics.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(plainLyrics, color = Color(0xFFF1F5F1), fontSize = 14.sp, lineHeight = 22.sp)
                }
                Text("Lirik disediakan oleh LRCLIB. Ketersediaan dan kecocokan bergantung pada katalog mereka.", color = Color(0xFF9AA99D), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InputPanel(value: String, onValueChange: (String) -> Unit, hint: String) {
    Surface(color = Color(0xFF17221B), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.padding(horizontal = 13.dp, vertical = 14.dp)) {
            if (value.isEmpty()) Text(hint, color = Color(0xFF718176), fontSize = 13.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFF1F5F1), fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private suspend fun searchAppleCatalog(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    // Search several storefronts to widen regional discovery.
    val storefronts = listOf("us", "id", "gb", "jp", "kr")
    val collected = mutableListOf<OnlineTrack>()
    for (country in storefronts) {
        val endpoint = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=15&country=$country"
        val json = runCatching { httpGet(endpoint) }.getOrNull() ?: continue
        val results = json.optJSONArray("results") ?: continue
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val title = item.optString("trackName")
            val artist = item.optString("artistName")
            val storeUrl = item.optString("trackViewUrl")
            if (title.isNotBlank() && artist.isNotBlank() && storeUrl.startsWith("https://")) {
                collected.add(
                    OnlineTrack(
                        title = title,
                        artist = artist,
                        audioUrl = "",
                        durationSeconds = item.optLong("trackTimeMillis", 0L).div(1000L).toInt(),
                        source = "Apple Music",
                        storeUrl = storeUrl
                    )
                )
            }
        }
    }
    collected.distinctBy { it.title.lowercase() + ":" + it.artist.lowercase() }
}

private suspend fun searchJamendo(query: String, clientId: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val encodedId = URLEncoder.encode(clientId, "UTF-8")
    val endpoint = "https://api.jamendo.com/v3.0/tracks/?client_id=$encodedId&format=json&limit=20&audioformat=mp32&search=$encodedQuery"
    val json = httpGet(endpoint)
    val results = json.optJSONArray("results") ?: return@withContext emptyList()
    buildList {
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val audio = item.optString("audio")
            if (audio.startsWith("http")) add(
                OnlineTrack(
                    title = item.optString("name", "Unknown title"),
                    artist = item.optString("artist_name", "Unknown artist"),
                    audioUrl = audio,
                    durationSeconds = item.optInt("duration", 0),
                    source = "Jamendo"
                )
            )
        }
    }
}

private suspend fun searchAudius(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val endpoint = "https://api.audius.co/v1/tracks/search?query=$encodedQuery&limit=20&sort_method=relevant"
    val json = httpGet(endpoint)
    val results = json.optJSONArray("data") ?: return@withContext emptyList()
    buildList {
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank() || id == "null" || item.optBoolean("is_stream_gated", false)) continue
            val user = item.optJSONObject("user")
            val title = item.optString("title", "Unknown title")
            val artist = user?.optString("name", "Unknown artist") ?: "Unknown artist"
            add(
                OnlineTrack(
                    title = title,
                    artist = artist,
                    audioUrl = "https://api.audius.co/v1/tracks/$id/stream",
                    durationSeconds = item.optInt("duration", 0),
                    source = "Audius"
                )
            )
        }
    }
}

private suspend fun findLyrics(title: String, artist: String, durationMs: Long): Pair<List<LyricLine>, String> = withContext(Dispatchers.IO) {
    val params = "track_name=${URLEncoder.encode(title, "UTF-8")}&artist_name=${URLEncoder.encode(artist, "UTF-8")}" +
        if (durationMs > 0L) "&duration=${durationMs / 1000L}" else ""
    val endpoint = "https://lrclib.net/api/get?$params"
    val json = httpGet(endpoint)
    val synced = json.optString("syncedLyrics").takeUnless { it == "null" } ?: ""
    val plain = json.optString("plainLyrics").takeUnless { it == "null" } ?: ""
    val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)""")
    val lines = synced.lines().mapNotNull { raw ->
        val match = regex.matchEntire(raw.trim()) ?: return@mapNotNull null
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        val text = match.groupValues[4]
        LyricLine((minutes * 60_000L) + (seconds * 1_000L) + fraction, text)
    }.sortedBy { it.timeMs }
    Pair(lines, plain)
}

private fun httpGet(address: String): JSONObject {
    val connection = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12000
        readTimeout = 15000
        setRequestProperty("User-Agent", "EnjiwsMusic/1.6 (https://github.com/enjiws/Enjiws-music)")
        setRequestProperty("Accept", "application/json")
    }
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        return JSONObject(body)
    } finally {
        connection.disconnect()
    }
}
