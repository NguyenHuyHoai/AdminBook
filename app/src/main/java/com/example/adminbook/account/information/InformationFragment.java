package com.example.adminbook.account.information;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import android.Manifest;
import com.bumptech.glide.Glide;
import com.example.adminbook.MainActivity;
import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentInformationBinding;
import com.example.adminbook.listusers.ItemUsers;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class InformationFragment extends Fragment {
    FragmentInformationBinding binding;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    CollectionReference usersCollection = firebaseFirestore.collection("Users");
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference().child("avatars");

    private TextView tvUsenameMP;
    private TextView tvMota;
    private Spinner spinnerGender;
    private Bitmap previousImageBitmap;
    private String previousUsername;
    private String previousGender;
    private String previousMota;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_IMAGE_PICK = 4;

    private ItemUsers itemUsers;
    ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInformationBinding.inflate(inflater, container, false);
        progressDialog = new ProgressDialog(getActivity());
        retrieveUserData();
        displayUserData();
        setupSaveButton();
        binding.btnFileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageChooser();
            }
        });
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                uploadImageToStorage();
            }
        });
        return binding.getRoot();
    }
    //Load Data
    private void retrieveUserData() {
        String userJson = getArguments().getString("itemUsersJson");
        itemUsers = new Gson().fromJson(userJson, ItemUsers.class);
    }
    private void displayUserData() {
        binding.tvUsenameMP.setText(itemUsers.getUsersName());
        binding.tvEmailMP.setText(itemUsers.getEmail());
        binding.tvMota.setText(itemUsers.getDescribe());

        String imageRef = itemUsers.getAvatar();
        Glide.with(this)
                .load(imageRef)
                .placeholder(R.drawable.icon_load) // Hình ảnh tạm thời khi đang tải
                .error(R.drawable.defaultavatar) // Hình ảnh lỗi nếu không tải được
                .into(binding.imageMP);
        if (itemUsers.getGender() != null) {
            if (itemUsers.getGender().equals("Nam")) {
                binding.spinnerGender.setSelection(0); // Chọn vị trí thứ 0 cho giá trị "Nam"
            } else if (itemUsers.getGender().equals("Nữ")) {
                binding.spinnerGender.setSelection(1); // Chọn vị trí thứ 1 cho giá trị "Nữ"
            } else if (itemUsers.getGender().equals("Khác")) {
                binding.spinnerGender.setSelection(2); // Chọn vị trí thứ 2 cho giá trị "Khác"
            }
        }
    }
    //Điều chỉnh button
    private void setupSaveButton(){
        // Khởi tạo các biến và lưu giá trị ban đầu của các trường
        tvUsenameMP = binding.tvUsenameMP;
        spinnerGender = binding.spinnerGender;
        tvMota = binding.tvMota;

        previousImageBitmap = null;
        previousUsername = tvUsenameMP.getText().toString();
        previousGender = (String) spinnerGender.getSelectedItem();
        previousMota = tvMota.getText().toString();

        // Ban đầu, ẩn btnSave
        binding.btnSave.setEnabled(false);
        binding.btnSave.setBackgroundResource(R.drawable.button_disabled);

        // Listener cho trường tvUsenameMP
        tvUsenameMP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkButtonState();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        // Listener cho spinnerGender
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                checkButtonState();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        // Listener cho trường tvMota
        tvMota.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkButtonState();
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }
    private boolean checkImageChanged() {
        if (previousImageBitmap != null) {
            return true;
        } else {
            return false;
        }
    }
    private boolean checkUsernameChanged() {
        String currentUsername = tvUsenameMP.getText().toString();
        return !currentUsername.equals(previousUsername);
    }
    private boolean checkGenderChanged() {
        String currentGender = (String) spinnerGender.getSelectedItem();
        return !currentGender.equals(previousGender);
    }
    private boolean checkMotaChanged() {
        String currentMota = tvMota.getText().toString();
        return !currentMota.equals(previousMota);
    }
    private void checkButtonState() {
        boolean isChanged = checkUsernameChanged() || checkGenderChanged()  || checkMotaChanged() || checkImageChanged();
        binding.btnSave.setEnabled(isChanged);
        if (isChanged) {
            binding.btnSave.setBackgroundResource(R.drawable.button_enabled);
        } else {
            binding.btnSave.setBackgroundResource(R.drawable.button_disabled);
        }
    }
    //Input Image
    private void openImageChooser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh");
        String[] options = {"Chụp ảnh", "Chọn ảnh từ thiết bị"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        // Chọn chụp ảnh
                        if (checkCameraPermission()) {
                            dispatchTakePictureIntent();
                        }
                        break;
                    case 1:
                        // Chọn ảnh từ thiết bị
                        if (checkExternalStoragePermission()) {
                            openGallery();
                        }
                        break;
                }
            }
        });
        builder.show();
    }
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa có quyền, yêu cầu quyền từ người dùng
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return false;
        } else {
            return true;
        }
    }
    private boolean checkExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa có quyền, yêu cầu quyền từ người dùng
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_PERMISSION);
            return false;
        } else {
            return true;
        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                binding.imageMP.setImageBitmap(imageBitmap);

                previousImageBitmap = imageBitmap;
                checkButtonState();
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                Uri imageUri = data.getData();
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                    binding.imageMP.setImageBitmap(imageBitmap);

                    previousImageBitmap = imageBitmap;
                    checkButtonState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //Out Image
    private void uploadImageToStorage() {
        if (previousImageBitmap != null) {
            // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            previousImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            String filename = UUID.randomUUID().toString() + ".jpg";

            StorageReference imageRef = storageRef.child(filename);
            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                            String imageUrl = uri.toString();
                            updateUserInfo(imageUrl);
                        }
                    });
                }
            });
        }
        else {
            updateUserInfo(itemUsers.getAvatar());
        }
    }
    private void updateUserInfo(String imageUrl) {
        String newGender = (String) spinnerGender.getSelectedItem();
        String newMota = tvMota.getText().toString();
        usersCollection.document(itemUsers.getUsersId())
                .update(
                        "gender", newGender,
                        "describe", newMota,
                        "avatar", imageUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.cancel();
                        showSuccessDialog();
                        setupSaveButton();
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
                .setTitle("Bạn đã cập nhật thông tin thất bại!")
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
                .setTitle("Bạn đã cập nhật thông tin thành công!")
                .setPositiveButton("Đóng", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}