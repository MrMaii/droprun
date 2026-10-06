package app.droprun;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Real manual-entry controls with local-only destination recording and no persistent writes. */
public class ManualPairValidationTest {
    static final String RELAY="https://manual.example.invalid",INSTANCE="11111111-2222-3333-4444-555555555555",CODE="A123B-456C7-890D1-234E5";
    static final String EN_MANUAL_ERROR="Check the Relay address, instance ID and code. Nothing was sent; your entries are kept.";
    static final String ZH_MANUAL_ERROR="请检查中转服务地址、实例 ID 和配对码。尚未发送，输入已保留。";
    static final String EN_LINK_ERROR="Check the full link, or clear it to use the three values. Nothing was sent; your entries are kept.";
    static final String ZH_LINK_ERROR="请检查完整配对链接，或清空链接改填下方三项。尚未发送，输入已保留。";

    @Test public void invalidManualValuesKeepInputsAndExposeCompleteErrorAtLargeText()throws Exception{
        Context context=fixtureContext();requireLargeText(context);
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoManualPairActivity> scenario=launch(context,language)){
            for(String[] values:new String[][]{{"","http://manual.example.invalid",INSTANCE,CODE},{"",RELAY,"not-an-instance",CODE},{"",RELAY,INSTANCE,"ABCD"}}){
                openAndContinue(scenario,values);assertErrorAndInputs(scenario,values,language);
                scenario.onActivity(activity->{assertEquals(0,activity.confirmations);assertNoRequests(activity);activity.manualDialog.dismiss();});
            }
        }
    }
    @Test public void invalidPairingLinkKeepsInputsAndExposesCompleteErrorAtLargeText()throws Exception{
        Context context=fixtureContext();requireLargeText(context);
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoManualPairActivity> scenario=launch(context,language)){
            String[] values={"https://manual.example.invalid/pair",RELAY,INSTANCE,CODE};
            openAndContinue(scenario,values);assertErrorAndInputs(scenario,values,language);
            scenario.onActivity(activity->{assertEquals("An invalid link must not fall back to the three manual fields",0,activity.confirmations);assertNoRequests(activity);});
            scenario.onActivity(activity->{activity.manualFields[0].setText("");activity.manualDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse("Clearing the link allows the retained valid manual values",activity.manualDialog.isShowing());assertEquals(1,activity.confirmations);assertNotNull(activity.confirmedTarget);assertEquals(RELAY,activity.confirmedTarget.relay);assertEquals(INSTANCE,activity.confirmedTarget.instanceId);assertEquals(CODE.replace("-",""),activity.confirmedTarget.code);assertNoRequests(activity);});
        }
    }
    @Test public void validManualValuesAndLinkOnlyReachLocalSourceConfirmation(){
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoManualPairActivity> scenario=launch(context,language)){
            String link="droprun://pair?relay=https%3A%2F%2Fmanual.example.invalid&instance="+INSTANCE+"&code="+CODE;
            int count=0;
            for(String[] values:new String[][]{{"",RELAY,INSTANCE,CODE},{link,"","",""}}){
                openAndContinue(scenario,values);int expected=++count;
                scenario.onActivity(activity->{assertFalse(activity.manualDialog.isShowing());assertEquals(expected,activity.confirmations);assertNotNull(activity.confirmedTarget);assertEquals(RELAY,activity.confirmedTarget.relay);assertEquals(INSTANCE,activity.confirmedTarget.instanceId);assertEquals(CODE.replace("-",""),activity.confirmedTarget.code);assertNoRequests(activity);});
            }
        }
    }
    static Context fixtureContext(){Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));return context;}
    static void requireLargeText(Context context){assertEquals("Run this focused check at 200% font size",2f,context.getResources().getConfiguration().fontScale,0.01f);}
    static ActivityScenario<DemoManualPairActivity> launch(Context context,String language){return ActivityScenario.launch(new Intent(context,DemoManualPairActivity.class).putExtra("language",language));}
    static void openAndContinue(ActivityScenario<DemoManualPairActivity> scenario,String[] values){
        scenario.onActivity(activity->{activity.manualEntry();assertTrue(activity.manualDialog.isShowing());for(int n=0;n<values.length;n++)activity.manualFields[n].setText(values[n]);activity.manualFields[3].requestFocus();});
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity->activity.manualDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
    }
    static void assertErrorAndInputs(ActivityScenario<DemoManualPairActivity> scenario,String[] values,String language)throws Exception{
        View[] root={null};
        scenario.onActivity(activity->{assertTrue("Invalid values must keep manual entry open",activity.manualDialog.isShowing());root[0]=activity.manualDialog.getWindow().getDecorView();assertNotNull("Show a form error instead of attaching the error to the empty link field",root[0].findViewWithTag("manual-pair-error"));});
        awaitFrame(scenario,root[0]);awaitFrame(scenario,root[0]);
        scenario.onActivity(activity->{
            TextView error=root[0].findViewWithTag("manual-pair-error");assertEquals(View.VISIBLE,error.getVisibility());boolean usingLink=!values[0].trim().isEmpty();String expected="zh".equals(language)?(usingLink?ZH_LINK_ERROR:ZH_MANUAL_ERROR):(usingLink?EN_LINK_ERROR:EN_MANUAL_ERROR);assertEquals(expected,error.getText().toString());assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,error.getAccessibilityLiveRegion());
            for(int n=0;n<values.length;n++)assertEquals("Keep every submitted input",values[n],activity.manualFields[n].getText().toString());
            assertNull("The optional blank pairing-link field must not receive a misleading error",activity.manualFields[0].getError());
            assertTrue("Exercise a compact dialog width",error.getWidth()<=Ui.dp(activity,320));assertTrue(error.getWidth()>0);
            Layout layout=error.getLayout();assertNotNull(layout);assertEquals("Show the complete explanation",error.length(),layout.getLineEnd(layout.getLineCount()-1));
            assertTrue("All error lines must fit",error.getHeight()>=layout.getHeight()+error.getCompoundPaddingTop()+error.getCompoundPaddingBottom());
            for(int line=0;line<layout.getLineCount();line++)assertEquals("Do not ellipsize the validation explanation",0,layout.getEllipsisCount(line));
            Rect visible=new Rect();assertTrue("Scroll the form error into view",error.getGlobalVisibleRect(visible));assertEquals("The full explanation must remain visible at large text",error.getHeight(),visible.height());
            assertTrue("Continue must remain reachable",activity.manualDialog.getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(visible));assertNoRequests(activity);
            assertTrue("Capture only the still-open validation dialog",activity.manualDialog.isShowing());
        });
        captureError(language,values[0].isEmpty()?"manual":"link");
    }
    static void captureError(String language,String source)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();java.io.File base=context.getExternalFilesDir(null);assertNotNull(base);
        java.io.File directory=new java.io.File(base,"ui-probe-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500,3000);
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,"manual-pair-"+language+"-"+source+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
    static void assertNoRequests(DemoManualPairActivity activity){assertEquals("Format checking must not pair",0,activity.pairingAttempts);assertEquals("Format checking must not use a network",0,activity.networkAttempts);}
    static void awaitFrame(ActivityScenario<DemoManualPairActivity> scenario,View root)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(activity->{root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();return true;}});root.invalidate();});
        assertTrue("Wait for native form layout",frame.await(3,TimeUnit.SECONDS));
    }
}
