package com.example.adminbook.listbooks;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.account.information.InformationFragment;
import com.example.adminbook.databinding.FragmentListBooksBinding;
import com.example.adminbook.listgenres.AddGenres;
import com.example.adminbook.listgenres.EditGenres;
import com.example.adminbook.listgenres.GenresAdapter;
import com.example.adminbook.listgenres.ItemGenres;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class ListBooksFragment extends Fragment {
    private FragmentListBooksBinding binding;
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = firebaseFirestore.collection("Books");
    private CollectionReference bookGenresCollection = firebaseFirestore.collection("BookGenres");
    private CollectionReference chaptersCollection = firebaseFirestore.collection("Chapters");

    private BooksAdapter adapter;
    private List<ItemBooks> booksList;

    public interface OnBooksAddedListener {
        void onBooksAdded();
    }
    private ListBooksFragment.OnBooksAddedListener booksAddedListener;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListBooksBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        booksList = new ArrayList<>();
        adapter = new BooksAdapter(booksList, getContext());
        binding.recyclerViewBooks.setAdapter(adapter);
        binding.recyclerViewBooks.setLayoutManager(new LinearLayoutManager(getContext()));

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
                showDeleteConfirmationDialog(position);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewBooks);

        // Thiết lập listener để nhận sự kiện click từ Adapter
        adapter.setBooksItemClickListener(new BooksAdapter.OnBooksItemClickListener() {
            @Override
            public void onBooksItemClick(ItemBooks booksItem) {
                showIngormationBottomSheet(booksItem);
            }
        });

        loadBooksData();
        binding.btnAddBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddBooksBottomSheet();
            }
        });
        binding.edSearchBook.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Không cần xử lý trước khi thay đổi văn bản
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Xử lý khi có thay đổi văn bản
                String searchText = charSequence.toString().trim().toLowerCase();
                filterBooksList(searchText);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Không cần xử lý sau khi thay đổi văn bản
            }
        });
        return view;
    }

    private void filterBooksList(String searchText) {
        List<ItemBooks> filteredList = new ArrayList<>();
        for (ItemBooks booksItem : booksList) {
            if (booksItem.getTitle().toLowerCase().contains(searchText)) {
                filteredList.add(booksItem);
            }
        }

        // Cập nhật danh sách hiển thị trên RecyclerView
        adapter.setBooksList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void showIngormationBottomSheet(ItemBooks booksItem) {
        InformationBook bottomSheetFragment = new InformationBook();

        Bundle bundle = new Bundle();
        bundle.putString("title", booksItem.getTitle());
        bundle.putString("author", booksItem.getAuthor());
        bundle.putString("booksId", booksItem.getBooksId());
        ArrayList<String> genresArrayList = new ArrayList<>(booksItem.getGenres());
        bundle.putStringArrayList("genres", genresArrayList);
        bundle.putString("description", booksItem.getDescription());
        bundle.putString("imageBook", booksItem.getImageBook());
        bundle.putString("CoverImage", booksItem.getCoverImage());
        bottomSheetFragment.setArguments(bundle);

        // Thiết lập listener để nhận kết quả cập nhật từ EditGenres
        bottomSheetFragment.setOnInformationListener(new InformationBook.OnInformationListener() {
            @Override
            public void onUpdateSuccess() {
                loadBooksData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    private void loadBooksData() {
        booksCollection.orderBy("title", Query.Direction.ASCENDING).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            booksList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ItemBooks booksItem = document.toObject(ItemBooks.class);
                                booksList.add(booksItem);
                            }
                            adapter.notifyDataSetChanged();

                            // Gọi phương thức của interface để thông báo cho ListGenresFragment
                            if (booksAddedListener != null) {
                                booksAddedListener.onBooksAdded();
                            }
                        }
                    }
                });
    }
    //Show fragment Add
    private void showAddBooksBottomSheet() {
        AddBooks bottomSheetFragment = AddBooks.newInstance();
        bottomSheetFragment.setOnBooksAddedListener(new AddBooks.OnBooksAddedListener() {
            @Override
            public void onBooksAdded() {
                // Gọi phương thức load lại dữ liệu sau khi thêm thành công
                loadBooksData();
            }
        });
        bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
    }
    private void setupBooksListener() {
        firebaseFirestore.collection("Books")
                .orderBy("title", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return;
                        }
                        if (querySnapshot != null) {
                            // Clear danh sách sách hiện tại
                            booksList.clear();
                            // Lặp qua các documents trong querySnapshot và thêm vào danh sách sách
                            for (DocumentSnapshot document : querySnapshot) {
                                if (document.exists()) {
                                    ItemBooks book = document.toObject(ItemBooks.class);
                                    if (book != null) {
                                        booksList.add(book);
                                    }
                                }
                            }
                            // Cập nhật RecyclerView khi có thay đổi dữ liệu
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
    //Delete ItemBook
    private void showDeleteConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Xác nhận xoá");
        builder.setMessage("Bạn có chắc chắn muốn xoá không?");
        builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteBooksFromFirestore(position);
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
    private void deleteBooksFromFirestore(int position) {
        if (booksList == null || booksList.isEmpty() || position < 0 || position >= booksList.size()) {
            return;
        }
        ItemBooks booksItem = booksList.get(position);
        bookGenresCollection.whereEqualTo("booksId", booksItem.getBooksId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete();

                                chaptersCollection.whereEqualTo("booksId", booksItem.getBooksId()).get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()){
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        document.getReference().delete();
                                                        booksCollection
                                                                .document(booksItem.getBooksId())
                                                                .delete()
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Toast.makeText(getContext(), "Đã xóa!", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                        });
                            }
                        } else {
                            // Xử lý nếu có lỗi xảy ra trong quá trình lấy dữ liệu
                            Toast.makeText(getContext(), "Có lỗi xảy ra trong quá trình lấy dữ liệu!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}