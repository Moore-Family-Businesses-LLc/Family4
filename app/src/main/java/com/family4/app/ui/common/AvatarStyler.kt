package com.family4.app.ui.common

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

/**
 * Central styling for member avatars.
 *
 * Family4 ships no avatar images, so every member is rendered as a coloured
 * circle carrying their initials. The colour is *deterministic* — derived from
 * the member id — so the same person is always the same colour on every screen
 * and across app restarts, with no storage required.
 *
 * Usage from a ViewHolder:
 * ```
 * AvatarStyler.bind(b.ivAvatar, b.tvAvatarInitials, member.id, member.displayName)
 * AvatarStyler.setPresence(b.viewPresenceRing, member.isOnline, animate = true)
 * ```
 */
object AvatarStyler {

    /**
     * Palette tuned so white initials clear WCAG AA (4.5:1) on every entry —
     * measured ratios are noted per colour. Do not lighten these without
     * re-checking contrast.
     */
    private val PALETTE = intArrayOf(
        0xFF00738C.toInt(),   // teal-cyan   — 5.48:1 on white
        0xFF5B32B8.toInt(),   // violet      — 8.10:1
        0xFF2456B0.toInt(),   // azure       — 6.92:1
        0xFFA32C63.toInt(),   // magenta     — 6.78:1
        0xFF16705A.toInt(),   // emerald     — 6.00:1
        0xFF8F4E14.toInt(),   // amber-brown — 6.43:1
        0xFF3C4CA8.toInt(),   // indigo      — 7.52:1
        0xFF8A2F76.toInt()    // plum        — 7.64:1
    )

    /** Key used to stash a running pulse animator on the ring view. */
    private val TAG_PULSE = "avatar_pulse".hashCode()

    /**
     * Stable colour for a member. Uses the id (never the display name) so a
     * rename does not change someone's colour.
     */
    fun colorFor(key: String): Int {
        if (key.isEmpty()) return PALETTE[0]
        // String.hashCode is stable across JVM runs for a given string.
        return PALETTE[abs(key.hashCode()) % PALETTE.size]
    }

    /**
     * Up to two initials from a display name: "Paul Moore" -> "PM",
     * "paul" -> "P", "" -> "?".
     */
    fun initialsFor(displayName: String): String {
        val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase(Locale.getDefault())
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase(Locale.getDefault())
        }
    }

    /**
     * Paints the circle and writes the initials.
     *
     * @param avatarView the circular [ImageView] using `@drawable/bg_avatar_circle`
     * @param initialsView the [TextView] stacked on top of it
     */
    fun bind(avatarView: ImageView, initialsView: TextView, memberId: String, displayName: String) {
        val color = colorFor(memberId)
        avatarView.setImageDrawable(null)                       // initials replace any glyph
        avatarView.backgroundTintList = ColorStateList.valueOf(color)
        initialsView.text = initialsFor(displayName)
    }

    /**
     * Shows/hides the presence ring.
     *
     * @param animate when true the ring breathes (scale + fade) to read as
     *        "live". Pass false in long lists where many simultaneous animators
     *        would cost more than they add.
     */
    fun setPresence(ringView: View, isOnline: Boolean, animate: Boolean = false) {
        stopPulse(ringView)
        if (!isOnline) {
            ringView.visibility = View.GONE
            return
        }
        ringView.visibility = View.VISIBLE
        ringView.alpha = 1f
        ringView.scaleX = 1f
        ringView.scaleY = 1f
        if (animate) startPulse(ringView)
    }

    private fun startPulse(ringView: View) {
        val animator = ValueAnimator.ofFloat(1f, 1.18f).apply {
            duration = 1_200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                ringView.scaleX = scale
                ringView.scaleY = scale
                // Fades slightly as the ring expands, so it reads as a breath.
                ringView.alpha = (2.1f - scale).coerceIn(0f, 1f)
            }
            start()
        }
        ringView.setTag(TAG_PULSE, animator)
    }

    /** Cancels any pulse bound to this view — must run on ViewHolder rebind. */
    fun stopPulse(ringView: View) {
        (ringView.getTag(TAG_PULSE) as? ValueAnimator)?.cancel()
        ringView.setTag(TAG_PULSE, null)
    }
}
