package com.example.aifraudguard

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aifraudguard.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Using View Binding for safe and easy access to your layout's views
    private lateinit var binding: ActivityMainBinding
    private var userName: String = ""
    private var userPhone: String = ""
    private var userEmail: String = ""

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, "Some permissions were not granted.", Toast.LENGTH_LONG).show()
            }
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Toast.makeText(this, "Please check if the setting was enabled.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check authentication BEFORE inflating layout
        if (!AuthHelper.isUserAuthenticated()) {
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user data from SharedPreferences (will be set after authentication)
        val sharedPrefs = getSharedPreferences("FraudGuardPrefs", MODE_PRIVATE)
        userName = sharedPrefs.getString("USER_NAME", null) ?: "Guest User"
        userPhone = sharedPrefs.getString("USER_PHONE_NUMBER", null) ?: "N/A"
        userEmail = sharedPrefs.getString("USER_EMAIL", null) ?: "guest@cyberguard.com"

        // User authenticated, show normal UI
        binding.welcomeText.text = "Welcome, $userName"
        
        // Load profile photo into the top-right header icon
        AuthHelper.getCurrentUser()?.photoUrl?.let { photoUrl ->
            // Clear tint so the photo isn't covered by the default white/gray tint
            binding.profileIcon.imageTintList = null
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.profileIcon)
        }

        binding.profileIcon.setOnClickListener {
            showProfileDialog()
        }
        
        // Setup ViewPager2 with swipeable pages
        setupViewPager()
    }
    
    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Start on News page (index 0)
        binding.viewPager.setCurrentItem(0, false)
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Listen to page changes to update bottom navigation
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigation(position)
            }
        })
    }
    
    private fun setupBottomNavigation() {
        binding.navNews.setOnClickListener {
            binding.viewPager.setCurrentItem(0, true)
        }
        
        binding.navLink.setOnClickListener {
            binding.viewPager.setCurrentItem(1, true)
        }
        
        binding.navAI.setOnClickListener {
            binding.viewPager.setCurrentItem(2, true)
        }

        binding.navMsgScan.setOnClickListener {
            binding.viewPager.setCurrentItem(3, true)
        }
    }
    
    private fun updateBottomNavigation(position: Int) {
        val primary = ContextCompat.getColor(this, R.color.primary)
        val gray = ContextCompat.getColor(this, R.color.gray)
        val bold = android.graphics.Typeface.BOLD
        val normal = android.graphics.Typeface.NORMAL

        // Reset all tabs to gray/normal
        binding.navNewsIcon.setColorFilter(gray)
        binding.navNewsText.setTextColor(gray)
        binding.navNewsText.setTypeface(null, normal)

        binding.navLinkIcon.setColorFilter(gray)
        binding.navLinkText.setTextColor(gray)
        binding.navLinkText.setTypeface(null, normal)

        binding.navAIIcon.setColorFilter(gray)
        binding.navAIText.setTextColor(gray)
        binding.navAIText.setTypeface(null, normal)

        binding.navMsgScanIcon.setColorFilter(gray)
        binding.navMsgScanText.setTextColor(gray)
        binding.navMsgScanText.setTypeface(null, normal)

        // Highlight selected tab
        when (position) {
            0 -> {
                binding.navNewsIcon.setColorFilter(primary)
                binding.navNewsText.setTextColor(primary)
                binding.navNewsText.setTypeface(null, bold)
            }
            1 -> {
                binding.navLinkIcon.setColorFilter(primary)
                binding.navLinkText.setTextColor(primary)
                binding.navLinkText.setTypeface(null, bold)
            }
            2 -> {
                binding.navAIIcon.setColorFilter(primary)
                binding.navAIText.setTextColor(primary)
                binding.navAIText.setTypeface(null, bold)
            }
            3 -> {
                binding.navMsgScanIcon.setColorFilter(primary)
                binding.navMsgScanText.setTextColor(primary)
                binding.navMsgScanText.setTypeface(null, bold)
            }
        }
    }



    private fun showProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_profile_menu, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Get views
        val ivProfilePhoto = dialogView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_profile_photo)
        val tvProfileName = dialogView.findViewById<TextView>(R.id.tv_profile_name)
        val tvProfileEmail = dialogView.findViewById<TextView>(R.id.tv_profile_email)
        val btnViewProfile = dialogView.findViewById<android.widget.LinearLayout>(R.id.btn_view_profile)
        val btnPermissions = dialogView.findViewById<android.widget.LinearLayout>(R.id.btn_permissions)
        val btnLogout = dialogView.findViewById<android.widget.LinearLayout>(R.id.btn_logout)

        // Load user data
        val currentUser = AuthHelper.getCurrentUser()
        tvProfileName.text = userName
        tvProfileEmail.text = userEmail

        // Load profile photo if available
        currentUser?.photoUrl?.let { photoUrl ->
            com.bumptech.glide.Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .into(ivProfilePhoto)
        }

        // Set click listeners
        btnViewProfile.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnPermissions.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, PermissionsSettingsActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            dialog.dismiss()
            showLogoutConfirmation()
        }

        dialog.show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                signOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOut() {
        // Sign out using AuthHelper
        AuthHelper.signOut(this)
        
        // Redirect to AuthActivity
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}