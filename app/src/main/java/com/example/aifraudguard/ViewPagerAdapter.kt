package com.example.aifraudguard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NewsFragment()           // News Feed
            1 -> LinkInspectorFragment()  // Link Check
            2 -> AIAssistantFragment()    // AI Assistant
            3 -> MessageScanFragment()    // Message Fraud Scanner
            else -> NewsFragment()
        }
    }
}