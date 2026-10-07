package app.droprun;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Observe the unchanged first screen, including controls that may be below its viewport. */
public class ShareSixProjectsCaptureTest {
    static boolean unresolvedLifetime;
    @Test public void sixCachedProjectsExposeInitialReadFeedback()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String mode=args.getString("shareSixProjectsProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=args.getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);assertTrue(Build.VERSION.SDK_INT>=30);assertFalse(unresolvedLifetime);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoShareSixProjectsActivity.class),0).exported);
        File base=target.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/share-six-projects-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());
        String oldLanguage=L.chinese()?"zh":"en";boolean oldDark=Ui.dark;int[] palette=HistoryShareStateTest.palette(),counts=new int[7];
        try{for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){
            assertFalse(unresolvedLifetime);assertEquals(counts[0],counts[2]);
            ActivityScenario<DemoShareSixProjectsActivity> window=null;DemoShareSixProjectsActivity[] held={null};Throwable failure=null;boolean full=false;
            try{
                unresolvedLifetime=true;
                window=ActivityScenario.launch(new Intent(target,DemoShareSixProjectsActivity.class).putExtra("shareSixProjectsProbe","accepted").putExtra("evidenceNonce",nonce).putExtra("language",config[0]).putExtra("appearance",config[1]));counts[0]++;
                window.onActivity(a->held[0]=a);settled(window,true);
                String[][] before={null};View[][] originalRows={null};int[] renders={0};
                window.onActivity(a->{safe(a,nonce);before[0]=values(a);originalRows[0]=rows(a);renders[0]=a.renderCalls;assertState(a,true);event(a,"entered",counts);});
                capture(window,directory,nonce,"loading",true,counts);
                window.onActivity(DemoShareSixProjectsActivity::releaseRead);settled(window,false);
                window.onActivity(a->{safe(a,nonce);assertState(a,false);assertArrayEquals(before[0],values(a));View[] current=rows(a);assertEquals(originalRows[0].length,current.length);for(int i=0;i<current.length;i++)assertSame(originalRows[0][i],current[i]);assertEquals(renders[0],a.renderCalls);});
                capture(window,directory,nonce,"failure",false,counts);full=true;
            }catch(Throwable error){failure=error;throw error;}
            finally{if(window!=null){DemoShareSixProjectsActivity a=held[0];try{
                assertNotNull(a);a.releaseRead();InstrumentationRegistry.getInstrumentation().runOnMainSync(a::allowScenarioClose);window.close();assertEquals(Lifecycle.State.DESTROYED,window.getState());assertTrue(a.isDestroyed());
                assertTrue("Owned worker must drain before another window",a.io.awaitTermination(3,TimeUnit.SECONDS));
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertEquals(0,a.forbiddenActions.get());assertNull(a.incoming);assertEquals(1,a.finishCalls);assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());assertEquals(0,a.finished.getCount());assertEquals(1,a.projectReads.get());assertEquals(1,a.activityReads.get());assertEquals(1,a.cacheApplies.get());counts[2]++;counts[4]+=a.projectReads.get();counts[5]+=a.activityReads.get();counts[6]+=a.cacheApplies.get();event(a,"DESTROYED",counts);});
                unresolvedLifetime=false;if(full){counts[1]++;event(a,"full-completed",counts);}
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            finally{if(a!=null)a.releaseRead();}}}
        }}finally{if(!unresolvedLifetime&&counts[0]==counts[2])HistoryShareStateTest.restore(oldLanguage,oldDark,palette);}
        assertFalse(unresolvedLifetime);assertArrayEquals(new int[]{2,2,2,4,2,2,2},counts);assertEquals(oldLanguage,L.chinese()?"zh":"en");assertEquals(oldDark,Ui.dark);assertArrayEquals(palette,HistoryShareStateTest.palette());
        send("SHARE_SIX_PROJECTS_SUMMARY",new JSONObject().put("nonce",nonce).put("entered",counts[0]).put("completed",counts[1]).put("DESTROYED",counts[2]).put("captures",counts[3]).put("projects_reads",counts[4]).put("activity_reads",counts[5]).put("memory_cache_applies",counts[6]).put("statics_restored",true).put("capture_accepted",false).put("scope","two fixed six-cache font1 windows; initial scroll only; no click, import, Send, Pair, network, business storage, IME or OS write"));
    }
    static void safe(DemoShareSixProjectsActivity a,String nonce){
        assertEquals(nonce,a.nonce);assertEquals(0,a.forbiddenActions.get());assertEquals(1,a.initializationCloses);assertNull(a.incoming);assertFalse(a.sent);assertFalse(a.closing);assertFalse(a.discardOnFinish);assertFalse(TaskSyncService.running);
        assertSame(a.memory,a.store);assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertFalse(a.memory.editorOpen);assertEquals(a.catalog,a.memory.cache.get("projects"));assertEquals(DemoShareSixProjectsActivity.ACTIVITY,a.memory.cache.get("activity"));
        assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertEquals(a.language.equals("zh"),L.chinese());assertEquals(a.theme.equals("dark"),Ui.dark);
        assertEquals(DemoShareSixProjectsActivity.MARKER,a.marker.getText().toString());assertEquals(0,a.step);assertEquals(0,a.scroll.getScrollY());assertEquals(0,a.scroll.getScrollX());assertFalse(a.materialOpen);assertFalse(a.showAll);assertNull(a.note);assertEquals(View.GONE,a.search.getVisibility());
        View[] rows=rows(a);assertEquals(6,rows.length);assertEquals(6,a.store.projects().length());for(String name:DemoShareSixProjectsActivity.NAMES)assertNotNull(ShareEditorTest.findText(a.projectList,name));for(View row:rows){assertTrue(row.isEnabled());assertTrue(row.isClickable());assertTrue(row.getHeight()>=Ui.dp(a,64));}
    }
    static void assertState(DemoShareSixProjectsActivity a,boolean loading){
        assertEquals(loading,a.projectRead==a.memory);assertEquals(!loading,a.projectReadFailed);assertEquals(1,a.projectReads.get());assertEquals(loading?0:1,a.activityReads.get());assertEquals(loading?0:1,a.cacheApplies.get());
        assertEquals(loading?L.t("Loading projects…","正在读取项目…"):L.t("Couldn't refresh projects. Showing saved projects.","项目刷新失败，仍显示上次同步的项目。"),a.projectReadStatus.getText().toString());
        assertEquals(loading?L.t("Loading…","正在读取…"):L.t("Retry","重试"),a.projectRefresh.getText().toString());assertEquals(!loading,a.projectRefresh.isEnabled());assertEquals(loading?0.5f:1f,a.projectRefresh.getAlpha(),0f);assertTrue(a.projectRefresh.isFocusable());assertTrue(a.projectRefresh.getHeight()>=Ui.dp(a,48));
        ShareEditorTest.assertCompleteText(a.projectReadStatus);ShareEditorTest.assertCompleteText(a.projectRefresh);
    }
    static String[] values(DemoShareSixProjectsActivity a){return new String[]{a.shared,a.attachments.toString(),a.draft,a.selected,a.model,a.effort,a.query,""+a.step,""+a.materialOpen,""+a.showAll};}
    static View[] rows(DemoShareSixProjectsActivity a){List<View> rows=new ArrayList<>();for(int i=0;i<a.projectList.getChildCount();i++){View view=a.projectList.getChildAt(i);if(view.isClickable())rows.add(view);}return rows.toArray(new View[0]);}
    static boolean ready(DemoShareSixProjectsActivity a){return a.hasWindowFocus()&&a.root.isAttachedToWindow()&&!a.root.isLayoutRequested()&&!a.holder.isLayoutRequested()&&!a.stage.isLayoutRequested()&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f&&a.stage.getChildCount()==1&&a.stage.getChildAt(0).getAlpha()==1f;}
    static void settled(ActivityScenario<DemoShareSixProjectsActivity> window,boolean loading)throws Exception{
        long deadline=SystemClock.uptimeMillis()+3000;boolean[] found={false};do{window.onActivity(a->found[0]=ready(a)&&(loading?a.projectRead==a.memory&&a.entered.getCount()==0:a.projectRead==null&&a.finished.getCount()==0));if(found[0])return;Thread.sleep(16);}while(SystemClock.uptimeMillis()<deadline);fail("Six-project read/layout did not settle");
    }
    static void frame(ActivityScenario<DemoShareSixProjectsActivity> window)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);window.onActivity(a->{a.root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(ready(a)){a.root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else a.root.postInvalidateOnAnimation();return true;}});a.root.invalidate();});assertTrue("Actual pre-draw",frame.await(3,TimeUnit.SECONDS));
    }
    static Rect bounds(View view){int[] at=new int[2];view.getLocationOnScreen(at);return new Rect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}
    static JSONObject geometry(View view,Rect viewport)throws JSONException{
        Rect visible=new Rect(),whole=bounds(view),intersection=new Rect(whole);boolean shown=view.getGlobalVisibleRect(visible),intersects=intersection.intersect(viewport);if(!intersects)intersection.setEmpty();
        return new JSONObject().put("bounds",whole.toShortString()).put("visible_rect",visible.toShortString()).put("viewport_intersection",intersection.toShortString()).put("has_visible_rect",shown).put("whole_target",shown&&visible.equals(whole)&&viewport.contains(whole));
    }
    static void capture(ActivityScenario<DemoShareSixProjectsActivity> window,File directory,String nonce,String phase,boolean loading,int[] counts)throws Exception{
        frame(window);frame(window);long settledAt=SystemClock.elapsedRealtime();Thread.sleep(2000);settled(window,loading);JSONObject[] data={null};String[] stem={null};
        window.onActivity(a->{safe(a,nonce);assertState(a,loading);assertTrue(ready(a));assertEquals(1f,a.getWindow().getAttributes().alpha,0f);WindowInsets insets=a.root.getRootWindowInsets();assertNotNull(insets);assertFalse("No keyboard capture",insets.isVisible(WindowInsets.Type.ime()));
            Rect safe=new Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());assertTrue(safe.intersect(new Rect(bars.left,bars.top,320-bars.right,640-bars.bottom)));
            Rect viewport=new Rect();assertTrue(a.scroll.getGlobalVisibleRect(viewport));assertTrue(viewport.intersect(safe));View header=(View)a.back.getParent();assertEquals(Ui.dp(a,48),header.getHeight());assertTrue(safe.contains(bounds(header)));assertTrue(safe.contains(bounds(a.marker)));ShareEditorTest.assertCompleteText(a.marker);assertTrue(bounds(a.marker).top>=bounds(a.dots).bottom);
            TextView title=ShareEditorTest.findText(a.stage,L.t("Where should this idea go?","转发给哪个项目？"));assertNotNull(title);assertTrue(viewport.contains(bounds(title)));ShareEditorTest.assertCompleteText(title);
            try{JSONArray rows=new JSONArray();for(View row:rows(a))rows.put(geometry(row,viewport));stem[0]="share-six-projects-"+a.language+"-"+a.theme+"-"+phase;
                data[0]=new JSONObject().put("nonce",nonce).put("language",a.language).put("theme",a.theme).put("state",phase).put("font_scale",1).put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("capture_accepted",false).put("scroll_x",a.scroll.getScrollX()).put("scroll_y",a.scroll.getScrollY()).put("project_count",a.store.projects().length()).put("row_count",rows.length()).put("rows",rows).put("safe_bounds",safe.toShortString()).put("scroll_viewport",viewport.toShortString()).put("status",a.projectReadStatus.getText()).put("status_geometry",geometry(a.projectReadStatus,viewport)).put("refresh",a.projectRefresh.getText()).put("refresh_geometry",geometry(a.projectRefresh,viewport)).put("refresh_enabled",a.projectRefresh.isEnabled()).put("marker",a.marker.getText()).put("marker_bounds",bounds(a.marker).toShortString()).put("header_bounds",bounds(header).toShortString()).put("marker_added_vertical_space_dp",0).put("required_predraws_completed",2).put("settled_at_elapsed_ms",settledAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("window_focus",a.hasWindowFocus()).put("ime_visible",false).put("forbidden",a.forbiddenActions.get()).put("projects_reads",a.projectReads.get()).put("activity_reads",a.activityReads.get()).put("memory_cache_applies",a.cacheApplies.get()).put("system_bar_pixels_require_visual_review",true);
            }catch(JSONException error){throw new AssertionError(error);}});
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(OutputStream output=Files.newOutputStream(new File(directory,stem[0]+".png").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));}Files.write(new File(directory,stem[0]+".json").toPath(),data[0].toString(2).getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}finally{image.recycle();}
        counts[3]++;window.onActivity(a->{safe(a,nonce);assertState(a,loading);event(a,"captured-"+phase,counts);});
    }
    static void event(DemoShareSixProjectsActivity a,String phase,int[] counts){
        try{send("SHARE_SIX_PROJECTS_EVENT",new JSONObject().put("nonce",a.nonce).put("language",a.language).put("theme",a.theme).put("event",phase).put("forbidden",a.forbiddenActions.get()).put("projects_reads",a.projectReads.get()).put("activity_reads",a.activityReads.get()).put("memory_cache_applies",a.cacheApplies.get()).put("destroyed",a.isDestroyed()).put("io_terminated",a.io.isTerminated()).put("checkpoint_noops",a.checkpointNoops).put("captures_completed",counts[3]));}catch(JSONException error){throw new AssertionError(error);}
    }
    static void send(String prefix,JSONObject data){Bundle result=new Bundle();result.putString("stream",prefix+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,result);}
}
