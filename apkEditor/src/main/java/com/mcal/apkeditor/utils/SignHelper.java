package com.mcal.apkeditor.utils;

import com.mcal.apklib.sign.SignApk;
import com.mcal.common.utils.ScopedStorage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class SignHelper {
    public static void sign(String sourceApkPath, String targetApkPath, Map<String, String> replacedFiles,
                            Map<String, String> addedFiles, Set<String> deletedFiles)
            throws IOException {
        InputStream publicKeyInput = new FileInputStream(ScopedStorage.getPublicKey());
        InputStream privateKeyInput = new FileInputStream(ScopedStorage.getPrivateKey());
        SignApk.signAPK(publicKeyInput, privateKeyInput, sourceApkPath,
                targetApkPath, addedFiles, deletedFiles, replacedFiles, 9);
    }
}
