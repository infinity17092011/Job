package com.jobagent

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    /** Blocking call. Run off the main thread. Returns null on any failure. */
    fun nextAction(ctx: Context, payload: JSONObject): JSONObject? {
        return try {
            val base = Profile.get(ctx, "backendUrl").trim().trimEnd('/')
            val conn = URL("$base/next-action").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 60000 // Render free tier cold start
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-app-token", Profile.get(ctx, "token").trim())
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) JSONObject(body) else null
        } catch (e: Exception) {
            null
        }
    }
}
