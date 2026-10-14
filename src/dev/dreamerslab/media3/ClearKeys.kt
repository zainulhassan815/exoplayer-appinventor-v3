package dev.dreamerslab.media3

import android.os.Bundle
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import org.json.JSONArray
import org.json.JSONObject

internal object ClearKeys {

    private const val KEY_LICENSE = "media3player.clearkey.license"
    private val HEX_KEY = Regex("[0-9a-fA-F]{32}")

    fun license(keys: Map<String, String>): String {
        require(keys.isNotEmpty()) { "Pass at least one key ID and key." }
        val entries = keys.map { (kid, key) ->
            JSONObject().put("kty", "oct").put("kid", base64Url(kid)).put("k", base64Url(key))
        }
        return JSONObject().put("keys", JSONArray(entries)).put("type", "temporary").toString()
    }

    fun withLicense(item: MediaItem, license: String): MediaItem {
        val extras = Bundle(item.mediaMetadata.extras ?: Bundle()).apply { putString(KEY_LICENSE, license) }
        return item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(extras).build()).build()
    }

    fun keys(item: MediaItem): Map<String, String>? {
        val license = item.mediaMetadata.extras?.getString(KEY_LICENSE) ?: return null
        val entries = JSONObject(license).getJSONArray("keys")
        return (0 until entries.length()).map(entries::getJSONObject).associate { hex(it.getString("kid")) to hex(it.getString("k")) }
    }

    fun sessionManager(item: MediaItem): DrmSessionManager? {
        val license = item.mediaMetadata.extras?.getString(KEY_LICENSE) ?: return null
        return DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(LocalMediaDrmCallback(license.toByteArray()))
    }

    private fun base64Url(hex: String): String {
        val digits = hex.replace("-", "").trim()
        require(HEX_KEY.matches(digits)) { "\"$hex\" isn't a key: use 32 hex digits." }
        val bytes = digits.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun hex(base64Url: String): String =
        Base64.decode(base64Url, Base64.URL_SAFE or Base64.NO_PADDING).joinToString("") { "%02x".format(it) }
}
