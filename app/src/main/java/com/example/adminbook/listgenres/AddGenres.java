package com.example.adminbook.listgenres;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddGenresBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AddGenres extends BottomSheetDialogFragment {
    FragmentAddGenresBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference genresCollectionRef = db.collection("Genres");

    public static AddGenres newInstance() {
        return new AddGenres();
    }
    public interface OnGenresAddedListener {
        void onGenresAdded();
    }
    private OnGenresAddedListener genresAddedListener;

    public void setOnGenresAddedListener(OnGenresAddedListener listener) {
        this.genresAddedListener = listener;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddGenresBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);
        setupButton();

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String genresName = binding.tvAddNG.getText().toString();
                checkAndAddGenresToFirestore(genresName);
//                // Đóng bottom sheet sau khi thêm thành công
//                dismiss();
            }
        });
        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return view;
    }
    //Setup Button
    private void setupButton() {
        checkButtonState();
        // Thêm TextChangedListener cho các EditText
        binding.tvAddNG.addTextChangedListener(textWatcher);
    }
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Khi text của bất kỳ EditText nào thay đổi, cập nhật trạng thái nút Change Password
            checkButtonState();
        }
        @Override
        public void afterTextChanged(Editable s) {
        }
    };
    private void checkButtonState() {
        String genresName = binding.tvAddNG.getText().toString().trim();

        if (genresName.isEmpty()) {
            binding.btnAdd.setEnabled(false);
            binding.btnAdd.setBackgroundResource(R.drawable.button_disabled);
        } else {
            binding.btnAdd.setEnabled(true);
            binding.btnAdd.setBackgroundResource(R.drawable.button_enabled);
        }
    }
    //Add Genres
    private void checkAndAddGenresToFirestore(String genresName) {
        // Kiểm tra xem genresName đã có trong Firestore chưa
        genresCollectionRef.whereEqualTo("genresName", genresName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Nếu có kết quả trả về, genresName đã tồn tại
                            if (!task.getResult().isEmpty()) {
                                if (isAdded() && getActivity() != null) {
                                    binding.tvErrorAG.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                    binding.tvErrorAG.setText("Tên thể loại đã tồn tại!");
                                }
                            } else {
                                // Nếu không có kết quả trả về, tiến hành thêm mới genres
                                addGenresToFirestore(genresName);
                            }
                        } else {
                            // Xử lý khi có lỗi xảy ra trong quá trình truy vấn
                            if (isAdded() && getActivity() != null) {
                                showFailureDialog();
                            }
                        }
                    }
                });
    }

    private void addGenresToFirestore(String genresName) {
        // Tạo một document mới với các thông tin genres
        Map<String, Object> genresData = new HashMap<>();
        genresData.put("genresName", genresName);

        // Thêm document vào collection "Genres" trong Firestore
        genresCollectionRef.add(genresData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String genresId = documentReference.getId();
                        documentReference.update("genresId", genresId);

                        // Gọi phương thức của interface để thông báo cho ListGenresFragment
                        if (genresAddedListener != null) {
                            genresAddedListener.onGenresAdded();
                        }
                        if (isAdded() && getActivity() != null) {
                            showSuccessDialog();
                        }
                        binding.tvAddNG.setText("");
                        binding.tvErrorAG.setText("");
                        setupButton();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showFailureDialog();
                    }
                });
    }
    // Hàm hiển thị Dialog thành công và thất bại
    private void showFailureDialog() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.icon_not_verified)
                .setTitle("Bạn đã thêm thất bại!")
                .setPositiveButton("Đóng", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
    private void showSuccessDialog() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.icon_verified)
                .setTitle("Bạn đã thêm thành công!")
                .setPositiveButton("Đóng", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}