package app.droprun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
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
    Store store;FrameLayout root,stage;Capped holder;LinearLayout sheet,projectList,panel;ScrollView scroll;Ui.Dots dots;ImageButton back,gauge;
    EditText search,note;TextView gaugeText,sendTitle,status,badge;Ui.PlaneView plane;Ui.Glass dialog;ColorDrawable scrim;ValueAnimator panelAnimator;
    AlertDialog discardDialog;
    String shared="",last="",selected="",model="",effort="",query="",draft="";JSONArray attachments=new JSONArray();
    int step=-1;boolean receiving=true,showAll=false,panelOpen=false,busy=false,closing=false,sent=false;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);Ui.configureOverlay(this);overridePendingTransition(0,0);
        Intent intent=getIntent();CharSequence text=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);shared=text==null?"":text.toString();
        build();
        if(state!=null&&state.getBoolean("sent")){sent=true;receiving=false;close();return;}
        if(state!=null&&state.containsKey("attachments")){
            try{attachments=new JSONArray(state.getString("attachments"));}catch(JSONException e){fatal(e);return;}
            shared=state.getString("shared",shared);selected=state.getString("selected","");draft=state.getString("draft","");query=state.getString("query","");showAll=state.getBoolean("showAll");
            receiving=false;
            if(store.paired()){start();model=state.getString("model",model);effort=state.getString("effort",effort);if(state.getInt("step")==1&&Store.projectEnabled(store.project(selected)))go(1,1);}
            else unpaired();return;
        }
        // Only a slow copy (a large video) shows the receiving notice; text shares go straight to step 1.
        handler.postDelayed(()->{if(receiving&&!gone())Ui.swap(stage,notice(L.t("Receiving shared material…","正在接收分享…")),1);},200);
        io.execute(()->{
            try{receive(intent);runOnUiThread(this::received);}
            catch(Exception e){runOnUiThread(()->fatal(e));}
        });
    }
    void received(){
        receiving=false;if(gone()){discardAttachments();return;}
        if(shared.trim().isEmpty()&&attachments.length()==0){fatal(new IOException(L.t("Share a link, text or file.","请分享链接、文字或文件")));return;}
        if(store.paired())start();else unpaired();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PAIR){store=new Store(this);if(result==RESULT_OK&&store.paired())start();else close();}
    }
    @Override public void onBackPressed(){
        if(dialog!=null){if(!busy)closeDialog(null);return;}
        if(step==1)go(0,-1);else close();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);io.shutdown();
        if(discardDialog!=null)discardDialog.dismiss();
        if(!sent&&isFinishing())discardAttachments();
        super.onDestroy();
    }
    void discardAttachments(){for(int n=0;n<attachments.length();n++){JSONObject a=attachments.optJSONObject(n);if(a!=null)new File(a.optString("path")).delete();}}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putBoolean("sent",sent);
        state.putString("query",query);state.putBoolean("showAll",showAll);
        if(!receiving){state.putString("attachments",attachments.toString());state.putString("shared",shared);state.putString("selected",selected);state.putString("draft",note==null?draft:note.getText().toString());state.putString("model",model);state.putString("effort",effort);state.putInt("step",step);}
    }
    int dp(int value){return Ui.dp(this,value);}
    boolean gone(){return closing||isFinishing()||isDestroyed();}

    // ---- overlay chrome -------------------------------------------------------------------------
    void build(){
        root=Ui.frame(this,false);scrim=new ColorDrawable(Ui.SCRIM);root.setBackground(scrim);root.setOnClickListener(v->close());root.setContentDescription(L.t("Cancel","取消"));
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
    /** Bottom sheet holder: never taller than 86% of the visible height, so long steps scroll inside the sheet. */
    static final class Capped extends FrameLayout {
        Capped(Context context){super(context);}
        @Override protected void onMeasure(int w,int h){
            if(MeasureSpec.getMode(h)==MeasureSpec.UNSPECIFIED){super.onMeasure(w,h);return;}
            super.onMeasure(w,MeasureSpec.makeMeasureSpec(Math.round(MeasureSpec.getSize(h)*0.86f),MeasureSpec.AT_MOST));
        }
    }
    void dim(boolean in){
        int full=Color.alpha(Ui.SCRIM);
        if(!Ui.motionEnabled(this)){scrim.setAlpha(in?full:0);return;}
        if(in)scrim.setAlpha(0);ObjectAnimator.ofInt(scrim,"alpha",in?0:full,in?full:0).setDuration(in?240:200).start();
    }
    void close(){
        if(closing)return;
        String message=note==null?draft:note.getText().toString();
        if(!sent&&!message.trim().isEmpty()){
            if(discardDialog!=null&&discardDialog.isShowing())return;
            discardDialog=new AlertDialog.Builder(this).setTitle(L.t("Discard your note?","放弃这段留言？")).setMessage(L.t("Nothing has been handed off. Closing will remove your note.","这次分享尚未交办。关闭后，这段留言将被丢弃。")).setNegativeButton(L.t("Keep editing","继续编辑"),null).setPositiveButton(L.t("Discard","放弃"),(d,w)->finishShare()).show();
            return;
        }
        finishShare();
    }
    void finishShare(){
        if(closing)return;closing=true;hideKeyboard();closeDialog(null);
        dim(false);Ui.slideDown(holder,()->{finish();overridePendingTransition(0,0);});
    }
    void hideKeyboard(){View focus=getCurrentFocus();if(focus==null)return;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}
    TextView notice(String value){TextView view=Ui.text(this,value,13,Ui.MUTED);view.setGravity(Gravity.CENTER);view.setPadding(dp(10),dp(14),dp(10),dp(14));return view;}
    void error(Exception e){if(gone())return;new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).show();}
    /** Nothing to show after this error: the overlay closes once the message is dismissed. */
    void fatal(Exception e){receiving=false;if(gone())return;new AlertDialog.Builder(this).setTitle(L.t("Could not complete this action","暂时无法完成")).setMessage(e.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).setOnDismissListener(d->close()).show();}

    // ---- receiving the share --------------------------------------------------------------------
    void receive(Intent intent)throws Exception{
        ArrayList<Uri> uris=new ArrayList<>();
        if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())){ArrayList<Uri> list=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(list!=null)uris.addAll(list);}
        else{Uri single=intent.getParcelableExtra(Intent.EXTRA_STREAM);if(single!=null)uris.add(single);}
        if(uris.isEmpty()&&intent.getClipData()!=null)for(int n=0;n<intent.getClipData().getItemCount();n++){Uri u=intent.getClipData().getItemAt(n).getUri();if(u!=null&&"content".equals(u.getScheme()))uris.add(u);}
        JSONArray received=new JSONArray();
        try{
            for(Uri uri:uris){
                if(!"content".equals(uri.getScheme()))throw new IOException(L.t("Send files using the system share menu.","请通过系统分享发送文件。"));
                if(received.length()>=8)throw new IOException(L.t("Share up to 8 attachments at a time.","一次最多 8 个附件"));
                String mime=getContentResolver().getType(uri);if(mime==null)mime=intent.getType();if(mime==null)mime="application/octet-stream";
                String name="shared-file";try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int index=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(index>=0&&c.getString(index)!=null)name=c.getString(index);}}
                File dir=store.attachments();File file=new File(dir,UUID.randomUUID().toString());long size=0;
                try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>50L*1024*1024)throw new IOException(L.t("An attachment exceeds the 50 MB limit.","附件超过 50 MB"));out.write(buffer,0,n);}}
                catch(Exception e){file.delete();throw e;}
                received.put(new JSONObject().put("path",file.getAbsolutePath()).put("name",name).put("mime",mime));
            }
        }catch(Exception e){for(int n=0;n<received.length();n++)new File(received.getJSONObject(n).getString("path")).delete();throw e;}
        attachments=received;
    }
    void unpaired(){
        dots.setVisibility(View.INVISIBLE);LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("Connect your computer first","先连接电脑"),20));
        column.addView(Ui.text(this,L.t("Send ideas to Codex on your own computer. Connect once, then share from your favorite apps.","分享会发送到你电脑上的 Codex。连接一次，以后从任何 App 分享都能直接转发。"),14,Ui.MUTED),Ui.margins(this,2,14));
        Button pair=Ui.button(this,L.t("Scan to connect","扫码连接"),true);pair.setOnClickListener(v->startActivityForResult(new Intent(this,PairActivity.class),PAIR));column.addView(pair,Ui.fill());
        Ui.swap(stage,column,1);
    }
    void start(){
        try{for(int n=0;n<attachments.length();n++){JSONObject item=attachments.getJSONObject(n);File previous=new File(item.getString("path"));if(!previous.getCanonicalFile().getParentFile().equals(store.attachments().getCanonicalFile())){if(!previous.getCanonicalPath().startsWith(getFilesDir().getCanonicalPath()+File.separator))throw new IOException("Invalid saved attachment.");File next=new File(store.attachments(),UUID.randomUUID().toString());java.nio.file.Files.move(previous.toPath(),next.toPath());item.put("path",next.getAbsolutePath());}}}catch(Exception e){fatal(e);return;}
        dots.setVisibility(View.VISIBLE);last=store.prefs.getString("lastProject","");model=store.defaultModel();effort=store.defaultEffort(model);
        go(0,1);
        String before=store.projects().toString();
        io.execute(()->{
            try{JSONObject data=store.get("/projects");store.prefs.edit().putString("projects",data.toString()).apply();}catch(Exception ignored){}
            runOnUiThread(()->{
                if(gone())return;
                if(model.isEmpty()){model=store.defaultModel();effort=store.defaultEffort(model);updateGauge();}
                if(step==0&&!store.projects().toString().equals(before))renderProjects();
            });
        });
    }
    void go(int target,int direction){
        if(gone())return;
        if(step==1&&note!=null)draft=note.getText().toString();
        hideKeyboard();step=target;
        Ui.swap(stage,target==0?stepProject():target==1?stepNote():stepSend(),direction);
        dots.setActive(target,true);back.setVisibility(target==1?View.VISIBLE:View.INVISIBLE);scroll.scrollTo(0,0);
        if(target==2)fly();
    }
    String projectName(){JSONObject project=store.project(selected);return project==null?"":project.optString("name");}

    // ---- the shared material --------------------------------------------------------------------
    TextView materialChip(){
        String label=materialLabel();TextView chip=new TextView(this);chip.setText(label);chip.setTextSize(13);chip.setTextColor(Ui.TEXT);chip.setTypeface(Ui.medium());chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12),dp(7),dp(12),dp(7));chip.setMinHeight(dp(34));chip.setBackground(Ui.outlined(this,Ui.SURFACE_2,Ui.LINE_STRONG,Ui.RADIUS,1));Ui.oneLine(chip);
        chip.setContentDescription(L.t("Shared material: ","分享的材料：")+label);return chip;
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
        LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("Where should this idea go?","转发给哪个项目？"),20));
        LinearLayout.LayoutParams chipParams=new LinearLayout.LayoutParams(-2,-2);chipParams.setMargins(0,dp(4),0,dp(12));column.addView(materialChip(),chipParams);
        LinearLayout box=Ui.card(this);box.setPadding(dp(6),dp(6),dp(6),dp(6));
        search=new EditText(this);search.setHint(L.t("Find a project","搜索项目"));search.setSingleLine(true);search.setImeOptions(EditorInfo.IME_ACTION_DONE);search.setText(query);Ui.styleInput(search);
        box.addView(search,Ui.margins(this,2,6));
        projectList=Ui.vertical(this);box.addView(projectList,Ui.fill());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();renderProjects();}public void afterTextChanged(Editable e){}});
        column.addView(box,Ui.margins(this,0,4));
        renderProjects();
        return column;
    }
    void renderProjects(){
        if(projectList==null)return;projectList.removeAllViews();
        JSONArray projects=store.projects();
        search.setVisibility(projects.length()>6||!query.isEmpty()?View.VISIBLE:View.GONE);
        if(projects.length()==0){projectList.addView(notice(L.t("Waiting for projects. Check that DropRun Connector is running on your computer.","等待电脑同步项目。请确认电脑上的 DropRun Connector 已启动。")));return;}
        // The chosen (else last-used) project first; long catalogs collapse behind "show all" unless searching.
        String filter=query.trim().toLowerCase(Locale.ROOT);
        String first=selected.isEmpty()?last:selected;ArrayList<JSONObject> ordered=new ArrayList<>();
        for(int n=0;n<projects.length();n++){JSONObject p=projects.optJSONObject(n);if(p==null)continue;if(p.optString("id").equals(first))ordered.add(0,p);else ordered.add(p);}
        int shown=0,hidden=0;
        for(JSONObject p:ordered){
            if(!filter.isEmpty()&&!p.optString("name").toLowerCase(Locale.ROOT).contains(filter))continue;
            if(filter.isEmpty()&&!showAll&&shown>=6){hidden++;continue;}
            if(shown>0){LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,Math.max(1,dp(1)));line.setMargins(dp(12),0,dp(12),0);projectList.addView(Ui.divider(this),line);}
            projectList.addView(projectRow(p),Ui.fill());shown++;
        }
        if(shown==0)projectList.addView(notice(L.t("No matching projects","没有匹配的项目")));
        if(hidden>0){TextView more=Ui.linkButton(this,L.t("Show all ","显示全部 ")+projects.length()+L.t(" projects"," 个项目"));more.setOnClickListener(v->{showAll=true;renderProjects();});LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMargins(dp(6),dp(6),0,dp(2));projectList.addView(more,params);}
    }
    View projectRow(JSONObject project){
        String id=project.optString("id"),name=project.optString("name");boolean enabled=Store.projectEnabled(project),chosen=id.equals(selected);
        LinearLayout row=Ui.row(this);row.setPadding(dp(12),dp(11),dp(12),dp(11));row.setMinimumHeight(dp(52));
        if(chosen)row.setBackground(Ui.outlined(this,Ui.LIME_SOFT,0,10,0));
        TextView label=Ui.text(this,name,15,chosen?Ui.ACCENT:Ui.TEXT);label.setPadding(0,0,0,0);if(chosen)label.setTypeface(Ui.medium());row.addView(label,Ui.grow());
        Ui.space(row,10);row.addView(Ui.pill(this,enabled?L.t("Allowed","已授权"):L.t("Allow access","需授权"),enabled?Ui.ACCENT:Ui.MUTED));
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(name+(enabled?L.t(", allowed","，已授权"):L.t(", permission required","，需授权"))+(chosen?L.t(", selected","，已选择"):""));Ui.bindPress(row);
        row.setOnClickListener(v->pick(project));
        return row;
    }
    void pick(JSONObject project){
        if(busy||step!=0)return;
        if(!Store.projectEnabled(project)){authorize(project);return;}
        selected=project.optString("id");renderProjects();busy=true;
        handler.postDelayed(()->{busy=false;if(!gone()&&step==0)go(1,1);},160);
    }
    void authorize(JSONObject project){
        String id=project.optString("id"),name=project.optString("name");hideKeyboard();
        Ui.Glass glass=Ui.glass(this,root,sheet);dialog=glass;glass.overlay.setOnClickListener(v->{if(!busy)closeDialog(null);});
        LinearLayout card=glass.card;
        card.addView(Ui.title(this,L.t("“","「")+name+L.t("” needs permission","」需要授权"),18));
        card.addView(Ui.text(this,L.t("Allow handoffs from this phone to read and edit the original project and run commands, subject to your execution setting. You can revoke this in Settings.","授权后，从这台手机转发到这个项目的任务，Codex 会直接在这个项目目录里读写文件和运行命令。可以在设置里随时关闭。"),14,Ui.MUTED),Ui.margins(this,4,12));
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
        LinearLayout card=glass.card;card.removeAllViews();
        Ui.CheckView check=new Ui.CheckView(this);check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.gravity=Gravity.CENTER_HORIZONTAL;params.topMargin=dp(8);card.addView(check,params);
        TextView done=Ui.title(this,L.t("Allowed","已授权"),16);done.setGravity(Gravity.CENTER);card.addView(done,Ui.margins(this,12,4));
        check.play(()->handler.postDelayed(()->closeDialog(()->{if(!gone()&&step==0)go(1,1);}),320));
    }
    void closeDialog(Runnable end){Ui.Glass open=dialog;dialog=null;if(open!=null)open.dismiss(end);else if(end!=null)end.run();}

    // ---- step 2: note, model and effort ---------------------------------------------------------
    View stepNote(){
        LinearLayout column=Ui.vertical(this);
        column.addView(Ui.title(this,L.t("What should Codex do?","想让 Codex 做什么？"),20));
        TextView target=Ui.caption(this,L.t("For “","转发到「")+projectName()+L.t("”","」"));column.addView(target);
        note=new EditText(this);note.setHint(L.t("Optional. Leave this blank and let Codex find the useful part.","可选。留空让 Codex 自己判断怎么用。"));note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        note.setMinLines(3);note.setMaxLines(6);note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15000)});note.setText(draft);Ui.styleInput(note);
        column.addView(note,Ui.margins(this,8,10));
        gauge=null;gaugeText=null;panel=null;panelOpen=false;
        if(store.models().length()>0){
            LinearLayout gaugeRow=Ui.row(this);gaugeRow.setClickable(true);gaugeRow.setFocusable(true);gaugeRow.setOnClickListener(v->togglePanel());Ui.bindPress(gaugeRow);
            gauge=Ui.iconButton(this,R.drawable.ic_gauge,L.t("Model & effort","模型强度"));gauge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);gauge.setFocusable(false);gauge.setOnClickListener(v->togglePanel());
            gaugeRow.addView(gauge,Ui.square(this,48));Ui.space(gaugeRow,10);
            gaugeText=Ui.caption(this,"");gaugeRow.addView(gaugeText,Ui.grow());
            column.addView(gaugeRow,Ui.fill());
            panel=Ui.vertical(this);panel.setVisibility(View.GONE);column.addView(panel,Ui.margins(this,4,0));
            updateGauge();
        }
        Button send=Ui.button(this,L.t("Hand off to Codex","交给 Codex"),true);send.setOnClickListener(v->{if(submit())go(2,1);});column.addView(send,Ui.margins(this,16,0));
        TextView hint=Ui.caption(this,L.t("A note is optional","留言可以留空"));hint.setGravity(Gravity.CENTER);column.addView(hint,Ui.margins(this,4,0));
        return column;
    }
    void updateGauge(){
        if(gaugeText==null)return;JSONObject chosen=store.model(model);String summary=(chosen==null?model:chosen.optString("displayName",model))+(effort.isEmpty()?"":" · "+effort);
        gaugeText.setText(summary);gaugeText.setTextColor(panelOpen?Ui.TEXT:Ui.MUTED);gauge.setImageTintList(ColorStateList.valueOf(panelOpen?Ui.ACCENT:Ui.TEXT));
        ((View)gaugeText.getParent()).setContentDescription(L.t("Model & effort, ","模型强度，")+summary+(panelOpen?L.t(", tap to collapse","，点按收起"):L.t(", tap to expand","，点按展开")));
    }
    void togglePanel(){if(panel==null)return;panelOpen=!panelOpen;if(panelOpen)renderPanel();reveal(panelOpen);updateGauge();}
    /** Expands or collapses the model panel with height and alpha together, so the button beneath slides instead of jumping. */
    void reveal(boolean show){
        if(panelAnimator!=null)panelAnimator.cancel();
        ViewGroup.LayoutParams params=panel.getLayoutParams();
        if(!Ui.motionEnabled(this)){params.height=-2;panel.setAlpha(1f);panel.setVisibility(show?View.VISIBLE:View.GONE);panel.requestLayout();return;}
        View parent=(View)panel.getParent();int width=Math.max(0,parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight());
        panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int from=show?0:panel.getHeight(),to=show?panel.getMeasuredHeight():0;
        if(show){panel.setVisibility(View.VISIBLE);panel.setAlpha(0f);}
        ValueAnimator animator=ValueAnimator.ofInt(from,to);animator.setDuration(show?220:160);animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a->{params.height=(int)a.getAnimatedValue();panel.setAlpha(show?a.getAnimatedFraction():1f-a.getAnimatedFraction());panel.requestLayout();});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){params.height=-2;panel.setAlpha(1f);panel.setVisibility(show?View.VISIBLE:View.GONE);panel.requestLayout();panelAnimator=null;}});
        panelAnimator=animator;animator.start();
    }
    void renderPanel(){
        panel.removeAllViews();JSONArray catalog=store.models();
        TextView modelLabel=Ui.label(this,L.t("Model","模型"));modelLabel.setPadding(0,dp(6),0,dp(6));panel.addView(modelLabel);
        for(int n=0;n<catalog.length();n++){
            JSONObject m=catalog.optJSONObject(n);if(m==null)continue;String id=m.optString("id");
            LinearLayout row=Ui.optionRow(this,m.optString("displayName",id),m.optBoolean("isDefault")?L.t("Default on your computer","电脑上的默认模型"):null,id.equals(model));
            row.setOnClickListener(v->{model=id;effort=store.defaultEffort(id);renderPanel();updateGauge();});panel.addView(row,Ui.margins(this,0,6));
        }
        JSONObject chosen=store.model(model);JSONArray efforts=chosen==null?null:chosen.optJSONArray("efforts");
        if(efforts==null||efforts.length()==0)return;
        List<String> labels=new ArrayList<>();for(int n=0;n<efforts.length();n++)labels.add(efforts.optString(n));
        panel.addView(Ui.label(this,L.t("Reasoning effort","推理强度")));
        panel.addView(Ui.segmented(this,labels,labels.indexOf(effort),index->{effort=labels.get(index);renderPanel();updateGauge();}),Ui.fill());
        StringBuilder hint=new StringBuilder();
        for(String label:labels){String meaning=effortHint(label);if(meaning.isEmpty())continue;if(hint.length()>0)hint.append(" · ");hint.append(label).append(' ').append(meaning);}
        if(hint.length()>0)panel.addView(Ui.caption(this,hint.toString()),Ui.margins(this,6,0));
    }
    static String effortHint(String id){
        return switch(id){case "minimal"->L.t("Fastest","最快");case "low"->L.t("Fast","快");case "medium"->L.t("Balanced","均衡");case "high"->L.t("Thorough","深入");case "xhigh"->L.t("Most thorough","最深");default->"";};
    }
    /** Writes the task to the outbox and starts delivery; the step 3 animation only plays once this has succeeded. */
    boolean submit(){
        if(step!=1||sent)return false;
        String material=shared.trim();JSONObject project=store.project(selected);
        if(project==null){selected="";error(new IOException(L.t("The project list changed. Choose a project again.","项目列表已变化，请重新选择")));go(0,-1);return false;}
        if(!Store.projectEnabled(project)){go(0,-1);authorize(project);return false;}
        try{
            String message=note.getText().toString();
            String title=TaskPresentation.clip(message.isEmpty()?material:message,80);
            JSONObject task=new JSONObject().put("id",UUID.randomUUID().toString()).put("projectId",selected).put("projectName",project.optString("name")).put("content",material).put("message",message).put("title",title).put("localFiles",attachments).put("assets",new JSONArray());
            if(!model.isEmpty())task.put("model",model);if(!effort.isEmpty())task.put("effort",effort);
            store.save(task);sent=true;store.prefs.edit().putString("lastProject",selected).apply();
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
        badge=new TextView(this);badge.setText("Codex");badge.setTextSize(11);badge.setTypeface(Ui.medium());badge.setTextColor(Ui.TEXT);badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.circle(this,Ui.SURFACE_2,Ui.LINE_STRONG));badge.setContentDescription(L.t("Codex on your computer","电脑上的 Codex"));flight.addView(badge,Ui.square(this,48));
        column.addView(flight,Ui.margins(this,6,2));
        status=Ui.text(this,outcome(),14,Ui.TEXT);status.setGravity(Gravity.CENTER);status.setVisibility(View.INVISIBLE);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        column.addView(status,Ui.margins(this,2,0));
        return column;
    }
    String outcome(){
        if(!store.online())return L.t("You're offline. Saved safely; it will send when connected.","手机离线，已保存。联网后自动发送。");
        String mode=store.directExecution()?L.t("Codex will read the available material and work on your request.","Codex 收到后会读取可获取的材料，并直接处理需求。"):L.t("Codex will read the available material and prepare a plan for your approval.","Codex 收到后会读取可获取的材料，先给方案，等你批准后执行。");
        String updates=TaskNotifications.allowed(this)?L.t("We'll notify you when results or decisions are ready. You can also check DropRun or Codex.","结果或需确认时会通知你，也可在 App 或 Codex 查看。"):L.t("Notifications are off. Check DropRun or Codex for updates.","通知未开启，请在 App 或 Codex 查看后续。");
        return mode+" "+updates;
    }
    void fly(){
        handler.postDelayed(()->{if(!gone())plane.play(360,this::landed);},50);
    }
    void landed(){
        if(gone())return;
        sendTitle.setText(L.t("Saved. We'll take it from here.","已保存，自动发送"));
        badge.setTextColor(Ui.ACCENT);badge.setBackground(Ui.circle(this,Ui.LIME_SOFT,Ui.LIME_LINE));Ui.pulse(badge);
        status.setVisibility(View.VISIBLE);Ui.fadeIn(status,220);
        int timeout=Ui.motionEnabled(this)?280:100;android.view.accessibility.AccessibilityManager accessibility=(android.view.accessibility.AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if(Build.VERSION.SDK_INT>=29)timeout=accessibility.getRecommendedTimeoutMillis(timeout,android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT|android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS);
        else if(accessibility.isTouchExplorationEnabled())timeout=8000;
        handler.postDelayed(this::close,timeout);
    }
}
