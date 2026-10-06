package app.droprun;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Private opt-in draft: two fixed memory failures; observe native events, never synthesize speech. */
public class SettingsFailureAnnouncementTest {
    static boolean unresolvedLifetime;

    @Test public void closedFailureAnnouncesOnce()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("settingsFailureAnnouncementProbe","");if(phase.isEmpty())return;assertEquals("accepted",phase);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);assertEquals(0f,DemoSettingsRecreationActivity.hierarchyFontScale,0f);assertNull(DemoSettingsRecreationActivity.finishBeforeAttach);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        assertFalse(context.getPackageManager().getActivityInfo(new ComponentName(context,DemoSettingsRecreationNormalActivity.class),0).exported);
        String previousLanguage=L.chinese()?"zh":"en";boolean destroyed=true;int entered=0,completed=0,closed=0,actualWorks=0,actualAnnouncements=0;
        try{for(String[] scene:new String[][]{{"en","light"},{"zh","dark"}}){
            String language=scene[0],theme=scene[1],identity=language+"|"+theme,expected=language.equals("zh")?"执行偏好保存失败。":"Could not save the execution preference.";
            assertTrue(destroyed);assertFalse(unresolvedLifetime);
            try(FailureEvents events=new FailureEvents(context.getPackageName(),expected)){
                ActivityScenario<DemoSettingsRecreationNormalActivity> scenario=null;DemoSettingsRecreationNormalActivity[] retained={null};DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};Throwable failure=null;
                try{
                    destroyed=false;unresolvedLifetime=true;event(nonce,identity,"launch-attempt",new JSONObject());
                    scenario=ActivityScenario.launch(new Intent(context,DemoSettingsRecreationNormalActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("fail",true).putExtra("modelHierarchy",false));event(nonce,identity,"returned-handle",new JSONObject());
                    scenario.onActivity(a->retained[0]=a);ready(scenario,false,null,expected);
                    scenario.onActivity(a->{safe(a,language,theme);assertNull(a.modeChange);assertEquals(0,a.workFactories);assertFalse(a.busy);choices(a,false);assertEquals(0,events.count.get());event(nonce,identity,"entered",metadata(a,events));});entered++;
                    scenario.onActivity(a->{a.saveMode(false);work[0]=a.work;operation[0]=a.modeChange;assertNotNull(work[0]);assertNotNull(operation[0]);assertSame(a,operation[0].observer.get());assertFalse(work[0].direct);assertEquals(expected,work[0].error);});
                    assertTrue("Only the fixed memory work starts",work[0].started.await(3,TimeUnit.SECONDS));
                    scenario.onActivity(a->{safe(a,language,theme);assertSame(operation[0],a.modeChange);assertFalse(operation[0].isDone());assertTrue(a.busy);assertEquals(L.t("Saving…","正在保存…"),a.modeNoticeView.getText().toString());assertEquals(1,a.workFactories);assertEquals(1,work[0].calls.get());assertFalse(operation[0].announced);assertEquals(0,events.count.get());choices(a,true);event(nonce,identity,"pending",metadata(a,events));});
                    work[0].release.countDown();
                    try{operation[0].get(3,TimeUnit.SECONDS);fail("The closed work must fail");}catch(ExecutionException error){assertTrue(error.getCause() instanceof IOException);assertEquals(expected,error.getCause().getMessage());}
                    ready(scenario,true,operation[0],expected);
                    scenario.onActivity(a->{settled(a,language,theme,operation[0],expected);event(nonce,identity,"callback-result",metadata(a,events));});
                    assertTrue("A real matching TYPE_ANNOUNCEMENT must arrive",events.first.await(3,TimeUnit.SECONDS));events.idle();events.assertOne();
                    scenario.onActivity(a->{settled(a,language,theme,operation[0],expected);assertTrue(operation[0].announced);event(nonce,identity,"observed-announcement",metadata(a,events));});
                    View[] originalNotice={null};scenario.onActivity(a->{originalNotice[0]=a.modeNoticeView;a.showModeChange(operation[0]);a.showModeChange(operation[0]);assertSame(originalNotice[0],a.modeNoticeView);});
                    Thread.sleep(300);events.idle();events.assertOne();
                    scenario.onActivity(a->{settled(a,language,theme,operation[0],expected);assertSame(originalNotice[0],a.modeNoticeView);assertTrue(operation[0].announced);event(nonce,identity,"duplicate-check",metadata(a,events));});completed++;
                    actualWorks+=work[0].calls.get();actualAnnouncements+=events.count.get();
                }catch(Throwable error){failure=error;throw error;}
                finally{
                    DemoSettingsRecreationActivity.ControlledWork owned=work[0]!=null?work[0]:retained[0]==null?null:retained[0].work;if(owned!=null)owned.release.countDown();
                    if(scenario!=null)try{
                        scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);assertTrue("The activity shuts down its own executor",retained[0].io.awaitTermination(3,TimeUnit.SECONDS));
                        if(owned!=null&&owned.runner!=null){owned.runner.join(3000);assertFalse("No worker survives the closed window",owned.runner.isAlive());}
                        destroyed=true;unresolvedLifetime=false;closed++;
                        if(failure==null)events.assertOne();
                        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoSettingsRecreationNormalActivity a=retained[0];assertTrue(a.isDestroyed());assertEquals(0,a.forbiddenActions.get());assertTrue(a.io.isTerminated());event(nonce,identity,"DESTROYED",metadata(a,events));});
                    }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
                }
            }
        }}finally{if(destroyed){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->L.language(previousLanguage));assertEquals(previousLanguage,L.chinese()?"zh":"en");}}
        assertFalse(unresolvedLifetime);assertEquals(2,entered);assertEquals(2,completed);assertEquals(2,closed);assertEquals(2,actualWorks);assertEquals(2,actualAnnouncements);
        assertEquals(0f,DemoSettingsRecreationActivity.hierarchyFontScale,0f);assertNull(DemoSettingsRecreationActivity.finishBeforeAttach);
        Bundle summary=new Bundle();summary.putString("stream","SETTINGS_FAILURE_ANNOUNCEMENT_SUMMARY\t"+nonce+"\taccepted\t"+new JSONObject().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("actual_work_calls",actualWorks).put("actual_announcements",actualAnnouncements).put("business_actions","not_invoked_by_test").put("speech_observation","not measured; native TYPE_ANNOUNCEMENT only")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,summary);
    }
    static void safe(DemoSettingsRecreationNormalActivity a,String language,String theme){
        assertEquals(0,a.forbiddenActions.get());assertEquals(language.equals("zh"),L.chinese());assertEquals(theme.equals("dark"),Ui.dark);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);
        assertTrue(a.getIntent().getBooleanExtra("fail",false));assertFalse(a.getIntent().getBooleanExtra("modelHierarchy",false));assertNull(a.modelProbeStore);assertEquals(0,a.modelSaves);
        assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertEquals(2,a.global.size());assertEquals(language,a.global.get("language"));assertEquals(theme,a.global.get("appearance"));assertEquals(1,a.cached.size());assertEquals(Boolean.TRUE,a.cached.get("directExecution"));assertTrue(a.store.directExecution());assertEquals(1,a.store.prefs.getAll().size());assertTrue(a.switching.isEmpty());
        AccessibilityManager manager=(AccessibilityManager)a.getSystemService(Context.ACCESSIBILITY_SERVICE);assertNotNull(manager);assertTrue("Observe existing automation accessibility, never enable it",manager.isEnabled());
    }
    static void settled(DemoSettingsRecreationNormalActivity a,String language,String theme,SettingsActivity.ModeChange operation,String expected){
        safe(a,language,theme);assertSame(operation,a.modeChange);assertSame(operation,a.shownModeResult);assertTrue(operation.isDone());assertFalse(a.busy);assertEquals(1,a.workFactories);assertEquals(1,a.work.calls.get());assertEquals(expected,a.modeMessage);assertEquals(expected,a.modeNoticeView.getText().toString());assertEquals(View.VISIBLE,a.modeNoticeView.getVisibility());assertTrue(a.modeNoticeView.isAttachedToWindow());assertEquals(Ui.AMBER,a.modeNoticeView.getCurrentTextColor());choices(a,false);
    }
    static View choice(View view,String title){CharSequence description=view.getContentDescription();if(description!=null&&description.toString().startsWith(title+L.t(", ","，")))return view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View found=choice(((ViewGroup)view).getChildAt(n),title);if(found!=null)return found;}return null;}
    static void choices(DemoSettingsRecreationNormalActivity a,boolean pending){
        for(boolean direct:new boolean[]{true,false}){View row=choice(a.body,direct?L.t("Act on the idea","直接执行"):L.t("Review a plan first","先看计划"));assertNotNull(row);assertEquals(!pending,row.isEnabled());assertEquals(!pending,row.isFocusable());assertEquals(direct,row.getContentDescription().toString().endsWith(L.t(", selected","，已选择")));}
    }
    static void ready(ActivityScenario<DemoSettingsRecreationNormalActivity> scenario,boolean completed,SettingsActivity.ModeChange operation,String expected)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] ready={false};do{scenario.onActivity(a->ready[0]=a.hasWindowFocus()&&a.body.isAttachedToWindow()&&!a.body.isLayoutRequested()&&a.body.getWidth()>0&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f&&(!completed||a.modeChange==operation&&a.shownModeResult==operation&&!a.busy&&expected.contentEquals(a.modeNoticeView.getText())&&a.modeNoticeView.isAttachedToWindow()));if(ready[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Known memory window/callback did not settle");
    }
    static JSONObject metadata(DemoSettingsRecreationNormalActivity a,FailureEvents events){
        try{return new JSONObject().put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("forbidden_actions",a.forbiddenActions.get()).put("work_factories",a.workFactories).put("work_calls",a.work==null?0:a.work.calls.get()).put("worker_alive",a.work!=null&&a.work.runner!=null&&a.work.runner.isAlive()).put("executor_terminated",a.io.isTerminated()).put("model_saves",a.modelSaves).put("confirmed_direct",a.store.directExecution()).put("busy",a.busy).put("notice",a.modeNoticeView.getText().toString()).put("notice_color",a.modeNoticeView.getCurrentTextColor()).put("announced",a.modeChange!=null&&a.modeChange.announced).put("activity_destroyed",a.isDestroyed()).put("matching_announcements",events.count.get()).put("native_events",events.snapshot());}catch(JSONException error){throw new AssertionError(error);}
    }
    static void event(String nonce,String identity,String event,JSONObject data){Bundle status=new Bundle();status.putString("stream","SETTINGS_FAILURE_ANNOUNCEMENT_EVENT\t"+nonce+"\taccepted\t"+identity+"\t"+event+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static final class FailureEvents implements AutoCloseable{
        final UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();final AtomicInteger count=new AtomicInteger();final CountDownLatch first=new CountDownLatch(1);final AtomicReference<Throwable> error=new AtomicReference<>();final List<JSONObject> rows=new ArrayList<>();
        FailureEvents(String targetPackage,String expected)throws Exception{
            AccessibilityServiceInfo info=automation.getServiceInfo();assertNotNull(info);assertTrue("The existing automation service must receive announcements",(info.eventTypes&AccessibilityEvent.TYPE_ANNOUNCEMENT)!=0);idle();
            automation.setOnAccessibilityEventListener(event->{if(event.getEventType()!=AccessibilityEvent.TYPE_ANNOUNCEMENT||!targetPackage.contentEquals(event.getPackageName()==null?"":event.getPackageName())||event.getText().size()!=1||!expected.equals(String.valueOf(event.getText().get(0))))return;
                try{synchronized(rows){rows.add(new JSONObject().put("type",event.getEventType()).put("package",event.getPackageName().toString()).put("text",event.getText().get(0).toString()).put("event_time_ms",event.getEventTime()).put("observed_elapsed_ms",SystemClock.elapsedRealtime()));count.incrementAndGet();}}catch(Throwable failure){error.compareAndSet(null,failure);}finally{first.countDown();}
            });
        }
        void idle()throws Exception{automation.waitForIdle(300,3000);}
        void assertOne(){assertNull(error.get());assertEquals(1,count.get());synchronized(rows){assertEquals(1,rows.size());}}
        JSONArray snapshot(){assertNull(error.get());synchronized(rows){return new JSONArray(rows);}}
        @Override public void close(){automation.setOnAccessibilityEventListener(null);}
    }
}
