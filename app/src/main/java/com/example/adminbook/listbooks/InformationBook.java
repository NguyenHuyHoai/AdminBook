package com.example.adminbook.listbooks;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.FragmentTransaction;

import com.example.adminbook.R;
import com.example.adminbook.databinding.FragmentInformationBookBinding;
import com.example.adminbook.listchapters.ListChapterFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class InformationBook extends BottomSheetDialogFragment {
    private FragmentInformationBookBinding binding;

    public interface OnInformationListener {
        void onUpdateSuccess();
    }

    private InformationBook.OnInformationListener informationListener;
    public void setOnInformationListener(InformationBook.OnInformationListener listener) {
        this.informationListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInformationBookBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        // Nhận dữ liệu từ Bundle
        Bundle bundle = getArguments();

        binding.btnUpdateBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditBook(bundle);
                dismiss();
            }
        });

        binding.btnAddChapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddChapter(bundle);
                dismiss();
            }
        });
        return view;
    }

    private void showAddChapter(Bundle bundle) {
        ListChapterFragment chapterFragment = new ListChapterFragment();
        chapterFragment.setArguments(bundle);
        // Thực hiện thay thế Fragment B bằng Fragment C
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down)
                .replace(R.id.content_frame, chapterFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showEditBook( Bundle bundle) {
        EditBooks editBooks = new EditBooks();
        editBooks.setArguments(bundle);
        // Thực hiện thay thế Fragment B bằng Fragment C
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_up, R.anim.slide_down, R.anim.slide_up, R.anim.slide_down)
                .replace(R.id.content_frame, editBooks)
                .addToBackStack(null)
                .commit();
    }
}