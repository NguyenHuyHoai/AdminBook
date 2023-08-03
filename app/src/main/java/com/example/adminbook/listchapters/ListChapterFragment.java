package com.example.adminbook.listchapters;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.adminbook.databinding.FragmentListChapterBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class ListChapterFragment extends Fragment {

    private FragmentListChapterBinding binding;
    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private CollectionReference booksCollection = firebaseFirestore.collection("Books");
    private CollectionReference chaptersCollection = firebaseFirestore.collection("Chapters");
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference chapterstorageRef = storage.getReference().child("content");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListChapterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();



        return view;
    }
}