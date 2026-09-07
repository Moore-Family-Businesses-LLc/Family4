package com.family4.app.ui.stealth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.security.StealthModeManager
import com.family4.app.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * StealthCalculatorActivity — disguises the app as a basic calculator.
 *
 * If stealth mode is enabled, the launcher alias points here instead of
 * SplashActivity. The parent taps digits then presses "=" to submit the
 * sequence (digits used as a pattern proxy: tap e.g. 1-5-9-3 then =).
 * After 3 failed attempts the screen shows an error and rate-limits.
 *
 * When stealth is disabled (normal mode) this Activity finishes immediately
 * and defers to SplashActivity through the real launcher alias.
 */
class StealthCalculatorActivity : AppCompatActivity() {

    private var display       = ""
    private var failCount     = 0
    private var blockedUntil  = 0L

    private lateinit var tvDisplay : TextView
    private lateinit var tvHint    : TextView
    private lateinit var patternView: PatternLockView

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)

        // If stealth is not enabled, just forward to the real app
        if (!StealthModeManager.isStealthEnabled(this)) {
            launchMain(); return
        }

        setContentView(R.layout.activity_stealth_calculator)
        tvDisplay  = findViewById(R.id.tvCalcDisplay)
        tvHint     = findViewById(R.id.tvCalcHint)
        patternView = findViewById(R.id.patternLockView)

        setupPattern()
        setupCalcButtons()
    }

    private fun setupPattern() {
        patternView.visibility = View.GONE   // hidden by default; shown after special tap

        patternView.onPatternComplete = { dots ->
            if (System.currentTimeMillis() < blockedUntil) {
                tvHint.text = "Too many attempts. Try later."
                patternView.showError()
            } else if (StealthModeManager.verifyPattern(this, dots)) {
                failCount = 0
                lifecycleScope.launch {
                    StealthModeManager.unlock(this@StealthCalculatorActivity)
                    launchMain()
                }
            } else {
                failCount++
                if (failCount >= 3) {
                    blockedUntil = System.currentTimeMillis() + 30_000
                    tvHint.text = "Locked for 30 seconds"
                } else {
                    tvHint.text = "Wrong pattern (${3 - failCount} left)"
                }
                patternView.showError()
            }
        }
    }

    private fun setupCalcButtons() {
        // Tapping "1337" (easter-egg sequence) reveals the pattern grid
        val specialSequence = "1337"
        val ids = listOf(
            R.id.btnC0, R.id.btnC1, R.id.btnC2, R.id.btnC3,
            R.id.btnC4, R.id.btnC5, R.id.btnC6, R.id.btnC7,
            R.id.btnC8, R.id.btnC9, R.id.btnCDot, R.id.btnCEquals,
            R.id.btnCAdd, R.id.btnCSub, R.id.btnCMul, R.id.btnCDiv,
            R.id.btnCClear, R.id.btnCBack
        )
        ids.forEach { id ->
            val btn = findViewById<Button>(id) ?: return@forEach
            btn.setOnClickListener { onCalcButton(btn.text.toString(), specialSequence) }
        }
    }

    private fun onCalcButton(label: String, specialSeq: String) {
        when (label) {
            "C"  -> { display = ""; tvDisplay.text = "0" }
            "⌫"  -> { display = display.dropLast(1); tvDisplay.text = display.ifEmpty { "0" } }
            "="  -> {
                // Evaluate simple expression for appearance
                tvDisplay.text = safeEval(display)
                display = ""
                // Also check if typed sequence equals special combo to reveal pattern
            }
            else -> {
                display += label
                tvDisplay.text = display
                // Check for trigger sequence
                if (display.endsWith(specialSeq)) {
                    display = ""
                    tvDisplay.text = "0"
                    revealPatternGrid()
                }
            }
        }
    }

    private fun revealPatternGrid() {
        patternView.visibility = View.VISIBLE
        tvHint.text = "Draw your unlock pattern"
        tvHint.visibility = View.VISIBLE
    }

    private fun safeEval(expr: String): String {
        return try {
            // Very basic: only handle single operator for display plausibility
            val result = when {
                expr.contains('+') -> {
                    val p = expr.split('+')
                    (p[0].toDoubleOrNull() ?: 0.0) + (p.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
                }
                expr.contains('-') -> {
                    val p = expr.split('-')
                    (p[0].toDoubleOrNull() ?: 0.0) - (p.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
                }
                expr.contains('×') -> {
                    val p = expr.split('×')
                    (p[0].toDoubleOrNull() ?: 0.0) * (p.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
                }
                expr.contains('÷') -> {
                    val p = expr.split('÷')
                    val denom = p.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                    if (denom == 0.0) "Error" else (p[0].toDoubleOrNull() ?: 0.0) / denom
                }
                else -> expr.toDoubleOrNull() ?: 0.0
            }
            if (result is Double) {
                if (result == result.toLong().toDouble()) result.toLong().toString()
                else "%.4f".format(result).trimEnd('0').trimEnd('.')
            } else result.toString()
        } catch (_: Exception) { "Error" }
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
