package com.example.aifraudguard

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.min
import org.json.JSONArray

// ─────────────────────────────────────────────────────────
//  Data model returned by FraudDetector
// ─────────────────────────────────────────────────────────
data class FraudAnalysisResult(
    val fraudScore: Float,
    val label: String,
    val reason: String,
    val recommendation: String,
    val matchedSignals: List<FraudSignal>,
    val usedMLModel: Boolean // To tell the user if it used the real model or fallback
)

data class FraudSignal(
    val name: String,
    val description: String,
    val severity: SignalSeverity
)

enum class SignalSeverity { LOW, MEDIUM, HIGH }

// ─────────────────────────────────────────────────────────
//  FraudDetector — Now supports TFLite Hybrid Inference
// ─────────────────────────────────────────────────────────
object FraudDetector {

    private const val TAG = "FraudDetector"
    private const val MODEL_PATH = "fraud_model.tflite"
    private const val VOCAB_PATH = "vocabulary.json"
    private const val SEQUENCE_LENGTH = 100
    
    private var tfliteInterpreter: Interpreter? = null
    private var vocabulary: Map<String, Int>? = null

    // ── Heuristic Analysis Engine ───────────────
    
    private val HEURISTIC_SIGNALS = listOf(
        Pair("Temporal Pressure", listOf("urgent", "immediately", "act now", "last chance")),
        Pair("Financial Context", listOf("bank", "account", "payment", "wire transfer")),
        Pair("Identity Verification", listOf("otp", "password", "pin", "verify")),
        Pair("Reward Enticement", listOf("win", "free", "gift", "prize", "lottery"))
    )

    /**
     * Analyze text using a Hybrid approach:
     * 1. Attempts to use local TFLite Model (if fraud_model.tflite exists in assets).
     * 2. Falls back to Keyword Scoring if model is missing or fails.
     */
    fun analyzeText(context: Context, inputText: String): FraudAnalysisResult {
        Log.d(TAG, "analyzeText() called with text length: ${inputText.length}")
        
        if (inputText.isBlank()) {
            return FraudAnalysisResult(0f, "SAFE", "No text provided.", "Please enter text to analyze.", emptyList(), false)
        }

        // Ensure initialization
        synchronized(this) {
            if (tfliteInterpreter == null) {
                Log.d(TAG, "Interpreter is null, attempting to initialize...")
                initInterpreter(context)
            }
            if (vocabulary == null) {
                Log.d(TAG, "Vocabulary is null, attempting to load...")
                loadVocabulary(context)
            }
        }

        var usedML = false
        var mlScore = 0f
        val signals = mutableListOf<FraudSignal>()

        // Try ML Inference
        val interpreter = tfliteInterpreter
        val vocab = vocabulary

        if (interpreter != null && vocab != null) {
            try {
                Log.d(TAG, "Running ML Inference...")
                mlScore = runMLInference(interpreter, inputText)
                usedML = true
                Log.d(TAG, "ML Inference completed successfully. Score: $mlScore")
                signals.add(FraudSignal("Deep Intelligence", "Advanced Transformer-based semantic evaluation", SignalSeverity.HIGH))
            } catch (e: Exception) {
                Log.e(TAG, "ML Inference crashed: ${e.message}")
                e.printStackTrace()
            }
        } else {
            val reason = when {
                interpreter == null && vocab == null -> "Both Interpreter and Vocabulary failed to load."
                interpreter == null -> "TFLite Interpreter is null (check model file)."
                else -> "Vocabulary is null (check vocabulary.json)."
            }
            Log.w(TAG, "Skipping ML Analysis: $reason")
        }

        // Run Heuristic Analysis (Multi-factor safety net)
        val (kwScore, kwSignals) = runHeuristicAnalysis(inputText)
        Log.d(TAG, "Heuristic Analysis hits: ${kwSignals.size}, Score: $kwScore")
        
        // Final hybrid score
        val finalScore = if (usedML) {
            // Combine: 75% AI, 25% Keyword
            (mlScore * 0.75f) + (kwScore * 0.25f)
        } else {
            kwScore
        }

        signals.addAll(kwSignals)
        val scoreClamped = finalScore.coerceIn(0f, 1f)

        val label = when {
            scoreClamped < 0.35f -> "SAFE"
            scoreClamped <= 0.65f -> "SUSPICIOUS"
            else -> "FRAUD"
        }

        val recommendation = when (label) {
            "SAFE" -> "This message appears legitimate. You can proceed, but stay alert for any requests for sensitive data."
            "SUSPICIOUS" -> "Caution: Potential scam patterns detected. Avoid clicking links or sharing info. Verify the sender through official channels."
            else -> "High-Risk Alert! Do NOT interact. Block the sender and delete this message immediately to protect your data."
        }

        val reason = if (usedML) "Validated by Deep Intelligence & ${kwSignals.size} Behavioral Markers" 
                     else "Multi-Factor Heuristic Analysis: ${kwSignals.size} risk indicators identified"

        Log.d(TAG, "Final Result -> Score: $scoreClamped, Label: $label, usedML: $usedML")
        return FraudAnalysisResult(scoreClamped, label, reason, recommendation, signals, usedML)
    }

    private fun initInterpreter(context: Context) {
        try {
            Log.d(TAG, "Attempting to load model from assets: $MODEL_PATH")
            val model = loadModelFile(context)
            val options = Interpreter.Options()
            tfliteInterpreter = Interpreter(model, options)
            Log.i(TAG, "✓ TFLite Interpreter initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize TFLite Interpreter: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadVocabulary(context: Context) {
        try {
            Log.d(TAG, "Attempting to load vocabulary from assets: $VOCAB_PATH")
            val vocabString = context.assets.open(VOCAB_PATH).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(vocabString)
            val map = mutableMapOf<String, Int>()
            for (i in 0 until jsonArray.length()) {
                map[jsonArray.getString(i)] = i
            }
            vocabulary = map
            Log.i(TAG, "✓ Vocabulary loaded successfully: ${map.size} tokens found.")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to load vocabulary: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Real ML Inference Logic
     * Tokenizes the string using the loaded vocabulary and runs TFLite inference.
     */
    private fun runMLInference(interpreter: Interpreter, text: String): Float {
        val inputIds = preprocessText(text)
        
        // The model expects shape [1, 100] as Int32
        // Note: For some models, you might need FloatArray or LongArray.
        // Based on model_server.py, we assume Int32.
        val input = Array(1) { IntArray(SEQUENCE_LENGTH) }
        input[0] = inputIds
        
        // Output probability shape [1, 1] as Float32
        val output = Array(1) { FloatArray(1) }
        
        interpreter.run(input, output)
        return output[0][0] // Return probability
    }

    /**
     * Preprocesses text: Lowercase, tokenize (whitespace split), map to IDs, pad/truncate to 100.
     */
    private fun preprocessText(text: String): IntArray {
        // Simple regex to split by whitespace and remove punctuation
        val cleanedText = text.lowercase().replace(Regex("[^a-z0-9\\s]"), "")
        val tokens = cleanedText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val result = IntArray(SEQUENCE_LENGTH) { 0 } // Initialize with PAD (0)
        
        val vocab = vocabulary ?: return result
        
        for (i in 0 until min(tokens.size, SEQUENCE_LENGTH)) {
            val token = tokens[i]
            // UNK is at index 1 based on vocabulary.json
            result[i] = vocab[token] ?: 1 
        }
        
        return result
    }

    private fun runHeuristicAnalysis(text: String): Pair<Float, List<FraudSignal>> {
        val cleaned = text.lowercase()
        var total = 0f
        val triggered = mutableListOf<FraudSignal>()

        for ((name, patterns) in HEURISTIC_SIGNALS) {
            val hits = patterns.count { cleaned.contains(it) }
            if (hits > 0) {
                total += min(hits * 0.15f, 0.40f)
                triggered.add(FraudSignal(name, "Detected $name marker (Pattern Match: $hits)", SignalSeverity.MEDIUM))
            }
        }
        
        if (text.contains("http") || text.contains("bit.ly")) {
            total += 0.25f
            triggered.add(FraudSignal("Digital Fingerprint", "Suspicious URL redirection detected", SignalSeverity.HIGH))
        }

        return Pair(total, triggered)
    }
}
