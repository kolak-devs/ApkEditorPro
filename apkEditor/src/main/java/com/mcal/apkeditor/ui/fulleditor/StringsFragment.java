package com.mcal.apkeditor.ui.fulleditor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mcal.apkeditor.databinding.FragmentStringsBinding;

public class StringsFragment extends Fragment {
    private FragmentStringsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentStringsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }
}
