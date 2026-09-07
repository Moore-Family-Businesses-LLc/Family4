package com.family4.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.family4.app.R
import com.family4.app.ai.AppAgentActionDispatcher
import com.family4.app.ai.FamilyAIAssistant
import com.family4.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Hide bottom nav on sub-screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_camera,
                R.id.nav_chat_detail,
                R.id.nav_note_detail,
                R.id.nav_walkie_talkie -> {
                    binding.bottomNavView.visibility = android.view.View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    binding.bottomNavView.visibility = android.view.View.VISIBLE
                    supportActionBar?.show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
