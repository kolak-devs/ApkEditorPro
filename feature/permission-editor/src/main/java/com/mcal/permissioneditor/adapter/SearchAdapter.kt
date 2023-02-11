package com.mcal.permissioneditor.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.text.Editable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.EditText
import com.mcal.permissioneditor.model.Permission

class SearchAdapter(context: Context, private val backupList: MutableList<Permission>) : ManifestAdapter(context, backupList) {
    private var mHighlight: MutableMap<String?, TextHighlight?> = HashMap()
    private var mSearchTask: SearchTask? = null

    private fun makeHighlightText(source: String, start: Int, end: Int): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder()
        val spannableString = SpannableString(source)
        if (start >= 0) {
            val backgroundColorSpan = BackgroundColorSpan(getColorAccent(mContext))
            val foregroundColorSpan = ForegroundColorSpan(-1)
            spannableString.setSpan(backgroundColorSpan, start, end, 33)
            spannableString.setSpan(foregroundColorSpan, start, end, 33)
        }
        spannableStringBuilder.append(spannableString)
        return spannableStringBuilder
    }

    fun bind(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {}
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                search(charSequence.toString())
            }
        })
    }

    override fun getItemSubTitle(i: Int): CharSequence {
        val name = getItem(i).name
        val textHighlight = mHighlight[name]
        return if (textHighlight == null) name else makeHighlightText(name, textHighlight.subtitle.start, textHighlight.subtitle.end)
    }

    override fun getItemTitle(i: Int): CharSequence {
        val item = getItem(i)
        val textHighlight = mHighlight[item.name]
        return item.label?.let { label ->
            if (textHighlight == null) {
                label
            } else {
                makeHighlightText(label, textHighlight.title.start, textHighlight.title.end)
            }
        } ?: ""
    }

    fun search(str: String?) {
        mHighlight.clear()
        mSearchTask?.cancel()
        mSearchTask = SearchTask(this).apply {
            execute(str)
        }
    }

    inner class Highlight {
        var start = -1
        var end = -1
    }

    @SuppressLint("StaticFieldLeak")
    inner class SearchTask(private val searchAdapter: SearchAdapter) : AsyncTask<String?, Void?, List<Permission>>() {
        private var cancel = false
        fun cancel() {
            cancel = true
            @Suppress("DEPRECATION")
            cancel(true)
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(strArr: Array<String?>): List<Permission> {
            val str = strArr[0]
            if (str == null || str.trim().isEmpty()) {
                return searchAdapter.backupList
            }
            val lowerCase = str.lowercase()
            val arrayList = ArrayList<Permission>()
            var i = 0
            while (i < searchAdapter.backupList.size && !cancel) {
                val permission = searchAdapter.backupList[i]
                permission.label?.lowercase()?.let { lowerCase2 ->
                    val lowerCase3 = permission.name.lowercase()
                    val indexOf = lowerCase2.indexOf(lowerCase)
                    val indexOf2 = lowerCase3.indexOf(lowerCase)
                    if (indexOf >= 0 || indexOf2 >= 0) {
                        arrayList.add(permission)
                        searchAdapter.mHighlight[permission.name] = TextHighlight().apply {
                            title.start = indexOf
                            title.end = indexOf + lowerCase.length
                            subtitle.start = indexOf2
                            subtitle.end = indexOf2 + lowerCase.length
                        }
                    }
                }
                i++
            }
            return arrayList
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(list: List<Permission>) {
            @Suppress("DEPRECATION")
            super.onPostExecute(list)
            if (!cancel) {
                searchAdapter.currentList = list
                searchAdapter.notifyDataSetChanged()
            }
        }
    }

    inner class TextHighlight {
        var subtitle: Highlight = Highlight()
        var title: Highlight = Highlight()
    }

    companion object {
        fun getColorAccent(context: Context): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(16843829, typedValue, true)
            return typedValue.data
        }
    }
}