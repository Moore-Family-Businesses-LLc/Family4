package com.family4.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.family4.app.ui.main.MainActivity

/**
 * AuthActivity — Biometric / PIN lock gate.
 * Shown on app resume if the user has enabled app lock in settings.
 */
class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                finish()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                finish()  // Return to lock screen / close app
            }
            override fun onAuthenticationFailed() {
                // Show error shake on UI
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Family4")
            .setSubtitle("Confirm your identity to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
