package com.mcal.apkeditor.ui.fulleditor.strings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mcal.apkeditor.databinding.FragmentStringsBinding
import com.mcal.apkeditor.ui.fulleditor.FullEditorViewModel

class StringsFragment : Fragment() {
    private lateinit var binding: FragmentStringsBinding
    private val model: FullEditorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStringsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}