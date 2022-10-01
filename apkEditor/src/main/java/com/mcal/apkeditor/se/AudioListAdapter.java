package com.mcal.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.FileSelectDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ZipHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AudioListAdapter extends BaseAdapter implements OnClickListener,
        OnCompletionListener, OnItemClickListener, OnItemLongClickListener,
        IFileSelection {

    private final Activity ctx;
    private final com.mcal.apkeditor.se.ZipHelper zipHelper;
    private final List<String> audioPathList;

    // Record all the extracted audios
    private final Set<String> extractedAudios = new HashSet<>();

    // Record all the replaces
    private final Map<String, String> replaces = new HashMap<>();

    // Support music playing
    private String workingDir;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private String playingEntry;

    public AudioListAdapter(Activity ctx, @NonNull com.mcal.apkeditor.se.ZipHelper zipHelper) {
        this.ctx = ctx;
        this.zipHelper = zipHelper;
        this.audioPathList = zipHelper.audioPathList;
    }

    @Override
    public int getCount() {
        return audioPathList.size();
    }

    @Override
    public Object getItem(int position) {
        return audioPathList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String audioPath = audioPathList.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_zipfile, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);

            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.saveMenu = convertView.findViewById(R.id.menu_save);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        int pos = audioPath.lastIndexOf('/');
        String audioName = audioPath.substring(pos + 1);
        String folder = audioPath.substring(0, pos + 1);

        // Image on click listener
        viewHolder.icon.setId(position);
        viewHolder.icon.setOnClickListener(this);
        viewHolder.editMenu.setId(audioPathList.size() + position);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.saveMenu.setId(2 * audioPathList.size() + position);
        viewHolder.saveMenu.setOnClickListener(this);

        if (position != playingPosition) {
            viewHolder.icon.setImageResource(R.drawable.round_play_circle_24);
        } else {
            viewHolder.icon.setImageResource(R.drawable.round_pause_circle_24);
        }
        viewHolder.filename.setText(audioName);
        viewHolder.desc1.setText(folder);

        return convertView;
    }

    @NonNull
    private String getNameByPath(@NonNull String path) {
        int pos = path.lastIndexOf('/');
        return path.substring(pos + 1);
    }

    protected void playPauseAudio(int position) throws Exception {
        String entryName = audioPathList.get(position);

        if (this.mediaPlayer == null) {
            this.workingDir = ScopedStorage.getTmpDir().getPath();
            this.mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
        } else {
            mediaPlayer.reset();
        }

        // Upzip
        if (!extractedAudios.contains(entryName)) {
            String name = getNameByPath(entryName);
            ZipHelper.unzipFileTo(zipHelper.getFilePath(), entryName, workingDir + name);
            extractedAudios.add(entryName);
        }

        // To play the audio
        if (!entryName.equals(this.playingEntry)) {
            String replacing = this.replaces.get(entryName);
            if (replacing != null) {
                mediaPlayer.setDataSource(replacing);
            } else {
                String name = getNameByPath(entryName);
                mediaPlayer.setDataSource(workingDir + name);
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            this.playingEntry = entryName;
            this.playingPosition = position;
        }
        // To stop the playing
        else {
            mediaPlayer.stop();
            this.playingEntry = null;
            this.playingPosition = -1;
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();

        // Click on the play button
        if (id < audioPathList.size()) {
            try {
                playPauseAudio(id);
                this.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Edit/replace
        else if (id < 2 * audioPathList.size()) {
            showReplaceDlg(id - audioPathList.size());
        }

        // Extract/save
        else if (id < 3 * audioPathList.size()) {
            extractFile(id - 2 * audioPathList.size());
        }
    }

    public void destroy() {
        if (this.mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playingEntry = null;
        playingPosition = -1;
        this.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        try {
            playPauseAudio(position);
            this.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, View arg1,
                                   final int position, long arg3) {

        parent.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenuInfo menuInfo) {
                // Extract
                MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.extract);
                item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        extractFile(position);
                        return true;
                    }
                });
                // Replace
                MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                        R.string.replace);
                item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showReplaceDlg(position);
                        return true;
                    }
                });
            }
        });
        return false;

    }

    // position is the clicked item index
    private void showReplaceDlg(int position) {
        String entryPath = audioPathList.get(position);

        new FileSelectDialog(
                ctx, this, "", entryPath, ctx.getString(R.string.select_file_replace));
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String entryPath, boolean openFile) {
        this.replaces.put(entryPath, filePath);

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return com.mcal.apkeditor.se.ZipHelper.isAudio(filename);
    }

    // Extract audio file to SD card
    private void extractFile(int position) {
        String entryName = audioPathList.get(position);
        String zipPath = zipHelper.getFilePath();
        ZipFileListAdapter.extractFromZip(ctx, zipPath, entryName);
    }

    // Return entry name to file path
    public Map<String, String> getReplaces() {
        return this.replaces;
    }
}
