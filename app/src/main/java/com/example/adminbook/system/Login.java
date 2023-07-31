package com.example.adminbook.system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.adminbook.MainActivity;
import com.example.adminbook.R;
import com.example.adminbook.databinding.ActivityLoginBinding;
import com.example.adminbook.listusers.ItemUsers;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Login extends AppCompatActivity {

    ActivityLoginBinding binding;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    CollectionReference usersCollection = firebaseFirestore.collection("Users");
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        progressDialog = new ProgressDialog(this);

        //nut Login
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.eTEmail.getText().toString();
                String password = binding.eTPass.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    binding.eTEmail.setError("Email không được để trống");
                    binding.eTEmail.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    binding.eTPass.setError("Mật khẩu không được để trống");
                    binding.eTPass.requestFocus();
                    return;
                }

                progressDialog.show();
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    if (user != null) {
                                        if (user.isEmailVerified()) {
                                            checkAccount(email);
                                        } else {
                                            binding.tvError.setText(""); // Xóa thông báo lỗi trước đó (nếu có)
                                            binding.tvError.setText("Vui lòng vào email xác minh rồi đăng nhập lại!");
                                            progressDialog.cancel();

                                        }
                                    }
                                } else {
                                    // Đăng nhập thất bại, xử lý tùy ý
                                    Toast.makeText(Login.this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show();
                                    progressDialog.cancel();
                                }
                            }
                        });
            }
        });

        //nut Register
        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, Register.class));
            }
        });

        //nut forgot password
        binding.tVForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, ForgotPass.class));
            }
        });
    }
    private void checkAccount(String email) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = usersCollection.document(userId);
            userRef.get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    boolean isAdmin = document.getBoolean("admin");
                                    boolean isLocked = document.getBoolean("locked");
                                    String avatar = document.getString("avatar");
                                    String usersName = document.getString("usersName");
                                    String gender = document.getString("gender");
                                    String describe = document.getString("describe");
                                    String email = document.getString("email");
                                    String usersId = document.getString("usersId");
                                    List<String> followedBooks = (List<String>) document.get("followedBooks");
                                    List<String> recentlyViewedBooks = (List<String>) document.get("recentlyViewedBooks");

                                    ItemUsers itemUsers = new ItemUsers(usersId, avatar, usersName,
                                            email, gender, describe, isAdmin, isLocked, followedBooks, recentlyViewedBooks);

                                    Gson gson = new Gson();
                                    String itemUsersJson = gson.toJson(itemUsers);

                                    if (isLocked) {
                                        binding.tvError.setText("");
                                        binding.tvError.setText("Tài khoản đã bị khóa!");
                                        progressDialog.cancel();
                                    } else if (!isAdmin) {
                                        binding.tvError.setText("");
                                        binding.tvError.setText("Tài khoản không có quyền truy cập!");
                                        progressDialog.cancel();
                                    } else {
                                        Intent intent = new Intent(Login.this, MainActivity.class);
                                        intent.putExtra("itemUsersJson", itemUsersJson);
                                        startActivity(intent);
                                        progressDialog.cancel();
                                        finish();
                                    }
                                }
                            }
                        }
                    });
        }
    }
}