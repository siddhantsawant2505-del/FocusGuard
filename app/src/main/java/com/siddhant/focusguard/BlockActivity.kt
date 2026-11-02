package com.siddhant.focusguard

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
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
    private lateinit var subtitleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AudioFocusHelper.requestExclusiveFocus(this)

        clock = SystemClockImpl(this)
        store = Store(this)
        engine = LimiterEngine(clock, store)

        setupView()
        setupBackHandler()
        startCountdown()
    }

    override fun onResume() {
        super.onResume()
        AudioFocusHelper.requestExclusiveFocus(this)
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
        val dp = resources.displayMetrics.density

        // Deep immersive background
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0F17"))
            setPadding((24 * dp).toInt(), (32 * dp).toInt(), (24 * dp).toInt(), (32 * dp).toInt())
        }

        // Inner elevated card container
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#131B28"))
                cornerRadius = 24 * dp
                setStroke((1.5f * dp).toInt(), Color.parseColor("#27364D"))
            }
            background = cardBg
            setPadding((28 * dp).toInt(), (36 * dp).toInt(), (28 * dp).toInt(), (36 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Status badge pill: "SHIELD ACTIVE"
        val badge = TextView(this).apply {
            text = "SHIELD ACTIVE"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setTextColor(Color.parseColor("#10B981"))
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                setColor(Color.parseColor("#064E3B"))
                cornerRadius = 12 * dp
                setStroke((1f * dp).toInt(), Color.parseColor("#059669"))
            }
            background = badgeBg
            setPadding((12 * dp).toInt(), (5 * dp).toInt(), (12 * dp).toInt(), (5 * dp).toInt())
        }

        // Main Title
        val titleText = TextView(this).apply {
            text = getString(R.string.take_a_pause)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            setTextColor(Color.parseColor("#F1F5F9"))
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (20 * dp).toInt()
            }
            layoutParams = params
        }

        // Giant countdown timer
        countdownText = TextView(this).apply {
            text = "--:--"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD))
            setTextColor(Color.parseColor("#34D399"))
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (16 * dp).toInt()
                bottomMargin = (8 * dp).toInt()
            }
            layoutParams = params
        }

        // Subtitle with remaining lock text
        subtitleText = TextView(this).apply {
            text = getString(R.string.unlocks_in, "--:--")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
        }

        // Calming mindful tip
        val adviceText = TextView(this).apply {
            text = getString(R.string.block_motivational)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * dp).toInt()
                bottomMargin = (28 * dp).toInt()
            }
            layoutParams = params
        }

        // Sleek modern button: "Return to Home Screen"
        val buttonDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#10B981"))
            cornerRadius = 20 * dp
        }
        val rippleColor = ColorStateList.valueOf(Color.parseColor("#34D399"))
        val buttonBg = RippleDrawable(rippleColor, buttonDrawable, null)

        val homeButton = Button(this).apply {
            text = getString(R.string.return_to_home)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
            setTextColor(Color.parseColor("#042F2E"))
            background = buttonBg
            isAllCaps = false
            setPadding((28 * dp).toInt(), (14 * dp).toInt(), (28 * dp).toInt(), (14 * dp).toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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

        card.addView(badge)
        card.addView(titleText)
        card.addView(countdownText)
        card.addView(subtitleText)
        card.addView(adviceText)
        card.addView(homeButton)

        root.addView(card)
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
        countdownText.text = formatted
        subtitleText.text = getString(R.string.unlocks_in, formatted)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        AudioFocusHelper.releaseFocus(this)
    }
}
