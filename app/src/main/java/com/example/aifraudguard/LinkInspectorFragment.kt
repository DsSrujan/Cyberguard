package com.example.aifraudguard

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aifraudguard.databinding.FragmentLinkInspectorBinding
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LinkInspectorFragment : Fragment() {

    private var _binding: FragmentLinkInspectorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: CheckResultAdapter
    private var currentCheckResult: CheckResult? = null

    // Setup Retrofit with your Ngrok endpoint so it works on Physical Devices!
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://kaden-subimposed-ducally.ngrok-free.dev") 
        .client(OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(LinkCheckApi::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkInspectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = CheckResultAdapter()
        binding.rvChecks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChecks.adapter = adapter
        binding.rvChecks.isNestedScrollingEnabled = false
    }

    private fun setupListeners() {
        binding.btnCheckUrl.setOnClickListener {
            val url = binding.etUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                performUrlCheck(url)
            } else {
                Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCheckAnother.setOnClickListener {
            resetUI()
        }

        binding.btnShareWarning.setOnClickListener {
            currentCheckResult?.let { result ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "⚠️ Warning! This URL is ${result.verdict}: ${result.url}\n\nChecked by AI Fraud Guard App.")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Warning via")
                startActivity(shareIntent)
            }
        }

        binding.btnReportUrl.setOnClickListener {
            currentCheckResult?.let { result ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:") // only email apps should handle this
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("report@fraudshield.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Phishing URL Report")
                    
                    val details = result.checks.joinToString("\n") { " - ${it.name}: ${it.message}" }
                    putExtra(Intent.EXTRA_TEXT, "Suspicious URL: ${result.url}\nRisk Score: ${result.final_score}\n\nDetails:\n$details")
                }
                startActivity(Intent.createChooser(intent, "Send Email"))
            }
        }
    }

    private fun performUrlCheck(url: String) {
        // Show loading, hide result
        binding.btnCheckUrl.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = api.checkUrl(UrlCheckRequest(url = url))
                currentCheckResult = result
                
                // Hide loading
                binding.progressBar.visibility = View.GONE
                
                populateResultUI(result)
                
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnCheckUrl.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Could not check URL, please try again. Verify backend is running.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateResultUI(result: CheckResult) {
        binding.scoreProgress.progress = result.final_score
        binding.tvScore.text = result.final_score.toString()
        binding.scoreProgress.setIndicatorColor(Color.parseColor(result.color))
        
        binding.tvVerdict.text = result.verdict
        binding.tvVerdict.setTextColor(Color.parseColor(result.color))
        binding.tvSimpleMessage.text = result.simple_message
        
        adapter.submitList(result.checks)

        // Slide up animation
        binding.resultCard.visibility = View.VISIBLE
        binding.resultCard.translationY = 500f
        binding.resultCard.animate().translationY(0f).setDuration(400).start()
    }

    private fun resetUI() {
        binding.resultCard.animate().translationY(500f).setDuration(300).withEndAction {
            binding.resultCard.visibility = View.GONE
            binding.btnCheckUrl.visibility = View.VISIBLE
            binding.etUrlInput.setText("")
            adapter.submitList(emptyList())
            currentCheckResult = null
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
