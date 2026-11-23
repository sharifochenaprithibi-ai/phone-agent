package com.example.phoneagent

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var step = 1
    private val mp = ViewGroup.LayoutParams.MATCH_PARENT
    private val gray = Color.parseColor("#9A9A9A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        step = if (AgentService.isRunning) 4 else 1
        render()
    }

    override fun onResume() {
        super.onResume()
        val running = AgentService.isRunning
        if (step == 3 && running) {
            step = 4
            render()
        } else if (step == 4 && !running) {
            step = 3
            render()
        }
    }

    // ---------- Screens ----------
    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(24))
        }
        root.addView(label("phone agent", 28f, Color.WHITE, bold = true, center = false))
        if (step <= 3) buildOnboarding(root) else buildPanel(root)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFillViewport = true
            addView(root)
        }
        setContentView(scroll)
    }

    private fun buildOnboarding(root: LinearLayout) {
        val (title, desc) = when (step) {
            1 -> "Welcome" to "This app controls your phone through the Accessibility Service. Everything stays on your device."
            2 -> "Allow restricted settings" to "Tap \"Open App Info\", then the \u22EE menu (top right) and choose \"Allow restricted settings\". If you don't see it, just continue."
            else -> "Enable Accessibility Service" to "Go to Accessibility Settings, find \"Phone Agent\" and toggle it on."
        }
        root.addView(label("Step $step of 3", 14f, gray))
        root.addView(label(title, 26f, Color.WHITE, bold = true))
        root.addView(label(desc, 16f, gray))
        root.addView(progress())
        root.addView(spacer())

        when (step) {
            1 -> {
                root.addView(button("Next", true) { step = 2; render() })
            }
            2 -> {
                root.addView(button("Open App Info", true) {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:$packageName"))
                    )
                })
                root.addView(button("Next", false) { step = 3; render() })
                root.addView(button("Go back", false) { step = 1; render() })
            }
            else -> {
                root.addView(button("Open Accessibility Settings", true) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                })
                root.addView(button("Go back", false) { step = 2; render() })
            }
        }
    }

    private fun buildPanel(root: LinearLayout) {
        root.addView(label("Service is ON", 22f, Color.parseColor("#4ADE80"), bold = true))
        root.addView(label("Test the basic commands", 15f, gray))

        root.addView(button("Home", false) { act { home() } })
        root.addView(button("Back", false) { act { back() } })
        root.addView(button("Recent apps", false) { act { recents() } })
        root.addView(button("Notifications", false) { act { notifications() } })
        root.addView(button("Scroll down", false) { act { scrollDown() } })
        root.addView(button("Scroll up", false) { act { scrollUp() } })

        val appInput = input("App name (e.g. Chrome)")
        root.addView(appInput)
        root.addView(button("Open app", true) { act { openApp(appInput.text.toString()) } })

        val textInput = input("Text")
        root.addView(textInput)
        root.addView(button("Click text on screen", false) {
            delayed { act { clickText(textInput.text.toString()) } }
        })
        root.addView(button("Type into focused field", false) {
            delayed { act { typeText(textInput.text.toString()) } }
        })
    }

    // ---------- Helpers ----------
    private fun act(block: AgentService.() -> Boolean) {
        val s = AgentService.instance
        if (s == null) {
            toast("Service is off")
            return
        }
        toast(if (s.block()) "Done" else "Failed / not found")
    }

    private fun delayed(block: () -> Unit) {
        toast("Switch to the target app within 4 seconds")
        Handler(Looper.getMainLooper()).postDelayed({ block() }, 4000)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false, center: Boolean = true) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = if (center) Gravity.CENTER else Gravity.START
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun button(t: String, primary: Boolean, onClick: () -> Unit) =
        Button(this).apply {
            text = t
            isAllCaps = false
            textSize = 16f
            setTextColor(if (primary) Color.BLACK else Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                if (primary) {
                    setColor(Color.WHITE)
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(1), Color.parseColor("#444444"))
                }
            }
            layoutParams = LinearLayout.LayoutParams(mp, dp(56)).apply { topMargin = dp(10) }
            setOnClickListener { onClick() }
        }

    private fun input(hintText: String) =
        EditText(this).apply {
            hint = hintText
            setHintTextColor(gray)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(mp, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(16) }
        }

    private fun spacer() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(mp, 0, 1f)
    }

    private fun progress(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.layoutParams = LinearLayout.LayoutParams(mp, dp(4)).apply { topMargin = dp(16) }
        for (i in 1..3) {
            val seg = View(this)
            seg.setBackgroundColor(if (i == step) Color.WHITE else Color.parseColor("#555555"))
            val lp = LinearLayout.LayoutParams(0, mp, 1f)
            if (i < 3) lp.rightMargin = dp(8)
            seg.layoutParams = lp
            bar.addView(seg)
        }
        return bar
    }
}
