package app.droprun;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Project history keeps older pages and scroll position while individual task statuses update. */
public class ProjectHistoryActivity extends StyledActivity {
    final ExecutorService io=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
    final List<JSONObject> rows=new ArrayList<>();final Set<String> pendingIds=new HashSet<>();final HistoryAdapter adapter=new HistoryAdapter();
    Store store;String projectId,projectName,cursor="",snapshot="";TextView notice;ListView list;Button more;LinearLayout empty;boolean busy,foreground,paged;Parcelable restoredScroll;
    final Runnable refresh=()->{if(paged){render();handler.postDelayed(this.refresh,7000);}else load(false);};
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);projectId=getIntent().getStringExtra("projectId");projectName=getIntent().getStringExtra("projectName");if(projectId==null){finish();return;}
        if(state!=null)restoredScroll=state.getParcelable("scroll");Ui.configureWindow(this);LinearLayout page=Ui.column(this);LinearLayout header=Ui.vertical(this);header.setPadding(dp(20),dp(4),dp(20),0);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back to projects","返回项目"));back.setOnClickListener(v->finish());ImageButton reload=Ui.iconButton(this,R.drawable.ic_refresh,L.t("Refresh project history","刷新项目历史"));reload.setOnClickListener(v->{paged=false;load(false);});Ui.topBar(this,header,back,projectName,false,reload);page.addView(header);
        if(projectName!=null){String label=store.projectLabel(projectId,projectName);if(!label.equals(projectName))header.addView(Ui.caption(this,L.t("Project · ","项目 · ")+label.substring(projectName.length()+3)));}
        notice=Ui.text(this,"",13,Ui.AMBER);notice.setPadding(dp(24),0,dp(24),dp(10));notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(notice);
        FrameLayout stage=new FrameLayout(this);page.addView(stage,new LinearLayout.LayoutParams(-1,0,1));list=new ListView(this);list.setDivider(null);list.setSelector(android.R.color.transparent);list.setPadding(dp(20),dp(10),dp(20),dp(24));list.setClipToPadding(false);list.setVerticalScrollBarEnabled(false);
        more=Ui.button(this,L.t("Load earlier handoffs","查看更早交办"),false);more.setOnClickListener(v->load(true));list.addFooterView(more,null,false);list.setAdapter(adapter);list.setItemsCanFocus(true);stage.addView(list,new FrameLayout.LayoutParams(-1,-1));
        empty=Ui.vertical(this);empty.setPadding(dp(28),dp(60),dp(28),dp(28));empty.setGravity(Gravity.CENTER);empty.addView(Ui.title(this,L.t("Nothing here yet","这里还没有交办"),22));empty.addView(Ui.caption(this,L.t("Refresh when connected, or share something to this project.","联网后刷新，或先分享内容到这个项目。")));stage.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        JSONArray cached=store.history(projectId).optJSONArray("tasks");paged=cached!=null&&cached.length()>50;render();
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(paged){render();handler.postDelayed(refresh,7000);}else load(false);}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){if(list!=null)state.putParcelable("scroll",list.onSaveInstanceState());super.onSaveInstanceState(state);}
    int dp(int n){return Ui.dp(this,n);}
    void load(boolean append){
        handler.removeCallbacks(refresh);if(busy||!foreground)return;busy=true;more.setEnabled(false);String next=append?cursor:"";
        io.execute(()->{String error="";try{store.loadHistory(projectId,next);}catch(Exception e){error=e.getMessage();}String message=error;
            runOnUiThread(()->{busy=false;if(isDestroyed()||!foreground)return;if(message.isEmpty()&&append)paged=true;notice.setText(message);notice.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);more.setEnabled(true);render();handler.postDelayed(refresh,7000);});});
    }
    void render(){
        JSONObject history=store.history(projectId);JSONArray tasks=history.optJSONArray("tasks"),pending=store.pending();String next=history.toString()+pending+store.tasks();if(next.equals(snapshot))return;snapshot=next;cursor=MainActivity.text(history,"nextCursor");rows.clear();pendingIds.clear();Set<String> known=new HashSet<>();
        for(int n=0;tasks!=null&&n<tasks.length();n++)known.add(tasks.optJSONObject(n).optString("id"));
        for(int n=0;n<pending.length();n++){JSONObject task=pending.optJSONObject(n);if(!projectId.equals(task.optString("projectId"))||known.contains(task.optString("id")))continue;rows.add(task);pendingIds.add(task.optString("id"));}
        for(int n=0;tasks!=null&&n<tasks.length();n++){JSONObject task=tasks.optJSONObject(n),current=store.task(task.optString("id"));rows.add(current!=null&&current.optLong("updated_at")>=task.optLong("updated_at")?current:task);}
        adapter.notifyDataSetChanged();more.setVisibility(cursor.isEmpty()?View.GONE:View.VISIBLE);empty.setVisibility(rows.isEmpty()?View.VISIBLE:View.GONE);list.setVisibility(rows.isEmpty()?View.GONE:View.VISIBLE);if(restoredScroll!=null){list.onRestoreInstanceState(restoredScroll);restoredScroll=null;}
    }
    void removeSaved(JSONObject task){
        new AlertDialog.Builder(this).setTitle(L.t("Remove this saved copy?","移除这份已保存副本？")).setMessage(L.t("This removes the phone's retry copy. If the Relay already received it, work may continue on your computer; check the online history to stop that task.","这会移除手机上的重试副本。如果中转服务已收到任务，电脑可能仍在工作；请联网查看历史并停止相应任务。" )).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Remove saved copy","移除已保存副本"),(d,w)->io.execute(()->{try{store.cancelPending(task.optString("id"));runOnUiThread(()->{if(isDestroyed())return;snapshot="";render();});}catch(Exception e){runOnUiThread(()->{notice.setText(e.getMessage());notice.setVisibility(View.VISIBLE);});}})).show();
    }
    void openSaved(JSONObject task){
        String detail=task.optString("message")+"\n\n"+task.optString("content");JSONArray files=task.optJSONArray("localFiles");
        if(files!=null&&files.length()>0)detail+="\n\n"+files.length()+L.t(" saved attachments"," 份已保存附件");
        if(!task.optString("sendError").isEmpty())detail+="\n\n"+task.optString("sendError");
        new AlertDialog.Builder(this).setTitle(L.t("Saved on this phone","已保存在手机")).setMessage(detail.trim()).setNegativeButton(L.t("Close","关闭"),null).setPositiveButton(L.t("Retry sending","重试发送"),(d,w)->{SyncJob.soon(this);TaskSyncService.start(this);notice.setText(L.t("Retry requested. Keep the app open to see confirmation.","已请求重试，请保持 App 打开查看接收确认。"));notice.setVisibility(View.VISIBLE);notice.announceForAccessibility(notice.getText());}).show();
    }
    final class HistoryAdapter extends BaseAdapter {
        public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return rows.get(p).optString("id").hashCode();}public boolean hasStableIds(){return true;}
        public View getView(int position,View recycled,ViewGroup parent){
            JSONObject task=rows.get(position);boolean pending=pendingIds.contains(task.optString("id"));LinearLayout outer;Holder holder;
            if(recycled instanceof LinearLayout&&recycled.getTag() instanceof Holder){outer=(LinearLayout)recycled;holder=(Holder)outer.getTag();}
            else {outer=Ui.vertical(ProjectHistoryActivity.this);outer.setPadding(0,0,0,dp(12));LinearLayout card=Ui.card(ProjectHistoryActivity.this);outer.addView(card);TextView status=Ui.pill(ProjectHistoryActivity.this,"",Ui.MUTED);card.addView(status,Ui.margins(ProjectHistoryActivity.this,0,8));TextView title=Ui.title(ProjectHistoryActivity.this,"",18);title.setMaxLines(3);card.addView(title);TextView meta=Ui.caption(ProjectHistoryActivity.this,"");card.addView(meta,Ui.margins(ProjectHistoryActivity.this,8,0));Button remove=Ui.button(ProjectHistoryActivity.this,L.t("Remove saved copy","移除已保存副本"),false);card.addView(remove,Ui.margins(ProjectHistoryActivity.this,12,0));holder=new Holder(card,title,status,meta,remove);outer.setTag(holder);Ui.bindPress(card);}
            String status=task.optString("status"),label=pending?L.t("Saved on this phone","已保存在手机"):TaskPresentation.status(status);holder.status.setText(label);holder.status.setTextColor(pending?Ui.AMBER:TaskPresentation.statusColor(status));holder.title.setText(MainActivity.name(task));
            String kind=MainActivity.text(task,"parent_task_id").isEmpty()?L.t("Shared","分享"):L.t("Follow-up","追问");holder.meta.setText(pending?(task.optString("sendError").isEmpty()?L.t("Tap to inspect or retry","点按查看或重试"):task.optString("sendError")):kind+" · "+TaskPresentation.elapsed(task.optLong("created_at"),System.currentTimeMillis()));holder.remove.setVisibility(pending?View.VISIBLE:View.GONE);holder.remove.setOnClickListener(v->removeSaved(task));holder.card.setFocusable(true);holder.card.setClickable(true);holder.card.setOnClickListener(v->{if(pending)openSaved(task);else startActivity(new Intent(ProjectHistoryActivity.this,TaskActivity.class).putExtra("taskId",task.optString("id")));});return outer;
        }
    }
    static final class Holder {final LinearLayout card;final TextView title,status,meta;final Button remove;Holder(LinearLayout card,TextView title,TextView status,TextView meta,Button remove){this.card=card;this.title=title;this.status=status;this.meta=meta;this.remove=remove;}}
}
