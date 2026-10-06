package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static app.droprun.ShareEditorTest.*;
import static org.junit.Assert.*;

/** Draft: eight guarded memory windows; only a readonly notice is replaced/revealed. */
public class ShareExecutionContextTest {
    static boolean unresolvedLifetime;
    @Test public void readonlyExecutionContextKeepsCompleteCopyAndReachability()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("shareExecutionContextProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        assertFalse("No next method after an unresolved probe window",unresolvedLifetime);
        float previous=DemoShareEditorActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean[] destroyed={true};
        try{
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})for(float scale:new float[]{1f,2f}){
                DemoShareEditorActivity.hierarchyFontScale=scale;String identity=language+"|"+theme+"|font"+(int)scale;
                ActivityScenario<DemoShareEditorActivity> scenario=null;Throwable failure=null;DemoShareEditorActivity[] retained={null};
                try{
                    scenario=launchProbe(language,theme,destroyed,identity);ready(scenario);
                    String[][] original={null};View[][] controls={null};
                    scenario.onActivity(a->{safe(a);retained[0]=a;assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);original[0]=values(a);controls[0]=new View[]{a.note,a.gaugeText,send(a,language),a.back,closeButton(a)};});event(identity,"entered");
                    for(int variant=0;variant<3;variant++){
                        final int setting=variant;
                        scenario.onActivity(a->replaceNotice(a,setting));awaitFrame(scenario);
                        scenario.onActivity(a->{assertNotice(a,setting,language);assertUnchanged(a,original[0],controls[0],language);reveal(a,title(a));});awaitFrame(scenario);
                        scenario.onActivity(a->{assertNotice(a,setting,language);assertCompleteText(title(a));assertSafeBounds(a,screenBounds(title(a)));revealTail(detail(a));});awaitFrame(scenario);
                        scenario.onActivity(a->{assertCompleteText(detail(a));assertSafeBounds(a,tailBounds(detail(a)));assertUnchanged(a,original[0],controls[0],language);reveal(a,send(a,language));});awaitFrame(scenario);
                        scenario.onActivity(a->{Button send=send(a,language);assertCompleteText(send);assertSafeBounds(a,screenBounds(send));assertTrue(send.getWidth()>=Ui.dp(a,48));assertTrue(send.getHeight()>=Ui.dp(a,48));assertTrue(send.isEnabled());assertTrue(send.isFocusable());assertTrue(send.isClickable());assertUnchanged(a,original[0],controls[0],language);counters(identity,a);});
                        event(identity,"variant-completed-"+setting);
                    }
                    event(identity,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,identity,retained[0]);}
            }
        }finally{if(destroyed[0]){DemoShareEditorActivity.hierarchyFontScale=previous;L.language(languageBefore);}}
    }
    static ActivityScenario<DemoShareEditorActivity> launchProbe(String language,String theme,boolean[] destroyed,String identity)throws android.content.pm.PackageManager.NameNotFoundException{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);
        assertFalse("Existing fixture must remain nonexported",target.getPackageManager().getActivityInfo(new ComponentName(target,DemoShareEditorActivity.class),0).exported);
        Intent intent=new Intent(target,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("materialReview",true).putExtra("compactHeight",true).putExtra("direct",true).putExtra("confirmed",true);
        assertTrue("No next launch after unresolved lifetime",destroyed[0]);destroyed[0]=false;unresolvedLifetime=true;
        event(identity,"launch-attempt");ActivityScenario<DemoShareEditorActivity> scenario=ActivityScenario.launch(intent);event(identity,"returned-handle");return scenario;
    }
    static void replaceNotice(DemoShareEditorActivity a,int variant){
        safe(a);View current=a.stage.findViewWithTag("share-execution-setting");assertNotNull(current);ViewGroup parent=(ViewGroup)current.getParent();int index=parent.indexOfChild(current);assertTrue(index>=0);ViewGroup.LayoutParams params=current.getLayoutParams();View focused=a.getWindow().getDecorView().findFocus();
        a.direct=variant!=1;a.confirmed=variant!=2;View replacement=a.executionSettingNotice();parent.removeViewAt(index);parent.addView(replacement,index,params);
        assertSame("Notice replacement never replaces the focused editor control",focused,a.getWindow().getDecorView().findFocus());safe(a);
    }
    static TextView title(DemoShareEditorActivity a){return a.stage.findViewWithTag("share-execution-title");}
    static TextView detail(DemoShareEditorActivity a){return a.stage.findViewWithTag("share-execution-detail");}
    static void assertNotice(DemoShareEditorActivity a,int variant,String language){
        LinearLayout group=a.stage.findViewWithTag("share-execution-setting");assertNotNull(group);assertEquals(2,group.getChildCount());assertSame(title(a),group.getChildAt(0));assertSame(detail(a),group.getChildAt(1));assertFalse(group.isClickable());assertFalse(group.isFocusable());
        boolean confirmed=variant!=2;int x=Ui.dp(a,confirmed?4:14),y=Ui.dp(a,confirmed?8:10);assertEquals(x,group.getPaddingLeft());assertEquals(x,group.getPaddingRight());assertEquals(y,group.getPaddingTop());assertEquals(y,group.getPaddingBottom());
        if(confirmed)assertNull("Saved execution context is plain readonly copy, not an input card",group.getBackground());
        else{assertTrue(group.getBackground() instanceof GradientDrawable);GradientDrawable shape=(GradientDrawable)group.getBackground();assertNotNull(shape.getColor());assertEquals(Ui.SURFACE_2,shape.getColor().getDefaultColor());}
        String heading=variant==2?L.t("Execution setting not confirmed","执行设置尚未确认"):variant==1?L.t("Saved setting · Plan review","已保存设置 · 先看计划"):L.t("Saved setting · Direct execution","已保存设置 · 直接执行");
        String explanation=variant==2?L.t("Check DropRun Settings before sending.","发送前，请在 DropRun 设置中查看。"):variant==1?L.t("Approve a plan before edits begin.","批准计划后才开始修改。"):L.t("Can edit project files and run commands.","可修改项目文件并运行命令。");explanation+=" "+L.t("Your Relay's setting applies when it first accepts this handoff.","Relay 首次接收交办时采用当时的设置。");
        assertEquals(heading,title(a).getText().toString());assertEquals(explanation,detail(a).getText().toString());assertEquals(confirmed?Ui.TEXT:Ui.AMBER,title(a).getCurrentTextColor());assertEquals(Ui.MUTED,detail(a).getCurrentTextColor());
        for(TextView text:new TextView[]{title(a),detail(a)}){assertFalse(text.isClickable());assertFalse(text.isFocusable());assertFalse(text.isTextSelectable());AccessibilityNodeInfo node=text.createAccessibilityNodeInfo();try{assertFalse(node.isClickable());assertFalse(node.isFocusable());assertEquals(text.getText().toString(),String.valueOf(node.getText()));}finally{node.recycle();}}
        assertTrue("Execution copy remains before Send in the same column",group.getBottom()<=send(a,language).getTop());safe(a);
    }
    static String[] values(DemoShareEditorActivity a){return new String[]{a.selected,a.model,a.effort,a.shared,a.attachments.toString(),a.draft,a.note.getText().toString()};}
    static void assertUnchanged(DemoShareEditorActivity a,String[] original,View[] controls,String language){assertArrayEquals(original,values(a));assertSame(controls[0],a.note);assertSame(controls[1],a.gaugeText);assertSame(controls[2],send(a,language));assertSame(controls[3],a.back);assertSame(controls[4],closeButton(a));safe(a);}
    static View closeButton(DemoShareEditorActivity a){View button=description(a.root,L.t("Close","关闭"));assertNotNull(button);return button;}
    static View description(View root,String label){CharSequence value=root.getContentDescription();if(value!=null&&label.contentEquals(value))return root;if(root instanceof ViewGroup)for(int n=0;n<((ViewGroup)root).getChildCount();n++){View found=description(((ViewGroup)root).getChildAt(n),label);if(found!=null)return found;}return null;}
    static Rect localTail(TextView text){assertCompleteText(text);Layout layout=text.getLayout();int last=layout.getLineCount()-1;return new Rect(text.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(last)),text.getCompoundPaddingTop()+layout.getLineTop(last),text.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(last)),text.getCompoundPaddingTop()+layout.getLineBottom(last));}
    static void revealTail(TextView text){assertTrue("Tail rectangle is within naturally measured text",new Rect(0,0,text.getWidth(),text.getHeight()).contains(localTail(text)));text.requestRectangleOnScreen(localTail(text),true);}
    static Rect tailBounds(TextView text){Rect result=localTail(text);int[] point=new int[2];text.getLocationOnScreen(point);result.offset(point[0],point[1]);return result;}
    static void assertSafeBounds(DemoShareEditorActivity a,Rect target){Rect visibleRoot=new Rect(),visibleScroll=new Rect(),safeWindow=new Rect();assertTrue(a.root.getGlobalVisibleRect(visibleRoot));assertTrue(a.scroll.getGlobalVisibleRect(visibleScroll));a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safeWindow);assertTrue(safeWindow.intersect(visibleRoot));assertTrue(safeWindow.intersect(visibleScroll));assertTrue("Complete target fits the actual scroll/root/window safe frame",safeWindow.contains(target));}
    static void guardCounters(DemoShareEditorActivity a){assertSafe(a);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertEquals(0,a.keyboardBypasses);}
    static void safe(DemoShareEditorActivity a){guardCounters(a);assertTrue(a.probeReady);assertTrue(a.getIntent().getBooleanExtra("modelDisclosure",false));assertTrue(a.getIntent().getBooleanExtra("materialReview",false));assertTrue(a.getIntent().getBooleanExtra("compactHeight",false));assertTrue(DemoShareEditorActivity.hierarchyFontScale==1f||DemoShareEditorActivity.hierarchyFontScale==2f);assertEquals(1,a.step);assertFalse(a.receiving);TextView marker=a.sheet.findViewWithTag("hierarchy-marker");assertNotNull(marker);assertEquals(L.t("UI probe · memory only","界面探针 · 仅内存"),marker.getText().toString());assertCompleteText(marker);}
    static void counters(String identity,DemoShareEditorActivity a){guardCounters(a);Bundle status=new Bundle();status.putString("stream","SHARE_EXECUTION_CONTEXT_COUNTERS\t"+identity+"\tforbidden="+a.forbiddenActions+"\tnetwork="+a.networkAttempts+"\tsave="+a.saveAttempts+"\tpairing="+a.pairingAttempts+"\tsubmit="+a.submitAttempts+"\tstart="+a.startAttempts+"\tcheckpoint_attempts="+a.checkpointAttempts+"\tcheckpoint_noops="+a.checkpointNoops+"\tkeyboard_bypasses="+a.keyboardBypasses+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static void closeKnown(ActivityScenario<DemoShareEditorActivity> scenario,Throwable failure,boolean[] destroyed,String identity,DemoShareEditorActivity retained)throws Throwable{
        if(scenario==null)return;
        try{scenario.close();assertEquals("Known handle must reach DESTROYED before another launch or restoration",Lifecycle.State.DESTROYED,scenario.getState());destroyed[0]=true;unresolvedLifetime=false;event(identity,"DESTROYED");if(retained!=null)counters(identity,retained);}
        catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void event(String identity,String name){Bundle status=new Bundle();status.putString("stream","SHARE_EXECUTION_CONTEXT_LIFETIME\t"+identity+"\t"+name+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
