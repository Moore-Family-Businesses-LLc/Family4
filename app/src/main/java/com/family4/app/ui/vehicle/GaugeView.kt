package com.family4.app.ui.vehicle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.family4.app.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Circular arc gauge.
 *
 * Draws a dark track arc with a coloured progress arc on top,
 * a centre value label, and a unit sub-label.
 *
 * Attributes (all optional):
 *   [gaugeMin]      minimum value (default 0)
 *   [gaugeMax]      maximum value (default 8000)
 *   [gaugeValue]    current value (default 0)
 *   [gaugeUnit]     unit string shown below the number
 *   [gaugeLabel]    small label at the bottom of the arc
 *   [gaugeArcColor] progress arc colour (default accent_cyan)
 *   [gaugeWarnAt]   threshold above which arc turns red
 *
 * Usage:
 * ```xml
 * <com.family4.app.ui.vehicle.GaugeView
 *     android:layout_width="160dp"
 *     android:layout_height="160dp"
 *     app:gaugeMax="8000"
 *     app:gaugeUnit="rpm"
 *     app:gaugeLabel="ENGINE"/>
 * ```
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Configurable properties ───────────────────────────────────────────

    var min: Float   = 0f;    set(v) { field = v; invalidate() }
    var max: Float   = 8000f; set(v) { field = v; invalidate() }
    var warnAt: Float = Float.MAX_VALUE; set(v) { field = v; invalidate() }

    private var _value: Float = 0f
    var value: Float
        get() = _value
        set(v) { _value = v.coerceIn(min, max); invalidate() }

    var unit:  String = "rpm"; set(v) { field = v; invalidate() }
    var label: String = "";    set(v) { field = v; invalidate() }

    // Arc geometry
    private val ARC_START = 135f  // degrees from 3-o'clock, CW
    private val ARC_SWEEP = 270f  // total sweep
    private val STROKE_W  = 14f   // dp — converted in onSizeChanged

    // ── Paints ────────────────────────────────────────────────────────────

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.12f
    }
    // Needle tick marks
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val arcRect = RectF()
    private var cx = 0f; private var cy = 0f; private var radius = 0f

    private val cyanColor  = ContextCompat.getColor(context, R.color.accent_cyan)
    private val warnColor  = ContextCompat.getColor(context, R.color.error_red)
    private val trackColor = ContextCompat.getColor(context, R.color.bg_elevated)

    // ── Measurement ───────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val d = resources.displayMetrics.density
        val strokePx = STROKE_W * d
        cx = w / 2f; cy = h / 2f
        radius = (min(w, h) / 2f) - strokePx

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)

        trackPaint.strokeWidth    = strokePx
        progressPaint.strokeWidth = strokePx
        tickPaint.strokeWidth     = strokePx * 0.4f

        valuePaint.textSize = radius * 0.52f
        unitPaint.textSize  = radius * 0.22f
        labelPaint.textSize = radius * 0.18f

        trackPaint.color    = trackColor
        unitPaint.color     = 0xFF8892B0.toInt()
        labelPaint.color    = 0xFF8892B0.toInt()
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        // Track
        canvas.drawArc(arcRect, ARC_START, ARC_SWEEP, false, trackPaint)

        // Progress arc
        val progress = (_value - min) / (max - min)
        val sweep    = progress * ARC_SWEEP
        progressPaint.color = if (_value >= warnAt) warnColor else cyanColor
        canvas.drawArc(arcRect, ARC_START, sweep, false, progressPaint)

        // Tick marks (10 evenly spaced)
        drawTicks(canvas, 10)

        // Value text
        valuePaint.color = if (_value >= warnAt) warnColor else 0xFFFFFFFF.toInt()
        canvas.drawText(formatValue(_value), cx, cy + radius * 0.12f, valuePaint)

        // Unit sub-label
        canvas.drawText(unit, cx, cy + radius * 0.45f, unitPaint)

        // Bottom arc label
        canvas.drawText(label.uppercase(), cx, cy + radius * 0.95f, labelPaint)
    }

    private fun drawTicks(canvas: Canvas, count: Int) {
        tickPaint.color = 0xFF4A5580.toInt()
        for (i in 0..count) {
            val angle = Math.toRadians((ARC_START + i * ARC_SWEEP / count).toDouble())
            val outer = radius
            val inner = radius * 0.85f
            val sx = (cx + cos(angle) * outer).toFloat()
            val sy = (cy + sin(angle) * outer).toFloat()
            val ex = (cx + cos(angle) * inner).toFloat()
            val ey = (cy + sin(angle) * inner).toFloat()
            canvas.drawLine(sx, sy, ex, ey, tickPaint)
        }
    }

    private fun formatValue(v: Float): String = when {
        v >= 1000 -> "%.1fk".format(v / 1000f)
        v >= 10   -> "%d".format(v.toInt())
        else      -> "%.1f".format(v)
    }
}
