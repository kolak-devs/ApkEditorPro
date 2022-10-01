package com.mcal.apkeditor

import android.app.Activity
import brut.androlib.AndrolibException
import brut.androlib.res.data.ResPackage
import brut.androlib.res.data.ResTable
import brut.androlib.res.decoder.ARSCDecoder
import brut.androlib.res.util.ExtFile
import com.mcal.apkeditor.ui.fulleditor.utils.TaskDecoder
import com.mcal.common.data.Preferences
import com.mcal.common.utils.deleteAll
import com.mcal.common.utilsOld.IOUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class ApkParseThread(
    activity: Activity, private val consumer: ApkParseConsumer?,
    apkPath: String?, decodeRootPath: String?,
    isFullDecoding: Boolean
) : Thread() {
    private val mActivity: Activity
    private val consumerRef: WeakReference<ApkParseConsumer?>
    private val mApkPath: String?
    private val mDecodeRootPath: String?

    // Full decoding means to decode all the files include images, assets, libs, and unknown files
    private var isFullDecoding = false

    // Record resource information
    var apkPackage: ResPackage? = null
        private set
    var resTable: ResTable? = null
        private set
    var errMessage: String? = null
        private set

    override fun run() {
        val ret = parse()
        if (!ret) {
            consumerRef.get()?.decodeFailed(errMessage)
        }
    }

    private fun parse(): Boolean {
        try {
            val apkPath = mApkPath
            val decodePath = mDecodeRootPath
            if (apkPath != null && decodePath != null) {
                val apkFile = ExtFile(File(apkPath))
                // After decoding resource table, show string list
                resTable = getResTable(apkFile)
                consumerRef.get()?.resTableDecoded(true)
                deleteAll(File(decodePath))
                val outDir = File(decodePath)
                if (!outDir.exists()) {
                    outDir.mkdirs()
                }
                TaskDecoder().decode(consumer, File(apkPath), File(decodePath))
            }
            return true
        } catch (e: Exception) {
            errMessage = e.message
            e.printStackTrace()
        }
        return false
    }

    @Throws(AndrolibException::class)
    private fun getResTable(apkFile: File, loadMainPkg: Boolean = true): ResTable {
        val resTable = ResTable()
        if (loadMainPkg) {
            if (Preferences.isFixMultiRes()) {
                loadOneMainPkg(resTable, apkFile)
            } else {
                loadMainPkg(resTable, apkFile)
            }
        }
        return resTable
    }

    @Throws(AndrolibException::class)
    private fun loadOneMainPkg(resTable: ResTable, apkFile: File): ResPackage? {
        val pkgs = getOneResPackagesFromApk(
            apkFile, resTable,
            sKeepBroken
        ) ?: return null
        apkPackage = pkgs
        if (apkPackage == null) {
            throw AndrolibException(
                "Arsc files with zero or multiple packages"
            )
        }
        resTable.addPackage(apkPackage, true)
        return apkPackage
    }

    @Throws(AndrolibException::class)
    private fun loadMainPkg(resTable: ResTable, apkFile: File): ResPackage? {
        val pkgs = getResPackagesFromApk(
            apkFile, resTable,
            sKeepBroken
        ) ?: return null
        when (pkgs.size) {
            1 -> apkPackage = pkgs[0]
            2 -> if (pkgs[0].name == "android") {
                apkPackage = pkgs[1]
            } else if (pkgs[0].name == "com.htc") {
                apkPackage = pkgs[1]
            }
        }
        if (apkPackage == null) {
            throw AndrolibException(
                "Arsc files with zero or multiple packages"
            )
        }
        resTable.addPackage(apkPackage, true)
        return apkPackage
    }

    @Throws(AndrolibException::class)
    private fun getOneResPackagesFromApk(
        apkFile: File,
        resTable: ResTable, keepBroken: Boolean
    ): ResPackage? {
        var zipFile: ZipFile? = null
        var ais: ByteArrayInputStream? = null
        try {
            zipFile = ZipFile(apkFile)
            val entry = zipFile.getEntry("resources.arsc")
            if (entry != null) {
                val size = entry.size.toInt()
                val data = ByteArray(size)
                IOUtils.readFully(zipFile.getInputStream(entry), data)
                ais = ByteArrayInputStream(data)
                return ARSCDecoder
                    .decode(ais, false, keepBroken, resTable)
                    .onePackage
            }
        } catch (e: IOException) {
            throw AndrolibException(
                "Could not read resources.arsc from file: $apkFile", e
            )
        } finally {
            try {
                zipFile?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (ais != null) try {
                ais.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    @Throws(AndrolibException::class)
    private fun getResPackagesFromApk(
        apkFile: File,
        resTable: ResTable, keepBroken: Boolean
    ): Array<ResPackage>? {
        var zipFile: ZipFile? = null
        var ais: ByteArrayInputStream? = null
        try {
            zipFile = ZipFile(apkFile)
            val entry = zipFile.getEntry("resources.arsc")
            if (entry != null) {
                val size = entry.size.toInt()
                val data = ByteArray(size)
                IOUtils.readFully(zipFile.getInputStream(entry), data)
                ais = ByteArrayInputStream(data)
                return ARSCDecoder
                    .decode(ais, false, keepBroken, resTable)
                    .packages
            }
        } catch (e: IOException) {
            throw AndrolibException(
                "Could not read resources.arsc from file: $apkFile", e
            )
        } finally {
            try {
                zipFile?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (ais != null) try {
                ais.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    companion object {
        var sKeepBroken = false
    }

    init {
        mActivity = activity
        consumerRef = WeakReference(consumer)
        mApkPath = apkPath
        mDecodeRootPath = decodeRootPath
        this.isFullDecoding = isFullDecoding;
    }
}