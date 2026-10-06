package app.droprun;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Eight opt-in real callback windows, fixed failure only. No store/API/file/OS/business action. */
public class TaskActionFeedbackTest {
    static boolean unresolvedLifetime;
    static final AtomicInteger guardSnapshots=new AtomicInteger();

    @Test public void closedFailuresRestoreRealActionFeedback()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("taskActionFeedbackProbe","");if(phase.isEmpty())return;assertEquals("accepted",phase);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);assertFalse(unresolvedLifetime);
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoTaskFeedbackActivity.class),0).exported);
        String languageBefore=L.chinese()?"zh":"en";boolean destroyed=true;int entered=0,completed=0,closed=0,actualWork=0,actualLoads=0;guardSnapshots.set(0);
        try{for(String[] scene:new String[][]{{"en","light"},{"zh","dark"}})for(DemoTaskFeedbackActivity.FailureCase kind:DemoTaskFeedbackActivity.FailureCase.values()){
            String language=scene[0],theme=scene[1],identity=language+"|"+theme+"|"+kind;assertTrue(destroyed);assertFalse(unresolvedLifetime);
            ActivityScenario<DemoTaskFeedbackActivity> scenario=null;DemoTaskFeedbackActivity[] retained={null};Throwable failure=null;int[] loadBase={0};String[] taskBefore={null};JSONObject[] callback={null};
            try{
                destroyed=false;unresolvedLifetime=true;event(nonce,phase,identity,"launch-attempt",new JSONObject());
                Intent intent=new Intent(target,DemoTaskFeedbackActivity.class).putExtra("taskActionFeedbackOptIn",true).putExtra("taskActionFeedbackProbe",phase).putExtra("evidenceNonce",nonce).putExtra("language",language).putExtra("appearance",theme).putExtra("failureCase",kind.name());
                scenario=ActivityScenario.launch(intent);event(nonce,phase,identity,"returned-handle",new JSONObject());ready(scenario);
                scenario.onActivity(a->{retained[0]=a;safe(a,language,theme);assertFalse(a.busy);assertFalse(a.loading);assertEquals(0,a.workRuns.get());assertNull(a.actionErrorDialog);assertEquals(View.GONE,a.notice.getVisibility());assertButtons(a,true);loadBase[0]=a.loads.get();taskBefore[0]=a.sample.toString();event(nonce,phase,identity,"entered",metadata(a,loadBase[0]));});entered++;
                scenario.onActivity(a->{a.beginSyntheticFailure();safe(a,language,theme);assertTrue(a.busy);a.readNotice("");assertEquals(L.t("Processing request…","正在处理请求…"),a.notice.getText().toString());a.readNotice("Synthetic refresh failure.");assertTrue(a.busy);assertEquals(L.t("Processing request…","正在处理请求…"),a.notice.getText().toString());assertEquals(View.VISIBLE,a.notice.getVisibility());assertButtons(a,false);assertEquals(loadBase[0],a.loads.get());event(nonce,phase,identity,"pending",metadata(a,loadBase[0]));});
                completed(scenario,loadBase[0]);
                scenario.onActivity(a->{
                    safe(a,language,theme);assertFalse(a.busy);assertFalse(a.loading);assertEquals(1,a.workRuns.get());assertFalse(a.workWasMain);assertEquals(1,a.loads.get()-loadBase[0]);assertEquals(taskBefore[0],a.sample.toString());assertButtons(a,true);
                    String expected=kind==DemoTaskFeedbackActivity.FailureCase.NONEMPTY?DemoTaskFeedbackActivity.NONEMPTY:L.t("Check the handoff's latest status before trying again.","请先查看任务的最新状态，再决定是否重试。");
                    assertEquals(View.VISIBLE,a.notice.getVisibility());assertEquals(expected,a.notice.getText().toString());assertNotNull(a.actionErrorDialog);assertTrue(a.actionErrorDialog.isShowing());
                    TextView message=a.actionErrorDialog.findViewById(android.R.id.message);assertNotNull(message);assertEquals(expected,message.getText().toString());
                    assertNotNull(findText(a.actionErrorDialog.getWindow().getDecorView(),L.t("Could not confirm this action","暂时无法确认操作结果")));
                    Button gotIt=a.actionErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE);assertNotNull(gotIt);assertTrue(gotIt.isEnabled());assertEquals(L.t("Got it","知道了"),gotIt.getText().toString());
                    callback[0]=metadata(a,loadBase[0]);event(nonce,phase,identity,"callback-result",callback[0]);
                });
                actualWork+=callback[0].getInt("work_runs");actualLoads+=callback[0].getInt("load_delta");
                scenario.onActivity(a->{safe(a,language,theme);assertFalse(a.busy);assertEquals(taskBefore[0],a.sample.toString());assertEquals(1,a.loads.get()-loadBase[0]);event(nonce,phase,identity,"full-completed",metadata(a,loadBase[0]));});
                scenario.onActivity(a->{safe(a,language,theme);assertTrue(a.actionErrorDialog.isShowing());a.actionErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
                ready(scenario);
                scenario.onActivity(a->{
                    safe(a,language,theme);assertFalse(a.actionErrorDialog.isShowing());
                    String expected=kind==DemoTaskFeedbackActivity.FailureCase.NONEMPTY?DemoTaskFeedbackActivity.NONEMPTY:L.t("Check the handoff's latest status before trying again.","请先查看任务的最新状态，再决定是否重试。");
                    assertEquals(expected,a.actionFailure);
                    a.readNotice("");assertEquals(View.VISIBLE,a.notice.getVisibility());assertEquals(expected,a.notice.getText().toString());
                    a.readNotice("Synthetic refresh failure.");assertEquals(View.VISIBLE,a.notice.getVisibility());assertEquals(expected,a.notice.getText().toString());
                    Bundle saved=new Bundle();a.onSaveInstanceState(saved);assertEquals(expected,saved.getString("actionFailure"));
                    a.notice("Synthetic new action.");assertEquals("",a.actionFailure);assertEquals(View.VISIBLE,a.notice.getVisibility());assertEquals("Synthetic new action.",a.notice.getText().toString());
                    a.readNotice("");assertEquals("",a.notice.getText().toString());assertEquals(View.GONE,a.notice.getVisibility());
                    assertEquals(taskBefore[0],a.sample.toString());assertEquals(1,a.workRuns.get());assertEquals(1,a.loads.get()-loadBase[0]);assertButtons(a,true);
                });completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(scenario!=null)try{
                    scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());destroyed=true;unresolvedLifetime=false;closed++;
                    assertNotNull(retained[0]);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoTaskFeedbackActivity a=retained[0];assertTrue(a.isDestroyed());assertEquals(0,a.forbiddenActions.get());assertEquals(1,a.workRuns.get());assertEquals(1,a.loads.get()-loadBase[0]);event(nonce,phase,identity,"DESTROYED",metadata(a,loadBase[0]));});
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            }
        }}finally{if(destroyed){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->L.language(languageBefore));assertEquals(languageBefore,L.chinese()?"zh":"en");}}
        assertEquals(8,entered);assertEquals(8,completed);assertEquals(8,closed);assertEquals(8,actualWork);assertEquals(8,actualLoads);assertEquals(40,guardSnapshots.get());assertFalse(unresolvedLifetime);
        Bundle summary=new Bundle();summary.putString("stream","TASK_ACTION_FEEDBACK_SUMMARY\t"+nonce+"\t"+phase+"\t"+new JSONObject().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("actual_work_runs",actualWork).put("actual_memory_load_delta",actualLoads).put("guard_snapshots",guardSnapshots.get()).put("business_actions","not_invoked_by_test").put("toast_observation","not measured; actual error dialog and notice are callback branch evidence")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,summary);
    }
    static void safe(DemoTaskFeedbackActivity a,String language,String theme){
        assertEquals(0,a.forbiddenActions.get());assertEquals(language.equals("zh"),L.chinese());assertEquals(theme.equals("dark"),Ui.dark);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);
        assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertTrue(a.store.prefs.getAll().isEmpty());assertEquals(2,a.global.size());
        assertEquals("running",a.sample.optString("status"));assertEquals("",a.sample.optString("report"));assertFalse(a.sample.has("preview_url"));assertFalse(a.sample.has("approvals"));assertNull(a.thumbnail);assertFalse(a.thumbnailRequested);
        assertNull(a.followupDialog);assertNull(a.localRemovalDialog);assertTrue(a.expanded.isEmpty());assertNotNull(a.marker);assertEquals(L.t("UI probe · memory only · no task sent","界面验证 · 仅内存 · 未发送任务"),a.marker.getText().toString());assertEquals(View.VISIBLE,a.marker.getVisibility());
    }
    static List<Button> buttons(View view){List<Button> result=new ArrayList<>();if(view instanceof Button)result.add((Button)view);if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)result.addAll(buttons(((ViewGroup)view).getChildAt(n)));return result;}
    static void assertButtons(DemoTaskFeedbackActivity a,boolean enabled){List<Button> buttons=buttons(a.body);assertFalse(buttons.isEmpty());for(Button button:buttons)assertEquals(enabled,button.isEnabled());}
    static TextView findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=findText(((ViewGroup)view).getChildAt(n),text);if(found!=null)return found;}return null;}
    static void ready(ActivityScenario<DemoTaskFeedbackActivity> scenario)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] ready={false};do{scenario.onActivity(a->ready[0]=a.hasWindowFocus()&&a.body.isAttachedToWindow()&&a.body.getWidth()>0&&a.body.getHeight()>0&&!a.body.isLayoutRequested()&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f);if(ready[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Feedback fixture did not reach its native window/layout state");
    }
    static void completed(ActivityScenario<DemoTaskFeedbackActivity> scenario,int loadBase)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] completed={false};do{scenario.onActivity(a->completed[0]=!a.busy&&a.workRuns.get()==1&&a.loads.get()-loadBase==1&&a.actionErrorDialog!=null&&a.actionErrorDialog.isShowing());if(completed[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Actual TaskActivity callback did not reach settled failure feedback");
    }
    static JSONObject metadata(DemoTaskFeedbackActivity a,int loadBase){
        try{
            List<Button> controls=buttons(a.body);boolean enabled=!controls.isEmpty(),disabled=!controls.isEmpty();JSONArray buttons=new JSONArray();for(Button button:controls){enabled&=button.isEnabled();disabled&=!button.isEnabled();buttons.put(new JSONObject().put("text",button.getText().toString()).put("enabled",button.isEnabled()));}
            boolean showing=a.actionErrorDialog!=null&&a.actionErrorDialog.isShowing();TextView message=a.actionErrorDialog==null?null:a.actionErrorDialog.findViewById(android.R.id.message);int guards=a.forbiddenActions.get();guardSnapshots.incrementAndGet();
            return new JSONObject().put("fixture",a.getClass().getSimpleName()).put("nonce",a.nonce).put("phase",a.phase).put("memory_only",true).put("forbidden_actions",guards).put("work_runs",a.workRuns.get()).put("work_was_main",a.workWasMain).put("memory_loads",a.loads.get()).put("load_delta",a.loads.get()-loadBase).put("busy",a.busy).put("loading",a.loading).put("notice_text",a.notice.getText().toString()).put("notice_visibility",a.notice.getVisibility()).put("marker_text",a.marker.getText().toString()).put("buttons",buttons).put("all_buttons_enabled",enabled).put("all_buttons_disabled",disabled).put("error_dialog_showing",showing).put("error_dialog_message",message==null?JSONObject.NULL:message.getText().toString()).put("activity_destroyed",a.isDestroyed()).put("toast_observation","not measured; actual error dialog and notice are callback branch evidence");
        }catch(JSONException error){throw new AssertionError(error);}
    }
    static void event(String nonce,String phase,String identity,String event,JSONObject data){Bundle status=new Bundle();status.putString("stream","TASK_ACTION_FEEDBACK_EVENT\t"+nonce+"\t"+phase+"\t"+identity+"\t"+event+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
