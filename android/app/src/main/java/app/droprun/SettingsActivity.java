package app.droprun;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.ref.WeakReference;

/** The one settings screen: computer link, execution mode, default model strength, project access, about. */
public class SettingsActivity extends StyledActivity {
    final ExecutorService io=Executors.newSingleThreadExecutor();
    /** Project ids whose permission change is still in flight; their chip reads 更改中 and ignores taps. */
    final Set<String> switching=new HashSet<>();
    Store store;LinearLayout body;TextView noticeView,modeNoticeView;String notice="",modeMessage="";boolean busy=false,showAccess=false;
    ModeChange modeChange,shownModeResult;int modeMessageColor=Ui.MUTED;
    TextView modelRefreshStatus;Button modelRefreshButton;boolean refreshing=false,refreshFailed=false;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);Ui.configureWindow(this);
        showAccess=state!=null&&state.getBoolean("showAccess");
        LinearLayout page=Ui.page(this);((View)page.getParent()).setId(R.id.settings_scroll);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back","返回"));back.setOnClickListener(v->finish());
        Ui.topBar(this,page,back,L.t("Settings","设置"),false,null);
        noticeView=Ui.text(this,"",12,Ui.AMBER);noticeView.setVisibility(View.GONE);page.addView(noticeView);
        body=Ui.vertical(this);page.addView(body,Ui.fill());
        ModeChange retained=(ModeChange)getLastNonConfigurationInstance();
        if(retained!=null&&sameModeScope(retained)){modeChange=retained;retained.observer=new WeakReference<>(this);showModeChange(retained);}else render();
        Ui.enter(body);refresh();
    }
    int dp(int value){return Ui.dp(this,value);}
    /** Reads the existing settings endpoints on open or explicit refresh; never approves or saves a preference. */
    void refresh(){
        if(refreshing||busy||!switching.isEmpty()||isDestroyed()||isFinishing())return;
        String before=snapshot();refreshing=true;refreshFailed=false;updateModelRefresh();
        io.execute(()->{
            boolean failed=false;
            try{JSONObject data=store.get("/projects");store.prefs.edit().putString("projects",data.toString()).apply();}catch(Exception ignored){failed=true;}
            try{store.getSettings();}catch(Exception ignored){failed=true;}
            try{JSONObject policy=store.get("/device/retention");store.prefs.edit().putString("retention",policy.toString()).apply();}catch(Exception ignored){failed=true;}
            boolean readFailed=failed;
            runOnUiThread(()->{
                if(isDestroyed()||isFinishing())return;
                refreshing=false;refreshFailed=readFailed;
                if(!snapshot().equals(before)){
                    ScrollView scroll=findViewById(R.id.settings_scroll);int scrollY=scroll.getScrollY();render();
                    scroll.post(()->{View focused=getCurrentFocus();if(!isDestroyed()&&!isFinishing()&&(focused==null||focused.isInTouchMode()))scroll.scrollTo(scroll.getScrollX(),scrollY);});
                    modelRefreshStatus.announceForAccessibility(modelRefreshStatus.getText());
                }else {updateModelRefresh();revealPreferenceAfterLayout();}
            });
        });
    }
    String modelRefreshMessage(){
        if(refreshing)return L.t("Refreshing settings…","正在刷新设置…");
        if(refreshFailed)return L.t("Some settings could not refresh. Check your Relay connection, then retry.","部分设置刷新失败。请检查中转连接后重试。");
        if(!store.computerOnline())return store.models().length()==0?L.t("Computer offline. Start Connector, then refresh.","电脑离线。启动 Connector 后刷新。"):L.t("Computer offline. Showing last synced models.","电脑离线，显示上次同步的模型。");
        if(store.models().length()==0)return L.t("No models synced yet. Keep Connector running, then refresh.","尚未同步模型。保持 Connector 运行后刷新。");
        return L.t("Models are available.","模型列表已就绪。");
    }
    void updateModelRefresh(){
        if(modelRefreshStatus==null||modelRefreshButton==null)return;
        modelRefreshStatus.setText(modelRefreshMessage());modelRefreshStatus.setTextColor(refreshFailed?Ui.AMBER:Ui.MUTED);
        boolean available=!refreshing&&!busy&&switching.isEmpty();modelRefreshButton.setEnabled(available);
        modelRefreshButton.setText(refreshFailed?L.t("Retry","重试"):L.t("Refresh","刷新"));
        modelRefreshButton.setContentDescription(refreshing?L.t("Refreshing settings","正在刷新设置"):!available?L.t("Wait for the current change before refreshing settings","当前更改完成后可刷新设置"):refreshFailed?L.t("Retry refreshing settings and models","重试刷新设置与模型"):L.t("Refresh settings and models","刷新设置与模型"));
    }
    String snapshot(){JSONObject data=store.projectsData();return data.optString("name")+store.computerOnline()+data.optJSONArray("projects")+data.optJSONArray("models")+store.directExecution()+store.prefs.getString("settingsError","")+store.prefs.getString("retention","");}
    /** Rebuilds the sections in place: the ScrollView around them keeps its position and nothing re-animates. */
    void render(){
        View focused=getCurrentFocus();boolean keepFocus=trackedFocus(focused);int focusId=keepFocus?focused.getId():View.NO_ID;Object focusTag=keepFocus?focused.getTag():null;
        noticeView.setText(notice);noticeView.setVisibility(notice.isEmpty()?View.GONE:View.VISIBLE);
        body.removeAllViews();
        computerSection();appearanceSection();modeSection();modelSection();accessSection();retentionSection();aboutSection();disconnectSection();
        if(keepFocus){View replacement=focusTag instanceof String?body.findViewWithTag(focusTag):body.findViewById(focusId);if(focusTag instanceof String&&(replacement==null||!replacement.isShown()||!replacement.isEnabled()||!replacement.isFocusable()))replacement=body.findViewById(R.id.settings_access);if(replacement!=null){replacement.requestFocus();revealPreferenceAfterLayout();}}
    }
    boolean trackedFocus(View view){return view!=null&&!view.isInTouchMode()&&(view.getId()==R.id.settings_appearance||view.getId()==R.id.settings_language||view.getId()==R.id.settings_model||view.getId()==R.id.settings_effort||view.getId()==R.id.settings_refresh||view.getId()==R.id.settings_access||view.getTag() instanceof String&&((String)view.getTag()).startsWith("project-access:"));}
    LinearLayout section(String label){body.addView(Ui.label(this,label));LinearLayout card=Ui.vertical(this);body.addView(card,Ui.cardParams(this));return card;}
    LinearLayout.LayoutParams trailing(){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMarginStart(dp(10));return params;}
    void error(Exception e){if(!isDestroyed())new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).show();}

    // ---- 电脑 -----------------------------------------------------------------------------------
    void computerSection(){
        LinearLayout card=section(L.t("Computer","电脑"));
        card.setPadding(dp(20),dp(20),dp(20),dp(20));card.setBackground(Ui.outlined(this,Ui.dark?0xFF2B372C:0xFF222B24,0,Ui.RADIUS_CARD,0));card.setElevation(Ui.dpf(this,3));
        LinearLayout head=Ui.row(this);
        ImageView computer=new ImageView(this);computer.setImageResource(R.drawable.ic_computer);computer.setImageTintList(ColorStateList.valueOf(Ui.LIME));computer.setPadding(dp(10),dp(10),dp(10),dp(10));computer.setBackground(Ui.outlined(this,0xFF354236,0,15,0));computer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);head.addView(computer,Ui.square(this,44));Ui.space(head,12);
        LinearLayout words=Ui.vertical(this);TextView name=Ui.title(this,store.computerName(),17);name.setTextColor(0xFFF4F6F0);name.setPadding(0,0,0,0);words.addView(name);words.addView(Ui.text(this,store.projects().length()+L.t(" Codex projects"," 个 Codex 项目"),12,0xFFC5CEC2),Ui.margins(this,5,0));head.addView(words,Ui.grow());
        boolean online=store.computerOnline();
        card.addView(head);
        LinearLayout connection=Ui.row(this);connection.addView(Ui.pill(this,online?L.t("Online","在线"):L.t("Offline","离线"),online?Ui.LIME:0xFFC5CEC2));Ui.space(connection,10);connection.addView(Ui.text(this,online?L.t("Ready to receive handoffs","随时可以接收任务"):L.t("Handoffs wait safely while offline","离线时任务会排队等待"),12,0xFFD3DACE),Ui.grow());card.addView(connection,Ui.margins(this,16,0));
    }
    void disconnectSection(){
        LinearLayout card=section(L.t("Connection management","连接管理"));
        Button disconnect=Ui.button(this,L.t("Disconnect this phone","断开这台手机"),false);Ui.styleGhost(disconnect);
        disconnect.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Ui.DIM,Ui.DANGER}));
        disconnect.setEnabled(!busy);
        disconnect.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L.t("Disconnect this phone?","断开连接？")).setMessage(L.t("This phone will lose access and unfinished handoffs will receive stop requests. Project files and Codex sessions stay on your computer. Scan again to reconnect.","这台手机将不能再向电脑转发任务，未完成的任务会收到停止请求。电脑上的文件和 Codex 会话不受影响。重新连接需要再次扫码。")).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Disconnect","断开"),(d,w)->disconnect()).show());
        card.addView(disconnect,Ui.margins(this,10,0));
    }
    void disconnect(){
        clearModeChange();
        busy=true;notice=L.t("Disconnecting…","正在断开…");render();
        io.execute(()->{
            try{
                store.revoke();
                runOnUiThread(()->{
                    Intent home=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    if(isDestroyed())getApplicationContext().startActivity(home);else{startActivity(home);finish();}
                });
            }catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;busy=false;notice="";render();new AlertDialog.Builder(this).setTitle(L.t("Server disconnect not confirmed","尚未确认服务器已断开")).setMessage(L.t("The Relay could not confirm revocation or stopping running work. You can forget the connection on this phone. Saved handoffs remain tied to this Relay and can resume if you pair with the same instance again.","中转服务尚未确认撤销访问或停止任务。你可以仅在这台手机忘记连接。已保存的交办仍绑定这个中转实例，重新配对同一实例后可继续发送。")).setNegativeButton(L.t("Keep connection","保留连接"),null).setPositiveButton(L.t("Forget on this phone","仅在手机忘记"),(d,w)->io.execute(()->{try{store.forgetLocal();runOnUiThread(()->{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();});}catch(Exception failed){runOnUiThread(()->error(failed));}})).show();});}
        });
    }

    // ---- 执行模式 -------------------------------------------------------------------------------
    void modeSection(){
        LinearLayout card=section(L.t("Execution","执行模式"));
        modeNoticeView=Ui.text(this,modeMessage,12,modeMessageColor);modeNoticeView.setVisibility(modeMessage.isEmpty()?View.GONE:View.VISIBLE);card.addView(modeNoticeView,Ui.margins(this,0,10));
        boolean direct=store.directExecution();
        card.addView(option(L.t("Act on the idea","直接执行"),L.t("Codex works directly in the original project","转发后立刻在项目里开工"),direct,()->changeMode(true)));
        Ui.space(card,8);
        card.addView(option(L.t("Review a plan first","先看计划"),L.t("Review and approve a plan before changes begin","先给出计划，批准后才动手"),!direct,()->changeMode(false)));
        String settingsError=store.prefs.getString("settingsError","");
        TextView hint=Ui.caption(this,settingsError.isEmpty()?L.t("Direct execution can edit original project files and run commands. Backups are not automatic. Publishing, payments and external messages still require separate approval.","直接执行会修改电脑上的项目文件并运行命令，不自动备份。生产部署、付费、对外发送仍需单独批准。"):settingsError+L.t(". Refresh when connected.","。请联网后刷新。"));
        if(!settingsError.isEmpty())hint.setTextColor(Ui.AMBER);
        hint.setPadding(0,dp(10),0,0);card.addView(hint);
    }
    LinearLayout option(String title,String detail,boolean selected,Runnable action){
        LinearLayout row=Ui.optionRow(this,title,detail,selected);
        row.setEnabled(!busy);row.setFocusable(!busy);row.setAlpha(busy?0.6f:1f);row.setOnClickListener(v->{if(!selected&&!busy)action.run();});return row;
    }
    void changeMode(boolean direct){
        if(direct)new AlertDialog.Builder(this).setTitle(L.t("Switch to direct execution?","改为直接执行？")).setMessage(L.t("Future handoffs will skip plan approval. Codex can edit the original project and run commands. Backups are not automatic.","之后转发的任务会跳过计划审批，Codex 直接修改电脑上的项目并运行命令，不自动备份。")).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Confirm","确认"),(d,w)->saveMode(true)).show();
        else saveMode(false);
    }
    void saveMode(boolean direct){
        ModeChange operation=new ModeChange(store.scope,modeWork(direct));modeChange=operation;operation.observer=new WeakReference<>(this);
        showModeChange(operation);io.execute(operation);
    }
    Callable<Void> modeWork(boolean direct){
        Store target=store;return ()->{target.setDirectExecution(direct);return null;};
    }
    boolean sameModeScope(ModeChange operation){
        return operation.scope.equals(store.scope)&&operation.scope.equals(Store.scope(store.preferences.getString("relay",""),store.preferences.getString("instanceId","")));
    }
    void showModeChange(ModeChange operation){
        if(modeChange!=operation||isDestroyed()||isFinishing())return;
        if(!sameModeScope(operation)){clearModeChange();busy=false;modeMessage=L.t("The connection changed. Reopen this screen.","连接已改变，请重新打开此页面。");modeMessageColor=Ui.AMBER;render();return;}
        if(operation.isDone()&&shownModeResult==operation)return;
        busy=!operation.isDone();modeMessageColor=busy?Ui.MUTED:Ui.AMBER;
        if(busy)modeMessage=L.t("Saving…","正在保存…");
        else try{operation.get();modeMessage=L.t("Execution preference saved.","执行偏好已保存。");modeMessageColor=Ui.ACCENT;}
        catch(Exception error){Throwable cause=error instanceof ExecutionException&&error.getCause()!=null?error.getCause():error;modeMessage=cause.getMessage();if(modeMessage==null||modeMessage.isEmpty())modeMessage=L.t("Could not save the execution preference.","执行偏好保存失败。");}
        if(!busy)shownModeResult=operation;render();
        if(!busy)modeNoticeView.post(()->{if(modeChange==operation&&!isDestroyed()&&!isFinishing()&&sameModeScope(operation)&&!operation.announced&&modeNoticeView.isAttachedToWindow()){operation.announced=true;modeNoticeView.announceForAccessibility(modeMessage);}});
    }
    void clearModeChange(){
        if(modeChange!=null&&modeChange.observer.get()==this)modeChange.observer.clear();modeChange=null;shownModeResult=null;modeMessage="";modeMessageColor=Ui.MUTED;
    }
    static final class ModeChange extends FutureTask<Void>{
        final String scope;WeakReference<SettingsActivity> observer=new WeakReference<>(null);boolean announced;
        ModeChange(String scope,Callable<Void> work){super(work);this.scope=scope;}
        @Override protected void done(){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{SettingsActivity activity=observer.get();if(activity!=null)activity.showModeChange(this);});}
    }

    // ---- 默认模型强度 ---------------------------------------------------------------------------
    void modelSection(){
        LinearLayout card=section(L.t("Default model & effort","默认模型强度"));JSONArray catalog=store.models();
        card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(Ui.outlined(this,Ui.SURFACE,0,Ui.RADIUS_CARD,0));
        boolean large=getResources().getConfiguration().fontScale>=1.5f;LinearLayout refreshRow=Ui.row(this);if(large)refreshRow.setOrientation(LinearLayout.VERTICAL);
        modelRefreshStatus=Ui.caption(this,modelRefreshMessage());modelRefreshStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        refreshRow.addView(modelRefreshStatus,large?Ui.fill():Ui.grow());
        modelRefreshButton=Ui.button(this,L.t("Refresh","刷新"),false);modelRefreshButton.setId(R.id.settings_refresh);
        modelRefreshButton.setMinHeight(dp(48));modelRefreshButton.setMinimumHeight(dp(48));modelRefreshButton.setPadding(dp(12),dp(10),dp(12),dp(10));modelRefreshButton.setOnClickListener(v->refresh());
        LinearLayout.LayoutParams refreshParams=new LinearLayout.LayoutParams(-2,-2);if(large)refreshParams.topMargin=dp(4);else refreshParams.setMarginStart(dp(8));refreshRow.addView(modelRefreshButton,refreshParams);
        updateModelRefresh();card.addView(refreshRow,Ui.margins(this,0,catalog.length()==0?0:10));
        if(catalog.length()==0)return;
        String model=store.defaultModel(),effort=store.defaultEffort(model);JSONObject current=store.model(model);
        LinearLayout choose=Ui.setting(this,L.t("Model","模型"),current==null?model:current.optString("displayName",model),()->{});
        choose.setId(R.id.settings_model);
        choose.setOnClickListener(v->{String[] names=new String[catalog.length()];int selected=0;for(int n=0;n<catalog.length();n++){JSONObject item=catalog.optJSONObject(n);names[n]=item.optString("displayName",item.optString("id"));if(item.optString("id").equals(model))selected=n;}new AlertDialog.Builder(this).setTitle(L.t("Default model","默认模型")).setSingleChoiceItems(names,selected,(dialog,index)->{String id=catalog.optJSONObject(index).optString("id");store.saveDefaults(id,store.defaultEffort(id));dialog.dismiss();render();}).setNegativeButton(L.t("Cancel","取消"),null).show();});card.addView(choose);
        JSONArray available=current==null?null:current.optJSONArray("efforts");
        if(available!=null&&available.length()>0){card.addView(Ui.divider(this));String meaning=effortWord(effort);LinearLayout strength=Ui.setting(this,L.t("Reasoning effort","推理强度"),meaning.isEmpty()?effort:meaning,()->{});strength.setId(R.id.settings_effort);strength.setOnClickListener(v->{String[] names=new String[available.length()];int selected=0;for(int n=0;n<names.length;n++){String id=available.optString(n),word=effortWord(id);names[n]=word.isEmpty()?id:word;if(id.equals(effort))selected=n;}new AlertDialog.Builder(this).setTitle(L.t("Reasoning effort","推理强度")).setSingleChoiceItems(names,selected,(dialog,index)->{store.saveDefaults(model,available.optString(index));dialog.dismiss();render();}).setNegativeButton(L.t("Cancel","取消"),null).show();});card.addView(strength);}
        card.addView(Ui.caption(this,L.t("You can still change these for an individual handoff.","每次转发时仍可临时更改。")),Ui.margins(this,10,0));
    }
    /** One-line key for the effort ids the model offers, e.g. "low 快 · medium 均衡 · high 深入". */
    static String legend(List<String> efforts){
        StringBuilder legend=new StringBuilder();
        for(String effort:efforts){
            String word=effortWord(effort);if(word.isEmpty())continue;
            if(legend.length()>0)legend.append(" · ");
            legend.append(effort).append(' ').append(word);
        }
        return legend.toString();
    }
    static String effortWord(String effort){
        return switch(effort){
            case "minimal" -> L.t("Fastest","最快");
            case "low" -> L.t("Fast","快");
            case "medium" -> L.t("Balanced","均衡");
            case "high" -> L.t("Thorough","深入");
            case "xhigh" -> L.t("Most thorough","最深");
            default -> "";
        };
    }

    // ---- 项目授权 -------------------------------------------------------------------------------
    void accessSection(){
        LinearLayout container=section(L.t("Project access","项目授权"));LinearLayout card=Ui.vertical(this);
        container.setPadding(dp(16),dp(12),dp(16),dp(12));container.setBackground(Ui.outlined(this,Ui.SURFACE,0,Ui.RADIUS_CARD,0));
        JSONArray projects=store.projects(),history=store.activity();
        LinearLayout disclosure=Ui.disclosure(this,projects.length()+L.t(" projects · manage access"," 个项目 · 管理授权"),card,showAccess,open->showAccess=open);disclosure.getChildAt(0).setId(R.id.settings_access);container.addView(disclosure);
        if(projects.length()==0)card.addView(Ui.caption(this,L.t("No projects have synced yet. Check that your Connector is running.","电脑还没有同步项目。请确认 Connector 已启动。")));
        for(int n=0;n<projects.length();n++){
            JSONObject p=projects.optJSONObject(n);if(p==null)continue;
            boolean enabled=Store.projectEnabled(p);String id=p.optString("id"),projectName=p.optString("name"),name=ProjectPresentation.label(id,projectName,projects,history);
            if(card.getChildCount()>0)card.addView(Ui.divider(this));
            boolean large=getResources().getConfiguration().fontScale>=1.5f;LinearLayout line=Ui.row(this);if(large)line.setOrientation(LinearLayout.VERTICAL);line.setPadding(0,dp(6),0,dp(6));line.setMinimumHeight(dp(48));
            TextView title=Ui.text(this,name,15,Ui.TEXT);title.setTypeface(Ui.medium());title.setContentDescription(name);
            if(!name.equals(projectName)){
                android.text.SpannableString identity=new android.text.SpannableString(projectName+"\n"+name.substring(projectName.length()+3));int hintStart=projectName.length()+1;
                identity.setSpan(new android.text.style.AbsoluteSizeSpan(Math.round(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,12,getResources().getDisplayMetrics()))),hintStart,identity.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                identity.setSpan(new android.text.style.ForegroundColorSpan(Ui.MUTED),hintStart,identity.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);title.setText(identity);
            }
            line.addView(title,large?Ui.fill():Ui.grow());
            boolean pending=switching.contains(id);
            TextView toggle=Ui.chip(this,pending?L.t("Updating","更改中"):enabled?L.t("Allowed","已允许"):L.t("Not allowed","未允许"),enabled);
            toggle.setTag("project-access:"+id);
            toggle.setEnabled(!pending&&!busy);toggle.setAlpha(pending||busy?0.5f:1f);
            toggle.setContentDescription(name+(pending?L.t(", updating","，正在更改"):enabled?L.t(", allowed, tap to revoke access","，已允许，点按停止转发"):L.t(", not allowed, tap to allow","，未允许，点按允许")));
            toggle.setOnClickListener(v->{
                if(enabled)new AlertDialog.Builder(this).setTitle(L.t("Stop handoffs to “","停止向「")+name+L.t("”?","」转发？")).setMessage(L.t("Queued and running handoffs for this project will be stopped. Changes already made will not be undone.","这个项目里排队和进行中的任务会被停止；已经发生的改动不会回滚。")).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Stop","停止"),(d,w)->setPermission(id,false)).show();
                else new AlertDialog.Builder(this).setTitle(L.t("Allow handoffs to this project?","允许向这个项目交办？")).setMessage(name+"\n\n"+L.t("Future shares can ask Codex to edit this original project using your execution preference.","之后的分享可按你的执行偏好，请 Codex 修改这个原项目。" )).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Allow","允许"),(d,w)->setPermission(id,true)).show();
            });
            LinearLayout.LayoutParams toggleParams=large?new LinearLayout.LayoutParams(-2,-2):trailing();if(large)toggleParams.topMargin=dp(8);line.addView(toggle,toggleParams);card.addView(line);
        }
        TextView note=Ui.caption(this,L.t("You can also allow a project when sharing to it for the first time.","第一次转发到某个项目时也会询问。"));note.setPadding(0,dp(8),0,0);card.addView(note);
    }
    void setPermission(String id,boolean enabled){
        switching.add(id);render();
        io.execute(()->{
            try{store.setProjectPermission(id,enabled);runOnUiThread(()->{if(isDestroyed())return;switching.remove(id);render();});}
            catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;switching.remove(id);render();error(e);});}
        });
    }

    void retentionSection(){
        LinearLayout card=section(L.t("Data retention","数据保留"));
        card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(Ui.outlined(this,Ui.SURFACE,0,Ui.RADIUS_CARD,0));
        JSONObject policy=new JSONObject();try{policy=new JSONObject(store.prefs.getString("retention","{}"));}catch(JSONException ignored){}
        int raw=policy.optInt("rawDays"),artifacts=policy.optInt("artifactDays");
        if(raw>=1&&raw<=365&&artifacts>=1&&artifacts<=365){
            card.addView(Ui.text(this,L.t("Last synced Relay policy","上次同步的中转策略"),13,Ui.MUTED));
            card.addView(Ui.text(this,L.t("Original uploads · ","原始上传 · ")+raw+L.t(" days after a task ends"," 天（任务结束后）"),15,Ui.TEXT),Ui.margins(this,8,0));
            card.addView(Ui.text(this,L.t("Screenshots & files · ","截图与文件 · ")+artifacts+L.t(" days after a task ends"," 天（任务结束后）"),15,Ui.TEXT),Ui.margins(this,4,0));
        }else card.addView(Ui.text(this,L.t("Connect, then use Refresh in Default model & effort to load your Relay’s retention policy.","联网后，在“默认模型强度”中点按刷新，获取中转服务保留策略。"),15,Ui.TEXT));
        card.addView(Ui.text(this,L.t("Reports stay until you delete them. Preview links and snapshots have separate expiry times. Cleanup runs periodically and does not delete your computer’s project files or backups.","报告保留至你删除。预览链接与快照另有有效期。清理定期运行，不会删除电脑上的项目文件或备份。"),13,Ui.TEXT),Ui.margins(this,12,0));
        card.addView(Ui.text(this,L.t("Change retention in your Relay deployment settings. This cached policy may be outdated while offline. Saved handoffs on this phone stay until sent or removed from project history.","在中转部署配置中修改保留期限。离线时，缓存的策略可能已过时。手机上的待发送副本保留至发送成功，或在项目历史中手动移除。"),13,Ui.MUTED),Ui.margins(this,8,0));
    }

    // ---- 关于 -----------------------------------------------------------------------------------
    void aboutSection(){
        LinearLayout card=section(L.t("About","关于"));
        TextView version=Ui.text(this,"DropRun "+BuildConfig.VERSION_NAME,15,Ui.TEXT);version.setTypeface(Ui.medium());card.addView(version);
        card.addView(Ui.caption(this,L.t("Your Relay: ","你的中转服务：")+store.relay().replace("https://","")));
        card.addView(Ui.caption(this,L.t("Self-hosted. Shared material and reports pass through your Relay. Codex credentials stay on your computer; transcription follows your Connector configuration.","自主部署。材料与报告经你的中转服务传递；Codex 凭证留在电脑，转写使用 Connector 配置的服务。")));
        Button notifications=Ui.button(this,L.t("Notification settings","通知设置"),false);notifications.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName())));card.addView(notifications,Ui.margins(this,12,0));
        TextView help=Ui.caption(this,L.t("Need help? Open DropRun setup on your computer to check tools and connection. Share only redacted diagnostics.","需要帮助？在电脑打开 DropRun 配置页，检查工具与连接；仅分享脱敏后的诊断信息。"));card.addView(help,Ui.margins(this,12,0));
    }

    void appearanceSection(){
        LinearLayout card=section(L.t("Make it yours","外观与语言"));String mode=store.preferences.getString("appearance","light");String[] values={"light","dark","system"};String[] labels={L.t("Light","浅色"),L.t("Dark","深色"),L.t("System","跟随系统")};int selected=java.util.Arrays.asList(values).indexOf(mode);
        LinearLayout appearance=Ui.setting(this,L.t("Appearance","外观"),labels[Math.max(0,selected)],()->new AlertDialog.Builder(this).setTitle(L.t("Appearance","外观")).setSingleChoiceItems(labels,Math.max(0,selected),(dialog,index)->{store.preferences.edit().putString("appearance",values[index]).apply();dialog.dismiss();recreate();}).setNegativeButton(L.t("Cancel","取消"),null).show());appearance.setId(R.id.settings_appearance);card.addView(appearance);
        card.addView(Ui.divider(this));
        LinearLayout language=Ui.setting(this,L.t("Language","语言"),L.chinese()?"简体中文":"English",()->new AlertDialog.Builder(this).setTitle(L.t("Language","语言")).setSingleChoiceItems(new String[]{"English","简体中文"},L.chinese()?1:0,(dialog,index)->{store.preferences.edit().putString("language",index==0?"en":"zh").apply();dialog.dismiss();recreate();}).setNegativeButton(L.t("Cancel","取消"),null).show());language.setId(R.id.settings_language);card.addView(language);
    }

    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("showAccess",showAccess);super.onSaveInstanceState(state);}
    @Override protected void onRestoreInstanceState(Bundle state){
        super.onRestoreInstanceState(state);revealPreferenceAfterLayout();
    }
    void revealPreferenceAfterLayout(){
        body.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            body.getViewTreeObserver().removeOnPreDrawListener(this);View focused=getCurrentFocus();
            if(trackedFocus(focused))
                focused.requestRectangleOnScreen(new android.graphics.Rect(0,0,focused.getWidth(),focused.getHeight()),true);
            return true;
        }});
    }
    @Override public Object onRetainNonConfigurationInstance(){return modeChange;}
    @Override protected void onDestroy(){refreshing=false;clearModeChange();io.shutdown();super.onDestroy();}
}
