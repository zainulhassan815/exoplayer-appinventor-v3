package dev.dreamerslab.media3.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle

internal class ThumbnailSprite(val uri: Uri, private val columns: Int, private val rows: Int, private val intervalMs: Long) {

    fun toExtras(extras: Bundle?): Bundle = Bundle(extras ?: Bundle()).apply {
        putString(KEY_URI, uri.toString())
        putInt(KEY_COLUMNS, columns)
        putInt(KEY_ROWS, rows)
        putLong(KEY_INTERVAL, intervalMs)
    }

    fun tile(sheet: Bitmap, positionMs: Long): Bitmap {
        val width = sheet.width / columns
        val height = sheet.height / rows
        val index = (positionMs / intervalMs).toInt().coerceIn(0, columns * rows - 1)
        return Bitmap.createBitmap(sheet, index % columns * width, index / columns * height, width, height)
    }

    companion object {
        private const val KEY_URI = "media3player.thumbnails.uri"
        private const val KEY_COLUMNS = "media3player.thumbnails.columns"
        private const val KEY_ROWS = "media3player.thumbnails.rows"
        private const val KEY_INTERVAL = "media3player.thumbnails.interval"

        fun from(extras: Bundle?): ThumbnailSprite? {
            val uri = extras?.getString(KEY_URI) ?: return null
            return ThumbnailSprite(Uri.parse(uri), extras.getInt(KEY_COLUMNS), extras.getInt(KEY_ROWS), extras.getLong(KEY_INTERVAL))
        }
    }
}
