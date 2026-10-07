package app.droprun;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Share overlay drawn over the app the user is sharing from: pick and authorize a project, leave a
 * note and choose the model, then watch the task fly to Codex and land back in that app.
 */
public class ShareActivity extends StyledActivity {
    static final int PAIR=40;
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final Handler handler=new Handler(Looper.getMainLooper());
    Store store,projectRead;FrameLayout root,stage;Capped holder;LinearLayout sheet,projectList,panel;ScrollView scroll;Ui.Dots dots;ImageButton back;
    EditText search,note;TextView gaugeText,sendTitle,status,badge;ImageView modelChevron;Ui.PlaneView plane;Ui.Glass dialog;ColorDrawable scrim;ObjectAnimator scrimAnimator;
    AlertDialog discardDialog;
    ShareImport incoming;Runnable importObserver;Bundle restoredState;
    TextView draftStatus;Button retryDraft;boolean discardOnFinish;
    LinearLayout receiveActions,receiveContent;
    TextView projectReadStatus,projectRefresh;boolean projectReadFailed;
    String shared="",last="",selected="",model="",effort="",query="",draft="";JSONArray attachments=new JSONArray();
    List<JSONObject> recentProjects=Collections.emptyList();
    int step=-1,permissionGeneration;boolean receiving=true,showAll=false,panelOpen=false,materialOpen=false,busy=false,closing=false,sent=false;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);Ui.configureOverlay(this);overridePendingTransition(0,0);
        try{state=recoveryState(state);}catch(Exception e){build();fatal(e);return;}
        Intent intent=importIntent();CharSequence text=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);shared=text==null?"":text.toString();
        build();
        if(state!=null&&state.getBoolean("sent")){sent=true;receiving=false;close();return;}
        if(state!=null){
            shared=state.getString("shared",shared);selected=state.getString("selected","");draft=state.getString("draft","");query=state.getString("query","");showAll=state.getBoolean("showAll");materialOpen=state.getBoolean("materialOpen",false);
            restoredState=state;
        }
        try{
            ShareImport retained=(ShareImport)getLastNonConfigurationInstance();
            incoming=ShareImport.open(store,intent,retained!=null?retained.id:state==null?null:state.getString("importId"),retained!=null?retained.store.scope:state==null?null:state.getString("importScope"),this);
            if(incoming.transferred){sent=true;receiving=false;close();return;}
            if(!incoming.store.scope.equals(store.scope))incoming.bind(store);
            importObserver=()->{
                if(!receiving||!incoming.finished||gone())return;
                if(incoming.error!=null){receiveFailed();return;}
                try{attachments=incoming.attachments();received();}catch(Exception e){fatal(e);}
            };
            incoming.observer=importObserver;
            incoming.editorObserver=this::draftFeedback;
            if(incoming.finished){importObserver.run();return;}
            if(!incoming.started){incoming.started=true;io.execute(incoming);}
        }catch(Exception e){fatal(e);return;}
        // Only a slow copy (a large video) shows the receiving notice; text shares go straight to step 1.
        handler.postDelayed(()->{if(receiving&&!gone())Ui.swap(stage,notice(L.t("Receiving shared material…","正在接收分享…")),1);},200);
    }
    void received(){
        receiving=false;if(gone()){if(discardOnFinish)discardAttachments();return;}
        clearReceiveActions();
        if(shared.trim().isEmpty()&&attachments.length()==0){fatal(new IOException(L.t("Share a link, text or file.","请分享链接、文字或文件")));return;}
        if(store.paired()){
            start();
            if(restoredState!=null&&restoredState.getInt("step",-1)>=0){model=restoredState.getString("model",model);effort=restoredState.getString("effort",effort);if(restoredState.getInt("step")==1&&Store.projectEnabled(store.project(selected)))go(1,1);}
        }else unpaired();
        restoredState=null;checkpoint();
    }
    Bundle recoveryState(Bundle state)throws Exception{return state;}
    Intent importIntent(){return getIntent();}
    void checkpoint(){
        if(incoming==null||sent||discardOnFinish||restoredState!=null||receiving)return;
        try{incoming.checkpoint(new JSONObject().put("selected",selected).put("draft",note==null?draft:note.getText().toString()).put("model",model).put("effort",effort).put("query",query).put("showAll",showAll).put("step",Math.min(1,step)));draftFeedback();}catch(Exception e){error(e);}
    }
    void draftFeedback(){if(draftStatus==null||incoming==null||gone())return;boolean failed=incoming.editorError!=null;draftStatus.setText(failed?L.t("Draft not saved. Keep this page open and retry.","草稿未保存，请保持页面打开并重试。"):incoming.editorSaving?L.t("Saving draft…","正在保存草稿…"):L.t("Draft saved on this phone","草稿已保存在手机"));draftStatus.setTextColor(failed?Ui.AMBER:Ui.MUTED);retryDraft.setVisibility(failed?View.VISIBLE:View.GONE);}
    @Override protected void onPause(){checkpoint();super.onPause();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PAIR){store=new Store(this);if(result==RESULT_OK&&store.paired())start();else close();}
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);layoutReceiveActions();}
    @Override public void onBackPressed(){
        if(dialog!=null){if(!busy)closeDialog(null);return;}
        if(step==1)go(0,-1);else close();
    }
    @Override protected void onDestroy(){
        if(scrimAnimator!=null){scrimAnimator.cancel();scrimAnimator=null;}
        handler.removeCallbacksAndMessages(null);io.shutdown();
        if(incoming!=null&&incoming.observer==importObserver){incoming.observer=null;incoming.editorObserver=null;incoming.release(this);}
        if(discardDialog!=null)discardDialog.dismiss();
        if(!sent&&discardOnFinish)discardAttachments();
        super.onDestroy();
    }
    void discardAttachments(){if(incoming!=null)incoming.cancel();}
    @Override public Object onRetainNonConfigurationInstance(){return incoming;}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putBoolean("sent",sent);
        state.putBoolean("materialOpen",materialOpen);
        state.putString("query",query);state.putBoolean("showAll",showAll);
        if(incoming!=null){state.putString("importId",incoming.id);state.putString("importScope",incoming.store.scope);}
        state.putString("shared",shared);state.putString("selected",selected);state.putString("draft",note==null?draft:note.getText().toString());state.putString("model",model);state.putString("effort",effort);state.putInt("step",step);
    }
    int dp(int value){return Ui.dp(this,value);}
    boolean gone(){return closing||isFinishing()||isDestroyed();}

    // ---- overlay chrome -------------------------------------------------------------------------
    void build(){
        root=Ui.frame(this,false);scrim=new ColorDrawable(Ui.SCRIM);scrim.setAlpha(0);root.setBackground(scrim);root.setOnClickListener(v->close());root.setContentDescription(L.t("Cancel","取消"));
        sheet=Ui.sheet(this);holder=new Capped(this);holder.addView(sheet,new FrameLayout.LayoutParams(-1,-2));
        root.addView(holder,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));insets(root);
        LinearLayout header=Ui.row(this);
        back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Previous step","返回上一步"));back.setVisibility(View.INVISIBLE);back.setOnClickListener(v->{if(step==1)go(0,-1);});header.addView(back,Ui.square(this,48));
        FrameLayout middle=new FrameLayout(this);dots=new Ui.Dots(this,3);middle.addView(dots,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));header.addView(middle,Ui.grow());
        ImageButton closeButton=Ui.iconButton(this,R.drawable.ic_close,L.t("Close","关闭"));closeButton.setOnClickListener(v->close());header.addView(closeButton,Ui.square(this,48));
        sheet.addView(header,Ui.fill());
        scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setPadding(0,dp(6),0,0);scroll.setClipToPadding(false);
        stage=new FrameLayout(this);scroll.addView(stage,new ViewGroup.LayoutParams(-1,-2));sheet.addView(scroll,Ui.fill());
        dim(true);Ui.slideUp(holder);
    }
    /** Status bar and keyboard pad the root; the navigation bar is absorbed into the sheet's bottom padding so the sheet sits flush with the screen edge. */
    void insets(View view){
        view.setOnApplyWindowInsetsListener((target,windowInsets)->{
            int left,top,right,bottom,keyboard=0;
            if(Build.VERSION.SDK_INT>=30){Insets bars=windowInsets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;keyboard=windowInsets.getInsets(WindowInsets.Type.ime()).bottom;}
            else{left=windowInsets.getSystemWindowInsetLeft();top=windowInsets.getSystemWindowInsetTop();right=windowInsets.getSystemWindowInsetRight();bottom=windowInsets.getSystemWindowInsetBottom();}
            boolean typing=keyboard>bottom;
            target.setPadding(left,top,right,typing?keyboard:0);sheet.setPadding(dp(20),dp(12),dp(20),dp(20)+(typing?0:bottom));
            return windowInsets;
        });
        view.requestApplyInsets();
    }
    /** Keep breathing room on tall screens; use the available height on compact screens. */
    static final class Capped extends FrameLayout {
        Capped(Context context){super(context);}
        @Override protected void onMeasure(int w,int h){
            if(MeasureSpec.getMode(h)==MeasureSpec.UNSPECIFIED){super.onMeasure(w,h);return;}
            int height=MeasureSpec.getSize(h);height=Math.min(height,Math.max(Ui.dp(getContext(),720),Math.round(height*0.86f)));
            super.onMeasure(w,MeasureSpec.makeMeasureSpec(height,MeasureSpec.AT_MOST));
        }
    }
    void dim(boolean in){
        if(scrimAnimator!=null){scrimAnimator.cancel();scrimAnimator=null;}
        int target=in?Ui.SCRIM:Ui.SCRIM&0x00FFFFFF;
        if(!Ui.motionEnabled(this)){scrim.setColor(target);return;}
        scrimAnimator=ObjectAnimator.ofArgb(scrim,"color",scrim.getColor(),target);scrimAnimator.setDuration(in?240:200);scrimAnimator.start();
    }
    void close(){
        if(closing)return;
        String message=note==null?draft:note.getText().toString();
        boolean failed=this instanceof RecoveredShareActivity||receiveActions!=null||(incoming!=null&&incoming.error!=null);
        if(!sent&&(!message.trim().isEmpty()||failed)){
            if(discardDialog!=null&&discardDialog.isShowing())return;
            discardDialog=new AlertDialog.Builder(this).setTitle(L.t("Discard this share?","放弃这次分享？")).setMessage(L.t("Nothing has been handed off. This removes this share's saved copies and note. Your original files stay in the source app.","这次分享尚未交办。将移除本次保存的副本和留言，来源 App 中的原文件不受影响。")).setNegativeButton(failed?L.t("Keep share","保留分享"):L.t("Keep editing","继续编辑"),null).setPositiveButton(L.t("Discard","放弃"),(d,w)->finishShare()).show();
            return;
        }
        finishShare();
    }
    void finishShare(){
        if(closing)return;discardOnFinish=!sent;closing=true;hideKeyboard();closeDialog(null);
        dim(false);Ui.slideDown(holder,()->{finish();overridePendingTransition(0,0);});
    }
    void hideKeyboard(){View focus=getCurrentFocus();if(focus==null)return;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}
    TextView notice(String value){TextView view=Ui.text(this,value,13,Ui.MUTED);view.setGravity(Gravity.CENTER);view.setPadding(dp(10),dp(14),dp(10),dp(14));return view;}
    void error(Exception e){if(gone())return;new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).show();}
    /** Nothing to show after this error: the overlay closes once the message is dismissed. */
    void fatal(Exception e){receiving=false;if(gone())return;new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).setOnDismissListener(d->{if(incoming==null)finishShare();else close();}).show();}

    // ---- receiving the share --------------------------------------------------------------------
    void clearReceiveActions(){if(receiveActions!=null){sheet.removeView(receiveActions);receiveActions=null;receiveContent=null;scroll.setLayoutParams(Ui.fill());scroll.setVerticalScrollBarEnabled(false);}}
    void receiveFailed(){
        receiving=false;step=-1;dots.setVisibility(View.INVISIBLE);back.setVisibility(View.INVISIBLE);
        LinearLayout column=Ui.vertical(this);column.addView(Ui.title(this,L.t("Let's finish receiving","材料尚未接收完成"),20));
        TextView summary=Ui.text(this,L.t(incoming.completeCount()+" of "+incoming.sources.length()+" files kept on your phone. Nothing has been handed off.","手机已保留 "+incoming.completeCount()+" / "+incoming.sources.length()+" 份文件，尚未交办。"),14,Ui.MUTED);summary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);column.addView(summary,Ui.margins(this,4,14));
        TextView problem=Ui.text(this,incoming.error.getMessage(),14,Ui.TEXT);problem.setTag("receive-problem");column.addView(problem,Ui.margins(this,0,14));
        JSONArray files=incoming.record.optJSONArray("files");LinearLayout materials=Ui.card(this);
        for(int n=0;n<incoming.sources.length();n++){
            JSONObject item=files==null?null:files.optJSONObject(n);boolean ready=item!=null&&item.optBoolean("complete");
            String name=item==null?L.t("File "+(n+1),"文件 "+(n+1)):item.optString("name",L.t("File "+(n+1),"文件 "+(n+1)));
            materials.addView(Ui.text(this,name,14,Ui.TEXT),Ui.margins(this,n==0?0:12,2));
            materials.addView(Ui.text(this,ready?L.t("Ready on this phone","已保存在手机"):n==incoming.failureIndex?L.t("Not received","未接收完成"):L.t("Waiting","等待接收"),12,ready?Ui.MUTED:Ui.AMBER),Ui.fill());
        }
        column.addView(materials,Ui.fill());
        clearReceiveActions();receiveContent=column;receiveActions=Ui.vertical(this);receiveActions.setPadding(0,dp(12),0,0);
        Button retry=Ui.button(this,L.t("Retry receiving","重试接收"),true);retry.setTag("retry-import");retry.setOnClickListener(v->retryIncoming());receiveActions.addView(retry,Ui.fill());
        Button discard=Ui.button(this,L.t("Discard share","放弃分享"),false);discard.setTag("discard-import");Ui.styleGhost(discard);discard.setOnClickListener(v->close());receiveActions.addView(discard,Ui.margins(this,6,0));
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));scroll.setVerticalScrollBarEnabled(true);sheet.addView(receiveActions,Ui.fill());layoutReceiveActions();
        Ui.swap(stage,column,1);scroll.scrollTo(0,0);
    }
    void layoutReceiveActions(){
        if(receiveActions==null)return;
        boolean wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        receiveContent.getChildAt(0).setVisibility(wide?View.GONE:View.VISIBLE);
        View problem=receiveContent.findViewWithTag("receive-problem");receiveContent.removeView(problem);receiveContent.addView(problem,wide?1:2);
        receiveActions.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        for(int n=0;n<receiveActions.getChildCount();n++){
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0);
            if(n>0){if(wide)params.setMarginStart(dp(8));else params.topMargin=dp(6);}
            receiveActions.getChildAt(n).setLayoutParams(params);
        }
    }
    void retryIncoming(){
        if(receiving||gone())return;
        try{if(!incoming.retry())return;receiving=true;Button retry=receiveActions.findViewWithTag("retry-import");retry.setEnabled(false);retry.setText(L.t("Receiving…","正在接收…"));Ui.swap(stage,notice(L.t("Receiving the remaining material…","正在接收剩余材料…")),1);io.execute(incoming);}
        catch(Exception e){error(e);}
    }
    void unpaired(){
        dots.setVisibility(View.INVISIBLE);LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("Connect your computer first","先连接电脑"),20));
        column.addView(Ui.text(this,L.t("Send ideas to Codex on your own computer. Connect once, then share from your favorite apps.","分享会发送到你电脑上的 Codex。连接一次，以后从任何 App 分享都能直接转发。"),14,Ui.MUTED),Ui.margins(this,2,14));
        Button pair=Ui.button(this,L.t("Scan to connect","扫码连接"),true);pair.setOnClickListener(v->startActivityForResult(new Intent(this,PairActivity.class),PAIR));column.addView(pair,Ui.fill());
        Ui.swap(stage,column,1);
    }
    void start(){
        try{incoming.bind(store);attachments=incoming.attachments();}catch(Exception e){fatal(e);return;}
        dots.setVisibility(View.VISIBLE);last=store.prefs.getString("lastProject","");model=store.defaultModel();effort=store.defaultEffort(model);
        go(0,1);
        refreshProjects(true);
    }
    void refreshProjects(boolean initializeModel){
        final Store target=store;if(gone()||projectRead==target)return;
        String before=target.projects().toString()+target.activity();projectRead=target;projectReadFailed=false;updateProjectRead();
        io.execute(()->{
            boolean failed=false;
            try{JSONObject data=target.get("/projects");data.getJSONArray("projects");target.prefs.edit().putString("projects",data.toString()).apply();}catch(Exception ignored){failed=true;}
            try{JSONObject data=target.get("/projects/activity");target.prefs.edit().putString("activity",data.toString()).apply();}catch(Exception ignored){}
            boolean readFailed=failed;
            runOnUiThread(()->{
                if(gone()||store!=target||projectRead!=target)return;
                projectRead=null;projectReadFailed=readFailed;
                if(initializeModel&&model.isEmpty()){model=target.defaultModel();effort=target.defaultEffort(model);updateGauge();}
                if(step==0){
                    if(!(target.projects().toString()+target.activity()).equals(before)){recentProjects=ProjectPresentation.merge(target.activity(),target.pending(),target.tasks());renderProjects();}
                    else updateProjectRead();
                }
            });
        });
    }
    void updateProjectRead(){
        if(projectReadStatus==null||projectRefresh==null)return;
        boolean loading=projectRead==store,empty=store.projects().length()==0;
        String message=loading?L.t("Loading projects…","正在读取项目…"):projectReadFailed?(empty?L.t("Couldn't load projects. Check your Relay connection, then retry.","无法读取项目。请检查中转连接后重试。"):L.t("Couldn't refresh projects. Showing saved projects.","项目刷新失败，仍显示上次同步的项目。")):empty?L.t("No projects synced yet. Check Connector, add or connect a project, then refresh.","尚未同步项目。请检查 Connector，添加或连接一个项目后刷新。"):"";
        projectReadStatus.setText(message);projectReadStatus.setTextColor(projectReadFailed?Ui.AMBER:Ui.MUTED);projectReadStatus.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);
        projectRefresh.setEnabled(!loading);projectRefresh.setAlpha(loading?0.5f:1f);
        projectRefresh.setText(loading?L.t("Loading…","正在读取…"):projectReadFailed?L.t("Retry","重试"):L.t("Refresh","刷新"));
        projectRefresh.setContentDescription(loading?L.t("Loading projects","正在读取项目"):projectReadFailed?L.t("Retry loading projects","重试读取项目"):L.t("Refresh projects","刷新项目"));
    }
    void go(int target,int direction){
        if(gone())return;
        if(step==1&&note!=null)draft=note.getText().toString();
        hideKeyboard();step=target;
        Ui.swap(stage,target==0?stepProject():target==1?stepNote():stepSend(),direction);
        dots.setActive(target,true);back.setVisibility(target==1?View.VISIBLE:View.INVISIBLE);scroll.scrollTo(0,0);
        checkpoint();
        if(target==2)fly();
    }
    String projectName(){JSONObject project=store.project(selected);return project==null?"":store.projectLabel(project);}

    // ---- the shared material --------------------------------------------------------------------
    View materialDisclosure(){
        LinearLayout content=Ui.vertical(this);content.setPadding(dp(14),0,dp(14),dp(12));
        if(materialOpen)materialContent(content);
        String label=materialLabel();LinearLayout group=Ui.disclosure(this,L.t("Received material: ","收到的材料：")+label,content,materialOpen,open->{materialOpen=open;if(open&&content.getChildCount()==0)materialContent(content);});
        group.setTag("share-material-review");
        LinearLayout header=(LinearLayout)group.getChildAt(0);header.setPadding(0,0,0,0);TextView summary=(TextView)header.getChildAt(0);summary.setText(materialSummary());summary.setTextSize(13);Ui.oneLine(summary);
        return group;
    }
    void materialContent(LinearLayout content){
        TextView scope=Ui.caption(this,L.t("Text and filenames received from the source app. Reading coverage is reported with the result.","这里显示来源 App 传来的文字和文件名；实际读取范围以交付报告为准。"));scope.setTag("share-material-scope");content.addView(scope);
        if(!shared.isEmpty()){TextView text=Ui.text(this,shared,14,Ui.TEXT);text.setTextIsSelectable(true);text.setTag("share-material-text");content.addView(text,Ui.margins(this,10,0));}
        if(attachments.length()>0){
            content.addView(Ui.label(this,L.t("Files (","文件（")+attachments.length()+L.t(")","）")));
            LinearLayout files=Ui.vertical(this);files.setTag("share-material-files");
            for(int n=0;n<attachments.length();n++){TextView name=Ui.text(this,attachments.optJSONObject(n).optString("name"),14,Ui.TEXT);name.setTextIsSelectable(true);name.setTag("share-material-file:"+n);files.addView(name,Ui.margins(this,n==0?0:10,0));}
            content.addView(files);
        }
    }
    TextView materialChip(){
        String label=materialLabel();TextView chip=new TextView(this);chip.setText(label);chip.setTextSize(13);chip.setTextColor(Ui.TEXT);chip.setTypeface(Ui.medium());chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setIncludeFontPadding(false);chip.setPadding(dp(14),dp(10),dp(14),dp(10));chip.setMinHeight(dp(40));chip.setBackground(Ui.outlined(this,Ui.SURFACE_2,Ui.LINE,14,1));Ui.oneLine(chip);
        chip.setContentDescription(L.t("Shared material: ","分享的材料：")+label);return chip;
    }
    String materialSummary(){
        int files=attachments.length();boolean hasText=!shared.trim().isEmpty();
        if(files==0||(files==1&&!hasText))return materialLabel();
        return files+(files==1?L.t(" file"," 个文件"):L.t(" files"," 个文件"))+(hasText?L.t(" · text"," · 文字"):"");
    }
    String materialLabel(){
        ArrayList<String> parts=new ArrayList<>();
        for(int n=0;n<attachments.length();n++)parts.add(attachments.optJSONObject(n).optString("name"));
        String text=shared.trim();
        if(!text.isEmpty()){String host=host(text);parts.add(host!=null?host+L.t(" · link"," · 链接"):L.t("Text · ","文字 · ")+TaskPresentation.clip(text,40));}
        return String.join("、",parts);
    }
    static String host(String text){
        Matcher m=Pattern.compile("https?://\\S+").matcher(text);if(!m.find())return null;
        String host=Uri.parse(m.group()).getHost();if(host==null||host.isEmpty())return null;
        return host.startsWith("www.")?host.substring(4):host;
    }

    // ---- step 1: project and authorization ------------------------------------------------------
    View stepProject(){
        recentProjects=ProjectPresentation.merge(store.activity(),store.pending(),store.tasks());
        LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("Where should this idea go?","转发给哪个项目？"),22));
        LinearLayout.LayoutParams chipParams=new LinearLayout.LayoutParams(-1,-2);chipParams.setMargins(0,dp(12),0,dp(18));column.addView(materialDisclosure(),chipParams);
        LinearLayout box=Ui.card(this);box.setPadding(dp(6),dp(6),dp(6),dp(6));
        box.setElevation(0);box.setBackground(Ui.outlined(this,Ui.SURFACE_2,Ui.LINE,Ui.RADIUS_CARD,1));
        search=new EditText(this);search.setHint(L.t("Find a project","搜索项目"));search.setSingleLine(true);search.setImeOptions(EditorInfo.IME_ACTION_DONE);search.setText(query);Ui.styleInput(search);
        box.addView(search,Ui.margins(this,2,6));
        boolean largeType=getResources().getConfiguration().fontScale>=1.5f;
        LinearLayout readFeedback=largeType?Ui.vertical(this):Ui.row(this);readFeedback.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        projectReadStatus=Ui.caption(this,"");projectReadStatus.setPadding(dp(12),dp(8),dp(8),dp(8));projectReadStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);readFeedback.addView(projectReadStatus,largeType?Ui.fill():Ui.grow());
        projectRefresh=Ui.linkButton(this,L.t("Refresh","刷新"));projectRefresh.setOnClickListener(v->refreshProjects(false));LinearLayout.LayoutParams refreshParams=new LinearLayout.LayoutParams(-2,-2);refreshParams.setMargins(dp(6),dp(6),dp(6),dp(6));readFeedback.addView(projectRefresh,refreshParams);
        box.addView(readFeedback,Ui.fill());
        projectList=Ui.vertical(this);box.addView(projectList,Ui.fill());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();renderProjects();}public void afterTextChanged(Editable e){}});
        column.addView(box,Ui.margins(this,0,4));
        renderProjects();
        return column;
    }
    void renderProjects(){
        if(projectList==null)return;projectList.removeAllViews();
        JSONArray projects=store.projects(),history=store.activity();
        search.setVisibility(projects.length()>6||!query.isEmpty()?View.VISIBLE:View.GONE);
        updateProjectRead();if(projects.length()==0)return;
        // Preserve the current choice; otherwise put retained recent activity before unused projects.
        String filter=query.trim().toLowerCase(Locale.ROOT);
        List<JSONObject> ordered=ProjectPresentation.sharing(projects,recentProjects,selected,last);
        int shown=0,hidden=0;
        for(JSONObject p:ordered){
            if(!filter.isEmpty()&&!ProjectPresentation.label(p.optString("id"),p.optString("name"),projects,history).toLowerCase(Locale.ROOT).contains(filter)&&!p.optString("id").toLowerCase(Locale.ROOT).contains(filter))continue;
            if(filter.isEmpty()&&!showAll&&shown>=6){hidden++;continue;}
            if(shown>0){LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,Math.max(1,dp(1)));line.setMargins(dp(12),0,dp(12),0);projectList.addView(Ui.divider(this),line);}
            projectList.addView(projectRow(p,projects,history),Ui.fill());shown++;
        }
        if(shown==0)projectList.addView(notice(L.t("No matching projects","没有匹配的项目")));
        if(hidden>0){TextView more=Ui.linkButton(this,L.t("Show all ","显示全部 ")+projects.length()+L.t(" projects"," 个项目"));more.setOnClickListener(v->{showAll=true;renderProjects();});LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMargins(dp(6),dp(6),0,dp(2));projectList.addView(more,params);}
    }
    View projectRow(JSONObject project,JSONArray catalog,JSONArray history){
        String id=project.optString("id"),name=ProjectPresentation.label(id,project.optString("name"),catalog,history);boolean available=project.optBoolean("available",true),enabled=Store.projectEnabled(project),chosen=id.equals(selected);
        LinearLayout row=Ui.row(this);row.setPadding(dp(12),dp(13),dp(12),dp(13));row.setMinimumHeight(dp(64));
        if(chosen)row.setBackground(Ui.outlined(this,Ui.LIME_SOFT,Ui.LIME_LINE,18,1));
        row.addView(Ui.projectTile(this,name),Ui.square(this,40));Ui.space(row,12);
        LinearLayout words=Ui.vertical(this);TextView label=Ui.title(this,name,17);label.setTextColor(chosen?Ui.ACCENT:Ui.TEXT);label.setPadding(0,0,0,0);words.addView(label);
        words.addView(Ui.caption(this,!available?L.t("Unavailable","暂不可用"):enabled?L.t("Allowed","已授权"):L.t("Allow access","需授权")),Ui.margins(this,4,0));row.addView(words,Ui.grow());
        Ui.space(row,8);ImageView arrow=new ImageView(this);arrow.setImageResource(chosen?R.drawable.ic_check:R.drawable.ic_chevron_left);arrow.setRotation(chosen?0:180);arrow.setImageTintList(ColorStateList.valueOf(chosen?Ui.ACCENT:Ui.DIM));arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(arrow,Ui.square(this,18));
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(name+(!available?L.t(", unavailable","，暂不可用"):enabled?L.t(", allowed","，已授权"):L.t(", permission required","，需授权"))+(chosen?L.t(", selected","，已选择"):""));Ui.bindPress(row);
        row.setOnClickListener(v->pick(project));
        return row;
    }
    void pick(JSONObject project){
        if(busy||step!=0)return;
        if(!project.optBoolean("available",true)){unavailableProject();return;}
        if(!Store.projectEnabled(project)){authorize(project);return;}
        selected=project.optString("id");go(1,1);
    }
    void unavailableProject(){new AlertDialog.Builder(this).setTitle(L.t("Project unavailable","项目暂不可用")).setMessage(L.t("Choose another project, or reconnect this one in DropRun setup on your computer.","请选择其他项目，或在电脑的 DropRun 配置页重新连接这个项目。" )).setPositiveButton(L.t("Got it","知道了"),null).show();}
    void authorize(JSONObject project){
        String id=project.optString("id"),name=store.projectLabel(project);hideKeyboard();
        permissionGeneration++;
        Ui.Glass glass=Ui.glass(this,root,sheet);dialog=glass;glass.overlay.setOnClickListener(v->{if(!busy)closeDialog(null);});
        LinearLayout card=glass.card;
        card.addView(Ui.title(this,L.t("“","「")+name+L.t("” needs permission","」需要授权"),18));
        card.addView(Ui.text(this,L.t("Allow handoffs from this phone to read and edit the original project and run commands, subject to your execution setting. You can revoke this in Settings.","授权后，Codex 可按你的执行设置，在原项目中读写文件并运行命令。可在设置中撤销授权。"),14,Ui.MUTED),Ui.margins(this,4,12));
        TextView problem=Ui.text(this,"",13,Ui.AMBER);problem.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);problem.setVisibility(View.GONE);card.addView(problem,Ui.margins(this,0,8));
        Button ok=Ui.button(this,L.t("Allow & continue","授权并继续"),true);card.addView(ok,Ui.fill());
        Button cancel=Ui.button(this,L.t("Cancel","取消"),false);Ui.styleGhost(cancel);card.addView(cancel,Ui.margins(this,6,0));
        cancel.setOnClickListener(v->{if(!busy)closeDialog(null);});
        ok.setOnClickListener(v->{
            if(busy)return;busy=true;ok.setEnabled(false);ok.setText(L.t("Allowing…","正在授权…"));cancel.setEnabled(false);problem.setVisibility(View.GONE);
            io.execute(()->{
                try{store.setProjectPermission(id,true);runOnUiThread(()->{busy=false;if(gone()||dialog!=glass)return;selected=id;renderProjects();authorized(glass);});}
                catch(Exception e){runOnUiThread(()->{busy=false;if(gone()||dialog!=glass)return;ok.setEnabled(true);ok.setText(L.t("Allow & continue","授权并继续"));cancel.setEnabled(true);problem.setText(e.getMessage());problem.setVisibility(View.VISIBLE);problem.post(()->problem.requestRectangleOnScreen(new android.graphics.Rect(0,-dp(8),problem.getWidth(),problem.getHeight()+dp(18)),true));});}
            });
        });
    }
    void authorized(Ui.Glass glass){
        if(gone()||dialog!=glass||step!=0)return;
        String project=selected;int generation=permissionGeneration;
        LinearLayout card=glass.card;card.removeAllViews();
        Ui.CheckView check=new Ui.CheckView(this);check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.gravity=Gravity.CENTER_HORIZONTAL;params.topMargin=dp(8);card.addView(check,params);
        TextView done=Ui.title(this,L.t("Allowed","已授权"),16);done.setGravity(Gravity.CENTER);card.addView(done,Ui.margins(this,12,4));
        check.play(()->{
            if(gone()||dialog!=glass||step!=0||generation!=permissionGeneration||!selected.equals(project))return;
            handler.postDelayed(()->{
                if(gone()||dialog!=glass||step!=0||generation!=permissionGeneration||!selected.equals(project))return;
                closeDialog(()->{if(!gone()&&dialog==null&&step==0&&generation==permissionGeneration&&selected.equals(project))go(1,1);});
            },320);
        });
    }
    void closeDialog(Runnable end){Ui.Glass open=dialog;dialog=null;if(open!=null)open.dismiss(end);else if(end!=null)end.run();}

    // ---- step 2: note, model and effort ---------------------------------------------------------
    View stepNote(){
        LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("What should Codex do?","想让 Codex 做什么？"),22));
        LinearLayout receipt=Ui.vertical(this);receipt.setTag("share-destination-material");receipt.setPadding(dp(16),dp(12),dp(16),dp(8));receipt.setBackground(Ui.outlined(this,Ui.SURFACE_2,0,Ui.RADIUS_CARD,0));
        LinearLayout destination=Ui.row(this);destination.setGravity(Gravity.TOP);destination.setTag("share-destination-identity");
        if(getResources().getConfiguration().fontScale<1.5f){destination.addView(Ui.projectTile(this,projectName()),Ui.square(this,32));Ui.space(destination,12);}
        TextView target=Ui.text(this,L.t("For “","转发到「")+projectName()+L.t("”","」"),17,Ui.TEXT);target.setTypeface(Ui.medium());target.setPadding(0,0,0,0);target.setTag("share-destination-name");destination.addView(target,Ui.grow());receipt.addView(destination,Ui.fill());
        receipt.addView(materialDisclosure(),Ui.margins(this,2,0));column.addView(receipt,Ui.margins(this,8,4));
        TextView noteLabel=Ui.text(this,L.t("Your note · optional","留言 · 可选"),13,Ui.TEXT);noteLabel.setTypeface(Ui.medium());noteLabel.setPadding(0,0,0,0);noteLabel.setTag("share-note-label");column.addView(noteLabel,Ui.margins(this,4,0));
        note=new EditText(this);note.setId(View.generateViewId());noteLabel.setLabelFor(note.getId());note.setHint(L.t("Leave blank for Codex to decide.","留空，让 Codex 判断。"));note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        note.setMaxLines(6);note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15000)});note.setText(draft);Ui.styleInput(note);note.setMinLines(2);
        note.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){draft=s.toString();checkpoint();}public void afterTextChanged(Editable value){}});
        column.addView(note,Ui.margins(this,8,6));
        gaugeText=null;modelChevron=null;panel=null;panelOpen=false;
        if(store.models().length()>0){
            LinearLayout gaugeRow=Ui.row(this);gaugeRow.setMinimumHeight(dp(48));gaugeRow.setClickable(true);gaugeRow.setFocusable(true);gaugeRow.setOnClickListener(v->togglePanel());Ui.bindPress(gaugeRow);
            gaugeText=Ui.caption(this,"");gaugeRow.addView(gaugeText,Ui.grow());
            Ui.space(gaugeRow,8);modelChevron=new ImageView(this);modelChevron.setImageResource(R.drawable.ic_chevron_left);modelChevron.setImageTintList(ColorStateList.valueOf(Ui.MUTED));modelChevron.setRotation(180);modelChevron.setTag("share-model-chevron");modelChevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);gaugeRow.addView(modelChevron,Ui.square(this,18));
            column.addView(gaugeRow,Ui.fill());
            panel=Ui.vertical(this);panel.setVisibility(View.GONE);column.addView(panel,Ui.margins(this,4,0));
            updateGauge();
        }
        column.addView(executionSettingNotice(),Ui.margins(this,executionSettingConfirmed()?8:12,0));
        Button send=Ui.button(this,L.t("Hand off to Codex","交给 Codex"),true);send.setTag("share-send");send.setOnClickListener(v->{if(submit())go(2,1);});column.addView(send,Ui.margins(this,12,0));
        draftStatus=Ui.caption(this,"");draftStatus.setGravity(Gravity.CENTER);draftStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);column.addView(draftStatus,Ui.margins(this,4,0));retryDraft=Ui.button(this,L.t("Retry saving draft","重试保存草稿"),false);retryDraft.setOnClickListener(v->checkpoint());column.addView(retryDraft,Ui.margins(this,6,0));draftFeedback();
        return column;
    }
    boolean executionSettingConfirmed(){return store.prefs.getBoolean("settingsKnown",false)&&store.prefs.getString("settingsError","").isEmpty();}
    View executionSettingNotice(){
        boolean confirmed=executionSettingConfirmed(),direct=store.directExecution();
        LinearLayout notice=Ui.vertical(this);notice.setTag("share-execution-setting");notice.setPadding(dp(confirmed?0:14),dp(confirmed?4:10),dp(confirmed?0:14),dp(confirmed?4:10));if(!confirmed)notice.setBackground(Ui.outlined(this,Ui.SURFACE_2,Ui.LINE,14,1));
        String title=confirmed?(direct?L.t("Saved setting · Direct execution","已保存设置 · 直接执行"):L.t("Saved setting · Plan review","已保存设置 · 先看计划")):L.t("Execution setting not confirmed","执行设置尚未确认");
        TextView heading=Ui.text(this,title,13,confirmed?Ui.TEXT:Ui.AMBER);heading.setTypeface(Ui.medium());heading.setTag("share-execution-title");notice.addView(heading,Ui.fill());
        String detail=confirmed?(direct?L.t("Can edit project files and run commands.","可修改项目文件并运行命令。"):L.t("Approve a plan before edits begin.","批准计划后才开始修改。")):L.t("Check DropRun Settings before sending.","发送前，请在 DropRun 设置中查看。");
        detail+=" "+L.t("Your Relay's setting applies when it first accepts this handoff.","Relay 首次接收交办时采用当时的设置。");
        TextView explanation=Ui.caption(this,detail);explanation.setTag("share-execution-detail");notice.addView(explanation,Ui.margins(this,2,0));return notice;
    }
    void updateGauge(){
        checkpoint();
        if(gaugeText==null)return;JSONObject chosen=store.model(model);String meaning=effortHint(effort),summary=(chosen==null?model:chosen.optString("displayName",model))+(effort.isEmpty()?"":" · "+(meaning.isEmpty()?effort:meaning));
        String title=L.t("Model & effort","模型强度");SpannableString label=new SpannableString(title+"\n"+summary);
        label.setSpan(new RelativeSizeSpan(1.25f),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new TypefaceSpan("sans-serif-medium"),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new ForegroundColorSpan(Ui.TEXT),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        gaugeText.setText(label);gaugeText.setTextColor(panelOpen?Ui.TEXT:Ui.MUTED);
        ((View)gaugeText.getParent()).setContentDescription(L.t("Model & effort, ","模型强度，")+summary+(panelOpen?L.t(", tap to collapse","，点按收起"):L.t(", tap to expand","，点按展开")));
    }
    void togglePanel(){if(panel==null)return;panelOpen=!panelOpen;if(panelOpen)renderPanel();reveal(panelOpen);updateGauge();}
    /** Expands or collapses the model panel with height and alpha together, so the button beneath slides instead of jumping. */
    void reveal(boolean show){Ui.expand(panel,show);modelChevron.animate().cancel();float angle=show?270:180;if(Ui.motionEnabled(this))modelChevron.animate().rotation(angle).setDuration(180).setInterpolator(new DecelerateInterpolator(1.8f)).start();else modelChevron.setRotation(angle);}
    void renderPanel(){
        View previous=panel.findFocus();Object focusTag=previous!=null&&!previous.isInTouchMode()?previous.getTag():null;
        panel.removeAllViews();JSONArray catalog=store.models();
        TextView modelLabel=Ui.label(this,L.t("Model","模型"));modelLabel.setPadding(0,dp(6),0,dp(6));panel.addView(modelLabel);
        for(int n=0;n<catalog.length();n++){
            JSONObject m=catalog.optJSONObject(n);if(m==null)continue;String id=m.optString("id");
            LinearLayout row=Ui.optionRow(this,m.optString("displayName",id),m.optBoolean("isDefault")?L.t("Default on your computer","电脑上的默认模型"):null,id.equals(model));
            row.setMinimumHeight(dp(48));
            row.setTag("model:"+id);
            row.setOnClickListener(v->{model=id;effort=store.defaultEffort(id);renderPanel();updateGauge();});panel.addView(row,Ui.margins(this,0,6));
        }
        JSONObject chosen=store.model(model);JSONArray efforts=chosen==null?null:chosen.optJSONArray("efforts");
        if(efforts==null||efforts.length()==0){restorePanelFocus(focusTag);return;}
        panel.addView(Ui.label(this,L.t("Reasoning effort","推理强度")));
        for(int n=0;n<efforts.length();n++){
            String id=efforts.optString(n),meaning=effortHint(id);LinearLayout row=Ui.optionRow(this,meaning.isEmpty()?id:meaning,null,id.equals(effort));row.setMinimumHeight(dp(48));row.setTag("effort:"+id);
            row.setOnClickListener(v->{effort=id;renderPanel();updateGauge();});panel.addView(row,Ui.margins(this,0,6));
        }
        restorePanelFocus(focusTag);
    }
    void restorePanelFocus(Object tag){
        if(tag==null)return;View replacement=panel.findViewWithTag(tag);
        if(replacement!=null&&replacement.requestFocus())replacement.post(()->{
            if(replacement.hasFocus())replacement.requestRectangleOnScreen(new android.graphics.Rect(0,0,replacement.getWidth(),replacement.getHeight()),true);
        });
    }
    static String effortHint(String id){
        return switch(id){case "minimal"->L.t("Fastest","最快");case "low"->L.t("Fast","快");case "medium"->L.t("Balanced","均衡");case "high"->L.t("Thorough","深入");case "xhigh"->L.t("Most thorough","最深");default->"";};
    }
    /** Writes the task to the outbox and starts delivery; the step 3 animation only plays once this has succeeded. */
    boolean submit(){
        if(step!=1||sent)return false;
        String material=shared.trim();JSONObject project=store.project(selected);
        if(project==null||!project.optBoolean("available",true)){selected="";unavailableProject();go(0,-1);return false;}
        if(!Store.projectEnabled(project)){go(0,-1);authorize(project);return false;}
        try{
            String message=note.getText().toString();
            String title=TaskPresentation.clip(message.isEmpty()?material:message,80);
            JSONObject task=new JSONObject().put("id",incoming.id).put("projectId",selected).put("projectName",project.optString("name")).put("content",material).put("message",message).put("title",title).put("localFiles",attachments).put("assets",new JSONArray());
            if(!model.isEmpty())task.put("model",model);if(!effort.isEmpty())task.put("effort",effort);
            incoming.save(task);sent=true;store.prefs.edit().putString("lastProject",selected).apply();
            if(Build.VERSION.SDK_INT>=30)sheet.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);else sheet.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            SyncJob.soon(this);
            TaskSyncService.start(this);
            return true;
        }catch(Exception e){if(sent)return true;error(e);return false;}
    }

    // ---- step 3: the send animation -------------------------------------------------------------
    View stepSend(){
        LinearLayout column=Ui.vertical(this);
        sendTitle=Ui.title(this,L.t("Saved on your phone","已保存在手机"),18);sendTitle.setGravity(Gravity.CENTER);column.addView(sendTitle,Ui.fill());
        TextView target=Ui.caption(this,L.t("For “","转发到「")+projectName()+L.t("”","」"));target.setGravity(Gravity.CENTER);Ui.oneLine(target);column.addView(target,Ui.fill());
        LinearLayout flight=Ui.row(this);
        TextView chip=materialChip();chip.setMaxWidth(dp(100));flight.addView(chip,new LinearLayout.LayoutParams(-2,-2));
        plane=new Ui.PlaneView(this);flight.addView(plane,new LinearLayout.LayoutParams(0,-2,1));
        badge=new TextView(this);badge.setText("Codex");badge.setTextSize(11);badge.setTypeface(Ui.medium());badge.setTextColor(Ui.TEXT);badge.setGravity(Gravity.CENTER);badge.setSingleLine(true);badge.setMinWidth(dp(48));badge.setPadding(dp(10),0,dp(10),0);
        badge.setBackground(Ui.outlined(this,Ui.SURFACE_2,Ui.LINE_STRONG,24,1));badge.setContentDescription(L.t("Codex on your computer","电脑上的 Codex"));flight.addView(badge,new LinearLayout.LayoutParams(-2,dp(48)));
        column.addView(flight,Ui.margins(this,6,2));
        status=Ui.text(this,outcome(),14,Ui.TEXT);status.setGravity(Gravity.CENTER);status.setVisibility(View.INVISIBLE);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        column.addView(status,Ui.margins(this,2,0));
        return column;
    }
    String outcome(){
        if(!store.online())return L.t("You're offline. Saved safely; it will send when connected.","手机离线，已保存。联网后自动发送。");
        if(!notificationsAllowed())return L.t("Notifications are off. Check DropRun or Codex for updates.","通知未开启，请在 App 或 Codex 查看后续。");
        return L.t("Check DropRun for progress, results and decisions.","在 DropRun 查看进度、结果和待确认事项。");
    }
    boolean notificationsAllowed(){return TaskNotifications.allowed(this);}
    void fly(){
        handler.postDelayed(()->{if(!gone())plane.play(360,this::landed);},50);
    }
    void landed(){
        if(gone())return;
        sendTitle.setText(store.online()?L.t("Saved. We'll take it from here.","已保存，自动发送"):L.t("Saved. Waiting for connection.","已保存，等待联网"));
        badge.setTextColor(Ui.ACCENT);badge.setBackground(Ui.outlined(this,Ui.LIME_SOFT,Ui.LIME_LINE,24,1));Ui.pulse(badge);
        status.setText(outcome());status.setVisibility(View.VISIBLE);Ui.fadeIn(status,220);
        int timeout=!store.online()||!notificationsAllowed()?4000:Ui.motionEnabled(this)?280:100;android.view.accessibility.AccessibilityManager accessibility=(android.view.accessibility.AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if(Build.VERSION.SDK_INT>=29)timeout=accessibility.getRecommendedTimeoutMillis(timeout,android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT|android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS);
        else if(accessibility.isTouchExplorationEnabled())timeout=8000;
        handler.postDelayed(this::close,timeout);
    }
}
