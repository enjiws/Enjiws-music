package com.enjiws.music

import android.Manifest
import android.content.ContentUris
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0D120F)
private val SurfaceDark = Color(0xFF17221B)
private val SurfaceSoft = Color(0xFF202D24)
private val Green = Color(0xFF91D6A0)
private val GreenDim = Color(0xFF426D4B)
private val White = Color(0xFFF1F5F1)
private val Muted = Color(0xFF9AA99D)

private data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: android.net.Uri
)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try { mediaController = controllerFuture?.get() } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(this))

        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Bg,
                surface = SurfaceDark,
                primary = Green,
                onBackground = White,
                onSurface = White
            )) {
                EnjiwsApp(controllerProvider = { mediaController })
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
private fun EnjiwsApp(controllerProvider: () -> MediaController?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var scanKey by remember { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf("Pilih lagu untuk memulai") }
    var currentArtist by remember { mutableStateOf("ENJIWS MUSIC") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }

    fun scanLibrary() {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        try {
            context.contentResolver.query(
                collection, projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawArtist = cursor.getString(artistCol)
                    result.add(Song(
                        id,
                        cursor.getString(titleCol) ?: "Judul tidak diketahui",
                        if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Artis tidak diketahui" else rawArtist,
                        cursor.getLong(durationCol),
                        ContentUris.withAppendedId(collection, id)
                    ))
                }
            }
        } catch (_: SecurityException) { }
        songs = result
    }

    LaunchedEffect(scanKey) { scanLibrary() }

    LaunchedEffect(Unit) {
        while (true) {
            val player = controllerProvider()
            if (player != null) {
                currentTitle = player.mediaMetadata.title?.toString() ?: "Pilih lagu untuk memulai"
                currentArtist = player.mediaMetadata.artist?.toString() ?: "ENJIWS MUSIC"
                isPlaying = player.isPlaying
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.takeIf { it > 0L } ?: 0L
                shuffle = player.shuffleModeEnabled
                repeatMode = player.repeatMode
            }
            delay(450)
        }
    }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }

    fun togglePlayback() {
        controllerProvider()?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(value: Float) {
        controllerProvider()?.seekTo(value.toLong())
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (showPlayer) {
            FullPlayer(
                title = currentTitle,
                artist = currentArtist,
                playing = isPlaying,
                position = position,
                duration = duration,
                shuffle = shuffle,
                repeatMode = repeatMode,
                onBack = { showPlayer = false },
                onPlayPause = { togglePlayback() },
                onPrevious = { controllerProvider()?.seekToPreviousMediaItem() },
                onNext = { controllerProvider()?.seekToNextMediaItem() },
                onSeek = { seekTo(it) },
                onShuffle = {
                    controllerProvider()?.let {
                        it.shuffleModeEnabled = !it.shuffleModeEnabled
                        shuffle = it.shuffleModeEnabled
                    }
                },
                onRepeat = {
                    controllerProvider()?.let {
                        it.repeatMode = when (it.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        repeatMode = it.repeatMode
                    }
                }
            )
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ENJIWS  /  SOUND", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                        Text("Your library.", color = White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ruang kecil untuk musikmu.", color = Muted, fontSize = 13.sp)
                    }
                    TextButton(onClick = { scanKey++ }) { Text("PINDAI", color = Green, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(22.dp))
                Surface(color = SurfaceDark, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", color = Green, fontSize = 23.sp)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Cari judul atau artis", color = Muted, fontSize = 14.sp)
                            BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KOLEKSI", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Spacer(Modifier.width(9.dp))
                    Text("${filtered.size} TRACKS", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (songs.isEmpty()) "Belum ada musik ditemukan.\nTekan PINDAI untuk mencoba lagi." else "Tidak ada hasil pencarian.", color = Muted)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 14.dp)) {
                        itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    val player = controllerProvider()
                                    if (player != null) {
                                        val queue = filtered.map {
                                            MediaItem.Builder().setMediaId(it.id.toString()).setUri(it.uri)
                                                .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).build())
                                                .build()
                                        }
                                        player.setMediaItems(queue, index, 0L)
                                        player.prepare()
                                        player.play()
                                        showPlayer = true
                                    }
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                                        .background(Brush.linearGradient(listOf(SurfaceSoft, GreenDim))),
                                    contentAlignment = Alignment.Center
                                ) { Text("♫", color = Green, fontSize = 23.sp) }
                                Spacer(Modifier.width(13.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Spacer(Modifier.height(3.dp))
                                    Text(song.artist, color = Muted, fontSize = 11.sp, maxLines = 1)
                                }
                                Text(formatTime(song.duration), color = Muted, fontSize = 11.sp)
                            }
                            HorizontalDivider(color = Color(0xFF253129), thickness = 0.6.dp)
                        }
                    }
                }
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showPlayer = true }
                ) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceSoft),
                                contentAlignment = Alignment.Center
                            ) { Text("♫", color = Green, fontSize = 21.sp) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(currentTitle, color = White, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(currentArtist, color = Muted, maxLines = 1, fontSize = 10.sp)
                            }
                            IconButton(onClick = { togglePlayback() }) {
                                Text(if (isPlaying) "Ⅱ" else "▶", color = Green, fontSize = 18.sp)
                            }
                            IconButton(onClick = { controllerProvider()?.seekToNextMediaItem() }) {
                                Text("»", color = Green, fontSize = 22.sp)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                            color = Green,
                            trackColor = SurfaceSoft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPlayer(
    title: String,
    artist: String,
    playing: Boolean,
    position: Long,
    duration: Long,
    shuffle: Boolean,
    repeatMode: Int,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(position, dragging) {
        if (!dragging) sliderValue = position.toFloat()
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1B3022), Bg, Bg))
        ).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("⌄  KEMBALI", color = Green, fontSize = 12.sp) }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF314D38), Color(0xFF17251B), Color(0xFF263B2C)))),
            contentAlignment = Alignment.Center
        ) {
            Text("e", color = Green.copy(alpha = 0.9f), fontSize = 150.sp, fontWeight = FontWeight.Light)
            Text("ENJIWS", color = White.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 5.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
        }
        Spacer(Modifier.height(34.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Spacer(Modifier.height(5.dp))
            Text(artist, color = Muted, fontSize = 14.sp)
        }
        Spacer(Modifier.height(22.dp))
        Slider(
            value = sliderValue.coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
            onValueChange = { sliderValue = it; dragging = true },
            onValueChangeFinished = { onSeek(sliderValue); dragging = false },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = duration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Green, activeTrackColor = Green, inactiveTrackColor = SurfaceSoft
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = Muted, fontSize = 11.sp)
            Text(formatTime(duration), color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onShuffle) { Text(if (shuffle) "SHUFFLE ON" else "SHUFFLE", color = if (shuffle) Green else Muted, fontSize = 10.sp) }
            TextButton(onClick = onPrevious) { Text("|◀", color = White, fontSize = 23.sp) }
            Surface(color = Green, shape = CircleShape, modifier = Modifier.size(68.dp).clickable { onPlayPause() }) {
                Box(contentAlignment = Alignment.Center) { Text(if (playing) "Ⅱ" else "▶", color = Bg, fontSize = 25.sp) }
            }
            TextButton(onClick = onNext) { Text("▶|", color = White, fontSize = 23.sp) }
            TextButton(onClick = onRepeat) {
                Text(when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> "REPEAT 1"
                    Player.REPEAT_MODE_ALL -> "REPEAT ALL"
                    else -> "REPEAT"
                }, color = if (repeatMode != Player.REPEAT_MODE_OFF) Green else Muted, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("MADE FOR YOUR MOMENTS", color = Muted.copy(alpha = 0.65f), fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
