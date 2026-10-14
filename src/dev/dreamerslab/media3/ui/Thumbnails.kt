package dev.dreamerslab.media3.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import java.net.URI

internal class ThumbnailCue(val startMs: Long, val image: Uri, val crop: (Bitmap) -> Bitmap)

internal sealed class ThumbnailSource {

    abstract fun toExtras(extras: Bundle?): Bundle

    class Grid(val uri: Uri, val columns: Int, val rows: Int, val intervalMs: Long) : ThumbnailSource() {

        override fun toExtras(extras: Bundle?): Bundle = Bundle(extras ?: Bundle()).apply {
            putString(KEY_GRID, uri.toString())
            putInt(KEY_COLUMNS, columns)
            putInt(KEY_ROWS, rows)
            putLong(KEY_INTERVAL, intervalMs)
        }

        fun cues(): List<ThumbnailCue> = (0 until columns * rows).map { index ->
            ThumbnailCue(index * intervalMs, uri) { sheet ->
                val width = sheet.width / columns
                val height = sheet.height / rows
                Bitmap.createBitmap(sheet, index % columns * width, index / columns * height, width, height)
            }
        }
    }

    class Track(val uri: Uri) : ThumbnailSource() {

        override fun toExtras(extras: Bundle?): Bundle = Bundle(extras ?: Bundle()).apply {
            putString(KEY_TRACK, uri.toString())
        }

        fun cues(vtt: String): List<ThumbnailCue> {
            val lines = vtt.lines().map { it.trim() }
            return lines.indices.mapNotNull { i ->
                val timing = TIMING.matchEntire(lines[i]) ?: return@mapNotNull null
                val target = lines.getOrNull(i + 1)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                cue(parseTime(timing.groupValues[1]), target)
            }
        }

        private fun cue(startMs: Long, target: String): ThumbnailCue {
            val image = Uri.parse(URI(uri.toString()).resolve(target.substringBefore('#')).toString())
            val region = REGION.find(target)?.groupValues?.drop(1)?.map(String::toInt)
                ?.let { (x, y, w, h) -> Rect(x, y, x + w, y + h) }
            return ThumbnailCue(startMs, image) { sheet ->
                region?.let { Bitmap.createBitmap(sheet, it.left, it.top, it.width(), it.height()) } ?: sheet
            }
        }

        private fun parseTime(time: String): Long {
            val parts = time.split(':')
            val seconds = parts.last().toDouble()
            val minutes = parts.dropLast(1).fold(0L) { total, part -> total * 60 + part.toLong() }
            return minutes * 60_000 + (seconds * 1000).toLong()
        }
    }

    companion object {
        private const val KEY_GRID = "media3player.thumbnails.grid"
        private const val KEY_COLUMNS = "media3player.thumbnails.columns"
        private const val KEY_ROWS = "media3player.thumbnails.rows"
        private const val KEY_INTERVAL = "media3player.thumbnails.interval"
        private const val KEY_TRACK = "media3player.thumbnails.track"

        private val TIMING = Regex("""([\d:.]+)\s+-->\s+[\d:.]+.*""")
        private val REGION = Regex("""#xywh=(\d+),(\d+),(\d+),(\d+)""")

        fun from(extras: Bundle?): ThumbnailSource? {
            extras ?: return null
            extras.getString(KEY_TRACK)?.let { return Track(Uri.parse(it)) }
            val grid = extras.getString(KEY_GRID) ?: return null
            return Grid(Uri.parse(grid), extras.getInt(KEY_COLUMNS), extras.getInt(KEY_ROWS), extras.getLong(KEY_INTERVAL))
        }
    }
}
