package com.family4.app.ui.stealth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.family4.app.R

/**
 * PatternLockView — 3×3 grid of touch-selectable dots connected by lines.
 *
 * Usage:
 *   patternView.onPatternComplete = { dots -> /* List<Int> 1..9 */ }
 *   patternView.reset()
 */
class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var onPatternComplete: ((List<Int>) -> Unit)? = null
    var minDots: Int = 4

    private val selectedDots = mutableListOf<Int>()   // 1-indexed, row-major
    private val dotCenters   = Array(9) { Pair(0f, 0f) }
    private var currentX = 0f
    private var currentY = 0f

    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var colorIdle    = 0xFF8892B0.toInt()
    private var colorActive  = 0xFF00D4FF.toInt()
    private var colorError   = 0xFFFF3D71.toInt()
    private var isError      = false

    init {
        try {
            colorIdle   = ContextCompat.getColor(context, R.color.text_muted)
            colorActive = ContextCompat.getColor(context, R.color.accent_cyan)
            colorError  = ContextCompat.getColor(context, R.color.error_red)
        } catch (_: Exception) {}
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        val cellW = w / 3f
        val cellH = h / 3f
        for (row in 0..2) {
            for (col in 0..2) {
                dotCenters[row * 3 + col] = Pair(
                    cellW * col + cellW / 2f,
                    cellH * row + cellH / 2f
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val radius     = (width.coerceAtMost(height) / 3f) * 0.18f
        val ringRadius = radius * 1.8f

        // Draw connecting lines
        paintLine.color = if (isError) colorError else colorActive
        if (selectedDots.size > 1) {
            val path = Path()
            selectedDots.forEachIndexed { idx, dot ->
                val (cx, cy) = dotCenters[dot - 1]
                if (idx == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
            }
            canvas.drawPath(path, paintLine)
        }
        // Line to finger if dragging
        if (selectedDots.isNotEmpty() && currentX != 0f) {
            val (lx, ly) = dotCenters[selectedDots.last() - 1]
            canvas.drawLine(lx, ly, currentX, currentY, paintLine)
        }

        // Draw dots
        for (i in 1..9) {
            val (cx, cy) = dotCenters[i - 1]
            val selected = i in selectedDots
            val color = when {
                isError && selected -> colorError
                selected           -> colorActive
                else               -> colorIdle
            }
            paintDot.color = color
            canvas.drawCircle(cx, cy, radius, paintDot)
            if (selected) {
                paintRing.color = color
                paintRing.alpha = 100
                canvas.drawCircle(cx, cy, ringRadius, paintRing)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isError = false
                currentX = event.x
                currentY = event.y
                detectHit(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                currentX = 0f; currentY = 0f
                if (selectedDots.size >= minDots) {
                    onPatternComplete?.invoke(selectedDots.toList())
                }
                invalidate()
            }
        }
        return true
    }

    fun reset() {
        selectedDots.clear()
        isError = false
        currentX = 0f; currentY = 0f
        invalidate()
    }

    fun showError() {
        isError = true
        invalidate()
        postDelayed({ reset() }, 800)
    }

    private fun detectHit(x: Float, y: Float) {
        val radius = (width.coerceAtMost(height) / 3f) * 0.28f
        for (i in 1..9) {
            if (i in selectedDots) continue
            val (cx, cy) = dotCenters[i - 1]
            val dx = x - cx; val dy = y - cy
            if (dx * dx + dy * dy <= radius * radius) {
                selectedDots.add(i)
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}
