package com.enjiws.music

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0D120F)
private val SurfaceDark = Color(0xFF17221B)
private val SurfaceSoft = Color(0xFF202D24)
private val Green = Color(0xFF91D6A0)
private val White = Color(0xFFF1F5F1)
private val Muted = Color(0xFF9AA99D)
private const val IDLE_TITLE = "Pilih lagu untuk memulai"

private data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumId: Long,
    val uri: Uri
)

private enum class Tab { Library, Online }

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
                EnjiwsRoot(controllerProvider = { mediaController })
            }
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
private fun EnjiwsRoot(controllerProvider: () -> MediaController?) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var scanKey by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(Tab.Library) }
    var showPlayer by remember { mutableStateOf(false) }
    var lyricsMode by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf(IDLE_TITLE) }
    var currentArtist by remember { mutableStateOf("ENJIWS MUSIC") }
    var artworkUri by remember { mutableStateOf<String?>(null) }
    var mediaId by remember { mutableStateOf<String?>(null) }
    var hasMedia by remember { mutableStateOf(false) }
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
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
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
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawArtist = cursor.getString(artistCol)
                    result.add(Song(
                        id,
                        cursor.getString(titleCol) ?: "Judul tidak diketahui",
                        if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Artis tidak diketahui" else rawArtist,
                        cursor.getLong(durationCol),
                        cursor.getLong(albumCol),
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
                val md = player.mediaMetadata
                currentTitle = md.title?.toString() ?: IDLE_TITLE
                currentArtist = md.artist?.toString() ?: "ENJIWS MUSIC"
                artworkUri = md.artworkUri?.toString()
                mediaId = player.currentMediaItem?.mediaId
                hasMedia = player.mediaItemCount > 0
                isPlaying = player.isPlaying
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.takeIf { it > 0L } ?: 0L
                shuffle = player.shuffleModeEnabled
                repeatMode = player.repeatMode
            }
            delay(300)
        }
    }

    val art by rememberArtwork(artworkUri, mediaId)

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }

    fun togglePlayback() {
        controllerProvider()?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                when (tab) {
                    Tab.Library -> LibraryScreen(
                        songs = songs,
                        filtered = filtered,
                        query = query,
                        onQuery = { query = it },
                        onScan = { scanKey++ },
                        onPlay = { index ->
                            val player = controllerProvider()
                            if (player != null) {
                                val queue = filtered.map { s ->
                                    MediaItem.Builder().setMediaId(s.id.toString()).setUri(s.uri)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(s.title)
                                                .setArtist(s.artist)
                                                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/${s.albumId}"))
                                                .build()
                                        ).build()
                                }
                                player.setMediaItems(queue, index, 0L)
                                player.prepare()
                                player.play()
                                showPlayer = true
                            }
                        }
                    )
                    Tab.Online -> OnlineMusicScreen(controllerProvider = controllerProvider)
                }
            }
            if (hasMedia) {
                MiniPlayer(
                    title = currentTitle,
                    artist = currentArtist,
                    art = art,
                    playing = isPlaying,
                    position = position,
                    duration = duration,
                    onOpen = { showPlayer = true },
                    onPlayPause = { togglePlayback() },
                    onNext = { controllerProvider()?.seekToNextMediaItem() }
                )
            }
            BottomBar(tab = tab, onTab = { tab = it })
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(250)) { it }
        ) {
            NowPlaying(
                title = currentTitle,
                artist = currentArtist,
                art = art,
                playing = isPlaying,
                position = position,
                duration = duration,
                shuffle = shuffle,
                repeatMode = repeatMode,
                lyricsMode = lyricsMode,
                onLyricsMode = { lyricsMode = it },
                onBack = { showPlayer = false },
                onPlayPause = { togglePlayback() },
                onPrevious = { controllerProvider()?.seekToPreviousMediaItem() },
                onNext = { controllerProvider()?.seekToNextMediaItem() },
                onSeek = { controllerProvider()?.seekTo(it) },
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
        }
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    filtered: List<Song>,
    query: String,
    onQuery: (String) -> Unit,
    onScan: () -> Unit,
    onPlay: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ENJIWS  /  SOUND", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                Text("Your library.", color = White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                Text("Ruang kecil untuk musikmu.", color = Muted, fontSize = 13.sp)
            }
            TextButton(onClick = onScan) { Text("PINDAI", color = Green, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(22.dp))
        Surface(color = SurfaceDark, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = Green, fontSize = 23.sp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Cari judul atau artis", color = Muted, fontSize = 14.sp)
                    BasicTextField(
                        value = query, onValueChange = onQuery, singleLine = true,
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
                Text(
                    if (songs.isEmpty()) "Belum ada musik ditemukan.\nTekan PINDAI untuk mencoba lagi." else "Tidak ada hasil pencarian.",
                    color = Muted
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlay(index) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteArtThumb(
                            url = "content://media/external/audio/albumart/${song.albumId}",
                            size = 46.dp,
                            mediaId = song.id.toString()
                        )
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
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    artist: String,
    art: Bitmap?,
    playing: Boolean,
    position: Long,
    duration: Long,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).clickable { onOpen() }
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtThumb(art, 42.dp, 10.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = White, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(artist, color = Muted, maxLines = 1, fontSize = 10.sp)
                }
                IconButton(onClick = onPlayPause) {
                    Text(if (playing) "Ⅱ" else "▶", color = Green, fontSize = 18.sp)
                }
                IconButton(onClick = onNext) { Text("»", color = Green, fontSize = 22.sp) }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                color = Green,
                trackColor = SurfaceSoft
            )
        }
    }
}

@Composable
private fun BottomBar(tab: Tab, onTab: (Tab) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Green, selectedTextColor = Green,
        unselectedIconColor = Muted, unselectedTextColor = Muted,
        indicatorColor = SurfaceSoft
    )
    NavigationBar(containerColor = Color(0xFF0A0F0C), contentColor = White) {
        NavigationBarItem(
            selected = tab == Tab.Library, onClick = { onTab(Tab.Library) },
            icon = { Text("♫", fontSize = 20.sp) },
            label = { Text("Koleksi", fontSize = 11.sp) },
            colors = colors
        )
        NavigationBarItem(
            selected = tab == Tab.Online, onClick = { onTab(Tab.Online) },
            icon = { Text("⌕", fontSize = 22.sp) },
            label = { Text("Online", fontSize = 11.sp) },
            colors = colors
        )
    }
}

@Composable
private fun NowPlaying(
    title: String,
    artist: String,
    art: Bitmap?,
    playing: Boolean,
    position: Long,
    duration: Long,
    shuffle: Boolean,
    repeatMode: Int,
    lyricsMode: Boolean,
    onLyricsMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(position, dragging) {
        if (!dragging) sliderValue = position.toFloat()
    }

    var lyrics by remember { mutableStateOf<LyricsResult?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(title, artist, lyricsMode) {
        if (!lyricsMode || title == IDLE_TITLE) return@LaunchedEffect
        lyricsLoading = true
        lyrics = null
        val res = try {
            LyricsRepo.get(title, artist, duration)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LyricsResult(emptyList(), "")
        }
        lyrics = res
        lyricsLoading = false
    }

    val tiny = remember(art) { art?.let { tinyBlur(it) } }

    Box(
        Modifier.fillMaxSize().background(Bg)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Background dinamis: cover diperkecil lalu di-upscale halus = efek blur.
        Crossfade(targetState = tiny, animationSpec = tween(600), label = "bg") { t ->
            if (t != null) {
                Image(
                    bitmap = t.asImageBitmap(), contentDescription = null,
                    contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B3022), Bg))))
            }
        }
        // Scrim gelap supaya teks selalu kontras di atas warna cover apa pun.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.62f), Color.Black.copy(alpha = 0.88f)))
            )
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("⌄  KEMBALI", color = Green, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                ModeChip("COVER", !lyricsMode) { onLyricsMode(false) }
                Spacer(Modifier.width(8.dp))
                ModeChip("LIRIK", lyricsMode) { onLyricsMode(true) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = lyricsMode, animationSpec = tween(250), label = "mode") { showLyrics ->
                    if (showLyrics) {
                        LyricsView(
                            result = lyrics,
                            loading = lyricsLoading,
                            positionMs = position,
                            onSeek = onSeek,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BoxWithConstraints(Modifier.fillMaxSize().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            val side = minOf(maxWidth, maxHeight)
                            Box(
                                Modifier.size(side).clip(RoundedCornerShape(28.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF314D38), Color(0xFF17251B), Color(0xFF263B2C)))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (art != null) {
                                    Image(
                                        bitmap = art.asImageBitmap(), contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("e", color = Green.copy(alpha = 0.9f), fontSize = 150.sp, fontWeight = FontWeight.Light)
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(5.dp))
                Text(artist, color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, maxLines = 1)
            }
            Spacer(Modifier.height(14.dp))
            Slider(
                value = sliderValue.coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                onValueChange = { sliderValue = it; dragging = true },
                onValueChangeFinished = { onSeek(sliderValue.toLong()); dragging = false },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = Green, activeTrackColor = Green, inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(position), color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                Text(formatTime(duration), color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onShuffle) { Text(if (shuffle) "SHUFFLE ON" else "SHUFFLE", color = if (shuffle) Green else Color.White.copy(alpha = 0.7f), fontSize = 10.sp) }
                TextButton(onClick = onPrevious) { Text("|◀", color = Color.White, fontSize = 23.sp) }
                Surface(color = Green, shape = CircleShape, modifier = Modifier.size(68.dp).clickable { onPlayPause() }) {
                    Box(contentAlignment = Alignment.Center) { Text(if (playing) "Ⅱ" else "▶", color = Bg, fontSize = 25.sp) }
                }
                TextButton(onClick = onNext) { Text("▶|", color = Color.White, fontSize = 23.sp) }
                TextButton(onClick = onRepeat) {
                    Text(when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> "REPEAT 1"
                        Player.REPEAT_MODE_ALL -> "REPEAT ALL"
                        else -> "REPEAT"
                    }, color = if (repeatMode != Player.REPEAT_MODE_OFF) Green else Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Green else Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = if (selected) Bg else Color.White,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val seconds = (ms / 1000).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
