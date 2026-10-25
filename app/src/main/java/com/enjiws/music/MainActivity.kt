package com.enjiws.music

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val controllerState = mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectController()
        setContent {
            val controller = controllerState.value
            var isPlaying by remember { mutableStateOf(false) }
            var trackTitle by remember { mutableStateOf("Belum ada lagu dipilih") }

            DisposableEffect(controller) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        trackTitle = mediaItem?.mediaMetadata?.title?.toString()
                            ?: mediaItem?.localConfiguration?.uri?.lastPathSegment
                            ?: "Lagu dipilih"
                    }
                }
                controller?.addListener(listener)
                isPlaying = controller?.isPlaying == true
                onDispose { controller?.removeListener(listener) }
            }

            val audioPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                    }
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Lagu dipilih"
                    trackTitle = name
                    mediaController?.let { player ->
                        player.setMediaItem(
                            MediaItem.Builder()
                                .setUri(uri)
                                .setMediaMetadata(
                                    androidx.media3.common.MediaMetadata.Builder()
                                        .setTitle(name)
                                        .build()
                                )
                                .build()
                        )
                        player.prepare()
                        player.play()
                    }
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF10110F)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ENJIWS",
                            color = Color(0xFFB7F34A),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 5.sp
                        )
                        Text(
                            "MUSIC",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(32.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B1D19), RoundedCornerShape(24.dp))
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("NOW PLAYING", color = Color(0xFFB7F34A), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                trackTitle,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7F34A))
                            ) {
                                Text("Pilih Lagu", color = Color(0xFF151711), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        mediaController?.let {
                                            if (it.isPlaying) it.pause() else it.play()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30342B))
                                ) {
                                    Text(if (isPlaying) "Pause" else "Play", color = Color.White)
                                }
                                Button(
                                    onClick = { mediaController?.seekToPreviousMediaItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30342B))
                                ) {
                                    Text("⏮", color = Color.White)
                                }
                                Button(
                                    onClick = { mediaController?.seekToNextMediaItem() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30342B))
                                ) {
                                    Text("⏭", color = Color.White)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (controller != null) "Pemutaran latar belakang aktif" else "Menghubungkan pemutar…",
                                color = Color(0xFFB9BDB3),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Musik tetap berjalan saat aplikasi diminimalkan.",
                            color = Color(0xFF858A7F),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                mediaController = future.get()
                controllerState.value = mediaController
            } catch (_: Exception) {
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        super.onDestroy()
    }
}
