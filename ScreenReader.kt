package com.jobagent

import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

object ScreenReader {
    /** Flattens the visible screen into numbered nodes. Password fields are never read. */
    fun read(root: AccessibilityNodeInfo): Pair<JSONArray, Map<Int, AccessibilityNodeInfo>> {
        val arr = JSONArray()
        val map = HashMap<Int, AccessibilityNodeInfo>()
        var id = 0

        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || id >= 120) return
            if (n.isVisibleToUser && !n.isPassword) {
                val text = n.text?.toString() ?: ""
                val desc = n.contentDescription?.toString() ?: ""
                if (text.isNotBlank() || desc.isNotBlank() || n.isEditable) {
                    map[id] = n
                    arr.put(
                        JSONObject()
                            .put("id", id)
                            .put("text", text.take(160))
                            .put("desc", desc.take(100))
                            .put("clickable", n.isClickable)
                            .put("editable", n.isEditable)
                    )
                    id++
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }

        walk(root)
        return Pair(arr, map)
    }
}
