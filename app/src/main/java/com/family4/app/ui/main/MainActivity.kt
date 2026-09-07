package com.family4.app.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.family4.app.R
import com.family4.app.ai.AppAgentActionDispatcher
import com.family4.app.ai.FamilyAIAssistant
import com.family4.app.data.prefs.SettingsKeys
import com.family4.app.data.prefs.settingsDataStore
import com.family4.app.databinding.ActivityMainBinding
import com.family4.app.ui.pin.PinLockActivity
import com.google.android.material.badge.BadgeDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** Badge counters for the bottom navigation. */
    private val viewModel: MainViewModel by viewModels()

    /** Timestamp (elapsedRealtime) when the app was last moved to background. */
    private var backgroundedAt = 0L

    /** Result launcher for the PIN lock screen. */
    private val pinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // Wrong PIN — finish so the app is removed from recents too
            finishAffinity()
        }
    }

    /** Injected dispatcher — routes AI actions into nav + Room */
    @Inject lateinit var agentDispatcher: AppAgentActionDispatcher

    /** Injected AI assistant — receives the dispatcher reference at runtime */
    @Inject lateinit var aiAssistant: FamilyAIAssistant

    // Top-level destinations — these hide the back arrow
    private val topLevelDestinations = setOf(
        R.id.nav_dashboard,
        R.id.nav_camera,
        R.id.nav_chat,
        R.id.nav_map,
        R.id.nav_more
    )

    // Destinations that take over the whole screen (no chrome)
    private val immersiveDestinations = setOf(
        R.id.nav_camera,
        R.id.nav_chat_detail,
        R.id.nav_note_detail,
        R.id.nav_walkie_talkie,
        R.id.nav_photo_viewer
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: let the app draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire AI dispatcher: give it NavController + register it with the assistant
        agentDispatcher.navController    = navController
        aiAssistant.actionDispatcher     = agentDispatcher

        val appBarConfig = AppBarConfiguration(topLevelDestinations)
        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNavView.setupWithNavController(navController)

        setupChromeVisibility()
        observeBadges()

        // Handle widget / voice-command deep-links from the launch intent
        handleVoiceNavIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceNavIntent(intent)
    }

    /**
     * Handles [voice_nav_target] extras placed by the widget or VoiceCommandService.
     * Maps the string target to a nav graph destination ID and navigates there.
     */
    private fun handleVoiceNavIntent(intent: android.content.Intent?) {
        val target = intent?.getStringExtra("voice_nav_target") ?: return
        val destId = when (target) {
            "dashboard" -> R.id.nav_dashboard
            "camera"    -> R.id.nav_camera
            "chat"      -> R.id.nav_chat
            "map"       -> R.id.nav_map
            "notes"     -> R.id.nav_notes
            "calendar"  -> R.id.nav_calendar
            "files"     -> R.id.nav_files
            "tasks"     -> R.id.nav_tasks
            "board"     -> R.id.nav_family_board
            "albums"    -> R.id.nav_albums
            "weather"   -> R.id.nav_weather
            "walkie"    -> R.id.nav_walkie_talkie
            else        -> return
        }
        // NavController must be ready — post to main thread if called from onCreate
        binding.root.post { navController.navigate(destId) }
        // Clear extra so rotation doesn't re-navigate
        intent.removeExtra("voice_nav_target")
    }

    /**
     * Distributes system-bar insets so:
     *  - The AppBarLayout (Toolbar) sits below the status bar.
     *  - The FragmentContainerView bottom-margin equals navBar + bottomNav height.
     *  - The nav container (divider + BottomNavigationView) gains bottom
     *    padding for the gesture bar.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()
            )
            // Toolbar: pad top so it clears the status bar
            binding.appBarLayout.setPadding(0, bars.top, 0, 0)
            // Nav container (divider + bar): pad bottom for gesture handle / 3-button bar
            binding.navContainer.setPadding(0, 0, 0, bars.bottom)
            // Fragment host: keep its bottom clear of the nav container + system nav bar
            val navHeight = binding.navContainer.height
                .takeIf { it > 0 } ?: (57 * resources.displayMetrics.density).toInt()
            binding.navHostFragment.setPadding(0, 0, 0, navHeight + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    // ── Chrome (toolbar + bottom nav) ────────────────────────────────────────

    /** Fades the bottom nav / action bar out on immersive destinations. */
    private fun setupChromeVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val immersive = destination.id in immersiveDestinations
            animateChrome(visible = !immersive)
            if (immersive) supportActionBar?.hide() else supportActionBar?.show()
        }
    }

    private fun animateChrome(visible: Boolean) {
        val nav = binding.navContainer
        if (visible && nav.visibility == View.VISIBLE) return
        if (!visible && nav.visibility == View.GONE) return

        if (visible) {
            nav.alpha = 0f
            nav.visibility = View.VISIBLE
            nav.animate().alpha(1f).translationY(0f).setDuration(180L).start()
        } else {
            nav.animate()
                .alpha(0f)
                .translationY(nav.height.toFloat())
                .setDuration(160L)
                .withEndAction { nav.visibility = View.GONE }
                .start()
        }
    }

    // ── Bottom-nav badges ────────────────────────────────────────────────────

    /**
     * Keeps the Chat and More badges in sync with unread messages and open
     * tasks. Collection is lifecycle-scoped, so it stops while backgrounded.
     */
    private fun observeBadges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.unreadMessages.collectLatest { count ->
                        applyBadge(R.id.nav_chat, count, R.color.accent_cyan)
                    }
                }
                launch {
                    viewModel.pendingTasks.collectLatest { count ->
                        applyBadge(R.id.nav_more, count, R.color.accent_purple)
                    }
                }
            }
        }
    }

    /** Shows a numbered badge, or removes it entirely when the count is zero. */
    private fun applyBadge(menuItemId: Int, count: Int, backgroundColorRes: Int) {
        if (count <= 0) {
            binding.bottomNavView.removeBadge(menuItemId)
            return
        }
        val badge: BadgeDrawable = binding.bottomNavView.getOrCreateBadge(menuItemId)
        badge.isVisible = true
        badge.maxCharacterCount = MAX_BADGE_DIGITS
        badge.number = count
        badge.backgroundColor = ContextCompat.getColor(this, backgroundColorRes)
        badge.badgeTextColor = ContextCompat.getColor(this, R.color.bg_primary)
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        checkPinOnResume()
    }

    /**
     * If the app PIN is enabled and the app was backgrounded for more than
     * [PIN_GRACE_MS] milliseconds, launch the PIN lock screen.
     */
    private fun checkPinOnResume() {
        val elapsed = SystemClock.elapsedRealtime() - backgroundedAt
        if (backgroundedAt == 0L || elapsed < PIN_GRACE_MS) return

        lifecycleScope.launch {
            val pinEnabled = settingsDataStore.data.map { it[SettingsKeys.APP_PIN_ENABLED] ?: false }.first()
            val pinHash    = settingsDataStore.data.map { it[SettingsKeys.APP_PIN] }.first()
            if (pinEnabled && !pinHash.isNullOrEmpty()) {
                pinLauncher.launch(Intent(this@MainActivity, PinLockActivity::class.java))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private companion object {
        /** Counts above 99 render as "99+". */
        const val MAX_BADGE_DIGITS = 2
        /** App must be backgrounded longer than this before PIN is re-checked. */
        const val PIN_GRACE_MS = 10_000L
    }
}
