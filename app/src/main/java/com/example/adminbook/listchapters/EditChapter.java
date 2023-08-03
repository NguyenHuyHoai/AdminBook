package com.example.adminbook.listchapters;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentEditChapterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class EditChapter extends BottomSheetDialogFragment {

    private FragmentEditChapterBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = db.collection("Books");
    private CollectionReference chaptersCollection = db.collection("Chapters");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference bookstorageRef = storage.getReference().child("content");
    private ProgressDialog progressDialog;

    private static final int PICK_FILE_REQUEST_CODE = 100;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;
    private static final int FILE_PICKER_REQUEST_CODE = 102;
    private String booksId;
    private String chaptersId;
    private Uri uri;
    private EditText name;
    private TextView namefile;
    private String currentName;
    private String currentFileName;
    private String previousName;
    private String previousNamefile;

    public static EditChapter newInstance() {
        return new EditChapter();
    }

    public interface OnChaptersEditedListener {
        void onChaptersEdited();
    }

    private EditChapter.OnChaptersEditedListener chaptersEditedListener;

    public void setOnChaptersEditedListener(EditChapter.OnChaptersEditedListener listener) {
        this.chaptersEditedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditChapterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        progressDialog = new ProgressDialog(getActivity());

        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            booksId = bundle.getString("booksId");
            String chaptersName = bundle.getString("chaptersName");
            binding.tvName.setText(chaptersName);
            currentName = chaptersName;
            String chaptersContent = bundle.getString("chaptersContent");
            currentFileName = chaptersContent;
            binding.tvLink.setText(chaptersContent);
            chaptersId = bundle.getString("getChaptersId");
        }

        setupSaveButton();

        binding.btnUpFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestReadExternalStoragePermission();
            }
        });

        binding.btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newName = binding.tvName.getText().toString().trim();
                String newFileName = binding.tvLink.getText().toString().trim();
                if (TextUtils.isEmpty(newName)) {
                    binding.tvName.setError("Tên chương không được để trống");
                    binding.tvName.requestFocus();
                    return;
                }
                uploadFileToFirebaseStorage(uri, newName, newFileName);
            }
        });

        return view;
    }

    //Setup Button
    private void setupSaveButton() {
        // Khởi tạo các biến và lưu giá trị ban đầu của các trường
        name = binding.tvName;
        namefile = binding.tvLink;

        previousName = name.getText().toString();
        previousNamefile = namefile.getText().toString();

        // Ban đầu, ẩn btnSave
        binding.btnUpdate.setEnabled(false);
        binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);

        name.addTextChangedListener(textWatcher);
        namefile.addTextChangedListener(textWatcher);
    }

    private boolean checkNameChanged() {
        String currentname = name.getText().toString();
        return !currentname.equals(previousName);
    }

    private boolean checkNameFileChanged() {
        String currentNamefile = namefile.getText().toString();
        return !currentNamefile.equals(previousNamefile);
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
        boolean isChanged = checkNameChanged() || checkNameFileChanged();
        binding.btnUpdate.setEnabled(isChanged);
        if (isChanged) {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_enabled);
        } else {
            binding.btnUpdate.setBackgroundResource(R.drawable.button_disabled);
        }
    }

    //Input Text
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
                uri = fileUri;
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

    //Update
    private void uploadFileToFirebaseStorage(Uri uriFile, String newName, String newFileName) {

        if (newName != currentName) {
            if (newFileName != currentFileName) {
                checkAndUpdateNameAndFile(uriFile, newName);
            } else {
                checkAndUpdateNameChapterToFireStore(uriFile, newName);
            }
        } else {
            updateNameChapter(uriFile);
        }

    }

    private void checkAndUpdateNameAndFile(Uri uriFile, String newName) {
        chaptersCollection.whereEqualTo("chaptersName", newName).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Nếu có kết quả trả về, tức là chaptersName đã tồn tại trong collection
                                binding.tvError.setText("");
                                binding.tvError.setText("Tên chương đã bị trùng!");
                            } else {
                                String filename = UUID.randomUUID().toString() + ".jpg";
                                StorageReference fileRef = bookstorageRef.child(filename);
                                progressDialog.setMessage("Đang tải lên...");
                                progressDialog.show();
                                fileRef.putFile(uriFile)
                                        .addOnSuccessListener(taskSnapshot -> {
                                            fileRef.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {
                                                        String newFileUrl = uri.toString();
                                                        chaptersCollection.document(chaptersId)
                                                                .update("chaptersContent", newFileUrl)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void unused) {
                                                                        if (chaptersEditedListener != null) {
                                                                            chaptersEditedListener.onChaptersEdited();
                                                                        }
                                                                        updateChapterToBooks(newName);
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        progressDialog.cancel();
                                                                        Toast.makeText(getActivity(), "Cập nhật Chapter thất bại!",
                                                                                Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
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

    private void checkAndUpdateNameChapterToFireStore(Uri uriFile, String newName) {
        chaptersCollection.whereEqualTo("chaptersName", newName).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Nếu có kết quả trả về, tức là chaptersName đã tồn tại trong collection
                                binding.tvError.setText("");
                                binding.tvError.setText("Tên chương đã bị trùng!");
                            } else {
                                progressDialog.setMessage("Đang tải lên...");
                                progressDialog.show();
                                chaptersCollection.document(chaptersId)
                                        .update("chaptersName", newName)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                updateChapterToBooks(newName);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.cancel();
                                                Toast.makeText(getActivity(), "Cập nhật Chapter thất bại!",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    private void updateNameChapter(Uri uriFile) {
        progressDialog.setMessage("Đang tải lên...");
        progressDialog.show();

        String filename = UUID.randomUUID().toString() + ".jpg";
        StorageReference fileRef = bookstorageRef.child(filename);
        fileRef.putFile(uriFile)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String newFileUrl = uri.toString();
                                chaptersCollection.document(chaptersId)
                                        .update("chaptersContent", newFileUrl)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                if (chaptersEditedListener != null) {
                                                    chaptersEditedListener.onChaptersEdited();
                                                }
                                                progressDialog.cancel();
                                                Toast.makeText(getActivity(), "Cập nhật Chapter thành công!",
                                                        Toast.LENGTH_SHORT).show();
                                                dismiss(); // Đóng dialog sau khi thêm file thành công
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.cancel();
                                                Toast.makeText(getActivity(), "Cập nhật Chapter thất bại!",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
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
                        currentChapters.remove(currentName);
                        // Cập nhật trường "chapter" trong Firestore
                        booksCollection.document(booksId).update("chapter", currentChapters);
                        if (chaptersEditedListener != null) {
                            chaptersEditedListener.onChaptersEdited();
                        }
                        progressDialog.cancel();
                        Toast.makeText(getActivity(), "Cập nhật Chapter thành công!", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.cancel();
                        Toast.makeText(getActivity(), "Cập nhật Chapter lên Books thất bại!", Toast.LENGTH_SHORT).show();
                        dismiss(); // Đóng dialog sau khi thêm file thành công
                    }
                });
    }
}