package com.family4.app.ui.parent

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.security.StealthModeManager
import com.family4.app.ui.stealth.PatternLockView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * Two-step pattern setup: draw once, confirm, then save.
 */
class PatternSetupBottomSheet : BottomSheetDialogFragment() {

    private var firstPattern: List<Int>? = null
    private var stage = STAGE_FIRST

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_pattern_setup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvInstr   = view.findViewById<TextView>(R.id.tvPatternInstruction)
        val patternView = view.findViewById<PatternLockView>(R.id.patternSetupView)

        tvInstr.text = "Draw a new unlock pattern"
        patternView.minDots = 4

        patternView.onPatternComplete = { dots ->
            when (stage) {
                STAGE_FIRST -> {
                    firstPattern = dots
                    stage = STAGE_CONFIRM
                    tvInstr.text = "Confirm your pattern"
                    patternView.reset()
                }
                STAGE_CONFIRM -> {
                    if (dots == firstPattern) {
                        lifecycleScope.launch {
                            StealthModeManager.setPattern(requireContext(), dots)
                            tvInstr.text = "Pattern saved ✓"
                            view.postDelayed({ dismiss() }, 800)
                        }
                    } else {
                        tvInstr.text = "Patterns don't match — try again"
                        stage = STAGE_FIRST
                        firstPattern = null
                        patternView.showError()
                    }
                }
            }
        }
    }

    companion object {
        private const val STAGE_FIRST   = 0
        private const val STAGE_CONFIRM = 1
    }
}
