package com.example.adminbook.listchapters;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddChapterBinding;
import com.example.adminbook.listbooks.AddBooks;
import com.example.adminbook.listbooks.ItemBooks;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddChapter extends BottomSheetDialogFragment {

    FragmentAddChapterBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = db.collection("Books");
    private CollectionReference chaptersCollection = db.collection("Chapters");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference bookstorageRef = storage.getReference().child("content");
    private ProgressDialog progressDialog;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;
    private static final int FILE_PICKER_REQUEST_CODE = 102;
    private String booksId;
    private Uri uriFile;

    public static AddChapter newInstance() {
        return new AddChapter();
    }
    public interface OnChaptersAddedListener {
        void onChaptersAdded();
    }
    private AddChapter.OnChaptersAddedListener chaptersAddedListener;

    public void setOnChaptersAddedListener(AddChapter.OnChaptersAddedListener listener) {
        this.chaptersAddedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddChapterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());

        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            booksId = bundle.getString("booksId");
        }
        setupSaveButton();

        binding.btnUpFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestReadExternalStoragePermission();
            }
        });

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newName = binding.tvName.getText().toString();
                String newLink = binding.tvLink.getText().toString();
                checkAndAddChapter(newName, newLink);
            }
        });

        return view;
    }

    //UpChapterToFireStorage
    private void checkAndAddChapter(String newName, String newLink) {
        chaptersCollection.whereEqualTo("chaptersName", newName)
                .whereEqualTo("booksId", booksId).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                binding.tvName.setError("Tên chương đã bị trùng!");
                                binding.tvName.requestFocus();
                            } else {
                                progressDialog.show();
                                StorageReference fileRef = bookstorageRef.child(newLink);
                                progressDialog.setMessage("Đang tải lên...");
                                progressDialog.show();
                                fileRef.putFile(uriFile)
                                        .addOnSuccessListener(taskSnapshot -> {
                                            fileRef.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {
                                                        String newFileUrl = uri.toString();
                                                        addChapterToFireStore(newName, newFileUrl);
                                                    });
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.cancel();
                                                Toast.makeText(getActivity(), "Update Chapter thất bại!",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }
                });
    }
    //Update to FireStore
    private void addChapterToFireStore(String newName, String newFileUrl) {
        ItemChapters newChapter = new ItemChapters();
        newChapter.setBooksId(booksId);
        newChapter.setChaptersName(newName);
        newChapter.setChaptersContent(newFileUrl);
        newChapter.setChaptersTime(Timestamp.now());
        chaptersCollection.add(newChapter)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String chaptersId = documentReference.getId();
                        documentReference.update("chaptersId", chaptersId);
                        updateChapterToBooks(newName);
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

    private void updateChapterToBooks(String newchaptersName) {
        booksCollection.document(booksId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> currentChapters = (List<String>) documentSnapshot.get("chapter");
                        if (currentChapters == null) {
                            currentChapters = new ArrayList<>();
                        }
                        // Thêm chương mới vào mảng "currentChapters"
                        currentChapters.add(newchaptersName);
                        // Cập nhật trường "chapter" trong Firestore
                        booksCollection.document(booksId).update("chapter", currentChapters);
                        if (chaptersAddedListener != null) {
                            chaptersAddedListener.onChaptersAdded();
                        }
                        progressDialog.cancel();
                        showSuccessDialog();
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

    //UpFILE
    private void requestReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            selectFile();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(getActivity(), "Quyền Bị Từ Chối", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooserIntent = Intent.createChooser(intent, "Chọn tệp");
        startActivityForResult(chooserIntent, FILE_PICKER_REQUEST_CODE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                String fileName = getFileNameFromUri(fileUri);
                binding.tvLink.setText(fileName);
                uriFile = fileUri;
            }
        }
    }
    private String getFileNameFromUri(Uri fileUri) {
        String fileName = null;
        Cursor cursor = getActivity().getContentResolver().query(fileUri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            fileName = cursor.getString(nameIndex);
            cursor.close();
        }
        return fileName;
    }

    // Setup Button
    private void setupSaveButton() {
        // Ban đầu, ẩn btnSave
        binding.btnAdd.setEnabled(false);
        binding.btnAdd.setBackgroundResource(R.drawable.button_disabled);

        checkButtonState();
        // Thêm TextChangedListener cho các EditText
        binding.tvName.addTextChangedListener(textWatcher);
        binding.tvLink.addTextChangedListener(textWatcher);
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
        String newName = binding.tvName.getText().toString().trim();
        String newLink = binding.tvLink.getText().toString().trim();

        boolean isAnyFieldEmpty = newName.isEmpty() || newLink.isEmpty();

        if (isAnyFieldEmpty) {
            binding.btnAdd.setEnabled(false);
            binding.btnAdd.setBackgroundResource(R.drawable.button_disabled);
        } else {
            binding.btnAdd.setEnabled(true);
            binding.btnAdd.setBackgroundResource(R.drawable.button_enabled);
        }
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