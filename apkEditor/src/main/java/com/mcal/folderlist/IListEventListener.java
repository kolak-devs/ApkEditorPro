package com.mcal.folderlist;

public interface IListEventListener {

    public void dirChanged(String newDir);

    public void fileRenamed(String dirPath, String oldName, String newName);

    public void fileDeleted(String dirPath, String fileName);

    public void fileAdded(String fileName);

    public void itemLongClicked();

    // Return true if the listener can deal with the click event
    public boolean fileClicked(String filePath);
}
