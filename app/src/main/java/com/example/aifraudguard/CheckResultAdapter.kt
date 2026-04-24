package com.example.aifraudguard

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CheckResultAdapter : ListAdapter<CheckDetail, CheckResultAdapter.ViewHolder>(CheckDetailDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.check_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCheckIcon: TextView = itemView.findViewById(R.id.tvCheckIcon)
        private val tvCheckName: TextView = itemView.findViewById(R.id.tvCheckName)
        private val tvCheckMessage: TextView = itemView.findViewById(R.id.tvCheckMessage)
        private val tvScoreImpact: TextView = itemView.findViewById(R.id.tvScoreImpact)

        fun bind(detail: CheckDetail, position: Int) {
            tvCheckIcon.text = detail.icon
            tvCheckName.text = detail.name
            tvCheckMessage.text = detail.message
            
            if (detail.score > 0) {
                tvScoreImpact.text = "+${detail.score}"
                tvScoreImpact.setTextColor(Color.parseColor("#F44336"))
            } else {
                tvScoreImpact.text = "0"
                tvScoreImpact.setTextColor(Color.parseColor("#4CAF50"))
            }

            itemView.alpha = 0f
            Handler(Looper.getMainLooper()).postDelayed({
                itemView.animate().alpha(1f).setDuration(300).start()
            }, position * 100L)
        }
    }

    class CheckDetailDiffCallback : DiffUtil.ItemCallback<CheckDetail>() {
        override fun areItemsTheSame(oldItem: CheckDetail, newItem: CheckDetail): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: CheckDetail, newItem: CheckDetail): Boolean {
            return oldItem == newItem
        }
    }
}
