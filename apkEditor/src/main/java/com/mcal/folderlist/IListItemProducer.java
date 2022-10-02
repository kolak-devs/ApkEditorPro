package com.mcal.folderlist;

import android.graphics.drawable.Drawable;

import com.mcal.common.utils.FileRecord;

import org.jetbrains.annotations.Nullable;

public interface IListItemProducer {
    @Nullable
    Drawable getFileIcon(String dirPath, FileRecord record);
    @Nullable
    String getDetail1(String dirPath, FileRecord record);
}
