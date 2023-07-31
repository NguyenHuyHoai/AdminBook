package com.example.adminbook.listusers;

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
import com.example.adminbook.databinding.ItemUsersBinding;
import com.example.adminbook.listbooks.BooksAdapter;
import com.example.adminbook.listbooks.ItemBooks;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder>{
    private List<ItemUsers> usersList;
    private Context context;

    // Khai báo interface để lắng nghe sự kiện click trên item
    public interface OnUsersItemClickListener {
        void onUsersItemClick(ItemUsers usersItem);
    }
    private UsersAdapter.OnUsersItemClickListener usersItemClickListener;
    // Setter để thiết lập listener từ ListGenresFragment
    public void setUsersItemClickListener(UsersAdapter.OnUsersItemClickListener listener) {
        this.usersItemClickListener = listener;
    }

    public UsersAdapter(List<ItemUsers> usersList, Context context) {
        this.usersList = usersList;
        this.context = context;
    }
    @NonNull
    @Override
    public UsersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemUsersBinding binding = ItemUsersBinding.inflate(layoutInflater, parent, false);

        return new UsersAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.ViewHolder holder, int position) {
        ItemUsers usersItem = usersList.get(position);
        holder.bind(usersItem);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemUsersBinding binding;

        public ViewHolder(ItemUsersBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.itemUsers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ItemUsers usersItem = usersList.get(position);
                        if (usersItemClickListener != null) {
                            usersItemClickListener.onUsersItemClick(usersItem);
                        }
                    }
                }
            });
        }
        public void bind(ItemUsers usersItem) {
            binding.tvUsersName.setText(usersItem.getUsersName());
            String adminMessage = usersItem.getAdmin() ? "Admin" : "Reader";
            binding.tvQuyen.setText(adminMessage);
            Glide.with(itemView.getContext())
                    .load(usersItem.getAvatar())
                    .placeholder(R.drawable.icon_load)
                    .error(R.drawable.icon_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.titleImageBook);
        }
    }

}
