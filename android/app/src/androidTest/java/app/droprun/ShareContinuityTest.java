package app.droprun;

import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Two fixed memory windows; no Close/Back/Send, persistence or device settings. */
public class ShareContinuityTest {
    static boolean unresolvedLifetime;
    @Test public void authorizedPickAndScrimReverseStayContinuous()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("shareContinuityProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=InstrumentationRegistry.getArguments().getString("shareContinuityNonce","");assertTrue(nonce.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=HistoryShareStateTest.context(DemoShareEditorActivity.class);
        float previous=DemoShareEditorActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] palette=HistoryShareStateTest.palette(),counts=new int[4];
        try{for(String config:new String[]{"en|light|1","zh|dark|1"}){
            String[] part=config.split("\\|");ActivityScenario<DemoShareEditorActivity> scenario=null;DemoShareEditorActivity[] held={null};Throwable failure=null;
            CountDownLatch sampled=new CountDownLatch(1);Throwable[] sampleFailure={null};Runnable[] tick={null};ObjectAnimator[] destructionAnimator={null};int[] destructionCancels={0},destructionEnds={0};boolean[] stopped={false},touch={false};
            try{
                assertFalse(unresolvedLifetime);DemoShareEditorActivity.hierarchyFontScale=Float.parseFloat(part[2]);assertTrue(DemoShareEditorActivity.hierarchyFontScale>0f);unresolvedLifetime=true;
                event(nonce,config,"launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",part[0]).putExtra("appearance",part[1]).putExtra("modelDisclosure",true));
                scenario.onActivity(a->{held[0]=a;touch[0]=a.root.isInTouchMode();event(nonce,config,"returned-handle",guards(a,part,counts));});
                HistoryShareStateTest.shareReady(scenario);
                scenario.onActivity(a->{
                    guards(a,part,counts);assertTrue(a.getIntent().getBooleanExtra("modelDisclosure",false));
                    try{JSONArray catalog=a.store.projects();catalog.put(new JSONObject().put("id","ui-probe-second").put("name","Second memory project").put("available",true).put("permission",new JSONObject().put("enabled",true)));}catch(Exception error){throw new AssertionError(error);}
                    a.draft="Keep this local note.";a.step=0;a.stage.removeAllViews();a.stage.addView(a.stepProject(),new ViewGroup.LayoutParams(-1,-2));counts[0]++;
                });
                HistoryShareStateTest.shareReady(scenario);
                scenario.onActivity(a->{
                    JSONObject first=a.store.projects().optJSONObject(0),second=a.store.projects().optJSONObject(1);assertNotNull(second);
                    View oldPane=a.stage.getChildAt(0),firstRow=a.projectList.getChildAt(0);int rows=a.projectList.getChildCount();String unchanged=materialAndOptions(a);int noops=a.checkpointNoops;
                    a.busy=true;a.pick(second);assertEquals(0,a.step);assertEquals("ui-probe-project",a.selected);assertEquals(noops,a.checkpointNoops);a.busy=false;
                    a.pick(second);assertEquals(1,a.step);assertEquals("ui-probe-second",a.selected);assertEquals("Second memory project",a.projectName());assertEquals(a.draft,a.note.getText().toString());
                    assertEquals(unchanged,materialAndOptions(a));assertSame(firstRow,a.projectList.getChildAt(0));assertEquals(rows,a.projectList.getChildCount());
                    if(Ui.motionEnabled(a)){assertSame(oldPane,a.stage.getChildAt(0));assertEquals(2,a.stage.getChildCount());}
                    View next=a.stage.getChildAt(a.stage.getChildCount()-1);int children=a.stage.getChildCount(),afterNoops=a.checkpointNoops;
                    a.pick(first);assertEquals(1,a.step);assertEquals("ui-probe-second",a.selected);assertSame(next,a.stage.getChildAt(a.stage.getChildCount()-1));assertEquals(children,a.stage.getChildCount());assertEquals(afterNoops,a.checkpointNoops);
                    event(nonce,config,"synchronous-pick",data("guards",guards(a,part,counts),"old_rows_unchanged",true,"busy_repeat_ignored",true,"step_repeat_ignored",true,"material_and_options_unchanged",true));
                });
                scenario.onActivity(a->{
                    guards(a,part,counts);int full=Color.alpha(Ui.SCRIM);assertEquals(part[1].equals("dark")?153:102,full);
                    if(!Ui.motionEnabled(a)){a.dim(false);assertEquals(0,a.scrim.getAlpha());a.dim(true);assertEquals(full,a.scrim.getAlpha());assertNull(a.scrimAnimator);event(nonce,config,"motion-disabled-instant",data("guards",guards(a,part,counts),"full",full));sampled.countDown();return;}
                    if(a.scrimAnimator!=null){a.scrimAnimator.cancel();a.scrimAnimator=null;}a.scrim.setAlpha(0);a.dim(true);
                    ObjectAnimator[] entering={a.scrimAnimator},outgoing={null};int[] phase={0},last={full};long started=SystemClock.elapsedRealtime();JSONArray samples=new JSONArray();
                    tick[0]=()->{
                        if(stopped[0])return;
                        try{
                            guards(a,part,counts);long elapsed=SystemClock.elapsedRealtime()-started;assertTrue("Finite scrim sampling: no retry after 1600ms",elapsed<=1600);
                            int alpha=a.scrim.getAlpha();assertTrue(alpha>=0&&alpha<=full);samples.put(data("ms",elapsed,"phase",phase[0],"alpha",alpha));
                            if(phase[0]==0&&alpha>=full/4&&alpha<full){
                                int before=alpha;a.dim(false);outgoing[0]=a.scrimAnimator;assertNotSame(entering[0],outgoing[0]);assertFalse(entering[0].isStarted());assertEquals("No alpha jump when reversing",before,a.scrim.getAlpha());last[0]=before;phase[0]=1;
                                event(nonce,config,"reverse",data("before",before,"after",a.scrim.getAlpha(),"old_animator_cancelled",true));
                            }else if(phase[0]==1){
                                assertTrue("Real outgoing samples must not rebound",alpha<=last[0]);last[0]=alpha;
                                if(alpha==0){a.dim(true);assertFalse(outgoing[0].isStarted());assertEquals(0,a.scrim.getAlpha());phase[0]=2;}
                            }else if(phase[0]==2&&alpha==full){
                                assertEquals(1,a.stage.getChildCount());assertEquals(1f,a.stage.getChildAt(0).getAlpha(),0f);assertEquals(0f,a.stage.getChildAt(0).getTranslationX(),0f);
                                assertEquals(touch[0],a.root.isInTouchMode());event(nonce,config,"actual-property-samples",data("guards",guards(a,part,counts),"samples",samples,"full",full,"reversal_observed",true));
                                counts[1]++;sampled.countDown();return;
                            }
                            a.root.postOnAnimation(tick[0]);
                        }catch(Throwable error){sampleFailure[0]=error;sampled.countDown();}
                    };
                    a.root.postOnAnimation(tick[0]);
                });
                assertTrue("Bounded actual callback collection",sampled.await(2,TimeUnit.SECONDS));
                if(sampleFailure[0]!=null)throw sampleFailure[0];
                scenario.onActivity(a->{guards(a,part,counts);stopped[0]=true;if(tick[0]!=null)a.root.removeCallbacks(tick[0]);});
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(scenario!=null)try{
                    boolean completed=failure==null;
                    assertNotNull(held[0]);scenario.onActivity(a->{
                        stopped[0]=true;if(tick[0]!=null)a.root.removeCallbacks(tick[0]);
                        if(completed&&Ui.motionEnabled(a)){a.dim(false);destructionAnimator[0]=a.scrimAnimator;assertNotNull(destructionAnimator[0]);destructionAnimator[0].addListener(new AnimatorListenerAdapter(){@Override public void onAnimationCancel(Animator animation){destructionCancels[0]++;}@Override public void onAnimationEnd(Animator animation){destructionEnds[0]++;}});assertTrue(destructionAnimator[0].isStarted());}
                    });
                    scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertTrue(held[0].io.awaitTermination(3,TimeUnit.SECONDS));
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{DemoShareEditorActivity a=held[0];assertTrue(a.isDestroyed());assertNull(a.scrimAnimator);if(destructionAnimator[0]!=null){assertFalse(destructionAnimator[0].isStarted());assertEquals("Owned scrim must end exactly once, naturally or after cancellation",1,destructionEnds[0]);assertTrue(destructionCancels[0]>=0&&destructionCancels[0]<=1);}counts[2]++;event(nonce,config,"DESTROYED",data("guards",guards(a,part,counts),"owned_scrim_cancel_callbacks",destructionCancels[0],"owned_scrim_end_callbacks",destructionEnds[0]));});
                    unresolvedLifetime=false;
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            }
        }
        assertEquals(2,counts[0]);assertEquals(2,counts[2]);event(nonce,"summary","complete",data("entered",counts[0],"actual_motion_completed",counts[1],"DESTROYED",counts[2],"actual_guard_snapshots",counts[3],"scope","Memory native state/properties only; no pixel, frame-time, IME, TalkBack, genuine share or release-package claim"));
        }finally{if(!unresolvedLifetime){DemoShareEditorActivity.hierarchyFontScale=previous;HistoryShareStateTest.restore(languageBefore,darkBefore,palette);}}
    }
    static String materialAndOptions(DemoShareEditorActivity a){return a.model+"|"+a.effort+"|"+a.shared+"|"+a.attachments+"|"+a.draft+"|"+a.query+"|"+a.showAll;}
    static JSONObject guards(DemoShareEditorActivity a,String[] part,int[] counts){
        ShareEditorTest.assertSafe(a);assertTrue(DemoShareEditorActivity.hierarchyFontScale>0f);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertNull(a.discardDialog);assertNull(a.dialog);assertFalse(a.closing);assertFalse(a.busy);assertFalse(TaskSyncService.running);assertEquals("Scrim alpha changes must retain the theme RGB",Ui.SCRIM&0x00FFFFFF,a.scrim.getColor()&0x00FFFFFF);
        assertEquals(part[0],L.chinese()?"zh":"en");assertEquals(part[1].equals("dark"),Ui.dark);assertEquals(Float.parseFloat(part[2]),a.getResources().getConfiguration().fontScale,0f);counts[3]++;
        return data("memory_only",true,"forbidden_actions",a.forbiddenActions,"network",a.networkAttempts,"save",a.saveAttempts,"pair",a.pairingAttempts,"submit",a.submitAttempts,"start",a.startAttempts,"checkpoint_attempts",a.checkpointAttempts,"checkpoint_noops",a.checkpointNoops,"step",a.step,"selected",a.selected,"destroyed",a.isDestroyed(),"io_terminated",a.io.isTerminated(),"service_running",TaskSyncService.running);
    }
    static JSONObject data(Object... pairs){return HistoryShareStateTest.data(pairs);}
    static void event(String nonce,String config,String event,JSONObject value){Bundle status=new Bundle();status.putString("stream","SHARE_CONTINUITY_EVENT\t"+nonce+"\t"+config+"\t"+event+"\t"+value+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
