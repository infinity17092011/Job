package com.jobagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private val inputs = LinkedHashMap<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun label(t: String, size: Float) = TextView(this).apply { text = t; textSize = size }

        fun input(key: String, hintText: String, multi: Boolean = false) {
            val e = EditText(this).apply {
                setHint(hintText)
                setText(Profile.get(this@MainActivity, key))
                if (multi) {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                }
            }
            inputs[key] = e
            col.addView(e)
        }

        fun button(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t
            setOnClickListener { onClick() }
        }

        col.addView(label("JobAgent", 26f))
        statusView = label("", 14f)
        col.addView(statusView)

        input("backendUrl", "Backend URL")
        input("token", "App token (same as APP_TOKEN on server)")
        input("goal", "Goal, e.g. Apply to Data Analyst jobs in Austin")
        input("name", "Full name")
        input("email", "Email")
        input("phone", "Phone")
        input("location", "Location")
        input("resume", "Resume summary: titles, years, key skills", multi = true)
        input("answers", "Screening answers, one per line: Question: Answer", multi = true)

        col.addView(button("1. Enable accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(button("2. Save and start applying") {
            saveAll()
            startAgent()
        })
        col.addView(button("Stop") {
            AgentService.instance?.stopAgent()
            refresh()
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val on = if (AgentService.instance != null) "ON" else "OFF"
        statusView.text = "Accessibility: $on\n${Profile.get(this, "status")}"
    }

    private fun saveAll() {
        inputs.forEach { (k, e) -> Profile.set(this, k, e.text.toString()) }
    }

    private fun startAgent() {
        val svc = AgentService.instance
        if (svc == null) {
            Toast.makeText(this, "Turn on JobAgent in Accessibility first", Toast.LENGTH_LONG).show()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(AgentService.INDEED)
        if (launch == null) {
            Toast.makeText(this, "Install the Indeed app and log in first", Toast.LENGTH_LONG).show()
            return
        }
        svc.startAgent()
        startActivity(launch)
    }
}
