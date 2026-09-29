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
    Store store;String taskId,snapshot="";LinearLayout body;TextView notice;boolean foreground,busy,loading,thumbnailRequested;
    android.graphics.Bitmap thumbnail;String thumbnailError="";final java.util.Set<String> expanded=new java.util.HashSet<>();
    AlertDialog followupDialog,actionErrorDialog;EditText followupInput;String followupDraft="",followupId=UUID.randomUUID().toString();
    final Runnable refresh=this::load;
    interface Work { void run() throws Exception; }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);taskId=getIntent().getStringExtra("taskId");
        if(state!=null){followupDraft=state.getString("followupDraft","");followupId=state.getString("followupId",followupId);java.util.ArrayList<String> sections=state.getStringArrayList("expanded");if(sections!=null)expanded.addAll(sections);}
        if(taskId==null||!taskId.matches("[a-zA-Z0-9-]{20,64}")){finish();return;}
        Ui.configureWindow(this);LinearLayout page=Ui.page(this);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back to history","返回历史"));back.setOnClickListener(v->finish());
        Ui.topBar(this,page,back,L.t("Handoff","交办"),false,null);
        notice=Ui.text(this,"",13,Ui.AMBER);notice.setVisibility(View.GONE);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(notice);
        body=Ui.vertical(this);page.addView(body,Ui.fill());render();Ui.enter(body);
        if(state!=null&&state.getBoolean("followupOpen"))followup();
    }
    @Override protected void onResume(){super.onResume();foreground=true;if(store!=null)load();}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(refresh);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putStringArrayList("expanded",new java.util.ArrayList<>(expanded));state.putString("followupId",followupId);state.putString("followupDraft",followupInput==null?followupDraft:followupInput.getText().toString());if(followupDialog!=null&&followupDialog.isShowing())state.putBoolean("followupOpen",true);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(followupDialog!=null)followupDialog.dismiss();if(actionErrorDialog!=null)actionErrorDialog.dismiss();io.shutdown();super.onDestroy();}
    void load(){
        handler.removeCallbacks(refresh);if(!foreground||busy||loading)return;loading=true;
        io.execute(()->{
            String error="";try{store.refreshTask(taskId);}catch(Exception e){error=e.getMessage();}
            String message=error;
            runOnUiThread(()->{loading=false;if(isDestroyed()||!foreground)return;notice.setText(message);notice.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);render();handler.postDelayed(refresh,5000);});
        });
    }
    String text(JSONObject task,String key){return task.isNull(key)?"":task.optString(key);}
    void block(String heading,String value){
        if(value.isEmpty())return;body.addView(Ui.label(this,heading));
        TextView content=Ui.text(this,ReportText.render(value),15,Ui.TEXT);content.setTextIsSelectable(true);
        body.addView(content,Ui.margins(this,0,16));
    }
    void button(String label,boolean primary,Runnable action){
        Button button=Ui.button(this,label,primary);button.setEnabled(!busy);button.setOnClickListener(v->action.run());body.addView(button,Ui.margins(this,8,0));
    }
    void disclosure(String heading,String value){
        if(value.isEmpty())return;
        TextView content=Ui.text(this,ReportText.render(value),15,Ui.TEXT);content.setTextIsSelectable(true);content.setPadding(0,0,0,Ui.dp(this,16));
        LinearLayout group=Ui.disclosure(this,heading,content,expanded.contains(heading),open->{if(open)expanded.add(heading);else expanded.remove(heading);});
        body.addView(group,Ui.margins(this,8,0));
    }
    void render(){
        JSONObject task=store.task(taskId);long now=System.currentTimeMillis();int liveApprovals=0;
        JSONArray approvals=task==null?null:task.optJSONArray("approvals");
        for(int n=0;approvals!=null&&n<approvals.length();n++){JSONObject approval=approvals.optJSONObject(n);if(approval!=null&&approval.optLong("expiresAt")>now&&approval.optJSONObject("details")!=null)liveApprovals++;}
        String previewState=task==null?"":TaskPresentation.previewStatus(text(task,"preview_status"),text(task,"preview_url"),task.optLong("preview_expires_at"),now);
        String next=(task==null?"missing":task.toString())+busy+(thumbnail!=null)+thumbnailError+liveApprovals+previewState;
        if(snapshot.equals(next))return;snapshot=next;body.removeAllViews();
        if(task==null){block(L.t("Handoff unavailable","任务暂不可用"),L.t("Refresh when connected. This handoff may have been deleted.","请联网刷新；任务也可能已被删除。"));return;}
        String status=text(task,"status"),plan=text(task,"plan_report"),report=text(task,"report");
        body.addView(Ui.title(this,MainActivity.name(task),24));
        LinearLayout statusLine=Ui.row(this);statusLine.addView(Ui.pill(this,TaskPresentation.status(status),TaskPresentation.statusColor(status)));body.addView(statusLine,Ui.margins(this,8,8));
        body.addView(Ui.caption(this,text(task,"project_name")+" · "+TaskPresentation.mode(text(task,"execution_mode"))));
        body.addView(Ui.caption(this,L.t("Shared ","交办于 ")+TaskPresentation.elapsed(task.optLong("created_at"),System.currentTimeMillis())));
        block(L.t("Needs attention","需要处理"),text(task,"error"));
        if(!plan.isEmpty()){
            if(status.equals("awaiting_plan_approval"))block(L.t("Understanding & plan","理解与计划"),plan);else disclosure(L.t("Earlier plan","之前的计划"),plan);
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
        if(report.isEmpty())block(L.t("What's happening","当前进展"),status.equals("waiting_for_approval")&&liveApprovals==0?L.t("This command request expired or is no longer available. Reconnect to refresh the task's status.","这条命令请求已过期或失效。请联网查看任务的最新状态。"):TaskPresentation.noReport(status,!plan.isEmpty()));
        else {LinearLayout outcome=Ui.card(this);TextView label=Ui.label(this,L.t("THE RESULT","交付结果"));label.setPadding(0,0,0,Ui.dp(this,8));outcome.addView(label);String summary=TaskPresentation.resultSummary(report);outcome.addView(Ui.text(this,summary,16,Ui.TEXT));body.addView(outcome,Ui.margins(this,18,6));}
        if(Store.finished(status)){
            if(thumbnail!=null){ImageView picture=new ImageView(this);picture.setImageBitmap(thumbnail);picture.setAdjustViewBounds(true);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);picture.setContentDescription(L.t("Verified screenshot from this handoff. Open all delivery files.","本次交办的已校验截图。打开全部交付文件。"));picture.setBackground(Ui.surface(this,Ui.SURFACE));picture.setClipToOutline(true);picture.setFocusable(true);picture.setOnClickListener(v->openDeliverables());Ui.bindPress(picture);body.addView(picture,Ui.margins(this,14,8));}
            else if(!thumbnailError.isEmpty())body.addView(Ui.caption(this,thumbnailError),Ui.margins(this,10,0));
            if(!thumbnailRequested)loadThumbnail();
        }
        preview(task);
        if(!report.isEmpty()||Store.finished(status))button(L.t("Screenshots & delivery files","截图与交付文件"),false,this::openDeliverables);
        if(Store.finished(status)&&!text(task,"thread_id").isEmpty())button(L.t("Follow up","继续追问"),true,this::followup);
        if(!report.isEmpty())disclosure(L.t("Full report & evidence","完整报告与证据"),report);
        disclosure(L.t("Your note","你的留言"),text(task,"message"));disclosure(L.t("Original material","原始材料"),text(task,"content"));
        if(!Store.finished(status))button(L.t("Stop handoff","停止任务"),false,()->confirm(L.t("Stop this handoff?","停止任务？"),L.t("A stop request will be sent. Changes already made to your project will not be undone.","发送停止请求；已发生的项目改动不会回滚。"),()->store.cancelTask(taskId)));
        else button(L.t("Delete record & material","删除记录与材料"),false,()->confirm(L.t("Delete this handoff?","删除这条任务？"),L.t("The Relay task, material and report will be deleted permanently. Your project files stay on your computer.","云端任务、材料与报告将删除，无法撤销。电脑上的项目文件不变。"),()->{store.deleteTask(taskId);runOnUiThread(this::finish);}));
    }
    void openDeliverables(){startActivity(new Intent(this,DeliverablesActivity.class).putExtra("taskId",taskId));}
    void loadThumbnail(){
        thumbnailRequested=true;io.execute(()->{android.graphics.Bitmap bitmap=null;String error="";
            try{JSONArray items=store.get("/tasks/"+taskId+"/deliverables").getJSONArray("deliverables");for(int n=0;n<items.length();n++){JSONObject item=items.getJSONObject(n);if(!item.optString("name").toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpg|jpeg|webp)$"))continue;java.io.File file=store.downloadDeliverable(taskId,item);try{android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(file.getPath(),options);options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1200||options.outHeight/options.inSampleSize>1600)options.inSampleSize*=2;options.inJustDecodeBounds=false;bitmap=android.graphics.BitmapFactory.decodeFile(file.getPath(),options);}finally{file.delete();}break;}}
            catch(Exception e){error=L.t("Screenshot unavailable right now. Open delivery files to retry.","截图暂不可用，可打开交付文件重试。");}
            android.graphics.Bitmap image=bitmap;String message=error;runOnUiThread(()->{if(isDestroyed()){if(image!=null)image.recycle();return;}thumbnail=image;thumbnailError=message;snapshot="";render();});
        });
    }
    void preview(JSONObject task){
        String url=text(task,"preview_url"),state=TaskPresentation.previewStatus(text(task,"preview_status"),url,task.optLong("preview_expires_at"),System.currentTimeMillis());
        if(state.isEmpty())return;
        block(L.t("Preview","预览"),TaskPresentation.snapshot(text(task,"preview_kind"))?L.t("Snapshot from this handoff · ","本次交付快照 · ")+text(task,"preview_version"):L.t("Live project preview. Later changes may alter what you see.","当前项目预览；后续修改可能改变内容。"));
        if(state.equals("ready"))button(L.t("Open preview","打开预览"),true,()->{
            Uri uri=Uri.parse(url);
            if("https".equals(uri.getScheme())&&uri.getHost()!=null)startActivity(new Intent(Intent.ACTION_VIEW,uri));
            else notice(L.t("Invalid preview address.","预览地址无效。"));
        });
        else if(state.equals("reopening"))block(L.t("Preview status","预览状态"),TaskPresentation.snapshot(text(task,"preview_kind"))?L.t("Renewing the saved snapshot link.","正在更新已保存快照的链接。"):L.t("Reopening. Waiting for your computer to provide a new address.","正在重开，等待电脑返回新地址。"));
        else {block(L.t("Preview status","预览状态"),L.t("This preview expired or stopped.","预览已失效或停止。"));if(Store.finished(text(task,"status"))&&!url.isEmpty()&&task.optInt("cancel_requested")==0)button(L.t("Reopen preview","重开预览"),false,()->perform(()->{store.reopenPreview(taskId);TaskSyncService.start(this);}));}
    }
    void confirm(String title,String message,Work work){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Confirm","确认"),(d,w)->perform(work)).show();}
    void notice(String message){notice.setText(message);notice.setVisibility(View.VISIBLE);}
    void perform(Work work){
        if(busy)return;busy=true;notice(L.t("Saving your decision…","正在保存你的决定…"));render();
        io.execute(()->{String error="";try{work.run();}catch(Exception e){error=e.getMessage();}String message=error;
            runOnUiThread(()->{if(isDestroyed())return;busy=false;notice(message.isEmpty()?L.t("Saved.","已保存。"):message);if(message.isEmpty())Toast.makeText(this,L.t("Decision saved","决定已保存"),Toast.LENGTH_SHORT).show();else actionErrorDialog=new AlertDialog.Builder(this).setTitle(L.t("Could not confirm this action","暂时无法确认操作结果")).setMessage(message).setPositiveButton(L.t("Got it","知道了"),null).show();render();if(foreground)load();});});
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
