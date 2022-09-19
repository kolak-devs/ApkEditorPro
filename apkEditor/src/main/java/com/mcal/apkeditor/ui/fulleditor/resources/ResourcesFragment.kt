package com.mcal.apkeditor.ui.fulleditor.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mcal.apkeditor.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {
    private lateinit var binding: FragmentResourcesBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }
}