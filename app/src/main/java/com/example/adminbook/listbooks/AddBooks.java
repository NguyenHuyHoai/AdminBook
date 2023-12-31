package com.example.adminbook.listbooks;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddBooksBinding;
import com.example.adminbook.databinding.FragmentAddGenresBinding;
import com.example.adminbook.listgenres.AddGenres;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddBooks extends BottomSheetDialogFragment {
    private FragmentAddBooksBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = db.collection("Books");
    private CollectionReference genresCollection = db.collection("Genres");
    private CollectionReference bookGenresCollection = db.collection("BookGenres");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference imageStorageRef = storage.getReference().child("imagebook");
    private StorageReference coverStorageRef = storage.getReference().child("coverbook");

    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_PICK = 3;
    private static final int REQUEST_COVER_IMAGE_PICK = 5;
    private Bitmap previousImageBitmap;
    private Bitmap previousCoverImageBitmap;
    ProgressDialog progressDialog;
    private String imageBook;
    private String coverBook;

    public static AddBooks newInstance() {
        return new AddBooks();
    }
    public interface OnBooksAddedListener {
        void onBooksAdded();
    }
    private AddBooks.OnBooksAddedListener booksAddedListener;

    public void setOnBooksAddedListener(AddBooks.OnBooksAddedListener listener) {
        this.booksAddedListener = listener;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddBooksBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());
        setupAddButton();

        binding.btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageFile();
            }
        });
        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                addNewBook();
            }
        });
        return view;
    }
    //input Image
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
                    previousCoverImageBitmap = imageBitmap;
                    checkButtonState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //Setup Button
    private void setupAddButton() {
        checkButtonState();
        // Thêm TextChangedListener cho các EditText
        binding.edNameBook.addTextChangedListener(textWatcher);
        binding.edNameAuthor.addTextChangedListener(textWatcher);
        binding.edListGenres.addTextChangedListener(textWatcher);
        binding.edDescription.addTextChangedListener(textWatcher);
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
    private boolean checkImageChanged() {
        // Kiểm tra xem previousImageBitmap có bị null hay không
        if (previousImageBitmap != null) {
            return false;
        } else {
            return true;
        }
    }
    private boolean checkCoverChanged() {
        // Kiểm tra xem previousImageBitmap có bị null hay không
        if (previousCoverImageBitmap != null) {
            return false;
        } else {
            return true;
        }
    }
    private void checkButtonState() {
        String nameBook = binding.edNameBook.getText().toString().trim();
        String nameAuthor = binding.edNameAuthor.getText().toString().trim();
        String listGenres = binding.edListGenres.getText().toString().trim();
        String description = binding.edDescription.getText().toString().trim();

        boolean isAnyFieldEmpty = nameBook.isEmpty() || nameAuthor.isEmpty() || listGenres.isEmpty() || description.isEmpty() || checkImageChanged() || checkCoverChanged();

        if (isAnyFieldEmpty) {
            binding.btnAdd.setEnabled(false);
            binding.btnAdd.setBackgroundResource(R.drawable.button_disabled);
        } else {
            binding.btnAdd.setEnabled(true);
            binding.btnAdd.setBackgroundResource(R.drawable.button_enabled);
        }
    }
    //Output
    private void addNewBook() {
        String genres = binding.edListGenres.getText().toString();
        List<String> genresList = Arrays.asList(genres.split(", "));
        String title = binding.edNameBook.getText().toString();
        String author = binding.edNameAuthor.getText().toString();
        String description = binding.edDescription.getText().toString();
        List<String> chapter = new ArrayList<>();
        final String[] a = new String[1];
        final String[] b = new String[1];

        checkGenresExist(genresList, new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean allGenresExist) {
                if (allGenresExist) {
                    // Chuyển đổi ảnh đại diện thành mảng byte
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    previousImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] avatarImageData = baos.toByteArray();

                    // Chuyển đổi ảnh bìa thành mảng byte
                    ByteArrayOutputStream baosCover = new ByteArrayOutputStream();
                    previousCoverImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baosCover);
                    byte[] coverImageData = baosCover.toByteArray();

                    // Tải ảnh đại diện lên Storage với tên ngẫu nhiên bắt đầu bằng "image"
                    String avatarImageFileName = UUID.randomUUID().toString() + ".jpg";
                    // Tải ảnh bìa lên Storage với tên ngẫu nhiên bắt đầu bằng "cover"
                    String coverImageFileName = UUID.randomUUID().toString() + ".jpg";

                    imageStorageRef.child(avatarImageFileName).putBytes(avatarImageData)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    imageStorageRef.child(avatarImageFileName).getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    String imageURL = uri.toString();
                                                    // Lưu imageURL vào biến a
                                                    a[0] = imageURL;
                                                    // Check if both URLs are available
                                                    if (a[0] != null && b[0] != null) {
                                                        saveBooksInfoToFirestore(title, author, description, genresList, a[0], b[0], chapter);
                                                    }
                                                }
                                            });
                                }
                            });
                    coverStorageRef.child(coverImageFileName).putBytes(coverImageData)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    coverStorageRef.child(coverImageFileName).getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    String coverURL = uri.toString();
                                                    // Lưu coverURL vào biến b
                                                    b[0] = coverURL;
                                                    // Check if both URLs are available
                                                    if (a[0] != null && b[0] != null) {
                                                        saveBooksInfoToFirestore(title, author, description, genresList, a[0], b[0], chapter);
                                                    }
                                                }
                                            });
                                }
                            });

                } else {
                    binding.tvErrorAB.setText("");
                    binding.tvErrorAB.setText("Không có thể loại mà sách đang cần!");
                    progressDialog.cancel();
                }
            }
        });
    }

    private void saveBooksInfoToFirestore(String title, String author, String description,
                                              List<String> genresList,String imageBook, String coverBook, List<String> chapter) {
            ItemBooks newBook = new ItemBooks();
            newBook.setTitle(title);
            newBook.setAuthor(author);
            newBook.setDescription(description);
            newBook.setGenres(genresList);
            newBook.setRating(0.0f);
            newBook.setRatingsCount(0);
            newBook.setViewsCount(0);
            newBook.setImageBook(imageBook);
            newBook.setCoverImage(coverBook);
            newBook.setCreationTimestamp(Timestamp.now());
            newBook.setChapter(chapter);

            booksCollection.add(newBook)
                    .addOnSuccessListener(documentReference -> {
                        String bookId = documentReference.getId();
                        documentReference.update("booksId", bookId);

                        for (String genresName : genresList) {
                            // Tạo một document trong collection "BookGenres" để thể hiện mối quan hệ nhiều - nhiều
                            getGenresIdFromName(genresName, genresId -> {
                                if (genresId != null) {
                                    // Nếu tìm thấy genreId cho genreName, tiến hành thêm document vào collection "BookGenres"
                                    String bookGenresId = "books_" + bookId + "_genres_" + genresId;
                                    // Tạo một Map chứa thông tin về mối quan hệ giữa sách và thể loại sách
                                    Map<String, Object> bookGenreData = new HashMap<>();
                                    bookGenreData.put("booksId", bookId);
                                    bookGenreData.put("genresId", genresId);
                                    // Thêm tài liệu vào collection "BookGenres" với bookGenresId là id của mối quan hệ
                                    bookGenresCollection.document(bookGenresId).set(bookGenreData);
                                }
                            });
                        }
                        // Gọi phương thức của interface để thông báo cho ListGenresFragment
                        if (booksAddedListener != null) {
                            booksAddedListener.onBooksAdded();
                        }
                        if (isAdded() && getActivity() != null) {
                            progressDialog.cancel();
                            showSuccessDialog();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.cancel();
                        showFailureDialog();
                    });
        }

    private void checkGenresExist(List<String> genresList, OnSuccessListener<Boolean> listener) {
        final int[] existingGenresCount = {0};

        for (String genreName : genresList) {
            genresCollection.whereEqualTo("genresName", genreName).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        existingGenresCount[0]++;

                        if (existingGenresCount[0] == genresList.size()) {
                            listener.onSuccess(true);
                        }
                    } else {
                        listener.onSuccess(false);
                    }
                }
            });
        }
    }
    private void getGenresIdFromName(String genresName, OnSuccessListener<String> listener) {
        // Thực hiện truy vấn vào collection "Genres" để lấy genreId tương ứng với genreName
        genresCollection.whereEqualTo("genresName", genresName)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (!querySnapshot.isEmpty()) {
                            // Nếu có kết quả trả về từ truy vấn, lấy genreId đầu tiên (giả sử không có các genresName trùng nhau)
                            String genresId = querySnapshot.getDocuments().get(0).getId();
                            listener.onSuccess(genresId);
                        } else {
                            // Nếu không tìm thấy genresName trong collection "Genres", gọi listener.onSuccess(null)
                            listener.onSuccess(null);
                        }
                    }
                });
    }

    // Hàm hiển thị Dialog thành công và thất bại
    public void showFailureDialog() {
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
    public void showSuccessDialog() {
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
}