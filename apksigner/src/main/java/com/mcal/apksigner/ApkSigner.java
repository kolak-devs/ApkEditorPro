package com.mcal.apksigner;

import com.android.apksigner.ApkSignerTool;
import com.mcal.common.utils.ScopedStorage;

import java.util.Arrays;
import java.util.List;

public class ApkSigner {
    public void signApk(String inputPath, String outputPath) {
        List<String> args = Arrays.asList(
                "sign",
                "--in",
                inputPath,
                "--out",
                outputPath,
                "--key",
                ScopedStorage.getFilesDir() + "/bin/testkey.pk8",
                "--cert",
                ScopedStorage.getFilesDir() + "/bin/testkey.x509.pem"
        );

        try {
            ApkSignerTool.main(args.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}