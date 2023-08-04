package com.example.adminbook.listbooks;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentEditBooksBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


public class EditBooks extends Fragment {

    private FragmentEditBooksBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = db.collection("Books");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference bookstorageRef = storage.getReference().child("imagebook");
    private ProgressDialog progressDialog;

    private EditText title;
    private EditText author;
    private EditText genres;
    private EditText description;
    private Bitmap previousImageBitmap;
    private String previousTitle;
    private String previousAuthor;
    private String previousDescription;
    private String previousGenres;

    private String booksId;
    private String imageBooks;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_IMAGE_PICK = 4;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditBooksBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());
        displayBookData();
        setupSaveButton();

        binding.btnFileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageChooser();
            }
        });
        binding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                uploadImageToStorage(imageBooks, booksId);
            }
        });
        return view;
    }

    private void displayBookData() {
        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if(bundle != null)
        {
            String bookId = bundle.getString("booksId");
            booksId = bookId;
            String title = bundle.getString("title", "");
            binding.edNameBook.setText(title);
            String author = bundle.getString("author", "");
            binding.edNameAuthor.setText(author);
            String description = bundle.getString("description", "");
            binding.edDescription.setText(description);
            // Lấy danh sách genres từ Bundle và hiển thị nó (ví dụ: sử dụng TextView)
            ArrayList<String> genresList = bundle.getStringArrayList("genres");
            String genresString = TextUtils.join(", ", genresList);
            binding.edListGenres.setText(genresString);
            // Lấy ảnh
            String imageBook = bundle.getString("imageBook");
            imageBooks = imageBook;
            Glide.with(this)
                    .load(imageBook)
                    .placeholder(R.drawable.icon_load) // Hình ảnh tạm thời khi đang tải
                    .error(R.drawable.defaultavatar) // Hình ảnh lỗi nếu không tải được
                    .into(binding.image);
        }
    }
    //Setup Button
    private void setupSaveButton(){
        // Khởi tạo các biến và lưu giá trị ban đầu của các trường
        title = binding.edNameBook;
        author = binding.edNameAuthor;
        genres = binding.edListGenres;
        description = binding.edDescription;

        previousImageBitmap = null;
        previousTitle = title.getText().toString();
        previousAuthor = author.getText().toString();
        previousGenres = genres.getText().toString();
        previousDescription = description.getText().toString();

        // Ban đầu, ẩn btnSave
        binding.btnUpdate.setEnabled(false);
        binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);

        title.addTextChangedListener(textWatcher);
        author.addTextChangedListener(textWatcher);
        genres.addTextChangedListener(textWatcher);
        description.addTextChangedListener(textWatcher);
    }
    private boolean checkImageChanged() {
        if (previousImageBitmap != null) {
            return true;
        } else {
            return false;
        }
    }
    private boolean checkTitleChanged() {
        String currentTitle = title.getText().toString();
        return !currentTitle.equals(previousTitle);
    }
    private boolean checkAuthorChanged() {
        String currentAuthor = author.getText().toString();
        return !currentAuthor.equals(previousAuthor);
    }
    private boolean checkGenresChanged() {
        String currentGenres = genres.getText().toString();
        return !currentGenres.equals(previousGenres);
    }
    private boolean checkDescriptionChanged() {
        String currentDescription = description.getText().toString();
        return !currentDescription.equals(previousDescription);
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
        boolean isChanged = checkTitleChanged() || checkAuthorChanged()  || checkGenresChanged() || checkDescriptionChanged() || checkImageChanged();
        binding.btnUpdate.setEnabled(isChanged);
        if (isChanged) {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_enabled);
        } else {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);
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
                binding.image.setImageBitmap(imageBitmap);

                previousImageBitmap = imageBitmap;
                checkButtonState();
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                Uri imageUri = data.getData();
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                    binding.image.setImageBitmap(imageBitmap);

                    previousImageBitmap = imageBitmap;
                    checkButtonState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //Out Image
    private void uploadImageToStorage(String imageBooks, String booksId) {
        if (previousImageBitmap != null) {
            // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            previousImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            String filename = UUID.randomUUID().toString() + ".jpg";

            StorageReference imageRef = bookstorageRef.child(filename);
            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                            String imageUrl = uri.toString();
                            updateUserInfo(imageUrl, booksId);
                        }
                    });
                }
            });
        }
        else {
            updateUserInfo(imageBooks,booksId);
        }
    }
    private void updateUserInfo(String imageUrl, String booksId) {
        String newDescription = description.getText().toString();
        String newTitle = title.getText().toString();
        String newAuthor = author.getText().toString();
        String newGenres = genres.getText().toString();

        booksCollection.document(booksId)
                .update(
                        "genders", newGenres,
                        "description", newDescription,
                        "imageBook", imageUrl,"title", newTitle, "author", newAuthor)
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