package ru.svolf.filemanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ru.svolf.filemanager.ui.main.FileManagerFragment

class FileManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, FileManagerFragment.newInstance())
                .commitNow()
        }
    }
}