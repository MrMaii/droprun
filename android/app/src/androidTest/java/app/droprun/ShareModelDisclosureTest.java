package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
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

/** Share-local model and effort controls only. All samples and choices remain in guarded memory. */
public class ShareModelDisclosureTest {
    String phase(){return InstrumentationRegistry.getArguments().getString("captureShareModelUi","");}
    @Test public void modelDisclosureAndEffortRowsStayReadableAndPreserveRawChoices()throws Exception{
        float previous=DemoShareEditorActivity.hierarchyFontScale;
        boolean red=phase().equals("red");List<String> problems=new ArrayList<>();
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false);
        try{for(String language:red?new String[]{"en"}:new String[]{"en","zh"})for(String theme:red?new String[]{"light"}:new String[]{"light","dark"})for(float scale:red?new float[]{1f}:new float[]{1f,2f}){
            DemoShareEditorActivity.hierarchyFontScale=scale;
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
            try(ActivityScenario<DemoShareEditorActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true))){
                ready(scenario);scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);a.note.setText(language.equals("zh")?"这条留言只在内存。":"This note stays in memory.");reveal(a,(View)a.gaugeText.getParent());});awaitFrame(scenario);
                capture(scenario,language,theme,scale,"collapsed");
                scenario.onActivity(a->{
                    if(!a.gaugeText.getText().toString().startsWith(L.t("Model & effort","模型强度")+"\n"))problems.add("Collapsed header has no visible Model & effort title");
                    View arrow=a.stage.findViewWithTag("share-model-chevron");if(arrow==null)problems.add("Collapsed header has no visible state chevron");else assertEquals(180f,arrow.getRotation(),0.01f);
                    View row=(View)a.gaugeText.getParent();assertTrue(row.isFocusable());assertTrue(row.getHeight()>=Ui.dp(a,48));assertCompleteText(a.gaugeText);visible(row);assertHeaderNode(a,false);assertSafe(a);assertTrue(row.performClick());
                });awaitFrame(scenario);
                scenario.onActivity(a->{assertTrue(a.panelOpen);View arrow=a.stage.findViewWithTag("share-model-chevron");if(arrow!=null)assertEquals(270f,arrow.getRotation(),0.01f);assertHeaderNode(a,true);reveal(a,a.panel.findViewWithTag("model:probe-thorough"));});awaitFrame(scenario);
                capture(scenario,language,theme,scale,"models");
                scenario.onActivity(a->{View other=a.panel.findViewWithTag("model:probe-thorough");if(other.getHeight()<Ui.dp(a,48))problems.add("Non-default model target is "+other.getHeight()+"px, below48dp");assertCompleteText(findText(other,"Codex · Another sample"));visible(other);assertTrue(other.performClick());assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);});awaitFrame(scenario);
                scenario.onActivity(a->{View first=a.panel.findViewWithTag("effort:low"),last=a.panel.findViewWithTag("effort:future-effort");assertNotNull(first);assertNotNull(last);Rect area=new Rect(0,0,last.getWidth(),last.getHeight());a.panel.offsetDescendantRectToMyCoords(last,area);Rect start=new Rect();first.getDrawingRect(start);a.panel.offsetDescendantRectToMyCoords(first,start);area.top=start.top-Ui.dp(a,38);a.panel.requestRectangleOnScreen(area,true);});awaitFrame(scenario);
                capture(scenario,language,theme,scale,"efforts");
                for(String id:new String[]{"low","medium","high","xhigh","future-effort"}){
                    scenario.onActivity(a->{View choice=a.panel.findViewWithTag("effort:"+id);reveal(a,choice);});awaitFrame(scenario);
                    scenario.onActivity(a->{
                        View choice=a.panel.findViewWithTag("effort:"+id);String meaning=effortHint(id,language),label=meaning.isEmpty()?id:meaning;
                        if(!(choice instanceof LinearLayout))problems.add("Effort "+id+" is a narrow segment, not an option row");
                        if(choice.getHeight()<Ui.dp(a,48)||choice.getWidth()<Ui.dp(a,48))problems.add("Effort "+id+" has a target below48dp: "+choice.getWidth()+"x"+choice.getHeight());
                        TextView text=findText(choice,label);if(text==null)problems.add("Effort "+id+" has no readable label "+label);else{assertCompleteText(text);visible(text);}
                        if(!red){assertEquals(a.panel.getWidth(),choice.getWidth());assertTrue(choice.requestFocus());}
                        assertTrue(choice.performClick());assertEquals(id,a.effort);assertEquals("probe-thorough",a.model);assertEquals("ui-probe-project",a.selected);assertEquals(language.equals("zh")?"这条留言只在内存。":"This note stays in memory.",a.note.getText().toString());assertEquals(a.draft,a.note.getText().toString());assertSafe(a);
                    });awaitFrame(scenario);
                    if(!red)scenario.onActivity(a->{View selected=a.panel.findViewWithTag("effort:"+id);assertSame(selected,a.getCurrentFocus());assertOptionNode(selected,effortHint(id,language).isEmpty()?id:effortHint(id,language),language);});
                }
                scenario.onActivity(a->{assertTrue(a.checkpointNoops>0);reveal(a,(View)a.gaugeText.getParent());});awaitFrame(scenario);
                scenario.onActivity(a->{assertTrue(((View)a.gaugeText.getParent()).performClick());});awaitFrame(scenario);
                scenario.onActivity(a->{assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());assertEquals("future-effort",a.effort);assertTrue(a.gaugeText.getText().toString().endsWith(" · future-effort"));View arrow=a.stage.findViewWithTag("share-model-chevron");if(arrow!=null)assertEquals(180f,arrow.getRotation(),0.01f);assertHeaderNode(a,false);assertSafe(a);});
                for(String tag:new String[]{"share-execution-title","share-execution-detail","share-send"}){scenario.onActivity(a->reveal(a,a.stage.findViewWithTag(tag)));awaitFrame(scenario);scenario.onActivity(a->{TextView target=a.stage.findViewWithTag(tag);assertCompleteText(target);visible(target);assertSafe(a);});}
            }
        }}finally{DemoShareEditorActivity.hierarchyFontScale=previous;}
        assertTrue(String.join("; ",problems),problems.isEmpty());
    }
    static String effortHint(String id,String language){return switch(id){case "low"->language.equals("zh")?"快":"Fast";case "medium"->language.equals("zh")?"均衡":"Balanced";case "high"->language.equals("zh")?"深入":"Thorough";case "xhigh"->language.equals("zh")?"最深":"Most thorough";default->"";};}
    static void assertSafe(DemoShareEditorActivity a){ShareEditorTest.assertSafe(a);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertNull(a.incoming);}
    static void assertHeaderNode(DemoShareEditorActivity a,boolean open){View row=(View)a.gaugeText.getParent();AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();try{assertTrue(node.isClickable());assertTrue(node.isFocusable());assertTrue(node.isEnabled());String description=String.valueOf(node.getContentDescription());assertTrue(description.startsWith(L.t("Model & effort, ","模型强度，")));assertTrue(description.endsWith(open?L.t(", tap to collapse","，点按收起"):L.t(", tap to expand","，点按展开")));}finally{node.recycle();}}
    static void assertOptionNode(View choice,String label,String language){AccessibilityNodeInfo node=choice.createAccessibilityNodeInfo();try{assertTrue(node.isEnabled());assertTrue(node.isClickable());assertTrue(node.isFocusable());assertEquals(label+(language.equals("zh")?"，已选择":", selected"),String.valueOf(node.getContentDescription()));}finally{node.recycle();}}
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,String language,String theme,float scale,String state)throws Exception{
        if(phase().isEmpty())return;assertTrue(phase().equals("red")||phase().equals("green"));Thread.sleep(2000);awaitFrame(scenario);
        JSONObject[] metadata={null};scenario.onActivity(a->{try{
            TextView marker=a.root.findViewWithTag("hierarchy-marker");assertCompleteText(marker);visible(marker);assertSafe(a);View arrow=a.stage.findViewWithTag("share-model-chevron");
            JSONArray choices=new JSONArray();if(a.panelOpen){for(String tag:new String[]{"model:probe-fast","model:probe-thorough","effort:low","effort:medium","effort:high","effort:xhigh","effort:future-effort"}){View row=a.panel.findViewWithTag(tag);if(row!=null)choices.put(new JSONObject().put("tag",tag).put("type",row.getClass().getSimpleName()).put("width",row.getWidth()).put("height",row.getHeight()).put("bounds",bounds(row).toShortString()).put("description",row.getContentDescription()));}}
            metadata[0]=new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("state",state).put("memory_only",true).put("header",a.gaugeText.getText()).put("header_bounds",bounds((View)a.gaugeText.getParent()).toShortString()).put("header_description",((View)a.gaugeText.getParent()).getContentDescription()).put("chevron_present",arrow!=null).put("chevron_rotation",arrow==null?JSONObject.NULL:arrow.getRotation()).put("panel_open",a.panelOpen).put("choices",choices).put("model",a.model).put("effort",a.effort).put("marker",marker.getText()).put("checkpoint_noops",a.checkpointNoops).put("checkpoint_persistence_attempts",a.checkpointAttempts).put("forbidden_actions",a.forbiddenActions);
        }catch(Exception error){throw new AssertionError(error);}});
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/share-model-"+phase()+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="share-model-"+language+"-"+theme+"-font"+(int)scale+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
