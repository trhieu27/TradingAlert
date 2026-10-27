package com.tradingalert.receiver.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.tradingalert.receiver.messaging.AlertMessagingService
import org.json.JSONObject

data class FirebaseProjectConfig(
    val projectNumber: String,
    val projectId: String,
    val storageBucket: String?,
    val applicationId: String,
    val packageName: String,
    val apiKey: String,
) {
    fun toFirebaseOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setApplicationId(applicationId)
        .setApiKey(apiKey)
        .setGcmSenderId(projectNumber)
        .setProjectId(projectId)
        .apply { storageBucket?.let(::setStorageBucket) }
        .build()
}

object FirebaseRuntimeConfig {
    private const val PREFS_NAME = "firebase_runtime_config"
    private const val KEY_CONFIG_JSON = "google_services_json"
    private const val MAX_CONFIG_LENGTH = 64 * 1_024

    fun initializeSavedDefaultApp(context: Context) {
        val saved = savedJson(context) ?: return
        val config = parse(context, saved).getOrNull() ?: return
        if (defaultApp(context) == null) {
            FirebaseApp.initializeApp(
                context,
                config.toFirebaseOptions(),
                FirebaseApp.DEFAULT_APP_NAME,
            )
        }
    }

    fun ensureDefaultApp(context: Context) {
        if (defaultApp(context) == null) FirebaseApp.initializeApp(context)
    }

    fun saveAndApply(context: Context, json: String): Result<FirebaseProjectConfig> = runCatching {
        val config = parse(context, json).getOrThrow()
        val normalizedJson = normalize(json)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONFIG_JSON, normalizedJson)
            .apply()

        defaultApp(context)?.delete()
        checkNotNull(
            FirebaseApp.initializeApp(
                context,
                config.toFirebaseOptions(),
                FirebaseApp.DEFAULT_APP_NAME,
            ),
        )
        context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AlertMessagingService.KEY_TOKEN)
            .remove(AlertMessagingService.KEY_TOKEN_TIME)
            .apply()
        config
    }

    fun editableJson(context: Context): String {
        savedJson(context)?.let { return it }
        val options = defaultApp(context)?.options ?: return ""
        val client = JSONObject()
            .put(
                "client_info",
                JSONObject()
                    .put("mobilesdk_app_id", options.applicationId)
                    .put(
                        "android_client_info",
                        JSONObject().put("package_name", context.packageName),
                    ),
            )
            .put(
                "api_key",
                org.json.JSONArray().put(JSONObject().put("current_key", options.apiKey)),
            )
            .put("oauth_client", org.json.JSONArray())
            .put("services", JSONObject())
        return JSONObject()
            .put(
                "project_info",
                JSONObject()
                    .put("project_number", options.gcmSenderId)
                    .put("project_id", options.projectId)
                    .apply { options.storageBucket?.let { put("storage_bucket", it) } },
            )
            .put("client", org.json.JSONArray().put(client))
            .put("configuration_version", "1")
            .toString(2)
    }

    fun parse(context: Context, json: String): Result<FirebaseProjectConfig> = runCatching {
        require(json.isNotBlank()) { "empty_config" }
        require(json.length <= MAX_CONFIG_LENGTH) { "config_too_large" }
        val root = JSONObject(json)
        val projectInfo = root.getJSONObject("project_info")
        val clients = root.getJSONArray("client")
        val matchingClient = (0 until clients.length())
            .asSequence()
            .map { clients.getJSONObject(it) }
            .firstOrNull { client ->
                client.getJSONObject("client_info")
                    .getJSONObject("android_client_info")
                    .optString("package_name") == context.packageName
            }
            ?: error("package_mismatch")
        val clientInfo = matchingClient.getJSONObject("client_info")
        val apiKeys = matchingClient.getJSONArray("api_key")
        require(apiKeys.length() > 0) { "missing_api_key" }

        FirebaseProjectConfig(
            projectNumber = projectInfo.requireText("project_number"),
            projectId = projectInfo.requireText("project_id"),
            storageBucket = projectInfo.optString("storage_bucket").takeIf(String::isNotBlank),
            applicationId = clientInfo.requireText("mobilesdk_app_id"),
            packageName = context.packageName,
            apiKey = apiKeys.getJSONObject(0).requireText("current_key"),
        )
    }

    private fun savedJson(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONFIG_JSON, null)

    private fun defaultApp(context: Context): FirebaseApp? =
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }

    private fun normalize(json: String): String = JSONObject(json).toString(2)

    private fun JSONObject.requireText(name: String): String =
        getString(name).trim().also { require(it.isNotEmpty()) { "missing_$name" } }
}
