package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static app.droprun.ShareEditorTest.*;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static app.droprun.ShareSettingsHierarchyTest.visible;
import static org.junit.Assert.*;

/** Window height and feedback visibility, using only the existing guarded memory editor. */
public class ShareCompactHeightTest {
    String phase(){return InstrumentationRegistry.getArguments().getString("captureShareCompactUi","");}
    @Test public void compactSheetShowsFeedbackAndLargerTextRemainsReachable()throws Exception{
        float previous=DemoShareEditorActivity.hierarchyFontScale;
        boolean red=phase().equals("red"),probe=phase().equals("probe");List<String> problems=new ArrayList<>();
        try{for(String language:red||probe?new String[]{"en"}:new String[]{"en","zh"})for(String theme:red||probe?new String[]{"light"}:new String[]{"light","dark"})for(float scale:red?new float[]{1f}:probe?new float[]{2f}:new float[]{1f,2f}){
            DemoShareEditorActivity.hierarchyFontScale=scale;
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
            try(ActivityScenario<DemoShareEditorActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("compactHeight",true))){
                ready(scenario);scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);a.note.setText(language.equals("zh")?"这条留言只在内存。":"This note stays in memory.");a.scroll.scrollTo(0,0);});awaitFrame(scenario);
                JSONArray[] measurements={null};scenario.onActivity(a->measurements[0]=measureCaps(a,problems));
                capture(scenario,language,theme,scale,"initial",measurements[0]);
                scenario.onActivity(a->{
                    assertEquals("Initial means the actual top of the form",0,a.scroll.getScrollY());assertTrue(a.hasWindowFocus());assertFalse(a.panelOpen);assertSafe(a);
                    View header=a.back.getParent() instanceof View?(View)a.back.getParent():null;assertNotNull(header);visible(a.back);assertTrue(a.back.getWidth()>=Ui.dp(a,48));assertTrue(a.back.getHeight()>=Ui.dp(a,48));
                    View marker=a.root.findViewWithTag("hierarchy-marker");visible(marker);assertFalse("Capture marker must not cover a header control",Rect.intersects(bounds(marker),bounds(header)));
                    Rect root=bounds(a.root),sheet=bounds(a.sheet);assertTrue(sheet.top>=root.top+a.root.getPaddingTop());assertTrue(sheet.bottom<=root.bottom-a.root.getPaddingBottom());
                    TextView send=a.stage.findViewWithTag("share-send");assertTrue(send.getHeight()>=Ui.dp(a,48));assertTrue(send.getWidth()>=Ui.dp(a,48));AccessibilityNodeInfo node=send.createAccessibilityNodeInfo();try{assertTrue(node.isEnabled());assertTrue(node.isClickable());assertTrue(node.isFocusable());}finally{node.recycle();}
                    if(scale==1f){if(!fullyVisible(a.draftStatus))problems.add(language+"/"+theme+" initial draft feedback is clipped: "+bounds(a.draftStatus));if(!fullyVisible(send))problems.add(language+"/"+theme+" initial Send is clipped");assertCompleteText(a.draftStatus);assertCompleteText(send);assertCompleteText(a.gaugeText);visible(a.gaugeText);visible(a.note);}
                });
                if(scale==2f){
                    for(TextTarget target:new TextTarget[]{a->findText(a.stage,L.t("What should Codex do?","想让 Codex 做什么？")),a->a.note,a->a.gaugeText,a->a.stage.findViewWithTag("share-execution-title"),a->a.stage.findViewWithTag("share-execution-detail"),a->a.stage.findViewWithTag("share-send"),a->a.draftStatus}){
                        scenario.onActivity(a->reveal(a,target.get(a)));awaitFrame(scenario);scenario.onActivity(a->{TextView view=target.get(a);assertCompleteText(view);visible(view);assertSafe(a);});
                    }
                    capture(scenario,language,theme,scale,"feedback",measurements[0]);
                }
                scenario.onActivity(a->{assertEquals("ui-probe-project",a.selected);assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);assertEquals(language.equals("zh")?"这条留言只在内存。":"This note stays in memory.",a.note.getText().toString());assertEquals(a.note.getText().toString(),a.draft);assertSafe(a);});
            }
        }}finally{DemoShareEditorActivity.hierarchyFontScale=previous;}
        assertTrue(String.join("; ",problems),problems.isEmpty());
    }
    interface TextTarget {TextView get(DemoShareEditorActivity a);}
    static void assertSafe(DemoShareEditorActivity a){ShareModelDisclosureTest.assertSafe(a);assertEquals(0,a.keyboardBypasses);}
    static boolean fullyVisible(View view){Rect shown=new Rect();return view.getGlobalVisibleRect(shown)&&bounds(view).equals(shown);}
    static JSONArray measureCaps(Context context,List<String> problems){
        JSONArray result=new JSONArray();ShareActivity.Capped capped=new ShareActivity.Capped(context);View content=new View(context);capped.addView(content,new FrameLayout.LayoutParams(-1,Ui.dp(context,1600)));
        int previousHeight=0,previousAvailable=0;
        try{for(int available:new int[]{320,399,400,401,616,719,720,721,800,900,1200}){
            int pixels=Ui.dp(context,available);capped.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(context,320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(pixels,View.MeasureSpec.AT_MOST));int measured=capped.getMeasuredHeight();
            result.put(new JSONObject().put("available_dp",available).put("measured_px",measured));
            if(measured>pixels)problems.add("Sheet exceeds available height at "+available);
            if(available<=720&&measured!=pixels)problems.add("Compact height loses space at "+available+": "+measured+"px");
            if(measured<previousHeight)problems.add("Increasing available height shrinks the sheet at "+available);
            if(available-previousAvailable==1&&measured-previousHeight>Ui.dp(context,1))problems.add("Adjacent height boundary jumps at "+available);
            if(available>=900&&measured>=pixels)problems.add("Tall window has no breathing room at "+available);
            previousHeight=measured;previousAvailable=available;
        }
        content.setLayoutParams(new FrameLayout.LayoutParams(-1,Ui.dp(context,200)));capped.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(context,320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(context,900),View.MeasureSpec.AT_MOST));assertEquals("Short content remains naturally wrapped",Ui.dp(context,200),capped.getMeasuredHeight());
        }catch(Exception error){throw new AssertionError(error);}return result;
    }
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,String language,String theme,float scale,String state,JSONArray measurements)throws Exception{
        if(phase().isEmpty())return;assertTrue(phase().equals("red")||phase().equals("probe")||phase().equals("green"));Thread.sleep(2000);awaitFrame(scenario);
        JSONObject[] metadata={null};scenario.onActivity(a->{try{
            TextView marker=a.root.findViewWithTag("hierarchy-marker");assertCompleteText(marker);visible(marker);assertSafe(a);Rect shown=new Rect();a.draftStatus.getGlobalVisibleRect(shown);
            metadata[0]=new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("state",state).put("memory_only",true).put("root_bounds",bounds(a.root).toShortString()).put("holder_bounds",bounds(a.holder).toShortString()).put("sheet_bounds",bounds(a.sheet).toShortString()).put("scroll_bounds",bounds(a.scroll).toShortString()).put("scroll_y",a.scroll.getScrollY()).put("stage_height",a.stage.getHeight()).put("root_padding_top",a.root.getPaddingTop()).put("root_padding_bottom",a.root.getPaddingBottom()).put("sheet_padding_bottom",a.sheet.getPaddingBottom()).put("draft_bounds",bounds(a.draftStatus).toShortString()).put("draft_visible",shown.toShortString()).put("draft_fully_visible",fullyVisible(a.draftStatus)).put("send_bounds",bounds(a.stage.findViewWithTag("share-send")).toShortString()).put("marker",marker.getText()).put("marker_bounds",bounds(marker).toShortString()).put("marker_in_sheet",marker.getParent()==a.sheet).put("measurements",measurements).put("model",a.model).put("effort",a.effort).put("checkpoint_persistence_attempts",a.checkpointAttempts).put("forbidden_actions",a.forbiddenActions);
        }catch(Exception error){throw new AssertionError(error);}});
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/share-compact-"+phase()+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="share-compact-"+language+"-"+theme+"-font"+(int)scale+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
