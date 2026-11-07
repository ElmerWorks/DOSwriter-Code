package com.example.doswriter.logic

import androidx.annotation.Keep
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

@Keep
data class RemoteEntry(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val mtime: Long
)

@Keep
data class RemoteListResponse(
    val path: String,
    val entries: List<RemoteEntry>
)

@Keep
data class RemoteWriteRequest(
    val path: String,
    val content: String
)

@Keep
data class RemoteWriteResponse(
    val status: String
)

class RemoteBrowserClient(
    private val baseUrl: String,
    private val token: String? = null
) {
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()

    var currentPath: String = "."
        private set

    fun navigateToSubPath(subDirName: String): String {
        currentPath = resolveSubPath(currentPath, subDirName)
        return currentPath
    }

    fun navigateToParent(): String {
        currentPath = resolveParentPath(currentPath)
        return currentPath
    }

    fun resetPath() {
        currentPath = "."
    }

    fun listCurrentDir(): RemoteListResponse {
        return listDir(currentPath)
    }

    fun listDir(path: String): RemoteListResponse {
        val encoded = URLEncoder.encode(path, "UTF-8")
        val requestBuilder = Request.Builder()
            .url("$baseUrl/api/list?path=$encoded")
            .get()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("X-API-Token", token)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("listDir failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw RuntimeException("Empty response")
            return gson.fromJson(body, RemoteListResponse::class.java)
        }
    }

    fun readFile(path: String): String {
        val encoded = URLEncoder.encode(path, "UTF-8")
        val requestBuilder = Request.Builder()
            .url("$baseUrl/api/read?path=$encoded")
            .get()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("X-API-Token", token)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("readFile failed: HTTP ${response.code}")
            }
            return response.body?.string() ?: ""
        }
    }

    fun writeFile(path: String, content: String) {
        val payload = RemoteWriteRequest(path, content)
        val body = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val requestBuilder = Request.Builder()
            .url("$baseUrl/api/write")
            .post(body)

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("X-API-Token", token)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("writeFile failed: HTTP ${response.code}")
            }
        }
    }

    companion object {
        fun resolveSubPath(current: String, subDirName: String): String {
            if (subDirName == "..") return resolveParentPath(current)
            if (subDirName == "." || subDirName.isEmpty()) return current
            return if (current == "." || current.isEmpty()) subDirName else "$current/$subDirName"
        }

        fun resolveParentPath(current: String): String {
            if (current == "." || current.isEmpty() || !current.contains("/")) return "."
            val parent = current.substringBeforeLast("/")
            return if (parent.isEmpty()) "." else parent
        }
    }
}
