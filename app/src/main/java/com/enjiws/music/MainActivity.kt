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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

private val Bg = Color(0xFF101410)
private val Panel = Color(0xFF1B241D)
private val Green = Color(0xFF7CDB8A)
private val White = Color(0xFFF2F5F2)
private val Muted = Color(0xFF9AA79C)

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
                background = Bg, surface = Panel, primary = Green,
                onBackground = White, onSurface = White
            )) {
                MusicLibraryScreen(
                    onPlay = { list, index -> playSongs(list, index) },
                    controllerProvider = { mediaController }
                )
            }
        }
    }

    private fun playSongs(songs: List<Song>, index: Int) {
        val controller = mediaController ?: return
        if (songs.isEmpty()) return
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build())
                .build()
        }
        controller.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0L)
        controller.prepare()
        controller.play()
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
private fun MusicLibraryScreen(
    onPlay: (List<Song>, Int) -> Unit,
    controllerProvider: () -> MediaController?
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var currentTitle by remember { mutableStateOf("Belum ada lagu diputar") }
    var playing by remember { mutableStateOf(false) }

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
                collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Judul tidak diketahui"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>")
                        "Artis tidak diketahui" else rawArtist
                    result.add(Song(
                        id, title, artist, cursor.getLong(durationCol),
                        ContentUris.withAppendedId(collection, id)
                    ))
                }
            }
        } catch (_: SecurityException) { }
        songs = result
    }

    LaunchedEffect(refreshKey) { scanLibrary() }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val c = controllerProvider()
            if (c != null) {
                currentTitle = c.mediaMetadata.title?.toString() ?: "Belum ada lagu diputar"
                playing = c.isPlaying
            }
            kotlinx.coroutines.delay(700)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ENJIWS", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Music", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { refreshKey++ }) { Text("Pindai", color = Green) }
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Cari lagu atau artis...", color = Muted)
                    inner()
                }
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Perpustakaan", color = White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("${filtered.size} lagu", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (songs.isEmpty()) "Belum ada lagu ditemukan.\nIzinkan akses audio, lalu tekan Pindai."
                    else "Lagu tidak ditemukan.",
                    color = Muted
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlay(filtered, index) }.padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("♫", color = Green, fontSize = 23.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = White, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(song.artist, color = Muted, fontSize = 12.sp, maxLines = 1)
                        }
                        Text(formatDuration(song.duration), color = Muted, fontSize = 11.sp)
                    }
                    HorizontalDivider(color = Color(0xFF29332B))
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SEDANG DIPUTAR", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(currentTitle, color = White, maxLines = 1, fontSize = 13.sp)
                }
                IconButton(onClick = {
                    controllerProvider()?.let { if (it.isPlaying) it.pause() else it.play() }
                    playing = !(controllerProvider()?.isPlaying ?: false)
                }) { Text(if (playing) "Ⅱ" else "▶", color = Green, fontSize = 20.sp) }
                IconButton(onClick = { controllerProvider()?.seekToNextMediaItem() }) {
                    Text("»", color = Green, fontSize = 24.sp)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
