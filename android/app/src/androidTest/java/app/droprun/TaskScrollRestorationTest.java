package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** One closed memory launch and one real Activity recreation. No source/file/Send/navigation action. */
public class TaskScrollRestorationTest {
    static boolean unresolvedLifetime;

    @Test public void expandedReportKeepsItsReadingPosition()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("taskScrollProbe","");if(phase.isEmpty())return;
        assertTrue(phase.equals("red")||phase.equals("accepted"));
        String nonce=InstrumentationRegistry.getArguments().getString("taskScrollNonce","");
        assertTrue(nonce.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        assertFalse(context.getPackageManager().getActivityInfo(new ComponentName(context,DemoTaskScrollActivity.class),0).exported);
        String language=L.chinese()?"zh":"en",theme=Ui.dark?"dark":"light";
        ActivityScenario<DemoTaskPreviewActivity> scenario=null;DemoTaskScrollActivity[] first={null},second={null};
        String[] original={null};int[] beforeY={0},beforeTop={0};boolean recreateRequested=false,recreateReturned=false;Throwable failure=null;
        try{
            unresolvedLifetime=true;event(nonce,phase,"launch-attempt",new JSONObject());
            scenario=ActivityScenario.launch(new Intent(context,DemoTaskScrollActivity.class).putExtra("taskScrollProbe",true).putExtra("nonce",nonce).putExtra("language",language).putExtra("appearance",theme));
            scenario.onActivity(a->{first[0]=(DemoTaskScrollActivity)a;safe(first[0],nonce,language,theme);original[0]=a.sample.toString();event(nonce,phase,"entered",metadata(first[0]));});
            settle(scenario);
            scenario.onActivity(a->{TextView title=TaskPreviewFeedbackTest.find(a.body,heading());assertNotNull(title);View header=(View)title.getParent();header.requestRectangleOnScreen(new Rect(0,0,header.getWidth(),header.getHeight()),true);});settle(scenario);
            scenario.onActivity(a->{TextView title=TaskPreviewFeedbackTest.find(a.body,heading());TaskReadingHierarchyTest.assertSafeRect(a,TaskPreviewFeedbackTest.screenBounds((View)title.getParent()));assertTrue(((View)title.getParent()).performClick());});settle(scenario);
            scenario.onActivity(a->{assertTrue(a.expanded.contains(heading()));TextView text=full(a);Layout layout=text.getLayout();assertNotNull(layout);int line=middleLine(text);text.requestRectangleOnScreen(new Rect(0,text.getTotalPaddingTop()+layout.getLineTop(line),text.getWidth(),text.getTotalPaddingTop()+layout.getLineBottom(line)),true);});settle(scenario);
            scenario.onActivity(a->{safe((DemoTaskScrollActivity)a,nonce,language,theme);Rect paragraph=middleBounds(a);TaskReadingHierarchyTest.assertSafeRect(a,paragraph);beforeY[0]=scroll(a).getScrollY();beforeTop[0]=paragraph.top;assertTrue("A meaningful nonzero reading position",beforeY[0]>scroll(a).getHeight());assertEquals(original[0],a.sample.toString());event(nonce,phase,"before-recreate",metadata((DemoTaskScrollActivity)a));});
            recreateRequested=true;scenario.recreate();recreateReturned=true;
            scenario.onActivity(a->{second[0]=(DemoTaskScrollActivity)a;assertNotSame(first[0],second[0]);event(nonce,phase,"recreated-handle",metadata(second[0]));});
            assertTrue(first[0].io.awaitTermination(3,TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{destroyed(first[0]);event(nonce,phase,"first-DESTROYED",metadata(first[0]));});
            settle(scenario);
            scenario.onActivity(a->{DemoTaskScrollActivity current=(DemoTaskScrollActivity)a;safe(current,nonce,language,theme);assertEquals(original[0],a.sample.toString());assertEquals(1,current.restoreCalls);assertTrue("Task rendered before hierarchy restoration",current.rendersBeforeRestore>=1);assertTrue(a.expanded.contains(heading()));event(nonce,phase,"after-recreate",metadata(current));int tolerance=Ui.dp(a,2);assertTrue("Native hierarchy must restore scrollY: before="+beforeY[0]+", after="+scroll(a).getScrollY(),Math.abs(beforeY[0]-scroll(a).getScrollY())<=tolerance);Rect paragraph=middleBounds(a);TaskReadingHierarchyTest.assertSafeRect(a,paragraph);assertTrue("Same report paragraph remains at the same visible offset",Math.abs(beforeTop[0]-paragraph.top)<=tolerance);assertNotEquals(View.NO_ID,scroll(a).getId());event(nonce,phase,"full-completed",metadata(current));});
        }catch(Throwable error){failure=error;throw error;}
        finally{
            if(scenario!=null)try{
                scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());
                assertNotNull(first[0]);assertTrue(first[0].io.awaitTermination(3,TimeUnit.SECONDS));
                if(recreateRequested){assertTrue("Unknown recreation stops this probe",recreateReturned);assertNotNull(second[0]);assertTrue(second[0].io.awaitTermination(3,TimeUnit.SECONDS));}
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{destroyed(first[0]);if(second[0]!=null)destroyed(second[0]);event(nonce,phase,"closed",new JSONObjectBuilder().put("DESTROYED",second[0]==null?1:2).put("forbidden_actions",first[0].forbiddenActions.get()+(second[0]==null?0:second[0].forbiddenActions.get())).put("language_unchanged",language.equals(L.chinese()?"zh":"en")).put("theme_unchanged",theme.equals(Ui.dark?"dark":"light")).value);});
                assertEquals(language,L.chinese()?"zh":"en");assertEquals(theme,Ui.dark?"dark":"light");unresolvedLifetime=false;
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
        }
    }
    static String heading(){return L.t("Full report & evidence","完整报告与证据");}
    static ScrollView scroll(DemoTaskPreviewActivity a){return (ScrollView)a.body.getParent().getParent();}
    static TextView full(DemoTaskPreviewActivity a){TextView text=TaskPreviewFeedbackTest.find(a.body,ReportText.render(DemoTaskScrollActivity.REPORT).toString());assertNotNull(text);return text;}
    static int middleLine(TextView text){int offset=text.getText().toString().indexOf(DemoTaskScrollActivity.MIDDLE);assertTrue(offset>=0);return text.getLayout().getLineForOffset(offset);}
    static Rect middleBounds(DemoTaskPreviewActivity a){TextView text=full(a);return TaskReadingHierarchyTest.lineBounds(text,middleLine(text));}
    static void safe(DemoTaskScrollActivity a,String nonce,String language,String theme){
        TaskPreviewFeedbackTest.assertSafe(a);assertFalse(TaskSyncService.running);assertEquals(nonce,a.nonce);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(language,L.chinese()?"zh":"en");assertEquals(theme,Ui.dark?"dark":"light");assertEquals(1,a.seeds);assertEquals(DemoTaskScrollActivity.REPORT,a.sample.optString("report"));assertEquals(1L,a.sample.optLong("created_at"));assertTrue(a.store.prefs.getAll().isEmpty());assertEquals(2,a.global.size());assertEquals(0,a.thumbnailOpens);assertFalse(a.busy);assertFalse(a.loading);assertNull(a.followupDialog);assertNull(a.actionErrorDialog);assertNull(a.localRemovalDialog);
    }
    static void destroyed(DemoTaskScrollActivity a){assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());assertEquals(0,a.forbiddenActions.get());assertFalse(a.foreground);assertFalse(a.handler.hasCallbacks(a.refresh));assertFalse(TaskSyncService.running);}
    static void settle(ActivityScenario<DemoTaskPreviewActivity> scenario)throws Exception{
        TaskPreviewFeedbackTest.frames(scenario);long deadline=SystemClock.elapsedRealtime()+3000,stableSince=0;int previous=Integer.MIN_VALUE;
        do{boolean[] ready={false};int[] current={0};scenario.onActivity(a->{TextView text=full(a);current[0]=scroll(a).getScrollY();ready[0]=a.hasWindowFocus()&&!a.body.isLayoutRequested()&&!scroll(a).isLayoutRequested()&&text.getTag(R.id.expand_animation)==null;});long now=SystemClock.elapsedRealtime();if(ready[0]&&current[0]==previous){if(stableSince==0)stableSince=now;if(now-stableSince>=100)return;}else stableSince=0;previous=current[0];Thread.sleep(20);}while(SystemClock.elapsedRealtime()<deadline);fail("Report layout and scroll did not settle within the fixed budget");
    }
    static JSONObject metadata(DemoTaskScrollActivity a){
        JSONObjectBuilder data=new JSONObjectBuilder().put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("elapsed_ms",SystemClock.elapsedRealtime()).put("forbidden_actions",a.forbiddenActions.get()).put("seeds",a.seeds).put("renders",a.renders).put("restore_calls",a.restoreCalls).put("renders_before_restore",a.rendersBeforeRestore).put("scroll_id",scroll(a).getId()).put("scroll_y",scroll(a).getScrollY()).put("expanded",a.expanded.contains(heading())).put("report_sha256",digest(a.sample.optString("report"))).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("service_running",TaskSyncService.running);
        TextView text=full(a);if(text.getLayout()!=null)data.put("line_count",text.getLayout().getLineCount()).put("middle_line",middleLine(text)).put("middle_top",middleBounds(a).top);
        return data.value;
    }
    static String digest(String text){try{StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)))result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}catch(Exception error){throw new AssertionError(error);}}
    static void event(String nonce,String phase,String event,JSONObject data){Bundle status=new Bundle();status.putString("stream","TASK_SCROLL_EVENT\t"+nonce+"\t"+phase+"\t"+event+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static final class JSONObjectBuilder {final JSONObject value=new JSONObject();JSONObjectBuilder put(String key,Object value){try{this.value.put(key,value);return this;}catch(JSONException error){throw new AssertionError(error);}}}
}
