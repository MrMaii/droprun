package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Private draft: four active, closed refresh failures. No business click, API, file, capture or OS change. */
public class TaskRefreshFeedbackTest {
    static boolean unresolvedLifetime;
    final AtomicInteger guardSnapshots=new AtomicInteger();

    @Test public void closedRefreshFailuresKeepLocalFeedback()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("taskRefreshFeedbackProbe","");if(phase.isEmpty())return;assertEquals("accepted",phase);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertTrue("Handler.hasCallbacks requires API 29",Build.VERSION.SDK_INT>=29);assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoTaskRefreshFeedbackActivity.class),0).exported);
        String previousLanguage=L.chinese()?"zh":"en";boolean destroyed=true;int entered=0,completed=0,closed=0,actualCalls=0,actualFinished=0;
        try{for(String[] scene:new String[][]{{"en","light","NULL"},{"en","light","EMPTY"},{"zh","dark","WHITESPACE"},{"zh","dark","NONEMPTY"}}){
            String language=scene[0],theme=scene[1],identity=language+"|"+theme+"|"+scene[2];assertTrue(destroyed);assertFalse(unresolvedLifetime);
            String expected=scene[2].equals("NONEMPTY")?DemoTaskRefreshFeedbackActivity.NONEMPTY:language.equals("zh")?"暂时无法刷新这条交办，稍后会自动重试。":"Could not refresh this handoff. We'll try again shortly.";
            ActivityScenario<DemoTaskRefreshFeedbackActivity> scenario=null;DemoTaskRefreshFeedbackActivity[] retained={null};String[] original={null};Throwable failure=null;JSONObject[] callback={null};
            try{
                destroyed=false;unresolvedLifetime=true;event(nonce,phase,identity,"launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(target,DemoTaskRefreshFeedbackActivity.class).putExtra("taskRefreshFeedbackOptIn",true).putExtra("taskRefreshFeedbackProbe",phase).putExtra("evidenceNonce",nonce).putExtra("language",language).putExtra("appearance",theme).putExtra("failureCase",scene[2]));
                event(nonce,phase,identity,"returned-handle",new JSONObject());scenario.onActivity(a->retained[0]=a);
                assertTrue("Inherited onResume starts the one fixed refresh",retained[0].refreshStarted.await(3,TimeUnit.SECONDS));ready(scenario,false);
                scenario.onActivity(a->{safe(a,language,theme);assertTrue(a.foreground);assertFalse(a.busy);assertTrue(a.loading);assertEquals(1,a.refreshCalls.get());assertEquals(0,a.refreshFinished.get());assertFalse(a.refreshWasMain);assertFalse(a.handler.hasCallbacks(a.refresh));assertEquals("",a.notice.getText().toString());assertEquals(View.GONE,a.notice.getVisibility());original[0]=a.sample.toString();event(nonce,phase,identity,"entered",metadata(a,original[0]));});entered++;
                scenario.onActivity(a->{a.load();safe(a,language,theme);assertTrue(a.loading);assertEquals(1,a.refreshCalls.get());assertEquals(0,a.refreshFinished.get());assertFalse(a.handler.hasCallbacks(a.refresh));assertEquals(original[0],a.sample.toString());event(nonce,phase,identity,"pending",metadata(a,original[0]));a.releaseSyntheticRefresh();});
                ready(scenario,true);
                scenario.onActivity(a->{
                    safe(a,language,theme);assertTrue(a.foreground);assertFalse(a.busy);assertFalse(a.loading);assertEquals(1,a.refreshCalls.get());assertEquals(1,a.refreshFinished.get());assertFalse(a.refreshWasMain);assertEquals(original[0],a.sample.toString());
                    assertEquals(expected,a.notice.getText().toString());assertEquals(View.VISIBLE,a.notice.getVisibility());assertTrue(a.notice.isShown());assertEquals(Ui.AMBER,a.notice.getCurrentTextColor());assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,a.notice.getAccessibilityLiveRegion());assertNull(a.actionErrorDialog);assertTrue(a.handler.hasCallbacks(a.refresh));
                    callback[0]=metadata(a,original[0]);event(nonce,phase,identity,"callback-result",callback[0]);
                    event(nonce,phase,identity,"full-completed",metadata(a,original[0]));
                });completed++;actualCalls+=callback[0].getInt("refresh_calls");actualFinished+=callback[0].getInt("refresh_finished");
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(retained[0]!=null)retained[0].releaseSyntheticRefresh();
                if(scenario!=null)try{
                    scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);
                    assertTrue("The Activity shuts down its own executor",retained[0].io.awaitTermination(3,TimeUnit.SECONDS));
                    Thread runner=retained[0].refreshRunner;if(runner!=null){runner.join(3000);assertFalse("No worker survives the closed window",runner.isAlive());}
                    destroyed=true;unresolvedLifetime=false;closed++;
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoTaskRefreshFeedbackActivity a=retained[0];assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());assertFalse(a.foreground);assertFalse(a.handler.hasCallbacks(a.refresh));event(nonce,phase,identity,"DESTROYED",metadata(a,original[0]));assertEquals(0,a.forbiddenActions.get());assertFalse(TaskSyncService.running);});
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            }
        }}finally{if(destroyed){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->L.language(previousLanguage));assertEquals(previousLanguage,L.chinese()?"zh":"en");}}
        assertFalse(unresolvedLifetime);assertEquals(4,entered);assertEquals(4,completed);assertEquals(4,closed);assertEquals(4,actualCalls);assertEquals(4,actualFinished);assertEquals(20,guardSnapshots.get());
        Bundle summary=new Bundle();summary.putString("stream","TASK_REFRESH_FEEDBACK_SUMMARY\t"+nonce+"\t"+phase+"\t"+new JSONObject().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("actual_refresh_calls",actualCalls).put("actual_refresh_finished",actualFinished).put("guard_snapshots",guardSnapshots.get()).put("language_restored",previousLanguage.equals(L.chinese()?"zh":"en")).put("business_actions","not_invoked_by_test").put("scope","active failure callbacks only; paused/destroyed late callbacks and success remain untested")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,summary);
    }
    static void safe(DemoTaskRefreshFeedbackActivity a,String language,String theme){
        assertEquals(0,a.forbiddenActions.get());assertFalse(TaskSyncService.running);assertEquals(language.equals("zh"),L.chinese());assertEquals(theme.equals("dark"),Ui.dark);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);
        assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertTrue(a.store.prefs.getAll().isEmpty());assertEquals(2,a.global.size());assertEquals(language,a.global.get("language"));assertEquals(theme,a.global.get("appearance"));
        assertEquals(DemoTaskRefreshFeedbackActivity.ID,a.taskId);assertEquals(a.taskId,a.sample.optString("id"));assertEquals("running",a.sample.optString("status"));assertEquals("",a.sample.optString("report"));assertFalse(a.sample.has("preview_url"));assertFalse(a.sample.has("thread_id"));assertFalse(a.sample.has("approvals"));assertNull(a.thumbnail);assertFalse(a.thumbnailRequested);
        assertNull(a.followupDialog);assertNull(a.localRemovalDialog);assertNull(a.actionErrorDialog);assertTrue(a.expanded.isEmpty());assertNotNull(a.marker);assertNotSame(a.notice,a.marker);assertEquals(L.t("UI probe · memory only · no task sent","界面验证 · 仅内存 · 未发送任务"),a.marker.getText().toString());assertEquals(View.VISIBLE,a.marker.getVisibility());
    }
    static void ready(ActivityScenario<DemoTaskRefreshFeedbackActivity> scenario,boolean callback)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] ready={false};do{scenario.onActivity(a->ready[0]=a.hasWindowFocus()&&a.body.isAttachedToWindow()&&a.body.getWidth()>0&&a.body.getHeight()>0&&!a.body.isLayoutRequested()&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f&&(!callback||!a.loading&&a.refreshFinished.get()==1&&a.notice.isShown()&&a.notice.getWidth()>0&&a.notice.getHeight()>0&&!a.notice.isLayoutRequested()));if(ready[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Fixed memory refresh window or actual UI callback did not settle");
    }
    JSONObject metadata(DemoTaskRefreshFeedbackActivity a,String original){
        try{guardSnapshots.incrementAndGet();return new JSONObject().put("fixture",a.getClass().getSimpleName()).put("nonce",a.nonce).put("phase",a.phase).put("memory_only",true).put("elapsed_ms",SystemClock.elapsedRealtime()).put("forbidden_actions",a.forbiddenActions.get()).put("refresh_calls",a.refreshCalls.get()).put("refresh_finished",a.refreshFinished.get()).put("refresh_was_main",a.refreshWasMain).put("refresh_started",a.refreshStarted.getCount()==0).put("refresh_released",a.refreshRelease.getCount()==0).put("foreground",a.foreground).put("busy",a.busy).put("loading",a.loading).put("named_refresh_pending",a.handler.hasCallbacks(a.refresh)).put("notice_text",a.notice.getText().toString()).put("notice_visibility",a.notice.getVisibility()).put("notice_color",a.notice.getCurrentTextColor()).put("notice_live_region",a.notice.getAccessibilityLiveRegion()).put("sample_unchanged",original==null?JSONObject.NULL:original.equals(a.sample.toString())).put("error_dialog_showing",a.actionErrorDialog!=null&&a.actionErrorDialog.isShowing()).put("activity_destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("worker_alive",a.refreshRunner!=null&&a.refreshRunner.isAlive()).put("service_running",TaskSyncService.running);}catch(JSONException error){throw new AssertionError(error);}
    }
    static void event(String nonce,String phase,String identity,String event,JSONObject data){Bundle status=new Bundle();status.putString("stream","TASK_REFRESH_FEEDBACK_EVENT\t"+nonce+"\t"+phase+"\t"+identity+"\t"+event+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
