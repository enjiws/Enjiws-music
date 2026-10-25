package com.enjiws.music

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val Background = Color(0xFF080B12)
private val Panel = Color(0xFF141A27)
private val Accent = Color(0xFFB7F36B)
private val Muted = Color(0xFF9AA4B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val player = ExoPlayer.Builder(this).build()
        setContent {
            var selectedUri by remember { mutableStateOf<Uri?>(null) }
            var trackName by remember { mutableStateOf("Pilih lagu dari perangkat") }
            var isPlaying by remember { mutableStateOf(false) }
            val picker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) { }
                    selectedUri = uri
                    trackName = uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.substringAfterLast(':')
                        ?.ifBlank { "Lagu pilihanmu" }
                        ?: "Lagu pilihanmu"
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.play()
                    isPlaying = true
                }
            }

            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                }
                player.addListener(listener)
                onDispose { player.removeListener(listener) }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF18243A), Background, Background)
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ENJIWS", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                Text("MUSIC", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Panel, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("♫", color = Accent, fontSize = 25.sp)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Your sound. Your space.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text("Pemutar musik lokal dengan tampilan gelap.", color = Muted, fontSize = 14.sp)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .background(Panel, RoundedCornerShape(26.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF314565), Color(0xFF182033), Color(0xFF27351E))
                                        ),
                                        RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("♫", color = Accent, fontSize = 100.sp)
                            }
                            Text(
                                text = trackName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (selectedUri == null) "Belum ada lagu diputar" else "Musik lokal",
                                color = Muted,
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { picker.launch(arrayOf("audio/*")) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10150A))
                                ) {
                                    Text("Pilih lagu", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        if (selectedUri != null) {
                                            if (player.isPlaying) player.pause() else player.play()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedUri != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3548), contentColor = Color.White)
                                ) {
                                    Text(if (isPlaying) "Jeda Ⅱ" else "Putar ▶", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Panel, RoundedCornerShape(20.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("LIBRARY", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("Musik dari perangkatmu", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Pilih file audio seperti MP3, FLAC, atau WAV lewat tombol di atas.", color = Muted, fontSize = 13.sp)
                        }

                        Text(
                            "ENJIWS MUSIC  •  VERSION 1.0",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color(0xFF626D80),
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}
