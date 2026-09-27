package app.droprun;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.util.concurrent.*;

/** User-started, bounded foreground download of task results while another app is open. */
public class TaskSyncService extends Service {
    static final String CHANNEL="task-receiving";
    static final int NOTIFICATION_ID=730;
    static volatile boolean running=false;
    final Handler handler=new Handler(Looper.getMainLooper());
    final ExecutorService io=Executors.newSingleThreadExecutor();
    Store store;
    long startedAt,offlineAt=-1;
    boolean busy=false,stopped=false;
    final Runnable poll=this::fetch;
    final Runnable limit=()->stopReceiving(TaskWatchPolicy.stopReason(store.watchCount(),TaskWatchPolicy.SESSION_MS,0));

    /** Call only from a visible activity or its explicit submit/approval action. */
    static void start(Activity activity) {
        Store store=new Store(activity);
        if(!store.paired()||activity.isFinishing()||activity.isDestroyed())return;
        SyncJob.schedule(activity);
        try{activity.startForegroundService(new Intent(activity,TaskSyncService.class));}
        catch(RuntimeException e){
            store.prefs.edit().putString("receiverNotice",L.t("Android cannot start live receiving right now. Handoffs are retained and will sync periodically. Open DropRun to refresh.","系统暂不允许即时接收。任务仍保留，结果由系统周期同步；回到 DropRun 可刷新。")).apply();
            SyncJob.soon(activity);
        }
    }
    static void startIfNeeded(Activity activity) {
        if(!running&&new Store(activity).watchCount()>0)start(activity);
    }
    @Override public void onCreate() {
        super.onCreate();store=new Store(this);startedAt=SystemClock.elapsedRealtime();
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,L.t("Receiving results","接收任务结果"),NotificationManager.IMPORTANCE_LOW));
        try{
            Notification notification=notification(L.t("Sending handoffs and receiving results","正在发送任务并接收结果"));
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(NOTIFICATION_ID,notification);
            running=true;store.prefs.edit().remove("receiverNotice").apply();
            manager.cancel(731);
            if(!store.prefs.getBoolean("notificationsInitialized",false))TaskNotifications.update(this,store.prefs,store.tasks());
        }catch(RuntimeException e){stopReceiving(L.t("Android cannot start live receiving right now. Handoffs are retained and will sync periodically. Open DropRun to refresh.","系统暂不允许即时接收。任务仍保留，结果由系统周期同步；回到 DropRun 可刷新。"));}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        startedAt=SystemClock.elapsedRealtime();offlineAt=-1;
        handler.removeCallbacks(limit);if(!stopped)handler.postDelayed(limit,TaskWatchPolicy.SESSION_MS);
        if(!stopped&&!busy){handler.removeCallbacks(poll);handler.post(poll);}
        return START_NOT_STICKY;
    }
    Notification notification(String text) {
        PendingIntent open=PendingIntent.getActivity(this,NOTIFICATION_ID,new Intent(this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(L.t("DropRun · receiving results","DropRun · 正在接收任务结果")).setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_PROGRESS);
        if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }
    void fetch() {
        if(stopped||busy)return;
        if(!store.paired()){stopReceiving("");return;}
        String reason=TaskWatchPolicy.stopReason(store.watchCount(),SystemClock.elapsedRealtime()-startedAt,offlineAt<0?0:SystemClock.elapsedRealtime()-offlineAt);
        if(reason!=null){stopReceiving(reason);return;}
        busy=true;
        io.execute(()->{
            boolean connected=false;
            try{if(store.online()){store.sync(false);connected=store.computerOnline();}}catch(Exception ignored){}
            boolean connection=connected;
            handler.post(()->{
                busy=false;if(stopped)return;
                long now=SystemClock.elapsedRealtime();
                if(connection)offlineAt=-1;else if(offlineAt<0)offlineAt=now;
                int active=store.watchCount();
                String stop=TaskWatchPolicy.stopReason(active,now-startedAt,offlineAt<0?0:now-offlineAt);
                if(stop!=null){stopReceiving(stop);return;}
                String status=connection?active+L.t(" handoffs awaiting results · we will notify you when ready"," 个任务等待结果 · 完成或需要处理时通知你"):L.t("Connection interrupted · retrying with your handoffs saved","连接暂时中断，正在重试 · 任务仍保留");
                ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification(status));
                handler.postDelayed(poll,connection?TaskWatchPolicy.POLL_MS:30000);
            });
        });
    }
    void stopReceiving(String reason) {
        if(stopped)return;stopped=true;running=false;handler.removeCallbacksAndMessages(null);
        if(!reason.isEmpty()){
            store.prefs.edit().putString("receiverNotice",reason).apply();
            TaskNotifications.receiverPaused(this,reason);
            SyncJob.soon(this);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    @Override public void onTimeout(int startId,int foregroundServiceType) {
        stopReceiving(L.t("Android ended this live receiving session. Your computer keeps working. Results will sync periodically; open DropRun to receive now.","系统已结束本次即时接收。电脑任务继续，结果由系统周期同步；打开 DropRun 可恢复即时接收。"));
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){stopped=true;running=false;handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
