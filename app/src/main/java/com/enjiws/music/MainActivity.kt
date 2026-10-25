package com.enjiws.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF09090F)
private val SurfaceDark = Color(0xFF171722)
private val Accent = Color(0xFFB69CFF)
private val SecondaryText = Color(0xFF9998AB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(9, 9, 15)
        window.navigationBarColor = android.graphics.Color.rgb(9, 9, 15)

        setContent {
            EnjiwsMusicApp()
        }
    }
}

@Composable
fun EnjiwsMusicApp() {
    var selectedTab by remember { mutableStateOf("Home") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = SurfaceDark,
            primary = Accent
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF101019),
                    contentColor = Accent
                ) {
                    val tabs = listOf(
                        "Home" to "⌂",
                        "Library" to "♫",
                        "Search" to "⌕"
                    )

                    tabs.forEach { (tab, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Text(icon, fontSize = 23.sp)
                            },
                            label = {
                                Text(tab, fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = Color(0xFF29233B),
                                unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding)
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ENJIWS",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp
                        )

                        Text(
                            "M U S I C",
                            color = Accent,
                            fontSize = 10.sp,
                            letterSpacing = 5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(SurfaceDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", color = Accent, fontSize = 25.sp)
                    }
                }

                Spacer(Modifier.height(30.dp))

                Text(
                    when (selectedTab) {
                        "Library" -> "Your collection."
                        "Search" -> "Find your sound."
                        else -> "Feel every note."
                    },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    when (selectedTab) {
                        "Library" -> "Your music, your world."
                        "Search" -> "Discover something new."
                        else -> "Music for every version of you."
                    },
                    color = SecondaryText,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF49346F),
                                    Color(0xFF211D3D),
                                    Color(0xFF11111C)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            "YOUR SOUND.",
                            color = Accent,
                            fontSize = 11.sp,
                            letterSpacing = 3.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Made for\nlate nights.",
                            color = Color.White,
                            fontSize = 29.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "♫",
                        modifier = Modifier.align(Alignment.TopEnd),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 76.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        when (selectedTab) {
                            "Library" -> "YOUR LIBRARY"
                            "Search" -> "DISCOVER"
                            else -> "QUICK ACCESS"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        "ENJIWS",
                        color = Accent,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(15.dp))

                FeatureCard(
                    icon = "♫",
                    title = "Local Music",
                    subtitle = "Play music stored on your device"
                )

                Spacer(Modifier.height(10.dp))

                FeatureCard(
                    icon = "▤",
                    title = "Lyrics",
                    subtitle = "Follow every word, in sync"
                )

                Spacer(Modifier.height(10.dp))

                FeatureCard(
                    icon = "♡",
                    title = "Your Favorites",
                    subtitle = "Keep the songs you love close"
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "A DIFFERENT WAY TO FEEL MUSIC",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp),
                    color = SecondaryText,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF29233B), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Accent, fontSize = 23.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}
