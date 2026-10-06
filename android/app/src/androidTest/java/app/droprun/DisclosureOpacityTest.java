package app.droprun;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/** Two opt-in native memory windows; local disclosure clicks only. No screenshots or business action. */
public class DisclosureOpacityTest {
    static boolean unresolvedLifetime;
    @Test public void reversingDisclosureContinuesCurrentOpacity()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String phase=args.getString("disclosureOpacityProbe","");if(phase.isEmpty())return;
        assertTrue(phase.equals("baseline")||phase.equals("accepted"));String nonce=args.getString("disclosureOpacityNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);assertFalse(unresolvedLifetime);assertTrue(Build.VERSION.SDK_INT>=30);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(TaskSyncService.running);assertTrue("Use the existing motion-enabled baseline without changing it",Ui.motionEnabled(target));assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoHistoryIdentityActivity.class),0).exported);
        File base=target.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/disclosure-opacity-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());
        float previous=DemoHistoryIdentityActivity.fontScale;String languageBefore=L.chinese()?"zh":"en";boolean[] destroyed={true};int[] totals={0,0,0};boolean complete=false;JSONArray events=new JSONArray(),windows=new JSONArray();
        try{
            for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){
                String identity=config[0]+"|"+config[1]+"|font1";assertTrue(destroyed[0]);assertFalse(unresolvedLifetime);DemoHistoryIdentityActivity.fontScale=1f;
                ActivityScenario<DemoHistoryIdentityActivity> scenario=null;Throwable failure=null;DemoHistoryIdentityActivity[] retained={null};Probe probe=new Probe();JSONObject report=new JSONObject().put("identity",identity).put("samples",probe.samples).put("clicks",probe.clicks);windows.put(report);
                try{
                    Intent intent=new Intent(target,DemoHistoryIdentityActivity.class).putExtra("historyIdentity",true).putExtra("language",config[0]).putExtra("appearance",config[1]).putExtra("projectId",DemoHistoryIdentityActivity.ID).putExtra("projectName",DemoHistoryIdentityActivity.sampleName(config[0].equals("zh"),false));
                    destroyed[0]=false;unresolvedLifetime=true;event(events,identity,"launch-attempt",null);scenario=ActivityScenario.launch(intent);event(events,identity,"returned-handle",null);HistoryIdentityTest.ready(scenario);
                    scenario.onActivity(a->{retained[0]=a;safe(a);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(config[1].equals("dark"),Ui.dark);assertTrue(Ui.motionEnabled(a));probe.body=Ui.vertical(a);probe.fullHeight=a.dp(112);probe.body.setMinimumHeight(probe.fullHeight);probe.body.addView(Ui.caption(a,L.t("Synthetic opacity probe; memory only.","透明度界面探针；仅内存。")));LinearLayout group=Ui.disclosure(a,L.t("Local disclosure probe","局部展开探针"),probe.body,false,probe.callbacks::add);probe.header=((ViewGroup)group).getChildAt(0);probe.arrow=((ViewGroup)probe.header).getChildAt(1);LinearLayout page=(LinearLayout)a.list.getParent().getParent();((LinearLayout)page.getChildAt(0)).addView(group,Ui.fill());});HistoryIdentityTest.ready(scenario);totals[0]++;event(events,identity,"entered",retained[0]);
                    click(retained[0],probe,true,false,phase,"open");
                    float closingStart=awaitInteriorAndReverse(retained[0],probe,true,Float.NaN,false,phase,"opening-samples","reverse-to-closed");
                    float openingStart=awaitInteriorAndReverse(retained[0],probe,false,closingStart,true,phase,"reversed-closing-samples","reverse-to-open");awaitEnd(retained[0],probe,true,openingStart,phase,"reversed-opening-samples");
                    float finalStart=click(retained[0],probe,false,false,phase,"close");awaitEnd(retained[0],probe,false,finalStart,phase,"final-closing-samples");
                    scenario.onActivity(a->{safe(a);assertEquals(Arrays.asList(true,false,true,false),probe.callbacks);assertEquals(View.GONE,probe.body.getVisibility());assertEquals(1f,probe.body.getAlpha(),0f);assertEquals(-2,probe.body.getLayoutParams().height);assertNull(probe.body.getTag(R.id.expand_animation));assertEquals(180f,probe.arrow.getRotation(),.01f);assertEquals(Boolean.FALSE,probe.header.getTag());});report.put("callback_count",probe.callbacks.size()).put("completed",true);totals[1]++;event(events,identity,"full-completed",retained[0]);
                }catch(Throwable error){failure=error;report.put("failure_class",error.getClass().getName());event(events,identity,"failed",retained[0]);throw error;}
                finally{closeKnown(scenario,failure,destroyed,events,identity);if(scenario!=null&&destroyed[0]){totals[2]++;event(events,identity,"after-destroyed",retained[0]);if(failure==null&&retained[0]!=null)assertEquals(0,retained[0].forbiddenActions);}}
            }
            assertArrayEquals(new int[]{2,2,2},totals);assertFalse(unresolvedLifetime);complete=true;
        }finally{if(destroyed[0]){DemoHistoryIdentityActivity.fontScale=previous;L.language(languageBefore);event(events,"all","font-and-language-restored",null);}write(directory,"run-events.json",new JSONObject().put("nonce",nonce).put("phase",phase).put("complete",complete).put("entered",totals[0]).put("completed",totals[1]).put("destroyed",totals[2]).put("unresolved_lifetime",unresolvedLifetime).put("sampling","native property observations; not every rendered frame or physical performance").put("events",events).put("windows",windows));}
    }
    static final class Probe {LinearLayout body;View header,arrow;int fullHeight;final List<Boolean> callbacks=new ArrayList<>();final JSONArray samples=new JSONArray(),clicks=new JSONArray();}
    static final class Sample {
        final long at=SystemClock.elapsedRealtime();final float alpha;final int height,layoutHeight,visibility;final boolean running,layoutRequested;final float fraction;
        Sample(Probe p){alpha=p.body.getAlpha();height=p.body.getHeight();layoutHeight=p.body.getLayoutParams().height;visibility=p.body.getVisibility();layoutRequested=p.body.isLayoutRequested();Object current=p.body.getTag(R.id.expand_animation);running=current instanceof ValueAnimator&&((ValueAnimator)current).isRunning();fraction=current instanceof ValueAnimator?((ValueAnimator)current).getAnimatedFraction():-1f;}
        JSONObject json(String name){try{return new JSONObject().put("stage",name).put("elapsed_ms",at).put("alpha",alpha).put("rendered_height",height).put("layout_height",layoutHeight).put("visibility",visibility).put("animator_running",running).put("layout_requested",layoutRequested).put("animated_fraction",fraction);}catch(JSONException e){throw new AssertionError(e);}}
    }
    // Timed reads/clicks use the known retained Activity; onActivity waits for idle and misses 280ms motion.
    static float click(DemoHistoryIdentityActivity activity,Probe p,boolean opening,boolean reversal,String phase,String name){
        float[] start={Float.NaN};InstrumentationRegistry.getInstrumentation().runOnMainSync(()->start[0]=clickOnMain(activity,p,opening,reversal,phase,name));return start[0];
    }
    static float clickOnMain(DemoHistoryIdentityActivity a,Probe p,boolean opening,boolean reversal,String phase,String name){
        assertSame(android.os.Looper.getMainLooper(),android.os.Looper.myLooper());assertNotNull(a);assertFalse(a.isDestroyed());assertFalse(a.isFinishing());safe(a);Sample before=new Sample(p);int count=p.callbacks.size();
        if(reversal){assertTrue("Reverse an actual running animation",before.running);assertTrue(before.alpha>0f&&before.alpha<1f);assertTrue(before.height>0&&before.height<p.fullHeight);assertTrue(before.fraction>0f&&before.fraction<1f);}
        assertTrue(p.header.performClick());Sample after=new Sample(p);assertEquals("One callback per local header click",count+1,p.callbacks.size());assertEquals(opening,p.callbacks.get(count));assertEquals(opening,p.header.getTag());
        try{p.clicks.put(new JSONObject().put("name",name).put("reversal",reversal).put("opening",opening).put("before",before.json("before-click")).put("after",after.json("after-click")).put("alpha_delta",after.alpha-before.alpha).put("rendered_height_delta",after.height-before.height).put("callback_before",count).put("callback_after",p.callbacks.size()));}catch(JSONException e){throw new AssertionError(e);}
        if(reversal&&phase.equals("accepted")){assertEquals("Reversal starts at current opacity in this same UI callback",before.alpha,after.alpha,.015f);assertEquals("No immediate rendered-height jump",before.height,after.height);}safe(a);return before.alpha;
    }
    static float awaitInteriorAndReverse(DemoHistoryIdentityActivity activity,Probe p,boolean opening,float start,boolean nextOpening,String phase,String name,String clickName)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] observed={false};float[] reversedFrom={Float.NaN};
        do{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoHistoryIdentityActivity a=activity;assertNotNull(a);assertFalse(a.isDestroyed());assertFalse(a.isFinishing());safe(a);Sample sample=new Sample(p);p.samples.put(sample.json(name));continuity(sample,opening,start,phase);
            observed[0]=sample.running&&sample.alpha>0f&&sample.alpha<1f&&sample.height>0&&sample.height<p.fullHeight&&sample.fraction>0f&&sample.fraction<1f;
            if(observed[0])reversedFrom[0]=clickOnMain(a,p,nextOpening,true,phase,clickName);
        });if(observed[0])return reversedFrom[0];Thread.sleep(12);}while(SystemClock.elapsedRealtime()<deadline);fail("No actual interior alpha/rendered-height/fraction observed for "+name);return Float.NaN;
    }
    static void awaitEnd(DemoHistoryIdentityActivity activity,Probe p,boolean opening,float start,String phase,String name)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] ended={false};do{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoHistoryIdentityActivity a=activity;assertNotNull(a);assertFalse(a.isDestroyed());assertFalse(a.isFinishing());safe(a);Sample sample=new Sample(p);p.samples.put(sample.json(name));continuity(sample,opening,start,phase);ended[0]=!sample.running&&p.body.getTag(R.id.expand_animation)==null&&sample.visibility==(opening?View.VISIBLE:View.GONE)&&sample.alpha==1f&&sample.layoutHeight==-2&&(!opening||!sample.layoutRequested);if(ended[0]){assertEquals(opening,p.header.getTag());assertEquals(opening?270f:180f,p.arrow.getRotation(),.01f);if(opening)assertEquals(p.fullHeight,sample.height);}});if(ended[0])return;Thread.sleep(12);}while(SystemClock.elapsedRealtime()<deadline);fail("Native disclosure did not reach final state for "+name);
    }
    static void continuity(Sample sample,boolean opening,float start,String phase){if(!phase.equals("accepted")||Float.isNaN(start)||sample.visibility!=View.VISIBLE)return;if(opening)assertTrue("Opening cannot jump below its observed starting alpha",sample.alpha>=start-.015f);else assertTrue("Closing cannot jump above its observed starting alpha",sample.alpha<=start+.015f);}
    static void safe(DemoHistoryIdentityActivity a){HistoryIdentityTest.assertSafe(a);assertTrue(a.getIntent().getBooleanExtra("historyIdentity",false));assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertEquals(3,a.adapter.getCount());}
    static void closeKnown(ActivityScenario<DemoHistoryIdentityActivity> scenario,Throwable failure,boolean[] destroyed,JSONArray events,String identity)throws Throwable{
        if(scenario==null)return;
        try{event(events,identity,"close-attempt",null);scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());destroyed[0]=true;unresolvedLifetime=false;event(events,identity,"DESTROYED",null);}catch(Throwable closeError){event(events,identity,"close-unresolved",null);if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void event(JSONArray events,String identity,String name,DemoHistoryIdentityActivity retained){try{JSONObject event=new JSONObject().put("identity",identity).put("event",name).put("elapsed_ms",SystemClock.elapsedRealtime());if(retained!=null)event.put("forbidden_actions",retained.forbiddenActions).put("memory_refreshes",retained.memoryRefreshes);events.put(event);Bundle output=new Bundle();output.putString("stream","DISCLOSURE_OPACITY_EVENT\t"+event+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,output);}catch(JSONException error){throw new AssertionError(error);}}
    static void write(File directory,String name,JSONObject data)throws Exception{try(OutputStream out=Files.newOutputStream(new File(directory,name).toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));}}
}
