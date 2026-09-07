package com.family4.app.ui.walkie

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.family4.app.R
import kotlin.math.*
import kotlin.random.Random

/**
 * Simple animated waveform view for the Walkie-Talkie screen.
 * Draws animated sine-bar lines when active.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00D4FF.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
    }
    private val barCount = 32
    private val amplitudes = FloatArray(barCount) { 0.15f }
    private var animating = false
    private val runnable = object : Runnable {
        override fun run() {
            if (animating) {
                updateAmplitudes()
                invalidate()
                postDelayed(this, 50)
            }
        }
    }

    private fun updateAmplitudes() {
        for (i in amplitudes.indices) {
            amplitudes[i] = 0.1f + Random.nextFloat() * 0.85f
        }
    }

    fun startAnimation() {
        animating = true
        post(runnable)
    }

    fun stopAnimation() {
        animating = false
        removeCallbacks(runnable)
        amplitudes.fill(0.15f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barWidth = w / (barCount * 2f)
        val spacing = barWidth
        val maxBarH = h * 0.8f

        for (i in 0 until barCount) {
            val x = spacing + i * (barWidth + spacing) + barWidth / 2
            val barH = maxBarH * amplitudes[i]
            val top = (h - barH) / 2f
            val bottom = top + barH
            barPaint.alpha = (180 + (amplitudes[i] * 75).toInt()).coerceAtMost(255)
            canvas.drawLine(x, top, x, bottom, barPaint)
        }
    }
}
