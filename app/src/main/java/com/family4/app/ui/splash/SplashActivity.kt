package com.family4.app.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.family4.app.ui.main.MainActivity
import com.family4.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.Main).launch {
            delay(1200L)
            val onboardingDone = getSharedPreferences("family4_prefs", MODE_PRIVATE)
                .getBoolean("onboarding_complete", false)
            val next = if (onboardingDone) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            }
            startActivity(next)
            finish()
        }
    }
}
