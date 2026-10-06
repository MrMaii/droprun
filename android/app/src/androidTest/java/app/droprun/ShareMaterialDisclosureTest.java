package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
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
import static app.droprun.ShareEditorTest.*;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static app.droprun.ShareSettingsHierarchyTest.visible;
import static org.junit.Assert.*;

/** Received strings only: no import object, file paths, clipboard, links, or Send actions. */
public class ShareMaterialDisclosureTest {
    String phase(){return InstrumentationRegistry.getArguments().getString("captureShareMaterialUi","");}
    @Test public void receivedMaterialCanBeInspectedWithoutChangingTheHandoff()throws Exception{
        float previous=DemoShareEditorActivity.hierarchyFontScale;boolean red=phase().equals("red");
        try{for(String language:red?new String[]{"en"}:new String[]{"en","zh"})for(String theme:red?new String[]{"light"}:new String[]{"light","dark"})for(float scale:red?new float[]{1f}:new float[]{1f,2f}){
            DemoShareEditorActivity.hierarchyFontScale=scale;Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
            try(ActivityScenario<DemoShareEditorActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("compactHeight",true).putExtra("materialReview",true))){
                String[] original={null,null};ready(scenario);scenario.onActivity(a->{a.note.setText(language.equals("zh")?"只核对材料，留言保留。":"Keep this note while checking the material.");original[0]=a.shared;original[1]=a.attachments.toString();assertTrue(a.shared.contains("/posts/alpha?"));assertTrue(a.shared.contains("/posts/beta?"));assertEquals(8,a.attachments.length());a.scroll.scrollTo(0,0);assertSafe(a);});awaitFrame(scenario);
                capture(scenario,language,theme,scale,"collapsed");
                scenario.onActivity(a->{assertNotNull("A received-material disclosure is required to inspect complete text, URL paths and filenames before Send",group(a));assertClosed(a);TextView label=summary(a);assertEquals(a.materialSummary(),label.getText().toString());assertEquals(1,label.getMaxLines());assertNode(a,false);assertTrue(header(a).performClick());});awaitMaterial(scenario);
                scenario.onActivity(a->{assertExactMaterial(a);assertNode(a,true);reveal(a,a.stage.findViewWithTag("share-material-scope"));});awaitFrame(scenario);scenario.onActivity(a->{TextView scope=a.stage.findViewWithTag("share-material-scope");assertCompleteText(scope);visible(scope);});
                for(int point:new int[]{0,1,2}){scenario.onActivity(a->revealLine(a,point));awaitFrame(scenario);scenario.onActivity(a->assertLineVisible(a,point));}
                capture(scenario,language,theme,scale,"text-tail");
                for(int index=0;index<8;index++){final int n=index;scenario.onActivity(a->reveal(a,a.stage.findViewWithTag("share-material-file:"+n)));awaitFrame(scenario);scenario.onActivity(a->{TextView file=a.stage.findViewWithTag("share-material-file:"+n);assertCompleteText(file);visible(file);assertTrue(file.getText().toString().endsWith("0"+(n+1)+".png"));});}
                capture(scenario,language,theme,scale,"files");
                scenario.onActivity(a->{assertUnchanged(a,original,language);a.go(0,-1);});awaitMaterial(scenario);
                scenario.onActivity(a->{assertEquals(0,a.step);assertExactMaterial(a);assertNode(a,true);a.go(1,1);});awaitMaterial(scenario);
                scenario.onActivity(a->{assertEquals(1,a.step);assertExactMaterial(a);assertUnchanged(a,original,language);reveal(a,header(a));});awaitFrame(scenario);
                scenario.onActivity(a->{assertTrue(header(a).performClick());});awaitMaterial(scenario);
                scenario.onActivity(a->{assertClosed(a);assertNode(a,false);assertUnchanged(a,original,language);a.scroll.scrollTo(0,0);});awaitFrame(scenario);
                if(scale==1f)scenario.onActivity(a->{visible(a.draftStatus);visible(a.stage.findViewWithTag("share-send"));});
                if(language.equals("en")&&theme.equals("light")&&scale==1f){
                    for(String kind:new String[]{"text","files","link"}){
                        scenario.onActivity(a->{a.materialSample(kind);a.go(1,0);});awaitMaterial(scenario);scenario.onActivity(a->{assertClosed(a);assertTrue(header(a).performClick());});awaitMaterial(scenario);scenario.onActivity(a->{assertExactMaterial(a);assertTrue(header(a).performClick());});awaitMaterial(scenario);
                    }
                }
                scenario.onActivity(ShareMaterialDisclosureTest::assertSafe);
            }
        }}finally{DemoShareEditorActivity.hierarchyFontScale=previous;}
    }
    static LinearLayout group(DemoShareEditorActivity a){return a.stage.findViewWithTag("share-material-review");}
    static View header(DemoShareEditorActivity a){return group(a).getChildAt(0);}
    static TextView summary(DemoShareEditorActivity a){return (TextView)((ViewGroup)header(a)).getChildAt(0);}
    static void assertClosed(DemoShareEditorActivity a){assertEquals(Boolean.FALSE,header(a).getTag());assertEquals(View.GONE,group(a).getChildAt(1).getVisibility());assertTrue(header(a).getHeight()>=Ui.dp(a,48));assertTrue(header(a).getWidth()>=Ui.dp(a,48));}
    static void assertNode(DemoShareEditorActivity a,boolean open){AccessibilityNodeInfo node=header(a).createAccessibilityNodeInfo();try{assertTrue(node.isClickable());assertTrue(node.isFocusable());assertTrue(node.isEnabled());String description=String.valueOf(node.getContentDescription());assertTrue(description.startsWith(L.t("Received material: ","收到的材料：")));assertTrue(description.contains(a.materialLabel()));assertTrue(description.endsWith(open?L.t(", expanded",", 已展开"):L.t(", collapsed",", 已折叠")));}finally{node.recycle();}}
    static void assertExactMaterial(DemoShareEditorActivity a){
        assertEquals(Boolean.TRUE,header(a).getTag());TextView raw=a.stage.findViewWithTag("share-material-text");
        if(a.shared.isEmpty())assertNull(raw);else{assertNotNull(raw);assertEquals(a.shared,raw.getText().toString());assertPlainFullText(raw);}
        LinearLayout files=a.stage.findViewWithTag("share-material-files");if(a.attachments.length()==0)assertNull(files);else{assertNotNull(files);assertEquals(a.attachments.length(),files.getChildCount());}
        for(int n=0;n<a.attachments.length();n++){TextView file=a.stage.findViewWithTag("share-material-file:"+n);assertNotNull(file);assertEquals(a.attachments.optJSONObject(n).optString("name"),file.getText().toString());assertPlainFullText(file);assertFalse(a.attachments.optJSONObject(n).has("path"));}
        TextView scope=a.stage.findViewWithTag("share-material-scope");assertEquals(L.t("Text and filenames received from the source app. Reading coverage is reported with the result.","这里显示来源 App 传来的文字和文件名；实际读取范围以交付报告为准。"),scope.getText().toString());assertSafe(a);
    }
    static void assertPlainFullText(TextView text){assertCompleteText(text);assertTrue(text.isTextSelectable());assertEquals(0,text.getAutoLinkMask());assertFalse(text.hasOnClickListeners());if(text.getText() instanceof Spanned)assertEquals(0,((Spanned)text.getText()).getSpans(0,text.length(),URLSpan.class).length);}
    static Rect lineRect(TextView raw,int point){int line=point==0?0:point==1?raw.getLayout().getLineCount()/2:raw.getLayout().getLineForOffset(raw.length()-1);Rect lineBox=new Rect();raw.getLayout().getLineBounds(line,lineBox);lineBox.left=0;lineBox.right=raw.getWidth();lineBox.offset(0,raw.getTotalPaddingTop());return lineBox;}
    static void revealLine(DemoShareEditorActivity a,int point){TextView raw=a.stage.findViewWithTag("share-material-text");raw.requestRectangleOnScreen(lineRect(raw,point),true);}
    static void assertLineVisible(DemoShareEditorActivity a,int point){TextView raw=a.stage.findViewWithTag("share-material-text");Rect line=lineRect(raw,point),screen=bounds(raw);line.offset(screen.left,screen.top);Rect viewport=bounds(a.scroll);assertTrue("Requested line remains inside the actual scroll viewport",viewport.contains(line));}
    static void assertUnchanged(DemoShareEditorActivity a,String[] original,String language){assertEquals(original[0],a.shared);assertEquals(original[1],a.attachments.toString());assertEquals("ui-probe-project",a.selected);assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);assertEquals(language.equals("zh")?"只核对材料，留言保留。":"Keep this note while checking the material.",a.note.getText().toString());assertEquals(a.note.getText().toString(),a.draft);assertSafe(a);}
    static void assertSafe(DemoShareEditorActivity a){ShareModelDisclosureTest.assertSafe(a);assertNull(a.incoming);}
    static void awaitMaterial(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{
        awaitFrame(scenario);long end=android.os.SystemClock.elapsedRealtime()+3000;boolean[] done={false};do{scenario.onActivity(a->{if(a.stage.getChildCount()!=1){done[0]=false;return;}View pane=a.stage.getChildAt(0),group=group(a);done[0]=pane.getAlpha()==1f&&pane.getTranslationX()==0f&&(group==null||((ViewGroup)group).getChildAt(1).getTag(R.id.expand_animation)==null);});if(done[0])return;Thread.sleep(20);}while(android.os.SystemClock.elapsedRealtime()<end);fail("Material disclosure and its single active step did not settle");
    }
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,String language,String theme,float scale,String state)throws Exception{
        if(phase().isEmpty()||(phase().equals("tail")&&!state.equals("text-tail")))return;assertTrue(phase().equals("red")||phase().equals("green")||phase().equals("final")||phase().equals("tail"));Thread.sleep(2000);awaitMaterial(scenario);JSONObject[] data={null};scenario.onActivity(a->{try{
            TextView marker=a.root.findViewWithTag("hierarchy-marker");visible(marker);assertCompleteText(marker);assertSafe(a);TextView label=findText(a.stage,a.materialSummary());assertNotNull(label);
            JSONArray names=new JSONArray(),files=new JSONArray();for(int n=0;n<a.attachments.length();n++){names.put(a.attachments.optJSONObject(n).optString("name"));TextView file=a.stage.findViewWithTag("share-material-file:"+n);if(file!=null)files.put(new JSONObject().put("index",n).put("bounds",bounds(file).toShortString()).put("selectable",file.isTextSelectable()));}
            data[0]=new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("state",state).put("memory_only",true).put("received_text",a.shared).put("filenames",names).put("summary",label.getText()).put("summary_bounds",bounds(label).toShortString()).put("summary_ellipsis",label.getLayout().getEllipsisCount(0)).put("disclosure_present",group(a)!=null).put("header_bounds",group(a)==null?JSONObject.NULL:bounds(header(a)).toShortString()).put("header_description",group(a)==null?JSONObject.NULL:header(a).getContentDescription()).put("scroll_y",a.scroll.getScrollY()).put("scroll_bounds",bounds(a.scroll).toShortString()).put("files",files).put("marker",marker.getText()).put("marker_bounds",bounds(marker).toShortString()).put("checkpoint_persistence_attempts",a.checkpointAttempts).put("keyboard_bypasses",a.keyboardBypasses).put("forbidden_actions",a.forbiddenActions);
            if(phase().equals("tail")){TextView raw=a.stage.findViewWithTag("share-material-text");data[0].put("last_received_character_offset",raw.length()-1).put("last_received_character_line",raw.getLayout().getLineForOffset(raw.length()-1)).put("last_received_character_line_bounds",lineRect(raw,2).toShortString());assertLineVisible(a,2);}
        }catch(Exception error){throw new AssertionError(error);}});
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/share-material-"+phase()+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="share-material-"+language+"-"+theme+"-font"+(int)scale+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(FileOutputStream output=new FileOutputStream(png)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));}try(FileOutputStream output=new FileOutputStream(json)){output.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
