package com.enjiws.music.parser

data class LyricLine(val timeInMs: Long, val text: String)

object LrcParser {
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val parsedLyrics = mutableListOf<LyricLine>()
        val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()

        lrcContent.lines().forEach { line ->
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val (minutes, seconds, milliseconds, text) = matchResult.destructured
                val timeMs = (minutes.toLong() * 60000) + 
                             (seconds.toLong() * 1000) + 
                             (if (milliseconds.length == 2) milliseconds.toLong() * 10 else milliseconds.toLong())
                parsedLyrics.add(LyricLine(timeMs, text.trim()))
            }
        }
        return parsedLyrics
    }
}
