package com.example.adminbook.listchapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminbook.databinding.ItemChaptersBinding;


import java.util.List;

public class ChaptersAdapter extends RecyclerView.Adapter<ChaptersAdapter.ViewHolder>{

    private List<ItemChapters> chaptersList;
    private Context context;

    // Khai báo interface để lắng nghe sự kiện click trên item
    public interface OnChaptersItemClickListener {
        void onChaptersItemClick(ItemChapters chaptersItem);
    }
    private ChaptersAdapter.OnChaptersItemClickListener chaptersItemClickListener;
    // Setter để thiết lập listener từ ListGenresFragment
    public void setChaptersItemClickListener(ChaptersAdapter.OnChaptersItemClickListener listener) {
        this.chaptersItemClickListener = listener;
    }

    public ChaptersAdapter(List<ItemChapters> chaptersList, Context context) {
        this.chaptersList = chaptersList;
        this.context = context;
    }

    @NonNull
    @Override
    public ChaptersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemChaptersBinding binding = ItemChaptersBinding.inflate(layoutInflater, parent, false);

        return new ChaptersAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChaptersAdapter.ViewHolder holder, int position) {
        ItemChapters chaptersItem = chaptersList.get(position);
        holder.bind(chaptersItem);
    }

    @Override
    public int getItemCount() {
        return chaptersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemChaptersBinding binding;

        public ViewHolder(ItemChaptersBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.tvchapterName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ItemChapters chaptersItem = chaptersList.get(position);
                        if (chaptersItemClickListener != null) {
                            chaptersItemClickListener.onChaptersItemClick(chaptersItem);
                        }
                    }
                }
            });
        }
        public void bind(ItemChapters chaptersItem) {
            binding.tvchapterName.setText(chaptersItem.getChaptersName());
        }
    }
}
