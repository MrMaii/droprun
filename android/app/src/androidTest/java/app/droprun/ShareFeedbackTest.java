package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Native feedback timing/layout only; no real share, saved outbox or Relay receipt is created. */
public class ShareFeedbackTest {
    static final String EN_OFFLINE="You're offline. Saved safely; it will send when connected.",ZH_OFFLINE="手机离线，已保存。联网后自动发送。";
    static final String EN_NOTIFICATIONS="Notifications are off. Check DropRun or Codex for updates.",ZH_NOTIFICATIONS="通知未开启，请在 App 或 Codex 查看后续。";

    @Test public void offlineFeedbackRemainsReadableBeyondTheOldTimeoutAtLargeText()throws Exception{checkAttention(false,true,"offline");}
    @Test public void notificationsOffFeedbackRemainsReadableBeyondTheOldTimeoutAtLargeText()throws Exception{checkAttention(true,false,"notifications");}
    void checkAttention(boolean online,boolean notifications,String source)throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoShareFeedbackActivity> scenario=launch(context,language,online,notifications)){
            DemoShareFeedbackActivity activity=ready(scenario);finishFlight(scenario,activity);
            assertAttention(scenario,activity,language,source);assertCloseNow(scenario,activity,language);
        }
    }
    @Test public void ordinaryFeedbackReturnsAutomatically()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoShareFeedbackActivity> scenario=launch(context,language,true,true)){
            DemoShareFeedbackActivity activity=ready(scenario);finishFlight(scenario,activity);assertAutomaticReturn(activity,language);
        }
    }
    @Test public void offlineFeedbackAutomaticallyReturnsAfterItsReadingWindow()throws Exception{
        Context context=fixtureContext();
        try(ActivityScenario<DemoShareFeedbackActivity> scenario=launch(context,"en",false,true)){
            DemoShareFeedbackActivity activity=ready(scenario);finishFlight(scenario,activity);
            AccessibilityManager accessibility=(AccessibilityManager)activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
            int recommended=Build.VERSION.SDK_INT>=29?accessibility.getRecommendedTimeoutMillis(4000,AccessibilityManager.FLAG_CONTENT_TEXT|AccessibilityManager.FLAG_CONTENT_CONTROLS):accessibility.isTouchExplorationEnabled()?8000:4000;
            assertTrue("Use a bounded local accessibility timeout",recommended<=10000);
            assertFalse("Leave the attention explanation readable before the four-second deadline",activity.closedSignal.await(3500,TimeUnit.MILLISECONDS));
            long remaining=Math.max(0,recommended+1500L-(SystemClock.uptimeMillis()-activity.landedAt));
            assertTrue("Attention feedback still returns automatically after the reading window",activity.closedSignal.await(remaining,TimeUnit.MILLISECONDS));
            assertTrue("Give attention feedback at least four seconds",activity.closedAt-activity.landedAt>=4000);
            assertTrue("Respect the actual system-recommended reading timeout",activity.closedAt-activity.landedAt>=recommended);assertEquals(1,activity.closeRequests);
            assertTrue(activity.destroyedSignal.await(2,TimeUnit.SECONDS));assertSafe(activity);
        }
    }
    @Test public void conditionsChangedDuringFlightRefreshBothCopyAndTiming()throws Exception{
        Context context=fixtureContext();
        for(boolean[] transition:new boolean[][]{{false,true,true,true},{true,true,false,true},{true,true,true,false}}){
            try(ActivityScenario<DemoShareFeedbackActivity> scenario=launch(context,"en",transition[0],transition[1])){
                DemoShareFeedbackActivity activity=ready(scenario);
                scenario.onActivity(a->{a.plane.play(360,a::landed);a.phoneOnline=transition[2];a.notificationsEnabled=transition[3];});
                assertTrue("Wait for the real flight callback",activity.landedSignal.await(3,TimeUnit.SECONDS));
                if(transition[2]&&transition[3]){assertAutomaticReturn(activity,"en");assertFalse("Do not retain the earlier offline copy",activity.landedStatus.contains("offline"));}
                else{String source=transition[2]?"notifications":"offline";assertAttention(scenario,activity,"en",source);assertCloseNow(scenario,activity,"en");}
            }
        }
    }
    static Context fixtureContext(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        assertEquals("Run this focused check at 200% font size",2f,context.getResources().getConfiguration().fontScale,0.01f);return context;
    }
    static ActivityScenario<DemoShareFeedbackActivity> launch(Context context,String language,boolean online,boolean notifications){
        return ActivityScenario.launch(new Intent(context,DemoShareFeedbackActivity.class).putExtra("language",language).putExtra("online",online).putExtra("notifications",notifications));
    }
    static DemoShareFeedbackActivity ready(ActivityScenario<DemoShareFeedbackActivity> scenario)throws Exception{
        DemoShareFeedbackActivity[] activity={null};CountDownLatch frame=new CountDownLatch(1);
        scenario.onActivity(a->{activity[0]=a;assertSafe(a);a.root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(a.hasWindowFocus()&&a.holder.getWidth()>0&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f){a.root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else a.root.postInvalidateOnAnimation();return true;
        }});a.root.invalidate();});
        assertTrue("Wait for the actual sheet entrance and native layout",frame.await(3,TimeUnit.SECONDS));return activity[0];
    }
    static void finishFlight(ActivityScenario<DemoShareFeedbackActivity> scenario,DemoShareFeedbackActivity activity)throws Exception{
        scenario.onActivity(a->a.plane.play(360,a::landed));assertTrue("Wait for the real flight callback",activity.landedSignal.await(3,TimeUnit.SECONDS));
    }
    static void assertAttention(ActivityScenario<DemoShareFeedbackActivity> scenario,DemoShareFeedbackActivity activity,String language,String source)throws Exception{
        assertFalse("Attention feedback must remain open beyond the old 100/280ms return window",activity.closedSignal.await(750,TimeUnit.MILLISECONDS));
        scenario.onActivity(a->{
            assertFalse(a.isFinishing());assertFalse(a.closing);assertSafe(a);assertEquals(View.VISIBLE,a.status.getVisibility());assertEquals(1f,a.status.getAlpha(),0f);
            String expected=source.equals("offline")?(language.equals("zh")?ZH_OFFLINE:EN_OFFLINE):(language.equals("zh")?ZH_NOTIFICATIONS:EN_NOTIFICATIONS);
            assertEquals("Explain the current attention state without the longer routine instructions",expected,a.status.getText().toString());assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,a.status.getAccessibilityLiveRegion());
            if(source.equals("offline"))assertEquals(language.equals("zh")?"已保存，等待联网":"Saved. Waiting for connection.",a.sendTitle.getText().toString());
            for(TextView text:new TextView[]{a.sendTitle,a.status,a.badge})assertCompleteAndVisible(text);
            assertEquals("Keep the complete Codex label on one line at large text",1,a.badge.getLayout().getLineCount());
            View close=findDescription(a.root,language.equals("zh")?"关闭":"Close");assertNotNull(close);assertTrue(close.isEnabled());Rect visible=new Rect();assertTrue(close.getGlobalVisibleRect(visible));assertEquals(close.getHeight(),visible.height());
            assertTrue("Exercise a compact phone width",a.sheet.getWidth()<=Ui.dp(a,320));
        });
        capture(language,source);
    }
    static void assertCloseNow(ActivityScenario<DemoShareFeedbackActivity> scenario,DemoShareFeedbackActivity activity,String language)throws Exception{
        scenario.onActivity(a->{View close=findDescription(a.root,language.equals("zh")?"关闭":"Close");assertFalse("Exercise a saved share with a nonempty in-memory note",a.draft.trim().isEmpty());long pressed=SystemClock.uptimeMillis();assertTrue(close.performClick());assertTrue("Close acts immediately",a.closing);assertNull("Saved feedback needs no discard confirmation",a.discardDialog);assertTrue(a.closedAt>=pressed);assertEquals(1,a.closeRequests);assertSafe(a);});
        assertTrue("The actual close finishes the probe",activity.destroyedSignal.await(2,TimeUnit.SECONDS));assertSafe(activity);
    }
    static void assertAutomaticReturn(DemoShareFeedbackActivity activity,String language)throws Exception{
        int base=Ui.motionEnabled(activity)?280:100;AccessibilityManager accessibility=(AccessibilityManager)activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        int recommended=Build.VERSION.SDK_INT>=29?accessibility.getRecommendedTimeoutMillis(base,AccessibilityManager.FLAG_CONTENT_TEXT|AccessibilityManager.FLAG_CONTENT_CONTROLS):accessibility.isTouchExplorationEnabled()?8000:base;
        assertTrue("Use a bounded local accessibility timeout",recommended<=10000);
        assertTrue("Routine feedback returns without adding the four-second attention delay",activity.closedSignal.await(recommended+1500L,TimeUnit.MILLISECONDS));
        assertTrue("Do not shorten the actual system-recommended timeout",activity.closedAt-activity.landedAt>=recommended);assertEquals(1,activity.closeRequests);
        assertEquals(language.equals("zh")?"已保存，自动发送":"Saved. We'll take it from here.",activity.landedTitle);
        assertTrue(activity.destroyedSignal.await(2,TimeUnit.SECONDS));assertSafe(activity);
    }
    static void assertCompleteAndVisible(TextView text){
        Layout layout=text.getLayout();assertNotNull(layout);assertEquals("Render every character",text.length(),layout.getLineEnd(layout.getLineCount()-1));
        for(int line=0;line<layout.getLineCount();line++)assertEquals("Do not ellipsize feedback",0,layout.getEllipsisCount(line));
        assertTrue("All text lines fit vertically: "+text.getText(),text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());
        Rect visible=new Rect();assertTrue("Feedback must be visible",text.getGlobalVisibleRect(visible));assertEquals("Keep the full feedback visible at large text",text.getHeight(),visible.height());
    }
    static void assertSafe(DemoShareFeedbackActivity activity){assertNull(activity.incoming);assertEquals(1,activity.initializationCloses);assertEquals(0,activity.networkAttempts);assertEquals(0,activity.saveAttempts);assertEquals(0,activity.pairingAttempts);}
    static View findDescription(View view,String description){
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View found=findDescription(((ViewGroup)view).getChildAt(n),description);if(found!=null)return found;}return null;
    }
    static void capture(String language,String source)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();java.io.File base=context.getExternalFilesDir(null);assertNotNull(base);
        java.io.File directory=new java.io.File(base,"ui-probe-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        String name="share-feedback-"+language+"-"+source+"-motion-"+(Ui.motionEnabled(context)?"on":"off")+".png";
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,name))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
}
