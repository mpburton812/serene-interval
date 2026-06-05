package com.example.meditationparticles.data.onenote

import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteGraphClientTest {
    @Test
    fun buildMultipartPageBody_includesPresentationAndAudioParts() {
        val client = OneNoteGraphClient()
        val body = client.buildMultipartPageBody(
            html = "<html><body><p>Hello</p></body></html>",
            attachments = listOf(
                OneNoteUploadAttachment(
                    partName = "audio_observation",
                    fileName = "serene_1_observation.m4a",
                    contentType = "audio/mp4",
                    bytes = byteArrayOf(1, 2, 3),
                ),
                OneNoteUploadAttachment(
                    partName = "audio_request",
                    fileName = "serene_1_request.m4a",
                    contentType = "audio/mp4",
                    bytes = byteArrayOf(9, 8),
                ),
            ),
        )

        val multipart = body as MultipartBody
        assertEquals(3, multipart.parts.size)

        val contentDispositions = multipart.parts.mapNotNull { it.headers?.get("Content-Disposition") }
        assertTrue(contentDispositions.any { it.contains("name=\"Presentation\"") })
        assertTrue(contentDispositions.any { it.contains("name=\"audio_observation\"") })
        assertTrue(contentDispositions.any { it.contains("name=\"audio_request\"") })
    }
}

