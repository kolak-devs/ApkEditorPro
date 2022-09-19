package com.mcal.apkeditor.ui.fulleditor.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mcal.apkeditor.databinding.FragmentResourcesBinding
import com.mcal.apkeditor.ui.fulleditor.FullEditorViewModel

class ResourcesFragment : Fragment() {
    private lateinit var binding: FragmentResourcesBinding
    private val model: FullEditorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}