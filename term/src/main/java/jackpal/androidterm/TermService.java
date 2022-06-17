package jackpal.androidterm;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.libtermexec.v1.ITerminal;
import jackpal.androidterm.util.SessionList;
import jackpal.androidterm.util.TermSettings;

public class TermService extends Service implements TermSession.FinishCallback {
    private static final int RUNNING_NOTIFICATION = 1;
    private final IBinder mTSBinder = new TSBinder();
    private SessionList mTermSessions;

    /* This should be @Override if building with API Level >=5 */
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        if (TermExec.SERVICE_ACTION_V1.equals(intent.getAction())) {
            Log.i("TermService", "Outside process called onBind()");

            return new RBinder();
        } else {
            Log.i("TermService", "Activity called onBind()");

            return mTSBinder;
        }
    }

    @Override
    public void onCreate() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        String defValue = getDir("HOME", MODE_PRIVATE).getAbsolutePath();
        String homePath = prefs.getString("home_path", defValue);
        editor.putString("home_path", homePath);
        editor.apply();

        showNotification(this);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    public void showNotification(Context context) {
        mTermSessions = new SessionList();

        Intent intent = new Intent(context, Term.class);
        String channel_id = "Default";
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notifyPendingIntent = PendingIntent.getActivity(context,
                    0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        } else {
            notifyPendingIntent = PendingIntent.getActivity(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel_id)
                .setSmallIcon(R.drawable.ic_stat_service_notification_icon)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(getText(R.string.application_terminal))
                .setContentText(getText(R.string.service_notify_text))
                .setContentIntent(notifyPendingIntent)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel notificationChannel = new NotificationChannel(channel_id, "Default channel", NotificationManager.IMPORTANCE_HIGH);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//
//        notificationManager.notify(0, builder.build());

        startForeground(RUNNING_NOTIFICATION, builder.build());

        Log.d(TermDebug.LOG_TAG, "TermService started");
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        for (TermSession session : mTermSessions) {
            /* Don't automatically remove from list of sessions -- we clear the
             * list below anyway and we could trigger
             * ConcurrentModificationException if we do */
            session.setFinishCallback(null);
            session.finish();
        }
        mTermSessions.clear();
    }

    public SessionList getSessions() {
        return mTermSessions;
    }

    public void onSessionFinish(TermSession session) {
        mTermSessions.remove(session);
    }

    public class TSBinder extends Binder {
        TermService getService() {
            Log.i("TermService", "Activity binding to service");
            return TermService.this;
        }
    }

    private final class RBinder extends ITerminal.Stub {
        @Nullable
        @Override
        public IntentSender startSession(final ParcelFileDescriptor pseudoTerminalMultiplexerFd,
                                         final ResultReceiver callback) {
            final String sessionHandle = UUID.randomUUID().toString();

            // distinct Intent Uri and PendingIntent requestCode must be sufficient to avoid collisions
            final Intent switchIntent = new Intent(RemoteInterface.PRIVACT_OPEN_NEW_WINDOW)
                    .setData(Uri.parse(sessionHandle))
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(RemoteInterface.PRIVEXTRA_TARGET_WINDOW, sessionHandle);

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0, switchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            }else {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0, switchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            }

            final PackageManager pm = getPackageManager();
            final String[] pkgs = pm.getPackagesForUid(getCallingUid());
            if (pkgs == null || pkgs.length == 0)
                return null;

            for (String packageName : pkgs) {
                try {
                    final PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);

                    final ApplicationInfo appInfo = pkgInfo.applicationInfo;
                    if (appInfo == null)
                        continue;

                    final CharSequence label = pm.getApplicationLabel(appInfo);

                    if (!TextUtils.isEmpty(label)) {
                        final String niceName = label.toString();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            GenericTermSession session = null;
                            try {
                                final TermSettings settings = new TermSettings(getResources(),
                                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

                                session = new BoundSession(pseudoTerminalMultiplexerFd, settings, niceName);

                                mTermSessions.add(session);

                                session.setHandle(sessionHandle);
                                session.setFinishCallback(new RBinderCleanupCallback(pendingIntent, callback));
                                session.setTitle("");

                                session.initializeEmulator(80, 24);
                            } catch (Exception whatWentWrong) {
                                Log.e("TermService", "Failed to bootstrap AIDL session: "
                                        + whatWentWrong.getMessage());

                                if (session != null)
                                    session.finish();
                            }
                        });

                        return pendingIntent.getIntentSender();
                    }
                } catch (PackageManager.NameNotFoundException ignore) {
                }
            }

            return null;
        }
    }

    private final class RBinderCleanupCallback implements TermSession.FinishCallback {
        private final PendingIntent result;
        private final ResultReceiver callback;

        public RBinderCleanupCallback(PendingIntent result, ResultReceiver callback) {
            this.result = result;
            this.callback = callback;
        }

        @Override
        public void onSessionFinish(TermSession session) {
            result.cancel();

            callback.send(0, new Bundle());

            mTermSessions.remove(session);
        }
    }
}