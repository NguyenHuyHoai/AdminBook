package com.example.adminbook.listchapters;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentListChapterBinding;
import com.example.adminbook.listbooks.BooksAdapter;
import com.example.adminbook.listbooks.ItemBooks;
import com.example.adminbook.listgenres.EditGenres;
import com.example.adminbook.listgenres.ListGenresFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;


public class ListChapterFragment extends Fragment {

    private FragmentListChapterBinding binding;
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = firebaseFirestore.collection("Books");
    private CollectionReference chaptersCollection = firebaseFirestore.collection("Chapters");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference chapterstorageRef = storage.getReference().child("content");

    private ChaptersAdapter adapter;
    private List<ItemChapters> chaptersList;
    private String booksId;

    public interface OnChaptersListener {
        void onChapters();
    }
    private ListChapterFragment.OnChaptersListener chaptersListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListChapterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        displayBookData();
        loadChaptersData();

        chaptersList = new ArrayList<>();
        adapter = new ChaptersAdapter(chaptersList, getContext());
        binding.recyclerViewChapters.setAdapter(adapter);
        binding.recyclerViewChapters.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddChapterFragment();
            }
        });

        return view;
    }

    private void showAddChapterFragment() {
        AddChapter bottomSheetFragment = new AddChapter();

        // Truyền dữ liệu của item vào EditGenres thông qua Bundle
        Bundle bundle = new Bundle();
        bundle.putString("booksId", booksId);
        bottomSheetFragment.setArguments(bundle);

        // Thiết lập listener để nhận kết quả cập nhật từ EditGenres
        bottomSheetFragment.setOnChaptersAddedListener(new AddChapter.OnChaptersAddedListener() {
            @Override
            public void onChaptersAdded() {
                loadChaptersData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    private void displayBookData() {
        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if(bundle != null)
        {
            booksId = bundle.getString("booksId");
        }
    }

    private void loadChaptersData() {
        Log.e("Booksid", booksId);
        chaptersCollection.whereEqualTo("booksId", booksId ).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Xóa dữ liệu hiện tại trong danh sách chương
                            chaptersList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ItemChapters chapterItem = document.toObject(ItemChapters.class);
                                chaptersList.add(chapterItem);
                            }
                            adapter.notifyDataSetChanged();
                            if (chaptersListener != null) {
                                chaptersListener.onChapters();
                            }
                        }
                    }
                });
    }
}