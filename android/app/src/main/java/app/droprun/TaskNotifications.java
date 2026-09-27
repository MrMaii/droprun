package app.droprun;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.util.*;

final class TaskNotifications {
    static boolean allowed(Context context) {
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel=manager.getNotificationChannel("tasks");
        return (Build.VERSION.SDK_INT<33||context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&manager.areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    static void receiverPaused(Context context,String reason) {
        if(!allowed(context))return;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("tasks",L.t("Handoff results","任务结果"),NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(context,731,new Intent(context,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(731,new Notification.Builder(context,"tasks").setSmallIcon(R.drawable.ic_notification).setContentTitle(L.t("DropRun · live receiving paused","DropRun · 即时接收已暂停")).setContentText(reason).setStyle(new Notification.BigTextStyle().bigText(reason)).setContentIntent(open).setAutoCancel(true).build());
    }
    static synchronized void update(Context context, android.content.SharedPreferences prefs, JSONArray tasks) {
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("tasks",L.t("Handoff results","任务结果"),NotificationManager.IMPORTANCE_DEFAULT));
        Set<String> seen=new HashSet<>(prefs.getStringSet("notified",Collections.emptySet()));
        boolean initialized=prefs.getBoolean("notificationsInitialized",false);
        for(int i=0;tasks!=null&&i<tasks.length();i++) {
            JSONObject task=tasks.optJSONObject(i);if(task==null)continue;String id=task.optString("id"),status=task.optString("status");
            JSONArray approvals=task.optJSONArray("approvals");boolean approval=status.equals("waiting_for_approval")&&approvals!=null&&approvals.length()>0;
            boolean plan=status.equals("awaiting_plan_approval");String notificationKey=TaskPresentation.notificationKey(id,status,task.optString("plan_version"),approval?approvals.optJSONObject(0).optString("id"):"");
            boolean preview=prefs.getBoolean("previewRequested:"+id,false)&&!task.optString("preview_status").equals("reopening");
            if(preview)notificationKey=id+":preview:"+task.optLong("preview_expires_at")+":"+task.optString("preview_version");
            if((!plan&&!approval&&!Arrays.asList("completed","blocked","failed","cancelled").contains(status))||seen.contains(notificationKey))continue;
            boolean allowed=allowed(context);
            if(initialized&&!allowed)continue;
            seen.add(notificationKey);
            if(!initialized)continue;
            Intent intent=new Intent(context,MainActivity.class).putExtra("taskId",id).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent click=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            String result=plan?L.t("Your plan is ready","理解与计划已就绪"):approval?L.t("Your command decision is needed","等待你的命令审批"):status.equals("completed")?L.t("Delivery ready","任务已完成"):status.equals("cancelled")?L.t("Handoff cancelled","任务已取消"):L.t("Your attention is needed","任务需要处理");
            String details=plan?L.t("Review the plan; edits wait for your approval","点击查看计划，批准后才在原项目开工"):approval?L.t("Review the command and approve or deny this request only","点击核对命令，仅批准本次或拒绝"):status.equals("completed")?L.t("Tap to read the Codex delivery report","点击查看 Codex 交付报告"):status.equals("cancelled")?L.t("Tap to view the handoff","点击查看任务状态"):task.optString("error",L.t("Tap to see what needs attention","点击查看受阻原因"));
            if(preview){boolean ready=TaskPresentation.previewStatus(task.optString("preview_status"),task.optString("preview_url"),task.optLong("preview_expires_at"),System.currentTimeMillis()).equals("ready");result=ready?L.t("Preview reopened","预览已重开"):L.t("Preview could not be reopened","预览暂无法重开");details=ready?L.t("Tap to open this delivery's preview","点击打开这次交付的预览"):L.t("View preview status; screenshots and reports are retained","点击查看预览状态，截图和报告仍保留");prefs.edit().remove("previewRequested:"+id).apply();}
            manager.notify(id.hashCode(),new Notification.Builder(context,"tasks").setSmallIcon(app.droprun.R.drawable.ic_notification).setContentTitle(task.optString("project_name")+" · "+result).setContentText(details).setContentIntent(click).setAutoCancel(true).build());
        }
        prefs.edit().putStringSet("notified",seen).putBoolean("notificationsInitialized",true).apply();
    }
}
