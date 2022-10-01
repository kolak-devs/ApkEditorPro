package com.mcal.apkeditor.prj

import android.widget.Toast
import com.mcal.apkeditor.R
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.deleteAll
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

internal class ProjectRemover(
    activity: ProjectListActivity,
    itemInfo: ProjectListAdapter.ItemInfo
) : ProcessingInterface {
    private val weakReference: WeakReference<ProjectListActivity>
    private val mItemInfo: ProjectListAdapter.ItemInfo
    private var result: Boolean
    private var errMessage: String? = null

    init {
        weakReference = WeakReference(activity)
        this.mItemInfo = itemInfo
        result = false
    }

    @Throws(Exception::class)
    override fun process() {

        // Remove the decoded directory
        try {
            deleteAll(File(mItemInfo.decodeDirectory))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Remove the project index files
        try {
            val projectFolder = ScopedStorage.makeDir(".projects").path
            deleteAll(File(projectFolder + mItemInfo.name))
            result = true
        } catch (e: Exception) {
            errMessage = e.message
        }
    }

    override fun afterProcess() {
        weakReference.get()?.let { activity ->
            if (result) {
                Toast.makeText(activity, String.format(activity.getString(R.string.project_removed), mItemInfo.name), Toast.LENGTH_LONG).show()
            } else if (errMessage != null) {
                Toast.makeText(activity, String.format(activity.getString(R.string.general_error), errMessage), Toast.LENGTH_LONG).show()
            }
            weakReference.get()?.updateProjectList()
        }
    }
}