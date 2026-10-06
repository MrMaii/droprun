package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import static app.droprun.TaskPreviewFeedbackTest.find;
import static app.droprun.TaskPreviewFeedbackTest.frames;
import static app.droprun.TaskPreviewFeedbackTest.screenBounds;
import static org.junit.Assert.*;

/** Private draft: two guarded memory font2 windows, no capture or business activation.
 * Task baseline b0e3b3aeb8c62f585e561d7d738dbf7dae2aa083c2dcf2c591ac07dcbf3646fe;
 * only109 proposal c74e76f474b79fa7bd0954b48564a01d474b68c36467deaf54830bcabbc49e4e.
 * Normal4 PNGs are a separate existing DeliveryStableCaptureTest invocation.
 */
public class TaskReadingHierarchyTest {
    static boolean unresolvedLifetime;
    static final String VERSION=DeliveryStableCaptureTest.VERSION;
    @Test public void readingAndPlanEligibilityRemainReachableAtDoubleFont()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("taskReadingProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        assertFalse("No next method after an unknown window",unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        assertFalse(context.getPackageManager().getActivityInfo(new ComponentName(context,DemoTaskPreviewActivity.class),0).exported);
        String languageBefore=L.chinese()?"zh":"en";boolean[] destroyed={true};
        try{
            for(String[] scene:new String[][]{{"en","light"},{"zh","dark"}}){
                String language=scene[0],theme=scene[1],identity=language+"|"+theme+"|font2";ActivityScenario<DemoTaskPreviewActivity> scenario=null;Throwable failure=null;DemoTaskPreviewActivity[] retained={null};
                try{
                    assertTrue(destroyed[0]);Intent intent=new Intent(context,DemoTaskPreviewActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("snapshotVersionProbe",VERSION);
                    destroyed[0]=false;unresolvedLifetime=true;event(identity,"launch-attempt");scenario=ActivityScenario.launch(intent);event(identity,"returned-handle");frames(scenario);
                    String[] original={null};View[] focus={null};scenario.onActivity(a->{retained[0]=a;safe(a,language,theme,2f);original[0]=a.sample.toString();focus[0]=a.getWindow().getDecorView().findFocus();assertReading(a);});event(identity,"entered");
                    reachReading(scenario);scenario.onActivity(a->{assertSame(focus[0],a.getWindow().getDecorView().findFocus());assertEquals(original[0],a.sample.toString());});
                    scenario.onActivity(a->{a.expanded.add(L.t("Full report & evidence","完整报告与证据"));a.snapshot="";a.render();});frames(scenario);
                    scenario.onActivity(a->{TextView full=find(a.body,ReportText.render(a.sample.optString("report")).toString());assertNotNull(full);assertTrue(full.isTextSelectable());revealLine(full,full.getLayout().getLineCount()-1);});frames(scenario);
                    scenario.onActivity(a->{TextView full=find(a.body,ReportText.render(a.sample.optString("report")).toString());assertSafeRect(a,lineBounds(full,full.getLayout().getLineCount()-1));assertEquals(original[0],a.sample.toString());});event(identity,"reading-completed");
                    planEligibility(scenario,original[0]);event(identity,"plan-eligibility-completed");
                    scenario.onActivity(a->{safe(a,language,theme,2f);assertReading(a);assertEquals(original[0],a.sample.toString());});event(identity,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,identity,retained[0]);}
            }
        }finally{if(destroyed[0])L.language(languageBefore);}
    }
    static void safe(DemoTaskPreviewActivity a,String language,String theme,float font){
        assertEquals(font,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertEquals(0f,a.body.getTranslationX(),0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(language.equals("zh"),L.chinese());
        TaskPreviewFeedbackTest.assertSafe(a);assertEquals(0,a.thumbnailOpens);assertTrue(a.store.prefs.getAll().isEmpty());assertEquals(2,a.global.size());
        assertFalse(a.busy);assertFalse(a.loading);assertNull(a.followupDialog);assertNull(a.actionErrorDialog);assertNull(a.localRemovalDialog);
        assertEquals(L.t("UI probe · memory only · no work sent","界面验证 · 仅内存 · 未发送任务"),a.notice.getText().toString());assertTextComplete(a.notice);
        assertEquals("completed",a.sample.optString("status"));assertEquals(VERSION,a.sample.optString("preview_version"));assertEquals(DeliveryStableCaptureTest.URL,a.sample.optString("preview_url"));assertEquals("snapshot",a.sample.optString("preview_kind"));assertEquals("ready",a.sample.optString("preview_status"));assertEquals(Long.MAX_VALUE,a.sample.optLong("preview_expires_at"));
    }
    static void assertReading(DemoTaskPreviewActivity a){
        TextView label=find(a.body,L.t("The result","交付结果")),summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")));assertNotNull(label);assertNotNull(summary);ViewGroup reading=(ViewGroup)label.getParent();
        assertNotSame(a.body,reading);assertSame(a.body,reading.getParent());assertSame(reading,summary.getParent());assertEquals(0,reading.getPaddingLeft());assertEquals(0,reading.getPaddingRight());assertEquals(0,reading.getPaddingTop());assertEquals(0,reading.getPaddingBottom());assertEquals(0f,reading.getElevation(),0f);assertNull(reading.getBackground());View viewport=(View)a.body.getParent().getParent();assertTrue(viewport.getBackground() instanceof GradientDrawable);int[] colors=((GradientDrawable)viewport.getBackground()).getColors();assertNotNull(colors);for(int color:colors)assertEquals(255,color>>>24);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,12,a.getResources().getDisplayMetrics()),label.getTextSize(),.01f);assertEquals(Ui.MUTED,label.getCurrentTextColor());if(Build.VERSION.SDK_INT>=28)assertTrue(label.isAccessibilityHeading());
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,18,a.getResources().getDisplayMetrics()),summary.getTextSize(),.01f);assertEquals(Ui.TEXT,summary.getCurrentTextColor());assertEquals(TaskPresentation.resultSummary(a.sample.optString("report")),summary.getText().toString());assertNull(summary.getEllipsize());assertTextComplete(summary);
        assertEquals(a.body.getWidth(),reading.getWidth());assertEquals(screenBounds(a.body).left,screenBounds(summary).left);assertEquals(screenBounds(a.body).right,screenBounds(summary).right);
        for(String text:new String[]{L.t("Preview","预览"),L.t("Open preview","打开预览"),L.t("Screenshots & delivery files","截图与交付文件")})assertSame(reading,find(a.body,text).getParent());
        for(String text:new String[]{L.t("Follow up","继续追问"),L.t("Full report & evidence","完整报告与证据"),L.t("Snapshot version","快照版本"),L.t("Delete record & material","删除记录与材料")}){View node=find(a.body,text);assertNotNull(node);for(android.view.ViewParent parent=node.getParent();parent instanceof View;parent=parent.getParent())assertNotSame(reading,parent);}
        TaskPreviewFeedbackTest.assertActions(a,true,false);assertNotNull(find(a.body,L.t("Fixed snapshot from this handoff.","本次交付的固定快照。")));
    }
    static void reachReading(ActivityScenario<DemoTaskPreviewActivity> scenario)throws Exception{
        scenario.onActivity(a->{TextView summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")));revealLine(summary,0);});frames(scenario);scenario.onActivity(a->{TextView summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")));assertSafeRect(a,lineBounds(summary,0));revealLine(summary,summary.getLayout().getLineCount()-1);});frames(scenario);
        scenario.onActivity(a->{TextView summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")));assertSafeRect(a,lineBounds(summary,summary.getLayout().getLineCount()-1));});
        for(String value:new String[]{L.t("Fixed snapshot from this handoff.","本次交付的固定快照。"),L.t("Open preview","打开预览"),L.t("Screenshots & delivery files","截图与交付文件"),L.t("Follow up","继续追问")}){
            scenario.onActivity(a->{TextView text=find(a.body,value);assertNotNull(text);text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});frames(scenario);
            scenario.onActivity(a->{TextView text=find(a.body,value);assertTextComplete(text);assertSafeRect(a,screenBounds(text));if(text instanceof Button){assertTrue(text.isEnabled());assertTrue(text.isFocusable());assertTrue(text.getWidth()>=Ui.dp(a,48));assertTrue(text.getHeight()>=Ui.dp(a,48));}assertEquals(0,a.forbiddenActions.get());});
        }
    }
    static void planEligibility(ActivityScenario<DemoTaskPreviewActivity> scenario,String original)throws Exception{
        scenario.onActivity(a->{try{a.expanded.clear();a.sample.put("status","awaiting_plan_approval").put("report","").put("plan_report",L.t("Memory plan only. No command or edit will run.","仅内存计划；不会运行命令或修改。")).put("plan_version","memory-plan-version").put("preview_status","").put("preview_url","");a.snapshot="";a.render();assertNull(find(a.body,L.t("The result","交付结果")));}catch(org.json.JSONException error){throw new AssertionError(error);}});frames(scenario);
        for(String value:new String[]{L.t("Approve this plan","批准这个计划"),L.t("Reject plan","拒绝计划")}){
            scenario.onActivity(a->{TextView text=find(a.body,value);assertTrue(text instanceof Button);assertTrue(text.isEnabled());text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});frames(scenario);
            scenario.onActivity(a->{TextView text=find(a.body,value);assertTextComplete(text);assertSafeRect(a,screenBounds(text));assertTrue(text.getWidth()>=Ui.dp(a,48));assertTrue(text.getHeight()>=Ui.dp(a,48));assertEquals(0,a.forbiddenActions.get());});
        }
        scenario.onActivity(a->{a.busy=true;a.snapshot="";a.render();for(String value:new String[]{L.t("Approve this plan","批准这个计划"),L.t("Reject plan","拒绝计划")})assertFalse(find(a.body,value).isEnabled());assertEquals(0,a.forbiddenActions.get());a.busy=false;try{a.sample=new JSONObject(original);}catch(org.json.JSONException error){throw new AssertionError(error);}a.snapshot="";a.render();});frames(scenario);
    }
    static void assertTextComplete(TextView text){assertNotNull(text);Layout layout=text.getLayout();assertNotNull(layout);assertTrue(layout.getLineCount()>0);assertEquals(text.getText().length(),layout.getLineEnd(layout.getLineCount()-1));for(int line=0;line<layout.getLineCount();line++)assertEquals(0,layout.getEllipsisCount(line));}
    static Rect localLine(TextView text,int line){assertTextComplete(text);Layout layout=text.getLayout();return new Rect(text.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(line)),text.getCompoundPaddingTop()+layout.getLineTop(line),text.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(line)),text.getCompoundPaddingTop()+layout.getLineBottom(line));}
    static void revealLine(TextView text,int line){Rect rect=localLine(text,line);assertTrue(new Rect(0,0,text.getWidth(),text.getHeight()).contains(rect));text.requestRectangleOnScreen(rect,true);}
    static Rect lineBounds(TextView text,int line){Rect rect=localLine(text,line);int[] origin=new int[2];text.getLocationOnScreen(origin);rect.offset(origin[0],origin[1]);return rect;}
    static void assertSafeRect(DemoTaskPreviewActivity a,Rect target){Rect root=new Rect(),scroll=new Rect(),safe=new Rect();assertTrue(((View)a.body.getParent()).getGlobalVisibleRect(root));View viewport=(View)a.body.getParent().getParent();assertTrue(viewport.getGlobalVisibleRect(scroll));scroll.top+=viewport.getPaddingTop();scroll.bottom-=viewport.getPaddingBottom();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);assertTrue(safe.intersect(root));assertTrue(safe.intersect(scroll));assertTrue(safe.contains(target));}
    static void closeKnown(ActivityScenario<DemoTaskPreviewActivity> scenario,Throwable failure,boolean[] destroyed,String identity,DemoTaskPreviewActivity retained)throws Throwable{
        if(scenario==null)return;try{scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());destroyed[0]=true;unresolvedLifetime=false;event(identity,"DESTROYED");if(retained!=null){assertEquals(0,retained.forbiddenActions.get());assertEquals(0,retained.thumbnailOpens);Bundle status=new Bundle();status.putString("stream","TASK_READING_COUNTERS\t"+identity+"\tforbidden="+retained.forbiddenActions.get()+"\tthumbnail_opens="+retained.thumbnailOpens+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}}catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void event(String identity,String value){Bundle status=new Bundle();status.putString("stream","TASK_READING_LIFETIME\t"+identity+"\t"+value+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
