package com.jobagent

import android.content.Context
import org.json.JSONObject

object Profile {
    private val defaults = mapOf("backendUrl" to "https://jobagent-21fm.onrender.com")

    private fun prefs(c: Context) = c.getSharedPreferences("jobagent", Context.MODE_PRIVATE)

    fun get(c: Context, key: String): String =
        prefs(c).getString(key, defaults[key] ?: "") ?: ""

    fun set(c: Context, key: String, value: String) {
        prefs(c).edit().putString(key, value).apply()
    }

    fun toJson(c: Context): JSONObject = JSONObject()
        .put("name", get(c, "name"))
        .put("email", get(c, "email"))
        .put("phone", get(c, "phone"))
        .put("location", get(c, "location"))
        .put("targetRoles", get(c, "goal"))
        .put("resumeSummary", get(c, "resume"))
        .put("answers", get(c, "answers"))
}
