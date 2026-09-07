package com.family4.app.ui.pin

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.data.prefs.settingsDataStore
import com.family4.app.databinding.ActivityPinLockBinding
import com.family4.app.security.PinHasher
import com.family4.app.data.prefs.SettingsKeys
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Full-screen PIN entry activity.
 *
 * Launched by [com.family4.app.ui.splash.SplashActivity] when
 * [SettingsKeys.APP_PIN_ENABLED] is true and a PIN digest is stored.
 * After a successful match this activity finishes with [RESULT_OK] so the
 * caller can continue its normal routing; on failure the user is kept here.
 *
 * The back button is blocked — the only way out is correct PIN entry
 * (or killing the app via the recents list, which triggers another check
 * next time).
 */
class PinLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinLockBinding

    private val enteredDigits = StringBuilder()
    private val MAX_DIGITS = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildNumpad()
        buildDotRow(0)

        // Block the back button — user must enter the PIN
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Shake the dot row to indicate the PIN is required
                shakeDots()
            }
        })
    }

    // ── Numpad ────────────────────────────────────────────────────────────────

    private fun buildNumpad() {
        val dp = resources.displayMetrics.density

        // Digits 1-9, then *, 0, ⌫
        val labels = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")

        labels.forEachIndexed { index, label ->
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                textSize = if (label == "⌫") 20f else 24f
                isAllCaps = false
                val size = (72 * dp).toInt()
                val margin = (6 * dp).toInt()
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams(
                    androidx.gridlayout.widget.GridLayout.spec(index / 3),
                    androidx.gridlayout.widget.GridLayout.spec(index % 3)
                ).apply {
                    width = size; height = size
                    setMargins(margin, margin, margin, margin)
                }
                setTextColor(getColor(R.color.text_primary))
                strokeColor = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.card_stroke)
                )
                cornerRadius = (36 * dp).toInt()
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.bg_elevated)
                )
                if (label.isEmpty()) {
                    isEnabled = false
                    visibility = View.INVISIBLE
                } else {
                    setOnClickListener { v ->
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onKey(label)
                    }
                }
            }
            binding.pinNumpad.addView(btn)
        }
    }

    private fun onKey(label: String) {
        when (label) {
            "⌫" -> {
                if (enteredDigits.isNotEmpty()) {
                    enteredDigits.deleteCharAt(enteredDigits.lastIndex)
                    buildDotRow(enteredDigits.length)
                }
            }
            else -> {
                if (enteredDigits.length < MAX_DIGITS) {
                    enteredDigits.append(label)
                    buildDotRow(enteredDigits.length)
                    // Auto-submit when 4 digits entered and PIN length might be 4
                    if (enteredDigits.length == 4) attemptUnlock(autoSubmit = true)
                    else if (enteredDigits.length == MAX_DIGITS) attemptUnlock(autoSubmit = false)
                }
            }
        }
        binding.tvPinError.visibility = View.INVISIBLE
    }

    // ── Dot row ───────────────────────────────────────────────────────────────

    private fun buildDotRow(filledCount: Int) {
        binding.pinDotRow.removeAllViews()
        val dp = resources.displayMetrics.density
        val dotSize = (14 * dp).toInt()
        val margin  = (8 * dp).toInt()
        // Show 6 dots always (max PIN length)
        repeat(MAX_DIGITS) { i ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).also { lp ->
                    lp.marginEnd = margin
                }
                val circle = android.graphics.drawable.GradientDrawable()
                circle.shape = android.graphics.drawable.GradientDrawable.OVAL
                circle.setColor(
                    if (i < filledCount) getColor(R.color.accent_cyan)
                    else getColor(R.color.bg_elevated)
                )
                background = circle
            }
            binding.pinDotRow.addView(dot)
        }
    }

    // ── Verification ──────────────────────────────────────────────────────────

    private fun attemptUnlock(autoSubmit: Boolean) {
        val pin = enteredDigits.toString()
        lifecycleScope.launch {
            val stored = settingsDataStore.data.map { it[SettingsKeys.APP_PIN] }.first()
            if (stored.isNullOrEmpty()) {
                // No PIN stored — let through (shouldn't reach here normally)
                unlockSuccess()
                return@launch
            }

            val ok = if (PinHasher.isLegacyPlaintext(stored)) {
                if (stored == pin) {
                    // Migrate in place
                    settingsDataStore.updateData { prefs ->
                        prefs.toMutablePreferences().also { it[SettingsKeys.APP_PIN] = PinHasher.hash(pin) }
                    }
                    true
                } else false
            } else {
                PinHasher.verify(pin, stored)
            }

            if (ok) {
                unlockSuccess()
            } else if (!autoSubmit || pin.length >= MAX_DIGITS) {
                // Wrong PIN — shake and clear
                shakeDots()
                binding.tvPinError.visibility = View.VISIBLE
                enteredDigits.clear()
                buildDotRow(0)
            }
        }
    }

    private fun unlockSuccess() {
        setResult(RESULT_OK)
        finish()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    private fun shakeDots() {
        ObjectAnimator.ofFloat(binding.pinDotRow, View.TRANSLATION_X, 0f, 18f, -18f, 12f, -12f, 0f).apply {
            duration = 400L
            interpolator = CycleInterpolator(1f)
            start()
        }
        binding.pinDotRow.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
