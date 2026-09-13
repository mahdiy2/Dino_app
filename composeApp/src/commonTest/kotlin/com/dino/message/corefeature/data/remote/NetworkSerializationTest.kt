package com.dino.message.corefeature.data.remote

import com.dino.message.authfeature.data.remote.dto.LoginRequest
import com.dino.message.chatfeature.data.remote.dto.ReactToMessageRequest
import com.dino.message.profilefeature.data.remote.dto.UpdateMyProfileRequest
import com.dino.message.threadfeature.data.remote.dto.CreatePostRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkSerializationTest {
    private val json = Json { explicitNulls = false }

    @Test
    fun requestDtosPreserveServerWireNames() {
        assertEquals(
            """{"username":"mahdi","password":"secret"}""",
            json.encodeToString(LoginRequest(username = "mahdi", password = "secret")),
        )
        assertEquals(
            """{"message_id":"m1","emoji":"like"}""",
            json.encodeToString(ReactToMessageRequest(messageId = "m1", emoji = "like")),
        )
        val postPayload = json.encodeToString(
            CreatePostRequest(text = "hello", followers_only = true, quoted_post_id = 7L),
        )
        assertTrue(postPayload.contains(""""followers_only":true"""))
        assertTrue(postPayload.contains(""""quoted_post_id":7"""))

        val profilePayload = json.encodeToString(
            UpdateMyProfileRequest(
                name = "name",
                bio = null,
                profile_image_shape = "circle",
                remove_profile_image = true,
                remove_profile_video = null,
            ),
        )
        assertTrue(profilePayload.contains(""""profile_image_shape":"circle""""))
        assertTrue(profilePayload.contains(""""remove_profile_image":true"""))
        assertTrue(!profilePayload.contains("remove_profile_video"))
    }
}
