package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Native editor/layout only. All samples stay in memory; Send is never activated. */
public class ShareEditorTest {
    @Test public void initialEditorAndSendRemainReachableAtLargeText()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})for(int setting=0;setting<3;setting++)try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,setting)){
            ready(scenario);scenario.onActivity(a->{assertEquals(0,a.scroll.getScrollY());assertEquals("",a.note.getText().toString());assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);assertSafe(a);assertCompleteText(findText(a.stage,language.equals("zh")?"想让 Codex 做什么？":"What should Codex do?"));});
            capture(scenario,language,setting,"initial");scrollToSend(scenario,language);
            scenario.onActivity(a->{assertTrue("Large-text editor requires real scrolling",a.scroll.getScrollY()>0);assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,setting,"send");
        }
    }
    @Test public void noteAndModelChoicesStayLocalWithoutSubmitting()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,0)){
            ready(scenario);scenario.onActivity(a->{a.note.setText(language.equals("zh")?"先整理这条材料的可用想法。":"Find the useful ideas in this material first.");assertEquals(a.note.getText().toString(),a.draft);reveal(a,(View)a.gaugeText.getParent());});awaitFrame(scenario);
            scenario.onActivity(a->{View gauge=(View)a.gaugeText.getParent();assertTrue(gauge.isFocusable());assertTrue(gauge.performClick());});awaitFrame(scenario);
            scenario.onActivity(a->{View model=a.panel.findViewWithTag("model:probe-thorough");assertNotNull(model);reveal(a,model);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(a.panel.findViewWithTag("model:probe-thorough").performClick());assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);View effort=a.panel.findViewWithTag("effort:xhigh");assertNotNull(effort);reveal(a,effort);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(a.panel.findViewWithTag("effort:xhigh").performClick());assertEquals("xhigh",a.effort);assertTrue("Preserve the optional note during parameter changes",a.note.getText().length()>0);View gauge=(View)a.gaugeText.getParent();reveal(a,gauge);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(((View)a.gaugeText.getParent()).performClick());});awaitFrame(scenario);scrollToSend(scenario,language);
            scenario.onActivity(a->{assertFalse(a.panelOpen);assertTrue("Describe effort in the chosen language while retaining the exact request value",a.gaugeText.getText().toString().endsWith(language.equals("zh")?" · 最深":" · Most thorough"));assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,0,"edited");
        }
    }
    @Test public void cachedExecutionSettingIsReadableBeforeSendInBothLanguages()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})for(int setting=0;setting<3;setting++)try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,setting)){
            final int variant=setting;
            ready(scenario);scenario.onActivity(a->{
                View cue=a.stage.findViewWithTag("share-execution-setting");assertNotNull("Show the cached/unknown execution preference before Send",cue);
                TextView title=a.stage.findViewWithTag("share-execution-title"),detail=a.stage.findViewWithTag("share-execution-detail");assertNotNull(title);assertNotNull(detail);
                String expected=variant==2?(language.equals("zh")?"执行设置尚未确认":"Execution setting not confirmed"):variant==1?(language.equals("zh")?"已保存设置 · 先看计划":"Saved setting · Plan review"):(language.equals("zh")?"已保存设置 · 直接执行":"Saved setting · Direct execution");
                assertEquals("Distinguish a saved preference from an unconfirmed fallback",expected,title.getText().toString());
                String explanation=detail.getText().toString();assertTrue("Explain the first-receipt boundary",explanation.contains(language.equals("zh")?"首次接收":"first accepts"));
                String consequence=variant==2?(language.equals("zh")?"设置中查看":"Settings before sending"):variant==1?(language.equals("zh")?"批准计划":"Approve a plan"):(language.equals("zh")?"修改项目文件":"edit project files");
                assertTrue("Explain the saved preference's consequence or how to check it",explanation.contains(consequence));assertCompleteText(title);assertCompleteText(detail);
                Rect cueBounds=new Rect(),sendBounds=new Rect();cue.getDrawingRect(cueBounds);a.stage.offsetDescendantRectToMyCoords(cue,cueBounds);Button send=send(a,language);send.getDrawingRect(sendBounds);a.stage.offsetDescendantRectToMyCoords(send,sendBounds);assertTrue("Place the decision cue before Send",cueBounds.bottom<=sendBounds.top);
                assertTrue("Unknown settings do not prevent saving a share",send.isEnabled());reveal(a,title);assertSafe(a);
            });awaitFrame(scenario);scenario.onActivity(a->{assertCompleteAndVisible(a.stage.findViewWithTag("share-execution-title"));reveal(a,a.stage.findViewWithTag("share-execution-detail"));});awaitFrame(scenario);
            scenario.onActivity(a->assertCompleteAndVisible(a.stage.findViewWithTag("share-execution-detail")));capture(scenario,language,setting,"setting");scrollToSend(scenario,language);scenario.onActivity(a->{assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,setting,"send");
        }
    }
    static Context fixtureContext(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        float font="1".equals(InstrumentationRegistry.getArguments().getString("fontScale"))?1f:2f;
        assertEquals("Run at the requested font scale",font,context.getResources().getConfiguration().fontScale,0.01f);assertEquals("Run at a compact 320dp width",320,context.getResources().getConfiguration().screenWidthDp);return context;
    }
    static ActivityScenario<DemoShareEditorActivity> launch(Context context,String language,int setting){return ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("direct",setting!=1).putExtra("confirmed",setting!=2));}
    static void ready(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{awaitFrame(scenario);scenario.onActivity(a->{assertTrue(a.hasWindowFocus());assertSafe(a);assertTrue(a.sheet.getWidth()<=Ui.dp(a,320));});}
    static void awaitFrame(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{a.root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(a.hasWindowFocus()&&a.holder.getWidth()>0&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f&&(a.panel==null||a.panel.getTag(R.id.expand_animation)==null)){a.root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else a.root.postInvalidateOnAnimation();return true;
        }});a.root.invalidate();});assertTrue("Wait for actual native layout and sheet/panel animations",frame.await(3,TimeUnit.SECONDS));
    }
    static void scrollToSend(ActivityScenario<DemoShareEditorActivity> scenario,String language)throws Exception{scenario.onActivity(a->reveal(a,send(a,language)));awaitFrame(scenario);}
    static void reveal(DemoShareEditorActivity activity,View view){assertNotNull(view);view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true);}
    static Button send(DemoShareEditorActivity activity,String language){TextView view=findText(activity.stage,language.equals("zh")?"交给 Codex":"Hand off to Codex");assertTrue("The actual primary action is present",view instanceof Button);return (Button)view;}
    static TextView findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=findText(((ViewGroup)view).getChildAt(n),text);if(found!=null)return found;}return null;}
    static void assertCompleteText(TextView text){
        assertNotNull(text);Layout layout=text.getLayout();assertNotNull(layout);assertEquals("Render every character",text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int line=0;line<layout.getLineCount();line++)assertEquals("Do not ellipsize decision copy",0,layout.getEllipsisCount(line));assertTrue("All lines fit vertically",text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());
    }
    static void assertCompleteAndVisible(TextView text){
        assertCompleteText(text);DemoShareEditorActivity activity=(DemoShareEditorActivity)text.getContext();diagnostics(activity,"visibility");
        Rect visible=new Rect(),rootVisible=new Rect(),scrollVisible=new Rect();assertTrue(text.getGlobalVisibleRect(visible));assertTrue(activity.root.getGlobalVisibleRect(rootVisible));assertTrue(activity.scroll.getGlobalVisibleRect(scrollVisible));
        Rect bounds=screenBounds(text),intersection=new Rect(bounds);assertTrue("Intersect the control with the root viewport",intersection.intersect(rootVisible));assertTrue("Intersect the control with the scroll viewport",intersection.intersect(scrollVisible));assertTrue("Intersect the control with the physical screen",intersection.intersect(screen(activity)));
        assertEquals("The entire control must fit inside all actual viewports",bounds,intersection);assertEquals("Framework visibility must cover the entire control",bounds,visible);
    }
    static Rect screenBounds(View view){int[] location=new int[2];view.getLocationOnScreen(location);return new Rect(location[0],location[1],location[0]+view.getWidth(),location[1]+view.getHeight());}
    static Rect screen(DemoShareEditorActivity activity){android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);assertEquals("Physical screen is 320dp wide",320f,metrics.widthPixels/metrics.density,0.01f);assertEquals("Physical screen is 640dp tall",640f,metrics.heightPixels/metrics.density,0.01f);return new Rect(0,0,metrics.widthPixels,metrics.heightPixels);}
    static void diagnostics(DemoShareEditorActivity activity,String phase){
        Button send=send(activity,L.chinese()?"zh":"en");Rect visible=new Rect();boolean shown=send.getGlobalVisibleRect(visible);
        String line="EditorProbe "+phase+" root="+activity.root.getHeight()+" holder="+activity.holder.getHeight()+" sheet="+activity.sheet.getHeight()+" scroll="+activity.scroll.getHeight()+" stage="+activity.stage.getHeight()+" scrollY="+activity.scroll.getScrollY()+" sendVisible="+shown+":"+visible+" sendScreen="+screenBounds(send)+" rootScreen="+screenBounds(activity.root)+" viewport="+screenBounds(activity.scroll)+" screen="+screen(activity)+"\n";
        android.os.Bundle status=new android.os.Bundle();status.putString("stream",line);InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
    }
    static void assertSafe(DemoShareEditorActivity activity){assertNull(activity.incoming);assertEquals(1,activity.initializationCloses);assertEquals(0,activity.startAttempts);assertEquals(0,activity.networkAttempts);assertEquals(0,activity.saveAttempts);assertEquals(0,activity.pairingAttempts);assertEquals(0,activity.submitAttempts);assertFalse(activity.sent);}
    static void capture(ActivityScenario<DemoShareEditorActivity> scenario,String language,int setting,String stage)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        CountDownLatch frames=new CountDownLatch(1);scenario.onActivity(a->{a.root.postOnAnimation(()->{a.root.postOnAnimation(frames::countDown);a.root.invalidate();});a.root.invalidate();});assertTrue("Wait two native frame callbacks before capturing",frames.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->diagnostics(a,"capture-"+stage));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();java.io.File base=context.getExternalFilesDir(null);assertNotNull(base);java.io.File directory=new java.io.File(base,"ui-probe-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String name="share-editor-"+language+"-"+(setting==2?"unknown":setting==1?"plan":"direct")+"-"+stage+"-motion-"+(Ui.motionEnabled(context)?"on":"off")+(context.getResources().getConfiguration().fontScale==1f?"-normal-text":"")+".png";
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,name))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
}
