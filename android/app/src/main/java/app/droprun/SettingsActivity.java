package app.droprun;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** The one settings screen: computer link, execution mode, default model strength, project access, about. */
public class SettingsActivity extends StyledActivity {
    final ExecutorService io=Executors.newSingleThreadExecutor();
    /** Project ids whose permission change is still in flight; their chip reads 更改中 and ignores taps. */
    final Set<String> switching=new HashSet<>();
    Store store;LinearLayout body;TextView noticeView;String notice="";boolean busy=false,showAccess=false;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);Ui.configureWindow(this);
        LinearLayout page=Ui.page(this);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back","返回"));back.setOnClickListener(v->finish());
        Ui.topBar(this,page,back,L.t("Settings","设置"),false,null);
        noticeView=Ui.text(this,"",12,Ui.AMBER);noticeView.setVisibility(View.GONE);page.addView(noticeView);
        body=Ui.vertical(this);page.addView(body,Ui.fill());
        render();Ui.enter(body);refresh();
    }
    int dp(int value){return Ui.dp(this,value);}
    /** Projects and the execution mode are re-read from the relay each time the screen opens; the sections rebuild only if something changed. */
    void refresh(){
        String before=snapshot();
        io.execute(()->{
            try{JSONObject data=store.get("/projects");store.prefs.edit().putString("projects",data.toString()).apply();}catch(Exception ignored){}
            try{store.getSettings();}catch(Exception ignored){}
            runOnUiThread(()->{if(!isDestroyed()&&!snapshot().equals(before))render();});
        });
    }
    String snapshot(){JSONObject data=store.projectsData();return data.optString("name")+store.computerOnline()+data.optJSONArray("projects")+data.optJSONArray("models")+store.directExecution()+store.prefs.getString("settingsError","");}
    /** Rebuilds the sections in place: the ScrollView around them keeps its position and nothing re-animates. */
    void render(){
        noticeView.setText(notice);noticeView.setVisibility(notice.isEmpty()?View.GONE:View.VISIBLE);
        body.removeAllViews();
        computerSection();appearanceSection();modeSection();modelSection();accessSection();aboutSection();disconnectSection();
    }
    LinearLayout section(String label){body.addView(Ui.label(this,label));LinearLayout card=Ui.card(this);body.addView(card,Ui.cardParams(this));return card;}
    LinearLayout.LayoutParams trailing(){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMarginStart(dp(10));return params;}
    void error(Exception e){if(!isDestroyed())new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).show();}

    // ---- 电脑 -----------------------------------------------------------------------------------
    void computerSection(){
        LinearLayout card=section(L.t("Computer","电脑"));
        LinearLayout head=Ui.row(this);
        TextView name=Ui.text(this,store.computerName(),17,Ui.TEXT);name.setTypeface(Ui.medium());name.setPadding(0,0,0,0);head.addView(name,Ui.grow());
        boolean online=store.computerOnline();
        head.addView(Ui.pill(this,online?L.t("Online","在线"):L.t("Offline","离线"),online?Ui.ACCENT:Ui.MUTED),trailing());
        card.addView(head);
        card.addView(Ui.caption(this,store.projects().length()+L.t(" Codex projects · "," 个 Codex 项目 · ")+(online?L.t("Ready to receive handoffs","随时可以接收任务"):L.t("Handoffs wait safely while offline","离线时任务会排队等待"))));
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
        row.setAlpha(busy?0.6f:1f);row.setOnClickListener(v->{if(!selected&&!busy)action.run();});return row;
    }
    void changeMode(boolean direct){
        if(direct)new AlertDialog.Builder(this).setTitle(L.t("Switch to direct execution?","改为直接执行？")).setMessage(L.t("Future handoffs will skip plan approval. Codex can edit the original project and run commands. Backups are not automatic.","之后转发的任务会跳过计划审批，Codex 直接修改电脑上的项目并运行命令，不自动备份。")).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Confirm","确认"),(d,w)->saveMode(true)).show();
        else saveMode(false);
    }
    void saveMode(boolean direct){
        busy=true;notice=L.t("Saving…","正在保存…");render();
        io.execute(()->{
            try{store.setDirectExecution(direct);runOnUiThread(()->{if(isDestroyed())return;busy=false;notice=L.t("Execution preference saved.","执行偏好已保存。");render();noticeView.announceForAccessibility(notice);});}
            catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;busy=false;notice=e.getMessage();render();});}
        });
    }

    // ---- 默认模型强度 ---------------------------------------------------------------------------
    void modelSection(){
        LinearLayout card=section(L.t("Default model & effort","默认模型强度"));JSONArray catalog=store.models();
        if(catalog.length()==0){card.addView(Ui.caption(this,L.t("Waiting for your computer's model list.","等待电脑同步模型列表。")));return;}
        String model=store.defaultModel(),effort=store.defaultEffort(model);JSONObject current=store.model(model);
        Button choose=Ui.button(this,L.t("Model · ","模型 · ")+(current==null?model:current.optString("displayName",model)),false);
        choose.setOnClickListener(v->{String[] names=new String[catalog.length()];int selected=0;for(int n=0;n<catalog.length();n++){JSONObject item=catalog.optJSONObject(n);names[n]=item.optString("displayName",item.optString("id"));if(item.optString("id").equals(model))selected=n;}new AlertDialog.Builder(this).setTitle(L.t("Default model","默认模型")).setSingleChoiceItems(names,selected,(dialog,index)->{String id=catalog.optJSONObject(index).optString("id");store.saveDefaults(id,store.defaultEffort(id));dialog.dismiss();render();}).setNegativeButton(L.t("Cancel","取消"),null).show();});card.addView(choose);
        JSONArray available=current==null?null:current.optJSONArray("efforts");
        if(available!=null&&available.length()>0){Button strength=Ui.button(this,L.t("Effort · ","推理强度 · ")+effort,false);strength.setOnClickListener(v->{String[] names=new String[available.length()];int selected=0;for(int n=0;n<names.length;n++){names[n]=available.optString(n);if(names[n].equals(effort))selected=n;}new AlertDialog.Builder(this).setTitle(L.t("Reasoning effort","推理强度")).setSingleChoiceItems(names,selected,(dialog,index)->{store.saveDefaults(model,available.optString(index));dialog.dismiss();render();}).setNegativeButton(L.t("Cancel","取消"),null).show();});card.addView(strength,Ui.margins(this,8,0));}
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
        JSONArray projects=store.projects();
        container.addView(Ui.disclosure(this,projects.length()+L.t(" projects · manage access"," 个项目 · 管理授权"),card,showAccess,open->showAccess=open));
        if(projects.length()==0)card.addView(Ui.caption(this,L.t("No projects have synced yet. Check that your Connector is running.","电脑还没有同步项目。请确认 Connector 已启动。")));
        for(int n=0;n<projects.length();n++){
            JSONObject p=projects.optJSONObject(n);if(p==null)continue;
            boolean enabled=Store.projectEnabled(p);String id=p.optString("id"),name=p.optString("name");
            if(card.getChildCount()>0)card.addView(Ui.divider(this));
            LinearLayout line=Ui.row(this);line.setPadding(0,dp(6),0,dp(6));line.setMinimumHeight(dp(48));
            TextView title=Ui.text(this,name,15,Ui.TEXT);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);line.addView(title,Ui.grow());
            boolean pending=switching.contains(id);
            TextView toggle=Ui.chip(this,pending?L.t("Updating","更改中"):enabled?L.t("Allowed","已允许"):L.t("Not allowed","未允许"),enabled);
            toggle.setEnabled(!pending&&!busy);toggle.setAlpha(pending?0.5f:1f);
            toggle.setContentDescription(name+(pending?L.t(", updating","，正在更改"):enabled?L.t(", allowed, tap to revoke access","，已允许，点按停止转发"):L.t(", not allowed, tap to allow","，未允许，点按允许")));
            toggle.setOnClickListener(v->{
                if(enabled)new AlertDialog.Builder(this).setTitle(L.t("Stop handoffs to “","停止向「")+name+L.t("”?","」转发？")).setMessage(L.t("Queued and running handoffs for this project will be stopped. Changes already made will not be undone.","这个项目里排队和进行中的任务会被停止；已经发生的改动不会回滚。")).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Stop","停止"),(d,w)->setPermission(id,false)).show();
                else new AlertDialog.Builder(this).setTitle(L.t("Allow handoffs to this project?","允许向这个项目交办？")).setMessage(name+"\n\n"+L.t("Future shares can ask Codex to edit this original project using your execution preference.","之后的分享可按你的执行偏好，请 Codex 修改这个原项目。" )).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Allow","允许"),(d,w)->setPermission(id,true)).show();
            });
            line.addView(toggle,trailing());card.addView(line);
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
        card.addView(Ui.setting(this,L.t("Appearance","外观"),labels[Math.max(0,selected)],()->new AlertDialog.Builder(this).setTitle(L.t("Appearance","外观")).setSingleChoiceItems(labels,Math.max(0,selected),(dialog,index)->{store.preferences.edit().putString("appearance",values[index]).apply();dialog.dismiss();recreate();}).setNegativeButton(L.t("Cancel","取消"),null).show()));
        card.addView(Ui.divider(this));
        card.addView(Ui.setting(this,L.t("Language","语言"),L.chinese()?"简体中文":"English",()->new AlertDialog.Builder(this).setTitle(L.t("Language","语言")).setSingleChoiceItems(new String[]{"English","简体中文"},L.chinese()?1:0,(dialog,index)->{store.preferences.edit().putString("language",index==0?"en":"zh").apply();dialog.dismiss();recreate();}).setNegativeButton(L.t("Cancel","取消"),null).show()));
    }

    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
