package app.droprun;

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
    Store store;LinearLayout root;TextView notice;ListView list;String snapshot="";boolean foreground,busy;
    final Runnable refresh=this::load;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new Store(this);Ui.configureWindow(this);SyncJob.schedule(this);handleIntent(getIntent());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    @Override protected void onResume(){super.onResume();foreground=true;if(store.paired()){show();TaskSyncService.startIfNeeded(this);load();}else if(root!=null)handleIntent(new Intent());}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PAIR){store=new Store(this);if(result!=RESULT_OK&&!store.paired())finish();}}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
    void handleIntent(Intent intent){
        String link=intent.getDataString();intent.setData(null);
        if(link!=null&&PairingTarget.parse(link)!=null&&store.paired()){Toast.makeText(this,L.t("Already connected. Disconnect in Settings before connecting another Relay.","已连接。请先在设置里断开，再连接另一个中转实例。"),Toast.LENGTH_LONG).show();}
        if(link!=null&&PairingTarget.parse(link)!=null&&!store.paired()){startActivityForResult(new Intent(this,PairActivity.class).putExtra("pairingLink",link),PAIR);return;}
        if(!store.paired()){startActivityForResult(new Intent(this,PairActivity.class),PAIR);return;}
        show();String id=intent.getStringExtra("taskId");intent.removeExtra("taskId");if(id!=null)startActivity(new Intent(this,TaskActivity.class).putExtra("taskId",id));
    }
    void load(){
        handler.removeCallbacks(refresh);if(!foreground||busy||!store.paired())return;busy=true;
        io.execute(()->{try{if(!TaskSyncService.running)store.sync();}catch(Exception ignored){}runOnUiThread(()->{busy=false;if(isDestroyed()||!foreground)return;show();handler.postDelayed(refresh,5000);});});
    }
    void build(){
        root=Ui.column(this);LinearLayout header=Ui.row(this);header.setPadding(dp(24),dp(14),dp(20),dp(8));
        TextView title=Ui.title(this,L.t("Recent handoffs","最近交办"),27);header.addView(title,Ui.grow());
        ImageButton gear=Ui.iconButton(this,R.drawable.ic_gear,L.t("Settings","设置"));gear.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(gear,Ui.square(this,48));root.addView(header);
        TextView intro=Ui.text(this,L.t("Good ideas, moving forward.","让好想法，接着往前走。"),14,Ui.MUTED);intro.setPadding(dp(24),0,dp(24),dp(18));root.addView(intro);
        notice=Ui.text(this,"",13,Ui.AMBER);notice.setPadding(dp(24),dp(10),dp(24),dp(10));notice.setMinHeight(dp(48));notice.setFocusable(true);Ui.bindPress(notice);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(notice);
        FrameLayout stage=new FrameLayout(this);root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        list=new ListView(this);list.setAdapter(adapter);list.setItemsCanFocus(true);list.setDivider(null);list.setSelector(android.R.color.transparent);list.setVerticalScrollBarEnabled(false);list.setClipToPadding(false);list.setPadding(dp(20),0,dp(20),dp(28));stage.addView(list,new FrameLayout.LayoutParams(-1,-1));
        ScrollView emptyScroll=new ScrollView(this);emptyScroll.setFillViewport(true);emptyScroll.setVerticalScrollBarEnabled(false);
        LinearLayout empty=Ui.vertical(this);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(32),dp(24),dp(32),dp(64));
        ImageView plane=new ImageView(this);plane.setImageResource(R.drawable.ic_plane);plane.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));plane.setPadding(dp(22),dp(22),dp(22),dp(22));plane.setBackground(Ui.circle(this,Ui.LIME_SOFT,Ui.LINE));empty.addView(plane,Ui.square(this,88));Ui.space(empty,24);
        TextView heading=Ui.title(this,L.t("Your next idea starts elsewhere.","下一个好想法，就在手边。"),24);heading.setGravity(Gravity.CENTER);empty.addView(heading);
        TextView hint=Ui.text(this,L.t("Share a link, photo or video from another app. Pick a project. Its progress will find a home here.","在其他 App 分享链接、图片或视频，选择一个项目。它的进展，会出现在这里。"),15,Ui.MUTED);hint.setGravity(Gravity.CENTER);empty.addView(hint,Ui.margins(this,10,0));emptyScroll.addView(empty,new ScrollView.LayoutParams(-1,-2));stage.addView(emptyScroll,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(emptyScroll);Ui.enter(root);
    }
    void show(){
        if(root==null)build();String next=String.valueOf(store.activity())+store.pending()+store.prefs.getString("syncError","")+store.prefs.getString("receiverNotice","")+store.computerOnline()+TaskNotifications.allowed(this)+(System.currentTimeMillis()/60000);if(next.equals(snapshot))return;snapshot=next;
        String error=store.prefs.getString("syncError",""),receiver=store.prefs.getString("receiverNotice","");
        String line=!error.isEmpty()?error:!store.computerOnline()?L.t("Computer offline · saved handoffs will wait.","电脑离线 · 已保存的交办会等待连接。"):!receiver.isEmpty()?receiver:!TaskNotifications.allowed(this)?L.t("Turn on notifications for deliveries and decisions.","开启通知，及时收到交付与待确认事项。"):"";
        notice.setText(line);notice.setVisibility(line.isEmpty()?View.GONE:View.VISIBLE);
        boolean notificationNotice=error.isEmpty()&&store.computerOnline()&&receiver.isEmpty()&&!TaskNotifications.allowed(this);
        notice.setOnClickListener(v->{if(notificationNotice){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},10);else startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));}else load();});
        items.clear();items.addAll(ProjectPresentation.merge(store.activity(),store.pending(),store.tasks()));adapter.notifyDataSetChanged();
    }
    int dp(int value){return Ui.dp(this,value);}
    static String text(JSONObject object,String key){return object.isNull(key)?"":object.optString(key);}
    static String name(JSONObject task){String title=text(task,"title");if(title.isEmpty())title=text(task,"message");if(title.isEmpty())title=text(task,"content");if(title.isEmpty())title=L.t("Shared material","分享的材料");return TaskPresentation.clip(title,100);}
    final class HomeAdapter extends BaseAdapter {
        public int getCount(){return items.size();}public Object getItem(int p){return items.get(p);}public long getItemId(int p){return items.get(p).optString("id").hashCode();}public boolean hasStableIds(){return true;}
        public View getView(int position,View recycled,ViewGroup parent){
            JSONObject project=items.get(position);LinearLayout outer;HomeHolder holder;
            if(recycled instanceof LinearLayout&&recycled.getTag() instanceof HomeHolder){outer=(LinearLayout)recycled;holder=(HomeHolder)outer.getTag();}
            else {
                outer=Ui.vertical(MainActivity.this);outer.setPadding(0,0,0,dp(12));LinearLayout card=Ui.card(MainActivity.this);outer.addView(card);
                LinearLayout head=Ui.row(MainActivity.this);TextView name=Ui.title(MainActivity.this,"",21);name.setMaxLines(2);head.addView(name,Ui.grow());head.addView(Ui.text(MainActivity.this,"›",28,Ui.MUTED));card.addView(head);
                TextView counts=Ui.text(MainActivity.this,"",14,Ui.MUTED);card.addView(counts,Ui.margins(MainActivity.this,2,12));LinearLayout footer=Ui.row(MainActivity.this);TextView state=Ui.pill(MainActivity.this,"",Ui.MUTED);footer.addView(state);TextView date=Ui.caption(MainActivity.this,"");date.setGravity(Gravity.END);footer.addView(date,Ui.grow());card.addView(footer);
                TextView pending=Ui.caption(MainActivity.this,"");card.addView(pending,Ui.margins(MainActivity.this,9,0));TextView unavailable=Ui.caption(MainActivity.this,L.t("Project unavailable · history is still here","项目暂不可用 · 历史记录仍在"));card.addView(unavailable,Ui.margins(MainActivity.this,8,0));
                holder=new HomeHolder(card,name,counts,state,date,pending,unavailable);outer.setTag(holder);card.setFocusable(true);card.setClickable(true);Ui.bindPress(card);
            }
            holder.name.setText(project.optString("name"));holder.counts.setText(ProjectPresentation.counts(project));holder.date.setText(TaskPresentation.elapsed(project.optLong("last_dispatch_at"),System.currentTimeMillis()));holder.date.setContentDescription(L.t("Last handoff · ","最近交办 · ")+holder.date.getText());String state=ProjectPresentation.state(project);holder.state.setText(state);holder.state.setTextColor(project.optInt("attention_count")>0?Ui.AMBER:project.optInt("active_count")>0?Ui.ACCENT:Ui.MUTED);
            int pending=project.optInt("pending_count");holder.pending.setText(pending+L.t(" saved on this phone · waiting to send"," 条已保存在手机 · 等待发送"));holder.pending.setVisibility(pending>0?View.VISIBLE:View.GONE);holder.unavailable.setVisibility(project.optBoolean("available",true)?View.GONE:View.VISIBLE);
            holder.card.setContentDescription(project.optString("name")+", "+ProjectPresentation.counts(project)+", "+state+(pending>0?", "+holder.pending.getText():""));holder.card.setOnClickListener(v->startActivity(new Intent(MainActivity.this,ProjectHistoryActivity.class).putExtra("projectId",project.optString("id")).putExtra("projectName",project.optString("name"))));return outer;
        }
    }
    static final class HomeHolder {
        final LinearLayout card;final TextView name,counts,state,date,pending,unavailable;
        HomeHolder(LinearLayout card,TextView name,TextView counts,TextView state,TextView date,TextView pending,TextView unavailable){this.card=card;this.name=name;this.counts=counts;this.state=state;this.date=date;this.pending=pending;this.unavailable=unavailable;}
    }
}
