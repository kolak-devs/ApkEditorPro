package com.mcal.apkeditor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.mcal.apklib.sign.SignApk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class SignHelper {
    public static void sign(@NonNull Context ctx, String sourceApkPath,
                            String targetApkPath, Map<String, String> replacedFiles,
                            Map<String, String> addedFiles, Set<String> deletedFiles)
            throws IOException {
        AssetManager am = ctx.getAssets();
        String keyName = "testkey";

        // Custom Key (keys are from file)
        if (keyName.charAt(0) == 'c' && keyName.charAt(1) == 'u') {
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            String privKeyPath = sp.getString(
                    "PrivateKeyPath", "");
            String pubKeyPath = sp.getString("PublicKeyPath",
                    "");
            InputStream publicKeyInput = new FileInputStream(pubKeyPath);
            InputStream privateKeyInput = new FileInputStream(privKeyPath);
            SignApk.signAPK(publicKeyInput, privateKeyInput, sourceApkPath,
                    targetApkPath, addedFiles, deletedFiles, replacedFiles,
                    9);
        }
        // Keys are in assets
        else {
            InputStream publicKeyInput = am.open("key/testkey.x509.pem");
            InputStream privateKeyInput = am.open("key/testkey.pk8");
            SignApk.signAPK(publicKeyInput, privateKeyInput, sourceApkPath,
                    targetApkPath, addedFiles, deletedFiles, replacedFiles,
                    9);
        }
    }
}
