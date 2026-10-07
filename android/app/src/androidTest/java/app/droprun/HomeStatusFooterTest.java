package app.droprun;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import static app.droprun.HomeRecoveryFeedbackTest.*;
import static org.junit.Assert.*;

/** Draft: only hierarchyProbe memory rows and native layout; no click, sync or capture writer. */
public class HomeStatusFooterTest {
    static boolean unresolvedLifetime;
    @Test public void recycledStatusKeepsTextAndFillTogether()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("homeStatusFooterProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        assertFalse("Do not start another method after an unresolved probe window",unresolvedLifetime);
        float previous=DemoHomeRecoveryActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";
        boolean[] destroyed={true};
        try{
            DemoHomeRecoveryActivity.hierarchyFontScale=1f;
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"}){
                String lifetime="recycledStatusKeepsTextAndFillTogether|"+language+"|"+theme+"|font1";
                ActivityScenario<DemoHomeRecoveryActivity> scenario=null;Throwable failure=null;
                try{
                    scenario=launchProbe(language,theme,destroyed,lifetime);
                    ready(scenario);lifecycle(lifetime,"entered");View[] recycled={null};MainActivity.HomeHolder[] identity={null};String[] id={null},name={null},counts={null};long[] stableId={0};
                    scenario.onActivity(a->{safe(a);recycled[0]=a.list.getChildAt(0);identity[0]=(MainActivity.HomeHolder)recycled[0].getTag();id[0]=identity[0].id;name[0]=identity[0].name.getText().toString();counts[0]=identity[0].counts.getText().toString();stableId[0]=a.adapter.getItemId(0);});
                    for(int phase=0;phase<4;phase++){
                        final int step=phase;String[] anchorBefore={null};int[] offsetBefore={0};View[] focusBefore={null};
                        scenario.onActivity(a->{try{
                            anchorBefore[0]=anchor(a);offsetBefore[0]=offset(a);focusBefore[0]=a.getWindow().getDecorView().findFocus();
                            JSONObject project=a.items.get(0);project.put("active_count",step==1?2:0).put("attention_count",step==2?3:0);
                            assertSame("Rebind the exact already laid-out outer row",recycled[0],a.adapter.getView(0,recycled[0],a.list));assertSame(identity[0],recycled[0].getTag());
                            MainActivity.HomeHolder holder=identity[0];int color=step==2?Ui.AMBER:step==1?Ui.ACCENT:Ui.MUTED;
                            assertEquals(ProjectPresentation.state(project),holder.state.getText().toString());assertEquals(color,holder.state.getCurrentTextColor());
                            assertEquals(id[0],holder.id);assertEquals(name[0],holder.name.getText().toString());assertEquals(counts[0],holder.counts.getText().toString());assertEquals(stableId[0],a.adapter.getItemId(0));
                            LinearLayout footer=(LinearLayout)holder.counts.getParent();assertEquals(LinearLayout.HORIZONTAL,footer.getOrientation());assertEquals(Gravity.END,(holder.date.getGravity()&Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK));
                            LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)holder.date.getLayoutParams();assertEquals(-2,params.width);assertEquals(0f,params.weight,0f);assertEquals(0,params.topMargin);
                            assertSame("A bind does not replace the currently focused View",focusBefore[0],a.getWindow().getDecorView().findFocus());assertCard(a,holder,project);safe(a);
                        }catch(org.json.JSONException error){throw new AssertionError(error);}});
                        awaitLayout(scenario);scenario.onActivity(a->{assertAnchor(a,anchorBefore[0],offsetBefore[0]);assertSame(focusBefore[0],a.getWindow().getDecorView().findFocus());assertCompleteText(identity[0].name);assertCompleteText(identity[0].state);assertCompleteText(identity[0].date);safe(a);});
                    }
                    lifecycle(lifetime,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,lifetime);}
            }
        }finally{if(destroyed[0]){DemoHomeRecoveryActivity.hierarchyFontScale=previous;L.language(languageBefore);}}
    }

    @Test public void doubleFontDateIsBelowStatusAndReachable()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("homeStatusFooterProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        assertFalse("Do not start another method after an unresolved probe window",unresolvedLifetime);
        float previous=DemoHomeRecoveryActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";
        boolean[] destroyed={true};
        try{
            DemoHomeRecoveryActivity.hierarchyFontScale=2f;
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"}){
                String lifetime="doubleFontDateIsBelowStatusAndReachable|"+language+"|"+theme+"|font2";
                ActivityScenario<DemoHomeRecoveryActivity> scenario=null;Throwable failure=null;
                try{
                    scenario=launchProbe(language,theme,destroyed,lifetime);
                    ready(scenario);lifecycle(lifetime,"entered");
                    scenario.onActivity(a->{try{for(JSONObject project:a.items)project.put("attention_count",3).put("active_count",2).put("last_dispatch_at",System.currentTimeMillis()-9999L*86400000L);a.adapter.notifyDataSetChanged();safe(a);}catch(org.json.JSONException error){throw new AssertionError(error);}});awaitLayout(scenario);
                    for(int position=0;position<3;position++){
                        final int row=position;scenario.onActivity(a->a.list.setSelectionFromTop(row,0));awaitLayout(scenario);
                        View[] bound={null},focused={null};String[] anchorBefore={null};int[] offsetBefore={0};MainActivity.HomeHolder[] identity={null};long[] stableId={0};
                        scenario.onActivity(a->{
                            safe(a);assertEquals(2f,a.getResources().getConfiguration().fontScale,0f);bound[0]=a.list.getChildAt(row-a.list.getFirstVisiblePosition());assertNotNull(bound[0]);identity[0]=(MainActivity.HomeHolder)bound[0].getTag();
                            anchorBefore[0]=anchor(a);offsetBefore[0]=offset(a);focused[0]=a.getWindow().getDecorView().findFocus();stableId[0]=a.adapter.getItemId(row);
                            assertSame(bound[0],a.adapter.getView(row,bound[0],a.list));assertSame(identity[0],bound[0].getTag());
                        });
                        awaitLayout(scenario);scenario.onActivity(a->{
                            MainActivity.HomeHolder holder=identity[0];JSONObject project=a.items.get(row);LinearLayout footer=(LinearLayout)holder.counts.getParent();
                            assertEquals(LinearLayout.VERTICAL,footer.getOrientation());assertEquals(Gravity.START,(holder.date.getGravity()&Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK));LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)holder.date.getLayoutParams();assertEquals(-1,params.width);assertEquals(0f,params.weight,0f);assertEquals(Ui.dp(a,8),params.topMargin);
                            LinearLayout.LayoutParams stateParams=(LinearLayout.LayoutParams)holder.state.getLayoutParams();assertEquals(-1,stateParams.width);assertEquals(-2,stateParams.height);
                            Rect counts=HomeProjectHierarchyTest.bounds(holder.counts),date=HomeProjectHierarchyTest.bounds(holder.date);assertEquals(counts.left,date.left);assertTrue("Date follows the complete counts with spacing",date.top>=counts.bottom+Ui.dp(a,8));
                            assertEquals(L.t("9999 days ago","9999 天前"),holder.date.getText().toString());assertEquals(L.t("Last handoff · ","最近交办 · ")+holder.date.getText(),holder.date.getContentDescription());
                            assertCompleteText(holder.name);assertCompleteText(holder.counts);assertCompleteText(holder.state);assertCompleteText(holder.date);assertFullVisibility(holder.date);
                            Rect safeBounds=new Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safeBounds);assertTrue(safeBounds.intersect(HomeProjectHierarchyTest.bounds(a.list)));assertTrue("Date is inside the visible window and actual list viewport",safeBounds.contains(date));
                            assertEquals(project.optString("name"),holder.name.getText().toString());assertEquals(Integer.MAX_VALUE,holder.name.getMaxLines());assertEquals(stableId[0],a.adapter.getItemId(row));assertSame(identity[0],bound[0].getTag());assertSame(focused[0],a.getWindow().getDecorView().findFocus());assertAnchor(a,anchorBefore[0],offsetBefore[0]);assertCard(a,holder,project);safe(a);
                        });
                    }
                    lifecycle(lifetime,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,lifetime);}
            }
        }finally{if(destroyed[0]){DemoHomeRecoveryActivity.hierarchyFontScale=previous;L.language(languageBefore);}}
    }
    static ActivityScenario<DemoHomeRecoveryActivity> launchProbe(String language,String theme,boolean[] destroyed,String lifetime)throws android.content.pm.PackageManager.NameNotFoundException{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);
        assertFalse("Only the existing guarded nonexported fixture",target.getPackageManager().getActivityInfo(new ComponentName(target,DemoHomeRecoveryActivity.class),0).exported);
        Intent intent=new Intent(target,DemoHomeRecoveryActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("hierarchyProbe",true);
        assertTrue("No next launch after an unresolved lifetime",destroyed[0]);destroyed[0]=false;unresolvedLifetime=true;
        // A launch exception may leave an unknown window without returning a handle.
        lifecycle(lifetime,"launch-attempt");ActivityScenario<DemoHomeRecoveryActivity> scenario=ActivityScenario.launch(intent);lifecycle(lifetime,"returned-handle");return scenario;
    }
    static void safe(DemoHomeRecoveryActivity a){assertTrue(a.getIntent().getBooleanExtra("hierarchyProbe",false));assertFalse(a.cacheOnlyProbe);assertFalse(a.backgroundSyncEnabled());assertEquals(0,a.probeStore.syncCalls.get());assertSafe(a);}
    static void assertCard(DemoHomeRecoveryActivity a,MainActivity.HomeHolder holder,JSONObject project){
        assertTrue(holder.card.getWidth()>=Ui.dp(a,48));assertTrue(holder.card.getHeight()>=Ui.dp(a,48));assertTrue(holder.card.isFocusable());assertTrue(holder.card.isClickable());assertTrue(holder.card.isEnabled());
        assertEquals(project.optString("name")+", "+ProjectPresentation.state(project)+", "+ProjectPresentation.counts(project)+", "+holder.date.getContentDescription()+(holder.pending.getVisibility()==View.VISIBLE?", "+holder.pending.getText():"")+(holder.unavailable.getVisibility()==View.VISIBLE?", "+holder.unavailable.getText():""),holder.card.getContentDescription());
        AccessibilityNodeInfo node=holder.card.createAccessibilityNodeInfo();try{assertTrue(node.isFocusable());assertTrue(node.isClickable());assertTrue(node.isEnabled());assertEquals(holder.card.getContentDescription(),node.getContentDescription());}finally{node.recycle();}
    }
    static void closeKnown(ActivityScenario<DemoHomeRecoveryActivity> scenario,Throwable failure,boolean[] destroyed,String lifetime)throws Throwable{
        if(scenario==null)return; // Unknown launch: no retry and no static/language restoration.
        try{
            scenario.close(); // Exactly one explicit close per known handle; no automatic close.
            assertEquals("Previous window must be DESTROYED before restoration or another launch",Lifecycle.State.DESTROYED,scenario.getState());destroyed[0]=true;unresolvedLifetime=false;lifecycle(lifetime,"DESTROYED");
        }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void lifecycle(String lifetime,String event){Bundle status=new Bundle();status.putString("stream","HOME_STATUS_FOOTER_LIFETIME\t"+lifetime+"\t"+event+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}

}
