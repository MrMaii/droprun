package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import static app.droprun.SettingsReadingPresentationTest.*;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareEditorTest.findText;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static org.junit.Assert.*;

/** Two opt-in font2 memory windows. No business clicks, preference edits or network. */
public class SettingsUniqueProjectNameTest {
    static final String[] PROJECT_IDS={"memory-unique-long","memory-unique-short"};
    @Test public void uniqueProjectNameRemainsReadableAtLargeFont()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("settingsUniqueNameProbe","");if(phase.isEmpty())return;assertTrue(phase.equals("baseline")||phase.equals("accepted"));boolean accepted=phase.equals("accepted");
        assertTrue(Build.VERSION.SDK_INT>=30);assertFalse(unresolvedLifetime);assertNull(DemoSettingsRecreationActivity.finishBeforeAttach);Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoSettingsRecreationActivity.class),0).exported);
        float priorFont=DemoSettingsRecreationActivity.hierarchyFontScale;String priorLanguage=L.chinese()?"zh":"en";boolean destroyed=true;int entered=0,completed=0,closed=0,verifiedLines=0;
        try{for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){
            String identity="unique-name|"+phase+"|"+String.join("|",config)+"|font2";assertTrue(destroyed);assertFalse(unresolvedLifetime);DemoSettingsRecreationActivity.hierarchyFontScale=2f;
            ActivityScenario<DemoSettingsRecreationActivity> scenario=null;DemoSettingsRecreationActivity[] retained={null};Throwable failure=null;
            try{
                destroyed=false;unresolvedLifetime=true;event(identity,"launch-attempt");scenario=ActivityScenario.launch(new Intent(target,DemoSettingsRecreationActivity.class).putExtra("language",config[0]).putExtra("appearance",config[1]).putExtra("modelHierarchy",true));event(identity,"returned-handle");ready(scenario);
                scenario.onActivity(a->{retained[0]=a;safe(a);assertEquals(2f,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertEquals(config[1].equals("dark"),Ui.dark);seedUnique(a);});frame(scenario);ready(scenario);entered++;event(identity,"entered");
                int[] lineCount={0};
                scenario.onActivity(a->{
                    for(int n=0;n<PROJECT_IDS.length;n++){
                        TextView name=uniqueName(a,n),chip=uniqueChip(a,n);Layout layout=name.getLayout();assertNotNull(layout);geometry(identity,n,name,chip);assertEquals(sample(n),a.store.projectLabel(PROJECT_IDS[n],sample(n)));assertEquals(sample(n),name.getText().toString());assertTrue(String.valueOf(chip.getContentDescription()).startsWith(sample(n)));assertEquals(n==0?L.t("Allowed","已允许"):L.t("Not allowed","未允许"),chip.getText().toString());assertTrue(chip.isEnabled());assertTrue(chip.isFocusable());assertTrue(chip.isClickable());assertTrue(chip.getForeground() instanceof RippleDrawable);assertTrue(chip.getWidth()>=Ui.dp(a,48));assertTrue(chip.getHeight()>=Ui.dp(a,48));
                        assertEquals(Ui.dp(a,280),name.getWidth());assertSame(name.getParent(),chip.getParent());assertEquals(LinearLayout.VERTICAL,((LinearLayout)name.getParent()).getOrientation());assertEquals(bounds(name).left,bounds(chip).left);assertTrue(chip.getTop()>=name.getBottom()+Ui.dp(a,8));
                        if(accepted){assertEquals(Integer.MAX_VALUE,name.getMaxLines());assertCompleteText(name);}else assertEquals(2,name.getMaxLines());
                    }
                    TextView longName=uniqueName(a,0);lineCount[0]=longName.getLineCount();if(accepted)assertTrue("The unique name occupies more than the old two-line cap",lineCount[0]>2);else{assertEquals(2,lineCount[0]);assertTrue("Baseline actually hides the unique name suffix",longName.getLayout().getEllipsisCount(1)>0);}safe(a);
                });
                if(accepted)for(int index=0;index<lineCount[0];index++){
                    final int line=index;scenario.onActivity(a->{TextView name=uniqueName(a,0);name.requestRectangleOnScreen(lineRect(name,line),true);});frame(scenario);ready(scenario);
                    scenario.onActivity(a->{TextView name=uniqueName(a,0);Rect local=new Rect(),global=new Rect();assertTrue(name.getLocalVisibleRect(local));Rect actual=lineRect(name,line);assertTrue("Every original name line is individually reachable",local.contains(actual));int[] at=new int[2];name.getLocationOnScreen(at);global.set(actual);global.offset(at[0],at[1]);View scroll=a.findViewById(R.id.settings_scroll);Rect viewport=bounds(scroll);viewport.left+=scroll.getPaddingLeft();viewport.top+=scroll.getPaddingTop();viewport.right-=scroll.getPaddingRight();viewport.bottom-=scroll.getPaddingBottom();Rect safeScreen=new Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safeScreen);assertTrue(safeScreen.intersect(viewport));assertTrue("Requested complete text line stays inside safe viewport",safeScreen.contains(global));safe(a);event(identity,"line-visible index="+line+" start="+name.getLayout().getLineStart(line)+" end="+name.getLayout().getLineEnd(line)+" bounds="+global.toShortString());});verifiedLines++;
                }
                for(int n=0;n<PROJECT_IDS.length;n++){final int index=n;scenario.onActivity(a->{TextView chip=uniqueChip(a,index);chip.requestRectangleOnScreen(new Rect(0,0,chip.getWidth(),chip.getHeight()),true);});frame(scenario);ready(scenario);scenario.onActivity(a->{SettingsRecreationTest.assertCompleteAndVisible(a,uniqueChip(a,index));safe(a);});}
                scenario.onActivity(a->{assertTrue(a.showAccess);assertEquals(2,a.store.projects().length());assertEquals(sample(0),a.store.projects().optJSONObject(0).optString("name"));assertEquals(sample(1),a.store.projects().optJSONObject(1).optString("name"));assertTrue(Store.projectEnabled(a.store.projects().optJSONObject(0)));assertFalse(Store.projectEnabled(a.store.projects().optJSONObject(1)));safe(a);guards(identity,"before-close",a);});completed++;event(identity,"full-completed");
            }catch(Throwable error){failure=error;throw error;}
            finally{if(scenario!=null)try{scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());destroyed=true;unresolvedLifetime=false;closed++;event(identity,"DESTROYED");if(retained[0]!=null){guards(identity,"after-destroyed",retained[0]);safe(retained[0]);}}catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(destroyed){DemoSettingsRecreationActivity.hierarchyFontScale=priorFont;L.language(priorLanguage);assertEquals(priorFont,DemoSettingsRecreationActivity.hierarchyFontScale,0f);assertEquals(priorLanguage,L.chinese()?"zh":"en");event("unique-name|"+phase,"font-and-language-restored");}}
        assertEquals(2,entered);assertEquals(2,completed);assertEquals(2,closed);assertFalse(unresolvedLifetime);event("unique-name|"+phase,"SUMMARY entered="+entered+" completed="+completed+" DESTROYED="+closed+" individually_verified_lines="+verifiedLines+" business_actions=not_invoked_by_test");
    }
    static String sample(int n){return n==0?L.t("Memory sample — research references for accessible mobile navigation, typography and interaction details — unique project identity at the very end","仅内存样本 · 用于核对无障碍移动端导航、字体阅读与交互细节的设计资料归档 · 请完整保留位于项目名称最末尾的唯一识别信息"):L.t("Memory sample — Notes","仅内存样本 · 便笺");}
    static void seedUnique(DemoSettingsRecreationActivity a){try{JSONObject data=a.store.projectsData();JSONArray projects=new JSONArray();for(int n=0;n<PROJECT_IDS.length;n++)projects.put(new JSONObject().put("id",PROJECT_IDS[n]).put("name",sample(n)).put("permission",new JSONObject().put("enabled",n==0)));data.put("projects",projects);a.cached.put("projects",data.toString());a.showAccess=true;a.render();safe(a);}catch(JSONException error){throw new AssertionError(error);}}
    static TextView uniqueName(DemoSettingsRecreationActivity a,int index){TextView name=findText(a.body,sample(index));assertNotNull(name);return name;}
    static TextView uniqueChip(DemoSettingsRecreationActivity a,int index){ViewGroup row=(ViewGroup)uniqueName(a,index).getParent();for(int n=0;n<row.getChildCount();n++){View child=row.getChildAt(n);if(child instanceof TextView&&child.isClickable())return (TextView)child;}fail("Permission chip is absent");return null;}
    static Rect lineRect(TextView view,int line){assertCompleteText(view);Layout layout=view.getLayout();return new Rect(view.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(line)),view.getCompoundPaddingTop()+layout.getLineTop(line),view.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(line)),view.getCompoundPaddingTop()+layout.getLineBottom(line));}
    static void geometry(String identity,int index,TextView name,TextView chip){try{Layout layout=name.getLayout();JSONArray ellipsis=new JSONArray();for(int n=0;n<layout.getLineCount();n++)ellipsis.put(layout.getEllipsisCount(n));event(identity,"PROJECT "+new JSONObject().put("id",PROJECT_IDS[index]).put("exact_name",name.getText()).put("name_width",name.getWidth()).put("name_height",name.getHeight()).put("max_lines",name.getMaxLines()).put("line_count",layout.getLineCount()).put("ellipsis_counts",ellipsis).put("last_layout_line_end",layout.getLineEnd(layout.getLineCount()-1)).put("name_bounds",bounds(name).toShortString()).put("chip_bounds",bounds(chip).toShortString()).put("chip_description",chip.getContentDescription()));}catch(JSONException error){throw new AssertionError(error);}}
}
