package com.browxer.engine.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.browxer.core.common.NetworkMonitor
import com.browxer.core.database.DownloadDao
import com.browxer.core.database.DownloadEntity
import com.browxer.core.datastore.SettingsRepository
import com.browxer.core.domain.DownloadResult
import com.browxer.core.domain.DownloadTask
import com.browxer.core.media.MediaStoreSaver
import com.browxer.engine.muxer.FFmpegMuxer
import com.browxer.engine.resolver.BlobResolver
import com.browxer.engine.resolver.YtdlpResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

/**
 * Foreground download manager: 3-slot semaphore, Wi-Fi gate, typed honest errors.
 * Declared with foregroundServiceType="dataSync" (crashes on Android 14+ without it).
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var direct: DirectDownloader
    @Inject lateinit var hls: HlsDownloader
    @Inject lateinit var dash: DashDownloader
    @Inject lateinit var ytdlp: YtdlpResolver
    @Inject lateinit var blobResolver: BlobResolver
    @Inject lateinit var muxer: FFmpegMuxer
    @Inject lateinit var saver: MediaStoreSaver
    @Inject lateinit var dao: DownloadDao
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ConcurrentLinkedQueue<DownloadTask>()
    private val slots = Semaphore(3) // max 3 concurrent downloads
    private val jobs = mutableMapOf<Long, Job>()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildSummary("Browxer", "Download manager running"))
        scope.launch { dispatchLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                val id = intent.getLongExtra(EXTRA_ID, -1)
                jobs.remove(id)?.cancel()
                queue.removeIf { it.id == id }
                scope.launch { dao.updateStatus(id, DownloadEntity.FAILED, "Cancelled") }
            }
            else -> intent?.getParcelableExtra<DownloadTask>(EXTRA_TASK)?.let { task ->
                queue.add(task)
                scope.launch { dao.updateStatus(task.id, DownloadEntity.QUEUED) }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun dispatchLoop() {
        while (true) {
            val task = queue.poll() ?: run { delay(250); continue }
            jobs[task.id] = scope.launch { slots.withPermit { runTask(task) } }
        }
    }

    private suspend fun runTask(task: DownloadTask) = runCatching {
        if (settings.wifiOnly.first() && !NetworkMonitor.isOnUnmeteredWifi(this)) {
            return@runCatching dao.updateStatus(
                task.id, DownloadEntity.PAUSED, "Waiting for Wi-Fi")
        }
        dao.updateStatus(task.id, DownloadEntity.DOWNLOADING)

        val result: DownloadResult = when (task.kind) {
            DownloadTask.Kind.DIRECT -> direct.download(task) { p -> report(task.id, p) }
            DownloadTask.Kind.HLS -> hls.download(task) { p -> report(task.id, p) }
            DownloadTask.Kind.DASH -> dash.download(task) { p -> report(task.id, p) }
            DownloadTask.Kind.YTDLP ->
                DownloadResult(ytdlp.download(task) { p -> report(task.id, p) })
            DownloadTask.Kind.BLOB -> {
                val blobId = task.url.removePrefix("blob:")
                val out = File(task.workDir, task.fileName)
                check(blobResolver.assembleTo(blobId, out)) { "Blob payload incomplete" }
                DownloadResult(out)
            }
        }

        if (result.needsMux && result.videoFile != null && result.audioFile != null)
            muxer.merge(result.videoFile, result.audioFile, result.output)

        val uri = saver.save(result.output, subDir = "Movies/Browxer")
        val entity = dao.getById(task.id)
        dao.updateDownload(entity!!.copy(
            status = DownloadEntity.COMPLETED, progress = 100,
            filePath = uri.toString()))
        notifyDone(task)
    }.onFailure { e ->
        dao.updateStatus(task.id, DownloadEntity.FAILED, friendlyError(e))
        notifyFailed(task, friendlyError(e))
    }

    private suspend fun report(id: Long, p: Float) {
        dao.updateProgress(id, (p * 100).toInt(), 0, 0, 0)
        getSystemService(NotificationManager::class.java).notify(
            NOTIF_ID + id.toInt(),
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading…")
                .setProgress(100, (p * 100).toInt(), false)
                .setOngoing(true)
                .addAction(0, "Cancel", cancelPending(id))
                .build())
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
    }

    private fun buildSummary(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title).setContentText(text).setOngoing(true).build()

    private fun notifyDone(task: DownloadTask) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIF_ID + task.id.toInt(),
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Downloaded successfully")
                .setContentText(task.fileName)
                .setAutoCancel(true).build())
    }

    private fun notifyFailed(task: DownloadTask, msg: String) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIF_ID + task.id.toInt(),
            NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download failed")
                .setContentText(msg)
                .setAutoCancel(true).build())
    }

    private fun cancelPending(id: Long): PendingIntent {
        val intent = Intent(this, DownloadService::class.java)
            .setAction(ACTION_CANCEL).putExtra(EXTRA_ID, id)
        return PendingIntent.getService(this, id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel() }

    companion object {
        const val EXTRA_TASK = "task"; const val EXTRA_ID = "id"
        const val ACTION_CANCEL = "com.browxer.action.CANCEL"
        private const val NOTIF_ID = 42; private const val CHANNEL = "downloads"

        fun enqueue(ctx: android.content.Context, task: DownloadTask) {
            ctx.startForegroundService(
                Intent(ctx, DownloadService::class.java).putExtra(EXTRA_TASK, task))
        }
    }
}
