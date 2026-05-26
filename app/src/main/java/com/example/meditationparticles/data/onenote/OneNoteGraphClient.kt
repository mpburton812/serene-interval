package com.example.meditationparticles.data.onenote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OneNoteGraphClient(
    private val httpClient: OkHttpClient = defaultClient,
) {
    suspend fun createPage(
        accessToken: String,
        sectionId: String,
        html: String,
    ): String {
        val request = Request.Builder()
            .url("$GRAPH_BASE/me/onenote/sections/$sectionId/pages")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(html.toRequestBody("text/html; charset=utf-8".toMediaType()))
            .build()
        val response = httpClient.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("Create page failed (${it.code}): $body")
            }
            val json = JSONObject(body)
            return json.optString("id").takeIf { id -> id.isNotBlank() }
                ?: throw IOException("Create page response missing id")
        }
    }

    suspend fun ensureSereneIntervalSection(accessToken: String): String {
        val notebookId = listNotebooks(accessToken).firstOrNull()?.id
            ?: throw IOException("No OneNote notebooks found for this account")
        val existing = listSections(accessToken, notebookId)
            .firstOrNull { it.displayName.equals(SECTION_NAME, ignoreCase = true) }
        if (existing != null) return existing.id
        return createSection(accessToken, notebookId, SECTION_NAME)
    }

    suspend fun listSections(accessToken: String, notebookId: String): List<OneNoteSection> {
        val request = Request.Builder()
            .url("$GRAPH_BASE/me/onenote/notebooks/$notebookId/sections")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("List sections failed (${it.code}): $body")
            }
            return parseSectionList(JSONObject(body))
        }
    }

    private suspend fun listNotebooks(accessToken: String): List<OneNoteNotebook> {
        val request = Request.Builder()
            .url("$GRAPH_BASE/me/onenote/notebooks")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("List notebooks failed (${it.code}): $body")
            }
            val array = JSONObject(body).optJSONArray("value") ?: JSONArray()
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optString("id")
                    if (id.isNotBlank()) {
                        add(
                            OneNoteNotebook(
                                id = id,
                                displayName = item.optString("displayName"),
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun createSection(
        accessToken: String,
        notebookId: String,
        displayName: String,
    ): String {
        val payload = JSONObject().put("displayName", displayName).toString()
        val request = Request.Builder()
            .url("$GRAPH_BASE/me/onenote/notebooks/$notebookId/sections")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        val response = httpClient.newCall(request).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                throw IOException("Create section failed (${it.code}): $body")
            }
            val id = JSONObject(body).optString("id")
            if (id.isBlank()) throw IOException("Create section response missing id")
            return id
        }
    }

    private fun parseSectionList(json: JSONObject): List<OneNoteSection> {
        val array = json.optJSONArray("value") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                if (id.isNotBlank()) {
                    add(
                        OneNoteSection(
                            id = id,
                            displayName = item.optString("displayName"),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private const val GRAPH_BASE = "https://graph.microsoft.com/v1.0"
        const val SECTION_NAME = "Serene Interval"

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

data class OneNoteNotebook(
    val id: String,
    val displayName: String,
)

data class OneNoteSection(
    val id: String,
    val displayName: String,
)
