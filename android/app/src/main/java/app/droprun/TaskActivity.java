package app.droprun;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.UUID;
import java.util.concurrent.*;

/** A task's full feedback and decisions, separate from the compact home history. */
public class TaskActivity extends StyledActivity {
    final ExecutorService io=Executors.newSingleThreadExecutor();
    final Handler handler=new Handler(Looper.getMainLooper());
    Store store;String taskId,snapshot="",actionFailure="";LinearLayout body;TextView notice,sharedAt;boolean foreground,busy,loading,thumbnailRequested;
    android.graphics.Bitmap thumbnail;String thumbnailError="";final java.util.Set<String> expanded=new java.util.HashSet<>();
    AlertDialog followupDialog,actionErrorDialog,localRemovalDialog;EditText followupInput;String followupDraft="",followupId=UUID.randomUUID().toString();
    final Runnable refresh=this::load;
    interface Work { void run() throws Exception; }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=createStore();taskId=getIntent().getStringExtra("taskId");
        if(state!=null){actionFailure=state.getString("actionFailure","");followupDraft=state.getString("followupDraft","");followupId=state.getString("followupId",followupId);java.util.ArrayList<String> sections=state.getStringArrayList("expanded");if(sections!=null)expanded.addAll(sections);}
        if(taskId==null||!taskId.matches("[a-zA-Z0-9-]{20,64}")){finish();return;}
        Ui.configureWindow(this);LinearLayout page=Ui.page(this);((View)page.getParent()).setId(R.id.task_scroll);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back to history","返回历史"));back.setOnClickListener(v->finish());
        Ui.topBar(this,page,back,L.t("Handoff","交办"),false,null);
        notice=Ui.text(this,"",13,Ui.AMBER);notice.setVisibility(View.GONE);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(notice);
        body=Ui.vertical(this);page.addView(body,Ui.fill());readNotice("");render();Ui.enter(body);
        if(state!=null&&state.getBoolean("followupOpen"))followup();
        if(state!=null&&state.getBoolean("localRemovalOpen")&&store.canClearUnavailableTask(taskId))confirmLocalRemoval();
    }
    Store createStore(){return new Store(this);}
    @Override protected void onResume(){super.onResume();foreground=true;if(store!=null)load();}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("actionFailure",actionFailure);state.putStringArrayList("expanded",new java.util.ArrayList<>(expanded));state.putString("followupId",followupId);state.putString("followupDraft",followupInput==null?followupDraft:followupInput.getText().toString());if(followupDialog!=null&&followupDialog.isShowing())state.putBoolean("followupOpen",true);if(localRemovalDialog!=null&&localRemovalDialog.isShowing())state.putBoolean("localRemovalOpen",true);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(followupDialog!=null)followupDialog.dismiss();if(actionErrorDialog!=null)actionErrorDialog.dismiss();if(localRemovalDialog!=null)localRemovalDialog.dismiss();io.shutdown();super.onDestroy();}
    void load(){
        handler.removeCallbacks(refresh);if(!foreground||busy||loading)return;loading=true;
        io.execute(()->{
            String error="";try{store.refreshTask(taskId);}catch(Exception e){String detail=e.getMessage();error=detail==null||detail.trim().isEmpty()?L.t("Could not refresh this handoff. We'll try again shortly.","暂时无法刷新这条交办，稍后会自动重试。"):detail;}
            String message=error;
            runOnUiThread(()->{loading=false;if(isDestroyed()||!foreground)return;readNotice(message);render();handler.postDelayed(refresh,5000);});
        });
    }
    String text(JSONObject task,String key){return task.isNull(key)?"":task.optString(key);}
    void block(String heading,String value){block(body,heading,value);}
    void block(LinearLayout target,String heading,String value){
        if(value.isEmpty())return;target.addView(Ui.label(this,heading));
        TextView content=Ui.text(this,ReportText.render(value),15,Ui.TEXT);content.setTextIsSelectable(true);
        target.addView(content,Ui.margins(this,0,16));
    }
    void button(String label,boolean primary,Runnable action){button(body,label,primary,action);}
    void button(LinearLayout target,String label,boolean primary,Runnable action){
        Button button=Ui.button(this,label,primary);
        if(label.equals(L.t("Delete record & material","删除记录与材料"))||label.equals(L.t("Stop handoff","停止任务")))Ui.styleDanger(button);
        button.setEnabled(!busy);button.setOnClickListener(v->action.run());target.addView(button,Ui.margins(this,8,0));
    }
    void disclosure(String heading,String value){
        if(value.isEmpty())return;
        TextView content=Ui.text(this,ReportText.render(value),15,Ui.TEXT);content.setTextIsSelectable(true);content.setPadding(0,0,0,Ui.dp(this,16));
        LinearLayout group=Ui.disclosure(this,heading,content,expanded.contains(heading),open->{if(open)expanded.add(heading);else expanded.remove(heading);});
        group.getChildAt(0).setPadding(0,0,0,0);
        body.addView(group,Ui.margins(this,8,0));
    }
    String taskViewSnapshot(JSONObject task){
        if(task==null)return "missing";String raw=task.toString();
        try{JSONObject view=new JSONObject(raw);view.remove("updated_at");return view.toString();}
        catch(JSONException failure){return raw;}
    }
    void render(){
        JSONObject task=store.task(taskId);long now=System.currentTimeMillis();int liveApprovals=0;
        JSONArray approvals=task==null?null:task.optJSONArray("approvals");
        for(int n=0;approvals!=null&&n<approvals.length();n++){JSONObject approval=approvals.optJSONObject(n);if(approval!=null&&approval.optLong("expiresAt")>now&&approval.optJSONObject("details")!=null)liveApprovals++;}
        String previewState=task==null?"":TaskPresentation.previewStatus(text(task,"preview_status"),text(task,"preview_url"),task.optLong("preview_expires_at"),now);
        String projectLabel=task==null?"":store.projectLabel(text(task,"project_id"),text(task,"project_name"));
        boolean unavailableDelete=store.canClearUnavailableTask(taskId);
        String sharedText=task==null?"":L.t("Shared ","交办于 ")+TaskPresentation.elapsed(task.optLong("created_at"),now);
        if(sharedAt!=null&&!sharedText.contentEquals(sharedAt.getText()))sharedAt.setText(sharedText);
        String next=taskViewSnapshot(task)+busy+(thumbnail!=null)+thumbnailError+liveApprovals+previewState+projectLabel+unavailableDelete;
        if(snapshot.equals(next))return;snapshot=next;body.removeAllViews();sharedAt=null;
        if(task==null){block(L.t("Handoff unavailable","任务暂不可用"),L.t("Refresh when connected. This handoff may have been deleted.","请联网刷新；任务也可能已被删除。"));return;}
        String status=text(task,"status"),plan=text(task,"plan_report"),report=text(task,"report");
        body.addView(Ui.title(this,MainActivity.name(task),report.isEmpty()?24:20));
        body.addView(Ui.caption(this,projectLabel+" · "+TaskPresentation.mode(text(task,"execution_mode"))),Ui.margins(this,6,0));
        boolean largeText=getResources().getConfiguration().fontScale>=1.5f;
        LinearLayout statusLine=largeText?Ui.vertical(this):Ui.row(this);statusLine.addView(Ui.pill(this,TaskPresentation.status(status),TaskPresentation.statusColor(status)),new LinearLayout.LayoutParams(-2,-2));
        sharedAt=Ui.caption(this,sharedText);sharedAt.setGravity(largeText?android.view.Gravity.START:android.view.Gravity.END);
        if(largeText)statusLine.addView(sharedAt,Ui.margins(this,6,0));else{Ui.space(statusLine,12);statusLine.addView(sharedAt,Ui.grow());}body.addView(statusLine,Ui.margins(this,12,6));
        block(L.t("Needs attention","需要处理"),text(task,"error"));
        if(unavailableDelete)block(L.t("Relay record inaccessible","无法访问中转记录"),L.t("Cloud deletion could not be confirmed. You can clear the phone's cached copy. The record may return if your Relay makes it available again.","尚未确认云端已删除。可以清除手机上的缓存副本；若中转服务再次提供此记录，它可能重新出现。"));
        if(!plan.isEmpty()){
            if(status.equals("awaiting_plan_approval"))block(L.t("Understanding & plan","理解与计划"),plan);else if(report.isEmpty())disclosure(L.t("Earlier plan","之前的计划"),plan);
            if(status.equals("awaiting_plan_approval")&&!text(task,"plan_version").isEmpty()){
                String version=text(task,"plan_version");
                button(L.t("Approve this plan","批准这个计划"),true,()->confirm(L.t("Approve this plan?","批准这个计划？"),L.t("Codex will edit the original project and run commands according to this plan.","Codex 将按当前计划修改原项目并运行命令。"),()->store.decidePlan(taskId,version,true)));
                button(L.t("Reject plan","拒绝计划"),false,()->confirm(L.t("Reject this plan?","拒绝这个计划？"),L.t("This handoff will not proceed to execution.","本次任务不会进入执行。"),()->store.decidePlan(taskId,version,false)));
            }
        }
        for(int n=0;approvals!=null&&n<approvals.length();n++){
            JSONObject approval=approvals.optJSONObject(n);if(approval==null||approval.optLong("expiresAt")<=now)continue;
            JSONObject details=approval.optJSONObject("details");if(details==null)continue;
            String id=approval.optString("id"),command=text(details,"command");
            block(L.t("Command approval","命令审批"),command+"\n"+text(details,"cwd")+"\n"+text(details,"reason"));
            button(L.t("Allow this command","允许这条命令"),true,()->confirm(L.t("Allow this command?","允许这条命令？"),command,()->store.decideApproval(taskId,id,true)));
            button(L.t("Deny this command","拒绝这条命令"),false,()->perform(()->store.decideApproval(taskId,id,false)));
        }
        LinearLayout delivery=body;
        if(report.isEmpty())block(L.t("What's happening","当前进展"),status.equals("waiting_for_approval")&&liveApprovals==0?L.t("This command request expired or is no longer available. Reconnect to refresh the task's status.","这条命令请求已过期或失效。请联网查看任务的最新状态。"):TaskPresentation.noReport(status,!plan.isEmpty()));
        else {
            delivery=Ui.vertical(this);TextView label=Ui.title(this,L.t("The result","交付结果"),14);label.setTextColor(Ui.MUTED);label.setPadding(0,0,0,Ui.dp(this,8));delivery.addView(label);
            CharSequence summary=ReportText.render(TaskPresentation.resultSummary(report));
            if(summary.length()>0&&summary.charAt(summary.length()-1)=='\n')summary=summary.subSequence(0,summary.length()-1);
            delivery.addView(Ui.text(this,summary,22,Ui.TEXT));body.addView(delivery,Ui.margins(this,12,0));
        }
        if(Store.finished(status)){
            if(thumbnail!=null){
                ImageView picture=new ImageView(this);picture.setImageBitmap(thumbnail);picture.setAdjustViewBounds(true);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                FrameLayout imageAction=new FrameLayout(this);imageAction.setMinimumHeight(Ui.dp(this,48));imageAction.setBackground(Ui.surface(this,Ui.SURFACE));imageAction.setClipToOutline(true);
                imageAction.setContentDescription(L.t("Verified screenshot from this handoff. Open all delivery files.","本次交办的已校验截图。打开全部交付文件。"));imageAction.setEnabled(!busy);imageAction.setFocusable(!busy);imageAction.setOnClickListener(v->{if(!busy)openDeliverables();});Ui.bindPress(imageAction);
                imageAction.addView(picture,new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER));delivery.addView(imageAction,Ui.margins(this,14,8));
            }
            else if(!thumbnailError.isEmpty())delivery.addView(Ui.caption(this,thumbnailError),Ui.margins(this,10,0));
            if(!thumbnailRequested)loadThumbnail();
        }
        LinearLayout artifacts=delivery;
        if(!report.isEmpty()){
            artifacts=Ui.vertical(this);artifacts.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));artifacts.setBackground(Ui.outlined(this,Ui.SURFACE,0,Ui.RADIUS_CARD,0));delivery.addView(artifacts,Ui.margins(this,12,0));
        }
        preview(task,artifacts);
        if(artifacts!=delivery&&artifacts.getChildCount()>0)artifacts.getChildAt(0).setPadding(0,0,0,Ui.dp(this,6));
        if(!report.isEmpty()||Store.finished(status))button(artifacts,L.t("Screenshots & delivery files","截图与交付文件"),Store.finished(status)&&!previewState.equals("ready"),this::openDeliverables);
        if(Store.finished(status)&&!text(task,"thread_id").isEmpty())button(L.t("Follow up","继续追问"),false,this::followup);
        if(TaskPresentation.snapshot(text(task,"preview_kind"))&&!previewState.isEmpty()&&!previewState.equals("unavailable"))disclosure(L.t("Snapshot version","快照版本"),text(task,"preview_version"));
        if(!report.isEmpty())disclosure(L.t("Full report & evidence","完整报告与证据"),report);
        if(!report.isEmpty()&&!plan.isEmpty()&&!status.equals("awaiting_plan_approval"))disclosure(L.t("Earlier plan","之前的计划"),plan);
        disclosure(L.t("Your note","你的留言"),text(task,"message"));disclosure(L.t("Original material","原始材料"),text(task,"content"));
        if(!Store.finished(status))button(L.t("Stop handoff","停止任务"),false,()->confirm(L.t("Stop this handoff?","停止任务？"),L.t("A stop request will be sent. Changes already made to your project will not be undone.","发送停止请求；已发生的项目改动不会回滚。"),()->store.cancelTask(taskId)));
        else button(L.t("Delete record & material","删除记录与材料"),false,()->confirm(L.t("Delete this handoff?","删除这条任务？"),L.t("The Relay task, material and report will be deleted permanently. Your project files stay on your computer.","云端任务、材料与报告将删除，无法撤销。电脑上的项目文件不变。"),()->{store.deleteTask(taskId);runOnUiThread(this::finish);}));
        if(unavailableDelete)button(L.t("Clear cached copy on this phone","清除这台手机上的缓存副本"),false,this::confirmLocalRemoval);
    }
    void confirmLocalRemoval(){
        if(busy||!store.canClearUnavailableTask(taskId)||localRemovalDialog!=null&&localRemovalDialog.isShowing())return;
        localRemovalDialog=new AlertDialog.Builder(this).setTitle(L.t("Clear this phone's cached copy?","清除这台手机上的缓存副本？")).setMessage(L.t("Cloud deletion could not be confirmed. This clears the phone's cached report, history entry and retry copy. It sends no deletion request. The record may return if your Relay makes it available again.","尚未确认云端已删除。这会清除手机缓存的报告、历史记录和重试副本，不会发送删除请求。若中转服务再次提供此记录，它可能重新出现。" )).setNegativeButton(L.t("Keep cached copy","保留缓存副本"),null).setPositiveButton(L.t("Clear cached copy","清除缓存副本"),(dialog,which)->perform(()->{store.clearUnavailableTask(taskId);runOnUiThread(this::finish);})).show();
    }
    void openDeliverables(){startActivity(new Intent(this,DeliverablesActivity.class).putExtra("taskId",taskId));}
    void loadThumbnail(){
        thumbnailRequested=true;io.execute(()->{android.graphics.Bitmap bitmap=null;String error="";
            try{JSONArray items=store.get("/tasks/"+taskId+"/deliverables").getJSONArray("deliverables");for(int n=0;n<items.length();n++){JSONObject item=items.getJSONObject(n);if(!item.optString("name").toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpg|jpeg|webp)$"))continue;java.io.File file=store.downloadDeliverable(taskId,item);try{android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(file.getPath(),options);options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1200||options.outHeight/options.inSampleSize>1600)options.inSampleSize*=2;options.inJustDecodeBounds=false;bitmap=android.graphics.BitmapFactory.decodeFile(file.getPath(),options);}finally{file.delete();}break;}}
            catch(Exception e){error=L.t("Screenshot unavailable right now. Open delivery files to retry.","截图暂不可用，可打开交付文件重试。");}
            android.graphics.Bitmap image=bitmap;String message=error;runOnUiThread(()->{if(isDestroyed()){if(image!=null)image.recycle();return;}thumbnail=image;thumbnailError=message;snapshot="";render();});
        });
    }
    void preview(JSONObject task,LinearLayout target){
        String url=text(task,"preview_url"),state=TaskPresentation.previewStatus(text(task,"preview_status"),url,task.optLong("preview_expires_at"),System.currentTimeMillis());
        if(state.isEmpty()){if(Store.finished(text(task,"status")))block(target,L.t("Preview","预览"),L.t("No preview is attached to this handoff. Check screenshots and delivery files below.","本次交办未附预览，可查看下方的截图与交付文件。"));return;}
        if(state.equals("unavailable"))block(target,L.t("Preview","预览"),Store.finished(text(task,"status"))||!text(task,"report").isEmpty()?L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。"):L.t("A preview isn't available right now.","预览暂不可用。"));
        else {
            String description=TaskPresentation.snapshot(text(task,"preview_kind"))?L.t("Fixed snapshot from this handoff.","本次交付的固定快照。"):L.t("Live project preview. Later changes may alter what you see.","当前项目预览；后续修改可能改变内容。");
            if(state.equals("ready")){
                target.addView(Ui.label(this,L.t("Preview","预览")));TextView explanation=Ui.text(this,description,15,Ui.TEXT);explanation.setTextIsSelectable(true);target.addView(explanation,Ui.margins(this,0,8));
            }else block(target,L.t("Preview","预览"),description);
        }
        if(state.equals("ready"))button(target,L.t("Open preview","打开预览"),true,()->{
            Uri uri=Uri.parse(url);
            if("https".equals(uri.getScheme())&&uri.getHost()!=null)startActivity(new Intent(Intent.ACTION_VIEW,uri));
            else notice(L.t("Invalid preview address.","预览地址无效。"));
        });
        else if(state.equals("reopening"))block(target,L.t("Preview status","预览状态"),TaskPresentation.snapshot(text(task,"preview_kind"))?L.t("Renewing the saved snapshot link.","正在更新已保存快照的链接。"):L.t("Reopening. Waiting for your computer to provide a new address.","正在重开，等待电脑返回新地址。"));
        else {if(!state.equals("unavailable"))block(target,L.t("Preview status","预览状态"),L.t("This preview expired or stopped.","预览已失效或停止。"));if(Store.finished(text(task,"status"))&&!url.isEmpty()&&task.optInt("cancel_requested")==0)button(target,L.t("Reopen preview","重开预览"),false,()->perform(()->{store.reopenPreview(taskId);TaskSyncService.start(this);}));}
    }
    void confirm(String title,String message,Work work){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Confirm","确认"),(d,w)->perform(work)).show();}
    void notice(String message){actionFailure="";showNotice(message);}
    void readNotice(String message){if(busy)return;showNotice(actionFailure.isEmpty()?message:actionFailure);}
    void showNotice(String message){notice.setText(message);notice.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);}
    void perform(Work work){
        if(busy)return;busy=true;notice(L.t("Processing request…","正在处理请求…"));render();
        io.execute(()->{Exception failure=null;try{work.run();}catch(Exception e){failure=e;}boolean failed=failure!=null;String detail=failed?failure.getMessage():null;
            runOnUiThread(()->{if(isDestroyed())return;busy=false;String message=failed?(detail==null||detail.trim().isEmpty()?L.t("Check the handoff's latest status before trying again.","请先查看任务的最新状态，再决定是否重试。"):detail):L.t("Request confirmed.","请求已确认。");actionFailure=failed?message:"";showNotice(message);if(!failed)Toast.makeText(this,L.t("Request confirmed","请求已确认"),Toast.LENGTH_SHORT).show();else actionErrorDialog=new AlertDialog.Builder(this).setTitle(L.t("Could not confirm this action","暂时无法确认操作结果")).setMessage(message).setPositiveButton(L.t("Got it","知道了"),null).show();render();if(foreground)load();});});
    }
    void followup(){
        EditText input=new EditText(this);followupInput=input;Ui.styleInput(input);input.setHint(L.t("Continue this handoff…","继续这个任务…"));input.setMinLines(3);input.setText(followupDraft);input.setSelection(input.length());
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15000)});
        LinearLayout box=Ui.vertical(this);int padding=Ui.dp(this,20);box.setPadding(padding,0,padding,0);box.addView(input);
        box.addView(Ui.caption(this,store.directExecution()?L.t("Continues the same session. Your current setting allows direct execution.","复用原会话，按当前设置直接执行。"):L.t("Continues the same session. Your current setting requires plan approval.","复用原会话，按当前设置先看计划。")));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(L.t("Follow up","继续追问")).setView(box).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Send","发送"),null).create();
        followupDialog=dialog;String id=followupId;
        dialog.setOnDismissListener(d->followupDraft=input.getText().toString());
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String message=input.getText().toString();if(message.trim().isEmpty()){input.setError(L.t("Write a follow-up first.","请填写追问"));return;}
            try{
                JSONObject parent=store.task(taskId);JSONObject next=new JSONObject().put("id",id).put("parentTaskId",taskId).put("projectId",parent.getString("project_id")).put("projectName",parent.optString("project_name")).put("rootTaskId",parent.optString("root_task_id",taskId)).put("message",message).put("assets",new JSONArray()).put("localFiles",new JSONArray());
                String model=store.defaultModel(),effort=store.defaultEffort(model);if(!model.isEmpty())next.put("model",model);if(!effort.isEmpty())next.put("effort",effort);
                store.save(next);input.setText("");followupDraft="";followupId=UUID.randomUUID().toString();SyncJob.soon(this);TaskSyncService.start(this);dialog.dismiss();notice(L.t("Follow-up saved. It will send when connected; the new report will appear in this project's history.","追问已保存，联网后发送；新报告会出现在历史列表。"));Toast.makeText(this,L.t("Follow-up saved","追问已保存"),Toast.LENGTH_SHORT).show();
            }catch(Exception e){input.setError(e.getMessage());}
        }));dialog.show();
    }
}
