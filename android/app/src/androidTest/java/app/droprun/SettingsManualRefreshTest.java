package app.droprun;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Four closed native read-controller windows. No write, file, navigation, IME or Relay. */
public class SettingsManualRefreshTest {
    static boolean unresolvedLifetime;
    int guardSnapshots;

    @Test public void recoveryUsesActualReadController()throws Throwable{
        Bundle arguments=InstrumentationRegistry.getArguments();
        if(!"true".equals(arguments.getString("settingsManualRefreshOptIn","false")))return;
        String nonce=arguments.getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertEquals("accepted",arguments.getString("settingsManualRefreshProbe",""));
        // No owned static, context, appearance or touch-mode change precedes the gates above.
        assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context target=instrumentation.getTargetContext();
        assertTrue(target.getPackageName().endsWith(".debug"));
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoSettingsRefreshActivity.class),0).exported);
        int entered=0,full=0,closed=0,reads=0,readDone=0,starts=0,callbacks=0,cacheEdits=0,knownHandles=0;
        String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] paletteBefore=palette();
        try{for(DemoSettingsRefreshActivity.Scene scene:DemoSettingsRefreshActivity.Scene.values()){
            assertFalse(unresolvedLifetime);ActivityScenario<DemoSettingsRefreshActivity> scenario=null;
            DemoSettingsRefreshActivity[] owned={null};boolean[] priorTouch={false},touchChanged={false};int[] callbacksBeforeClose={-1};Throwable failure=null;
            try{
                DemoSettingsRefreshActivity.arm(true,nonce,scene);unresolvedLifetime=true;
                event(nonce,scene,"launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(target,DemoSettingsRefreshActivity.class).putExtra("settingsManualRefreshOptIn",true).putExtra("evidenceNonce",nonce).putExtra("scene",scene.name()));
                knownHandles++;event(nonce,scene,"returned-handle",new JSONObject());
                scenario.onActivity(a->{owned[0]=a;assertTrue(a.entered);priorTouch[0]=a.getWindow().getDecorView().isInTouchMode();event(nonce,scene,"entered",metadata(a));});entered++;
                awaitCallback(scenario,1);settle(scenario);
                scenario.onActivity(a->{
                    safe(a,nonce,scene);assertEquals(1,a.readEpisodes.get());assertEquals(1,a.readCompleted.get());assertEquals(1,a.controllerStarts);assertEquals(1,a.controllerCallbacks);assertFalse(a.refreshing);assertTrue(a.modelRefreshButton.isEnabled());
                    if(scene==DemoSettingsRefreshActivity.Scene.OFFLINE_RECOVERY){assertEquals(0,a.store.models().length());assertFalse(a.store.computerOnline());assertEquals("Computer offline. Start Connector, then refresh.",a.modelRefreshStatus.getText().toString());}
                    else if(scene==DemoSettingsRefreshActivity.Scene.NULL_RECOVERY_FONT2){assertEquals(0,a.store.models().length());assertTrue(a.refreshFailed);assertEquals("重试",a.modelRefreshButton.getText().toString());assertEquals("部分设置刷新失败。请检查中转连接后重试。",a.modelRefreshStatus.getText().toString());}
                    else{assertEquals(1,a.store.models().length());assertNotNull(a.body.findViewById(R.id.settings_model));}
                    if(scene.font==2f)assertEquals(LinearLayout.VERTICAL,((LinearLayout)a.modelRefreshStatus.getParent()).getOrientation());
                    event(nonce,scene,"initial-callback",metadata(a));a.modelRefreshButton.requestRectangleOnScreen(new Rect(0,0,a.modelRefreshButton.getWidth(),a.modelRefreshButton.getHeight()),true);
                });frame(scenario);settle(scenario);
                String[] cachedModels={null},defaultModel={null},defaultEffort={null};int[] renderBefore={0};
                scenario.onActivity(a->{
                    safe(a,nonce,scene);Rect visible=new Rect();assertTrue(a.modelRefreshButton.getGlobalVisibleRect(visible));assertEquals(a.modelRefreshButton.getHeight(),visible.height());
                    assertTrue(a.modelRefreshButton.getHeight()>=Ui.dp(a,48));cachedModels[0]=a.store.models().toString();defaultModel[0]=a.store.defaultModel();defaultEffort[0]=a.store.defaultEffort(defaultModel[0]);renderBefore[0]=a.renderCalls;
                    assertTrue(a.modelRefreshButton.performClick());assertTrue(a.refreshing);assertFalse(a.modelRefreshButton.isEnabled());assertEquals(renderBefore[0],a.renderCalls);assertEquals(2,a.controllerStarts);
                    a.refresh();a.modelRefreshButton.performClick();assertEquals("Repeated pending actions do not start another controller",2,a.controllerStarts);assertEquals(renderBefore[0],a.renderCalls);
                });awaitRead(scenario,2);frame(scenario);settle(scenario);
                if(scene==DemoSettingsRefreshActivity.Scene.OFFLINE_RECOVERY||scene==DemoSettingsRefreshActivity.Scene.CACHED_EMPTY_FAILURE){
                    instrumentation.setInTouchMode(false);touchChanged[0]=true;
                    scenario.onActivity(a->{View preference=a.body.findViewById(scene==DemoSettingsRefreshActivity.Scene.OFFLINE_RECOVERY?R.id.settings_appearance:R.id.settings_model);assertNotNull(preference);assertFalse(preference.isInTouchMode());assertTrue(preference.requestFocus());preference.requestRectangleOnScreen(new Rect(0,0,preference.getWidth(),preference.getHeight()),true);});frame(scenario);settle(scenario);
                }
                int[] pendingY={-1};
                if(scene==DemoSettingsRefreshActivity.Scene.POSITION_AND_DESTROY_FONT2){
                    instrumentation.setInTouchMode(true);touchChanged[0]=true;
                    scenario.onActivity(a->{ScrollView scroll=a.findViewById(R.id.settings_scroll);int max=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());assertTrue(max>Ui.dp(a,48));int old=scroll.getScrollY();int desired=old<max-1?max:max/2;assertNotEquals(old,desired);scroll.scrollTo(0,desired);});frame(scenario);settle(scenario);
                    scenario.onActivity(a->{pendingY[0]=((ScrollView)a.findViewById(R.id.settings_scroll)).getScrollY();assertTrue(pendingY[0]>0);});
                }
                scenario.onActivity(a->{
                    safe(a,nonce,scene);assertTrue(a.refreshing);assertFalse(a.modelRefreshButton.isEnabled());assertFalse(a.workerWasMain);assertEquals(2,a.readEpisodes.get());assertEquals(1,a.readCompleted.get());assertEquals(cachedModels[0],a.store.models().toString());assertEquals(defaultModel[0],a.store.defaultModel());assertEquals(defaultEffort[0],a.store.defaultEffort(defaultModel[0]));
                    assertEquals(scene.language.equals("zh")?"正在刷新设置…":"Refreshing settings…",a.modelRefreshStatus.getText().toString());event(nonce,scene,"manual-pending",metadata(a));a.releaseKnownRead();
                });awaitCallback(scenario,2);frame(scenario);frame(scenario);settle(scenario);
                scenario.onActivity(a->{
                    safe(a,nonce,scene);assertFalse(a.refreshing);assertTrue(a.modelRefreshButton.isEnabled());assertEquals(2,a.readEpisodes.get());assertEquals(2,a.readCompleted.get());assertEquals(2,a.controllerStarts);assertEquals(2,a.controllerCallbacks);assertNotNull(a.body.findViewById(R.id.settings_model));
                    if(scene==DemoSettingsRefreshActivity.Scene.CACHED_EMPTY_FAILURE){assertTrue(a.refreshFailed);assertEquals("Retry",a.modelRefreshButton.getText().toString());assertEquals(cachedModels[0],a.store.models().toString());assertEquals(defaultModel[0],a.store.defaultModel());assertEquals(defaultEffort[0],a.store.defaultEffort(defaultModel[0]));assertEquals(renderBefore[0],a.renderCalls);}
                    else{assertFalse(a.refreshFailed);assertTrue(a.store.models().length()>0);assertEquals(scene.language.equals("zh")?"模型列表已就绪。":"Models are available.",a.modelRefreshStatus.getText().toString());assertTrue(a.renderCalls>renderBefore[0]);}
                    if(scene==DemoSettingsRefreshActivity.Scene.OFFLINE_RECOVERY||scene==DemoSettingsRefreshActivity.Scene.CACHED_EMPTY_FAILURE){View focused=a.body.findViewById(scene==DemoSettingsRefreshActivity.Scene.OFFLINE_RECOVERY?R.id.settings_appearance:R.id.settings_model);assertSame(focused,a.getCurrentFocus());Rect visible=new Rect();assertTrue(focused.getGlobalVisibleRect(visible));assertEquals(focused.getHeight(),visible.height());}
                    if(pendingY[0]>=0){ScrollView scroll=a.findViewById(R.id.settings_scroll);int max=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight());assertEquals("Use the user's pending-time reading position after actual layout",Math.min(pendingY[0],max),scroll.getScrollY());}
                    event(nonce,scene,"manual-callback",metadata(a));
                });
                if(scene==DemoSettingsRefreshActivity.Scene.NULL_RECOVERY_FONT2){
                    scenario.onActivity(a->{a.modelRefreshButton.requestRectangleOnScreen(new Rect(0,0,a.modelRefreshButton.getWidth(),a.modelRefreshButton.getHeight()),true);});frame(scenario);settle(scenario);
                    scenario.onActivity(a->{Rect visible=new Rect();assertTrue(a.modelRefreshButton.getGlobalVisibleRect(visible));assertEquals(a.modelRefreshButton.getHeight(),visible.height());assertTrue(a.modelRefreshButton.getHeight()>=Ui.dp(a,48));});
                }
                if(scene==DemoSettingsRefreshActivity.Scene.POSITION_AND_DESTROY_FONT2){
                    scenario.onActivity(a->{assertTrue(a.modelRefreshButton.performClick());assertTrue(a.refreshing);});awaitRead(scenario,3);
                    scenario.onActivity(a->{assertEquals(2,a.controllerCallbacks);assertEquals(3,a.controllerStarts);assertEquals(3,a.readEpisodes.get());assertEquals(2,a.readCompleted.get());event(nonce,scene,"known-pending-close",metadata(a));});
                }
                scenario.onActivity(a->{callbacksBeforeClose[0]=a.controllerCallbacks;event(nonce,scene,"full-completed",metadata(a));});full++;
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(scenario!=null)try{
                    scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(owned[0]);
                    assertTrue("Only the known executor drains",owned[0].io.awaitTermination(3,TimeUnit.SECONDS));Thread worker=owned[0].readWorker;if(worker!=null){worker.join(3000);assertFalse(worker.isAlive());}
                    instrumentation.runOnMainSync(()->{DemoSettingsRefreshActivity a=owned[0];assertTrue(a.isDestroyed());assertTrue(a.destroyed);assertFalse(a.refreshing);assertTrue(a.io.isTerminated());assertEquals(0,a.updatesAfterClose);assertEquals(0,a.forbiddenActions.get());if(callbacksBeforeClose[0]>=0)assertEquals("Closed callback must not touch the UI",callbacksBeforeClose[0],a.controllerCallbacks);assertEquals(a.readEpisodes.get(),a.readCompleted.get());assertFalse(TaskSyncService.running);event(nonce,scene,"DESTROYED",metadata(a));});
                    closed++;unresolvedLifetime=false;
                    reads+=owned[0].readEpisodes.get();readDone+=owned[0].readCompleted.get();starts+=owned[0].controllerStarts;callbacks+=owned[0].controllerCallbacks;cacheEdits+=owned[0].cacheApplies.get();
                    // Restore framework input mode only after this known window and worker have closed.
                    if(touchChanged[0])instrumentation.setInTouchMode(priorTouch[0]);
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            }
        }}finally{if(!unresolvedLifetime&&closed==knownHandles)restore(languageBefore,darkBefore,paletteBefore);}
        assertFalse(unresolvedLifetime);assertEquals(knownHandles,closed);assertEquals(4,entered);assertEquals(4,full);assertEquals(4,closed);assertEquals(9,reads);assertEquals(reads,readDone);assertEquals(reads,starts);assertEquals(8,callbacks);assertEquals(25,cacheEdits);
        Bundle summary=new Bundle();summary.putString("stream","SETTINGS_MANUAL_REFRESH_SUMMARY\t"+nonce+"\taccepted\t"+new JSONObject().put("entered",entered).put("full_completed",full).put("DESTROYED",closed).put("actual_read_episodes",reads).put("actual_completed_reads",readDone).put("actual_controller_starts",starts).put("actual_ui_callbacks",callbacks).put("actual_cache_applies",cacheEdits).put("guard_snapshots",guardSnapshots).put("known_handles",knownHandles).put("language_restored",languageBefore.equals(L.chinese()?"zh":"en")).put("dark_restored",darkBefore==Ui.dark).put("palette_restored",Arrays.equals(paletteBefore,palette())).put("mode_operation_native","unverified; unchanged source only").put("announcement_count","unverified; no event observer")+"\n");instrumentation.sendStatus(0,summary);
    }
    static int[] palette(){return new int[]{Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM};}
    static void restore(String language,boolean dark,int[] p){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{L.language(language);Ui.dark=dark;Ui.BG=p[0];Ui.SURFACE=p[1];Ui.SURFACE_2=p[2];Ui.SURFACE_3=p[3];Ui.LINE=p[4];Ui.LINE_STRONG=p[5];Ui.TEXT=p[6];Ui.MUTED=p[7];Ui.DIM=p[8];Ui.LIME_SOFT=p[9];Ui.LIME_LINE=p[10];Ui.ACCENT=p[11];Ui.DANGER=p[12];Ui.AMBER=p[13];Ui.SCRIM=p[14];assertEquals(language,L.chinese()?"zh":"en");assertEquals(dark,Ui.dark);assertArrayEquals(p,palette());});}
    void safe(DemoSettingsRefreshActivity a,String nonce,DemoSettingsRefreshActivity.Scene scene){
        assertEquals(0,a.forbiddenActions.get());assertEquals(0,a.updatesAfterClose);assertFalse(TaskSyncService.running);assertEquals(nonce,a.nonce());assertSame(scene,a.scene());assertEquals(scene.language.equals("zh"),L.chinese());assertEquals(scene.appearance.equals("dark"),Ui.dark);assertEquals(scene.font,a.getResources().getConfiguration().fontScale,0f);
        assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertEquals(2,a.global.size());assertEquals(scene.language,a.global.get("language"));assertEquals(scene.appearance,a.global.get("appearance"));assertFalse(a.cached.containsKey("token"));assertFalse(a.cached.containsKey("credential"));assertFalse(a.cached.containsKey("defaultModel"));assertFalse(a.cached.containsKey("defaultEffort"));
        assertEquals(a.readEpisodes.get(),a.projectsGets.get());assertTrue(a.settingsGets.get()<=a.readEpisodes.get());assertTrue(a.retentionGets.get()<=a.readEpisodes.get());assertNotNull(a.marker);assertTrue(a.marker.isShown());assertEquals(scene.language.equals("zh")?"界面验证 · 仅内存 · 未发送设置":"UI probe · memory only · no setting sent",a.marker.getText().toString());
    }
    static void awaitRead(ActivityScenario<DemoSettingsRefreshActivity> scenario,int number)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;DemoSettingsRefreshActivity.Episode[] known={null};
        do{scenario.onActivity(a->{if(a.active!=null&&a.active.number==number)known[0]=a.active;});if(known[0]!=null){assertTrue(known[0].started.await(3,TimeUnit.SECONDS));return;}Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Known controller did not enter fixed read episode");
    }
    static void awaitCallback(ActivityScenario<DemoSettingsRefreshActivity> scenario,int number)throws Exception{
        awaitRead(scenario,number);DemoSettingsRefreshActivity.Episode[] known={null};scenario.onActivity(a->known[0]=a.active);assertNotNull(known[0]);assertTrue(known[0].readDone.await(3,TimeUnit.SECONDS));assertTrue(known[0].callback.await(3,TimeUnit.SECONDS));
    }
    static void frame(ActivityScenario<DemoSettingsRefreshActivity> scenario)throws Exception{
        CountDownLatch drawn=new CountDownLatch(1);scenario.onActivity(a->{View root=a.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){root.getViewTreeObserver().removeOnPreDrawListener(this);drawn.countDown();return true;}});root.invalidate();});assertTrue("Known native frame",drawn.await(3,TimeUnit.SECONDS));
    }
    static void settle(ActivityScenario<DemoSettingsRefreshActivity> scenario)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] settled={false};
        do{scenario.onActivity(a->{ScrollView scroll=a.findViewById(R.id.settings_scroll);settled[0]=a.hasWindowFocus()&&a.body.isAttachedToWindow()&&a.body.getWidth()>0&&!a.body.isLayoutRequested()&&!scroll.isLayoutRequested()&&!a.modelRefreshStatus.isLayoutRequested()&&!a.modelRefreshButton.isLayoutRequested()&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f;});if(settled[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Native layout did not settle");
    }
    JSONObject metadata(DemoSettingsRefreshActivity a){
        try{guardSnapshots++;ScrollView scroll=a.findViewById(R.id.settings_scroll);View focused=a.getCurrentFocus();return new JSONObject().put("fixture",a.getClass().getSimpleName()).put("nonce",a.nonce()).put("scene",a.scene().name()).put("language",a.scene().language).put("appearance",a.scene().appearance).put("font_scale",a.getResources().getConfiguration().fontScale).put("memory_only",true).put("forbidden_actions",a.forbiddenActions.get()).put("read_episodes",a.readEpisodes.get()).put("completed_reads",a.readCompleted.get()).put("projects_gets",a.projectsGets.get()).put("settings_gets",a.settingsGets.get()).put("retention_gets",a.retentionGets.get()).put("cache_applies",a.cacheApplies.get()).put("controller_starts",a.controllerStarts).put("ui_callbacks",a.controllerCallbacks).put("render_calls",a.renderCalls).put("updates_after_close",a.updatesAfterClose).put("worker_was_main",a.workerWasMain).put("refreshing",a.refreshing).put("refresh_failed",a.refreshFailed).put("status",a.modelRefreshStatus.getText().toString()).put("button_text",a.modelRefreshButton.getText().toString()).put("button_enabled",a.modelRefreshButton.isEnabled()).put("button_height_px",a.modelRefreshButton.getHeight()).put("required_48dp_px",Ui.dp(a,48)).put("model_count",a.store.models().length()).put("default_model",a.store.defaultModel()).put("focus_id",focused==null?View.NO_ID:focused.getId()).put("scroll_y",scroll.getScrollY()).put("layout_requested",scroll.isLayoutRequested()||a.body.isLayoutRequested()).put("activity_destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("worker_alive",a.readWorker!=null&&a.readWorker.isAlive()).put("service_running",TaskSyncService.running);}catch(JSONException error){throw new AssertionError(error);}
    }
    static void event(String nonce,DemoSettingsRefreshActivity.Scene scene,String kind,JSONObject data){Bundle status=new Bundle();status.putString("stream","SETTINGS_MANUAL_REFRESH_EVENT\t"+nonce+"\taccepted\t"+scene.name()+"\t"+kind+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
