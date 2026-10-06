package app.droprun;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static app.droprun.ShareSettingsHierarchyTest.visible;
import static org.junit.Assert.*;

/** Eight capture-only windows. Never clicks any control or enters a business flow. */
public class NavigationIconCaptureTest {
    @Test public void captureMemoryHomeAndHistoryIcons()throws Exception{
        String phase=InstrumentationRegistry.getArguments().getString("captureNavigationIcons","");assertEquals("accepted",phase);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        float homeFont=DemoHomeRecoveryActivity.hierarchyFontScale,historyFont=DemoHistoryIdentityActivity.fontScale;
        try{DemoHomeRecoveryActivity.hierarchyFontScale=1;DemoHistoryIdentityActivity.fontScale=1;
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"}){
                Intent home=new Intent(context,DemoHomeRecoveryActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("hierarchyProbe",true);
                try(ActivityScenario<DemoHomeRecoveryActivity> scenario=ActivityScenario.launch(home)){capture(scenario,"home",language,theme);}
                Intent history=new Intent(context,DemoHistoryIdentityActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("historyIdentity",true).putExtra("projectId",DemoHistoryIdentityActivity.ID).putExtra("projectName",DemoHistoryIdentityActivity.sampleName(language.equals("zh"),false));
                try(ActivityScenario<DemoHistoryIdentityActivity> scenario=ActivityScenario.launch(history)){capture(scenario,"history",language,theme);}
            }
        }finally{DemoHomeRecoveryActivity.hierarchyFontScale=homeFont;DemoHistoryIdentityActivity.fontScale=historyFont;}
    }
    static View findDescription(View root,String value){if(value.contentEquals(String.valueOf(root.getContentDescription())))return root;if(root instanceof ViewGroup)for(int n=0;n<((ViewGroup)root).getChildCount();n++){View view=findDescription(((ViewGroup)root).getChildAt(n),value);if(view!=null)return view;}return null;}
    static ImageButton button(Activity activity,String screen){View view=screen.equals("home")?findDescription(activity.getWindow().getDecorView(),L.t("Settings","设置")):activity.findViewById(android.R.id.content).findViewWithTag("history-project-info");assertTrue(view instanceof ImageButton);return (ImageButton)view;}
    static boolean settled(Activity activity,String screen){View decor=activity.getWindow().getDecorView(),target=button(activity,screen);if(!activity.hasWindowFocus()||!decor.isAttachedToWindow()||decor.getWidth()!=320||decor.getHeight()!=640||decor.isLayoutRequested()||activity.getWindow().getAttributes().alpha!=1f)return false;for(View view=target;view!=null;view=view.getParent() instanceof View?(View)view.getParent():null)if(view.getAlpha()!=1f||view.getTranslationX()!=0f||view.getTranslationY()!=0f||view.isLayoutRequested())return false;return target.getWidth()>0&&target.getHeight()>0;}
    static <A extends Activity> long ready(ActivityScenario<A> scenario,String screen)throws Exception{long end=SystemClock.elapsedRealtime()+3000;boolean[] done={false};do{InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->done[0]=settled(a,screen));if(done[0])return SystemClock.elapsedRealtime();Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("Capture window/layout/alpha did not settle");return 0;}
    static void safe(Activity activity){
        assertEquals(1f,activity.getResources().getConfiguration().fontScale,0f);
        if(activity instanceof DemoHomeRecoveryActivity){DemoHomeRecoveryActivity a=(DemoHomeRecoveryActivity)activity;assertEquals(0,a.forbiddenActions.get());assertEquals(0,a.probeStore.syncCalls.get());assertFalse(a.cacheOnlyProbe);assertFalse(a.backgroundSyncEnabled());assertTrue(a.getIntent().getBooleanExtra("hierarchyProbe",false));}
        else{DemoHistoryIdentityActivity a=(DemoHistoryIdentityActivity)activity;assertEquals(0,a.forbiddenActions);assertFalse(a.busy);assertNull(a.removal);assertEquals(3,a.rows.size());assertTrue(a.pendingIds.isEmpty());assertTrue(a.getIntent().getBooleanExtra("historyIdentity",false));}
    }
    <A extends Activity> void capture(ActivityScenario<A> scenario,String screen,String language,String theme)throws Exception{
        long focusedAt=ready(scenario,screen);CountDownLatch draws=new CountDownLatch(2);int[] drawCount={0};
        scenario.onActivity(a->{View decor=a.getWindow().getDecorView();decor.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){drawCount[0]++;draws.countDown();if(draws.getCount()==0)decor.getViewTreeObserver().removeOnPreDrawListener(this);else decor.postInvalidateOnAnimation();return true;}});decor.postInvalidateOnAnimation();});
        assertTrue("Two native pre-draws completed",draws.await(3,TimeUnit.SECONDS));Thread.sleep(2000);ready(scenario,screen);JSONObject[] data={null};
        scenario.onActivity(a->{try{
            safe(a);assertEquals(theme.equals("dark"),Ui.dark);ImageButton target=button(a,screen);assertEquals(Ui.dp(a,48),target.getWidth());assertEquals(Ui.dp(a,48),target.getHeight());assertEquals(Ui.dp(a,9),target.getPaddingLeft());assertNotNull(target.getDrawable());assertEquals(Ui.dp(a,24),target.getDrawable().getIntrinsicWidth());assertEquals(Ui.dp(a,24),target.getDrawable().getIntrinsicHeight());assertEquals(Ui.TEXT,target.getImageTintList().getDefaultColor());assertTrue(target.isEnabled());assertTrue(target.isClickable());assertTrue(target.isFocusable());visible(target);
            TextView marker=a.findViewById(android.R.id.content).findViewWithTag(screen.equals("home")?"home-recovery-ui-probe":"history-memory-marker");assertNotNull(marker);assertCompleteText(marker);visible(marker);
            WindowInsetsController controller=a.getWindow().getInsetsController();assertNotNull(controller);int appearance=controller.getSystemBarsAppearance();assertEquals(theme.equals("dark")?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,appearance&WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            data[0]=new JSONObject().put("screen",screen).put("language",language).put("theme",theme).put("font_scale",1).put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("forbidden_actions",0).put("business_clicks",0).put("home_sync_calls",0).put("target_bounds",bounds(target).toShortString()).put("intrinsic_width",target.getDrawable().getIntrinsicWidth()).put("intrinsic_height",target.getDrawable().getIntrinsicHeight()).put("drawable_class",target.getDrawable().getClass().getName()).put("tint",Integer.toHexString(target.getImageTintList().getDefaultColor())).put("description",target.getContentDescription()).put("target_has_focus",target.hasFocus()).put("marker",marker.getText()).put("marker_bounds",bounds(marker).toShortString()).put("window_focus",a.hasWindowFocus()).put("window_alpha",a.getWindow().getAttributes().alpha).put("ready_at_elapsed_ms",focusedAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("completed_predraws",drawCount[0]).put("requested_system_bars_appearance",appearance).put("system_bars_pixels_require_visual_review",true);
        }catch(Exception error){throw new AssertionError(error);}});
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File directory=new File(context.getExternalFilesDir(null),"ui-probe-evidence/navigation-icons-accepted-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name=screen+"-"+language+"-"+theme+"-font1";File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());Bitmap raw=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(raw);try{assertEquals(320,raw.getWidth());assertEquals(640,raw.getHeight());try(FileOutputStream output=new FileOutputStream(png)){assertTrue(raw.compress(Bitmap.CompressFormat.PNG,100,output));}try(FileOutputStream output=new FileOutputStream(json)){output.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{raw.recycle();}
    }
}
