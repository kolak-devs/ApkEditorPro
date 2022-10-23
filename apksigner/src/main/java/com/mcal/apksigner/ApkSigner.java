package com.mcal.apksigner;

import android.util.Log;

import com.android.apksigner.ApkSignerTool;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ScopedStorage;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ApkSigner {
    private static final String TAG = "ApkSigner";

    public void signApk(String inputPath, String outputPath) {
        List<String> args = Arrays.asList(
                "sign",
                "--in",
                inputPath,
                "--out",
                outputPath,
                "--key",
                ScopedStorage.getFilesDir() + File.separator + "bin/testkey.pk8",
                "--cert",
                ScopedStorage.getFilesDir() + File.separator + "bin/testkey.x509.pem"
        );

        try {
            ApkSignerTool.main(args.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void signApkCustom(String inputPath, String outputPath) {
        List<String> args = Arrays.asList(
                "sign",
                "--in",
                inputPath,
                "--out",
                outputPath,
                "--ks",
                ScopedStorage.getKey().getPath(),
                "--ks-key-alias",
                "\"" + Preferences.getKSAlias() + "\"",
                "--ks-pass",
                "pass:\"" + Preferences.getKSPass() + "\"",
                "--key-pass",
                "pass:\"" + Preferences.getKeyPass() + "\"",
                "--v1-signing-enabled",
                "true",
                "--v2-signing-enabled",
                "true"
        );
        try {
            Log.d(TAG, "signApkCustom: " + ScopedStorage.getKey().getPath());
            Log.d(TAG, "signApkCustom: " + Arrays.toString(args.toArray(new String[0])));
            ApkSignerTool.main(args.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}