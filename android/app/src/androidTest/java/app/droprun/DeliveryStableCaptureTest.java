package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static app.droprun.TaskPreviewFeedbackTest.assertCompleteAndVisible;
import static app.droprun.TaskPreviewFeedbackTest.find;
import static app.droprun.TaskPreviewFeedbackTest.screenBounds;
import static org.junit.Assert.*;

/** Four opt-in, capture-only memory result windows. No click, scroll, preview or business action. */
public class DeliveryStableCaptureTest {
    static final String VERSION="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    static final String URL="https://preview.example.invalid/s/sample";

    @Test public void captureFourSettledMemoryDeliveries()throws Throwable{
        String phase=InstrumentationRegistry.getArguments().getString("captureDeliveryStable","");
        if(phase.isEmpty())return;assertEquals("accepted",phase);
        assertTrue("Insets-controller diagnostic requires API30+",Build.VERSION.SDK_INT>=30);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        assertFalse("Only the guarded nonexported fixture",context.getPackageManager().getActivityInfo(new ComponentName(context,DemoTaskPreviewNormalActivity.class),0).exported);
        String nonce=InstrumentationRegistry.getArguments().getString("evidenceNonce","");assertEquals("Canonical fresh UUID required",UUID.fromString(nonce).toString(),nonce);
        File external=context.getExternalFilesDir(null);assertNotNull(external);File directory=new File(external,"ui-probe-evidence/delivery-stable-20261006-"+nonce);
        assertTrue(directory.getCanonicalPath().startsWith(external.getCanonicalPath()+File.separator+"ui-probe-evidence"+File.separator));
        assertFalse("Never overwrite an earlier evidence run",directory.exists());assertTrue(directory.mkdirs());
        String languageBefore=L.chinese()?"zh":"en";boolean allDestroyed=true;JSONArray lifetimes=new JSONArray();
        try{
            for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"}){
                ActivityScenario<DemoTaskPreviewNormalActivity> scenario=null;Throwable failure=null;JSONObject lifetime=new JSONObject().put("language",language).put("theme",theme).put("destroyed",false);
                lifetimes.put(lifetime);allDestroyed=false;
                try{
                    Intent intent=new Intent(context,DemoTaskPreviewNormalActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("snapshotVersionProbe",VERSION);
                    scenario=ActivityScenario.launch(intent);capture(scenario,directory,language,theme,nonce);
                }catch(Throwable error){failure=error;throw error;}
                finally{
                    if(scenario!=null)try{scenario.close();assertEquals("Previous native window must be destroyed before any next launch or language restoration",Lifecycle.State.DESTROYED,scenario.getState());lifetime.put("destroyed",true).put("closed_at_elapsed_ms",SystemClock.elapsedRealtime());allDestroyed=true;}
                    catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
                }
            }
            assertTrue(allDestroyed);write(new File(directory,"run-result.json"),new JSONObject().put("nonce",nonce).put("memory_only",true).put("captures",4).put("all_destroyed",true).put("lifetimes",lifetimes).put("forbidden_actions",0).put("business_clicks",0).put("navigation_actions",0).put("scroll_actions",0));
        }finally{
            if(allDestroyed)InstrumentationRegistry.getInstrumentation().runOnMainSync(()->L.language(languageBefore));
        }
    }
    static void safe(DemoTaskPreviewNormalActivity a,String language,String theme){
        assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(language.equals("zh"),L.chinese());
        assertEquals("Guarded memory-only fixture",0,a.forbiddenActions.get());assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertEquals(2,a.global.size());assertTrue(a.store.prefs.getAll().isEmpty());
        assertTrue(a.thumbnailRequested);assertNull(a.thumbnail);assertEquals(0,a.thumbnailOpens);assertFalse(a.busy);assertFalse(a.loading);assertFalse(a.getIntent().getBooleanExtra("thumbnailProbe",false));
        assertEquals(VERSION,a.getIntent().getStringExtra("snapshotVersionProbe"));assertEquals(VERSION,a.sample.optString("preview_version"));assertEquals(URL,a.sample.optString("preview_url"));assertEquals("snapshot",a.sample.optString("preview_kind"));assertEquals("ready",a.sample.optString("preview_status"));assertEquals(Long.MAX_VALUE,a.sample.optLong("preview_expires_at"));assertEquals("completed",a.sample.optString("status"));
        assertTrue(a.expanded.isEmpty());assertNull(a.followupDialog);assertNull(a.actionErrorDialog);assertNull(a.localRemovalDialog);
        assertEquals(L.t("UI probe · memory only · no work sent","界面验证 · 仅内存 · 未发送任务"),a.notice.getText().toString());assertCompleteAndVisible(a,a.notice);
        assertCompleteAndVisible(a,find(a.body,L.t("Local preview sample","本地预览示例")));
        assertCompleteAndVisible(a,find(a.body,L.t("The result","交付结果")));
        assertCompleteAndVisible(a,find(a.body,L.t("Fixed snapshot from this handoff.","本次交付的固定快照。")));
        assertTrue(a.body.getParent().getParent() instanceof ScrollView);assertEquals(0,((ScrollView)a.body.getParent().getParent()).getScrollY());
    }
    static boolean settled(DemoTaskPreviewNormalActivity a){
        View decor=a.getWindow().getDecorView();if(!a.hasWindowFocus()||!decor.isAttachedToWindow()||decor.getWidth()!=320||decor.getHeight()!=640||decor.isLayoutRequested()||a.getWindow().getAttributes().alpha!=1f)return false;
        for(View view=a.body;view!=null;view=view.getParent() instanceof View?(View)view.getParent():null)if(view.getAlpha()!=1f||view.getTranslationX()!=0f||view.getTranslationY()!=0f||view.isLayoutRequested())return false;
        return a.body.getWidth()>0&&a.body.getHeight()>0&&a.notice.getWidth()>0;
    }
    static long ready(ActivityScenario<DemoTaskPreviewNormalActivity> scenario)throws Exception{
        long end=SystemClock.elapsedRealtime()+3000;boolean[] done={false};do{InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->done[0]=settled(a));if(done[0])return SystemClock.elapsedRealtime();Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("Native focus/layout/page and window alpha did not settle");return 0;
    }
    static void capture(ActivityScenario<DemoTaskPreviewNormalActivity> scenario,File directory,String language,String theme,String nonce)throws Exception{
        long focusedAt=ready(scenario);CountDownLatch draws=new CountDownLatch(2);int[] count={0};
        scenario.onActivity(a->{View decor=a.getWindow().getDecorView();decor.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){count[0]++;draws.countDown();if(draws.getCount()==0)decor.getViewTreeObserver().removeOnPreDrawListener(this);else decor.postInvalidateOnAnimation();return true;}});decor.postInvalidateOnAnimation();});
        assertTrue("Two native pre-draws completed",draws.await(3,TimeUnit.SECONDS));long compositorAt=SystemClock.elapsedRealtime();Thread.sleep(2000);ready(scenario);
        JSONObject[] data={null};scenario.onActivity(a->{try{
            safe(a,language,theme);assertTrue(settled(a));assertTrue(SystemClock.elapsedRealtime()-compositorAt>=2000);
            WindowInsetsController controller=a.getWindow().getInsetsController();assertNotNull(controller);int appearance=controller.getSystemBarsAppearance(),mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            assertEquals(theme.equals("dark")?0:mask,appearance&mask);WindowInsets insets=a.getWindow().getDecorView().getRootWindowInsets();assertNotNull(insets);assertTrue(insets.isVisible(WindowInsets.Type.statusBars()));assertTrue(insets.isVisible(WindowInsets.Type.navigationBars()));assertFalse(insets.isVisible(WindowInsets.Type.ime()));
            Rect marker=screenBounds(a.notice);assertTrue(marker.top>=insets.getInsets(WindowInsets.Type.statusBars()).top);assertTrue(marker.bottom<=640-insets.getInsets(WindowInsets.Type.navigationBars()).bottom);
            data[0]=new JSONObject().put("nonce",nonce).put("fixture",a.getClass().getSimpleName()).put("language",language).put("theme",theme).put("font_scale",1).put("memory_only",true).put("forbidden_actions",a.forbiddenActions.get()).put("business_clicks",0).put("navigation_actions",0).put("scroll_actions",0).put("preview_actions",0).put("notice",a.notice.getText()).put("notice_bounds",marker.toShortString()).put("body_bounds",screenBounds(a.body).toShortString()).put("scroll_y",0).put("window_focus",a.hasWindowFocus()).put("window_alpha",a.getWindow().getAttributes().alpha).put("page_alpha",a.body.getAlpha()).put("page_translation_y",a.body.getTranslationY()).put("completed_predraws",count[0]).put("ready_at_elapsed_ms",focusedAt).put("compositor_wait_at_elapsed_ms",compositorAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("requested_system_bars_appearance",appearance).put("status_bar_color",String.format("#%08X",a.getWindow().getStatusBarColor())).put("navigation_bar_color",String.format("#%08X",a.getWindow().getNavigationBarColor())).put("status_inset_top",insets.getInsets(WindowInsets.Type.statusBars()).top).put("navigation_inset_bottom",insets.getInsets(WindowInsets.Type.navigationBars()).bottom).put("ime_visible",false).put("system_bar_pixels_require_direct_visual_review",true).put("synthetic_preview_url",URL).put("synthetic_version",VERSION);
        }catch(Exception error){throw new AssertionError(error);}});
        String name="delivery-stable-"+language+"-"+theme+"-font1";File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());
        Bitmap raw=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(raw);try{assertEquals(320,raw.getWidth());assertEquals(640,raw.getHeight());assertTrue("Fresh PNG only",png.createNewFile());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(raw.compress(Bitmap.CompressFormat.PNG,100,out));}write(json,data[0]);}finally{raw.recycle();}
        scenario.onActivity(a->{safe(a,language,theme);assertTrue(settled(a));});
    }
    static void write(File file,JSONObject data)throws Exception{assertTrue("Fresh evidence file only",file.createNewFile());try(FileOutputStream out=new FileOutputStream(file)){out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));}}
}
