package app.droprun;

import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Busy-state semantics only: the fixture never saves preferences or calls a Relay. */
public class SettingsBusyTest {
    @Test public void savingDisablesExecutionChoicesAndSuccessRestoresThem()throws Exception{checkPending(true);}
    @Test public void savingDisablesExecutionChoicesAndFailureRestoresThem()throws Exception{checkPending(false);}
    void checkPending(boolean success)throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        try(ActivityScenario<DemoSettingsBusyActivity> scenario=ActivityScenario.launch(DemoSettingsBusyActivity.class)){
            boolean[] wasTouch={false},cached={false};scenario.onActivity(activity->{wasTouch[0]=activity.body.isInTouchMode();cached[0]=activity.store.directExecution();});
            try{
                instrumentation.setInTouchMode(false);awaitLayout(scenario);
                scenario.onActivity(activity->{
                    assertEquals(0,activity.modeRequests);assertChoicesAvailable(activity,cached[0]);
                    View next=choice(activity,!cached[0]);assertTrue("Exercise real keyboard focus on the unselected option",next.requestFocus());assertSame(next,activity.getCurrentFocus());
                    assertTrue("The idle option dispatches a local request",next.performClick());assertTrue(activity.busy);assertEquals(1,activity.modeRequests);assertEquals(!cached[0],activity.requestedDirect);
                    assertEquals(L.t("Saving…","正在保存…"),activity.noticeView.getText().toString());
                });
                awaitLayout(scenario);
                scenario.onActivity(activity->{
                    assertEquals("Pending work does not change the confirmed cached selection",cached[0],activity.store.directExecution());
                    for(boolean direct:new boolean[]{true,false}){
                        View row=choice(activity,direct);assertFalse("Pending execution choices must be disabled",row.isEnabled());assertFalse("Keyboard navigation must skip pending choices",row.isFocusable());
                        assertFalse("A disabled pending choice cannot take keyboard focus",row.requestFocus());
                        AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();try{assertFalse("Accessibility must report the choice as disabled",node.isEnabled());}finally{node.recycle();}
                        row.performClick();assertEquals("Repeated clicks must not dispatch another request",1,activity.modeRequests);
                        assertEquals("Keep the old selected option until confirmation",direct==cached[0],row.getContentDescription().toString().endsWith(L.t(", selected","，已选择")));
                    }
                    activity.settle(success);
                });
                awaitLayout(scenario);
                scenario.onActivity(activity->{
                    assertFalse(activity.busy);assertChoicesAvailable(activity,cached[0]);
                    assertEquals(success?L.t("Execution preference saved.","执行偏好已保存。"):L.t("Could not save the execution preference.","执行偏好保存失败。"),activity.noticeView.getText().toString());
                    assertTrue("Restored choices can dispatch a later request",choice(activity,!cached[0]).performClick());assertEquals(2,activity.modeRequests);activity.settle(false);
                });
            }finally{instrumentation.setInTouchMode(wasTouch[0]);}
        }
    }
    static void assertChoicesAvailable(DemoSettingsBusyActivity activity,boolean cached){
        for(boolean direct:new boolean[]{true,false}){View row=choice(activity,direct);assertTrue(row.isEnabled());assertTrue(row.isFocusable());assertEquals(direct==cached,row.getContentDescription().toString().endsWith(L.t(", selected","，已选择")));}
    }
    static View choice(DemoSettingsBusyActivity activity,boolean direct){
        String title=direct?L.t("Act on the idea","直接执行"):L.t("Review a plan first","先看计划");View row=find(activity.body,title);assertNotNull("Find the actual execution option",row);return row;
    }
    static View find(View view,String title){
        CharSequence description=view.getContentDescription();if(description!=null&&(description.toString().equals(title)||description.toString().startsWith(title+L.t(", ","，"))))return view;
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View found=find(((ViewGroup)view).getChildAt(n),title);if(found!=null)return found;}return null;
    }
    static void awaitLayout(ActivityScenario<DemoSettingsBusyActivity> scenario)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(activity.hasWindowFocus()&&activity.body.getWidth()>0&&activity.body.getAlpha()==1f){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else root.postInvalidateOnAnimation();return true;
        }});root.invalidate();});assertTrue("Wait for the actual settings layout and entrance",frame.await(3,TimeUnit.SECONDS));
    }
}
