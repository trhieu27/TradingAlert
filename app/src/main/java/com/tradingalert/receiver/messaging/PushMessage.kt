package com.tradingalert.receiver.messaging

data class PushMessage(
    val title: String,
    val body: String,
    val reference: String? = null,
    val imageUrl: String? = null,
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
                imageUrl = IMAGE_KEYS
                    .firstNotNullOfOrNull { key -> data[key]?.trim()?.takeIf(String::isNotEmpty) }
                    ?.take(MAX_IMAGE_URL_LENGTH),
            )
        }

        private val IMAGE_KEYS = listOf("image", "image_url", "imageUrl", "picture")
        private const val MAX_IMAGE_URL_LENGTH = 2_048
    }
}
