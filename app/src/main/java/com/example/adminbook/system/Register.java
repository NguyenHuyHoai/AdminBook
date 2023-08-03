package com.example.adminbook.system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.ActivityRegisterBinding;
import com.example.adminbook.listusers.ItemUsers;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirestoreRegistrar;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Register extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference usersCollection = firebaseFirestore.collection("Users");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference().child("avatars");
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);

        //nut Register
        binding.btnRegisterMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usersname = binding.eTUser.getText().toString();
                String email = binding.eTEmail.getText().toString();
                String password = binding.eTPass.getText().toString();
                String Rpass = binding.eTRPass.getText().toString();

                if (TextUtils.isEmpty(usersname)) {
                    binding.eTUser.setError("Tên tài khoản không được để trống");
                    binding.eTUser.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    binding.eTEmail.setError("Email không được để trống");
                    binding.eTEmail.requestFocus();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.endsWith("@gmail.com")) {
                    binding.eTEmail.setError("Email không hợp lệ");
                    binding.eTEmail.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    binding.eTPass.setError("Mật khẩu không được để trống");
                    binding.eTPass.requestFocus();
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
                                    binding.tvErrorDK.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                    binding.tvErrorDK.setText("Email đã được sử dụng!");
                                    progressDialog.cancel();
                                } else {
                                    usersCollection.whereEqualTo("usersName", usersname).get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.getResult() != null && !task.getResult().isEmpty()) {
                                                        // Username đã tồn tại, hiển thị thông báo lỗi
                                                        binding.tvErrorDK.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                                        binding.tvErrorDK.setText("Tên tài khoản đã được sử dụng!");
                                                        progressDialog.cancel();
                                                    } else {
                                                        createUser(email, password, usersname);
                                                    }
                                                }
                                            });
                                }
                            }
                        });

//                firebaseAuth.createUserWithEmailAndPassword(email, password)
//                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
//                            @Override
//                            public void onSuccess(AuthResult authResult) {
//                                firebaseAuth.getCurrentUser().sendEmailVerification();
//                                String userId = authResult.getUser().getUid();
//                                // Ví dụ: Tải hình ảnh từ một URI local
//                                String imageResourceName = "default_image";
//                                String imageUri = "android.resource://" + getPackageName() + "/drawable/" + imageResourceName;
//                                StorageReference imageRef = storageRef.child("avatar.jpg");
//                                UploadTask uploadTask = imageRef.putFile(Uri.parse(imageUri));
//                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                                            @Override
//                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                                                // Hình ảnh đã được tải lên thành công
//                                                // Lấy liên kết của hình ảnh từ TaskSnapshot
//                                                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                                                    @Override
//                                                    public void onSuccess(Uri uri) {
//                                                        String imageUrl = uri.toString();
////                                                        saveUserToFirestore(userId, username, email, imageUrl);
//                                                        usersCollection.document(userId)
//                                                                .set(new ItemUsers(userId, imageUrl, usersname, email, "Nam", "", false,
//                                                                        false, new ArrayList<>(), new ArrayList<>()));
//                                                        progressDialog.cancel();
//                                                    }
//                                                });
//                                            }
//                                        });
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Toast.makeText(Register.this, "Đăng kí thất bại!", Toast.LENGTH_SHORT).show();
//                                progressDialog.cancel();
//                            }
//                        });
            }
        });
        //nut Back
        binding.tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Register.this, Login.class));
            }
        });
    }
    private void createUser(String email, String password, String usersname) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        firebaseAuth.getCurrentUser().sendEmailVerification();
                        String userId = authResult.getUser().getUid();
                        // Ví dụ: Tải hình ảnh từ một URI local
                        String imageResourceName = "icon_dep";
                        String imageUri = "android.resource://" + getPackageName() + "/drawable/" + imageResourceName;
                        StorageReference imageRef = storageRef.child("image.jpg");
                        UploadTask uploadTask = imageRef.putFile(Uri.parse(imageUri));
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Hình ảnh đã được tải lên thành công
                                // Lấy liên kết của hình ảnh từ TaskSnapshot
                                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String imageUrl = uri.toString();
//                                                        saveUserToFirestore(userId, username, email, imageUrl);
                                        usersCollection.document(userId)
                                                .set(new ItemUsers(userId, imageUrl, usersname, email, "Nam", "", false,
                                                        false, new ArrayList<>(), new ArrayList<>()));
                                        startActivity(new Intent(Register.this, Login.class));
                                        progressDialog.cancel();
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Register.this, "Đăng kí thất bại!", Toast.LENGTH_SHORT).show();
                        progressDialog.cancel();
                    }
                });
    }
}