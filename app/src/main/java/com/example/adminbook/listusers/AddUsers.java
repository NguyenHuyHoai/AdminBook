package com.example.adminbook.listusers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddUsersBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;


public class AddUsers extends BottomSheetDialogFragment {

    private FragmentAddUsersBinding binding;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference usersCollection = firebaseFirestore.collection("Users");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    // Đường dẫn đến thư mục "avatar" và tập tin "image.jpg"
    private StorageReference imageRef = storageRef.child("avatars/image.jpg");
    private ProgressDialog progressDialog;
    public static AddUsers newInstance() {
        return new AddUsers();
    }
    public interface OnAdminAddedListener {
        void onAdminAdded();
    }
    private AddUsers.OnAdminAddedListener adminAddedListener;

    public void setOnAdminAddedListener(AddUsers.OnAdminAddedListener listener) {
        this.adminAddedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddUsersBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());

        binding.cbShowpass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updatePasswordVisibility(isChecked);
            }
        });
        updatePasswordVisibility(binding.cbShowpass.isChecked());
        setupButton();
        binding.btnRegisterMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usersname = binding.eTUser.getText().toString();
                String email = binding.eTEmail.getText().toString();
                String password = binding.eTPass.getText().toString();
                String Rpass = binding.eTRPass.getText().toString();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.endsWith("@gmail.com")) {
                    binding.eTEmail.setError("Email không hợp lệ");
                    binding.eTEmail.requestFocus();
                    return;
                }
                String passwordPattern = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$";
                if (!password.matches(passwordPattern)) {
                    binding.eTPass.setError("Mật khẩu trên 6 kí tự và có cả chữ lẫn số!");
                    binding.eTPass.requestFocus();
                    return;
                }
                if (!password.equals(Rpass)) {
                    binding.eTRPass.setError("Mật khẩu không khớp");
                    binding.eTRPass.requestFocus();
                    return;
                }

                progressDialog.show();
                usersCollection.whereEqualTo("email", email).get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.getResult() != null && !task.getResult().isEmpty()) {
                                    // Username đã tồn tại, hiển thị thông báo lỗi
                                    binding.tvError.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                    binding.tvError.setText("Email đã được sử dụng!");
                                    progressDialog.cancel();
                                } else {
                                    usersCollection.whereEqualTo("usersName", usersname).get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.getResult() != null && !task.getResult().isEmpty()) {
                                                        // Username đã tồn tại, hiển thị thông báo lỗi
                                                        binding.tvError.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                                        binding.tvError.setText("Tên tài khoản đã được sử dụng!");
                                                        progressDialog.cancel();
                                                    } else {
                                                        createUser(email, password, usersname);
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }
        });
        return view;
    }

    private void createUser(String email, String password, String usersname) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        firebaseAuth.getCurrentUser().sendEmailVerification();
                        String userId = authResult.getUser().getUid();

                        // Lấy link (URL) của ảnh
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                usersCollection.document(userId)
                                        .set(new ItemUsers(userId, imageUrl, usersname, email, "Nam",
                                                "", true,
                                                false, new ArrayList<>(), new ArrayList<>()));

                                // Gọi phương thức của interface để thông báo cho ListGenresFragment
                                if (adminAddedListener != null) {
                                    adminAddedListener.onAdminAdded();
                                }
                                if (isAdded() && getActivity() != null) {
                                    progressDialog.cancel();
                                    showSuccessDialog();
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.cancel();
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
                        dismiss();
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
                        dismiss();
                    }
                })
                .show();
    }

    // show Pass
    private void updatePasswordVisibility(boolean showPassword) {
        int inputType = showPassword ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

        binding.eTPass.setInputType(inputType);
        binding.eTRPass.setInputType(inputType);
    }
    //Setup button
    private void setupButton() {
        checkButtonState();
        // Thêm TextChangedListener cho các EditText
        binding.eTPass.addTextChangedListener(textWatcher);
        binding.eTRPass.addTextChangedListener(textWatcher);
        binding.eTUser.addTextChangedListener(textWatcher);
        binding.eTEmail.addTextChangedListener(textWatcher);
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
        String email = binding.eTEmail.getText().toString().trim();
        String newpass = binding.eTPass.getText().toString().trim();
        String rnewpass = binding.eTRPass.getText().toString().trim();
        String usersName = binding.eTUser.getText().toString().trim();

        boolean isAnyFieldEmpty = email.isEmpty() || newpass.isEmpty() || rnewpass.isEmpty() || usersName.isEmpty();

        if (isAnyFieldEmpty) {
            binding.btnRegisterMain.setEnabled(false);
            binding.btnRegisterMain.setBackgroundResource(R.drawable.button_disabled);
        } else {
            binding.btnRegisterMain.setEnabled(true);
            binding.btnRegisterMain.setBackgroundResource(R.drawable.button_enabled);
        }
    }
}