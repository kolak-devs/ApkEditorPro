package jackpal.androidterm;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import jackpal.androidterm.util.SessionList;

public class WindowList extends ListActivity {
    private SessionList sessions;
    private WindowListAdapter mWindowListAdapter;
    private TermService mTermService;

    private final ServiceConnection mTSConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mTermService = binder.getService();
            populateList();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mTermService = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ListView listView = getListView();
        View newWindow = getLayoutInflater().inflate(R.layout.window_list_new_window, listView, false);
        listView.addHeaderView(newWindow, null, true);

        setResult(RESULT_CANCELED);

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent TSIntent = new Intent(this, TermService.class);
        if (!bindService(TSIntent, mTSConnection, BIND_AUTO_CREATE)) {
            Log.w(TermDebug.LOG_TAG, "bind to service failed!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        WindowListAdapter adapter = mWindowListAdapter;
        if (sessions != null) {
            sessions.removeCallback(adapter);
            sessions.removeTitleChangedListener(adapter);
        }
        if (adapter != null) {
            adapter.setSessions(null);
        }
        unbindService(mTSConnection);
    }

    private void populateList() {
        sessions = mTermService.getSessions();
        WindowListAdapter adapter = mWindowListAdapter;

        if (adapter == null) {
            adapter = new WindowListAdapter(sessions);
            setListAdapter(adapter);
            mWindowListAdapter = adapter;
        } else {
            adapter.setSessions(sessions);
        }
        sessions.addCallback(adapter);
        sessions.addTitleChangedListener(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent data = new Intent();
        data.putExtra(Term.EXTRA_WINDOW_ID, position - 1);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {// Action bar home button selected
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
