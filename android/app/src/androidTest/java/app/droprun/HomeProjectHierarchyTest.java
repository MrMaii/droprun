package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static app.droprun.HomeRecoveryFeedbackTest.*;
import static org.junit.Assert.*;

/** Actual project rows over three memory-only records; never clicks or opens a project. */
public class HomeProjectHierarchyTest {
    @Test public void projectIdentityUsesTheCardWidthInBothLanguagesAndThemes()throws Exception{
        float previous=DemoHomeRecoveryActivity.hierarchyFontScale;
        try{
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})for(float scale:new float[]{1f,2f}){
                DemoHomeRecoveryActivity.hierarchyFontScale=scale;
                Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
                try(ActivityScenario<DemoHomeRecoveryActivity> scenario=ActivityScenario.launch(new Intent(context,DemoHomeRecoveryActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("hierarchyProbe",true))){
                    ready(scenario);capture(scenario,language,theme,scale);
                    scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);});
                    for(int position=0;position<3;position++){
                        final int row=position;scenario.onActivity(a->a.list.setSelectionFromTop(row,0));awaitLayout(scenario);
                        scenario.onActivity(a->{
                            View visible=a.list.getChildAt(row-a.list.getFirstVisiblePosition());assertNotNull("The requested small-sample row is laid out",visible);MainActivity.HomeHolder holder=(MainActivity.HomeHolder)visible.getTag();
                            JSONObject project=a.items.get(row);int contentEdge=left(holder.card)+holder.card.getPaddingLeft(),expectedLeft=contentEdge;
                            assertEquals("Project identity begins at the card content edge",expectedLeft,left(holder.name));
                            assertEquals("Counts share the project identity edge",expectedLeft,left(holder.counts));assertEquals("State begins at the card content edge",contentEdge,left(holder.state));
                            int expectedWidth=holder.card.getWidth()-holder.card.getPaddingLeft()-holder.card.getPaddingRight()-Ui.dp(a,16);
                            assertEquals("The project name uses all space before the chevron",expectedWidth,holder.name.getWidth());assertEquals(Ui.dp(a,232),holder.name.getWidth());
                            assertEquals(project.optString("name"),holder.name.getText().toString());assertCompleteText(holder.name);assertCompleteText(holder.counts);assertCompleteText(holder.state);
                            assertEquals(ProjectPresentation.counts(project),holder.counts.getText().toString());assertEquals(ProjectPresentation.state(project),holder.state.getText().toString());
                            assertTrue(holder.card.getHeight()>=Ui.dp(a,48));assertTrue(holder.card.getWidth()>=Ui.dp(a,48));assertTrue(holder.card.isFocusable());assertTrue(holder.card.isEnabled());
                            AccessibilityNodeInfo node=holder.card.createAccessibilityNodeInfo();try{assertEquals(project.optString("name")+", "+ProjectPresentation.state(project)+", "+ProjectPresentation.counts(project)+", "+holder.date.getContentDescription()+(holder.pending.getVisibility()==View.VISIBLE?", "+holder.pending.getText():"")+(holder.unavailable.getVisibility()==View.VISIBLE?", "+holder.unavailable.getText():""),String.valueOf(node.getContentDescription()));assertTrue(node.isClickable());assertTrue(node.isFocusable());assertTrue(node.isEnabled());}finally{node.recycle();}
                            assertEquals(0,a.probeStore.syncCalls.get());assertFalse(a.backgroundSyncEnabled());assertSafe(a);
                        });
                    }
                }
            }
        }finally{DemoHomeRecoveryActivity.hierarchyFontScale=previous;}
    }
    static int left(View view){int[] point=new int[2];view.getLocationOnScreen(point);return point[0];}
    static Rect bounds(View view){int[] point=new int[2];view.getLocationOnScreen(point);return new Rect(point[0],point[1],point[0]+view.getWidth(),point[1]+view.getHeight());}
    static void capture(ActivityScenario<DemoHomeRecoveryActivity> scenario,String language,String theme,float scale)throws Exception{
        String phase=InstrumentationRegistry.getArguments().getString("captureHomeHierarchyUi","");if(phase.isEmpty())return;assertTrue(phase.equals("red")||phase.equals("green"));
        JSONObject[] metadata={null};scenario.onActivity(a->{try{
            MainActivity.HomeHolder holder=(MainActivity.HomeHolder)a.list.getChildAt(0).getTag();assertCompleteText(a.demoNotice);assertFullVisibility(a.demoNotice);assertSafe(a);
            metadata[0]=new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("synthetic_projects",true).put("name",holder.name.getText().toString()).put("name_bounds",bounds(holder.name).toShortString()).put("name_width",holder.name.getWidth()).put("card_bounds",bounds(holder.card).toShortString()).put("card_height",holder.card.getHeight()).put("content_left",left(holder.card)+holder.card.getPaddingLeft()).put("name_left",left(holder.name)).put("counts",holder.counts.getText().toString()).put("state",holder.state.getText().toString()).put("card_description",holder.card.getContentDescription()).put("marker",a.demoNotice.getText().toString()).put("forbidden_actions",a.forbiddenActions.get()).put("sync_calls",a.probeStore.syncCalls.get());
        }catch(Exception error){throw new AssertionError(error);}});
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/home-hierarchy-"+phase+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="home-"+language+"-"+theme+"-font"+(int)scale;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());
        Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{screenshot.recycle();}
    }
}
