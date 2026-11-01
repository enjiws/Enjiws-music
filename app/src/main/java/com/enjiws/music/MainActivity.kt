package com.enjiws.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.enjiws.music.player.MusicPlayerManager
import com.enjiws.music.ui.PlayerScreen

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: MusicPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerManager = MusicPlayerManager(this)

        setContent {
            MaterialTheme {
                Surface {
                    PlayerScreen(
                        playerManager = playerManager,
                        currentLyric = "🎵 Lirik lagu akan tersinkron di sini..."
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
