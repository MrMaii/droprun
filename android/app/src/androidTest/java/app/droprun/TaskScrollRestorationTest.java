package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
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
    @Test public void metadataOnlyRefreshKeepsReportReadingState()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("taskMetadataProbe","");if(phase.isEmpty())return;assertEquals("accepted",phase);
        String nonce=InstrumentationRegistry.getArguments().getString("taskMetadataNonce","");assertEquals(java.util.UUID.fromString(nonce).toString(),nonce);
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=instrumentation.getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));assertFalse(context.getPackageManager().getActivityInfo(new ComponentName(context,DemoTaskScrollActivity.class),0).exported);
        String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] paletteBefore=metadataPalette();int entered=0,completed=0,closed=0,metadataUpdates=0,touchRestored=0;
        try{for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){
            String language=config[0],theme=config[1],scene=language+"|"+theme;assertFalse(unresolvedLifetime);
            ActivityScenario<DemoTaskPreviewActivity> scenario=null;DemoTaskScrollActivity[] retained={null};TextView[] originalText={null},caption={null};CharSequence[] buffer={null};View[] originalHeader={null};int[] selection={0,0},reading={0,0},headerPosition={0,0};boolean[] priorTouch={false};boolean touchChanged=false;Throwable failure=null;
            try{
                unresolvedLifetime=true;event(nonce,"metadata",scene+"|launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(context,DemoTaskScrollActivity.class).putExtra("taskScrollProbe",true).putExtra("nonce",nonce).putExtra("language",language).putExtra("appearance",theme));
                scenario.onActivity(a->{DemoTaskScrollActivity current=(DemoTaskScrollActivity)a;retained[0]=current;safe(current,nonce,language,theme);assertFalse(a.sample.has("updated_at"));priorTouch[0]=a.body.isInTouchMode();event(nonce,"metadata",scene+"|entered",metadataState(current));});entered++;settle(scenario);
                instrumentation.setInTouchMode(false);touchChanged=true;
                scenario.onActivity(a->{assertFalse(a.body.isInTouchMode());event(nonce,"metadata",scene+"|touch-mode-changed",metadataState((DemoTaskScrollActivity)a));TextView title=TaskPreviewFeedbackTest.find(a.body,heading());View header=(View)title.getParent();header.requestRectangleOnScreen(new Rect(0,0,header.getWidth(),header.getHeight()),true);});settle(scenario);
                scenario.onActivity(a->{TextView title=TaskPreviewFeedbackTest.find(a.body,heading());assertTrue(((View)title.getParent()).performClick());});settle(scenario);
                scenario.onActivity(a->{TextView text=full(a);assertTrue(text.getText() instanceof Spannable);int start=text.getText().toString().indexOf(DemoTaskScrollActivity.MIDDLE);assertTrue(start>=0);selection[0]=start;selection[1]=start+DemoTaskScrollActivity.MIDDLE.length();Selection.setSelection((Spannable)text.getText(),selection[0],selection[1]);Layout layout=text.getLayout();int line=middleLine(text);text.requestRectangleOnScreen(new Rect(0,text.getTotalPaddingTop()+layout.getLineTop(line),text.getWidth(),text.getTotalPaddingTop()+layout.getLineBottom(line)),true);});settle(scenario);
                scenario.onActivity(a->{safe((DemoTaskScrollActivity)a,nonce,language,theme);originalText[0]=full(a);buffer[0]=originalText[0].getText();originalHeader[0]=(View)TaskPreviewFeedbackTest.find(a.body,heading()).getParent();caption[0]=a.sharedAt;assertNotNull(caption[0]);assertEquals(selection[0],Selection.getSelectionStart(buffer[0]));assertEquals(selection[1],Selection.getSelectionEnd(buffer[0]));Rect paragraph=middleBounds(a);TaskReadingHierarchyTest.assertSafeRect(a,paragraph);reading[0]=scroll(a).getScrollY();reading[1]=paragraph.top;assertTrue(reading[0]>scroll(a).getHeight());event(nonce,"metadata",scene+"|reading-before",metadataState((DemoTaskScrollActivity)a));try{a.sample.put("updated_at",2L);}catch(JSONException error){throw new AssertionError(error);}a.render();});settle(scenario);
                scenario.onActivity(a->{assertMetadataReading(a,originalText[0],buffer[0],originalHeader[0],selection);int tolerance=Ui.dp(a,2);assertTrue(Math.abs(reading[0]-scroll(a).getScrollY())<=tolerance);assertTrue(Math.abs(reading[1]-middleBounds(a).top)<=tolerance);assertSame(caption[0],a.sharedAt);safe((DemoTaskScrollActivity)a,nonce,language,theme);event(nonce,"metadata",scene+"|reading-after",metadataState((DemoTaskScrollActivity)a));});metadataUpdates++;
                scenario.onActivity(a->{View header=originalHeader[0];header.requestRectangleOnScreen(new Rect(0,0,header.getWidth(),header.getHeight()),true);});settle(scenario);
                scenario.onActivity(a->{TaskReadingHierarchyTest.assertSafeRect(a,TaskPreviewFeedbackTest.screenBounds(originalHeader[0]));assertTrue(originalHeader[0].requestFocus());assertSame(originalHeader[0],a.getWindow().getDecorView().findFocus());Selection.setSelection((Spannable)buffer[0],selection[0],selection[1]);});settle(scenario);
                scenario.onActivity(a->{assertMetadataReading(a,originalText[0],buffer[0],originalHeader[0],selection);assertSame(originalHeader[0],a.getWindow().getDecorView().findFocus());headerPosition[0]=scroll(a).getScrollY();headerPosition[1]=TaskPreviewFeedbackTest.screenBounds(originalHeader[0]).top;event(nonce,"metadata",scene+"|focus-before",metadataState((DemoTaskScrollActivity)a));try{a.sample.put("updated_at",3L);}catch(JSONException error){throw new AssertionError(error);}a.render();});settle(scenario);
                scenario.onActivity(a->{assertMetadataReading(a,originalText[0],buffer[0],originalHeader[0],selection);assertSame(originalHeader[0],a.getWindow().getDecorView().findFocus());int tolerance=Ui.dp(a,2);assertTrue(Math.abs(headerPosition[0]-scroll(a).getScrollY())<=tolerance);assertTrue(Math.abs(headerPosition[1]-TaskPreviewFeedbackTest.screenBounds(originalHeader[0]).top)<=tolerance);safe((DemoTaskScrollActivity)a,nonce,language,theme);event(nonce,"metadata",scene+"|focus-after",metadataState((DemoTaskScrollActivity)a));});metadataUpdates++;
                scenario.onActivity(a->{caption[0].setText("Stale");long before=System.currentTimeMillis();a.render();long after=System.currentTimeMillis();String actual=a.sharedAt.getText().toString(),prefix=L.t("Shared ","交办于 ");assertTrue(actual.equals(prefix+TaskPresentation.elapsed(a.sample.optLong("created_at"),before))||actual.equals(prefix+TaskPresentation.elapsed(a.sample.optLong("created_at"),after)));assertSame(caption[0],a.sharedAt);assertMetadataReading(a,originalText[0],buffer[0],originalHeader[0],selection);assertSame(originalHeader[0],a.getWindow().getDecorView().findFocus());event(nonce,"metadata",scene+"|clock-label-refreshed",metadataState((DemoTaskScrollActivity)a));});settle(scenario);
                String changed=DemoTaskScrollActivity.REPORT+"\n\nSynthetic report update.";
                scenario.onActivity(a->{try{a.sample.put("report",changed);}catch(JSONException error){throw new AssertionError(error);}a.render();});TaskPreviewFeedbackTest.frames(scenario);
                scenario.onActivity(a->{TextView current=TaskPreviewFeedbackTest.find(a.body,ReportText.render(changed).toString());assertNotNull(current);assertNotSame(originalText[0],current);assertEquals(ReportText.render(changed).toString(),current.getText().toString());assertTrue(a.expanded.contains(heading()));assertEquals(0,a.forbiddenActions.get());event(nonce,"metadata",scene+"|report-changed",metadataState((DemoTaskScrollActivity)a));try{a.sample.put("report",DemoTaskScrollActivity.REPORT);a.sample.remove("updated_at");}catch(JSONException error){throw new AssertionError(error);}a.render();});settle(scenario);
                scenario.onActivity(a->{safe((DemoTaskScrollActivity)a,nonce,language,theme);assertFalse(a.sample.has("updated_at"));assertEquals(0,((DemoTaskScrollActivity)a).restoreCalls);event(nonce,"metadata",scene+"|full-completed",metadataState((DemoTaskScrollActivity)a));});completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{if(scenario!=null)try{
                scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);assertTrue(retained[0].io.awaitTermination(3,TimeUnit.SECONDS));
                instrumentation.runOnMainSync(()->{DemoTaskScrollActivity a=retained[0];destroyed(a);event(nonce,"metadata",scene+"|DESTROYED",new JSONObjectBuilder().put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("forbidden_actions",a.forbiddenActions.get()).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("restore_calls",a.restoreCalls).put("foreground",a.foreground).put("refresh_queued",a.handler.hasCallbacks(a.refresh)).value);});closed++;
                if(touchChanged){instrumentation.setInTouchMode(priorTouch[0]);event(nonce,"metadata",scene+"|touch-mode-restored",new JSONObjectBuilder().put("prior_touch_mode",priorTouch[0]).put("setInTouchMode_returned",true).value);touchRestored++;}
                unresolvedLifetime=false;
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(!unresolvedLifetime)restoreMetadataPalette(languageBefore,darkBefore,paletteBefore);}
        assertEquals(2,entered);assertEquals(2,completed);assertEquals(2,closed);assertEquals(4,metadataUpdates);assertEquals(2,touchRestored);assertFalse(unresolvedLifetime);
        event(nonce,"metadata","summary",new JSONObjectBuilder().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("metadata_updates",metadataUpdates).put("touch_mode_restores",touchRestored).put("clock_scope","Synthetic stale label restored by actual render; no wall-clock minute wait").value);
    }
    static void assertMetadataReading(DemoTaskPreviewActivity a,TextView text,CharSequence buffer,View header,int[] selection){
        assertSame(text,full(a));assertSame(buffer,text.getText());assertSame(header,TaskPreviewFeedbackTest.find(a.body,heading()).getParent());assertEquals(selection[0],Selection.getSelectionStart(buffer));assertEquals(selection[1],Selection.getSelectionEnd(buffer));assertTrue(a.expanded.contains(heading()));assertEquals(ReportText.render(DemoTaskScrollActivity.REPORT).toString(),text.getText().toString());assertEquals(0,a.forbiddenActions.get());
    }
    static JSONObject metadataState(DemoTaskScrollActivity a){
        TextView text=TaskPreviewFeedbackTest.find(a.body,ReportText.render(a.sample.optString("report")).toString());assertNotNull(text);View header=(View)TaskPreviewFeedbackTest.find(a.body,heading()).getParent();
        return new JSONObjectBuilder().put("fixture",a.getClass().getSimpleName()).put("language",L.chinese()?"zh":"en").put("theme",Ui.dark?"dark":"light").put("memory_only",true).put("forbidden_actions",a.forbiddenActions.get()).put("render_calls",a.renders).put("restore_calls",a.restoreCalls).put("scroll_y",scroll(a).getScrollY()).put("header_top",TaskPreviewFeedbackTest.screenBounds(header).top).put("header_focused",a.getWindow().getDecorView().findFocus()==header).put("report_view_identity",System.identityHashCode(text)).put("report_buffer_identity",System.identityHashCode(text.getText())).put("header_identity",System.identityHashCode(header)).put("selection_start",Selection.getSelectionStart(text.getText())).put("selection_end",Selection.getSelectionEnd(text.getText())).put("report_sha256",digest(a.sample.optString("report"))).put("updated_at",a.sample.has("updated_at")?a.sample.optLong("updated_at"):JSONObject.NULL).put("shared_text",a.sharedAt==null?JSONObject.NULL:a.sharedAt.getText().toString()).put("touch_mode",a.body.isInTouchMode()).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).value;
    }
    static int[] metadataPalette(){return new int[]{Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM};}
    static void restoreMetadataPalette(String language,boolean dark,int[] p){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{L.language(language);Ui.dark=dark;Ui.BG=p[0];Ui.SURFACE=p[1];Ui.SURFACE_2=p[2];Ui.SURFACE_3=p[3];Ui.LINE=p[4];Ui.LINE_STRONG=p[5];Ui.TEXT=p[6];Ui.MUTED=p[7];Ui.DIM=p[8];Ui.LIME_SOFT=p[9];Ui.LIME_LINE=p[10];Ui.ACCENT=p[11];Ui.DANGER=p[12];Ui.AMBER=p[13];Ui.SCRIM=p[14];assertEquals(language,L.chinese()?"zh":"en");assertEquals(dark,Ui.dark);assertArrayEquals(p,metadataPalette());});}
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
