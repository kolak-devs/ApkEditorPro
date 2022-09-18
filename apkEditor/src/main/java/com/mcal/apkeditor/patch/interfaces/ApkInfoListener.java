package com.mcal.apkeditor.patch.interfaces;

import com.mcal.apkeditor.IGeneralCallback;
import com.mcal.apkeditor.ResListAdapter;
import com.mcal.common.utils.ApkInfoParser;

public interface ApkInfoListener {
    ResListAdapter getResListAdapter();

    String getDecodeRootPath();

    ApkInfoParser.AppInfo getApkInfo();

    String getApkPath();

    void setManifestModified(boolean b);

    boolean isDexDecoded();

    void decodeDex(IGeneralCallback patchExecutor);

    // Strings
    String addLanguageRetError(String strCode);
    void translateLanguage(String lang);
}
