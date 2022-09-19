package com.mcal.apkeditor.ui.fulleditor.utils

import brut.androlib.AndrolibException
import brut.androlib.res.data.ResPackage
import brut.androlib.res.data.ResTable
import brut.androlib.res.decoder.ARSCDecoder
import com.mcal.apkeditor.ApkDecoderMine
import com.mcal.apkeditor.ApkParseConsumer
import com.mcal.apkeditor.ApkParseThread
import com.mcal.common.data.Preferences
import com.mcal.common.utilsOld.IOUtils
import com.mcal.common.utilsOld.LOGGER
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class TaskDecoder {
    var apkPackage: ResPackage? = null
        private set

    fun decode(consumer: ApkParseConsumer, apkFile: File, decodePath: File) {
        val table = getResTable(apkFile)
        consumer.resTableDecoded(true)
        val decoder = ApkDecoderMine(table)
        decoder.decode(apkFile, decodePath)
        consumer.resourceDecoded(decoder.fileEntry2ZipEntry)
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
        LOGGER.info("Loading resource table of apk file...")
        val pkgs = getOneResPackagesFromApk(
            apkFile, resTable,
            ApkParseThread.sKeepBroken
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
    private fun loadMainPkg(resTable: ResTable, apkFile: File): ResPackage? {
        LOGGER.info("Loading resource table of apk file...")
        val pkgs = getResPackagesFromApk(
            apkFile, resTable,
            ApkParseThread.sKeepBroken
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
}