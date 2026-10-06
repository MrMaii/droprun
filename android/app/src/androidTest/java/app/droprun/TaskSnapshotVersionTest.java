package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.concurrent.TimeUnit;
import static app.droprun.TaskPreviewFeedbackTest.*;
import static org.junit.Assert.*;

/** Local disclosure over an in-memory version; never opens links/files or accesses the clipboard. */
public class TaskSnapshotVersionTest {
    static final String VERSION="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    @Test public void fullVersionStaysInAnAccessibleDisclosureAcrossRecreation()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})for(float scale:new float[]{1f,2f})
            try(ActivityScenario<DemoTaskPreviewActivity> scenario=launchVersion(language,theme,scale)){
                frames(scenario);reveal(scenario,false);capture(scenario,language,theme,scale,"collapsed");
                scenario.onActivity(a->{
                    assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);
                    ViewGroup card=resultCard(a);assertFalse("The primary result explanation must not expose a 64-character technical version",visibleVersion(card));
                    assertNotNull(find(card,L.t("Fixed snapshot from this handoff.","本次交付的固定快照。")));
                    ViewGroup group=versionGroup(a);assertSame(a.body,group.getParent());assertOutsideSurface(card,group);
                    assertEquals("One version disclosure",1,countLabels(a.body,versionTitle()));
                    assertTrue(a.body.indexOfChild(find(a.body,L.t("Follow up","继续追问")))<a.body.indexOfChild(group));
                    View report=(View)find(a.body,L.t("Full report & evidence","完整报告与证据")).getParent().getParent();assertTrue(a.body.indexOfChild(group)<a.body.indexOfChild(report));
                    assertDisclosure(a,false);assertPayload(a);assertActions(a,true,false);
                });
                revealHeader(scenario);scenario.onActivity(a->{assertHeaderVisible(a);header(a).performClick();});settleDisclosure(scenario,true);
                reveal(scenario,true);scenario.onActivity(a->{assertDisclosure(a,true);assertFullValue(a);assertPayload(a);});capture(scenario,language,theme,scale,"expanded");
                scenario.recreate();frames(scenario);reveal(scenario,true);scenario.onActivity(a->{assertDisclosure(a,true);assertFullValue(a);assertPayload(a);});
                revealHeader(scenario);scenario.onActivity(a->{assertHeaderVisible(a);header(a).performClick();});settleDisclosure(scenario,false);
                scenario.recreate();frames(scenario);revealHeader(scenario);scenario.onActivity(a->{assertDisclosure(a,false);assertHeaderVisible(a);assertPayload(a);});
            }
    }
    static ActivityScenario<DemoTaskPreviewActivity> launchVersion(String language,String theme,float scale){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        return ActivityScenario.launch(new Intent(context,scale==1f?DemoTaskPreviewNormalActivity.class:DemoTaskPreviewActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("snapshotVersionProbe",VERSION));
    }
    static String versionTitle(){return L.t("Snapshot version","快照版本");}
    static ViewGroup resultCard(DemoTaskPreviewActivity a){TextView label=find(a.body,L.t("The result","交付结果"));assertNotNull(label);return (ViewGroup)label.getParent();}
    static ViewGroup versionGroup(DemoTaskPreviewActivity a){TextView label=find(a.body,versionTitle());assertNotNull("Version disclosure exists",label);return (ViewGroup)label.getParent().getParent();}
    static View header(DemoTaskPreviewActivity a){return versionGroup(a).getChildAt(0);}
    static TextView value(DemoTaskPreviewActivity a){return (TextView)versionGroup(a).getChildAt(1);}
    static int countLabels(View view,String title){int count=view instanceof TextView&&title.contentEquals(((TextView)view).getText())?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=countLabels(((ViewGroup)view).getChildAt(i),title);return count;}
    static boolean visibleVersion(View view){if(view instanceof TextView&&view.isShown()&&((TextView)view).getText().toString().contains(VERSION))return true;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(visibleVersion(((ViewGroup)view).getChildAt(i)))return true;return false;}
    static TextView previewExplanation(DemoTaskPreviewActivity a){ViewGroup card=resultCard(a);TextView label=find(card,L.t("Preview","预览"));assertNotNull(label);return (TextView)card.getChildAt(card.indexOfChild(label)+1);}
    static void assertPayload(DemoTaskPreviewActivity a){assertEquals(VERSION,a.sample.optString("preview_version"));assertEquals(URL,a.sample.optString("preview_url"));assertEquals("snapshot",a.sample.optString("preview_kind"));assertEquals("ready",a.sample.optString("preview_status"));assertEquals(Long.MAX_VALUE,a.sample.optLong("preview_expires_at"));assertSafe(a);}
    static void assertDisclosure(DemoTaskPreviewActivity a,boolean open){
        TextView value=value(a);assertEquals(open?View.VISIBLE:View.GONE,value.getVisibility());assertEquals(open,a.expanded.contains(versionTitle()));assertEquals(open,header(a).getTag());assertNull(value.getTag(R.id.expand_animation));assertEquals(1f,value.getAlpha(),0f);
        AccessibilityNodeInfo node=header(a).createAccessibilityNodeInfo();try{assertEquals(versionTitle()+(open?L.t(", expanded",", 已展开"):L.t(", collapsed",", 已折叠")),String.valueOf(node.getContentDescription()));assertTrue(node.isClickable());assertTrue(node.isFocusable());assertTrue(node.isEnabled());}finally{node.recycle();}
    }
    static void assertHeaderVisible(DemoTaskPreviewActivity a){View header=header(a);assertTrue(header.getHeight()>=Ui.dp(a,48));TaskThumbnailTest.assertVisible(a,header);assertCompleteAndVisible(a,find(header,versionTitle()));}
    static void assertFullValue(DemoTaskPreviewActivity a){
        TextView value=value(a);assertEquals(VERSION+"\n",value.getText().toString());assertTrue(value.isTextSelectable());assertCompleteAndVisible(a,value);
        AccessibilityNodeInfo node=value.createAccessibilityNodeInfo();try{assertEquals(VERSION+"\n",String.valueOf(node.getText()));}finally{node.recycle();}
    }
    static void reveal(ActivityScenario<DemoTaskPreviewActivity> scenario,boolean expanded)throws Exception{scenario.onActivity(a->{TextView target=expanded?value(a):previewExplanation(a);target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);});frames(scenario);scenario.onActivity(a->assertCompleteAndVisible(a,expanded?value(a):previewExplanation(a)));}
    static void revealHeader(ActivityScenario<DemoTaskPreviewActivity> scenario)throws Exception{scenario.onActivity(a->{View target=header(a);target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);});frames(scenario);}
    static void settleDisclosure(ActivityScenario<DemoTaskPreviewActivity> scenario,boolean open)throws Exception{
        CountDownLatch settled=new CountDownLatch(1);scenario.onActivity(a->{a.body.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){TextView content=value(a);if(content.getTag(R.id.expand_animation)==null&&content.getVisibility()==(open?View.VISIBLE:View.GONE)&&content.getAlpha()==1f){a.body.getViewTreeObserver().removeOnPreDrawListener(this);a.body.postOnAnimation(()->a.body.postOnAnimation(settled::countDown));}else a.body.postInvalidateOnAnimation();return true;}});a.body.invalidate();});assertTrue("Wait for the existing disclosure's final native layout",settled.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    static void capture(ActivityScenario<DemoTaskPreviewActivity> scenario,String language,String theme,float scale,String state)throws Exception{
        String phase=InstrumentationRegistry.getArguments().getString("captureSnapshotUi","");if(phase.isEmpty()||(scale==2f&&state.equals("collapsed")))return;assertTrue(phase.equals("red")||phase.equals("green")||phase.equals("green-marked"));
        JSONObject[] metadata={null};ViewGroup[] decor={null};android.widget.FrameLayout[] marker={null};
        try{
            scenario.onActivity(a->{try{
                TextView target=state.equals("expanded")?value(a):previewExplanation(a);assertCompleteAndVisible(a,target);assertPayload(a);
                metadata[0]=new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("state",state).put("synthetic_version",VERSION).put("main_explanation",previewExplanation(a).getText().toString()).put("main_contains_version",visibleVersion(resultCard(a))).put("target_bounds",screenBounds(target).toShortString()).put("target_text",target.getText().toString()).put("target_selectable",target.isTextSelectable()).put("version_disclosure_exists",find(a.body,versionTitle())!=null).put("forbidden_actions",a.forbiddenActions.get());
                TextView label=new TextView(a);label.setText(language.equals("zh")?"UI 探针 · 内存版本 · 未执行任务":"UI probe · memory version · no task");label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,9);label.setSingleLine(true);label.setGravity(android.view.Gravity.CENTER);label.setTextColor(0xFFFFFFFF);label.setBackgroundColor(0xFF17251D);
                marker[0]=new android.widget.FrameLayout(a);marker[0].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);marker[0].addView(label,new android.widget.FrameLayout.LayoutParams(-1,-1));decor[0]=(ViewGroup)a.getWindow().getDecorView();int left=Ui.dp(a,20),width=decor[0].getWidth()-2*left,height=Ui.dp(a,14),top=Ui.dp(a,28);marker[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));marker[0].layout(left,top,left+width,top+height);assertNotNull(label.getLayout());assertEquals(label.length(),label.getLayout().getLineEnd(0));assertEquals(0,label.getLayout().getEllipsisCount(0));assertTrue(label.getLayout().getLineWidth(0)<=label.getWidth()-label.getCompoundPaddingLeft()-label.getCompoundPaddingRight());assertTrue(label.getHeight()>=label.getLayout().getHeight()+label.getCompoundPaddingTop()+label.getCompoundPaddingBottom());metadata[0].put("marker_text_size_px",label.getTextSize()).put("marker_height",label.getHeight());decor[0].getOverlay().add(marker[0]);assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(target)));
            }catch(Exception error){throw new AssertionError(error);}});
            frames(scenario);InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/task-snapshot-version-"+phase+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="snapshot-"+language+"-"+theme+"-font"+(int)scale+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());
            Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{screenshot.recycle();}
        }finally{scenario.onActivity(a->{if(decor[0]!=null&&marker[0]!=null)decor[0].getOverlay().remove(marker[0]);});}
    }
}
