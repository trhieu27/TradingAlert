package com.tradingalert.receiver.messaging

data class PushMessage(
    val title: String,
    val body: String,
    val reference: String? = null,
) {
    companion object {
        fun from(
            data: Map<String, String>,
            defaultTitle: String,
            defaultBody: String,
        ): PushMessage {
            val title = data["title"].orEmpty().trim().ifEmpty { defaultTitle }
            val body = data["body"].orEmpty().trim().ifEmpty { defaultBody }
            return PushMessage(
                title = title.take(120),
                body = body.take(500),
                reference = data["reference"]?.trim()?.takeIf(String::isNotEmpty)?.take(100),
            )
        }
    }
}
