package app.droprun;

import android.graphics.Rect;
import android.graphics.Bitmap;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Background settings updates must keep the keyboard user's current preference visible. */
public class SettingsRefreshFocusTest {
    @Test public void refreshKeepsAppearanceKeyboardFocus()throws Exception{checkPreference(R.id.settings_appearance);}
    @Test public void refreshKeepsLanguageKeyboardFocus()throws Exception{checkPreference(R.id.settings_language);}
    void checkPreference(int id)throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        try(ActivityScenario<DemoSettingsFocusActivity> scenario=ActivityScenario.launch(DemoSettingsFocusActivity.class)){
            boolean[] wasTouch={false};scenario.onActivity(activity->wasTouch[0]=activity.getWindow().getDecorView().isInTouchMode());
            try{
                instrumentation.setInTouchMode(false);
                scenario.onActivity(activity->{
                    View preference=activity.body.findViewById(id);assertNotNull(preference);
                    assertFalse("Exercise keyboard focus rather than touch",preference.isInTouchMode());
                    assertTrue("The preference accepts keyboard focus",preference.requestFocus());
                    assertSame(preference,activity.getCurrentFocus());
                    activity.render();
                });
                awaitFrame(scenario);
                scenario.onActivity(activity->{
                    View preference=activity.body.findViewById(id);assertNotNull(preference);
                    assertSame("A rebuilt preference keeps keyboard focus",preference,activity.getCurrentFocus());
                    Rect visible=new Rect();assertTrue("Focused preference remains visible",preference.getGlobalVisibleRect(visible));
                    assertEquals("The complete preference remains on screen",preference.getHeight(),visible.height());
                    assertEquals("The complete preference fits the viewport",preference.getWidth(),visible.width());
                });
                capturePreference(context,id);
            }finally{instrumentation.setInTouchMode(wasTouch[0]);}
        }
    }
    static void capturePreference(android.content.Context context,int id)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi","false")))return;
        File external=context.getExternalFilesDir(null);assertNotNull("An evidence directory must be available",external);
        File directory=new File(external,"ui-probe-evidence");assertTrue("Create the evidence directory",directory.isDirectory()||directory.mkdirs());
        Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull("Capture the native focused screen",screenshot);
        File file=new File(directory,"settings-"+(id==R.id.settings_appearance?"appearance":"language")+"-focused.png");
        try(FileOutputStream output=new FileOutputStream(file)){assertTrue("Save the original screenshot PNG",screenshot.compress(Bitmap.CompressFormat.PNG,100,output));}finally{screenshot.recycle();}
    }
    static void awaitFrame(ActivityScenario<DemoSettingsFocusActivity> scenario)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();return true;}});root.invalidate();});
        assertTrue("Wait for native focus and scroll layout",frame.await(3,TimeUnit.SECONDS));
    }
}
