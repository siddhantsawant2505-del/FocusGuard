package com.siddhant.focusguard

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.util.Locale

class BlockActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var clock: Clock
    private lateinit var engine: LimiterEngine

    private var countdownTimer: CountDownTimer? = null
    private lateinit var countdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clock = SystemClockImpl(this)
        store = Store(this)
        engine = LimiterEngine(clock, store)

        setupView()
        setupBackHandler()
        startCountdown()
    }

    override fun onResume() {
        super.onResume()
        store.refresh()
        startCountdown()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        store.refresh()
        startCountdown()
    }

    private fun setupView() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(48, 48, 48, 48)
        }

        val titleText = TextView(this).apply {
            text = getString(R.string.time_up)
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        countdownText = TextView(this).apply {
            text = getString(R.string.unlocks_in, "--:--")
            textSize = 24f
            setTextColor(Color.parseColor("#80CBC4"))
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val subtitleText = TextView(this).apply {
            text = "Take a break. Your budget refreshes when the cooldown expires."
            textSize = 16f
            setTextColor(Color.parseColor("#B0BEC5"))
            gravity = Gravity.CENTER
        }

        val homeButton = android.widget.Button(this).apply {
            text = "Return to Home"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#37474F"))
            setPadding(48, 24, 48, 24)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 64
            }
            layoutParams = params
            setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }

        root.addView(titleText)
        root.addView(countdownText)
        root.addView(subtitleText)
        root.addView(homeButton)

        setContentView(root)
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })
    }

    private fun startCountdown() {
        countdownTimer?.cancel()

        val remainingMs = engine.remainingLockMs()
        if (remainingMs <= 0L) {
            engine.refreshLockExpiration()
            finish()
            return
        }

        updateTimeDisplay(remainingMs)

        countdownTimer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimeDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                engine.refreshLockExpiration()
                finish()
            }
        }.start()
    }

    private fun updateTimeDisplay(millisUntilFinished: Long) {
        val totalSeconds = (millisUntilFinished / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        countdownText.text = getString(R.string.unlocks_in, formatted)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
