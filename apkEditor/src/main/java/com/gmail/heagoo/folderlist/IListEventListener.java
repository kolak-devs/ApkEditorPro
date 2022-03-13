package com.gmail.heagoo.folderlist;

import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

public interface IListEventListener {

	public void dirChanged(String newDir);
	
	public void fileRenamed(String dirPath, String oldName, String newName);
	
	public void fileDeleted(String dirPath, String fileName);

	public void itemLongClicked(ContextMenu menu, View v,
			ContextMenuInfo menuInfo);

	// Return true if the listener can deal with the click event
	public boolean fileClicked(View view, String filePath);
}
