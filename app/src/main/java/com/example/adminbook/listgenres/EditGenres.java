package com.example.adminbook.listgenres;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentEditGenresBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EditGenres extends BottomSheetDialogFragment {
    FragmentEditGenresBinding editGenresBinding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference genresCollectionRef = db.collection("Genres");
    private CollectionReference booksCollectionRef = db.collection("Books");

    private String newGenres;
    private String oldGenres ;
    private boolean isContentChanged = false;
    //Load data
    public interface OnGenresEditedListener {
        void onGenresEdited();
    }
    private EditGenres.OnGenresEditedListener genresEditedListener;
    public void setOnGenresEditedListener(EditGenres.OnGenresEditedListener listener) {
        this.genresEditedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        editGenresBinding = FragmentEditGenresBinding.inflate(inflater, container, false);
        View view = editGenresBinding.getRoot();
        setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);

        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            String genresName = bundle.getString("genresName", "");
            editGenresBinding.tvUpdateNG.setText(genresName);
        }
        oldGenres = editGenresBinding.tvUpdateNG.getText().toString();

        // Thêm sự kiện text change listener để kiểm tra sự thay đổi của nội dung
        editGenresBinding.tvUpdateNG.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Kiểm tra sự thay đổi của nội dung
                isContentChanged = !editGenresBinding.tvUpdateNG.getText().toString().trim().isEmpty();
                // Enable/disable btnUpdate và áp dụng style tùy theo trạng thái sự thay đổi
                if (isContentChanged) {
                    editGenresBinding.btnUpdate.setEnabled(true);
                    editGenresBinding.btnUpdate.setBackgroundResource(R.drawable.button_enabled);
                } else {
                    editGenresBinding.btnUpdate.setEnabled(false);
                    editGenresBinding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);
                }
            }
        });
        editGenresBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        editGenresBinding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newGenresName = editGenresBinding.tvUpdateNG.getText().toString().trim();
                // Kiểm tra nếu nội dung không bị thay đổi hoặc là rỗng, không làm gì cả
                if (!isContentChanged || newGenresName.isEmpty()) {
                    dismiss();
                    return;
                }
                checkGenresNameExist(newGenresName);
            }
        });
        return view;
    }
    private void checkGenresNameExist(String newGenresName) {
        genresCollectionRef
                .whereEqualTo("genresName", newGenresName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Nếu có kết quả trả về (tức là tên thể loại đã tồn tại)
                            if (!task.getResult().isEmpty()) {
                                Toast.makeText(getContext(), "Tên thể loại đã tồn tại!", Toast.LENGTH_SHORT).show();
                            } else {
                                // Tên thể loại chưa tồn tại, tiến hành cập nhật lên Firestore
                                updateGenresName(newGenresName);
                            }
                        } else {
                            // Xử lý lỗi nếu có
                            Toast.makeText(getContext(), "Lỗi kiểm tra tên thể loại!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void updateGenresName(String newGenresName) {
        // Lấy dữ liệu của item đang được hiển thị trong EditGenres
        Bundle bundle = getArguments();
        newGenres =  newGenresName;
        if (bundle != null) {
            String genresId = bundle.getString("genresId", "");
            if (!genresId.isEmpty()) {
                Map<String, Object> updatedData = new HashMap<>();
                updatedData.put("genresName", newGenresName);
                genresCollectionRef
                        .document(genresId)
                        .update(updatedData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Query query = booksCollectionRef.whereArrayContains("genres", oldGenres);
                                query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            List<String> genresList = (List<String>) documentSnapshot.get("genres");
                                            if (genresList != null) {
                                                int index = genresList.indexOf(oldGenres);
                                                if (index != -1) {
                                                    genresList.set(index, newGenresName);
                                                    // Update the document with the modified list
                                                    documentSnapshot.getReference().update("genres", genresList);
                                                }
                                            }
                                        }
                                    }
                                });

                                Toast.makeText(getContext(), "Cập nhật thành công!",
                                        Toast.LENGTH_SHORT).show();

                                if (genresEditedListener != null) {
                                    genresEditedListener.onGenresEdited();
                                }
                                dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

}
