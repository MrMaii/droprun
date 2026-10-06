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
    final ExecutorService io=Executors.newSingleThreadExecutor(),local=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
    final List<JSONObject> rows=new ArrayList<>();final Set<String> pendingIds=new HashSet<>();final HistoryAdapter adapter=new HistoryAdapter();
    Store store;String projectId,projectName,cursor="",snapshot="";TextView notice,emptyTitle,emptyDetail;ListView list;Button more;ImageButton reload;LinearLayout empty;boolean busy,foreground,paged;Parcelable restoredScroll;int emptyReadState; // 0 checking, 1 checked, 2 failed
    Removal removal;final Runnable removalObserver=this::updateRemoval;String readError="",actionError="";
    final Runnable refresh=()->{if(paged){render();handler.postDelayed(this.refresh,7000);}else load(false);};
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=createStore();projectId=getIntent().getStringExtra("projectId");projectName=getIntent().getStringExtra("projectName");if(projectId==null){finish();return;}
        removal=(Removal)getLastNonConfigurationInstance();if(removal!=null&&!removal.store.scope.equals(store.scope))removal=null;
        if(state!=null&&store.scope.equals(state.getString("actionScope",store.scope))){actionError=state.getString("actionError","");String interrupted=state.getString("removing","");if(removal==null&&!interrupted.isEmpty()){JSONArray pending=store.pending();for(int n=0;n<pending.length();n++)if(interrupted.equals(pending.optJSONObject(n).optString("id")))actionError=L.t("Removal was interrupted. The saved copy is still here; you can try again.","移除操作中断，已保存副本仍在，可以重试。");}}
        if(state!=null)restoredScroll=state.getParcelable("scroll");Ui.configureWindow(this);LinearLayout page=Ui.column(this);LinearLayout header=Ui.vertical(this);header.setPadding(dp(20),dp(4),dp(20),0);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back to projects","返回项目"));back.setOnClickListener(v->finish());reload=Ui.iconButton(this,R.drawable.ic_refresh,L.t("Refresh project history","刷新项目历史"));reload.setOnClickListener(v->{paged=false;load(false);});Ui.topBar(this,header,back,L.t("Handoffs","交办"),false,reload);page.addView(header);
        LinearLayout identity=Ui.row(this);TextView name=Ui.title(this,projectName==null?projectId:projectName,24);name.setTag("history-project-name");name.setMaxLines(getResources().getConfiguration().fontScale>=1.5f?3:2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setContentDescription(name.getText());identity.addView(name,Ui.grow());
        ImageButton info=Ui.iconButton(this,R.drawable.ic_info,L.t("Project information","项目信息"));info.setTag("history-project-info");info.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L.t("Project information","项目信息")).setMessage(name.getText()+"\n\n"+L.t("Project ID: ","项目 ID：")+projectId).setPositiveButton(L.t("Close","关闭"),null).show());identity.addView(info,Ui.square(this,48));header.addView(identity,Ui.margins(this,8,4));
        if(projectName!=null){String label=store.projectLabel(projectId,projectName);if(!label.equals(projectName))header.addView(Ui.caption(this,L.t("Project · ","项目 · ")+label.substring(projectName.length()+3)));}
        notice=Ui.text(this,"",14,Ui.AMBER);notice.setPadding(dp(24),dp(10),dp(24),dp(10));notice.setMinimumHeight(dp(48));notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);Ui.bindPress(notice);notice.setOnClickListener(v->{if(!readError.isEmpty()&&actionError.isEmpty()&&!removing())new AlertDialog.Builder(this).setTitle(L.t("History could not refresh","暂时无法刷新历史")).setMessage(readError).setPositiveButton(L.t("Close","关闭"),null).show();});page.addView(notice);
        FrameLayout stage=new FrameLayout(this);page.addView(stage,new LinearLayout.LayoutParams(-1,0,1));list=new ListView(this);list.setDivider(null);list.setSelector(android.R.color.transparent);list.setPadding(dp(20),dp(10),dp(20),dp(24));list.setClipToPadding(false);list.setVerticalScrollBarEnabled(false);
        more=Ui.button(this,L.t("Load earlier handoffs","查看更早交办"),false);more.setOnClickListener(v->load(true));list.addFooterView(more,null,false);list.setAdapter(adapter);list.setItemsCanFocus(true);stage.addView(list,new FrameLayout.LayoutParams(-1,-1));
        empty=Ui.vertical(this);empty.setPadding(dp(28),dp(60),dp(28),dp(28));empty.setGravity(Gravity.CENTER);emptyTitle=Ui.title(this,L.t("Loading handoffs…","正在读取交办…"),22);empty.addView(emptyTitle);emptyDetail=Ui.caption(this,L.t("Checking this project's history.","正在读取这个项目的交办记录。"));empty.addView(emptyDetail);stage.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        JSONArray cached=store.history(projectId).optJSONArray("tasks");paged=cached!=null&&cached.length()>50;render();
        if(removal!=null){removal.observer=removalObserver;if(removal.finished)actionError=removal.error;}showNotice();
    }
    @Override protected void onResume(){super.onResume();foreground=true;updateRefresh();if(paged){render();handler.postDelayed(refresh,7000);}else load(false);}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(removal!=null&&removal.observer==removalObserver)removal.observer=null;io.shutdown();local.shutdown();super.onDestroy();}
    @Override public Object onRetainNonConfigurationInstance(){return removal;}
    @Override protected void onSaveInstanceState(Bundle state){if(list!=null)state.putParcelable("scroll",list.onSaveInstanceState());state.putString("actionScope",store.scope);state.putString("actionError",actionError);if(removing())state.putString("removing",removal.id);super.onSaveInstanceState(state);}
    int dp(int n){return Ui.dp(this,n);}
    Store createStore(){return new Store(this);}
    void updateRefresh(){
        if(reload==null)return;reload.setEnabled(!busy);reload.setAlpha(busy?0.5f:1f);
        reload.setContentDescription(busy?L.t("Refreshing history…","正在刷新历史…"):L.t("Refresh project history","刷新项目历史"));
    }
    void load(boolean append){
        handler.removeCallbacks(refresh);if(busy||!foreground)return;busy=true;updateRefresh();emptyReadState=0;updateEmptyCopy();more.setEnabled(false);String next=append?cursor:"";
        io.execute(()->{String error="";int outcome=2;try{store.loadHistory(projectId,next);outcome=1;}catch(Exception e){error=e.getMessage();}int result=outcome;String message=result==2&&(error==null||error.trim().isEmpty())?L.t("Couldn't refresh history. Use Refresh to try again.","暂时无法刷新历史，请点按刷新重试。"):error;
            runOnUiThread(()->{busy=false;if(isDestroyed()||!foreground)return;updateRefresh();emptyReadState=result;if(result==1&&append)paged=true;readError=message;showNotice();more.setEnabled(true);render();handler.postDelayed(refresh,7000);});});
    }
    void updateEmptyCopy(){
        String title=emptyReadState==0?L.t("Loading handoffs…","正在读取交办…"):emptyReadState==2?L.t("History could not load","暂时无法读取历史"):L.t("Nothing here yet","这里还没有交办");
        String detail=emptyReadState==0?L.t("Checking this project's history.","正在读取这个项目的交办记录。"):emptyReadState==2?L.t("Use Refresh to try again when connected.","联网后点按刷新，重新读取历史。"):L.t("Refresh when connected, or share something to this project.","联网后刷新，或先分享内容到这个项目。");
        if(!title.contentEquals(emptyTitle.getText()))emptyTitle.setText(title);
        if(!detail.contentEquals(emptyDetail.getText()))emptyDetail.setText(detail);
    }
    void render(){
        updateEmptyCopy();JSONObject history=store.history(projectId);JSONArray tasks=history.optJSONArray("tasks"),pending=store.pending();String next=history.toString()+pending+store.tasks();if(next.equals(snapshot))return;snapshot=next;cursor=MainActivity.text(history,"nextCursor");
        View visible=list.getChildAt(0);int offset=visible==null?0:visible.getTop()-list.getPaddingTop();
        // A pending keyboard selection is not a laid-out anchor yet.
        String anchor=(list.isInTouchMode()||list.getSelectedView()==null)&&visible!=null&&visible.getTag() instanceof Holder?((Holder)visible.getTag()).id:null;
        rows.clear();pendingIds.clear();Set<String> known=new HashSet<>();
        for(int n=0;tasks!=null&&n<tasks.length();n++)known.add(tasks.optJSONObject(n).optString("id"));
        for(int n=0;n<pending.length();n++){JSONObject task=pending.optJSONObject(n);if(!projectId.equals(task.optString("projectId"))||known.contains(task.optString("id")))continue;rows.add(task);pendingIds.add(task.optString("id"));}
        for(int n=0;tasks!=null&&n<tasks.length();n++){JSONObject task=tasks.optJSONObject(n),current=store.task(task.optString("id"));rows.add(current!=null&&current.optLong("updated_at")>=task.optLong("updated_at")?current:task);}
        adapter.notifyDataSetChanged();more.setVisibility(cursor.isEmpty()?View.GONE:View.VISIBLE);empty.setVisibility(rows.isEmpty()?View.VISIBLE:View.GONE);list.setVisibility(rows.isEmpty()?View.GONE:View.VISIBLE);
        if(restoredScroll!=null){list.onRestoreInstanceState(restoredScroll);restoredScroll=null;}
        else if(anchor!=null)for(int n=0;n<rows.size();n++)if(anchor.equals(rows.get(n).optString("id"))){list.setSelectionFromTop(n,offset);break;}
    }
    void removeSaved(JSONObject task){
        if(removing())return;
        new AlertDialog.Builder(this).setTitle(L.t("Remove this saved copy?","移除这份已保存副本？")).setMessage(L.t("This removes the phone's retry copy. If the Relay already received it, work may continue on your computer; check the online history to stop that task.","这会移除手机上的重试副本。如果中转服务已收到任务，电脑可能仍在工作；请联网查看历史并停止相应任务。" )).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Remove saved copy","移除已保存副本"),(d,w)->{if(removing())return;removal=new Removal(store,task.optString("id"));removal.observer=removalObserver;actionError="";updateRemoval();local.execute(removal);}).show();
    }
    boolean removing(){return removal!=null&&!removal.finished;}
    void showNotice(){boolean details=actionError.isEmpty()&&!removing()&&!readError.isEmpty();String message=!actionError.isEmpty()?actionError:removing()?L.t("Removing saved copy…","正在移除已保存副本…"):details?L.t("Couldn't refresh history. Tap for details.","暂时无法刷新历史，点按查看详情。"):"";notice.setText(message);noticeInteractive(details);notice.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);}
    void noticeInteractive(boolean details){notice.setEnabled(details);notice.setClickable(details);notice.setFocusable(details);if(!details){notice.animate().cancel();notice.setScaleX(1f);notice.setScaleY(1f);}}
    void updateRemoval(){if(isDestroyed())return;if(removal.finished)actionError=removal.error;showNotice();snapshot="";render();}
    static final class Removal implements Runnable {
        final Store store;final String id;volatile boolean finished;volatile String error="";volatile Runnable observer;
        Removal(Store store,String id){this.store=store;this.id=id;}
        public void run(){try{store.cancelPending(id);}catch(Exception e){error=L.t("Could not remove the saved copy. ","未能移除已保存副本。 ")+e.getMessage();}finally{finished=true;new Handler(Looper.getMainLooper()).post(()->{Runnable callback=observer;if(callback!=null)callback.run();});}}
    }
    void openSaved(JSONObject task){
        if(removing()&&removal.id.equals(task.optString("id")))return;
        String detail=task.optString("message")+"\n\n"+task.optString("content");JSONArray files=task.optJSONArray("localFiles");
        if(files!=null&&files.length()>0)detail+="\n\n"+files.length()+L.t(" saved attachments"," 份已保存附件");
        if(!task.optString("sendError").isEmpty())detail+="\n\n"+task.optString("sendError");
        new AlertDialog.Builder(this).setTitle(L.t("Saved on this phone","已保存在手机")).setMessage(detail.trim()).setNegativeButton(L.t("Close","关闭"),null).setPositiveButton(L.t("Retry sending","重试发送"),(d,w)->{SyncJob.soon(this);TaskSyncService.start(this);notice.setText(L.t("Retry requested. Keep the app open to see confirmation.","已请求重试，请保持 App 打开查看接收确认。"));noticeInteractive(false);notice.setVisibility(View.VISIBLE);notice.announceForAccessibility(notice.getText());}).show();
    }
    final class HistoryAdapter extends BaseAdapter {
        // AbsListView restores saved rows only when their stable ID is nonnegative.
        public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return Integer.toUnsignedLong(rows.get(p).optString("id").hashCode());}public boolean hasStableIds(){return true;}
        public View getView(int position,View recycled,ViewGroup parent){
            JSONObject task=rows.get(position);boolean pending=pendingIds.contains(task.optString("id"));LinearLayout outer;Holder holder;
            if(recycled instanceof LinearLayout&&recycled.getTag() instanceof Holder){outer=(LinearLayout)recycled;holder=(Holder)outer.getTag();}
            else {outer=Ui.vertical(ProjectHistoryActivity.this);LinearLayout card=Ui.vertical(ProjectHistoryActivity.this);card.setPadding(0,dp(20),0,dp(20));card.setMinimumHeight(dp(48));outer.addView(card);outer.addView(Ui.divider(ProjectHistoryActivity.this));TextView title=Ui.title(ProjectHistoryActivity.this,"",19);title.setMaxLines(3);card.addView(title);TextView meta=Ui.caption(ProjectHistoryActivity.this,"");LinearLayout footer=Ui.row(ProjectHistoryActivity.this);TextView status=Ui.pill(ProjectHistoryActivity.this,"",Ui.MUTED);LinearLayout statusBox=Ui.row(ProjectHistoryActivity.this);statusBox.addView(status,new LinearLayout.LayoutParams(-2,-2));footer.addView(statusBox,Ui.grow());Ui.space(footer,8);ImageView arrow=new ImageView(ProjectHistoryActivity.this);arrow.setImageResource(R.drawable.ic_chevron_left);arrow.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.MUTED));arrow.setRotation(180);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);footer.addView(arrow,Ui.square(ProjectHistoryActivity.this,18));card.addView(footer,Ui.margins(ProjectHistoryActivity.this,6,0));card.addView(meta,Ui.margins(ProjectHistoryActivity.this,6,0));Button remove=Ui.button(ProjectHistoryActivity.this,L.t("Remove saved copy","移除已保存副本"),false);card.addView(remove,Ui.margins(ProjectHistoryActivity.this,12,0));holder=new Holder(card,title,status,meta,remove);outer.setTag(holder);Ui.bindPress(card);}
            String status=task.optString("status"),label=pending?L.t("Saved on this phone","已保存在手机"):TaskPresentation.status(status);int color=pending?Ui.AMBER:TaskPresentation.statusColor(status);holder.status.setText(label);holder.status.setTextColor(color);((android.graphics.drawable.GradientDrawable)holder.status.getBackground()).setColor((color&0x00ffffff)|0x1F000000);holder.title.setText(MainActivity.name(task));
            holder.id=task.optString("id");boolean deleting=removing()&&removal.id.equals(holder.id);holder.remove.setText(deleting?L.t("Removing…","正在移除…"):L.t("Remove saved copy","移除已保存副本"));holder.remove.setEnabled(!removing());holder.card.setEnabled(!deleting);
            String kind=MainActivity.text(task,"parent_task_id").isEmpty()?L.t("Shared","分享"):L.t("Follow-up","追问");holder.meta.setText(deleting?L.t("Removing only this phone's copy.","仅移除手机上的保存副本。"):pending?(task.optString("sendError").isEmpty()?L.t("Tap to inspect or retry","点按查看或重试"):task.optString("sendError")):kind+" · "+TaskPresentation.elapsed(task.optLong("created_at"),System.currentTimeMillis()));holder.remove.setVisibility(pending?View.VISIBLE:View.GONE);holder.remove.setOnClickListener(v->removeSaved(task));holder.card.setFocusable(true);holder.card.setClickable(true);holder.card.setOnClickListener(v->{if(pending)openSaved(task);else startActivity(new Intent(ProjectHistoryActivity.this,TaskActivity.class).putExtra("taskId",task.optString("id")));});return outer;
        }
    }
    static final class Holder {String id;final LinearLayout card;final TextView title,status,meta;final Button remove;Holder(LinearLayout card,TextView title,TextView status,TextView meta,Button remove){this.card=card;this.title=title;this.status=status;this.meta=meta;this.remove=remove;}}
}
