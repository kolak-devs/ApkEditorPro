package com.mcal.apkeditor

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mcal.apkeditor.activities.ApkComposeActivity
import com.mcal.apkeditor.ce.IApkMaking
import com.mcal.apkeditor.data.Constants
import com.mcal.common.utils.ActivityHelper
import com.mcal.common.utilsOld.ITaskCallback
import com.mcal.common.utilsOld.ITaskCallback.TaskStepInfo
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference

class ApkComposeService : Service(), ITaskCallback {
    private val composeResult = ComposeResult()

    // For UI management
    private val handler: MyHandler = MyHandler()
    private val binder = ComposeServiceBinder()
    private var decodeRootPath: String? = null
    private var srcApkPath: String? = null
    private var stringModified: Boolean? = null
    private var manifestModified: Boolean? = null
    private var resFileModified: Boolean? = null
    private var modifiedSmaliFolders: ArrayList<String>? = null

    private var addedFiles: MutableMap<String, String>? = null
    private var replacedFiles: MutableMap<String, String>? = null
    private var deletedFiles: MutableSet<String>? = null

    // Recorded all the relation between file entry to zip entry
    // like res/drawable-hdpi-v4/a.png -> res/drawable-hdpi/a.png
    private var fileEntry2ZipEntry: Map<String, String>? = null

    // Output apk path
    private var targetApkPath: String? = null

    // Composing thread and result
    private var composeThread: ComposeThread? = null
    private var observer: WeakReference<ITaskCallback>? = null
    private var extraMaker: IApkMaking? = null

    // Foreground notification
    private var mNotificationManager: NotificationManager? = null
    private var mNotifyBuilder: NotificationCompat.Builder? = null
    private var foregroundStarted = false
    private var signAPK = false

    // Update notification title and description
    // Called from non-UI thread
    private var lastUpdateTime: Long = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // When the service is restarted, intent = null
        decodeRootPath = ActivityHelper.getParam(intent, "decodeRootPath")
        srcApkPath = ActivityHelper.getParam(intent, "srcApkPath")
        targetApkPath = ActivityHelper.getParam(intent, "targetApkPath")
        var str = ActivityHelper.getParam(intent, "stringModified")
        stringModified = str.toBoolean()
        str = ActivityHelper.getParam(intent, "manifestModified")
        manifestModified = str.toBoolean()
        str = ActivityHelper.getParam(intent, "resFileModified")
        resFileModified = str.toBoolean()
        modifiedSmaliFolders = ActivityHelper.getStringArray(intent, "modifiedSmaliFolders")
        signAPK = ActivityHelper.getBoolParam(intent, "signAPK")
        addedFiles = ActivityHelper.getMapParam(intent, "addedFiles")
        replacedFiles = ActivityHelper.getMapParam(intent, "replacedFiles")
        deletedFiles = HashSet()
        val delEntries: List<String>? = ActivityHelper.getStringArray(intent, "deletedFiles")
        delEntries?.let { entries ->
            deletedFiles?.addAll(entries)
        }
        val passedFile = ActivityHelper.getParam(intent, "fileEntry2ZipEntry")
        if (passedFile != null) {
            fileEntry2ZipEntry = getMapFromFile(passedFile)
        }
        resetStatus()

        // Initially show notification in pro version
        // For free version, only show it when ad is ready
        if (BuildConfig.IS_PRO) {
            showNotification()
        }
        startComposeThread()
        return START_STICKY
    }

    private fun showNotification() {
        val composeIntent = Intent(this, ApkComposeActivity::class.java)
        composeIntent.action = Constants.ACTION.MAIN_ACTION
        composeIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val pendingIntent = PendingIntent.getActivity(this, 0, composeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val iconId = R.mipmap.ic_launcher_round
        val appName = getString(R.string.app_name)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotifyBuilder = NotificationCompat.Builder(this, ApkComposeActivity.PRIMARY_NOTIF_CHANNEL)
        mNotifyBuilder?.let { builder ->
            builder.setContentTitle(appName)
                .setTicker(appName)
                .setContentText(getString(R.string.build_ongoing))
                .setSmallIcon(iconId)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build())
        }
        foregroundStarted = true
    }

    private fun updateNotification(forceShow: Boolean, title: String, desc: String) {
        if (mNotificationManager != null) {
            handler.removeMessages(0)
            handler.setInfo(title, desc)
            // If not updated for a long time, then directly update it
            if (forceShow || System.currentTimeMillis() - lastUpdateTime > 1000) {
                handler.sendEmptyMessage(0)
            } else {
                handler.sendEmptyMessageDelayed(0, 500)
            }
        }
    }

    private fun resetStatus() {
        composeThread?.let { thread ->
            if (thread.isActive) {
                thread.stopRunning()
            }
        }
        composeResult.clear()
    }

    private fun startComposeThread() {
        composeThread = if (srcApkPath != null) {
            ApkComposeThread(
                this, decodeRootPath,
                srcApkPath, targetApkPath
            )
        } else {
            // srcApkPath == null, means currently is a full decoding
            decodeRootPath?.let { decodePath ->
                targetApkPath?.let { apkPath ->
                    ApkComposeThreadNew(this, decodePath, apkPath)
                }
            }
        }
        composeThread?.let { thread ->
            if (extraMaker != null) {
                thread.setExtraMaker(extraMaker)
            }

            stringModified?.let { string ->
                manifestModified?.let { manifest ->
                    resFileModified?.let { res ->
                        thread.setModification(
                            string, manifest, res,
                            modifiedSmaliFolders, addedFiles, replacedFiles,
                            deletedFiles, fileEntry2ZipEntry, signAPK
                        )
                    }
                }
            }
            thread.setTaskCallback(this)
            thread.execute()
        }
    }

    private fun getMapFromFile(filepath: String): Map<String, String> {
        val result: MutableMap<String, String> = HashMap()
        var br: BufferedReader? = null
        try {
            br = BufferedReader(
                InputStreamReader(FileInputStream(filepath))
            )
            var line = br.readLine()
            while (line != null) {
                val key = line
                val value = br.readLine()
                if (value != null) {
                    result[key] = value
                } else {
                    break
                }
                line = br.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun setTaskStepInfo(stepInfo: TaskStepInfo) {
        synchronized(composeResult) { composeResult.curStep = stepInfo }
        observer?.get()?.setTaskStepInfo(stepInfo)
        val desc = String.format(
            resources.getString(R.string.step) + " %d/%d: %s",
            stepInfo.stepIndex, stepInfo.stepTotal,
            stepInfo.stepDescription
        )
        val forceShow = stepInfo.stepIndex == stepInfo.stepTotal
        updateNotification(forceShow, getString(R.string.build_ongoing), desc)
    }

    override fun setTaskProgress(progress: Float) {
        observer?.get()?.setTaskProgress(progress)
    }

    override fun taskSucceed() {
        synchronized(composeResult) {
            composeResult.finished = true
            composeResult.ret = true
            composeResult.failMessage = null
        }
        updateNotification(true, getString(R.string.build_finished), getString(R.string.succeed))
        observer?.get()?.taskSucceed()
    }

    override fun taskFailed(errMessage: String) {
        synchronized(composeResult) {
            composeResult.finished = true
            composeResult.ret = false
            composeResult.failMessage = errMessage
        }
        updateNotification(true, getString(R.string.build_finished), getString(R.string.failed))
        observer?.get()?.taskFailed(errMessage)
    }

    override fun taskWarning(message: String) {
        observer?.get()?.taskWarning(message)
    }

    class ComposeResult {
        var finished = false
        var ret = false
        var failMessage: String? = null
        var curStep: TaskStepInfo? = null
        fun clear() {
            finished = false
            failMessage = null
            curStep = null
        }
    }

    @SuppressLint("HandlerLeak")
    private inner class MyHandler : Handler(Looper.myLooper() ?: Looper.getMainLooper()) {
        private var title: String? = null
        private var desc: String? = null
        fun setInfo(title: String?, desc: String?) {
            this.title = title
            this.desc = desc
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    mNotifyBuilder?.let { builder ->
                        title?.let {
                            builder.setContentTitle(it)
                        }
                        desc?.let {
                            builder.setContentText(it)
                        }
                        if (foregroundStarted) {
                            mNotificationManager?.notify(
                                Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                                builder.build()
                            )
                        } else {
                            startForeground(
                                Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                                builder.build()
                            )
                            foregroundStarted = true
                        }
                    }
                    lastUpdateTime = System.currentTimeMillis()
                }
            }
        }
    }

    inner class ComposeServiceBinder : Binder() {
        fun setObserver(_observer: ITaskCallback) {
            observer = WeakReference(_observer)
            synchronized(composeResult) {
                if (composeResult.finished) {
                    if (composeResult.ret) {
                        _observer.taskSucceed()
                    } else {
                        _observer.taskFailed(composeResult.failMessage)
                    }
                } else {
                    if (composeResult.curStep != null) {
                        _observer.setTaskStepInfo(composeResult.curStep)
                    }
                }
            }
        }

        fun buildAgain() {
            resetStatus()
            showNotification()
            startComposeThread()
        }

        // Stop the build thread
        fun stopBuilding() {
            composeThread?.let { thread ->
                if (thread.isActive) {
                    thread.stopRunning()
                }
            }
            hideNotification()
        }

        fun setBuildHooker(extraMaker: IApkMaking?) {
            this@ApkComposeService.extraMaker = extraMaker
        }

        // Get key/value maps
        val values: Map<String, Any?>
            get() {
                val ret: MutableMap<String, Any?> = HashMap()
                ret["srcApkPath"] = srcApkPath
                ret["targetApkPath"] = targetApkPath
                ret["decodeRootPath"] = decodeRootPath
                modifiedSmaliFolders?.let { codeModified ->
                    ret["codeModified"] = codeModified.isNotEmpty()
                }
                ret["signAPK"] = signAPK
                return ret
            }

        fun hideNotification() {
            mNotificationManager?.let { manager ->
                manager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE)
                if (foregroundStarted) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    foregroundStarted = false
                }
                composeThread?.let { thread ->
                    if (thread.isActive) {
                        thread.stopRunning()
                    }
                }
                mNotificationManager = null
                Log.e("DEBUG", "notification hidden.")
            }
        }
        fun showNotification() {
            this@ApkComposeService.showNotification()
        }

        // Build thread is still running
        val isRunning: Boolean
            get() = composeThread != null && composeThread!!.isActive
    }
}