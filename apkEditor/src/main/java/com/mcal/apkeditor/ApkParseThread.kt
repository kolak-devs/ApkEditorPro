package com.mcal.apkeditor

import android.app.Activity
import brut.androlib.AndrolibException
import brut.androlib.res.data.ResPackage
import brut.androlib.res.data.ResTable
import brut.androlib.res.decoder.ARSCDecoder
import brut.androlib.res.util.ExtFile
import com.mcal.common.data.Preferences
import com.mcal.common.utils.deleteAll
import com.mcal.common.utilsOld.IOUtils
import com.mcal.common.utilsOld.LOGGER
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

class ApkParseThread(
    activity: Activity, consumer: ApkParseConsumer?,
    apkPath: String, decodeRootPath: String,
    isFullDecoding: Boolean
) : Thread() {
    private val mActivity: Activity
    private val consumerRef: WeakReference<ApkParseConsumer?>
    private val mApkPath: String
    private val mDecodeRootPath: String

    // Full decoding means to decode all the files include images, assets, libs, and unknown files
    private var isFullDecoding = false

    // Record resource information
    var apkPackage: ResPackage? = null
        private set
    var resTable: ResTable? = null
        private set
    var errMessage: String? = null
        private set
    private var decoder: ApkDecoderMine? = null
    override fun run() {
        // Play tricks to extract files: borrow ApkComposeThread to extract files
        val tmp = ApkComposeThread(mActivity, null, null, null)
        var ret = tmp.prepare()
        if (!ret) {
            consumerRef.get()?.decodeFailed(tmp.errMessage)
            return
        }
        ret = parse()
        if (!ret) {
            consumerRef.get()?.decodeFailed(errMessage)
        }
    }

    private fun parse(): Boolean {
        try {
            val activity: Activity = mActivity
            val apkFile = ExtFile(File(mApkPath))
            // After decoding resource table, show string list
            resTable = getResTable(apkFile)
            consumerRef.get()?.resTableDecoded(true)
            resTable?.let { table ->
                decoder = ApkDecoderMine(table)
            }
            deleteAll(File(mDecodeRootPath))
            val outDir = File(mDecodeRootPath)
            if (!outDir.exists()) {
                outDir.mkdirs()
            }

            // File outDir = new File("/storage/emulated/0/decoded/");
            decoder?.let { apkDecoder ->
                apkDecoder.decode(activity, apkFile, outDir)
                consumerRef.get()?.resourceDecoded(apkDecoder.fileEntry2ZipEntry)
            }
            return true
        } catch (e: Exception) {
            errMessage = e.message
            e.printStackTrace()
        }
        return false
    }

    @Throws(AndrolibException::class)
    private fun getResTable(apkFile: ExtFile, loadMainPkg: Boolean = true): ResTable {
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
    private fun loadOneMainPkg(resTable: ResTable, apkFile: ExtFile): ResPackage? {
        LOGGER.info("Loading resource table of apk file...")
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
        LOGGER.info("Loaded.")
        return apkPackage
    }

    @Throws(AndrolibException::class)
    private fun loadMainPkg(resTable: ResTable, apkFile: ExtFile): ResPackage? {
        LOGGER.info("Loading resource table of apk file...")
        val pkgs = getResPackagesFromApk(
            apkFile, resTable,
            sKeepBroken
        ) ?: return null
        when (pkgs.size) {
            1 -> apkPackage = pkgs[0]
            2 -> if (pkgs[0].name == "android") {
                LOGGER.warning("Skipping \"android\" package group")
                apkPackage = pkgs[1]
            } else if (pkgs[0].name == "com.htc") {
                LOGGER.warning("Skipping \"htc\" package group")
                apkPackage = pkgs[1]
            }
        }
        if (apkPackage == null) {
            throw AndrolibException(
                "Arsc files with zero or multiple packages"
            )
        }
        resTable.addPackage(apkPackage, true)
        LOGGER.info("Loaded.")
        return apkPackage
    }

    @Throws(AndrolibException::class)
    private fun getOneResPackagesFromApk(
        apkFile: ExtFile,
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
        apkFile: ExtFile,
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

    fun stopParse() {
        decoder?.stopDecode()
    }

    companion object {
        // ??
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