package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Two closed memory windows; real refresh/callback and performClick, no import, IME or send. */
public class ShareProjectLoadTest {
    static boolean unresolvedLifetime;
    @Test public void projectReadStatesPreserveTheShareAndIgnoreLateUi()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("shareProjectLoadProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertFalse("No next launch after unknown lifetime",unresolvedLifetime);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoShareProjectLoadActivity.class),0).exported);
        String oldLanguage=L.chinese()?"zh":"en";boolean oldDark=Ui.dark;int[] palette=HistoryShareStateTest.palette(),totals=new int[8];
        try{for(String[] config:new String[][]{{"en","light","empty"},{"zh","dark","cached"}}){
            assertFalse(unresolvedLifetime);assertEquals(totals[0],totals[3]);
            ActivityScenario<DemoShareProjectLoadActivity> window=null;DemoShareProjectLoadActivity[] held={null};Throwable failure=null;boolean full=false;
            boolean[] late={false};int[][] lateUi={null};String[][] lateValues={null};View[][] lateControls={null};
            try{
                unresolvedLifetime=true;
                window=ActivityScenario.launch(new Intent(target,DemoShareProjectLoadActivity.class).putExtra("shareProjectLoadProbe","accepted").putExtra("evidenceNonce",nonce).putExtra("language",config[0]).putExtra("appearance",config[1]).putExtra("scene",config[2]));totals[0]++;
                window.onActivity(a->held[0]=a);settled(window,true);
                window.onActivity(a->{safe(a,nonce);totals[1]++;assertEquals(0,a.step);assertControl(a,true,false);assertEquals(L.t("Loading projects…","正在读取项目…"),a.projectReadStatus.getText().toString());event(a,"entered",totals);});
                if(config[2].equals("empty")){
                    String[][] before={null};View[][] controls={null};int[] renders={0};
                    window.onActivity(a->{before[0]=values(a);controls[0]=projectControls(a);renders[0]=a.renderCalls;assertEquals(0,a.projectList.getChildCount());repeatWhilePending(a);a.releaseRead();});settled(window,false);
                    window.onActivity(a->{safe(a,nonce);assertTrue(a.projectReadFailed);assertEquals("Couldn't load projects. Check your Relay connection, then retry.",a.projectReadStatus.getText().toString());assertControl(a,false,true);assertEquals(0,a.projectList.getChildCount());assertEquals(renders[0],a.renderCalls);unchanged(a,before[0],controls[0]);assertReads(a,1,1,1);event(a,"empty-failed",totals);a.nextRead();assertTrue(a.projectRefresh.performClick());});settled(window,true);
                    window.onActivity(a->{assertControl(a,true,false);assertEquals(0,a.projectList.getChildCount());unchanged(a,before[0],controls[0]);repeatWhilePending(a);a.releaseRead();});settled(window,false);
                    window.onActivity(a->{safe(a,nonce);assertFalse("Activity failure is not catalog failure",a.projectReadFailed);assertEquals("No projects synced yet. Check Connector, add or connect a project, then refresh.",a.projectReadStatus.getText().toString());assertControl(a,false,false);assertEquals(0,a.projectList.getChildCount());assertEquals(renders[0],a.renderCalls);unchanged(a,before[0],controls[0]);assertReads(a,2,2,2);event(a,"confirmed-empty-activity-failed",totals);a.nextRead();assertTrue(a.projectRefresh.performClick());});settled(window,true);
                    window.onActivity(a->{unchanged(a,before[0],controls[0]);repeatWhilePending(a);a.releaseRead();});settled(window,false);
                    window.onActivity(a->{safe(a,nonce);assertFalse(a.projectReadFailed);assertEquals(View.GONE,a.projectReadStatus.getVisibility());assertControl(a,false,false);assertEquals(2,rows(a).length);assertEquals(renders[0]+1,a.renderCalls);unchanged(a,before[0],controls[0]);assertReads(a,3,3,4);event(a,"populated",totals);});
                }else{
                    String[][] cache={null},noteValues={null};View[][] originalRows={null},noteControls={null};int[][] noteUi={null};
                    window.onActivity(a->{safe(a,nonce);originalRows[0]=rows(a);assertEquals(2,originalRows[0].length);cache[0]=new String[]{a.memory.cache.get("projects"),a.memory.cache.get("activity")};int renders=a.renderCalls;repeatWhilePending(a);assertEquals(renders,a.renderCalls);assertSameRows(originalRows[0],rows(a));event(a,"cached-pending",totals);View alpha=rowNamed(a,"Memory Alpha");assertTrue(alpha.isEnabled());assertTrue(alpha.isClickable());assertTrue(alpha.performClick());});settled(window,true);
                    window.onActivity(a->{safe(a,nonce);assertEquals(1,a.step);assertEquals(DemoShareProjectLoadActivity.ALPHA,a.selected);assertEquals("memory-model",a.model);assertEquals("high",a.effort);assertEquals("Preserve this memory note. 保留留言。",a.note.getText().toString());assertFalse(a.note.getShowSoftInputOnFocus());noteValues[0]=values(a);noteControls[0]=editorControls(a);noteUi[0]=uiCounts(a);a.releaseRead();});settled(window,false);
                    window.onActivity(a->{safe(a,nonce);assertTrue(a.projectReadFailed);assertArrayEquals(noteValues[0],values(a));assertSameControls(noteControls[0],editorControls(a));assertArrayEquals(noteUi[0],uiCounts(a));assertEquals(cache[0][0],a.memory.cache.get("projects"));assertEquals(cache[0][1],a.memory.cache.get("activity"));assertSameRows(originalRows[0],rows(a));assertReads(a,1,1,1);event(a,"cached-failure-kept-editor",totals);assertTrue(a.back.performClick());});settled(window,false);
                    window.onActivity(a->{safe(a,nonce);assertEquals(0,a.step);assertEquals("项目刷新失败，仍显示上次同步的项目。",a.projectReadStatus.getText().toString());assertControl(a,false,true);assertEquals(2,rows(a).length);event(a,"cached-failure-visible",totals);a.nextRead();assertTrue(a.projectRefresh.performClick());});settled(window,true);
                    window.onActivity(a->{safe(a,nonce);repeatWhilePending(a);assertEquals(2,rows(a).length);assertControl(a,true,false);lateUi[0]=uiCounts(a);lateValues[0]=values(a);lateControls[0]=projectControls(a);late[0]=true;event(a,"closing-with-held-read",totals);});
                }
                full=true;
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(window!=null){DemoShareProjectLoadActivity a=held[0];try{
                    assertNotNull(a);if(!late[0])a.releaseRead();
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(a::allowScenarioClose);
                    window.close();assertEquals(Lifecycle.State.DESTROYED,window.getState());assertTrue(a.isDestroyed());
                    // The second cached read returns only after actual destruction, through the real callback.
                    a.releaseRead();assertTrue("Owned worker must drain",a.io.awaitTermination(3,TimeUnit.SECONDS));
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                        safe(a,nonce);assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());assertEquals(1,a.finishCalls);assertEquals(0,a.read.finished.getCount());
                        if(late[0]){assertSame(a.memory,a.projectRead);assertArrayEquals(lateUi[0],uiCounts(a));assertArrayEquals(lateValues[0],values(a));assertSameControls(lateControls[0],projectControls(a));assertReads(a,2,2,3);event(a,"late-callback-no-ui",totals);}
                        totals[3]++;totals[5]+=a.projectReads.get();totals[6]+=a.activityReads.get();totals[7]+=a.cacheWrites.get();event(a,"DESTROYED",totals);
                    });unresolvedLifetime=false;if(full){totals[2]++;event(a,"full-completed",totals);}
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
                finally{if(a!=null)a.releaseRead();}}
            }
        }}finally{if(!unresolvedLifetime&&totals[0]==totals[3])HistoryShareStateTest.restore(oldLanguage,oldDark,palette);}
        assertFalse(unresolvedLifetime);assertEquals(2,totals[0]);assertEquals(2,totals[1]);assertEquals(2,totals[2]);assertEquals(2,totals[3]);assertEquals(5,totals[5]);assertEquals(5,totals[6]);assertEquals(7,totals[7]);
        assertEquals(oldLanguage,L.chinese()?"zh":"en");assertEquals(oldDark,Ui.dark);assertArrayEquals(palette,HistoryShareStateTest.palette());
        Bundle result=new Bundle();result.putString("stream","SHARE_PROJECT_LOAD_SUMMARY\t"+nonce+"\t"+new JSONObject().put("entered",totals[1]).put("completed",totals[2]).put("DESTROYED",totals[3]).put("actual_events",totals[4]).put("projects_reads",totals[5]).put("activity_reads",totals[6]).put("memory_cache_writes",totals[7]).put("statics_restored",true).put("scope","memory read/callback states only; no import, persistent draft, network, IME, send, capture or stale-store runtime claim")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,result);
    }
    static void safe(DemoShareProjectLoadActivity a,String nonce){
        assertEquals(nonce,a.nonce);assertEquals(0,a.forbiddenActions.get());assertEquals(1,a.initializationCloses);assertNull(a.incoming);assertFalse(a.sent);assertFalse(a.closing);assertFalse(a.discardOnFinish);assertFalse(TaskSyncService.running);
        assertSame(a.memory,a.store);assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertNull(a.memory.writeKey);assertFalse(a.memory.editorOpen);
        assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(a.language.equals("zh"),L.chinese());assertEquals(a.theme.equals("dark"),Ui.dark);assertEquals(DemoShareProjectLoadActivity.MARKER,a.marker.getText().toString());
    }
    static void repeatWhilePending(DemoShareProjectLoadActivity a){
        assertSame(a.memory,a.projectRead);assertEquals(0,a.read.entered.getCount());int reads=a.projectReads.get();a.refreshProjects(false);assertSame(a.memory,a.projectRead);assertEquals(reads,a.projectReads.get());assertEquals(0,a.forbiddenActions.get());
    }
    static void assertControl(DemoShareProjectLoadActivity a,boolean loading,boolean failed){
        TextView control=a.projectRefresh;assertEquals(!loading,control.isEnabled());assertEquals(loading?0.5f:1f,control.getAlpha(),0f);assertTrue(control.isClickable());assertTrue(control.isFocusable());assertTrue(control.getHeight()>=Ui.dp(a,48));
        assertEquals(loading?L.t("Loading…","正在读取…"):failed?L.t("Retry","重试"):L.t("Refresh","刷新"),control.getText().toString());assertEquals(loading?L.t("Loading projects","正在读取项目"):failed?L.t("Retry loading projects","重试读取项目"):L.t("Refresh projects","刷新项目"),String.valueOf(control.getContentDescription()));ShareEditorTest.assertCompleteText(control);if(a.projectReadStatus.getVisibility()==View.VISIBLE)ShareEditorTest.assertCompleteText(a.projectReadStatus);
    }
    static String[] values(DemoShareProjectLoadActivity a){return new String[]{a.shared,a.attachments.toString(),a.draft,a.selected,a.model,a.effort,a.query,""+a.showAll,""+a.materialOpen,""+a.step,a.note==null?"<no editor>":a.note.getText().toString()};}
    static View[] projectControls(DemoShareProjectLoadActivity a){return new View[]{a.projectReadStatus,a.projectRefresh,a.projectList,a.search};}
    static View[] editorControls(DemoShareProjectLoadActivity a){return new View[]{a.note,a.gaugeText,a.panel,a.back,a.stage.findViewWithTag("share-send")};}
    static int[] uiCounts(DemoShareProjectLoadActivity a){return new int[]{a.renderCalls,a.statusCalls,a.gaugeCalls};}
    static void unchanged(DemoShareProjectLoadActivity a,String[] values,View[] controls){assertArrayEquals(values,values(a));assertSameControls(controls,projectControls(a));}
    static void assertSameControls(View[] before,View[] after){assertEquals(before.length,after.length);for(int i=0;i<before.length;i++){assertNotNull(before[i]);assertSame(before[i],after[i]);}}
    static View[] rows(DemoShareProjectLoadActivity a){List<View> rows=new ArrayList<>();for(int i=0;i<a.projectList.getChildCount();i++){View child=a.projectList.getChildAt(i);if(child.isClickable())rows.add(child);}return rows.toArray(new View[0]);}
    static void assertSameRows(View[] before,View[] after){assertSameControls(before,after);}
    static View rowNamed(DemoShareProjectLoadActivity a,String label){for(View row:rows(a))if(ShareEditorTest.findText(row,label)!=null)return row;throw new AssertionError("Missing memory project "+label);}
    static void assertReads(DemoShareProjectLoadActivity a,int projects,int activity,int writes){assertEquals(projects,a.projectReads.get());assertEquals(activity,a.activityReads.get());assertEquals(writes,a.cacheWrites.get());}
    static void settled(ActivityScenario<DemoShareProjectLoadActivity> window,boolean pending)throws Exception{
        long deadline=SystemClock.uptimeMillis()+3000;boolean[] ready={false};do{
            window.onActivity(a->{ready[0]=a.hasWindowFocus()&&!a.holder.isLayoutRequested()&&!a.stage.isLayoutRequested()&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f&&(pending?a.projectRead==a.memory&&a.read.entered.getCount()==0:a.projectRead==null&&a.read.finished.getCount()==0)&&a.stage.getChildCount()==1;});
            if(ready[0])return;Thread.sleep(16);
        }while(SystemClock.uptimeMillis()<deadline);fail("Memory project read/layout did not settle");
    }
    static void event(DemoShareProjectLoadActivity a,String phase,int[] totals){
        try{totals[4]++;JSONObject data=new JSONObject().put("nonce",a.nonce).put("language",a.language).put("theme",a.theme).put("scene",a.scene).put("event",phase).put("forbidden",a.forbiddenActions.get()).put("projects_reads",a.projectReads.get()).put("activity_reads",a.activityReads.get()).put("memory_cache_writes",a.cacheWrites.get()).put("read_inflight",a.projectRead!=null).put("read_failed",a.projectReadFailed).put("render_calls",a.renderCalls).put("status_calls",a.statusCalls).put("gauge_calls",a.gaugeCalls).put("checkpoint_noops",a.checkpointNoops).put("keyboard_noops",a.keyboardNoops).put("step",a.step).put("destroyed",a.isDestroyed()).put("io_terminated",a.io.isTerminated());Bundle result=new Bundle();result.putString("stream","SHARE_PROJECT_LOAD_EVENT\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,result);}catch(Exception error){throw new AssertionError(error);}
    }
}
