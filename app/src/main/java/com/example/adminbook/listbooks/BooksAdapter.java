package com.example.adminbook.listbooks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.adminbook.R;
import com.example.adminbook.databinding.ItemBooksBinding;
import com.example.adminbook.listgenres.ItemGenres;

import java.util.List;

public class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.ViewHolder>{
    private List<ItemBooks> booksList;
    private Context context;

    // Khai báo interface để lắng nghe sự kiện click trên item
    public interface OnBooksItemClickListener {
        void onBooksItemClick(ItemBooks booksItem);
    }
    private BooksAdapter.OnBooksItemClickListener booksItemClickListener;
    // Setter để thiết lập listener từ ListGenresFragment
    public void setBooksItemClickListener(BooksAdapter.OnBooksItemClickListener listener) {
        this.booksItemClickListener = listener;
    }

    public BooksAdapter(List<ItemBooks> booksList, Context context) {
        this.booksList = booksList;
        this.context = context;
    }
    @NonNull
    @Override
    public BooksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemBooksBinding binding = ItemBooksBinding.inflate(layoutInflater, parent, false);

        return new BooksAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BooksAdapter.ViewHolder holder, int position) {
        ItemBooks booksItem = booksList.get(position);
        holder.bind(booksItem);
    }

    @Override
    public int getItemCount() {
        return booksList.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemBooksBinding binding;

        public ViewHolder(ItemBooksBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.itembook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ItemBooks booksItem = booksList.get(position);
                        if (booksItemClickListener != null) {
                            booksItemClickListener.onBooksItemClick(booksItem);
                        }
                    }
                }
            });
        }
        public void bind(ItemBooks booksItem) {
            binding.titleNameBook.setText(booksItem.getTitle());
            Glide.with(itemView.getContext())
                    .load(booksItem.getImageBook())
                    .placeholder(R.drawable.icon_load)
                    .error(R.drawable.icon_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.titleImageBook);
        }
    }
    public void removeItem(int position) {
        if (booksList == null || booksList.isEmpty() || position < 0 || position >= booksList.size()) {
            return;
        }
        booksList.remove(position);
        notifyItemRemoved(position);
    }
}
