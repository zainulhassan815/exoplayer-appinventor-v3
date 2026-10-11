package dev.dreamerslab.media3

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler

class DownloadsService : DownloadService(NOTIFICATION_ID, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL) {

    private val notifications by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
        super.onCreate()
    }

    override fun getDownloadManager(): DownloadManager =
        OfflineStorage.downloadManager(this, DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        notifications.buildProgressNotification(
            this,
            R.drawable.stat_sys_download,
            null,
            null,
            downloads,
            notMetRequirements
        )

    private companion object {
        const val NOTIFICATION_ID = 0x4D33
        const val CHANNEL_ID = "media3_downloads"
    }
}
