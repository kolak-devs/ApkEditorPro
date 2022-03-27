package com.mcal.apkeditor.patch;

import com.mcal.apkeditor.activities.ApkInfoActivity;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface IBeforeAddFile {

    // Return true means already consumed
    public boolean consumeAddedFile(ApkInfoActivity activity, ZipFile zfile,
                                    ZipEntry entry) throws Exception;
}
