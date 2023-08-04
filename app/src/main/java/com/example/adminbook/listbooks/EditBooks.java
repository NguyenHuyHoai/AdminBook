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
    private StorageReference imageStorageRef = storage.getReference().child("imagebook");
    private StorageReference coverStorageRef = storage.getReference().child("coverbook");
    private ProgressDialog progressDialog;

    private EditText title;
    private EditText author;
    private EditText genres;
    private EditText description;
    private Bitmap previousImageBitmap;
    private Bitmap previousCoverBitmap;
    private String previousTitle;
    private String previousAuthor;
    private String previousDescription;
    private String previousGenres;

    private String booksId;
    private String imageBooks;
    private String coverBooks;

    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_PICK = 4;
    private static final int REQUEST_COVER_IMAGE_PICK = 5;
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
                openImageFile();
            }
        });
        binding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                uploadImageToStorage();
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
            // Lấy ảnh
            String coverBook = bundle.getString("CoverImage");
            coverBooks = coverBook;
            Glide.with(this)
                    .load(coverBook)
                    .placeholder(R.drawable.icon_load) // Hình ảnh tạm thời khi đang tải
                    .error(R.drawable.defaultavatar) // Hình ảnh lỗi nếu không tải được
                    .into(binding.theme);
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
        previousCoverBitmap = null;
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
    private boolean checkCoverChanged() {
        if (previousCoverBitmap != null) {
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
        boolean isChanged = checkTitleChanged() || checkAuthorChanged()  || checkGenresChanged() || checkDescriptionChanged() || checkImageChanged() || checkCoverChanged();
        binding.btnUpdate.setEnabled(isChanged);
        if (isChanged) {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_enabled);
        } else {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);
        }
    }
    //Input Image
    private void openImageFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh");
        String[] options = {"Chọn ảnh đại diện", "Chọn ảnh bìa"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        // Chọn ảnh đại diện
                        if (checkExternalStoragePermission()) {
                            openGallery();
                        }
                        break;
                    case 1:
                        // Chọn ảnh bìa
                        if (checkExternalStoragePermission()) {
                            openGalleryForCoverImage();
                        }
                        break;
                }
            }
        });
        builder.show();
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
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    private void openGalleryForCoverImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_COVER_IMAGE_PICK);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                // Xử lý chọn ảnh đại diện
                Uri imageUri = data.getData();
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                    // Hiển thị ảnh đại diện trong ImageView tương ứng
                    binding.image.setImageBitmap(imageBitmap);
                    // Lưu ảnh đại diện đã chọn để sử dụng sau này
                    previousImageBitmap = imageBitmap;
                    checkButtonState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_COVER_IMAGE_PICK) {
                // Xử lý chọn ảnh bìa
                Uri imageUri = data.getData();
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                    // Hiển thị ảnh bìa trong ImageView tương ứng
                    binding.theme.setImageBitmap(imageBitmap);
                    // Lưu ảnh bìa đã chọn để sử dụng sau này
                    previousCoverBitmap = imageBitmap;
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
            if (previousCoverBitmap != null)
            {
                final String[] a = new String[1];
                final String[] b = new String[1];
                // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                previousImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();
                String filename = UUID.randomUUID().toString() + ".jpg";

                StorageReference imageRef = imageStorageRef.child(filename);
                UploadTask uploadTask = imageRef.putBytes(data);
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                                String imageUrl = uri.toString();
                                // Lưu imageURL vào biến a
                                a[0] = imageUrl;
                                // Check if both URLs are available
                                if (a[0] != null && b[0] != null) {
                                    updateUserInfo(a[0], b[0]);
                                }
                            }
                        });
                    }
                });
                // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
                ByteArrayOutputStream baoss = new ByteArrayOutputStream();
                previousCoverBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] dataa = baoss.toByteArray();
                String filenamee = UUID.randomUUID().toString() + ".jpg";

                StorageReference coverRef = coverStorageRef.child(filenamee);
                UploadTask uploadTaskk = coverRef.putBytes(dataa);
                uploadTaskk.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        coverRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                                String coverUrl = uri.toString();
                                // Lưu coverURL vào biến b
                                b[0] = coverUrl;
                                // Check if both URLs are available
                                if (a[0] != null && b[0] != null) {
                                    updateUserInfo(a[0], b[0]);
                                }
                            }
                        });
                    }
                });
            }
            else {
                // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                previousImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();
                String filename = UUID.randomUUID().toString() + ".jpg";

                StorageReference imageRef = imageStorageRef.child(filename);
                UploadTask uploadTask = imageRef.putBytes(data);
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                                String imageUrl = uri.toString();
                                updateUserInfo(imageUrl, coverBooks);
                            }
                        });
                    }
                });
            }
        }
        else {
            // Tạo ByteArrayOutputStream để nén ảnh thành một mảng byte
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            previousCoverBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            String filename = UUID.randomUUID().toString() + ".jpg";

            StorageReference coverRef = coverStorageRef.child(filename);
            UploadTask uploadTask = coverRef.putBytes(data);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    coverRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Ở đây, bạn có thể lấy URL và cập nhật thông tin vào Collection của tài khoản đang đăng nhập
                            String coverUrl = uri.toString();
                            updateUserInfo(imageBooks,coverUrl);
                        }
                    });
                }
            });
        }
    }
    private void updateUserInfo(String imageUrl, String coverUrl) {
        String newDescription = description.getText().toString();
        String newTitle = title.getText().toString();
        String newAuthor = author.getText().toString();
        String newGenres = genres.getText().toString();

        booksCollection.document(booksId)
                .update(
                        "genders", newGenres,
                        "description", newDescription,
                        "imageBook", imageUrl,"title", newTitle, "author", newAuthor,"coverImage", coverUrl)
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