package com.mcal.apkeditor.patch.interfaces;

import android.app.Activity;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface IBeforeAddFile {

    // Return true means already consumed
    public boolean consumeAddedFile(Activity activity1, ApkInfoListener activity, ZipFile zfile, ZipEntry entry) throws Exception;
}
