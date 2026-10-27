package com.tradingalert.receiver.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class NotificationImageWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val imageUrl = inputData.getString(KEY_IMAGE_URL) ?: return Result.success()
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, INVALID_NOTIFICATION_ID)
        if (notificationId == INVALID_NOTIFICATION_ID) return Result.failure()
        val image = downloadImage(imageUrl) ?: return Result.success()
        val message = PushMessage(
            title = inputData.getString(KEY_TITLE).orEmpty(),
            body = inputData.getString(KEY_BODY).orEmpty(),
            reference = inputData.getString(KEY_REFERENCE),
            imageUrl = imageUrl,
        )
        NotificationHelper.show(
            context = applicationContext,
            message = message,
            notificationId = notificationId,
            image = image,
            playSound = false,
        )
        return Result.success()
    }

    private suspend fun downloadImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(imageUrl)
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            Log.w(TAG, "Ignoring notification image with a non-HTTPS URL")
            return@withContext null
        }

        val connection = runCatching { URL(imageUrl).openConnection() as HttpsURLConnection }
            .getOrElse {
                Log.w(TAG, "Unable to open notification image URL", it)
                return@withContext null
            }
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connect()
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "Notification image returned HTTP ${connection.responseCode}")
                return@withContext null
            }
            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) {
                Log.w(TAG, "Notification image exceeds the app download limit")
                return@withContext null
            }

            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead < 0) break
                    totalBytes += bytesRead
                    if (totalBytes > MAX_DOWNLOAD_BYTES) return@withContext null
                    output.write(buffer, 0, bytesRead)
                }
                output.toByteArray()
            }
            decodeScaledBitmap(bytes)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to download notification image", error)
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeScaledBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_BITMAP_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_BITMAP_DIMENSION
        ) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    companion object {
        private const val TAG = "NotificationImage"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_REFERENCE = "reference"
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val INVALID_NOTIFICATION_ID = -1
        private const val MAX_DOWNLOAD_BYTES = 12 * 1_024 * 1_024
        private const val MAX_BITMAP_DIMENSION = 1_024
        private const val CONNECT_TIMEOUT_MILLIS = 5_000
        private const val READ_TIMEOUT_MILLIS = 8_000

        fun enqueue(
            context: Context,
            message: PushMessage,
            notificationId: Int,
        ) {
            val imageUrl = message.imageUrl ?: return
            val inputData = Data.Builder()
                .putString(KEY_TITLE, message.title)
                .putString(KEY_BODY, message.body)
                .putString(KEY_IMAGE_URL, imageUrl)
                .putInt(KEY_NOTIFICATION_ID, notificationId)
                .apply { message.reference?.let { putString(KEY_REFERENCE, it) } }
                .build()
            val request = OneTimeWorkRequestBuilder<NotificationImageWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "notification-image-$notificationId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
