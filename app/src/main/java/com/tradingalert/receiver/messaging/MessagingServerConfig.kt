package com.tradingalert.receiver.messaging

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.tradingalert.receiver.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerMessagingConfig(
    val topic: String,
    val firebase: FirebaseClientConfig,
)

data class FirebaseClientConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val gcmSenderId: String,
    val storageBucket: String?,
)

object MessagingServerConfig {
    private const val CONFIG_PATH = "/config"
    private const val PREFS_NAME = "messaging_server_config"
    private const val KEY_CACHED_CONFIG = "cached_config"
    private const val KEY_SUBSCRIBED_TOPIC = "subscribed_topic"
    private const val CONNECT_TIMEOUT_MILLIS = 5_000
    private const val READ_TIMEOUT_MILLIS = 5_000
    private const val MAX_RESPONSE_LENGTH = 8 * 1_024
    private val topicPattern = Regex("^[a-zA-Z0-9-_.~%]{1,900}$")

    fun initializeCachedFirebase(context: Context): Boolean =
        cachedConfig(context)
            ?.let { config -> runCatching { applyFirebaseConfig(context, config.firebase) }.isSuccess }
            ?: false

    fun fetchAndApply(context: Context): Result<ServerMessagingConfig> =
        runCatching {
            val config = runCatching { fetchRemoteConfig() }
                .getOrElse { error -> cachedConfig(context) ?: throw error }
            applyFirebaseConfig(context, config.firebase)
            cacheConfig(context, config)
            config
        }.onFailure { error ->
            Log.e(TAG, "Unable to load or apply server messaging configuration", error)
        }

    fun subscribe(
        context: Context,
        messaging: FirebaseMessaging,
        topic: String,
        onComplete: (Boolean) -> Unit,
    ) {
        messaging.subscribeToTopic(topic).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val previousTopic = subscribedTopic(context)
                preferences(context).edit().putString(KEY_SUBSCRIBED_TOPIC, topic).apply()
                if (!previousTopic.isNullOrBlank() && previousTopic != topic) {
                    messaging.unsubscribeFromTopic(previousTopic)
                }
            }
            onComplete(task.isSuccessful)
        }
    }

    private fun fetchRemoteConfig(): ServerMessagingConfig {
        val baseUrl = BuildConfig.WORKER_BASE_URL.trim().trimEnd('/')
        require(baseUrl.startsWith("https://")) { "WORKER_BASE_URL must use HTTPS" }

        val connection = (URL(baseUrl + CONFIG_PATH).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
        }
        return try {
            require(connection.responseCode in 200..299) {
                "Configuration server returned HTTP ${connection.responseCode}"
            }
            val response = connection.inputStream.bufferedReader().use { reader ->
                val content = reader.readText()
                require(content.length <= MAX_RESPONSE_LENGTH) { "Configuration response is too large" }
                content
            }
            parseConfig(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseConfig(json: String): ServerMessagingConfig {
        val root = JSONObject(json)
        val topic = root.getString("topic").trim()
        require(topicPattern.matches(topic)) { "Invalid FCM topic returned by server" }

        val firebase = root.getJSONObject("firebase")
        require(firebase.getString("androidPackageName") == BuildConfig.APPLICATION_ID) {
            "Firebase configuration does not match this Android package"
        }
        return ServerMessagingConfig(
            topic = topic,
            firebase = FirebaseClientConfig(
                projectId = firebase.requiredString("projectId"),
                applicationId = firebase.requiredString("applicationId"),
                apiKey = firebase.requiredString("apiKey"),
                gcmSenderId = firebase.requiredString("gcmSenderId"),
                storageBucket = firebase.optString("storageBucket").trim().ifEmpty { null },
            ),
        )
    }

    @Synchronized
    private fun applyFirebaseConfig(context: Context, config: FirebaseClientConfig) {
        val existing = FirebaseApp.getApps(context)
            .firstOrNull { app -> app.name == FirebaseApp.DEFAULT_APP_NAME }
        if (existing?.options?.applicationId == config.applicationId) return
        existing?.delete()

        val options = FirebaseOptions.Builder()
            .setProjectId(config.projectId)
            .setApplicationId(config.applicationId)
            .setApiKey(config.apiKey)
            .setGcmSenderId(config.gcmSenderId)
            .apply { config.storageBucket?.let(::setStorageBucket) }
            .build()
        checkNotNull(FirebaseApp.initializeApp(context, options)) {
            "Unable to initialize Firebase from server configuration"
        }
    }

    private fun cacheConfig(context: Context, config: ServerMessagingConfig) {
        val json = JSONObject()
            .put("topic", config.topic)
            .put(
                "firebase",
                JSONObject()
                    .put("androidPackageName", BuildConfig.APPLICATION_ID)
                    .put("projectId", config.firebase.projectId)
                    .put("applicationId", config.firebase.applicationId)
                    .put("apiKey", config.firebase.apiKey)
                    .put("gcmSenderId", config.firebase.gcmSenderId)
                    .put("storageBucket", config.firebase.storageBucket.orEmpty()),
            )
            .toString()
        preferences(context).edit().putString(KEY_CACHED_CONFIG, json).apply()
    }

    private fun cachedConfig(context: Context): ServerMessagingConfig? =
        preferences(context).getString(KEY_CACHED_CONFIG, null)
            ?.let { json -> runCatching { parseConfig(json) }.getOrNull() }

    private fun subscribedTopic(context: Context): String? =
        preferences(context).getString(KEY_SUBSCRIBED_TOPIC, null)
            ?.takeIf(topicPattern::matches)

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun JSONObject.requiredString(name: String): String =
        getString(name).trim().also { value -> require(value.isNotEmpty()) { "$name is required" } }

    private const val TAG = "MessagingServerConfig"
}
