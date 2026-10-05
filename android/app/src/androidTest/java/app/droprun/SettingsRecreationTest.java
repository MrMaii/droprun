package app.droprun;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Production lifecycle with controlled memory work; no real setting is saved or request sent. */
public class SettingsRecreationTest {
    @Test public void pendingSuccessKeepsItsOperationAcrossRecreation()throws Exception{checkRecreation(false);}
    @Test public void pendingFailureKeepsItsOperationAcrossRecreation()throws Exception{checkRecreation(true);}
    @Test public void queuedCompletionDisplaysAndAnnouncesOnlyOnce()throws Exception{
        for(String language:new String[]{"en","zh"})try(SavedEvents events=new SavedEvents(language);ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,false,false)){
            DemoSettingsRecreationActivity[] previous={null};DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
            try{
                frames(scenario,false);scenario.onActivity(a->previous[0]=a);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                DemoSettingsRecreationActivity.finishBeforeAttach=work[0];scenario.recreate();frames(scenario,true);events.idle();
                scenario.onActivity(a->{
                    assertNotSame(previous[0],a);assertTrue(previous[0].isDestroyed());assertSame(operation[0],a.modeChange);assertSame(a,operation[0].observer.get());
                    assertTrue(operation[0].isDone());assertSame("Queued done must not rebuild the already displayed result",a.firstRenderedChild,a.body.getChildAt(0));
                    assertSettled(a,false);assertEquals(0,a.workFactories);assertEquals(1,work[0].calls.get());assertEquals(0,previous[0].forbiddenActions.get());
                });assertEquals("Adopted completion announces once after attachment",1,events.count.get());
            }finally{DemoSettingsRecreationActivity.finishBeforeAttach=null;if(work[0]!=null)work[0].release.countDown();}
        }
    }
    @Test public void oldCompletionCannotReplaceANewerOperation()throws Exception{
        for(String language:new String[]{"en","zh"})try(SavedEvents events=new SavedEvents(language);ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,false,false)){
            DemoSettingsRecreationActivity.ControlledWork[] first={null},second={null};SettingsActivity.ModeChange[] old={null},current={null};
            try{
                frames(scenario,false);start(scenario,first,old);assertTrue(first[0].started.await(3,TimeUnit.SECONDS));
                scenario.onActivity(a->{a.saveMode(false);second[0]=a.work;current[0]=a.modeChange;assertNotSame(old[0],current[0]);assertEquals(2,a.workFactories);});
                complete(first[0],old[0],false);assertTrue(second[0].started.await(3,TimeUnit.SECONDS));frames(scenario,false);events.idle();
                scenario.onActivity(a->{assertSame(current[0],a.modeChange);assertTrue(a.busy);assertEquals(L.t("Saving…","正在保存…"),a.modeNoticeView.getText().toString());assertChoices(a,true);assertEquals(0,a.forbiddenActions.get());});
                assertEquals("Old completion must not announce the new work as saved",0,events.count.get());
                complete(second[0],current[0],false);frames(scenario,true);events.idle();scenario.onActivity(a->assertSettled(a,false));assertEquals(1,events.count.get());
            }finally{if(first[0]!=null)first[0].release.countDown();if(second[0]!=null)second[0].release.countDown();}
        }
    }
    @Test public void oldCompletionCannotReplaceUnrelatedBusyFeedback()throws Exception{
        for(String language:new String[]{"en","zh"})try(SavedEvents events=new SavedEvents(language);ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,false,false)){
            DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
            try{
                frames(scenario,false);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                scenario.onActivity(a->{a.clearModeChange();a.busy=true;a.notice=L.t("Disconnecting…","正在断开…");a.render();});
                complete(work[0],operation[0],false);frames(scenario,false);events.idle();
                scenario.onActivity(a->{assertNull(a.modeChange);assertNull(operation[0].observer.get());assertTrue(a.busy);assertEquals(L.t("Disconnecting…","正在断开…"),a.noticeView.getText().toString());assertEquals("",a.modeMessage);assertEquals(View.GONE,a.modeNoticeView.getVisibility());assertChoices(a,true);assertEquals(0,a.forbiddenActions.get());});
                assertEquals(0,events.count.get());
            }finally{if(work[0]!=null)work[0].release.countDown();}
        }
    }
    @Test public void changedConnectionEndsObservationOfOldCompletion()throws Exception{
        for(String language:new String[]{"en","zh"})try(SavedEvents events=new SavedEvents(language);ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,false,false)){
            DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
            try{
                frames(scenario,false);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));scenario.onActivity(a->a.global.put("instanceId","settings-ui-other-instance"));
                complete(work[0],operation[0],false);frames(scenario,true);events.idle();
                scenario.onActivity(a->{assertNull(a.modeChange);assertNull(operation[0].observer.get());assertFalse(a.busy);assertEquals(L.t("The connection changed. Reopen this screen.","连接已改变，请重新打开此页面。"),a.modeNoticeView.getText().toString());assertChoices(a,false);assertEquals(1,a.workFactories);assertEquals(0,a.forbiddenActions.get());});
                assertEquals("Old scope success must not be announced",0,events.count.get());
            }finally{if(work[0]!=null)work[0].release.countDown();}
        }
    }
    @Test public void nativeStatusEvidenceAndLargeTextTargets()throws Exception{
        for(String language:new String[]{"en","zh"}){
            boolean failure=language.equals("zh");
            try(ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,failure,true)){
                DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
                try{
                    frames(scenario,false);reveal(scenario,false);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                    reveal(scenario,false);scenario.onActivity(a->assertCompleteAndVisible(a,a.modeNoticeView));capture(scenario,language,"pending");
                    complete(work[0],operation[0],failure);frames(scenario,true);reveal(scenario,false);scenario.onActivity(a->{assertSettled(a,failure);assertCompleteAndVisible(a,a.modeNoticeView);});capture(scenario,language,failure?"failure":"success");
                }finally{if(work[0]!=null)work[0].release.countDown();}
            }
            try(ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,false,false)){
                DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
                try{
                    frames(scenario,false);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                    for(boolean direct:new boolean[]{true,false}){reveal(scenario,direct);scenario.onActivity(a->{assertEquals(2f,a.getResources().getConfiguration().fontScale,0f);View row=choice(a,direct);assertTrue(row.getHeight()>=Ui.dp(a,48));assertCompleteAndVisible(a,row);});}
                    scenario.onActivity(a->a.modeNoticeView.requestRectangleOnScreen(new Rect(0,0,a.modeNoticeView.getWidth(),a.modeNoticeView.getHeight()),true));frames(scenario,false);scenario.onActivity(a->{assertCompleteAndVisible(a,a.modeNoticeView);assertEquals(0,a.forbiddenActions.get());});
                }finally{if(work[0]!=null)work[0].release.countDown();}
            }
        }
    }
    void checkRecreation(boolean failure)throws Exception{
        for(String language:new String[]{"en","zh"})try(SavedEvents events=new SavedEvents(language);ActivityScenario<DemoSettingsRecreationActivity> scenario=launch(language,failure,false)){
            DemoSettingsRecreationActivity[] previous={null};DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
            try{
                frames(scenario,false);scenario.onActivity(a->previous[0]=a);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                scenario.recreate();frames(scenario,false);
                scenario.onActivity(a->{
                    assertNotSame(previous[0],a);assertTrue(previous[0].isDestroyed());assertSame(operation[0],a.modeChange);assertSame(a,operation[0].observer.get());
                    assertEquals(1,previous[0].workFactories);assertEquals(0,a.workFactories);assertEquals(1,work[0].calls.get());assertTrue(a.busy);
                    assertEquals(L.t("Saving…","正在保存…"),a.modeNoticeView.getText().toString());assertEquals(View.VISIBLE,a.modeNoticeView.getVisibility());assertChoices(a,true);
                    for(boolean direct:new boolean[]{true,false})choice(a,direct).performClick();assertSame(operation[0],a.modeChange);assertEquals(0,a.workFactories);assertEquals(1,work[0].calls.get());assertEquals(0,previous[0].forbiddenActions.get());assertEquals(0,a.forbiddenActions.get());
                });
                complete(work[0],operation[0],failure);frames(scenario,true);events.idle();scenario.onActivity(a->assertSettled(a,failure));assertEquals(failure?0:1,events.count.get());
                scenario.recreate();frames(scenario,true);events.idle();
                scenario.onActivity(a->{assertSame(operation[0],a.modeChange);assertSettled(a,failure);assertEquals(0,a.workFactories);assertEquals(1,work[0].calls.get());});
                assertEquals("User recreation restores feedback without repeating Saved",failure?0:1,events.count.get());
            }finally{if(work[0]!=null)work[0].release.countDown();}
        }
    }
    static ActivityScenario<DemoSettingsRecreationActivity> launch(String language,boolean failure,boolean normal){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        return ActivityScenario.launch(new Intent(context,normal?DemoSettingsRecreationNormalActivity.class:DemoSettingsRecreationActivity.class).putExtra("language",language).putExtra("fail",failure));
    }
    static void start(ActivityScenario<DemoSettingsRecreationActivity> scenario,DemoSettingsRecreationActivity.ControlledWork[] work,SettingsActivity.ModeChange[] operation){
        scenario.onActivity(a->{assertChoices(a,false);assertTrue("Actual Review option enters production saveMode",choice(a,false).performClick());work[0]=a.work;operation[0]=a.modeChange;assertNotNull(work[0]);assertNotNull(operation[0]);assertEquals(1,a.workFactories);assertFalse(work[0].direct);assertTrue(a.busy);assertEquals(L.t("Saving…","正在保存…"),a.modeNoticeView.getText().toString());assertChoices(a,true);assertEquals(0,a.forbiddenActions.get());});
    }
    static void complete(DemoSettingsRecreationActivity.ControlledWork work,SettingsActivity.ModeChange operation,boolean failure)throws Exception{
        work.release.countDown();try{operation.get(3,TimeUnit.SECONDS);assertFalse("Expected controlled failure",failure);}
        catch(ExecutionException error){assertTrue(failure);assertTrue(error.getCause() instanceof java.io.IOException);assertEquals(work.error,error.getCause().getMessage());}
        assertTrue(operation.isDone());assertEquals(1,work.calls.get());
    }
    static void assertSettled(DemoSettingsRecreationActivity a,boolean failure){
        assertFalse(a.busy);assertTrue(a.modeChange.isDone());assertEquals(View.VISIBLE,a.modeNoticeView.getVisibility());assertEquals(failure?L.t("Could not save the execution preference.","执行偏好保存失败。"):L.t("Execution preference saved.","执行偏好已保存。"),a.modeNoticeView.getText().toString());assertChoices(a,false);assertEquals(0,a.forbiddenActions.get());
    }
    static void assertChoices(DemoSettingsRecreationActivity a,boolean pending){
        assertTrue("Memory work never changes confirmed setting",a.store.directExecution());
        for(boolean direct:new boolean[]{true,false}){View row=choice(a,direct);assertEquals(!pending,row.isEnabled());assertEquals(!pending,row.isFocusable());assertEquals(direct,row.getContentDescription().toString().endsWith(L.t(", selected","，已选择")));AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();try{assertEquals(!pending,node.isEnabled());}finally{node.recycle();}}
    }
    static View choice(DemoSettingsRecreationActivity a,boolean direct){View result=find(a.body,direct?L.t("Act on the idea","直接执行"):L.t("Review a plan first","先看计划"));assertNotNull(result);return result;}
    static View find(View view,String title){CharSequence description=view.getContentDescription();if(description!=null&&(description.toString().equals(title)||description.toString().equals(title+L.t(", selected","，已选择"))))return view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View result=find(((ViewGroup)view).getChildAt(n),title);if(result!=null)return result;}return null;}
    static void frames(ActivityScenario<DemoSettingsRecreationActivity> scenario,boolean settled)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{View root=a.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(a.hasWindowFocus()&&a.body.getWidth()>0&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f&&(!settled||!a.busy)){root.getViewTreeObserver().removeOnPreDrawListener(this);root.postOnAnimation(()->root.postOnAnimation(frame::countDown));}else root.postInvalidateOnAnimation();return true;
        }});root.invalidate();});assertTrue("Actual UI and two native frames",frame.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    static void reveal(ActivityScenario<DemoSettingsRecreationActivity> scenario,boolean direct)throws Exception{
        scenario.onActivity(a->{View row=choice(a,direct);row.requestRectangleOnScreen(new Rect(0,0,row.getWidth(),row.getHeight()),true);});frames(scenario,false);
    }
    static final class SavedEvents implements AutoCloseable{
        final UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();final AtomicInteger count=new AtomicInteger();
        SavedEvents(String language)throws Exception{idle();String expected=language.equals("zh")?"执行偏好已保存。":"Execution preference saved.";automation.setOnAccessibilityEventListener(event->{if(event.getEventType()==AccessibilityEvent.TYPE_ANNOUNCEMENT)for(CharSequence text:event.getText())if(expected.contentEquals(text)){count.incrementAndGet();break;}});}
        void idle()throws Exception{automation.waitForIdle(300,3000);}
        public void close(){automation.setOnAccessibilityEventListener(null);}
    }
    static Rect screenBounds(View view){int[] at=new int[2];view.getLocationOnScreen(at);return new Rect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}
    static boolean fullyVisible(DemoSettingsRecreationActivity a,View view){
        Rect local=new Rect(),global=new Rect();Point origin=new Point();if(!view.getLocalVisibleRect(local)||!local.equals(new Rect(0,0,view.getWidth(),view.getHeight()))||!view.getGlobalVisibleRect(global,origin))return false;global.offset(-origin.x,-origin.y);if(!global.equals(local))return false;
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();a.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);Rect screen=new Rect(0,0,metrics.widthPixels,metrics.heightPixels),bounds=screenBounds(view);View scroll=a.findViewById(R.id.settings_scroll);Rect viewport=screenBounds(scroll);viewport.left+=scroll.getPaddingLeft();viewport.top+=scroll.getPaddingTop();viewport.right-=scroll.getPaddingRight();viewport.bottom-=scroll.getPaddingBottom();return screen.intersect(viewport)&&screen.intersect(bounds)&&screen.equals(bounds);
    }
    static void assertCompleteAndVisible(DemoSettingsRecreationActivity a,View view){
        assertTrue("Entire target fits actual scroll and screen",fullyVisible(a,view));
        if(view instanceof TextView){TextView text=(TextView)view;Layout layout=text.getLayout();assertNotNull(layout);assertEquals(text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int line=0;line<layout.getLineCount();line++)assertEquals(0,layout.getEllipsisCount(line));assertTrue(text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());}
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View child=((ViewGroup)view).getChildAt(n);if(child instanceof android.widget.ImageView&&child.getVisibility()==View.INVISIBLE&&child.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO)continue;assertCompleteAndVisible(a,child);}
    }
    static void capture(ActivityScenario<DemoSettingsRecreationActivity> scenario,String language,String state)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        JSONObject[] geometry={null};scenario.onActivity(a->{try{assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);View row=choice(a,false);assertCompleteAndVisible(a,row);geometry[0]=new JSONObject().put("language",language).put("state",state).put("activity_font_scale",1).put("system_font_unchanged",true).put("saving_or_result_fully_visible",fullyVisible(a,a.modeNoticeView)).put("notice_screen_bounds",screenBounds(a.modeNoticeView).toShortString()).put("review_option_screen_bounds",screenBounds(row).toShortString()).put("scroll_screen_bounds",screenBounds(a.findViewById(R.id.settings_scroll)).toShortString()).put("busy",a.busy).put("forbidden_actions",a.forbiddenActions.get());}catch(Exception error){throw new AssertionError(error);}});
        android.widget.FrameLayout[] marker={null};ViewGroup[] decor={null};
        try{
        scenario.onActivity(a->{
            TextView label=new TextView(a);label.setText(language.equals("zh")?"UI 探针 · 仅内存 · 未发送请求":"UI probe · memory only · no request");label.setTextSize(9);label.setSingleLine(true);label.setGravity(android.view.Gravity.CENTER);label.setTextColor(0xFFFFFFFF);label.setBackgroundColor(0xFF17251D);
            marker[0]=new android.widget.FrameLayout(a);marker[0].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);marker[0].addView(label,new android.widget.FrameLayout.LayoutParams(-1,-1));
            decor[0]=(ViewGroup)a.getWindow().getDecorView();int left=Ui.dp(a,20),width=decor[0].getWidth()-2*left,height=Ui.dp(a,14),top=Ui.dp(a,28);marker[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));marker[0].layout(left,top,left+width,top+height);decor[0].getOverlay().add(marker[0]);
            assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(a.modeNoticeView)));assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(choice(a,false))));assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(choice(a,true))));
        });
            frames(scenario,false);InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/settings-recreation-marked");assertTrue(directory.isDirectory()||directory.mkdirs());String name="settings-"+language+"-"+state+"-local-font1";Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(new File(directory,name+".json"))){out.write(geometry[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{screenshot.recycle();}
        }finally{scenario.onActivity(a->{if(decor[0]!=null&&marker[0]!=null)decor[0].getOverlay().remove(marker[0]);});}
    }
}
