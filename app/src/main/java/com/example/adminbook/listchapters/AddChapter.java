package com.example.adminbook.listchapters;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentAddChapterBinding;

public class AddChapter extends Fragment {

    FragmentAddChapterBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddChapterBinding.inflate(inflater, container, false);
        View view = binding.getRoot();



        return view;
    }
}