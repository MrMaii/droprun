package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Four closed memory windows. Parent Back routing is real; list/file/save states are synthetic. */
public class DeliveryBackHierarchyTest {
    static boolean unresolvedLifetime;
    @Test public void previewBackReturnsToFilesWithoutChangingOtherBackStates()throws Throwable{
        String opt=InstrumentationRegistry.getArguments().getString("deliveryBackProbe","");if(opt.isEmpty())return;assertEquals("accepted",opt);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertTrue(Build.VERSION.SDK_INT>=29);assertFalse(unresolvedLifetime);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoDeliveryBackActivity.class),0).exported);
        String oldLanguage=L.chinese()?"zh":"en";boolean oldDark=Ui.dark;int[] palette={Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM};
        int handles=0,entered=0,completed=0,closed=0,actualLists=0,actualPreviews=0;
        try{for(String[] config:new String[][]{{"en","light","preview"},{"zh","dark","preview"},{"en","light","picking"},{"zh","dark","picking"}}){
            assertFalse(unresolvedLifetime);assertEquals(handles,closed);String language=config[0],theme=config[1],scene=config[2];
            ActivityScenario<DemoDeliveryBackActivity> window=null;DemoDeliveryBackActivity[] activity={null};Throwable failure=null;
            try{
                unresolvedLifetime=true;
                window=ActivityScenario.launch(new Intent(target,DemoDeliveryBackActivity.class).putExtra("deliveryBackProbe","accepted").putExtra("evidenceNonce",nonce).putExtra("language",language).putExtra("appearance",theme).putExtra("scene",scene).putExtra("taskId",DemoDeliveryBackActivity.TASK_ID));handles++;
                window.onActivity(a->activity[0]=a);ready(window);window.onActivity(a->{safe(a);assertEquals(1,a.listRenders);assertEquals(1,a.previewRenders);assertNotNull(a.fileBack);assertEquals(scene.equals("picking"),a.picking);event(a,"entered");});entered++;
                if(scene.equals("preview")){
                    window.onActivity(a->{safe(a);assertTrue(back(a.page).getHeight()>=Ui.dp(a,48));assertTrue(back(a.page).performClick());assertFalse("Preview Back must stay in the delivery Activity",a.isFinishing());assertEquals(2,a.listRenders);assertNull(a.fileBack);assertNull(a.currentFile);assertNull(a.currentItem);event(a,"preview-top-back");});
                    ready(window);window.onActivity(a->{a.showMemoryPreview();safe(a);});ready(window);
                    window.onActivity(a->{safe(a);assertTrue(a.fileBack.performClick());assertEquals(3,a.listRenders);assertNull(a.fileBack);assertFalse(a.isFinishing());event(a,"body-back");a.showMemoryPreview();a.beginSyntheticSaving();assertTrue(a.saveBusy());assertFalse(a.saveButton.isEnabled());assertFalse(a.fileBack.isEnabled());assertTrue(back(a.page).performClick());assertEquals(3,a.listRenders);assertFalse(a.isFinishing());});
                    readyDialog(window);window.onActivity(a->{safe(a);View dialog=dialogRoot(a);assertNotNull(dialog);Button stay=dialog.findViewById(android.R.id.button2);assertEquals(L.t("Stay here","留在此页"),stay.getText().toString());assertTrue(stay.performClick());assertTrue(a.saveBusy());assertFalse(a.isFinishing());assertEquals(3,a.listRenders);event(a,"saving-dialog-stay");a.endSyntheticSaving();assertFalse(a.saveBusy());});
                    ready(window);window.onActivity(a->{safe(a);assertTrue(back(a.page).performClick());assertEquals(4,a.listRenders);assertNull(a.fileBack);assertFalse(a.isFinishing());});ready(window);
                    window.onActivity(a->{safe(a);assertTrue(back(a.page).performClick());assertEquals(4,a.listRenders);assertEquals(3,a.previewRenders);event(a,"list-exit-requested");});
                }else{
                    window.onActivity(a->{safe(a);assertTrue(a.picking);assertFalse(a.saveBusy());assertFalse(a.fileBack.isEnabled());assertTrue(back(a.page).performClick());assertEquals(1,a.listRenders);event(a,"picking-exit-requested");});
                }
                awaitBackExit(window,activity[0]);
                completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{
                if(window!=null)try{
                    window.close();assertEquals(Lifecycle.State.DESTROYED,window.getState());assertNotNull(activity[0]);assertTrue(activity[0].io.awaitTermination(2,TimeUnit.SECONDS));
                    assertTrue(activity[0].isDestroyed());assertEquals(0,activity[0].forbiddenActions.get());assertEquals(activity[0].listRenders,activity[0].taskReads);
                    closed++;unresolvedLifetime=false;actualLists+=activity[0].listRenders;actualPreviews+=activity[0].previewRenders;event(activity[0],"DESTROYED");
                }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
            }
        }}finally{
            if(!unresolvedLifetime&&closed==handles)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                L.language(oldLanguage);Ui.dark=oldDark;Ui.BG=palette[0];Ui.SURFACE=palette[1];Ui.SURFACE_2=palette[2];Ui.SURFACE_3=palette[3];Ui.LINE=palette[4];Ui.LINE_STRONG=palette[5];Ui.TEXT=palette[6];Ui.MUTED=palette[7];Ui.DIM=palette[8];Ui.LIME_SOFT=palette[9];Ui.LIME_LINE=palette[10];Ui.ACCENT=palette[11];Ui.DANGER=palette[12];Ui.AMBER=palette[13];Ui.SCRIM=palette[14];
            });
        }
        assertFalse(unresolvedLifetime);assertEquals(4,entered);assertEquals(4,completed);assertEquals(4,closed);assertEquals(10,actualLists);assertEquals(8,actualPreviews);
        assertEquals(oldLanguage,L.chinese()?"zh":"en");assertEquals(oldDark,Ui.dark);assertArrayEquals(palette,new int[]{Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM});
        Bundle result=new Bundle();result.putString("stream","DELIVERY_BACK_SUMMARY\t"+nonce+"\t"+new JSONObject().put("entered",entered).put("completed",completed).put("DESTROYED",closed).put("actual_list_renders",actualLists).put("actual_preview_renders",actualPreviews).put("statics_restored",true).put("scope","fixed synthetic routing; no download, verification, picker, save worker or external navigation")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,result);
    }
    static void safe(DemoDeliveryBackActivity a){
        assertEquals(0,a.forbiddenActions.get());assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertTrue(a.store.prefs.getAll().isEmpty());assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertFalse(TaskSyncService.running);
        assertEquals(a.language.equals("zh"),L.chinese());assertEquals(a.theme.equals("dark"),Ui.dark);assertEquals(a.listRenders,a.taskReads);assertNotNull(a.marker);assertEquals(DemoDeliveryBackActivity.MARKER,a.marker.getText().toString());assertTrue(a.marker.isShown());
    }
    static ImageButton back(View root){
        if(root instanceof ImageButton&&L.t("Back","返回").equals(String.valueOf(root.getContentDescription())))return (ImageButton)root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){ImageButton found=back(((ViewGroup)root).getChildAt(i));if(found!=null)return found;}return null;
    }
    static void awaitBackExit(ActivityScenario<DemoDeliveryBackActivity> window,DemoDeliveryBackActivity activity)throws Exception{
        long deadline=SystemClock.uptimeMillis()+3000;
        while(window.getState()!=Lifecycle.State.DESTROYED&&SystemClock.uptimeMillis()<deadline)Thread.sleep(16);
        assertEquals("Framework Back must close before test cleanup",Lifecycle.State.DESTROYED,window.getState());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue(activity.isFinishing());assertTrue(activity.isDestroyed());});
        assertTrue(activity.io.awaitTermination(2,TimeUnit.SECONDS));event(activity,"framework-back-closed");
    }
    static void ready(ActivityScenario<DemoDeliveryBackActivity> window)throws Exception{
        long deadline=SystemClock.uptimeMillis()+3000;boolean[] done={false};do{window.onActivity(a->{safe(a);done[0]=a.hasWindowFocus()&&a.page.isAttachedToWindow()&&a.page.getWidth()>0&&!a.page.isLayoutRequested()&&a.page.getAlpha()==1f&&a.page.getTranslationY()==0f&&back(a.page)!=null;});if(done[0])return;Thread.sleep(16);}while(SystemClock.uptimeMillis()<deadline);fail("Memory delivery page did not settle");
    }
    static View dialogRoot(DemoDeliveryBackActivity a){
        for(View root:WindowInspector.getGlobalWindowViews()){
            TextView message=root.findViewById(android.R.id.message);if(message==null)continue;
            Context context=message.getContext();while(context instanceof ContextWrapper&&context!=a){Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;}
            if(context==a&&L.t("Leaving closes this preview while the save continues. Check the selected location for the result.","离开会关闭预览，保存仍将继续。请到所选位置检查结果。").contentEquals(message.getText()))return root;
        }return null;
    }
    static void readyDialog(ActivityScenario<DemoDeliveryBackActivity> window)throws Exception{
        long deadline=SystemClock.uptimeMillis()+2000;boolean[] done={false};do{window.onActivity(a->{safe(a);View root=dialogRoot(a);Button stay=root==null?null:root.findViewById(android.R.id.button2);done[0]=stay!=null&&stay.isShown()&&stay.getHeight()>0&&!stay.isLayoutRequested();});if(done[0])return;Thread.sleep(16);}while(SystemClock.uptimeMillis()<deadline);fail("Owned save-warning dialog not visible");
    }
    static void event(DemoDeliveryBackActivity a,String phase){
        try{JSONObject data=new JSONObject().put("nonce",a.nonce).put("language",a.language).put("theme",a.theme).put("scene",a.scene).put("event",phase).put("forbidden_actions",a.forbiddenActions.get()).put("list_renders",a.listRenders).put("preview_renders",a.previewRenders).put("task_reads",a.taskReads).put("picking",a.picking).put("save_busy",a.saveBusy()).put("finishing",a.isFinishing()).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated());Bundle out=new Bundle();out.putString("stream","DELIVERY_BACK_EVENT\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,out);}catch(Exception failure){throw new AssertionError(failure);}
    }
}
