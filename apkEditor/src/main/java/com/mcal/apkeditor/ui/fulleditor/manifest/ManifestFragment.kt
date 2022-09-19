package com.mcal.apkeditor.ui.fulleditor.manifest

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.ApkInfoActivity
import com.mcal.apkeditor.activities.ManifestSearchResultActivity
import com.mcal.apkeditor.adapters.IManifestChangeCallback
import com.mcal.apkeditor.adapters.LineRecord
import com.mcal.apkeditor.adapters.ManifestListAdapter
import com.mcal.apkeditor.autocomplete.AutoCompleteAdapter
import com.mcal.apkeditor.databinding.FragmentManifestBinding
import com.mcal.apkeditor.ui.fulleditor.FullEditorViewModel
import com.mcal.common.utils.writeToFile
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class ManifestFragment : Fragment(), IManifestChangeCallback {
    private lateinit var binding: FragmentManifestBinding
    private val model: FullEditorViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentManifestBinding.inflate(inflater, container, false)
        initAdapter()
        initSearchAdapter()
        return binding.root
    }

    private fun initAdapter() {
        val adapter = ManifestListAdapter(requireActivity(), model.decodedPath + "/AndroidManifest.xml", this)
        val manifestList = binding.manifestList
        manifestList.layoutManager = LinearLayoutManager(context)
        manifestList.adapter = adapter
    }

    private fun initSearchAdapter() {
        val keywordAdapter = AutoCompleteAdapter(requireContext(), "mf_keywords")
        binding.mfKeyword.setAdapter(keywordAdapter)

        binding.btnSearchMf.setOnClickListener {
            var keyword = binding.mfKeyword.text.toString()
            keyword = keyword.trim()
            if (keyword == "") {
                Toast.makeText(
                    requireContext(), R.string.empty_input_tip, Toast.LENGTH_SHORT
                ).show()
            } else {
                keywordAdapter.addInputHistory(keyword)
                val lines = ArrayList<Int>()
                val lineContents = ArrayList<String>()
                searchManifest(keyword, lines, lineContents)
                if (lines.isEmpty()) {
                    Toast.makeText(
                        requireContext(), R.string.notfound_in_manifest,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(requireContext(), ManifestSearchResultActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("filePath", model.decodedPath + "/AndroidManifest.xml")
                    bundle.putIntegerArrayList("lineIndexs", lines)
                    bundle.putStringArrayList("lineContents", lineContents)
                    intent.putExtras(bundle)
                    startActivityForResult(intent, ApkInfoActivity.RC_SEARCH_MF)
                }
            }
        }
    }

    override fun tryToDeleteSection(lineRec: LineRecord?): String? {
        return null
    }

    override fun manifestChanged(newContent: String) {
        writeToFile(model.decodedPath + "/AndroidManifest.xml", newContent.toByteArray())
        model.manifestModified = true
    }

    // Return line NO and line content
    private fun searchManifest(
        keyword: String, lineIndexes: MutableList<Int>,
        lineContents: MutableList<String>
    ) {
        val filePath: String = model.decodedPath + "/AndroidManifest.xml"
        try {
            val fis = FileInputStream(filePath)
            val br = BufferedReader(InputStreamReader(fis))
            var line = br.readLine()
            var index = 1
            while (line != null) {
                if (line.contains(keyword)) {
                    lineIndexes.add(index)
                    lineContents.add(line)
                }
                line = br.readLine()
                index += 1
            }
            br.close()
            fis.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}