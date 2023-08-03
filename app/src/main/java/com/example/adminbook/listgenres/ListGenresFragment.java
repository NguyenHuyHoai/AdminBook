package com.example.adminbook.listgenres;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.adminbook.databinding.FragmentListGenresBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListGenresFragment extends Fragment {
    private FragmentListGenresBinding binding;
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference genresCollection = firebaseFirestore.collection("Genres");
    private GenresAdapter adapter;
    private List<ItemGenres> genresList;
    public interface OnGenresListener {
        void onGenres();
    }
    private OnGenresListener genresListener;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListGenresBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        genresList = new ArrayList<>();
        adapter = new GenresAdapter(genresList, getContext()); // Thêm dòng này để khởi tạo adapter
        binding.recyclerViewGenres.setAdapter(adapter);
        binding.recyclerViewGenres.setLayoutManager(new LinearLayoutManager(getContext()));

        // Thiết lập listener để nhận sự kiện click từ Adapter
        adapter.setGenresItemClickListener(new GenresAdapter.OnGenresItemClickListener() {
            @Override
            public void onGenresItemClick(ItemGenres genresItem) {
                showEditGenresBottomSheet(genresItem);
            }
        });

        loadGenresData();

        binding.btnAddGenres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddGenresBottomSheet();
            }
        });
        binding.edSearchGenres.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Không cần xử lý trước khi thay đổi văn bản
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Xử lý khi có thay đổi văn bản
                String searchText = charSequence.toString().trim().toLowerCase();
                filterGenresList(searchText);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Không cần xử lý sau khi thay đổi văn bản
            }
        });
        return view;
    }

    private void filterGenresList(String searchText) {
        List<ItemGenres> filteredList = new ArrayList<>();
        for (ItemGenres genresItem : genresList) {
            if (genresItem.getGenresName().toLowerCase().contains(searchText)) {
                filteredList.add(genresItem);
            }
        }

        // Cập nhật danh sách hiển thị trên RecyclerView
        adapter.setGenresList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void loadGenresData() {
        firebaseFirestore.collection("Genres")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            genresList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ItemGenres genresItem = document.toObject(ItemGenres.class);
                                genresList.add(genresItem);
                            }
                            adapter.notifyDataSetChanged();

                            // Gọi phương thức của interface để thông báo cho ListGenresFragment
                            if (genresListener != null) {
                                genresListener.onGenres();
                            }
                        }
                    }
                });
    }
    private void showAddGenresBottomSheet() {
        AddGenres bottomSheetFragment = AddGenres.newInstance();
        bottomSheetFragment.setOnGenresAddedListener(new AddGenres.OnGenresAddedListener() {
            @Override
            public void onGenresAdded() {
                // Gọi phương thức load lại dữ liệu sau khi thêm thành công
                loadGenresData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }
    private void showEditGenresBottomSheet(ItemGenres genresItem) {
        EditGenres bottomSheetFragment = new EditGenres();

        // Truyền dữ liệu của item vào EditGenres thông qua Bundle
        Bundle bundle = new Bundle();
        bundle.putString("genresName", genresItem.getGenresName());
        bundle.putString("genresId", genresItem.getGenresId());
        bottomSheetFragment.setArguments(bundle);

        // Thiết lập listener để nhận kết quả cập nhật từ EditGenres
        bottomSheetFragment.setOnGenresEditedListener(new EditGenres.OnGenresEditedListener() {
            @Override
            public void onGenresEdited() {
                loadGenresData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }
}