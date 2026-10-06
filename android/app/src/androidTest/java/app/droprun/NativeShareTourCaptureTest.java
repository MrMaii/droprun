package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareSettingsHierarchyTest.visible;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static org.junit.Assert.*;

/** PRIVATE V2 DRAFT: opt-in capture only. Not yet compiled, installed or executed. */
public class NativeShareTourCaptureTest {
    static final String LINK="  https://example.invalid/posts/alpha?layout=compact#motionA  ";
    String run,language,source;File directory;long startAt,readyAt,cleanupDeadline;
    boolean readySent,doneConfirmed,started;DemoShareEditorActivity retained;
    final java.util.List<JSONObject> events=new java.util.ArrayList<>();

    @Test public void captureOneGuardedNativeShareTour()throws Throwable{
        android.os.Bundle args=InstrumentationRegistry.getArguments();
        assertEquals("reviewed-memory-tour-v1",args.getString("nativeShareTour"));
        run=args.getString("tourRun","");language=args.getString("tourLanguage","");source=args.getString("tourSource","");
        assertTrue(run.matches("[a-f0-9]{32}"));assertTrue(language.equals("en")||language.equals("zh"));assertTrue(source.matches("[a-f0-9]{64}"));
        assertTrue("Capture-only IME guard requires SDK30+",android.os.Build.VERSION.SDK_INT>=30);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertEquals("app.droprun.mobile.debug",context.getPackageName());
        directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/native-share-tour-"+run);
        assertFalse(directory.exists());assertTrue(directory.mkdirs());
        float previous=DemoShareEditorActivity.hierarchyFontScale;ActivityScenario<DemoShareEditorActivity> scenario=null;Throwable failure=null;boolean destroyed=false;
        try{
            DemoShareEditorActivity.hierarchyFontScale=1f;
            Intent intent=new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance","light").putExtra("modelDisclosure",true).putExtra("compactHeight",true).putExtra("materialReview",true).putExtra("direct",true).putExtra("confirmed",true);
            scenario=ActivityScenario.launch(intent);
            scenario.onActivity(a->{retained=a;a.materialSample("link");a.go(1,0);});
            settle(scenario,3000);twoPredraws(scenario,3000);hold(scenario,2000);
            scenario.onActivity(a->{guard(a);assertFalse(a.panelOpen);ShareMaterialDisclosureTest.assertClosed(a);assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);});
            readyAt=SystemClock.elapsedRealtime();cleanupDeadline=readyAt+27000;
            event("READY",scenario);writeOnce("READY.json",identity().put("ready_elapsed_ms",readyAt));readySent=true;
            JSONObject start=awaitSignal("START.json",readyAt+15000,true);assertEquals("START",start.getString("signal"));
            started=true;startAt=SystemClock.elapsedRealtime();cleanupDeadline=startAt+24000;
            event("START_RECEIVED",scenario);

            at(scenario,800);click(scenario,"material-open",a->ShareMaterialDisclosureTest.header(a));settle(scenario,700);
            scenario.onActivity(a->{ShareMaterialDisclosureTest.assertExactMaterial(a);assertEquals(LINK,((TextView)a.stage.findViewWithTag("share-material-text")).getText().toString());});
            reveal(scenario,"received-link",a->a.stage.findViewWithTag("share-material-text"));
            scenario.onActivity(a->{TextView raw=a.stage.findViewWithTag("share-material-text");assertCompleteText(raw);visible(raw);});event("MATERIAL_OPEN_SETTLED",scenario);
            at(scenario,3100);reveal(scenario,"material-header",ShareMaterialDisclosureTest::header);click(scenario,"material-close",ShareMaterialDisclosureTest::header);settle(scenario,700);
            scenario.onActivity(ShareMaterialDisclosureTest::assertClosed);event("MATERIAL_CLOSED_SETTLED",scenario);
            at(scenario,4100);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());click(scenario,"model-open",a->(View)a.gaugeText.getParent());settle(scenario,700);
            scenario.onActivity(a->{assertTrue(a.panelOpen);assertEquals(270f,a.modelChevron.getRotation(),0.01f);});event("MODEL_OPEN_SETTLED",scenario);
            at(scenario,5700);reveal(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));click(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));settle(scenario,700);
            scenario.onActivity(a->{assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});event("MODEL_SELECTED",scenario);
            at(scenario,6600);reveal(scenario,"effort:high",a->a.panel.findViewWithTag("effort:high"));
            scenario.onActivity(a->{View choice=a.panel.findViewWithTag("effort:high");ShareModelDisclosureTest.assertOptionNode(choice,L.t("Thorough","深入"),language);visible(choice);});event("HIGH_VISIBLE_NO_CLICK",scenario);
            at(scenario,8000);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());click(scenario,"model-close",a->(View)a.gaugeText.getParent());settle(scenario,700);
            scenario.onActivity(a->{assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());assertEquals(180f,a.modelChevron.getRotation(),0.01f);assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});
            at(scenario,8800);scenario.onActivity(a->{guard(a);a.scroll.smoothScrollTo(0,0);guard(a);});settle(scenario,700);event("FINAL_EDITOR_HOLD",scenario);
            while(!new File(directory,"DONE.json").exists()&&SystemClock.elapsedRealtime()<cleanupDeadline)hold(scenario,60);
            verifyDone(awaitSignal("DONE.json",cleanupDeadline,false));
            event("RECORDER_DONE",scenario);
        }catch(Throwable problem){failure=problem;}
        finally{
            // Failed takes stop scripted actions but hold this Activity until DONE or the cooperative deadline.
            if(readySent&&!doneConfirmed){try{verifyDone(awaitSignal("DONE.json",cleanupDeadline,false));}catch(Throwable waitFailure){if(failure==null)failure=waitFailure;else failure.addSuppressed(waitFailure);}}
            if(scenario!=null){try{scenario.close();destroyed=scenario.getState()==Lifecycle.State.DESTROYED;assertTrue("Owned Activity reached DESTROYED",destroyed);}catch(Throwable closeFailure){if(failure==null)failure=closeFailure;else failure.addSuppressed(closeFailure);}}
            if(destroyed){DemoShareEditorActivity.hierarchyFontScale=previous;try{counters(retained);}catch(Throwable guardFailure){if(failure==null)failure=guardFailure;else failure.addSuppressed(guardFailure);}}
            // If launch/close cannot establish destruction, do not restore the static or permit another take.
            JSONObject finalData=identity().put("eligible_for_media_review",failure==null&&doneConfirmed&&destroyed).put("recorder_done_confirmed",doneConfirmed).put("destroyed",destroyed).put("fixture_static_restored",destroyed).put("started",started).put("failure",failure==null?JSONObject.NULL:android.util.Log.getStackTraceString(failure)).put("events",new org.json.JSONArray(events));
            if(retained!=null)finalData.put("guard_counters",counterData(retained));
            writeOnce("native-result.json",finalData);
        }
        if(failure!=null)throw failure;
    }
    interface Target {View get(DemoShareEditorActivity activity);}
    void click(ActivityScenario<DemoShareEditorActivity> scenario,String name,Target target)throws Exception{
        scenario.onActivity(a->{guard(a);View view=target.get(a);assertNotNull(view);visible(view);assertTrue(view.isEnabled());assertTrue(view.getHeight()>=Ui.dp(a,48));assertTrue(view.performClick());guard(a);});event("CLICK_"+name,scenario);
    }
    void reveal(ActivityScenario<DemoShareEditorActivity> scenario,String name,Target target)throws Exception{
        scenario.onActivity(a->{guard(a);View view=target.get(a);assertNotNull(view);view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),false);});settle(scenario,700);scenario.onActivity(a->{guard(a);visible(target.get(a));});event("REVEAL_"+name,scenario);
    }
    static void counters(DemoShareEditorActivity a){assertNotNull(a);ShareModelDisclosureTest.assertSafe(a);assertEquals(1,a.initializationCloses);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertTrue(a.checkpointNoops>0);assertNull(a.incoming);assertFalse(a.sent);assertFalse(a.closing);assertFalse(a.discardOnFinish);assertNull(a.discardDialog);assertEquals(LINK,a.shared);assertEquals(0,a.attachments.length());assertEquals("ui-probe-project",a.selected);assertEquals("",a.draft);}
    static void guard(DemoShareEditorActivity a){
        counters(a);assertEquals(1,a.step);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertFalse(Ui.dark);assertTrue(Ui.motionEnabled(a));assertEquals("",a.note.getText().toString());assertFalse(a.receiving);assertFalse(a.busy);assertTrue(a.direct);assertTrue(a.confirmed);assertTrue(a.model.equals("probe-fast")||a.model.equals("probe-thorough"));assertEquals(a.model.equals("probe-fast")?"medium":"high",a.effort);
        WindowInsets insets=a.getWindow().getDecorView().getRootWindowInsets();assertNotNull(insets);assertFalse("IME visible: reject take without changing it",insets.isVisible(WindowInsets.Type.ime()));
        TextView marker=a.root.findViewWithTag("hierarchy-marker");assertCompleteText(marker);visible(marker);
    }
    void hold(ActivityScenario<DemoShareEditorActivity> scenario,long duration)throws Exception{long end=SystemClock.elapsedRealtime()+duration;do{scenario.onActivity(a->{guard(a);assertTrue("Capture window keeps focus",a.hasWindowFocus());});Thread.sleep(Math.min(50,Math.max(1,end-SystemClock.elapsedRealtime())));}while(SystemClock.elapsedRealtime()<end);}
    void at(ActivityScenario<DemoShareEditorActivity> scenario,long offset)throws Exception{long target=startAt+offset;while(SystemClock.elapsedRealtime()<target)hold(scenario,Math.min(50,target-SystemClock.elapsedRealtime()));assertTrue("Action missed fixed take budget",SystemClock.elapsedRealtime()-target<450);}
    void settle(ActivityScenario<DemoShareEditorActivity> scenario,long duration)throws Exception{
        long end=SystemClock.elapsedRealtime()+duration;boolean[] ok={false};int[] lastY={Integer.MIN_VALUE};long[] stableSince={-1};
        // Do not mistake the unchanged pre-scroll position for a settled animation.
        twoPredraws(scenario,duration);
        while(SystemClock.elapsedRealtime()<end){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{guard(a);View decor=a.getWindow().getDecorView();boolean quiet=false;
                if(a.stage.getChildCount()==1){View pane=a.stage.getChildAt(0);ViewGroup group=ShareMaterialDisclosureTest.group(a);quiet=a.hasWindowFocus()&&decor.isAttachedToWindow()&&!decor.isLayoutRequested()&&a.getWindow().getAttributes().alpha==1f&&a.sheet.getAlpha()==1f&&a.sheet.getTranslationY()==0f&&pane.getAlpha()==1f&&pane.getTranslationX()==0f&&!pane.isLayoutRequested()&&group.getChildAt(1).getTag(R.id.expand_animation)==null&&a.panel.getTag(R.id.expand_animation)==null;}
                int y=a.scroll.getScrollY();long now=SystemClock.elapsedRealtime();
                if(!quiet){stableSince[0]=-1;lastY[0]=y;ok[0]=false;return;}
                if(lastY[0]!=y||stableSince[0]<0){lastY[0]=y;stableSince[0]=now;}
                ok[0]=now-stableSince[0]>=100;
            });
            if(ok[0])return;Thread.sleep(20);
        }
        fail("Native Share layout and scrollY did not settle for100ms inside the original time budget");
    }
    void twoPredraws(ActivityScenario<DemoShareEditorActivity> scenario,long budget)throws Exception{CountDownLatch draw=new CountDownLatch(2);scenario.onActivity(a->{View view=a.root;view.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){draw.countDown();if(draw.getCount()==0)view.getViewTreeObserver().removeOnPreDrawListener(this);else view.postInvalidateOnAnimation();return true;}});view.postInvalidateOnAnimation();});assertTrue(draw.await(budget,TimeUnit.MILLISECONDS));}
    JSONObject identity()throws Exception{return new JSONObject().put("run",run).put("language",language).put("source_sha256",source).put("sdk",android.os.Build.VERSION.SDK_INT).put("theme","light").put("font_scale",1).put("memory_only",true).put("shared",LINK).put("project","ui-probe-project").put("preset_project_not_user_selection",true);}
    void event(String label,ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{JSONObject data=identity().put("event",label).put("elapsed_ms",SystemClock.elapsedRealtime()).put("from_start_ms",started?SystemClock.elapsedRealtime()-startAt:JSONObject.NULL);scenario.onActivity(a->{try{guard(a);data.put("model",a.model).put("effort",a.effort).put("model_open",a.panelOpen).put("material_open",a.materialOpen).put("scroll_y",a.scroll.getScrollY()).put("window_focus",a.hasWindowFocus()).put("window_alpha",a.getWindow().getAttributes().alpha).put("ime_visible",a.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime())).put("motion_enabled",Ui.motionEnabled(a)).put("sheet_bounds",bounds(a.sheet).toShortString()).put("marker_bounds",bounds(a.root.findViewWithTag("hierarchy-marker")).toShortString()).put("guard_counters",counterData(a));}catch(Exception error){throw new AssertionError(error);}});events.add(data);try(FileOutputStream output=new FileOutputStream(new File(directory,"events.jsonl"),true)){output.write((data.toString()+"\n").getBytes(StandardCharsets.UTF_8));}}
    static JSONObject counterData(DemoShareEditorActivity a)throws Exception{return new JSONObject().put("forbidden",a.forbiddenActions).put("network",a.networkAttempts).put("save",a.saveAttempts).put("pair",a.pairingAttempts).put("submit",a.submitAttempts).put("start",a.startAttempts).put("checkpoint_persistence",a.checkpointAttempts).put("checkpoint_noops",a.checkpointNoops).put("keyboard_bypasses",a.keyboardBypasses).put("initialization_closes",a.initializationCloses);}
    void writeOnce(String name,JSONObject value)throws Exception{File target=new File(directory,name),part=new File(directory,name+".part");assertFalse(target.exists());assertTrue(part.createNewFile());try(FileOutputStream out=new FileOutputStream(part)){out.write(value.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}assertTrue(part.renameTo(target));}
    JSONObject awaitSignal(String name,long deadline,boolean watchDone)throws Exception{File file=new File(directory,name);while(!file.exists()&&SystemClock.elapsedRealtime()<deadline){if(watchDone&&new File(directory,"DONE.json").exists())throw new AssertionError("Host ended recording before START; reject take");Thread.sleep(40);}assertTrue("Missing "+name+"; recorder lifetime may be unknown",file.exists());JSONObject data=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));assertEquals(run,data.getString("run"));assertEquals(language,data.getString("language"));assertEquals(source,data.getString("source_sha256"));return data;}
    void verifyDone(JSONObject data)throws Exception{assertEquals("DONE",data.getString("signal"));assertTrue(data.getBoolean("recorder_exit_confirmed"));assertEquals(0,data.getInt("recorder_exit_code"));doneConfirmed=true;assertTrue("Host rejected this take",data.getBoolean("eligible_for_media_review"));}
}
