package com.tradingalert.receiver.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushMessageTest {
    @Test
    fun `parses expected data fields`() {
        val message = PushMessage.from(
            mapOf("title" to "BTC cảnh báo", "body" to "Giá đã chạm mức", "reference" to "btc-01"),
            defaultTitle = "Default title",
            defaultBody = "Default body",
        )

        assertEquals("BTC cảnh báo", message.title)
        assertEquals("Giá đã chạm mức", message.body)
        assertEquals("btc-01", message.reference)
    }

    @Test
    fun `uses safe defaults for missing fields`() {
        val message = PushMessage.from(
            data = emptyMap(),
            defaultTitle = "Cảnh báo mới",
            defaultBody = "Bạn vừa nhận được tín hiệu mới.",
        )

        assertEquals("Cảnh báo mới", message.title)
        assertEquals("Bạn vừa nhận được tín hiệu mới.", message.body)
        assertNull(message.reference)
    }

    @Test
    fun `limits untrusted payload lengths`() {
        val message = PushMessage.from(
            data = mapOf("title" to "a".repeat(500), "body" to "b".repeat(1_000)),
            defaultTitle = "Default title",
            defaultBody = "Default body",
        )

        assertEquals(120, message.title.length)
        assertEquals(500, message.body.length)
    }
}
