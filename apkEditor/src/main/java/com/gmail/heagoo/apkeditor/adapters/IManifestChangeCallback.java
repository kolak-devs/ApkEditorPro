package com.gmail.heagoo.apkeditor.adapters;

public interface IManifestChangeCallback {
    // Try to delete the section
    // Return null if succeed, otherwise return the fail reason
    String tryToDeleteSection(LineRecord lineRec);

    // Set the new modified content
    void manifestChanged(String newContent);
}