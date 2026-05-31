package com.example.domainhunterlite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.domainhunterlite.databinding.ItemDomainBinding

class DomainAdapter : ListAdapter<ClassifiedDomain, DomainAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemDomainBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(domain: ClassifiedDomain) {
            binding.tvDomain.text = domain.domain
            
            // تعيين اللون والنص حسب النوع
            when (domain.type) {
                DomainType.EMPTY -> {
                    binding.tvType.text = "📄 Empty"
                    binding.tvType.setTextColor(binding.root.context.getColor(R.color.text_secondary))
                }
                DomainType.PARKED -> {
                    binding.tvType.text = "💰 For Sale"
                    binding.tvType.setTextColor(binding.root.context.getColor(R.color.warning))
                }
                DomainType.ACTIVE -> {
                    binding.tvType.text = "🌐 Active"
                    binding.tvType.setTextColor(binding.root.context.getColor(R.color.success))
                }
            }
            
            binding.btnOpen.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${domain.domain}"))
                it.context.startActivity(intent)
            }
            
            binding.btnCopy.setOnClickListener {
                val clipboard = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("domain", domain.domain))
                Toast.makeText(it.context, "Copied!", Toast.LENGTH_SHORT).show()
            }
            
            binding.btnDetails.setOnClickListener {
                Toast.makeText(it.context, "Status: ${domain.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDomainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ClassifiedDomain>() {
        override fun areItemsTheSame(oldItem: ClassifiedDomain, newItem: ClassifiedDomain) =
            oldItem.domain == newItem.domain

        override fun areContentsTheSame(oldItem: ClassifiedDomain, newItem: ClassifiedDomain) =
            oldItem == newItem
    }
}
