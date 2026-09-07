package com.family4.app.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.family4.app.BuildConfig
import com.family4.app.databinding.ActivityOnboardingBinding
import com.family4.app.drive.DriveManager
import com.family4.app.notifications.NotificationHelper
import com.family4.app.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OnboardingActivity — Beautiful 3-step welcome + setup flow.
 *
 * Step 1 → WELCOME    — app name, logo, tagline
 * Step 2 → PROFILE    — display name, avatar, optional family invite code
 * Step 3 → CONNECT    — Google Sign-In (Drive) + permission grants + finish
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject lateinit var driveManager: DriveManager

    private var currentStep = 1
    private val totalSteps  = 3

    // ── Google Sign-In ────────────────────────────────────────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            if (account != null) {
                binding.tvGoogleAccount.text = "✅ ${account.email}"
                binding.btnGoogleSignIn.isVisible = false
                binding.tvGoogleAccount.isVisible = true
                // Init Drive and create folder structure
                lifecycleScope.launch {
                    driveManager.initialize(account)
                    driveManager.ensureFolderStructure()
                }
                updateNextButton()
            }
        }
    }

    // ── Permission Launcher ───────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        binding.tvPermissionStatus.text =
            if (allGranted) "✅ All permissions granted"
            else "⚠️ Some permissions denied — features may be limited"
        updateNextButton()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupGoogleSignIn()
        renderStep(currentStep)
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                renderStep(currentStep)
            } else {
                finishOnboarding()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                renderStep(currentStep)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.btnGrantPermissions.setOnClickListener {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            ))
        }
    }

    private fun renderStep(step: Int) {
        // Update step indicators
        binding.stepIndicator1.isSelected = step >= 1
        binding.stepIndicator2.isSelected = step >= 2
        binding.stepIndicator3.isSelected = step >= 3
        binding.tvStepCount.text = "Step $step of $totalSteps"

        // Toggle visibility of each step panel
        binding.panelWelcome.isVisible  = step == 1
        binding.panelProfile.isVisible  = step == 2
        binding.panelConnect.isVisible  = step == 3

        binding.btnBack.isVisible = step > 1
        binding.btnNext.text = if (step == totalSteps) "Let's Go! 🚀" else "Continue"

        updateNextButton()
    }

    private fun updateNextButton() {
        binding.btnNext.isEnabled = when (currentStep) {
            1 -> true   // Welcome step — always can proceed
            2 -> binding.etDisplayName.text?.isNotBlank() == true
            3 -> true   // Connect step — Google and permissions are optional on first run
            else -> true
        }
    }

    private fun finishOnboarding() {
        // Save that onboarding is complete
        getSharedPreferences("family4_prefs", MODE_PRIVATE)
            .edit().putBoolean("onboarding_complete", true).apply()
        val name = binding.etDisplayName.text?.toString()?.trim()
        if (!name.isNullOrBlank()) {
            getSharedPreferences("family4_prefs", MODE_PRIVATE)
                .edit().putString("display_name", name).apply()
        }
        NotificationHelper.showToast(this, "Welcome to Family4, ${name ?: "friend"}! 🎉")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
