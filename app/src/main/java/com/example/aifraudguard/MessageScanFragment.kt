package com.example.aifraudguard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aifraudguard.databinding.FragmentMessageScanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageScanFragment : Fragment() {

    private var _binding: FragmentMessageScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var signalsAdapter: CheckResultAdapter
    private var currentMode = "SMS" // "SMS" or "Email"

    // ── Label → color map ─────────────────────────────────
    private val COLOR_SAFE       = "#10B981"
    private val COLOR_SUSPICIOUS = "#F59E0B"
    private val COLOR_FRAUD      = "#EF4444"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToggle()
        setupListeners()
    }

    // ─────────────────────────────────────────────────────────
    //  Setup
    // ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        signalsAdapter = CheckResultAdapter()
        binding.rvSignals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSignals.adapter = signalsAdapter
        binding.rvSignals.isNestedScrollingEnabled = false
    }

    private fun setupToggle() {
        binding.toggleSms.setOnClickListener {
            if (currentMode != "SMS") {
                currentMode = "SMS"
                updateToggleUI()
                binding.etMessageInput.hint = "Paste SMS text here…"
            }
        }
        binding.toggleEmail.setOnClickListener {
            if (currentMode != "Email") {
                currentMode = "Email"
                updateToggleUI()
                binding.etMessageInput.hint = "Paste Email subject + body here…"
            }
        }
    }

    private fun updateToggleUI() {
        val active   = requireContext().getColor(R.color.primary)
        val inactive = Color.parseColor("#94A3B8")

        if (currentMode == "SMS") {
            binding.toggleSms.setBackgroundResource(R.drawable.bg_top_header)
            binding.toggleSms.setTextColor(Color.WHITE)
            binding.toggleEmail.setBackgroundColor(Color.TRANSPARENT)
            binding.toggleEmail.setTextColor(inactive)
        } else {
            binding.toggleEmail.setBackgroundResource(R.drawable.bg_top_header)
            binding.toggleEmail.setTextColor(Color.WHITE)
            binding.toggleSms.setBackgroundColor(Color.TRANSPARENT)
            binding.toggleSms.setTextColor(inactive)
        }
    }

    private fun setupListeners() {
        binding.btnAnalyze.setOnClickListener {
            val text = binding.etMessageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                performAnalysis(text)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please paste a message to analyze",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnScanAnother.setOnClickListener {
            resetUI()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Analysis
    // ─────────────────────────────────────────────────────────

    private fun performAnalysis(text: String) {
        // Show loading state
        binding.btnAnalyze.visibility = View.GONE
        binding.progressBarScan.visibility = View.VISIBLE
        binding.resultCardScan.visibility = View.GONE

        lifecycleScope.launch {
            // Run detection on background thread
            val result = withContext(Dispatchers.Default) {
                FraudDetector.analyzeText(requireContext(), text)
            }

            // UI Polish delay
            delay(500)

            // Show Toast if ML model was actually used
            if (result.usedMLModel) {
                Toast.makeText(context, "Validated by Deep Intelligence (Transformer-V3)", Toast.LENGTH_SHORT).show()
            }

            binding.progressBarScan.visibility = View.GONE
            showResult(result)
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UI population
    // ─────────────────────────────────────────────────────────

    private fun showResult(result: FraudAnalysisResult) {
        val hexColor = when (result.label) {
            "FRAUD"      -> COLOR_FRAUD
            "SUSPICIOUS" -> COLOR_SUSPICIOUS
            else         -> COLOR_SAFE
        }
        val color = Color.parseColor(hexColor)

        // Score ring (0–100)
        val scorePercent = (result.fraudScore * 100).toInt().coerceIn(0, 100)
        binding.fraudScoreRing.progress = scorePercent
        binding.fraudScoreRing.setIndicatorColor(color)

        // Score text
        binding.tvFraudScore.text = String.format("%.2f", result.fraudScore)

        // Label
        binding.tvFraudLabel.text = result.label
        binding.tvFraudLabel.setTextColor(color)

        // Reason & Recommendation
        binding.tvFraudReason.text = result.reason
        binding.tvFraudRecommendation.text = result.recommendation

        // Signals list
        if (result.matchedSignals.isNotEmpty()) {
            binding.tvSignalsHeader.visibility = View.VISIBLE

            // Map FraudSignal → CheckDetail (the type CheckResultAdapter expects)
            val checkItems = result.matchedSignals.map { signal ->
                val icon = when (signal.severity) {
                    SignalSeverity.HIGH   -> "🔴"
                    SignalSeverity.MEDIUM -> "🟡"
                    SignalSeverity.LOW    -> "🔵"
                }
                val scoreImpact = when (signal.severity) {
                    SignalSeverity.HIGH   -> 3
                    SignalSeverity.MEDIUM -> 2
                    SignalSeverity.LOW    -> 1
                }
                val color = when (signal.severity) {
                    SignalSeverity.HIGH   -> "#EF4444"
                    SignalSeverity.MEDIUM -> "#F59E0B"
                    SignalSeverity.LOW    -> "#64748B"
                }
                CheckDetail(
                    name    = signal.name,
                    score   = scoreImpact,
                    message = signal.description,
                    status  = signal.severity.name,
                    color   = color,
                    icon    = icon
                )
            }
            signalsAdapter.submitList(checkItems)
        } else {
            binding.tvSignalsHeader.visibility = View.GONE
            signalsAdapter.submitList(emptyList())
        }

        // Animate card up
        binding.resultCardScan.visibility = View.VISIBLE
        binding.resultCardScan.translationY = 600f
        binding.resultCardScan.animate()
            .translationY(0f)
            .setDuration(450)
            .start()
    }

    private fun resetUI() {
        binding.resultCardScan.animate()
            .translationY(600f)
            .setDuration(300)
            .withEndAction {
                binding.resultCardScan.visibility = View.GONE
                binding.btnAnalyze.visibility = View.VISIBLE
                binding.etMessageInput.setText("")
                signalsAdapter.submitList(emptyList())
                binding.tvSignalsHeader.visibility = View.GONE
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
