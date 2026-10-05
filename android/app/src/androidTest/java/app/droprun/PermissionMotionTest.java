package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import static org.junit.Assert.*;

/** Proposed native callback-race checks only; permission outcomes are simulated in memory. */
public class PermissionMotionTest {
    interface ProbeCase {void run(ActivityScenario<DemoPermissionMotionActivity> scenario)throws Exception;}

    @Test public void currentConfirmationAdvancesOnce()throws Exception{both(scenario->{
        Ui.CheckView check=confirmFirst(scenario);drainCallbacks(check);
        await(scenario,a->a.noteTransitions.get()==1);
        scenario.onActivity(a->{assertEquals(1,a.step);assertEquals(DemoPermissionMotionActivity.FIRST,a.selected);assertNull(a.dialog);assertEquals(1,a.navigationDismissTargets.size());assertSame(a.confirmedGlass,a.navigationDismissTargets.get(0));assertSafe(a);});
        drainCallbacks(check);scenario.onActivity(a->assertEquals("The actual success UI advances only once",1,a.noteTransitions.get()));
    });}

    @Test public void cancelledConfirmationCannotDismissNewPermission()throws Exception{both(scenario->{
        Ui.CheckView check=confirmFirst(scenario);scenario.onActivity(a->{a.onBackPressed();assertNull("Back clears the current dialog immediately",a.dialog);});
        await(scenario,a->a.confirmedGlass.overlay.getParent()==null);
        Ui.Glass[] replacement={null};scenario.onActivity(a->replacement[0]=a.openPermission(DemoPermissionMotionActivity.SECOND));
        drainCallbacks(check);
        scenario.onActivity(a->{assertSame("The old confirmation cannot close a later permission dialog",replacement[0],a.dialog);assertFalse(replacement[0].dismissed);assertNotNull(replacement[0].overlay.getParent());assertEquals(0,a.step);assertEquals(0,a.noteTransitions.get());assertEquals(0,a.navigationDismissTargets.size());assertSafe(a);});
    });}

    @Test public void replacementOpenedAndClosedDuringDismissCannotAdvance()throws Exception{both(scenario->{
        scenario.onActivity(a->a.duringDismiss=()->{
            assertNull(a.dialog);assertTrue("Inject only after the real old dismiss begins",a.confirmedGlass.dismissed);assertNotNull(a.confirmedGlass.overlay.getParent());
            a.replacementGlass=a.openPermission(DemoPermissionMotionActivity.SECOND);a.closeDialog(null);assertNull("Replacement was deliberately canceled; identity alone is null again",a.dialog);
        });
        Ui.CheckView check=confirmFirst(scenario);drainCallbacks(check);
        scenario.onActivity(a->{assertNotNull("Exercise the replacement interleaving",a.replacementGlass);assertTrue(a.replacementGlass.dismissed);assertNull(a.dialog);assertEquals(DemoPermissionMotionActivity.FIRST,a.selected);assertEquals("New permission generation prevents the old advance",0,a.noteTransitions.get());assertEquals(0,a.step);assertEquals(1,a.navigationDismissTargets.size());assertSame(a.confirmedGlass,a.navigationDismissTargets.get(0));assertSafe(a);});
    });}

    @Test public void changedSelectionDuringDismissCannotAdvance()throws Exception{both(scenario->{
        scenario.onActivity(a->a.duringDismiss=()->{assertTrue(a.confirmedGlass.dismissed);a.selected=DemoPermissionMotionActivity.SECOND;});
        Ui.CheckView check=confirmFirst(scenario);drainCallbacks(check);
        scenario.onActivity(a->{assertEquals(DemoPermissionMotionActivity.SECOND,a.selected);assertNull(a.dialog);assertEquals(0,a.noteTransitions.get());assertEquals(0,a.step);assertEquals(1,a.navigationDismissTargets.size());assertSafe(a);});
    });}

    @Test public void destroyedPageDoesNotRunOldDialogCallback()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"}){
            DemoPermissionMotionActivity[] instance={null};Ui.CheckView check;
            try(ActivityScenario<DemoPermissionMotionActivity> scenario=launch(context,language)){
                ready(scenario);scenario.onActivity(a->instance[0]=a);check=confirmFirst(scenario);
            }
            assertTrue("The actual probe was destroyed",instance[0].destroyedSignal.await(3,TimeUnit.SECONDS));drainCallbacks(check);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue(instance[0].isDestroyed());assertEquals("No old callback acts on detached dialog UI",0,instance[0].navigationDismissTargets.size());assertEquals(0,instance[0].noteTransitions.get());assertSafe(instance[0]);});
        }
    }

    static void both(ProbeCase test)throws Exception{
        Context context=fixtureContext();for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoPermissionMotionActivity> scenario=launch(context,language)){ready(scenario);test.run(scenario);}
    }
    static Context fixtureContext(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run probes in the public package",context.getPackageName().endsWith(".debug"));
        assertEquals(2f,context.getResources().getConfiguration().fontScale,0.01f);assertEquals(320,context.getResources().getConfiguration().screenWidthDp);
        assertTrue("Interleavings require the existing motion-enabled baseline; never alter it here",Ui.motionEnabled(context));return context;
    }
    static ActivityScenario<DemoPermissionMotionActivity> launch(Context context,String language){return ActivityScenario.launch(new Intent(context,DemoPermissionMotionActivity.class).putExtra("language",language));}
    static void ready(ActivityScenario<DemoPermissionMotionActivity> scenario)throws Exception{
        await(scenario,a->a.probeReady&&a.hasWindowFocus()&&a.holder.getWidth()>0&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f);
        scenario.onActivity(a->{assertEquals(1,a.initializationCloses);assertEquals(0,a.step);assertSafe(a);});
    }
    static Ui.CheckView confirmFirst(ActivityScenario<DemoPermissionMotionActivity> scenario){
        Ui.CheckView[] check={null};scenario.onActivity(a->{Ui.Glass permission=a.openPermission(DemoPermissionMotionActivity.FIRST);check[0]=a.simulateConfirmation(permission,DemoPermissionMotionActivity.FIRST);assertSafe(a);});return check[0];
    }
    static void await(ActivityScenario<DemoPermissionMotionActivity> scenario,Predicate<DemoPermissionMotionActivity> condition)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] reached={false};
        do{scenario.onActivity(a->reached[0]=condition.test(a));if(reached[0])return;Thread.sleep(16);}while(SystemClock.elapsedRealtime()<deadline);
        fail("Actual native boundary did not settle in 3s");
    }
    static void drainCallbacks(Ui.CheckView check)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+3000;boolean[] completed={false};
        do{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->completed[0]=check.progress==1f);if(completed[0])break;Thread.sleep(16);}while(SystemClock.elapsedRealtime()<deadline);
        assertTrue("Wait for the actual CheckView animation",completed[0]);
        CountDownLatch barrier=new CountDownLatch(1);new Handler(Looper.getMainLooper()).postDelayed(barrier::countDown,700);assertTrue("Drain the existing delayed callback/dismiss window",barrier.await(3,TimeUnit.SECONDS));
        CountDownLatch frames=new CountDownLatch(1);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->android.view.Choreographer.getInstance().postFrameCallback(first->android.view.Choreographer.getInstance().postFrameCallback(second->frames.countDown())));assertTrue("Wait for two native frames",frames.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    static void assertSafe(DemoPermissionMotionActivity activity){assertNull("Never open ShareImport",activity.incoming);assertEquals("No permission/API/credential/preferences/file/save/service/navigation action",0,activity.forbiddenActions.get());}
}
