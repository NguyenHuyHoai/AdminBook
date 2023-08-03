package com.example.adminbook.listusers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddUsersBinding;
import com.example.adminbook.databinding.FragmentEditUsersBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class EditUsers extends BottomSheetDialogFragment {

    private FragmentEditUsersBinding binding;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference usersCollection = firebaseFirestore.collection("Users");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    // Đường dẫn đến thư mục "avatar" và tập tin "image.jpg"
    private StorageReference imageRef = storageRef.child("avatars/image.jpg");
    private ProgressDialog progressDialog;

    private boolean initialAdminValue;
    private boolean initialLockedValue;
    private boolean isRadioGroupChanged = false;

    public static EditUsers newInstance() {
        return new EditUsers();
    }
    public interface OnAdminAddedListener {
        void onAdminAdded();
    }
    private EditUsers.OnAdminAddedListener adminAddedListener;

    public void setOnAdminAddedListener(EditUsers.OnAdminAddedListener listener) {
        this.adminAddedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditUsersBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());
        displayBookData();
        setupRadioGroups();
        binding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserData();
            }
        });
        return view;
    }
    private void displayBookData() {
        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            String usersId = bundle.getString("usersId");
            String usersName = bundle.getString("usersName", "");
            binding.eTUser.setText(usersName);
            String email = bundle.getString("email", "");
            binding.eTEmail.setText(email);

            // Lấy ảnh
            String avatar = bundle.getString("avatar");
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.icon_load) // Hình ảnh tạm thời khi đang tải
                    .error(R.drawable.defaultavatar) // Hình ảnh lỗi nếu không tải được
                    .into(binding.image);

            // Lưu trạng thái ban đầu của các RadioButton
            initialAdminValue = "Admin".equals(bundle.getString("admin"));
            initialLockedValue = "Bị khóa".equals(bundle.getString("locked"));

            // Set giá trị ban đầu cho RadioGroup Role
            if (initialAdminValue) {
                binding.radioButtonAdmin.setChecked(true);
            } else {
                binding.radioButtonReader.setChecked(true);
            }

            // Set giá trị ban đầu cho RadioGroup Status
            if (initialLockedValue) {
                binding.radioButtonLocked.setChecked(true);
            } else {
                binding.radioButtonActive.setChecked(true);
            }

//            // Lấy giá trị của admin và locked từ bundle
//            String adminValue = bundle.getString("admin");
//            String lockedValue = bundle.getString("locked");
//
//            // Thiết lập giá trị cho RadioGroup Role
//            if ("Admin".equals(adminValue)) {
//                // Thiết lập lựa chọn Admin cho RadioGroup Role
//                binding.radioButtonAdmin.setChecked(true);
//            } else {
//                // Thiết lập lựa chọn Reader cho RadioGroup Role
//                binding.radioButtonReader.setChecked(true);
//            }
//
//            // Thiết lập giá trị cho RadioGroup Status
//            if ("Bị khóa".equals(lockedValue)) {
//                // Thiết lập lựa chọn Bị khóa cho RadioGroup Status
//                binding.radioButtonLocked.setChecked(true);
//            } else {
//                // Thiết lập lựa chọn Đang hoạt động cho RadioGroup Status
//                binding.radioButtonActive.setChecked(true);
//            }
        }
    }

    private void setupRadioGroups() {
        // Thêm sự kiện cho RadioGroup Role
        binding.radioGroupRole.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                isRadioGroupChanged = true; // Đánh dấu có sự thay đổi trong RadioGroup Role
                enableUpdateButtonIfChanged(); // Kiểm tra và bật/tắt nút "Update"
            }
        });

        binding.radioGroupStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                isRadioGroupChanged = true; // Đánh dấu có sự thay đổi trong RadioGroup Status
                enableUpdateButtonIfChanged(); // Kiểm tra và bật/tắt nút "Update"
            }
        });

    }

    private void enableUpdateButtonIfChanged() {
        boolean isAdmin = binding.radioButtonAdmin.isChecked();
        boolean isLocked = binding.radioButtonLocked.isChecked();

        // So sánh với giá trị ban đầu trong Bundle và kiểm tra có sự thay đổi hay không
        if ((isAdmin != initialAdminValue || isLocked != initialLockedValue) && isRadioGroupChanged) {
            binding.btnUpdate.setEnabled(true);
            binding.btnUpdate.setBackgroundResource(R.drawable.button_enabled);
        } else {
            binding.btnUpdate.setEnabled(false);
            binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);
        }

        // Đặt lại biến isRadioGroupChanged về false
        isRadioGroupChanged = false;
    }

    private void saveUserData() {
        String usersId = getArguments().getString("usersId");
        String usersName = binding.eTUser.getText().toString().trim();
        String email = binding.eTEmail.getText().toString().trim();
        boolean isAdmin = binding.radioButtonAdmin.isChecked();
        boolean isLocked = binding.radioButtonLocked.isChecked();

        // Cập nhật các trường thay đổi vào tài liệu tương ứng trong Firestore
        // Ví dụ: Cập nhật vào tài liệu có id là usersId trong collection "Users"
        DocumentReference userDocRef = firebaseFirestore.collection("Users").document(usersId);
        Map<String, Object> userData = new HashMap<>();

        // Kiểm tra từng trường nếu có thay đổi thì cập nhật vào userData
        if (!usersName.equals(getArguments().getString("usersName", ""))) {
            userData.put("usersName", usersName);
        }
        if (!email.equals(getArguments().getString("email", ""))) {
            userData.put("email", email);
        }
        if (isAdmin != "Admin".equals(getArguments().getString("admin"))) {
            userData.put("admin", isAdmin);
        }
        if (isLocked != "Bị khóa".equals(getArguments().getString("locked"))) {
            userData.put("locked", isLocked);
        }

        userDocRef.update(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Thông báo cho người dùng cập nhật thành công (nếu cần thiết)
                        // Gọi phương thức của interface để thông báo cho ListGenresFragment
                        if (adminAddedListener != null) {
                            adminAddedListener.onAdminAdded();
                        }
                        if (isAdded() && getActivity() != null) {
                            progressDialog.cancel();
                            showSuccessDialog();
                        }
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
                .setTitle("Bạn đã cập nhật thất bại!")
                .setPositiveButton("Đóng", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .show();
    }
    private void showSuccessDialog() {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.icon_verified)
                .setTitle("Bạn đã cập nhật thành công!")
                .setPositiveButton("Đóng", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .show();
    }
}