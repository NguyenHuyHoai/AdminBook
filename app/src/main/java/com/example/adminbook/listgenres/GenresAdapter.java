package com.example.adminbook.listgenres;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminbook.databinding.ItemGenresBinding;

import java.util.List;

public class GenresAdapter extends RecyclerView.Adapter<GenresAdapter.ViewHolder> {
    private List<ItemGenres> genresList;
    private Context context;

    // Khai báo interface để lắng nghe sự kiện click trên item
    public interface OnGenresItemClickListener {
        void onGenresItemClick(ItemGenres genresItem);
    }
    private OnGenresItemClickListener genresItemClickListener;
    // Setter để thiết lập listener từ ListGenresFragment
    public void setGenresItemClickListener(OnGenresItemClickListener listener) {
        this.genresItemClickListener = listener;
    }

    public void setGenresList(List<ItemGenres> genresList) {
        this.genresList = genresList;
    }

    public GenresAdapter(List<ItemGenres> genresList, Context context) {
        this.genresList = genresList;
        this.context = context;
    }
    @NonNull
    @Override
    public GenresAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemGenresBinding binding = ItemGenresBinding.inflate(layoutInflater, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GenresAdapter.ViewHolder holder, int position) {
        ItemGenres genresItem = genresList.get(position);
        holder.bind(genresItem);
    }

    @Override
    public int getItemCount() {
        return genresList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemGenresBinding binding;

        public ViewHolder(ItemGenresBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Thêm sự kiện click cho textGenresName
            binding.textGenresName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ItemGenres genresItem = genresList.get(position);
                        if (genresItemClickListener != null) {
                            genresItemClickListener.onGenresItemClick(genresItem);
                        }
                    }
                }
            });
        }
        public void bind(ItemGenres genresItem) {
            binding.textGenresName.setText(genresItem.getGenresName());
        }
    }
}
