package com.mcal.folderlist;

import android.graphics.drawable.Drawable;

import com.mcal.common.utils.FileRecord;

public interface IListItemProducer {

    public Drawable getFileIcon(String dirPath, FileRecord record);

    public String getDetail1(String dirPath, FileRecord record);
}
