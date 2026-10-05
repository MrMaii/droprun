package app.droprun;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Point;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Tests the real home notice/dialog/scroll behavior; all synchronization stays in memory. */
public class HomeRecoveryFeedbackTest {
    @Test public void manualCheckAfterCacheOnlyPollingPerformsOneSync()throws Exception{
        assertFalse("Never simulate over a live receiver",TaskSyncService.running);boolean wasRunning=TaskSyncService.running;
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();boolean[] touch={false},changedTouch={false},changedRunning={false};
        try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch("en")){
            ready(scenario);scenario.onActivity(a->touch[0]=a.list.isInTouchMode());instrumentation.setInTouchMode(true);changedTouch[0]=true;
            scenario.onActivity(a->a.list.setSelectionFromTop(1,-Ui.dp(a,17)));awaitLayout(scenario);
            String[] anchor={null};int[] offset={0};CountDownLatch[] gate={null};scenario.onActivity(a->{
                assertFalse(TaskSyncService.running);anchor[0]=anchor(a);offset[0]=offset(a);assertTrue(offset[0]<0);TaskSyncService.running=true;changedRunning[0]=true;a.cacheOnlyProbe=true;gate[0]=a.cacheReadStarted;a.load(false);
            });assertTrue("Enter only the worker-thread cache polling gate",gate[0].await(3,TimeUnit.SECONDS));
            scenario.onActivity(a->{assertTrue(a.busy);assertFalse(a.checkingStatus);assertEquals(0,a.probeStore.syncCalls.get());assertFalse("The UI thread must never enable background jobs",a.backgroundSyncEnabled());a.load(true);assertChecking(a);a.cacheReadRelease.countDown();});
            awaitStarted(scenario);awaitLayout(scenario);scenario.onActivity(a->{assertChecking(a);assertEquals("The queued manual action must perform one actual in-memory sync",1,a.probeStore.syncCalls.get());assertAnchor(a,anchor[0],offset[0]);});
            release(scenario,true);awaitSettled(scenario);scenario.onActivity(a->{assertEquals(1,a.probeStore.syncCalls.get());assertEquals("",a.store.prefs.getString("syncError",""));assertProjects(a);assertAnchor(a,anchor[0],offset[0]);assertFalse(a.backgroundSyncEnabled());assertSafe(a);});
        }finally{if(changedRunning[0])TaskSyncService.running=wasRunning;if(changedTouch[0])instrumentation.setInTouchMode(touch[0]);}
        assertEquals("Restore the receiver flag after the local simulation",wasRunning,TaskSyncService.running);
    }
    @Test public void largeTextShowsCompleteProjectNamesInBothLanguages()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch(language)){
            ready(scenario);scenario.onActivity(a->{View row=a.list.getChildAt(0);assertNotNull("Measure an actual laid-out project row",row);MainActivity.HomeHolder holder=(MainActivity.HomeHolder)row.getTag();assertEquals(a.items.get(a.list.getFirstVisiblePosition()).optString("name"),holder.name.getText().toString());assertCompleteText(holder.name);assertSafe(a);});
        }
    }
    @Test public void errorDetailsAndFailedCheckStayReadableInBothLanguages()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch(language)){
            ready(scenario);scenario.onActivity(a->{assertIssueNotice(a);assertTrue(a.notice.performClick());assertDetails(a);});awaitDialog(scenario);
            scenario.onActivity(a->{assertCompleteText(a.syncErrorDialog.findViewById(android.R.id.message));assertFullVisibility(a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE));assertTrue(a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).getHeight()>=Ui.dp(a,48));assertTrue(a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());});
            awaitStarted(scenario);awaitLayout(scenario);
            scenario.onActivity(a->{assertChecking(a);a.load(true);a.load(true);assertEquals(1,a.probeStore.syncCalls.get());assertSafe(a);});capture(scenario,language,"checking");
            release(scenario,false);awaitSettled(scenario);
            scenario.onActivity(a->{assertIssueNotice(a);assertEquals(1,a.probeStore.syncCalls.get());assertTrue(a.notice.performClick());assertDetails(a);assertSafe(a);});capture(scenario,language,"failed-details");
        }
    }
    @Test public void successfulCheckPreservesProjectsAndScrollInBothLanguages()throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch(language)){
            ready(scenario);boolean[] touch={false};scenario.onActivity(a->touch[0]=a.list.isInTouchMode());
            try{
                instrumentation.setInTouchMode(true);scenario.onActivity(a->a.list.setSelectionFromTop(1,-Ui.dp(a,17)));awaitLayout(scenario);
                String[] anchor={null};int[] offset={0};scenario.onActivity(a->{anchor[0]=anchor(a);offset[0]=offset(a);assertTrue("Exercise a genuinely scrolled row",offset[0]<0);assertTrue(a.notice.performClick());assertDetails(a);assertTrue(a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());});
                awaitStarted(scenario);awaitLayout(scenario);scenario.onActivity(a->{assertChecking(a);assertAnchor(a,anchor[0],offset[0]);assertProjects(a);});
                release(scenario,true);awaitSettled(scenario);scenario.onActivity(a->{
                    assertEquals("",a.store.prefs.getString("syncError",""));assertTrue(a.notice.isEnabled());assertEquals("UI probe · no network or stored data",a.notice.getText().toString());assertProjects(a);assertAnchor(a,anchor[0],offset[0]);assertEquals(1,a.probeStore.syncCalls.get());assertSafe(a);
                });capture(scenario,language,"recovered");
            }finally{instrumentation.setInTouchMode(touch[0]);}
        }
    }
    @Test public void manualCheckReusesAnInFlightReadInBothLanguages()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch(language)){
            ready(scenario);scenario.onActivity(a->a.load(false));awaitStarted(scenario);
            scenario.onActivity(a->{assertFalse(a.checkingStatus);assertTrue(a.notice.performClick());assertDetails(a);assertTrue(a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());});awaitLayout(scenario);
            scenario.onActivity(a->{assertChecking(a);assertEquals(1,a.probeStore.syncCalls.get());a.load(true);assertEquals(1,a.probeStore.syncCalls.get());});
            release(scenario,true);awaitSettled(scenario);scenario.onActivity(a->{assertEquals(1,a.probeStore.syncCalls.get());assertSafe(a);assertProjects(a);});
        }
    }
    @Test public void closingHistoricDetailsDoesNotCheckOrChangeProjects()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoHomeRecoveryActivity> scenario=launch(language)){
            ready(scenario);scenario.onActivity(a->{assertTrue(a.notice.performClick());assertDetails(a);assertTrue(a.syncErrorDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());});awaitLayout(scenario);
            scenario.onActivity(a->{assertFalse(a.syncErrorDialog.isShowing());assertFalse(a.busy);assertFalse(a.checkingStatus);assertEquals(0,a.probeStore.syncCalls.get());assertIssueNotice(a);assertProjects(a);assertSafe(a);});capture(scenario,language,"idle");
        }
    }
    static ActivityScenario<DemoHomeRecoveryActivity> launch(String language){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run probes in the public package",context.getPackageName().endsWith(".debug"));
        float font="1".equals(InstrumentationRegistry.getArguments().getString("fontScale"))?1f:2f;assertEquals("Run at requested text size",font,context.getResources().getConfiguration().fontScale,0.01f);assertEquals("Compact screen width",320,context.getResources().getConfiguration().screenWidthDp);
        return ActivityScenario.launch(new Intent(context,DemoHomeRecoveryActivity.class).putExtra("language",language));
    }
    static void ready(ActivityScenario<DemoHomeRecoveryActivity> scenario)throws Exception{
        awaitLayout(scenario);scenario.onActivity(a->{assertFalse(a.backgroundSyncEnabled());assertEquals(0,a.probeStore.syncCalls.get());assertFalse(a.busy);assertFalse(a.checkingStatus);assertSafe(a);assertProjects(a);assertCompleteText(a.demoNotice);assertCompleteText(a.notice);});
    }
    static void assertSafe(DemoHomeRecoveryActivity a){assertEquals("No file, preference edit, service, navigation or API access",0,a.forbiddenActions.get());assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);}
    static void assertProjects(DemoHomeRecoveryActivity a){assertEquals(Arrays.asList("home-ui-project-0","home-ui-project-1","home-ui-project-2"),Arrays.asList(a.items.get(0).optString("id"),a.items.get(1).optString("id"),a.items.get(2).optString("id")));assertEquals(3,a.adapter.getCount());}
    static void assertIssueNotice(DemoHomeRecoveryActivity a){assertEquals(L.t("Sync needs attention · View details","同步需要处理 · 查看详情"),a.notice.getText().toString());assertTrue(a.notice.isEnabled());assertTrue(a.notice.isFocusable());assertCompleteText(a.notice);assertFullVisibility(a.notice);}
    static void assertDetails(DemoHomeRecoveryActivity a){
        assertTrue(a.syncErrorDialog.isShowing());assertNotNull(findText(a.syncErrorDialog.getWindow().getDecorView(),L.t("Last sync issue","上次同步问题")));
        TextView message=a.syncErrorDialog.findViewById(android.R.id.message);assertNotNull(message);assertEquals(DemoHomeRecoveryActivity.ISSUE,message.getText().toString());
        assertEquals(L.t("Check again","重新检查"),a.syncErrorDialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());assertEquals(L.t("Close","关闭"),a.syncErrorDialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
    }
    static void assertChecking(DemoHomeRecoveryActivity a){
        assertTrue(a.busy);assertTrue(a.checkingStatus);assertEquals(L.t("Checking status…","正在检查状态…"),a.notice.getText().toString());assertFalse(a.notice.isEnabled());assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,a.notice.getAccessibilityLiveRegion());assertTrue(a.notice.getHeight()>=Ui.dp(a,48));assertCompleteText(a.notice);assertFullVisibility(a.notice);
        AccessibilityNodeInfo node=a.notice.createAccessibilityNodeInfo();try{assertFalse("Expose disabled checking state to accessibility",node.isEnabled());assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,node.getLiveRegion());}finally{node.recycle();}
    }
    static String anchor(DemoHomeRecoveryActivity a){View row=a.list.getChildAt(0);assertNotNull(row);assertTrue(row.getTag() instanceof MainActivity.HomeHolder);return ((MainActivity.HomeHolder)row.getTag()).id;}
    static int offset(DemoHomeRecoveryActivity a){return a.list.getChildAt(0).getTop()-a.list.getPaddingTop();}
    static void assertAnchor(DemoHomeRecoveryActivity a,String id,int top){assertEquals("Keep the visible project",id,anchor(a));assertEquals("Keep its scroll offset",top,offset(a));}
    static void awaitStarted(ActivityScenario<DemoHomeRecoveryActivity> scenario)throws Exception{CountDownLatch[] started={null};scenario.onActivity(a->started[0]=a.probeStore.started);assertTrue("Only the in-memory sync starts",started[0].await(3,TimeUnit.SECONDS));}
    static void release(ActivityScenario<DemoHomeRecoveryActivity> scenario,boolean success){scenario.onActivity(a->{a.probeStore.success=success;a.probeStore.release.countDown();});}
    static void awaitSettled(ActivityScenario<DemoHomeRecoveryActivity> scenario)throws Exception{awaitLayout(scenario,true);scenario.onActivity(a->{assertFalse(a.busy);assertFalse(a.checkingStatus);});}
    static void awaitDialog(ActivityScenario<DemoHomeRecoveryActivity> scenario)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{View root=a.syncErrorDialog.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(root.hasWindowFocus()&&root.getWidth()>0){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else root.postInvalidateOnAnimation();return true;
        }});root.invalidate();});assertTrue("Wait for the actual error dialog layout",frame.await(3,TimeUnit.SECONDS));
    }
    static void awaitLayout(ActivityScenario<DemoHomeRecoveryActivity> scenario)throws Exception{awaitLayout(scenario,false);}
    static void awaitLayout(ActivityScenario<DemoHomeRecoveryActivity> scenario,boolean settled)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{View root=a.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(a.hasWindowFocus()&&a.root.getWidth()>0&&a.root.getAlpha()==1f&&a.root.getTranslationY()==0f&&(!settled||!a.busy)){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else root.postInvalidateOnAnimation();return true;
        }});root.invalidate();});assertTrue("Wait for actual native layout and synchronization settlement",frame.await(3,TimeUnit.SECONDS));
    }
    static TextView findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return(TextView)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=findText(((ViewGroup)view).getChildAt(n),text);if(found!=null)return found;}return null;}
    static void assertCompleteText(TextView text){Layout layout=text.getLayout();assertNotNull(layout);assertEquals("Render all notice text",text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int n=0;n<layout.getLineCount();n++)assertEquals(0,layout.getEllipsisCount(n));assertTrue("Fit all lines vertically",text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());}
    static void assertFullVisibility(View view){
        Rect visible=new Rect();Point origin=new Point();assertTrue(view.getGlobalVisibleRect(visible,origin));visible.offset(-origin.x,-origin.y);assertEquals("The entire control is visible in its own root",new Rect(0,0,view.getWidth(),view.getHeight()),visible);
        int[] at=new int[2];view.getLocationOnScreen(at);android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();((android.view.WindowManager)view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
        assertTrue("The entire control fits on the physical screen",new Rect(0,0,metrics.widthPixels,metrics.heightPixels).contains(new Rect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight())));
    }
    static void capture(ActivityScenario<DemoHomeRecoveryActivity> scenario,String language,String phase)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);
        CountDownLatch frames=new CountDownLatch(1);scenario.onActivity(a->{a.root.postOnAnimation(()->a.root.postOnAnimation(frames::countDown));a.root.invalidate();});assertTrue(frames.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();java.io.File directory=new java.io.File(context.getExternalFilesDir(null),"ui-probe-evidence/home-recovery-final");assertTrue(directory.isDirectory()||directory.mkdirs());
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String name="home-recovery-"+language+"-"+phase+"-motion-"+(Ui.motionEnabled(context)?"on":"off")+(context.getResources().getConfiguration().fontScale==1f?"-normal-text":"")+".png";
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,name))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
}
