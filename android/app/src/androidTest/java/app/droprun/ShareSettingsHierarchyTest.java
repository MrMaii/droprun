package app.droprun;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static app.droprun.ShareEditorTest.findText;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static org.junit.Assert.*;

/** Bounded real controls with opt-in, non-exported memory fixtures. No IME, API or Send. */
public class ShareSettingsHierarchyTest {
    final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    String phase(){return InstrumentationRegistry.getArguments().getString("captureShareSettingsUi","");}
    String[] languages(){return phase().equals("red")?new String[]{"en"}:new String[]{"en","zh"};}
    String[] themes(){return phase().equals("red")?new String[]{"light"}:new String[]{"light","dark"};}
    float[] scales(){return phase().equals("red")?new float[]{1f}:new float[]{1f,2f};}
    Context context(){Context context=instrumentation.getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));return context;}

    @Test public void shareStepsHaveOneHeadingAndKeepTheMemoryDraft()throws Exception{
        float previous=DemoShareEditorActivity.hierarchyFontScale;
        try{for(String language:languages())for(String theme:themes())for(float scale:scales()){
            DemoShareEditorActivity.hierarchyFontScale=scale;
            try(ActivityScenario<DemoShareEditorActivity> scenario=ActivityScenario.launch(new Intent(context(),DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme))){
                ready(scenario);scenario.onActivity(a->a.go(0,-1));ready(scenario);
                JSONObject project=captureShare(scenario,language,theme,scale,"project");
                scenario.onActivity(a->{assertEquals(1,a.projectList.getChildCount());assertTrue(a.projectList.getChildAt(0).performClick());});readyStep(scenario,1);
                JSONObject note=captureShare(scenario,language,theme,scale,"note");
                assertEquals("Project step starts with its question, without a duplicate eyebrow",0,project.getInt("heading_index"));
                assertEquals("Intent step starts with its question, without a duplicate eyebrow",0,note.getInt("heading_index"));
                scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);a.note.setText(language.equals("zh")?"本地留言保留测试":"Keep this local note");a.onBackPressed();});readyStep(scenario,0);
                scenario.onActivity(a->{assertEquals("ui-probe-project",a.selected);assertTrue(a.projectList.getChildAt(0).performClick());});readyStep(scenario,1);
                scenario.onActivity(a->{assertEquals(language.equals("zh")?"本地留言保留测试":"Keep this local note",a.note.getText().toString());assertEquals(a.draft,a.note.getText().toString());assertTrue(a.keyboardBypasses>=4);assertSafe(a);});
                for(String tag:new String[]{"share-execution-title","share-execution-detail","share-send"}){
                    scenario.onActivity(a->{View target=a.stage.findViewWithTag(tag);assertNotNull(target);target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);});ready(scenario);
                    scenario.onActivity(a->{TextView target=a.stage.findViewWithTag(tag);assertCompleteText(target);visible(target);assertSafe(a);});
                }
            }
        }}finally{DemoShareEditorActivity.hierarchyFontScale=previous;}
    }
    JSONObject captureShare(ActivityScenario<DemoShareEditorActivity> scenario,String language,String theme,float scale,String step)throws Exception{
        JSONObject[] result={null};scenario.onActivity(a->{try{
            LinearLayout column=(LinearLayout)a.stage.getChildAt(0);TextView heading=findText(column,a.step==0?L.t("Where should this idea go?","转发给哪个项目？"):L.t("What should Codex do?","想让 Codex 做什么？"));
            assertNotNull(heading);assertCompleteText(heading);visible(heading);TextView marker=a.root.findViewWithTag("hierarchy-marker");assertCompleteText(marker);visible(marker);
            TextView material=findText(column,a.materialLabel());assertNotNull(material);assertCompleteText(material);visible(material);assertSafe(a);
            result[0]=new JSONObject().put("kind","share").put("step",step).put("language",language).put("theme",theme).put("font_scale",scale).put("memory_only",true).put("heading_index",column.indexOfChild(heading)).put("heading",heading.getText()).put("heading_bounds",bounds(heading).toShortString()).put("column_height",column.getHeight()).put("prefix_height",bounds(heading).top-bounds(column).top).put("marker",marker.getText()).put("forbidden_actions",a.forbiddenActions).put("keyboard_bypasses",a.keyboardBypasses);
        }catch(Exception error){throw new AssertionError(error);}});capture("share-"+language+"-"+theme+"-font"+(int)scale+"-"+step,result[0]);return result[0];
    }

    @Test public void defaultModelRowsKeepRawValuesFocusCancelAndRecreation()throws Exception{
        float previous=DemoSettingsRecreationActivity.hierarchyFontScale;
        try{for(String language:languages())for(String theme:themes())for(float scale:scales()){
            DemoSettingsRecreationActivity.hierarchyFontScale=scale;
            try(ActivityScenario<DemoSettingsRecreationActivity> scenario=ActivityScenario.launch(new Intent(context(),DemoSettingsRecreationActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelHierarchy",true))){
                ready(scenario);scenario.onActivity(a->{View card=(View)a.body.findViewById(R.id.settings_model).getParent();card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true);});ready(scenario);
                JSONObject[] metadata={null};scenario.onActivity(a->{try{
                    View model=a.body.findViewById(R.id.settings_model),effort=a.body.findViewById(R.id.settings_effort);assertSafe(a);assertCompleteText(a.hierarchyNotice);visible(a.hierarchyNotice);
                    metadata[0]=new JSONObject().put("kind","settings").put("language",language).put("theme",theme).put("font_scale",scale).put("memory_only",true).put("model_type",model.getClass().getSimpleName()).put("model_bounds",bounds(model).toShortString()).put("effort_bounds",bounds(effort).toShortString()).put("card_height",((View)model.getParent()).getHeight()).put("model_description",model.getContentDescription()).put("effort_description",effort.getContentDescription()).put("marker",a.hierarchyNotice.getText()).put("forbidden_actions",a.forbiddenActions.get());
                }catch(Exception error){throw new AssertionError(error);}});capture("settings-"+language+"-"+theme+"-font"+(int)scale,metadata[0]);
                scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertRow(a,R.id.settings_model,L.t("Model","模型"),L.t("Local UI model","本地界面模型"));assertRow(a,R.id.settings_effort,L.t("Reasoning effort","推理强度"),L.t("Balanced","均衡"));assertEquals(0,a.modelSaves);});
                instrumentation.setInTouchMode(false);
                scenario.onActivity(a->{assertTrue(a.body.findViewById(R.id.settings_model).requestFocus());assertTrue(a.body.findViewById(R.id.settings_model).performClick());});dialogClick(language.equals("zh")?"取消":"Cancel");ready(scenario);
                scenario.onActivity(a->{assertEquals(0,a.modelSaves);assertEquals("sample-default",a.store.defaultModel());assertSame(a.body.findViewById(R.id.settings_model),a.getCurrentFocus());assertTrue(a.body.findViewById(R.id.settings_model).performClick());});dialogClick(language.equals("zh")?"另一个界面模型":"Another UI model");ready(scenario);
                scenario.onActivity(a->{assertEquals(1,a.modelSaves);assertEquals("sample-second",a.store.defaultModel());assertEquals("low",a.store.defaultEffort("sample-second"));assertSame(a.body.findViewById(R.id.settings_model),a.getCurrentFocus());assertTrue(a.body.findViewById(R.id.settings_effort).requestFocus());assertTrue(a.body.findViewById(R.id.settings_effort).performClick());});dialogClick(language.equals("zh")?"深入":"Thorough");ready(scenario);
                scenario.onActivity(a->{assertEquals(2,a.modelSaves);assertEquals("high",a.store.defaultEffort("sample-second"));assertSame(a.body.findViewById(R.id.settings_effort),a.getCurrentFocus());assertTrue(a.body.findViewById(R.id.settings_effort).performClick());});dialogClick(language.equals("zh")?"取消":"Cancel");ready(scenario);
                scenario.onActivity(a->{assertEquals(2,a.modelSaves);assertEquals("high",a.store.defaultEffort("sample-second"));assertSame(a.body.findViewById(R.id.settings_effort),a.getCurrentFocus());});scenario.recreate();ready(scenario);
                scenario.onActivity(a->{assertEquals(2,a.modelSaves);assertEquals("sample-second",a.store.defaultModel());assertEquals("high",a.store.defaultEffort("sample-second"));assertSame(a.body.findViewById(R.id.settings_effort),a.getCurrentFocus());assertRow(a,R.id.settings_effort,L.t("Reasoning effort","推理强度"),L.t("Thorough","深入"));assertTrue(a.body.findViewById(R.id.settings_effort).performClick());});dialogClick("future-effort");ready(scenario);
                scenario.onActivity(a->{assertEquals(3,a.modelSaves);assertEquals("future-effort",a.store.defaultEffort("sample-second"));assertRow(a,R.id.settings_effort,L.t("Reasoning effort","推理强度"),"future-effort");assertSafe(a);});
            }
        }}finally{DemoSettingsRecreationActivity.hierarchyFontScale=previous;}
    }
    void assertRow(DemoSettingsRecreationActivity a,int id,String title,String value){
        View row=a.body.findViewById(id);assertTrue("Preference is a native setting row, not a command button",row instanceof LinearLayout);assertTrue(row.getHeight()>=Ui.dp(a,48));assertTrue(row.getWidth()>=Ui.dp(a,48));assertCompleteText(findText(row,title));assertCompleteText(findText(row,value));
        AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();try{assertEquals(title+", "+value,String.valueOf(node.getContentDescription()));assertTrue(node.isClickable());assertTrue(node.isFocusable());assertTrue(node.isEnabled());}finally{node.recycle();}
    }
    void dialogClick(String text)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;
        do{AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();if(root!=null)try{
            assertEquals(context().getPackageName(),String.valueOf(root.getPackageName()));
            for(AccessibilityNodeInfo candidate:root.findAccessibilityNodeInfosByText(text))try{if(text.contentEquals(candidate.getText()==null?"":candidate.getText())){AccessibilityNodeInfo action=candidate;while(!action.isClickable()&&action.getParent()!=null)action=action.getParent();assertTrue(action.performAction(AccessibilityNodeInfo.ACTION_CLICK));return;}}finally{candidate.recycle();}
        }finally{root.recycle();}Thread.sleep(25);}while(SystemClock.elapsedRealtime()<deadline);fail("Native dialog item absent: "+text);
    }
    <T extends Activity> void ready(ActivityScenario<T> scenario)throws Exception{
        instrumentation.waitForIdleSync();long deadline=SystemClock.elapsedRealtime()+4000;boolean[] settled={false};
        do{scenario.onActivity(a->{View content=a.findViewById(android.R.id.content);settled[0]=a.hasWindowFocus()&&content.isAttachedToWindow()&&!content.isLayoutRequested();if(a instanceof DemoShareEditorActivity){DemoShareEditorActivity share=(DemoShareEditorActivity)a;settled[0]&=!share.busy&&share.stage.getChildCount()==1&&share.stage.getChildAt(0).getAlpha()==1f&&share.stage.getChildAt(0).getTranslationY()==0f;}});if(settled[0])return;Thread.sleep(25);}while(SystemClock.elapsedRealtime()<deadline);fail("Memory window did not settle");
    }
    void readyStep(ActivityScenario<DemoShareEditorActivity> scenario,int step)throws Exception{long deadline=SystemClock.elapsedRealtime()+3000;boolean[] reached={false};do{scenario.onActivity(a->reached[0]=a.step==step&&!a.busy);if(reached[0]){ready(scenario);return;}Thread.sleep(25);}while(SystemClock.elapsedRealtime()<deadline);fail("Local step did not settle");}
    static void assertSafe(DemoShareEditorActivity a){ShareEditorTest.assertSafe(a);assertEquals(0,a.forbiddenActions);assertNull(a.incoming);}
    static void assertSafe(DemoSettingsRecreationActivity a){assertEquals(0,a.forbiddenActions.get());assertEquals(0,a.workFactories);assertFalse(a.store.paired());}
    static Rect bounds(View view){int[] point=new int[2];view.getLocationOnScreen(point);return new Rect(point[0],point[1],point[0]+view.getWidth(),point[1]+view.getHeight());}
    static void visible(View view){Rect shown=new Rect();assertTrue(view.getGlobalVisibleRect(shown));assertEquals(bounds(view),shown);}
    void capture(String name,JSONObject metadata)throws Exception{
        if(phase().isEmpty())return;assertTrue(phase().equals("red")||phase().equals("green"));Thread.sleep(2000);instrumentation.waitForIdleSync();
        File directory=new File(context().getExternalFilesDir(null),"ui-probe-evidence/share-settings-"+phase()+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());
        Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(image);try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata.toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
