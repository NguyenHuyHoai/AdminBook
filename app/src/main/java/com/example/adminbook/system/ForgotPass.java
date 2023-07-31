package com.example.adminbook.system;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.ActivityForgotPassBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPass extends AppCompatActivity {

    ActivityForgotPassBinding binding;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPassBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);

        binding.btnSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.eTEmail.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    binding.eTEmail.setError("Email không được để trống");
                    binding.eTEmail.requestFocus();
                    return;
                }
                progressDialog.setTitle("Đang gửi Email !!! ");
                progressDialog.show();
                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                progressDialog.cancel();
                                Toast.makeText(ForgotPass.this,
                                        "Email đã được gửi thành công !", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.cancel();
                                Toast.makeText(ForgotPass.this,
                                        "Email chưa đăng ký hoặc không tồn tại !", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        binding.tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ForgotPass.this, Login.class));
            }
        });
    }
}