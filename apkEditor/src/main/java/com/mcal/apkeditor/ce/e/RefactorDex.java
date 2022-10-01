package com.mcal.apkeditor.ce.e;

import static com.mcal.common.utils.StringHelperKt.getRandomString;

import android.content.Context;

import com.mcal.apkeditor.ce.IApkMaking;
import com.mcal.apkeditor.ce.IDescriptionUpdate;
import com.mcal.apkeditor.dex.DexStringEditor;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ZipHelper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RefactorDex implements IApkMaking, Serializable {
    private final String oldPath;
    private final String newPath;

    public RefactorDex(String oldPath, String newPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath, Map<String, String> allReplaces, IDescriptionUpdate updater) throws Exception {
        List<String> entryList = ZipHelper.listFiles(apkFilePath, "");
        for (String entry : entryList) {
            if (entry.endsWith(".dex")) {
                String savePath = ScopedStorage.getTmpDir() + getRandomString(6) + ".dex";
                DexStringEditor editor = new DexStringEditor(apkFilePath, entry);
                if (editor.refactorPackageName(oldPath, newPath, savePath)) {
                    allReplaces.put(entry, savePath);
                }
            }
        }
    }
}
