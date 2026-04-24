package com.example.aifraudguard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NewsFragment()         // Left page - News Feed
            1 -> LinkInspectorFragment() // Middle page - Link Check
            2 -> AIAssistantFragment()  // Right page - AI Assistant
            else -> NewsFragment()
        }
    }
}