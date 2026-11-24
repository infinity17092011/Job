package com.jobagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.random.Random

class AgentService : AccessibilityService() {

    companion object {
        @Volatile var instance: AgentService? = null
        const val INDEED = "com.indeed.android.jobsearch"
        const val MAX_STEPS = 250
    }

    private val main = Handler(Looper.getMainLooper())
    private val net = Executors.newSingleThreadExecutor()
    private var running = false
    private var steps = 0
    private val history = ArrayList<JSONObject>()

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        running = false
        main.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    fun startAgent() {
        if (running) return
        running = true
        steps = 0
        history.clear()
        setStatus("Running")
        schedule(3000)
    }

    fun stopAgent(msg: String = "Stopped") {
        running = false
        main.removeCallbacksAndMessages(null)
        setStatus(msg)
    }

    private fun setStatus(msg: String) {
        Profile.set(this, "status", msg)
        main.post { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }

    private fun schedule(delayMs: Long) {
        main.postDelayed({ tick() }, delayMs)
    }

    private fun tick() {
        if (!running) return
        if (steps >= MAX_STEPS) { stopAgent("Step limit reached"); return }

        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != INDEED) {
            schedule(2500) // wait until Indeed is on screen
            return
        }

        val (nodes, refs) = ScreenReader.read(root)
        val payload = JSONObject()
            .put("goal", Profile.get(this, "goal").ifBlank { "Apply to jobs matching the profile" })
            .put("profile", Profile.toJson(this))
            .put("history", JSONArray(history.takeLast(10)))
            .put("screen", nodes)

        net.execute {
            val resp = ApiClient.nextAction(this, payload)
            main.post { handle(resp, refs) }
        }
    }

    private fun str(o: JSONObject, k: String): String = if (o.isNull(k)) "" else o.optString(k, "")

    private fun handle(resp: JSONObject?, refs: Map<Int, AccessibilityNodeInfo>) {
        if (!running) return
        if (resp == null) { schedule(4000); return }

        val action = str(resp, "action").ifBlank { "wait" }
        val nodeId = if (resp.isNull("nodeId")) -1 else resp.optInt("nodeId", -1)
        val text = str(resp, "text")
        val reason = str(resp, "reason")
        val ref = refs[nodeId]
        var result = "ok"

        when (action) {
            "tap" -> if (ref == null || !clickNode(ref)) result = "failed"
            "type" -> {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                if (ref == null || !ref.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) result = "failed"
            }
            "scroll_down" -> if (!swipe(true)) result = "failed"
            "scroll_up" -> if (!swipe(false)) result = "failed"
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "done" -> { stopAgent("Done: $reason"); return }
        }

        history.add(
            JSONObject().put("action", action).put("nodeId", nodeId)
                .put("text", text).put("result", result)
        )
        steps++
        schedule(2000L + Random.nextLong(0L, 2000L)) // human-paced
    }

    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        while (cur != null) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = cur.parent
        }
        val r = Rect()
        n.getBoundsInScreen(r)
        return gesture(Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }, 80)
    }

    private fun swipe(down: Boolean): Boolean {
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        val y1 = m.heightPixels * (if (down) 0.7f else 0.3f)
        val y2 = m.heightPixels * (if (down) 0.3f else 0.7f)
        return gesture(Path().apply { moveTo(x, y1); lineTo(x, y2) }, 350)
    }

    private fun gesture(path: Path, durationMs: Long): Boolean {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(g, null, null)
    }
}
