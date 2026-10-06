package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static app.droprun.HistoryIdentityTest.*;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static app.droprun.HomeRecoveryFeedbackTest.assertFullVisibility;
import static org.junit.Assert.*;

/** Six opt-in memory windows; four fresh raw captures. No control click or business operation. */
public class HistoryRowsPresentationTest {
    static boolean unresolvedLifetime;
    @Test public void historyRowsPreserveReadingAndBindings()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String phase=args.getString("historyRowsPresentationProbe","");if(phase.isEmpty())return;
        assertTrue(phase.equals("baseline")||phase.equals("accepted"));String nonce=args.getString("historyRowsPresentationNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);assertFalse("Stop after unresolved lifetime",unresolvedLifetime);assertTrue(Build.VERSION.SDK_INT>=30);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoHistoryIdentityActivity.class),0).exported);
        File base=target.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/history-rows-presentation-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());
        JSONArray events=new JSONArray();int[] totals={0,0,0,0,0};boolean[] destroyed={true};boolean complete=false;float previous=DemoHistoryIdentityActivity.fontScale;String languageBefore=L.chinese()?"zh":"en";
        try{
            for(String[] config:new String[][]{{"en","light","1"},{"en","dark","1"},{"zh","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
                String language=config[0],theme=config[1],identity=language+"|"+theme+"|font"+config[2];float scale=Float.parseFloat(config[2]);assertTrue(destroyed[0]);assertFalse(unresolvedLifetime);DemoHistoryIdentityActivity.fontScale=scale;
                ActivityScenario<DemoHistoryIdentityActivity> scenario=null;Throwable failure=null;DemoHistoryIdentityActivity[] retained={null};
                try{
                    Intent intent=new Intent(target,DemoHistoryIdentityActivity.class).putExtra("historyIdentity",true).putExtra("language",language).putExtra("appearance",theme).putExtra("projectId",DemoHistoryIdentityActivity.ID).putExtra("projectName",DemoHistoryIdentityActivity.sampleName(language.equals("zh"),false));
                    destroyed[0]=false;unresolvedLifetime=true;event(events,identity,"launch-attempt",null);scenario=ActivityScenario.launch(intent);event(events,identity,"returned-handle",null);ready(scenario);
                    scenario.onActivity(a->{retained[0]=a;safe(a);assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);});totals[0]++;event(events,identity,"entered",retained[0]);
                    for(int position=0;position<3;position++){
                        final int index=position;scenario.onActivity(a->a.list.setSelectionFromTop(index,0));frame(scenario);View[] outer={null};ProjectHistoryActivity.Holder[] held={null};
                        scenario.onActivity(a->{outer[0]=row(a,a.rows.get(index).optString("id"));held[0]=(ProjectHistoryActivity.Holder)outer[0].getTag();checkRow(a,held[0],phase);});
                        for(String part:new String[]{"title","meta","status"}){scenario.onActivity(a->{TextView text=text(held[0],part);text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});frame(scenario);scenario.onActivity(a->{assertSame(held[0],row(a,held[0].id).getTag());readable(a,text(held[0],part));safe(a);});}
                        scenario.onActivity(a->{try{JSONObject task=a.rows.get(index);String id=held[0].id,title=held[0].title.getText().toString(),original=task.optString("status");long stable=a.adapter.getItemId(index);View focus=a.getWindow().getDecorView().findFocus();
                            for(String state:new String[]{"failed",original}){task.put("status",state);assertSame(outer[0],a.adapter.getView(index,outer[0],a.list));assertSame(held[0],outer[0].getTag());assertEquals(id,held[0].id);assertEquals(stable,a.adapter.getItemId(index));assertEquals(title,held[0].title.getText().toString());assertEquals(TaskPresentation.status(state),held[0].status.getText().toString());assertEquals(TaskPresentation.statusColor(state),held[0].status.getCurrentTextColor());assertEquals(tintFor(TaskPresentation.statusColor(state)),tint(held[0].status));assertSame(focus,a.getWindow().getDecorView().findFocus());totals[3]++;}safe(a);
                        }catch(JSONException error){throw new AssertionError(error);}});frame(scenario);
                    }
                    scenario.onActivity(a->a.list.setSelectionFromTop(1,-a.dp(8)));frame(scenario);String[] anchor={null};int[] offset={0};View[] focus={null};
                    scenario.onActivity(a->{focus[0]=findDescription(a.getWindow().getDecorView(),L.t("Refresh project history","刷新项目历史"));assertNotNull(focus[0]);assertTrue(focus[0].requestFocusFromTouch());});frame(scenario);
                    scenario.onActivity(a->{anchor[0]=first(a).id;offset[0]=a.list.getChildAt(0).getTop();try{a.probe.entries.getJSONObject(0).put("updated_at",100);}catch(JSONException error){throw new AssertionError(error);}a.snapshot="";a.render();});frame(scenario);
                    scenario.onActivity(a->{assertEquals(anchor[0],first(a).id);assertEquals(offset[0],a.list.getChildAt(0).getTop());assertTrue(focus[0].hasFocus());safe(a);});
                    if(scale==1f){scenario.onActivity(a->a.list.setSelectionFromTop(0,0));frame(scenario);capture(scenario,directory,nonce,phase,language,theme);totals[4]++;}
                    scenario.onActivity(HistoryRowsPresentationTest::safe);totals[1]++;event(events,identity,"full-completed",retained[0]);
                }catch(Throwable error){failure=error;event(events,identity,"failed:"+error.getClass().getSimpleName(),retained[0]);throw error;}
                finally{closeKnown(scenario,failure,destroyed,events,identity);if(scenario!=null&&destroyed[0]){totals[2]++;event(events,identity,"after-destroyed",retained[0]);if(failure==null&&retained[0]!=null)assertEquals(0,retained[0].forbiddenActions);}}
            }
            assertArrayEquals(new int[]{6,6,6,36,4},totals);assertFalse(unresolvedLifetime);complete=true;
        }finally{if(destroyed[0]){DemoHistoryIdentityActivity.fontScale=previous;L.language(languageBefore);event(events,"all","font-and-language-restored",null);}write(directory,"run-events.json",new JSONObject().put("nonce",nonce).put("phase",phase).put("complete",complete).put("entered",totals[0]).put("completed",totals[1]).put("destroyed",totals[2]).put("rebinds",totals[3]).put("captures",totals[4]).put("unresolved_lifetime",unresolvedLifetime).put("events",events));}
    }
    static void safe(DemoHistoryIdentityActivity a){assertSafe(a);assertTrue(a.getIntent().getBooleanExtra("historyIdentity",false));assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertEquals(3,a.adapter.getCount());assertTrue(a.adapter.hasStableIds());assertEquals(1,a.list.getFooterViewsCount());}
    static View row(DemoHistoryIdentityActivity a,String id){for(int n=0;n<a.list.getChildCount();n++){View view=a.list.getChildAt(n);if(view.getTag() instanceof ProjectHistoryActivity.Holder&&id.equals(((ProjectHistoryActivity.Holder)view.getTag()).id))return view;}fail("Requested memory row is not laid out: "+id);return null;}
    static TextView text(ProjectHistoryActivity.Holder h,String part){return part.equals("title")?h.title:part.equals("meta")?h.meta:h.status;}
    static void checkRow(DemoHistoryIdentityActivity a,ProjectHistoryActivity.Holder h,String phase){
        safe(a);assertTrue(h.card.getHeight()>=a.dp(48));assertTrue(h.card.getWidth()>=a.dp(48));assertTrue(h.card.isEnabled());assertTrue(h.card.isClickable());assertTrue(h.card.isFocusable());assertTrue(h.card.getForeground() instanceof RippleDrawable);assertEquals(View.GONE,h.remove.getVisibility());for(String part:new String[]{"title","meta","status"})assertCompleteText(text(h,part));
        if(!phase.equals("accepted"))return;assertNull(h.card.getBackground());assertEquals(0f,h.card.getElevation(),0f);assertTrue(h.card.getMinimumHeight()>=a.dp(48));assertEquals(a.dp(280),h.title.getWidth());assertEquals(bounds(a.list).left+a.list.getPaddingLeft(),bounds(h.title).left);assertEquals(bounds(h.title).left,bounds(h.meta).left);assertEquals(bounds(h.title).left,bounds(h.status).left);
        ViewGroup outer=(ViewGroup)h.card.getParent();assertEquals(2,outer.getChildCount());View divider=outer.getChildAt(1);assertSame(h.card,outer.getChildAt(0));assertFalse(divider.isClickable());assertFalse(divider.isFocusable());assertEquals(a.dp(1),divider.getHeight());assertEquals(bounds(h.card).bottom,bounds(divider).top);assertEquals(0,outer.getPaddingBottom());
    }
    static void readable(DemoHistoryIdentityActivity a,TextView text){assertCompleteText(text);assertFullVisibility(text);Rect safe=new Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);assertTrue(safe.intersect(bounds(a.list)));assertTrue("Full target fits list/window",safe.contains(bounds(text)));}
    static void frame(ActivityScenario<DemoHistoryIdentityActivity> scenario)throws Exception{CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{View root=a.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(a.hasWindowFocus()&&root.isAttachedToWindow()&&!root.isLayoutRequested()&&!a.list.isLayoutRequested()&&a.list.getChildCount()>0){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else root.postInvalidateOnAnimation();return true;}});root.postInvalidateOnAnimation();});assertTrue("Observed native pre-draw",frame.await(3,TimeUnit.SECONDS));ready(scenario);}
    static void capture(ActivityScenario<DemoHistoryIdentityActivity> scenario,File directory,String nonce,String phase,String language,String theme)throws Exception{
        frame(scenario);frame(scenario);long settledAt=SystemClock.elapsedRealtime();Thread.sleep(2000);ready(scenario);JSONObject[] data={null};
        scenario.onActivity(a->{safe(a);View root=a.getWindow().getDecorView();assertEquals(320,root.getWidth());assertEquals(640,root.getHeight());assertTrue(a.hasWindowFocus());assertFalse(root.isLayoutRequested());assertFalse(a.list.isLayoutRequested());assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertNotNull(root.getRootWindowInsets());assertFalse(root.getRootWindowInsets().isVisible(WindowInsets.Type.ime()));assertTrue(SystemClock.elapsedRealtime()-settledAt>=2000);
            TextView marker=a.findViewById(android.R.id.content).findViewWithTag("history-memory-marker");assertNotNull(marker);assertCompleteText(marker);assertFullVisibility(marker);ProjectHistoryActivity.Holder h=first(a);checkRow(a,h,phase);for(String part:new String[]{"title","meta","status"})readable(a,text(h,part));assertEquals("history-ui-task-0",h.id);
            try{data[0]=new JSONObject().put("nonce",nonce).put("phase",phase).put("language",language).put("theme",theme).put("font_scale",a.getResources().getConfiguration().fontScale).put("memory_only",true).put("capture_accepted",false).put("fixture",a.getClass().getSimpleName()).put("marker",marker.getText()).put("marker_bounds",bounds(marker).toShortString()).put("observed_predraws",2).put("settled_at_elapsed_ms",settledAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("window_focus",a.hasWindowFocus()).put("forbidden_actions",a.forbiddenActions).put("memory_refreshes",a.memoryRefreshes).put("business_actions","not_invoked_by_test").put("anchor",h.id).put("anchor_top",a.list.getChildAt(0).getTop()).put("title_width",h.title.getWidth()).put("first_row_bounds",bounds(h.card).toShortString()).put("system_bar_pixels_require_visual_review",true);}catch(JSONException error){throw new AssertionError(error);}});
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);String stem="history-rows-"+language+"-"+theme+"-font1";try{assertEquals(320,bitmap.getWidth());assertEquals(640,bitmap.getHeight());try(OutputStream out=Files.newOutputStream(new File(directory,stem+".png").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}write(directory,stem+".json",data[0]);}finally{bitmap.recycle();}
    }
    static void closeKnown(ActivityScenario<DemoHistoryIdentityActivity> scenario,Throwable failure,boolean[] destroyed,JSONArray events,String identity)throws Throwable{
        if(scenario==null)return;
        try{event(events,identity,"close-attempt",null);scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());destroyed[0]=true;unresolvedLifetime=false;event(events,identity,"DESTROYED",null);}catch(Throwable closeError){event(events,identity,"close-unresolved:"+closeError.getClass().getSimpleName(),null);if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void event(JSONArray events,String identity,String name,DemoHistoryIdentityActivity retained){try{JSONObject event=new JSONObject().put("identity",identity).put("event",name).put("elapsed_ms",SystemClock.elapsedRealtime());if(retained!=null)event.put("forbidden_actions",retained.forbiddenActions).put("memory_refreshes",retained.memoryRefreshes);events.put(event);Bundle output=new Bundle();output.putString("stream","HISTORY_ROWS_EVENT\t"+event+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,output);}catch(JSONException error){throw new AssertionError(error);}}
    static void write(File directory,String name,JSONObject data)throws Exception{try(OutputStream out=Files.newOutputStream(new File(directory,name).toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));}}
}
