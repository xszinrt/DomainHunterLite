package com.example.domainhunterlite;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import com.example.domainhunterlite.databinding.ItemDomainBinding;
import java.util.ArrayList;
import java.util.List;

public class DomainAdapter extends RecyclerView.Adapter<DomainAdapter.ViewHolder> {
    
    private List<ClassifiedDomain> items = new ArrayList<>();
    
    public void submitList(List<ClassifiedDomain> list) {
        items = list;
        notifyDataSetChanged();
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemDomainBinding binding = ItemDomainBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDomainBinding binding;
        
        ViewHolder(ItemDomainBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(ClassifiedDomain domain) {
            binding.tvDomain.setText(domain.domain);
            
            switch (domain.type) {
                case EMPTY:
                    binding.tvType.setText("📄 Empty");
                    binding.tvType.setTextColor(binding.getRoot().getContext().getColor(R.color.text_secondary));
                    break;
                case PARKED:
                    binding.tvType.setText("💰 For Sale");
                    binding.tvType.setTextColor(binding.getRoot().getContext().getColor(R.color.warning));
                    break;
                case ACTIVE:
                    binding.tvType.setText("🌐 Active");
                    binding.tvType.setTextColor(binding.getRoot().getContext().getColor(R.color.success));
                    break;
            }
            
            binding.btnOpen.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + domain.domain));
                v.getContext().startActivity(intent);
            });
            
            binding.btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("domain", domain.domain));
                Toast.makeText(v.getContext(), "Copied!", Toast.LENGTH_SHORT).show();
            });
            
            binding.btnDetails.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Status: " + domain.statusCode, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
