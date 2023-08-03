package com.example.adminbook.listchapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.adminbook.databinding.FragmentListChapterBinding;
import com.example.adminbook.listbooks.BooksAdapter;
import com.example.adminbook.listbooks.InformationBook;
import com.example.adminbook.listbooks.ItemBooks;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                showDeleteChapterDialog(position);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewChapters);

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddChapterFragment();
            }
        });

        // Thiết lập listener để nhận sự kiện click từ Adapter
        adapter.setChaptersItemClickListener(new ChaptersAdapter.OnChaptersItemClickListener() {
            @Override
            public void onChaptersItemClick(ItemChapters chaptersItem) {
                showEditChapters(chaptersItem);
            }
        });

        return view;
    }

    private void showEditChapters(ItemChapters chaptersItem) {
        EditChapter bottomSheetFragment = new EditChapter();

        Bundle bundle = new Bundle();
        bundle.putString("booksId", chaptersItem.getBooksId());
        bundle.putString("chaptersId", chaptersItem.getChaptersId());
        bundle.putString("chaptersName", chaptersItem.getChaptersName());
        bundle.putString("chaptersContent", chaptersItem.getChaptersContent());
        String chaptersTimeString = chaptersItem.getChaptersTime().toString();
        bundle.putString("chaptersTime", chaptersTimeString);
        bottomSheetFragment.setArguments(bundle);

        // Thiết lập listener để nhận kết quả cập nhật từ EditGenres
        bottomSheetFragment.setOnChaptersEditedListener(new EditChapter.OnChaptersEditedListener() {
            @Override
            public void onChaptersEdited() {
                loadChaptersData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    private void showDeleteChapterDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Xác nhận xoá");
        builder.setMessage("Bạn có chắc chắn muốn xoá không?");
        builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteChapterFromFirestore(position);
                adapter.removeItem(position);
            }
        });
        builder.setNegativeButton("Đóng", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                adapter.notifyItemChanged(position);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                adapter.notifyItemChanged(position);
            }
        });
        builder.show();
    }
    private void deleteChapterFromFirestore(int position) {
        if (chaptersList == null || chaptersList.isEmpty() || position < 0 || position >= chaptersList.size()) {
            return;
        }

        ItemChapters chaptersItem = chaptersList.get(position);
        String chapterNameToDelete = chaptersItem.getChaptersName();
        String booksIdToDeleteChapterFrom = chaptersItem.getBooksId();

        // Query to find documents in "Books" collection where "chapter" array contains chapterNameToDelete and "booksId" matches the given booksIdToDeleteChapterFrom
        booksCollection.whereArrayContains("chapter", chapterNameToDelete)
                .whereEqualTo("booksId", booksIdToDeleteChapterFrom)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Get the chapter array from the document
                                List<String> chaptersArray = (List<String>) document.get("chapter");

                                // Remove the chapterNameToDelete from the array
                                if (chaptersArray != null) {
                                    chaptersArray.remove(chapterNameToDelete);
                                }

                                // Update the document with the modified chapter array
                                document.getReference().update("chapter", chaptersArray);
                            }

                            // After updating "chapter" array in "Books" collection, delete the corresponding chapter from "Chapters" collection
                            chaptersCollection.document(chaptersItem.getChaptersId())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                                chaptersCollection
                                                    .document(chaptersItem.getChaptersId())
                                                    .delete()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Handle the failure case when deleting from "Chapters" collection
                                            Toast.makeText(getContext(), "Lỗi xóa dữ liệu chương!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // Handle the failure case when fetching data from "Books" collection
                            Toast.makeText(getContext(), "Có lỗi xảy ra trong quá trình lấy dữ liệu!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    //Open ListChapter
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
        chaptersCollection.whereEqualTo("booksId", booksId).get()
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