package com.family4.app.ui.splash

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.family4.app.databinding.ActivitySplashBinding
import com.family4.app.ui.main.MainActivity
import com.family4.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated brand splash.
 *
 * Sequence (total ~1.8s):
 *  1. Halo fades in and begins an infinite breathe/pulse loop.
 *  2. Logo scales up with an overshoot bounce.
 *  3. Wordmark and tagline rise and fade in.
 *  4. Progress bar + footer fade in.
 *  5. Route to Onboarding or Main, with a cross-fade transition.
 *
 * All animators are cancelled in [onDestroy] so nothing leaks if the user
 * backgrounds the app mid-animation.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    /** Infinite halo pulse — held so it can be cancelled deterministically. */
    private var pulseAnimator: ValueAnimator? = null

    /** Entrance choreography. */
    private var entranceSet: AnimatorSet? = null

    /** Routing coroutine. */
    private var routeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playEntranceAnimation()
        scheduleRouting()
    }

    // ── Animation ────────────────────────────────────────────────────────────

    private fun playEntranceAnimation() {
        // Halo: fade in, then breathe forever.
        val glowFade = ObjectAnimator.ofFloat(binding.splashGlow, View.ALPHA, 0f, 1f).apply {
            duration = 500L
            interpolator = DecelerateInterpolator()
        }

        // Logo: pop in from 40% scale with a subtle overshoot.
        binding.splashLogo.scaleX = 0.4f
        binding.splashLogo.scaleY = 0.4f
        val logoScaleX = ObjectAnimator.ofFloat(binding.splashLogo, View.SCALE_X, 0.4f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.splashLogo, View.SCALE_Y, 0.4f, 1f)
        val logoFade   = ObjectAnimator.ofFloat(binding.splashLogo, View.ALPHA, 0f, 1f)
        val logoSet = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoFade)
            duration = 620L
            interpolator = OvershootInterpolator(1.6f)
        }

        // Wordmark + tagline: rise 24dp and fade in, staggered.
        val title   = riseIn(binding.splashTitle,   distancePx = dp(24f), duration = 420L)
        val tagline = riseIn(binding.splashTagline, distancePx = dp(18f), duration = 420L)

        // Progress + footer: quiet fade at the end.
        val progress = ObjectAnimator.ofFloat(binding.splashProgress, View.ALPHA, 0f, 1f)
            .apply { duration = 300L }
        val footer = ObjectAnimator.ofFloat(binding.splashFooter, View.ALPHA, 0f, 0.85f)
            .apply { duration = 300L }

        entranceSet = AnimatorSet().apply {
            play(glowFade)
            play(logoSet).after(120L)
            play(title).after(logoSet)
            play(tagline).after(title)
            play(progress).after(560L)
            play(footer).after(700L)
            addListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator) = startHaloPulse()
            })
            start()
        }
    }

    /** Slide-up + fade-in helper for a single view. */
    private fun riseIn(view: View, distancePx: Float, duration: Long): AnimatorSet {
        view.translationY = distancePx
        val move = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, distancePx, 0f)
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        return AnimatorSet().apply {
            playTogether(move, fade)
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
    }

    /** Slow breathe on the halo — scale 1.0 → 1.12 with a matching alpha dip. */
    private fun startHaloPulse() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.12f).apply {
            duration = 1400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                binding.splashGlow.scaleX = scale
                binding.splashGlow.scaleY = scale
                binding.splashGlow.alpha = 1.4f - scale   // 0.4 → 0.28
            }
            start()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    // ── Routing ──────────────────────────────────────────────────────────────

    private fun scheduleRouting() {
        routeJob = CoroutineScope(Dispatchers.Main).launch {
            delay(SPLASH_DURATION_MS)

            val onboardingDone = getSharedPreferences("family4_prefs", MODE_PRIVATE)
                .getBoolean("onboarding_complete", false)

            val next = if (onboardingDone) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            }

            // Fade the splash out under the incoming activity.
            ObjectAnimator.ofFloat(binding.root, View.ALPHA, 1f, 0f).apply {
                duration = 220L
                interpolator = AccelerateInterpolator()
                start()
            }

            startActivity(next)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        routeJob?.cancel()
        pulseAnimator?.cancel()
        entranceSet?.cancel()
        super.onDestroy()
    }

    /** Adapter so callers only override the listener callbacks they need. */
    private abstract class SimpleAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) = Unit
        override fun onAnimationCancel(animation: Animator) = Unit
        override fun onAnimationRepeat(animation: Animator) = Unit
    }

    private companion object {
        /** Long enough for the full choreography to land before we navigate. */
        const val SPLASH_DURATION_MS = 1_800L
    }
}
