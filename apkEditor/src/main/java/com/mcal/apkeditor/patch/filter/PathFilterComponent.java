package com.mcal.apkeditor.patch.filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.patch.PathFilter;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;
import com.mcal.common.utils.SignatureHelper;

import java.io.File;
import java.util.List;

// Support "[APPLICATION]" "[ACTIVITIES]" "LAUNCHER_ACTIVITIES"
public class PathFilterComponent extends PathFilter {

    private final ComponentType compType;
    private final String decodeRootPath;
    private String applicationName;
    private String signature;
    private List<String> componentList;
    private int cursor = 0;

    public PathFilterComponent(@NonNull IPatchContext ctx, @NonNull ComponentType compType) {
        this.compType = compType;
        this.decodeRootPath = ctx.getDecodeRootPath();
        switch (compType) {
            case APPLICATION:
                this.applicationName = ctx.getApplicationName();
                break;
            case ACTIVITY:
                this.componentList = ctx.getActivities();
                break;
            case LAUNCHER_ACTIVITY:
                this.componentList = ctx.getLauncherActivities();
                break;
            case SIGNATURE:
                this.signature = SignatureHelper.getApkSignatureData(decodeRootPath);
                break;
        }
    }

    @Override
    public String getNextEntry() {
        switch (compType) {
            case APPLICATION:
                if (cursor == 0) {
                    cursor += 1;
                    return getSmaliPath(applicationName);
                }
                break;
            case ACTIVITY:
            case LAUNCHER_ACTIVITY:
                if (cursor < this.componentList.size()) {
                    return getSmaliPath(componentList.get(cursor++));
                }
                break;
            case SIGNATURE:
                return signature;
        }

        return null;
    }

    private String getSmaliPath(String clsName) {
        String path = getRelativePath("smali", clsName, true);

        int index = 2;
        while (path == null && index < 8) {
            path = getRelativePath("smali_classes" + index, clsName, true);
            index += 1;
        }

        // When path is still null, return the default one
        if (path == null) {
            path = getRelativePath("smali", clsName, false);
        }

        return path;
    }

    @Nullable
    private String getRelativePath(String smaliFolderName, @NonNull String clsName, boolean notExistRetNull) {
        String relativePath = smaliFolderName + "/" +
                clsName.replaceAll("\\.", "/") + ".smali";
        String absolutionPath = decodeRootPath + "/" + relativePath;
        if (notExistRetNull) {
            return new File(absolutionPath).exists() ? relativePath : null;
        } else {
            return relativePath;
        }
    }

    @Override
    public boolean isTarget(@NonNull String entryPath) {
        int pos = entryPath.indexOf('/');
        if (pos != -1 && entryPath.endsWith(".smali")) {
            String str = entryPath.substring(pos + 1, entryPath.length() - 6);
            String clsName = str.replaceAll("/", ".");

            switch (compType) {
                case APPLICATION:
                    return clsName.equals(applicationName);
                case ACTIVITY:
                case LAUNCHER_ACTIVITY:
                    return componentList.contains(clsName);
            }
        }
        return false;
    }

    @Override
    public boolean isSmaliNeeded() {
        return true;
    }

    @Override
    public boolean isWildMatch() {
        return false;
    }

    public enum ComponentType {
        APPLICATION,
        ACTIVITY,
        LAUNCHER_ACTIVITY,
        SIGNATURE
    }
}
