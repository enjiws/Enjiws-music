package com.enjiws.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Model lirik tersinkron. Isi dari parser LRCLIB yang sudah ada di project:
// teks + timestamp (milidetik) tiap baris.
// Nama-nama di file ini sengaja diawali "Np" supaya tidak bentrok dengan
// deklarasi layar Now Playing lama yang mungkin sudah ada di package ini.
// ---------------------------------------------------------------------------
data class NpLyricLine(val text: String, val timeMs: Long)

enum class NpRepeatMode { OFF, ALL, ONE }

// ---------------------------------------------------------------------------
// Layar Now Playing — desain meniru referensi (header kaca, artwork besar,
// lirik tersinkron dengan baris aktif paling menonjol, progress bar, dan
// bar kontrol kaca dengan tombol play/pause besar di tengah).
// Semua tombol memakai glyph teks (bukan Icons.Filled) supaya file ini
// tidak butuh dependency material-icons tambahan.
// ---------------------------------------------------------------------------
@Composable
fun NowPlayingScreenNew(
    title: String,
    artist: String,
    artworkUrl: String,
    lyrics: List<NpLyricLine>,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: NpRepeatMode,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
) {
    val currentLineIndex = remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }

    Box(Modifier.fillMaxSize()) {
        NpBackground()

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Header: tombol kembali | Now Playing / Sedang Diputar | tombol ...
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassGlyphButton(onClick = onBack, glyph = "‹", fontSize = 26.sp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Now Playing", color = Color.White, fontSize = 15.sp, fontFamily = FontFamily.Serif)
                    Text("Sedang Diputar", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                GlassGlyphButton(onClick = onMore, glyph = "•••", fontSize = 16.sp)
            }

            Spacer(Modifier.height(28.dp))

            // Artwork besar dengan sudut membulat
            Box(
                Modifier.size(280.dp)
                    .shadow(28.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (artworkUrl.isNotBlank()) {
                    RemoteArtThumb(artworkUrl, 280.dp)
                } else {
                    Text("♪", color = Color.White.copy(alpha = 0.5f), fontSize = 64.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            // Judul + artis
            Text(
                title.ifBlank { "Unknown title" },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                artist.ifBlank { "Unknown artist" },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(18.dp))

            // Lirik tersinkron: 5 baris di sekitar baris aktif, baris aktif
            // paling besar dan terang, baris lain meredup sesuai jaraknya.
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (lyrics.isEmpty()) {
                    Text("Lirik tidak tersedia", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                } else {
                    val start = (currentLineIndex - 2).coerceAtLeast(0)
                    val end = (start + 5).coerceAtMost(lyrics.size)
                    for (i in start until end) {
                        val distance = kotlin.math.abs(i - currentLineIndex)
                        val isCurrent = i == currentLineIndex
                        Text(
                            text = lyrics[i].text,
                            color = if (isCurrent) Color.White
                                else Color.White.copy(alpha = if (distance == 1) 0.55f else 0.35f),
                            fontSize = if (isCurrent) 26.sp else if (distance == 1) 18.sp else 16.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Waktu + progress bar
            Text(
                "${formatTimeNp(positionMs)} / ${formatTimeNp(durationMs)}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF9B8CFF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            // Bar kontrol kaca: shuffle | prev | play besar | next | repeat
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlGlyphButton(
                        onClick = onShuffleToggle,
                        glyph = "⇄",
                        fontSize = 22.sp,
                        tint = if (isShuffle) Color.White else Color.White.copy(alpha = 0.45f)
                    )
                    ControlGlyphButton(onClick = onPrev, glyph = "⏮", fontSize = 26.sp, tint = Color.White)
                    Surface(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (isPlaying) "❚❚" else "▶",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    ControlGlyphButton(onClick = onNext, glyph = "⏭", fontSize = 26.sp, tint = Color.White)
                    ControlGlyphButton(
                        onClick = onRepeatToggle,
                        glyph = if (repeatMode == NpRepeatMode.ONE) "↻1" else "↻",
                        fontSize = 22.sp,
                        tint = if (repeatMode == NpRepeatMode.OFF) Color.White.copy(alpha = 0.45f) else Color.White
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Latar gradasi ungu-biru-pink seperti referensi (cahaya nebula lembut di
// atas dasar gelap). Ubah warna/posisinya di sini kalau mau nuansa lain.
// ---------------------------------------------------------------------------
@Composable
private fun NpBackground() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0618))) {
        Box(
            Modifier.size(360.dp).offset((-80).dp, (-60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF7C3AED).copy(alpha = 0.55f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier.size(340.dp).offset(180.dp, 120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFDB2777).copy(alpha = 0.45f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier.size(300.dp).offset((-100).dp, 420.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2563EB).copy(alpha = 0.45f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier.size(260.dp).offset(200.dp, 560.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF9333EA).copy(alpha = 0.40f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
private fun GlassGlyphButton(onClick: () -> Unit, glyph: String, fontSize: TextUnit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(glyph, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControlGlyphButton(onClick: () -> Unit, glyph: String, fontSize: TextUnit, tint: Color) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(glyph, color = tint, fontSize = fontSize, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTimeNp(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

// ---------------------------------------------------------------------------
// Pembungkus siap pakai: tinggal panggil dari layar Now Playing yang sudah
// ada, dengan controllerProvider yang sama seperti OnlineMusicScreen dan
// daftar lirik dari parser LRCLIB. Posisi, status putar, shuffle, dan repeat
// dibaca dari MediaController tiap 500 ms.
// ---------------------------------------------------------------------------
@Composable
fun NowPlayingScreenFromControllerNew(
    controllerProvider: () -> MediaController?,
    lyrics: List<NpLyricLine>,
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(NpRepeatMode.OFF) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var artworkUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val c = controllerProvider()
            if (c != null) {
                positionMs = c.currentPosition
                durationMs = c.duration.coerceAtLeast(0)
                isPlaying = c.isPlaying
                isShuffle = c.shuffleModeEnabled
                repeatMode = when (c.repeatMode) {
                    Player.REPEAT_MODE_ALL -> NpRepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> NpRepeatMode.ONE
                    else -> NpRepeatMode.OFF
                }
                val meta = c.mediaMetadata
                title = meta.title?.toString().orEmpty()
                artist = meta.artist?.toString().orEmpty()
                artworkUrl = meta.artworkUri?.toString().orEmpty()
            }
            delay(500)
        }
    }

    NowPlayingScreenNew(
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        lyrics = lyrics,
        positionMs = positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        onBack = onBack,
        onMore = onMore,
        onPlayPause = {
            val c = controllerProvider()
            if (c != null) {
                if (c.isPlaying) c.pause() else c.play()
            }
        },
        onNext = { controllerProvider()?.seekToNext() },
        onPrev = { controllerProvider()?.seekToPrevious() },
        onSeek = { controllerProvider()?.seekTo(it) },
        onShuffleToggle = {
            val c = controllerProvider()
            if (c != null) c.shuffleModeEnabled = !c.shuffleModeEnabled
        },
        onRepeatToggle = {
            val c = controllerProvider()
            if (c != null) {
                c.repeatMode = when (c.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0618)
@Composable
private fun NowPlayingScreenNewPreview() {
    NowPlayingScreenNew(
        title = "Bintang di Surga",
        artist = "Noah",
        artworkUrl = "",
        lyrics = listOf(
            NpLyricLine("Di bawah langit malam", 60_000),
            NpLyricLine("Bintang-bintang bersinar", 70_000),
            NpLyricLine("Malam ini terasa indah", 80_000),
            NpLyricLine("Kau dan aku bersama", 90_000),
            NpLyricLine("Selamanya", 100_000)
        ),
        positionMs = 84_000,
        durationMs = 225_000,
        isPlaying = true,
        isShuffle = false,
        repeatMode = NpRepeatMode.OFF,
        onBack = {},
        onMore = {},
        onPlayPause = {},
        onNext = {},
        onPrev = {},
        onSeek = {},
        onShuffleToggle = {},
        onRepeatToggle = {}
    )
}
