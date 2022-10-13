package ru.svolf.feature_selectapk

import android.os.Bundle
import com.mcal.common.activities.CustomizedLangActivity
import ru.svolf.feature_selectapk.ui.FileFragment

class FileActivity : CustomizedLangActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, FileFragment.newInstance())
                .commitNow()
        }
    }
}