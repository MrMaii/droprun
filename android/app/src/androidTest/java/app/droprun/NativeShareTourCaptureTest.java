package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.MotionEvent;
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

/** Explicit opt-in native capture; guarded in-memory samples only. */
public class NativeShareTourCaptureTest {
    static final String LINK="  https://example.invalid/posts/alpha?layout=compact#motionA  ";
    String run,language,source;File directory;long startAt,readyAt,cleanupDeadline;
    boolean readySent,doneConfirmed,started,projectTour,touchTour,effortChanged;int expectedStep=1,projectRowClicks;DemoShareEditorActivity retained;
    static final String SECOND_PROJECT="ui-probe-second";
    String expectedProject="ui-probe-project",fixedCatalog;int localDowns,localUps,localCancels;
    final org.json.JSONArray pressSamples=new org.json.JSONArray();
    final java.util.List<JSONObject> events=new java.util.ArrayList<>();

    @Test public void captureOneGuardedNativeShareTour()throws Throwable{
        android.os.Bundle args=InstrumentationRegistry.getArguments();
        String mode=args.getString("nativeShareTour");touchTour="reviewed-memory-project-touch-tour-v3".equals(mode);projectTour=touchTour||"reviewed-memory-project-tour-v2".equals(mode);
        assertTrue("Explicit reviewed capture mode required",projectTour||"reviewed-memory-tour-v1".equals(mode));
        run=args.getString("tourRun","");language=args.getString("tourLanguage","");source=args.getString("tourSource","");
        assertTrue(run.matches("[a-f0-9]{32}"));assertTrue(language.equals("en")||language.equals("zh"));assertTrue(source.matches("[a-f0-9]{64}"));
        assertTrue("Capture-only IME guard requires SDK30+",android.os.Build.VERSION.SDK_INT>=30);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertEquals("app.droprun.mobile.debug",context.getPackageName());
        directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/native-share-tour-"+run);
        assertFalse(directory.exists());assertTrue(directory.mkdirs());
        float previous=DemoShareEditorActivity.hierarchyFontScale;ActivityScenario<DemoShareEditorActivity> scenario=null;Throwable failure=null;boolean destroyed=false,processStaticsRestored=false;
        String previousLanguage=L.chinese()?"zh":"en";boolean previousDark=Ui.dark;int[] previousPalette=touchTour?HistoryShareStateTest.palette():null;
        try{
            DemoShareEditorActivity.hierarchyFontScale=1f;
            Intent intent=new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance","light").putExtra("modelDisclosure",true).putExtra("compactHeight",true).putExtra("materialReview",true).putExtra("direct",true).putExtra("confirmed",true);
            scenario=ActivityScenario.launch(intent);
            expectedStep=projectTour?0:1;
            scenario.onActivity(a->{retained=a;a.materialSample("link");if(touchTour)seedSecondProject(a);a.go(expectedStep,0);});
            settle(scenario,3000);twoPredraws(scenario,3000);hold(scenario,2000);
            scenario.onActivity(a->{guard(a);assertFalse(a.panelOpen);ShareMaterialDisclosureTest.assertClosed(a);assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);});
            readyAt=SystemClock.elapsedRealtime();cleanupDeadline=readyAt+27000;
            event("READY",scenario);writeOnce("READY.json",identity().put("ready_elapsed_ms",readyAt));readySent=true;
            JSONObject start=awaitSignal("START.json",readyAt+15000,true);assertEquals("START",start.getString("signal"));
            started=true;startAt=SystemClock.elapsedRealtime();cleanupDeadline=startAt+24000;
            event("START_RECEIVED",scenario);

            if(touchTour)captureTouchTour(scenario);
            else if(projectTour)captureProjectTour(scenario);
            else{
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
            }
            while(!new File(directory,"DONE.json").exists()&&SystemClock.elapsedRealtime()<cleanupDeadline)hold(scenario,60);
            verifyDone(awaitSignal("DONE.json",cleanupDeadline,false));
            event("RECORDER_DONE",scenario);
        }catch(Throwable problem){failure=problem;}
        finally{
            // Failed takes stop scripted actions but hold this Activity until DONE or the cooperative deadline.
            if(readySent&&!doneConfirmed){try{verifyDone(awaitSignal("DONE.json",cleanupDeadline,false));}catch(Throwable waitFailure){if(failure==null)failure=waitFailure;else failure.addSuppressed(waitFailure);}}
            if(scenario!=null){try{scenario.close();destroyed=scenario.getState()==Lifecycle.State.DESTROYED;assertTrue("Owned Activity reached DESTROYED",destroyed);}catch(Throwable closeFailure){if(failure==null)failure=closeFailure;else failure.addSuppressed(closeFailure);}}
            if(destroyed){DemoShareEditorActivity.hierarchyFontScale=previous;
                if(touchTour)try{HistoryShareStateTest.restore(previousLanguage,previousDark,previousPalette);assertEquals(previous,DemoShareEditorActivity.hierarchyFontScale,0f);assertEquals(previousLanguage,L.chinese()?"zh":"en");assertEquals(previousDark,Ui.dark);assertArrayEquals(previousPalette,HistoryShareStateTest.palette());processStaticsRestored=true;}catch(Throwable restoreFailure){if(failure==null)failure=restoreFailure;else failure.addSuppressed(restoreFailure);}
                try{if(touchTour)counters(retained,expectedProject);else counters(retained);}catch(Throwable guardFailure){if(failure==null)failure=guardFailure;else failure.addSuppressed(guardFailure);}}
            // If launch/close cannot establish destruction, do not restore the static or permit another take.
            JSONObject finalData=identity().put("eligible_for_media_review",failure==null&&doneConfirmed&&destroyed).put("recorder_done_confirmed",doneConfirmed).put("destroyed",destroyed).put("fixture_static_restored",destroyed).put("started",started).put("failure",failure==null?JSONObject.NULL:android.util.Log.getStackTraceString(failure)).put("events",new org.json.JSONArray(events));
            if(touchTour)finalData.put("process_statics_restored",processStaticsRestored).put("static_restore_scope","font/language/dark/15 palette colors; only after known DESTROYED").put("input_scope","local model-header MotionEvent dispatch; other choices performClick; no global input").put("local_downs",localDowns).put("local_ups",localUps).put("local_cancels",localCancels).put("press_samples",pressSamples).put("effort_changed",effortChanged);
            if(retained!=null)finalData.put("guard_counters",counterData(retained));
            writeOnce("native-result.json",finalData);
        }
        if(failure!=null)throw failure;
    }
    // V3 adds two closed memory choices and a single local model-header touch. V1/V2 scripts stay unchanged.
    void seedSecondProject(DemoShareEditorActivity a){
        assertTrue(touchTour);assertFalse(TaskSyncService.running);assertEquals(1,a.store.projects().length());assertEquals("ui-probe-project",a.store.projects().optJSONObject(0).optString("id"));
        try{a.store.projects().put(new JSONObject().put("id",SECOND_PROJECT).put("name",L.t("Second UI sample","第二个界面示例")).put("available",true).put("permission",new JSONObject().put("enabled",true)));}catch(Exception error){throw new AssertionError(error);}fixedCatalog=a.store.projects().toString();
    }
    View memoryProjectRow(DemoShareEditorActivity a,String id){
        assertTrue(touchTour);assertEquals(0,a.step);assertTrue(id.equals("ui-probe-project")||id.equals(SECOND_PROJECT));assertEquals(2,a.store.projects().length());assertEquals(fixedCatalog,a.store.projects().toString());assertEquals("",a.query);
        JSONObject project=a.store.project(id);assertNotNull(project);assertTrue(project.optBoolean("available"));assertTrue(Store.projectEnabled(project));
        String label=project.optString("name")+L.t(", allowed","，已授权")+(id.equals(a.selected)?L.t(", selected","，已选择"):"");View found=null;int clickableRows=0;
        assertNotNull(a.projectList);assertTrue(a.projectList.isAttachedToWindow());
        for(int n=0;n<a.projectList.getChildCount();n++){View child=a.projectList.getChildAt(n);if(child.isClickable())clickableRows++;if(label.contentEquals(String.valueOf(child.getContentDescription()))){assertNull(found);found=child;}}
        assertEquals(2,clickableRows);assertNotNull(found);assertSame(a.projectList,found.getParent());assertTrue(found.isAttachedToWindow());assertTrue(found.isEnabled());assertTrue(found.hasOnClickListeners());return found;
    }
    void chooseProject(ActivityScenario<DemoShareEditorActivity> scenario,String id)throws Exception{
        scenario.onActivity(a->{assertEquals(0,expectedStep);guard(a);View row=memoryProjectRow(a,id);visible(row);assertTrue(row.getHeight()>=Ui.dp(a,48));assertTrue(row.performClick());assertEquals(1,a.step);assertEquals(id,a.selected);expectedProject=id;expectedStep=1;projectRowClicks++;guard(a);});event("CLICK_project-"+id,scenario);
    }
    void captureTouchTour(ActivityScenario<DemoShareEditorActivity> scenario)throws Throwable{
        at(scenario,800);chooseProject(scenario,"ui-probe-project");settle(scenario,700);assertProjectTourNote(scenario);event("FIRST_NOTE_SETTLED",scenario);
        at(scenario,2400);navigate(scenario,"note-to-project",a->a.back,1,0);settle(scenario,700);scenario.onActivity(a->{guard(a);ShareMaterialDisclosureTest.assertClosed(a);visible(memoryProjectRow(a,SECOND_PROJECT));});event("PROJECT_RETURN_SETTLED",scenario);
        at(scenario,3800);chooseProject(scenario,SECOND_PROJECT);settle(scenario,700);assertProjectTourNote(scenario);event("SECOND_NOTE_SETTLED",scenario);
        at(scenario,5200);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());pressModelHeader(scenario);settle(scenario,700);
        scenario.onActivity(a->{assertTrue(a.panelOpen);assertEquals(270f,a.modelChevron.getRotation(),0.01f);});event("MODEL_OPEN_SETTLED",scenario);
        at(scenario,6800);reveal(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));click(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));settle(scenario,700);
        scenario.onActivity(a->{assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});event("MODEL_SELECTED",scenario);
        at(scenario,7800);reveal(scenario,"effort:medium",a->a.panel.findViewWithTag("effort:medium"));
        scenario.onActivity(a->{guard(a);assertEquals("high",a.effort);View choice=a.panel.findViewWithTag("effort:medium");visible(choice);assertTrue(choice.isEnabled());assertTrue(choice.getHeight()>=Ui.dp(a,48));assertTrue(choice.performClick());assertEquals("medium",a.effort);effortChanged=true;guard(a);});event("CLICK_effort:medium",scenario);settle(scenario,700);
        scenario.onActivity(a->{View choice=a.panel.findViewWithTag("effort:medium");ShareModelDisclosureTest.assertOptionNode(choice,L.t("Balanced","均衡"),language);visible(choice);});event("MEDIUM_SELECTED",scenario);
        at(scenario,9200);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());click(scenario,"model-close",a->(View)a.gaugeText.getParent());settle(scenario,700);
        scenario.onActivity(a->{assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());assertEquals(180f,a.modelChevron.getRotation(),0.01f);assertEquals(L.t("Model & effort","模型强度")+"\nCodex · Another sample · "+L.t("Balanced","均衡"),a.gaugeText.getText().toString());});event("MODEL_CLOSED_SETTLED",scenario);
        at(scenario,10200);scenario.onActivity(a->{guard(a);a.scroll.smoothScrollTo(0,0);guard(a);});settle(scenario,700);assertProjectTourNote(scenario);assertEquals(2,projectRowClicks);assertTrue(effortChanged);assertEquals(1,localDowns);assertEquals(1,localUps);assertEquals(0,localCancels);event("FINAL_EDITOR_HOLD",scenario);
    }
    void localEvent(View target,int action,long downAt){
        MotionEvent event=MotionEvent.obtain(downAt,SystemClock.uptimeMillis(),action,target.getWidth()/2f,target.getHeight()/2f,0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try{assertTrue("Only attached model header consumes this local event",target.dispatchTouchEvent(event));if(action==MotionEvent.ACTION_DOWN)localDowns++;else if(action==MotionEvent.ACTION_UP)localUps++;else if(action==MotionEvent.ACTION_CANCEL)localCancels++;}finally{event.recycle();}
    }
    void pressModelHeader(ActivityScenario<DemoShareEditorActivity> scenario)throws Throwable{
        CountDownLatch done=new CountDownLatch(1);View[] target={null};Runnable[] frame={null};Throwable[] error={null};boolean[] down={false},up={false},interior={false};java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean();long[] downAt={0},upAt={0};Throwable failure=null;
        int minimumDownMs=Math.max(120,android.view.ViewConfiguration.getTapTimeout()+16),releaseBy=Math.min(350,android.view.ViewConfiguration.getLongPressTimeout()-50);assertTrue("Tap/long-press timing cannot fit this take",minimumDownMs<releaseBy);
        try{
            scenario.onActivity(a->{guard(a);assertTrue(touchTour);assertFalse(a.panelOpen);View header=(View)a.gaugeText.getParent();target[0]=header;visible(header);assertTrue(header.isAttachedToWindow());assertTrue(header.isEnabled());assertTrue(header.isClickable());assertTrue(header.isFocusable());assertTrue(header.getHeight()>=Ui.dp(a,48));assertTrue(header.getForeground() instanceof android.graphics.drawable.RippleDrawable);assertEquals(1f,header.getScaleX(),0f);assertEquals(1f,header.getScaleY(),0f);
                downAt[0]=SystemClock.uptimeMillis();down[0]=true;localEvent(header,MotionEvent.ACTION_DOWN,downAt[0]);
                frame[0]=new Runnable(){public void run(){if(stop.get())return;try{
                    guard(a);assertTrue(a.hasWindowFocus());assertSame(header,a.gaugeText.getParent());assertTrue(header.isAttachedToWindow());long now=SystemClock.uptimeMillis();assertTrue("Model-header sampler exceeded 700ms; reject take",now-downAt[0]<=700);
                    float x=header.getScaleX(),y=header.getScaleY();assertTrue(x>=0.9749f&&x<=1.0001f);assertEquals(x,y,0.0001f);boolean pressed=header.isPressed();String phase=up[0]?"release":"press";
                    pressSamples.put(new JSONObject().put("phase",phase).put("uptime_ms",now).put("elapsed_ms",now-downAt[0]).put("scale_x",x).put("scale_y",y).put("pressed",pressed).put("panel_open",a.panelOpen));
                    if(!up[0]){assertTrue("No valid press observed before the long-press safety bound",now-downAt[0]<=releaseBy);assertFalse(a.panelOpen);if(x>0.9751f&&x<0.9999f)interior[0]=true;
                        // A scroll parent may defer the pressed state. Require an observed interior sample and actual pressed state, not a sleep guess.
                        if(interior[0]&&pressed&&now-downAt[0]>=minimumDownMs){assertFalse("Sampler aborted before local UP",stop.get());localEvent(header,MotionEvent.ACTION_UP,downAt[0]);up[0]=true;upAt[0]=now;}
                    }else if(now-upAt[0]>=180&&x==1f&&y==1f&&!pressed){assertTrue(a.panelOpen);stop.set(true);done.countDown();return;}
                    header.postOnAnimation(this);
                }catch(Throwable problem){error[0]=problem;stop.set(true);done.countDown();}}};header.postOnAnimation(frame[0]);
            });
            assertTrue("Bounded model-header sampling did not return",done.await(900,TimeUnit.MILLISECONDS));if(error[0]!=null)throw error[0];assertTrue("No actual interior press sample: reject take",interior[0]);assertTrue(up[0]);
        }catch(Throwable problem){stop.set(true);failure=problem;}finally{
            // No global injection, no late UP on failure. Only cancel our own unfinished pointer on this same attached header.
            try{scenario.onActivity(a->{stop.set(true);if(target[0]!=null&&frame[0]!=null)target[0].removeCallbacks(frame[0]);if(down[0]&&!up[0]&&target[0]!=null&&target[0].isAttachedToWindow())localEvent(target[0],MotionEvent.ACTION_CANCEL,downAt[0]);});}catch(Throwable cleanupFailure){if(failure==null)failure=cleanupFailure;else failure.addSuppressed(cleanupFailure);}
        }
        if(failure!=null)throw failure;
        event("MODEL_HEADER_LOCAL_PRESS_SAMPLED",scenario);
    }
    void captureProjectTour(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{
        at(scenario,800);navigate(scenario,"project-to-note",this::soleProjectRow,0,1);settle(scenario,700);assertProjectTourNote(scenario);event("FIRST_NOTE_SETTLED",scenario);
        at(scenario,2400);navigate(scenario,"note-to-project",a->a.back,1,0);settle(scenario,700);scenario.onActivity(a->{guard(a);ShareMaterialDisclosureTest.assertClosed(a);visible(soleProjectRow(a));});event("PROJECT_RETURN_SETTLED",scenario);
        at(scenario,3800);navigate(scenario,"project-to-note-again",this::soleProjectRow,0,1);settle(scenario,700);assertProjectTourNote(scenario);event("SECOND_NOTE_SETTLED",scenario);
        at(scenario,5200);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());click(scenario,"model-open",a->(View)a.gaugeText.getParent());settle(scenario,700);
        scenario.onActivity(a->{assertTrue(a.panelOpen);assertEquals(270f,a.modelChevron.getRotation(),0.01f);});event("MODEL_OPEN_SETTLED",scenario);
        at(scenario,6800);reveal(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));click(scenario,"model:probe-thorough",a->a.panel.findViewWithTag("model:probe-thorough"));settle(scenario,700);
        scenario.onActivity(a->{assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});event("MODEL_SELECTED",scenario);
        at(scenario,7800);reveal(scenario,"effort:high",a->a.panel.findViewWithTag("effort:high"));
        scenario.onActivity(a->{View choice=a.panel.findViewWithTag("effort:high");ShareModelDisclosureTest.assertOptionNode(choice,L.t("Thorough","深入"),language);visible(choice);});event("HIGH_VISIBLE_NO_CLICK",scenario);
        at(scenario,9200);reveal(scenario,"model-header",a->(View)a.gaugeText.getParent());click(scenario,"model-close",a->(View)a.gaugeText.getParent());settle(scenario,700);
        scenario.onActivity(a->{assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());assertEquals(180f,a.modelChevron.getRotation(),0.01f);assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});
        at(scenario,10200);scenario.onActivity(a->{guard(a);a.scroll.smoothScrollTo(0,0);guard(a);});settle(scenario,700);assertProjectTourNote(scenario);assertEquals(2,projectRowClicks);event("FINAL_EDITOR_HOLD",scenario);
    }
    void assertProjectTourNote(ActivityScenario<DemoShareEditorActivity> scenario){
        scenario.onActivity(a->{assertTrue(projectTour);assertEquals(1,expectedStep);guard(a);assertFalse(a.materialOpen);ShareMaterialDisclosureTest.assertClosed(a);assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());
            View receipt=a.stage.findViewWithTag("share-destination-material");assertNotNull(receipt);visible(receipt);
            TextView destination=a.stage.findViewWithTag("share-destination-name");assertNotNull(destination);assertEquals(L.t("For “","转发到「")+a.projectName()+L.t("”","」"),destination.getText().toString());assertCompleteText(destination);visible(destination);});
    }
    View soleProjectRow(DemoShareEditorActivity a){
        assertTrue(projectTour);assertEquals(0,a.step);assertEquals(1,a.store.projects().length());JSONObject project=a.store.projects().optJSONObject(0);assertNotNull(project);assertEquals("ui-probe-project",project.optString("id"));assertTrue(project.optBoolean("available",true));assertTrue(Store.projectEnabled(project));assertEquals("",a.query);
        assertNotNull(a.projectList);assertTrue(a.projectList.isAttachedToWindow());assertEquals(1,a.projectList.getChildCount());View row=a.projectList.getChildAt(0);assertNotNull(row);assertTrue(row.isAttachedToWindow());assertSame(a.projectList,row.getParent());assertTrue(row.isEnabled());assertTrue(row.isClickable());assertTrue(row.hasOnClickListeners());return row;
    }
    void navigate(ActivityScenario<DemoShareEditorActivity> scenario,String name,Target target,int before,int after)throws Exception{
        assertTrue(projectTour);assertTrue((before==0&&after==1)||(before==1&&after==0));
        scenario.onActivity(a->{assertEquals(before,expectedStep);guard(a);View view=target.get(a);assertNotNull(view);visible(view);assertTrue(view.isEnabled());assertTrue(view.getHeight()>=Ui.dp(a,48));assertTrue(view.performClick());assertEquals(after,a.step);expectedStep=after;if(before==0)projectRowClicks++;guard(a);});event("CLICK_"+name,scenario);
    }
    interface Target {View get(DemoShareEditorActivity activity);}
    void click(ActivityScenario<DemoShareEditorActivity> scenario,String name,Target target)throws Exception{
        scenario.onActivity(a->{guard(a);View view=target.get(a);assertNotNull(view);visible(view);assertTrue(view.isEnabled());assertTrue(view.getHeight()>=Ui.dp(a,48));assertTrue(view.performClick());guard(a);});event("CLICK_"+name,scenario);
    }
    void reveal(ActivityScenario<DemoShareEditorActivity> scenario,String name,Target target)throws Exception{
        scenario.onActivity(a->{guard(a);View view=target.get(a);assertNotNull(view);view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),false);});settle(scenario,700);scenario.onActivity(a->{guard(a);visible(target.get(a));});event("REVEAL_"+name,scenario);
    }
    static void counters(DemoShareEditorActivity a){counters(a,"ui-probe-project");}
    static void counters(DemoShareEditorActivity a,String selected){assertNotNull(a);ShareModelDisclosureTest.assertSafe(a);assertEquals(1,a.initializationCloses);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertTrue(a.checkpointNoops>0);assertNull(a.incoming);assertFalse(a.sent);assertFalse(a.closing);assertFalse(a.discardOnFinish);assertNull(a.discardDialog);assertEquals(LINK,a.shared);assertEquals(0,a.attachments.length());assertEquals(selected,a.selected);assertEquals("",a.draft);}
    void guard(DemoShareEditorActivity a){
        if(touchTour){counters(a,expectedProject);assertFalse(TaskSyncService.running);assertEquals(fixedCatalog,a.store.projects().toString());}else counters(a);assertEquals(projectTour?expectedStep:1,a.step);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertFalse(Ui.dark);assertTrue(Ui.motionEnabled(a));if(projectTour&&expectedStep==0){if(touchTour)memoryProjectRow(a,expectedProject);else soleProjectRow(a);}else assertEquals("",a.note.getText().toString());assertFalse(a.receiving);assertFalse(a.busy);assertTrue(a.direct);assertTrue(a.confirmed);assertTrue(a.model.equals("probe-fast")||a.model.equals("probe-thorough"));assertEquals(a.model.equals("probe-fast")||touchTour&&effortChanged?"medium":"high",a.effort);
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
                if(a.stage.getChildCount()==1){View pane=a.stage.getChildAt(0);ViewGroup group=ShareMaterialDisclosureTest.group(a);assertNotNull(group);quiet=a.hasWindowFocus()&&decor.isAttachedToWindow()&&!decor.isLayoutRequested()&&a.getWindow().getAttributes().alpha==1f&&a.sheet.getAlpha()==1f&&a.sheet.getTranslationY()==0f&&pane.getAlpha()==1f&&pane.getTranslationX()==0f&&!pane.isLayoutRequested()&&group.getChildAt(1).getTag(R.id.expand_animation)==null;
                    if(!projectTour||expectedStep==1)quiet=quiet&&a.panel.getTag(R.id.expand_animation)==null;
                    else{assertSame(pane,a.projectList.getParent().getParent());quiet=quiet&&(touchTour?memoryProjectRow(a,expectedProject):soleProjectRow(a)).isAttachedToWindow();}
                }
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
    JSONObject identity()throws Exception{JSONObject data=new JSONObject().put("run",run).put("language",language).put("source_sha256",source).put("sdk",android.os.Build.VERSION.SDK_INT).put("theme","light").put("font_scale",1).put("memory_only",true).put("shared",LINK).put("project","ui-probe-project");if(touchTour)return data.put("project",expectedProject).put("capture_mode","reviewed-memory-project-touch-tour-v3").put("fixture_project_preset",true).put("fixture_project_ids",new org.json.JSONArray().put("ui-probe-project").put(SECOND_PROJECT)).put("native_memory_project_row_clicks",projectRowClicks);if(projectTour)return data.put("capture_mode","reviewed-memory-project-tour-v2").put("fixture_project_preset",true).put("native_memory_project_row_clicks",projectRowClicks);return data.put("preset_project_not_user_selection",true);}
    void event(String label,ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{JSONObject data=identity().put("event",label).put("elapsed_ms",SystemClock.elapsedRealtime()).put("from_start_ms",started?SystemClock.elapsedRealtime()-startAt:JSONObject.NULL);scenario.onActivity(a->{try{guard(a);if(touchTour)data.put("selected_project",a.selected);if(projectTour)data.put("step",a.step).put("expected_step",expectedStep);data.put("model",a.model).put("effort",a.effort).put("model_open",a.panelOpen).put("material_open",a.materialOpen).put("scroll_y",a.scroll.getScrollY()).put("window_focus",a.hasWindowFocus()).put("window_alpha",a.getWindow().getAttributes().alpha).put("ime_visible",a.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime())).put("motion_enabled",Ui.motionEnabled(a)).put("sheet_bounds",bounds(a.sheet).toShortString()).put("marker_bounds",bounds(a.root.findViewWithTag("hierarchy-marker")).toShortString()).put("guard_counters",counterData(a));}catch(Exception error){throw new AssertionError(error);}});events.add(data);try(FileOutputStream output=new FileOutputStream(new File(directory,"events.jsonl"),true)){output.write((data.toString()+"\n").getBytes(StandardCharsets.UTF_8));}}
    static JSONObject counterData(DemoShareEditorActivity a)throws Exception{return new JSONObject().put("forbidden",a.forbiddenActions).put("network",a.networkAttempts).put("save",a.saveAttempts).put("pair",a.pairingAttempts).put("submit",a.submitAttempts).put("start",a.startAttempts).put("checkpoint_persistence",a.checkpointAttempts).put("checkpoint_noops",a.checkpointNoops).put("keyboard_bypasses",a.keyboardBypasses).put("initialization_closes",a.initializationCloses);}
    void writeOnce(String name,JSONObject value)throws Exception{File target=new File(directory,name),part=new File(directory,name+".part");assertFalse(target.exists());assertTrue(part.createNewFile());try(FileOutputStream out=new FileOutputStream(part)){out.write(value.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}assertTrue(part.renameTo(target));}
    JSONObject awaitSignal(String name,long deadline,boolean watchDone)throws Exception{File file=new File(directory,name);while(!file.exists()&&SystemClock.elapsedRealtime()<deadline){if(watchDone&&new File(directory,"DONE.json").exists())throw new AssertionError("Host ended recording before START; reject take");Thread.sleep(40);}assertTrue("Missing "+name+"; recorder lifetime may be unknown",file.exists());JSONObject data=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));assertEquals(run,data.getString("run"));assertEquals(language,data.getString("language"));assertEquals(source,data.getString("source_sha256"));return data;}
    void verifyDone(JSONObject data)throws Exception{assertEquals("DONE",data.getString("signal"));assertTrue(data.getBoolean("recorder_exit_confirmed"));assertEquals(0,data.getInt("recorder_exit_code"));doneConfirmed=true;assertTrue("Host rejected this take",data.getBoolean("eligible_for_media_review"));}
}
