package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic fallback UI. Only disclosure clicks; diagnostics require an explicit opt-in. */
public class DeliveryVerificationDisclosureTest {
    @Test public void fallbackVerificationDetailsAreOptionalAndComplete()throws Exception{
        float oldFont=DemoDeliveryVerificationActivity.probeFontScale;String oldLanguage=L.chinese()?"zh":"en";
        List<ActivityScenario<DemoDeliveryVerificationActivity>> windows=new ArrayList<>();
        Throwable runFailure=null;boolean launchPending=false;
        try{for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})for(float font:new float[]{1f,2f}){
            DemoDeliveryVerificationActivity.probeFontScale=font;
            Intent intent=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),DemoDeliveryVerificationActivity.class)
                    .putExtra("verificationUiProbe",true).putExtra("language",language).putExtra("appearance",theme);
            launchPending=true;ActivityScenario<DemoDeliveryVerificationActivity> opened=ActivityScenario.launch(intent);windows.add(opened);launchPending=false;
            try(ActivityScenario<DemoDeliveryVerificationActivity> scenario=opened){
                ready(scenario);
                int[] collapsedTop={0},expandedTop={0};
                scenario.onActivity(a->{safe(a);assertEquals(font,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);
                    TextView hash=find(a.page,DemoDeliveryVerificationActivity.HASH);assertNotNull(hash);assertFalse("Baseline red: permanent full hash must become optional",hash.isShown());
                    TextView algorithm=find(a.page,"SHA-256");assertNotNull(algorithm);assertFalse(algorithm.isShown());
                    assertFalse(a.verificationExpanded);assertTrue(a.saveButton.isEnabled());assertTrue(a.fileBack.isEnabled());
                    TextView contents=find(a.page,L.t("File contents","文件内容"));assertNotNull(contents);collapsedTop[0]=contents.getTop();
                    View header=header(a);assertTrue(header.isClickable());assertTrue(header.isFocusable());assertTrue(header.getHeight()>=Ui.dp(a,48));
                    assertTrue(header.getContentDescription().toString().contains(L.t("collapsed","已折叠")));
                });
                capture(scenario,language,theme,font,"collapsed");
                scenario.onActivity(a->{safe(a);View h=header(a);h.requestRectangleOnScreen(new Rect(0,0,h.getWidth(),h.getHeight()),true);});
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{safe(a);whole(header(a));assertTrue(header(a).performClick());safe(a);});
                settled(scenario,true);
                scenario.onActivity(a->{safe(a);assertTrue(a.verificationExpanded);TextView hash=find(a.page,DemoDeliveryVerificationActivity.HASH);assertTrue(hash.isShown());assertTrue(hash.isTextSelectable());complete(hash);assertTrue(find(a.page,"SHA-256").isShown());
                    expandedTop[0]=find(a.page,L.t("File contents","文件内容")).getTop();assertTrue("Collapsed evidence consumes less space in the same build",expandedTop[0]>collapsedTop[0]);
                    assertNotNull(find(a.page,L.t("Save to open","可保存后查看")));assertEquals(1,a.blockedPathReads);
                });
                reveal(scenario,DemoDeliveryVerificationActivity.HASH);
                String explanation=language.equals("zh")?"预览只显示内容，不执行 HTML、脚本或补丁。":"Preview displays content only. HTML, scripts and patches are not executed.";
                reveal(scenario,explanation);
                capture(scenario,language,theme,font,"expanded-tail");
                scenario.onActivity(a->{safe(a);});scenario.recreate();ready(scenario);settled(scenario,true);
                scenario.onActivity(a->{safe(a);assertTrue("UI-only expansion survives recreation",a.verificationExpanded);assertTrue(find(a.page,DemoDeliveryVerificationActivity.HASH).isShown());
                    ((ScrollView)a.page.getParent()).scrollTo(0,0);View h=header(a);h.requestRectangleOnScreen(new Rect(0,0,h.getWidth(),h.getHeight()),true);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{safe(a);whole(header(a));assertTrue(header(a).performClick());safe(a);});
                settled(scenario,false);
                scenario.onActivity(a->{safe(a);assertFalse(a.verificationExpanded);assertFalse(find(a.page,DemoDeliveryVerificationActivity.HASH).isShown());assertTrue(a.saveButton.isEnabled());assertTrue(a.fileBack.isEnabled());});
                scenario.onActivity(a->{safe(a);((ScrollView)a.page.getParent()).scrollTo(0,0);});
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                capture(scenario,language,theme,font,"recreated-collapsed");
            }
            assertEquals("Each owned window is destroyed before the next configuration",Lifecycle.State.DESTROYED,opened.getState());
        }}catch(Exception|Error error){runFailure=error;throw error;}
        finally{
            Throwable closeFailure=null;
            for(ActivityScenario<DemoDeliveryVerificationActivity> window:windows)try{if(window.getState()!=Lifecycle.State.DESTROYED)window.close();}catch(Throwable error){if(closeFailure==null)closeFailure=error;else closeFailure.addSuppressed(error);}
            boolean destroyed=true;for(ActivityScenario<DemoDeliveryVerificationActivity> window:windows)destroyed&=window.getState()==Lifecycle.State.DESTROYED;
            if(launchPending||!destroyed){AssertionError error=new AssertionError("Fixture ownership/closure is unconfirmed; do not restore context",runFailure!=null?runFailure:closeFailure);if(runFailure!=null&&closeFailure!=null&&runFailure!=closeFailure)error.addSuppressed(closeFailure);throw error;}
            DemoDeliveryVerificationActivity.probeFontScale=oldFont;L.language(oldLanguage);
            if(closeFailure!=null){if(runFailure!=null)runFailure.addSuppressed(closeFailure);else throw new AssertionError("Fixture cleanup reported a failure",closeFailure);}
        }
    }
    static void safe(DemoDeliveryVerificationActivity a){
        assertEquals(0,a.forbiddenActions);assertEquals(1,a.previewRenders);assertEquals(1,a.initialLists);assertEquals(1,a.blockedPathReads);assertTrue(a.currentFile instanceof DemoDeliveryVerificationActivity.NoIoFile);
        assertTrue(a.marker.isAttachedToWindow());assertTrue(a.marker.isShown());complete(a.marker);
        // Page marker can scroll off-screen in private tail evidence; its exact
        // text/height must still be recorded. Public frames need whole(marker).
        assertTrue(a.marker.getText().toString().contains(L.t("no download or verification","未下载或校验")));
    }
    static View header(DemoDeliveryVerificationActivity a){
        TextView title=find(a.page,L.t("Verified file · ","文件已核对 · ")+DeliverablesActivity.size(0));assertNotNull(title);complete(title);
        return (View)title.getParent();
    }
    static TextView find(View view,String text){
        if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return (TextView)view;
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView result=find(((ViewGroup)view).getChildAt(n),text);if(result!=null)return result;}return null;
    }
    static void ready(ActivityScenario<DemoDeliveryVerificationActivity> scenario)throws Exception{
        long limit=SystemClock.uptimeMillis()+4000;boolean[] ready={false};
        while(SystemClock.uptimeMillis()<limit){scenario.onActivity(a->{assertEquals(0,a.forbiddenActions);assertEquals(1,a.blockedPathReads);assertNotNull(a.marker);assertTrue(a.marker.getText().toString().contains(L.t("no download or verification","未下载或校验")));
            ready[0]=a.hasWindowFocus()&&a.page.isAttachedToWindow()&&a.page.getWidth()>0&&a.page.getAlpha()==1f&&a.getWindow().getAttributes().alpha==1f&&a.marker.isAttachedToWindow()&&a.marker.getLayout()!=null;
            if(ready[0]){safe(a);whole(a.marker);}
        });if(ready[0])return;Thread.sleep(16);}fail("Native focus/layout/alpha readiness not reached");
    }
    static void settled(ActivityScenario<DemoDeliveryVerificationActivity> scenario,boolean open)throws Exception{
        long limit=SystemClock.uptimeMillis()+3000;boolean[] done={false};
        while(SystemClock.uptimeMillis()<limit){scenario.onActivity(a->{safe(a);View h=header(a);LinearLayout group=(LinearLayout)h.getParent();View evidence=group.getChildAt(1);View arrow=((ViewGroup)h).getChildAt(1);
            done[0]=evidence.getLayoutParams().height==ViewGroup.LayoutParams.WRAP_CONTENT&&evidence.getVisibility()==(open?View.VISIBLE:View.GONE)&&Math.abs(arrow.getRotation()-(open?270f:180f))<0.01f;
        });if(done[0])return;Thread.sleep(16);}fail("Disclosure native endpoint not reached");
    }
    static void reveal(ActivityScenario<DemoDeliveryVerificationActivity> scenario,String text)throws Exception{
        scenario.onActivity(a->{safe(a);TextView v=find(a.page,text);assertNotNull(v);assertTrue(v.isTextSelectable());complete(v);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(a->{safe(a);TextView v=find(a.page,text);whole(v);Rect body=new Rect(),marker=new Rect();v.getGlobalVisibleRect(body);a.marker.getGlobalVisibleRect(marker);assertFalse("Marker must not cover evidence",Rect.intersects(body,marker));});
    }
    static void complete(TextView view){
        Layout layout=view.getLayout();assertNotNull(layout);assertTrue(layout.getLineCount()>0);assertEquals(view.getText().length(),layout.getLineEnd(layout.getLineCount()-1));
        for(int n=0;n<layout.getLineCount();n++)assertEquals(0,layout.getEllipsisCount(n));assertTrue(layout.getLineBottom(layout.getLineCount()-1)<=view.getHeight()-view.getCompoundPaddingTop()-view.getCompoundPaddingBottom());
    }
    static void whole(View view){Rect visible=new Rect();assertTrue(view.getGlobalVisibleRect(visible));assertEquals(view.getWidth(),visible.width());assertEquals(view.getHeight(),visible.height());}
    static Rect bounds(View view){int[] xy=new int[2];view.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());}
    static boolean fullyVisible(View view){Rect visible=new Rect();return view.getGlobalVisibleRect(visible)&&visible.width()==view.getWidth()&&visible.height()==view.getHeight();}
    static JSONObject textData(TextView view)throws Exception{
        Layout layout=view.getLayout();JSONObject value=new JSONObject().put("text",view.getText()).put("shown",view.isShown()).put("selectable",view.isTextSelectable()).put("bounds",bounds(view).toShortString()).put("whole_in_frame",fullyVisible(view)).put("text_size_px",view.getTextSize());
        if(layout!=null){value.put("line_count",layout.getLineCount()).put("last_line_end",layout.getLineEnd(layout.getLineCount()-1)).put("text_length",view.getText().length());Rect last=new Rect();layout.getLineBounds(layout.getLineCount()-1,last);value.put("last_line_rect",last.toShortString());}return value;
    }
    static void capture(ActivityScenario<DemoDeliveryVerificationActivity> scenario,String language,String theme,float font,String state)throws Exception{
        String requested=InstrumentationRegistry.getArguments().getString("captureDeliveryEvidence","");
        if(requested.isEmpty())return;assertEquals("true",requested);
        String run=InstrumentationRegistry.getArguments().getString("captureDeliveryEvidenceRun","");assertTrue("Fresh 32-hex evidence run identifier",run.matches("[0-9a-f]{32}"));
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));
        long limit=SystemClock.uptimeMillis()+4000;boolean[] ready={false};
        do{InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->{safe(a);View decor=a.getWindow().getDecorView();ready[0]=a.hasWindowFocus()&&decor.getWidth()==320&&decor.getHeight()==640&&!decor.isLayoutRequested()&&!a.page.isLayoutRequested()&&a.page.getAlpha()==1f&&a.page.getTranslationX()==0f&&a.page.getTranslationY()==0f&&a.getWindow().getAttributes().alpha==1f;});if(ready[0])break;Thread.sleep(16);}while(SystemClock.uptimeMillis()<limit);
        assertTrue("Capture focus/layout/alpha endpoint",ready[0]);CountDownLatch draws=new CountDownLatch(2);int[] count={0};
        scenario.onActivity(a->{safe(a);View decor=a.getWindow().getDecorView();decor.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){count[0]++;draws.countDown();if(draws.getCount()==0)decor.getViewTreeObserver().removeOnPreDrawListener(this);else decor.postInvalidateOnAnimation();return true;}});decor.postInvalidateOnAnimation();});
        assertTrue("Two native pre-draws",draws.await(3,TimeUnit.SECONDS));Thread.sleep(2000);JSONObject[] data={null};
        scenario.onActivity(a->{try{safe(a);assertEquals(font,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertTrue(a.hasWindowFocus());assertEquals(1f,a.page.getAlpha(),0f);assertEquals(0f,a.page.getTranslationY(),0f);assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertFalse(a.page.isLayoutRequested());
            boolean expanded=state.equals("expanded-tail");assertEquals(expanded,a.verificationExpanded);View h=header(a);LinearLayout group=(LinearLayout)h.getParent();View evidence=group.getChildAt(1),arrow=((ViewGroup)h).getChildAt(1);assertEquals(expanded?View.VISIBLE:View.GONE,evidence.getVisibility());assertEquals(expanded?270f:180f,arrow.getRotation(),0.01f);assertTrue(h.getHeight()>=Ui.dp(a,48));
            TextView hash=find(a.page,DemoDeliveryVerificationActivity.HASH),explanation=find(a.page,L.t("Preview displays content only. HTML, scripts and patches are not executed.","预览只显示内容，不执行 HTML、脚本或补丁。"));assertNotNull(hash);assertNotNull(explanation);assertEquals(expanded,hash.isShown());assertEquals(expanded,explanation.isShown());if(expanded){complete(hash);complete(explanation);whole(explanation);}else whole(a.marker);
            data[0]=new JSONObject().put("fixture","DemoDeliveryVerificationActivity").put("scope","Synthetic fallback UI; no download or verification").put("language",language).put("theme",theme).put("font_scale",font).put("state",state).put("ui_only_recreation",state.equals("recreated-collapsed")).put("production_file_restore_executed",false).put("forbidden_actions",a.forbiddenActions).put("blocked_path_reads",a.blockedPathReads).put("preview_renders",a.previewRenders).put("initial_lists",a.initialLists).put("file_substitute",a.currentFile.getClass().getSimpleName()).put("allowed_source_actions","Disclosure header and Activity-local recreation only; Save/Back never clicked").put("disclosure_expanded",expanded).put("header_bounds",bounds(h).toShortString()).put("header_description",h.getContentDescription()).put("header_focusable",h.isFocusable()).put("chevron_rotation",arrow.getRotation()).put("marker",textData(a.marker)).put("hash",textData(hash)).put("explanation",textData(explanation)).put("content_label",textData(find(a.page,L.t("File contents","文件内容")))).put("summary",textData(find(a.page,L.t("Verified file · ","文件已核对 · ")+DeliverablesActivity.size(0)))).put("save_enabled",a.saveButton.isEnabled()).put("back_enabled",a.fileBack.isEnabled()).put("window_focus",a.hasWindowFocus()).put("page_alpha",a.page.getAlpha()).put("window_alpha",a.getWindow().getAttributes().alpha).put("animators_enabled",android.animation.ValueAnimator.areAnimatorsEnabled()).put("completed_predraws",count[0]).put("captured_at_elapsed_ms",SystemClock.elapsedRealtime()).put("marker_required_for_public_media",true);
        }catch(Exception error){throw new AssertionError(error);}});
        // Instrumentation diagnostics use the unwrapped target context, never the fixture's business-file hooks.
        File external=target.getExternalFilesDir(null);assertNotNull("Explicit external diagnostics root",external);File directory=new File(external,"ui-probe-evidence/delivery-verification-accepted-20261006/"+run);assertTrue(directory.isDirectory()||directory.mkdirs());String name="delivery-verification-"+language+"-"+theme+"-font"+(int)font+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse("No evidence overwrite",png.exists());assertFalse("No metadata overwrite",json.exists());
        Bitmap raw=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(raw);try{assertEquals(320,raw.getWidth());assertEquals(640,raw.getHeight());try(FileOutputStream output=new FileOutputStream(png)){assertTrue(raw.compress(Bitmap.CompressFormat.PNG,100,output));}try(FileOutputStream output=new FileOutputStream(json)){output.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{raw.recycle();}
    }

}
