package com.example.adminbook.listusers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentListUsersBinding;
import com.example.adminbook.listbooks.BooksAdapter;
import com.example.adminbook.listbooks.ItemBooks;
import com.example.adminbook.listbooks.ListBooksFragment;
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

public class ListUsersFragment extends Fragment {
    FragmentListUsersBinding binding;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    CollectionReference usersCollection = firebaseFirestore.collection("Users");
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference userstorageRef = storage.getReference().child("avatars");
    private UsersAdapter adapter;
    private List<ItemUsers> usersList;
    public interface OnUsersAddedListener {
        void onUsersAdded();
    }
    private ListUsersFragment.OnUsersAddedListener usersAddedListener;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListUsersBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        usersList = new ArrayList<>();
        adapter = new UsersAdapter(usersList, getContext());
        binding.recyclerViewUsers.setAdapter(adapter);
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        // Thiết lập listener để nhận sự kiện chọn từ Spinner
        binding.spinnerOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                loadUsersData(selectedOption);
            };
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Thiết lập listener để nhận sự kiện click từ Adapter
        adapter.setUsersItemClickListener(new UsersAdapter.OnUsersItemClickListener() {
            @Override
            public void onUsersItemClick(ItemUsers usersItem) {
                showIngormationBottomSheet(usersItem);
            }
        });

        binding.btnAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        return view;
    }

    private void loadUsersData(String selectedOption) {
        Query query;
        if (selectedOption.equals("Tổng")) {
            query = usersCollection.orderBy("usersName", Query.Direction.ASCENDING);
        } else if (selectedOption.equals("Danh sách admin")) {
            query = usersCollection.whereEqualTo("admin", true);
        } else if (selectedOption.equals("Danh sách người đọc")) {
            query = usersCollection.whereEqualTo("admin", false);
        } else if (selectedOption.equals("Danh sách bị khóa")) {
            query = usersCollection.whereEqualTo("locked", true);
        } else {
            // Mặc định nếu không có lựa chọn phù hợp
            return;
        }

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    usersList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ItemUsers usersItem = document.toObject(ItemUsers.class);
                        usersList.add(usersItem);
                    }
                    adapter.notifyDataSetChanged();

                    // Gọi phương thức của interface để thông báo cho ListGenresFragment
                    if (usersAddedListener != null) {
                        usersAddedListener.onUsersAdded();
                    }
                }
            }
        });
    }
    private void showIngormationBottomSheet(ItemUsers usersItem) {
    }
}