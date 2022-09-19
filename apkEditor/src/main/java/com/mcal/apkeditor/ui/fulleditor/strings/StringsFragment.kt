package com.mcal.apkeditor.ui.fulleditor.strings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mcal.apkeditor.databinding.FragmentStringsBinding

class StringsFragment : Fragment() {
    private lateinit var binding: FragmentStringsBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStringsBinding.inflate(inflater, container, false)
        return binding.root
    }
}