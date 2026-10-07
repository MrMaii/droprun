package app.droprun;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** The dashboard contains only projects that have received an actual handoff. */
public class MainActivity extends StyledActivity {
    static final int PAIR=40;
    final ExecutorService io=Executors.newSingleThreadExecutor();final Handler handler=new Handler(Looper.getMainLooper());
    final List<JSONObject> items=new ArrayList<>();final HomeAdapter adapter=new HomeAdapter();
    JSONArray catalog=new JSONArray();
    Store store;LinearLayout root;TextView notice,draftsNotice;ListView list;String snapshot="";boolean foreground,busy,checkingStatus;
    AlertDialog syncErrorDialog;
    final Runnable refresh=this::load;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=createStore();Ui.configureWindow(this);if(backgroundSyncEnabled())SyncJob.schedule(this);handleIntent(getIntent());}
    Store createStore(){return new Store(this);}
    boolean backgroundSyncEnabled(){return true;}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    @Override protected void onResume(){super.onResume();foreground=true;if(store.paired()){show();if(backgroundSyncEnabled()){TaskSyncService.startIfNeeded(this);load();}}else if(root!=null)handleIntent(new Intent());}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PAIR){store=new Store(this);if(result!=RESULT_OK&&!store.paired())finish();}}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(syncErrorDialog!=null)syncErrorDialog.dismiss();io.shutdown();super.onDestroy();}
    void handleIntent(Intent intent){
        String link=intent.getDataString();intent.setData(null);
        if(link!=null&&PairingTarget.parse(link)!=null&&store.paired()){Toast.makeText(this,L.t("Already connected. Disconnect in Settings before connecting another Relay.","已连接。请先在设置里断开，再连接另一个中转实例。"),Toast.LENGTH_LONG).show();}
        if(link!=null&&PairingTarget.parse(link)!=null&&!store.paired()){startActivityForResult(new Intent(this,PairActivity.class).putExtra("pairingLink",link),PAIR);return;}
        if(!store.paired()){startActivityForResult(new Intent(this,PairActivity.class),PAIR);return;}
        show();String id=intent.getStringExtra("taskId");intent.removeExtra("taskId");if(id!=null)startActivity(new Intent(this,TaskActivity.class).putExtra("taskId",id));
    }
    void load(){load(false);}
    void load(boolean manual){
        handler.removeCallbacks(refresh);if(!foreground||!store.paired())return;
        if(busy){if(manual){checkingStatus=true;show();}return;}busy=true;checkingStatus=manual;if(manual)show();
        io.execute(()->{
            boolean checked=manual||!backgroundSyncEnabled()||!TaskSyncService.running;
            try{if(checked)store.sync();}catch(Exception ignored){}
            runOnUiThread(()->{busy=false;boolean requested=checkingStatus;checkingStatus=false;if(isDestroyed()||!foreground)return;if(requested&&!checked){load(true);return;}show();if(backgroundSyncEnabled())handler.postDelayed(refresh,5000);});
        });
    }
    void showSyncError(String error){
        if(checkingStatus||syncErrorDialog!=null&&syncErrorDialog.isShowing())return;
        syncErrorDialog=new AlertDialog.Builder(this).setTitle(L.t("Last sync issue","上次同步问题")).setMessage(error).setNegativeButton(L.t("Close","关闭"),null).setPositiveButton(L.t("Check again","重新检查"),(dialog,which)->load(true)).show();
    }
    void build(){
        root=Ui.column(this);LinearLayout header=Ui.row(this);header.setPadding(dp(20),dp(18),dp(20),dp(12));
        TextView title=Ui.title(this,L.t("Recent handoffs","最近交办"),27);header.addView(title,Ui.grow());
        ImageButton gear=Ui.iconButton(this,R.drawable.ic_gear,L.t("Settings","设置"));gear.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(gear,Ui.square(this,48));root.addView(header);
        notice=Ui.text(this,"",12,Ui.AMBER);notice.setPadding(dp(20),dp(4),dp(20),dp(16));notice.setMinHeight(dp(48));notice.setFocusable(true);Ui.bindPress(notice);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(notice);
        draftsNotice=Ui.linkButton(this,"");draftsNotice.setPadding(dp(24),dp(10),dp(24),dp(10));draftsNotice.setOnClickListener(v->startActivity(new Intent(this,ShareDraftsActivity.class)));LinearLayout.LayoutParams draftMargins=Ui.margins(this,0,12);draftMargins.setMargins(dp(20),0,dp(20),dp(12));root.addView(draftsNotice,draftMargins);
        FrameLayout stage=new FrameLayout(this);root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        list=new ListView(this);list.setId(android.R.id.list);list.setAdapter(adapter);list.setItemsCanFocus(true);list.setDivider(null);list.setSelector(android.R.color.transparent);list.setVerticalScrollBarEnabled(false);list.setClipToPadding(false);list.setPadding(dp(20),0,dp(20),dp(28));stage.addView(list,new FrameLayout.LayoutParams(-1,-1));
        ScrollView emptyScroll=new ScrollView(this);emptyScroll.setFillViewport(true);emptyScroll.setVerticalScrollBarEnabled(false);
        LinearLayout empty=Ui.vertical(this);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(32),dp(24),dp(32),dp(64));
        ImageView plane=new ImageView(this);plane.setImageResource(R.drawable.ic_plane);plane.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));plane.setPadding(dp(22),dp(22),dp(22),dp(22));plane.setBackground(Ui.circle(this,Ui.LIME_SOFT,Ui.LINE));empty.addView(plane,Ui.square(this,88));Ui.space(empty,24);
        TextView heading=Ui.title(this,L.t("Your next idea starts elsewhere.","下一个好想法，就在手边。"),24);heading.setGravity(Gravity.CENTER);empty.addView(heading);
        TextView hint=Ui.text(this,L.t("In another app, share a link, photo or video. Choose DropRun, then pick a project. Follow its progress here.","在其他 App 分享链接、图片或视频，选择 DropRun，再选择项目。进展会出现在这里。"),15,Ui.MUTED);hint.setGravity(Gravity.CENTER);empty.addView(hint,Ui.margins(this,10,0));emptyScroll.addView(empty,new ScrollView.LayoutParams(-1,-2));stage.addView(emptyScroll,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(emptyScroll);Ui.enter(root);
    }
    void show(){
        if(root==null)build();int drafts=ShareDrafts.list(store).size();draftsNotice.setText(L.t(drafts+(drafts==1?" unfinished share · continue":" unfinished shares · continue"),drafts+" 份未完成的分享 · 继续"));draftsNotice.setVisibility(drafts==0?View.GONE:View.VISIBLE);
        catalog=store.projects();String next=String.valueOf(store.activity())+catalog+store.pending()+store.prefs.getString("syncError","")+store.prefs.getString("receiverNotice","")+store.computerOnline()+TaskNotifications.allowed(this)+checkingStatus+(System.currentTimeMillis()/60000);if(next.equals(snapshot))return;snapshot=next;
        String error=store.prefs.getString("syncError",""),receiver=store.prefs.getString("receiverNotice","");
        String line=checkingStatus?L.t("Checking status…","正在检查状态…"):!error.isEmpty()?L.t("Sync needs attention · View details","同步需要处理 · 查看详情"):!store.computerOnline()?L.t("Computer offline · saved handoffs will wait.","电脑离线 · 已保存的交办会等待连接。"):!receiver.isEmpty()?receiver:!TaskNotifications.allowed(this)?L.t("Turn on notifications for deliveries and decisions.","开启通知，及时收到交付与待确认事项。"):"";
        notice.setText(line);notice.setTextColor(checkingStatus?Ui.MUTED:Ui.AMBER);notice.setEnabled(!checkingStatus);notice.setVisibility(line.isEmpty()?View.GONE:View.VISIBLE);
        boolean notificationNotice=error.isEmpty()&&store.computerOnline()&&receiver.isEmpty()&&!TaskNotifications.allowed(this);
        notice.setOnClickListener(v->{if(!error.isEmpty())showSyncError(error);else if(notificationNotice){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},10);else startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));}else load(true);});
        View visible=list.getChildAt(0);int offset=visible==null?0:visible.getTop()-list.getPaddingTop();
        // A pending keyboard selection is not a laid-out anchor yet.
        String anchor=(list.isInTouchMode()||list.getSelectedView()==null)&&visible!=null&&visible.getTag() instanceof HomeHolder?((HomeHolder)visible.getTag()).id:null;
        items.clear();items.addAll(ProjectPresentation.merge(store.activity(),store.pending(),store.tasks()));adapter.notifyDataSetChanged();
        if(anchor!=null)for(int n=0;n<items.size();n++)if(anchor.equals(items.get(n).optString("id"))){list.setSelectionFromTop(n,offset);break;}
    }
    int dp(int value){return Ui.dp(this,value);}
    static String text(JSONObject object,String key){return object.isNull(key)?"":object.optString(key);}
    static String name(JSONObject task){String title=text(task,"title");if(title.isEmpty())title=text(task,"message");if(title.isEmpty())title=text(task,"content");if(title.isEmpty())title=L.t("Shared material","分享的材料");return TaskPresentation.clip(title,100);}
    final class HomeAdapter extends BaseAdapter {
        // AbsListView restores saved rows only when their stable ID is nonnegative.
        public int getCount(){return items.size();}public Object getItem(int p){return items.get(p);}public long getItemId(int p){return Integer.toUnsignedLong(items.get(p).optString("id").hashCode());}public boolean hasStableIds(){return true;}
        public View getView(int position,View recycled,ViewGroup parent){
            JSONObject project=items.get(position);LinearLayout outer;HomeHolder holder;
            if(recycled instanceof LinearLayout&&recycled.getTag() instanceof HomeHolder){outer=(LinearLayout)recycled;holder=(HomeHolder)outer.getTag();}
            else {
                outer=Ui.vertical(MainActivity.this);outer.setPadding(0,0,0,dp(12));LinearLayout card=Ui.vertical(MainActivity.this);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(Ui.outlined(MainActivity.this,Ui.SURFACE,0,Ui.RADIUS_CARD,0));card.setMinimumHeight(dp(48));outer.addView(card,Ui.fill());
                LinearLayout head=Ui.row(MainActivity.this);
                LinearLayout words=Ui.vertical(MainActivity.this);TextView name=Ui.title(MainActivity.this,"",20);words.addView(name);TextView state=Ui.text(MainActivity.this,"",15,Ui.MUTED);words.addView(state,Ui.margins(MainActivity.this,4,0));head.addView(words,Ui.grow());
                ImageView arrow=new ImageView(MainActivity.this);arrow.setImageResource(R.drawable.ic_chevron_left);arrow.setRotation(180);arrow.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.DIM));arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);head.addView(arrow,Ui.square(MainActivity.this,16));card.addView(head);
                boolean large=getResources().getConfiguration().fontScale>=1.5f;LinearLayout footer=large?Ui.vertical(MainActivity.this):Ui.row(MainActivity.this);TextView counts=Ui.text(MainActivity.this,"",13,Ui.MUTED);footer.addView(counts,large?Ui.fill():Ui.grow());TextView date=Ui.caption(MainActivity.this,"");date.setGravity(large?Gravity.START:Gravity.END);LinearLayout.LayoutParams dateParams=large?Ui.margins(MainActivity.this,8,0):new LinearLayout.LayoutParams(-2,-2);if(!large)dateParams.setMarginStart(dp(8));footer.addView(date,dateParams);card.addView(footer,Ui.margins(MainActivity.this,12,0));
                TextView pending=Ui.caption(MainActivity.this,"");card.addView(pending,Ui.margins(MainActivity.this,9,0));TextView unavailable=Ui.caption(MainActivity.this,L.t("Project unavailable · history is still here","项目暂不可用 · 历史记录仍在"));card.addView(unavailable,Ui.margins(MainActivity.this,8,0));
                holder=new HomeHolder(card,name,counts,state,date,pending,unavailable);outer.setTag(holder);card.setFocusable(true);card.setClickable(true);Ui.bindPress(card);
            }
            holder.id=project.optString("id");String label=ProjectPresentation.label(project.optString("id"),project.optString("name"),catalog,new JSONArray(items));holder.name.setText(label);holder.name.setMaxLines(Integer.MAX_VALUE);holder.counts.setText(ProjectPresentation.counts(project));holder.date.setText(TaskPresentation.listDate(project.optLong("last_dispatch_at"),System.currentTimeMillis()));holder.date.setContentDescription(L.t("Last handoff · ","最近交办 · ")+holder.date.getText());String state=ProjectPresentation.state(project);holder.state.setText(state);int color=project.optInt("attention_count")>0?Ui.AMBER:project.optInt("active_count")>0?Ui.ACCENT:Ui.MUTED;holder.state.setTextColor(color);
            int pending=project.optInt("pending_count");holder.pending.setText(pending+L.t(" saved on this phone · waiting to send"," 条已保存在手机 · 等待发送"));holder.pending.setVisibility(pending>0?View.VISIBLE:View.GONE);holder.unavailable.setVisibility(project.optBoolean("available",true)?View.GONE:View.VISIBLE);
            holder.card.setContentDescription(label+", "+state+", "+ProjectPresentation.counts(project)+", "+holder.date.getContentDescription()+(pending>0?", "+holder.pending.getText():"")+(holder.unavailable.getVisibility()==View.VISIBLE?", "+holder.unavailable.getText():""));holder.card.setOnClickListener(v->startActivity(new Intent(MainActivity.this,ProjectHistoryActivity.class).putExtra("projectId",project.optString("id")).putExtra("projectName",project.optString("name"))));return outer;
        }
    }
    static final class HomeHolder {
        String id;
        final LinearLayout card;final TextView name,counts,state,date,pending,unavailable;
        HomeHolder(LinearLayout card,TextView name,TextView counts,TextView state,TextView date,TextView pending,TextView unavailable){this.card=card;this.name=name;this.counts=counts;this.state=state;this.date=date;this.pending=pending;this.unavailable=unavailable;}
    }
}
