package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static app.droprun.SettingsRecreationTest.*;
import static org.junit.Assert.*;

/** Native settings semantics over the existing guarded memory fixture; never sends a request. */
public class SettingsSemanticsTest {
    @Test public void pendingFeedbackUsesNeutralToneAcrossRecreation()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})
            checkFeedback(language,theme,"pending");
    }
    @Test public void savedFeedbackUsesSuccessToneAcrossRecreation()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})
            checkFeedback(language,theme,"success");
    }
    @Test public void failedFeedbackAndChangedConnectionStayWarnings()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"}){
            checkFeedback(language,theme,"failure");checkFeedback(language,theme,"scope");
        }
    }
    @Test public void optionNodesIncludeVisibleDetailAndSelection()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})
            try(ActivityScenario<DemoSettingsRecreationActivity> scenario=launchTheme(language,theme,false)){
                for(int pass=0;pass<2;pass++){
                frames(scenario,false);scenario.onActivity(a->{
                    for(boolean direct:new boolean[]{true,false}){
                        String title=direct?L.t("Act on the idea","直接执行"):L.t("Review a plan first","先看计划");
                        String detail=direct?L.t("Codex works directly in the original project","转发后立刻在项目里开工"):L.t("Review and approve a plan before changes begin","先给出计划，批准后才动手");
                        assertDescription(choice(a,direct),title+L.t(", ","，")+detail+(direct?L.t(", selected","，已选择"):""));
                    }
                    // The other caller is a model picker; absent detail must not add a separator or "null".
                    for(CharSequence detail:new CharSequence[]{null,""})for(boolean selected:new boolean[]{false,true})
                        assertTemporaryDescription(a,Ui.optionRow(a,"Model",detail,selected),"Model"+(selected?L.t(", selected","，已选择"):""));
                    assertTemporaryDescription(a,Ui.optionRow(a,"Model",L.t("Default on your computer","电脑上的默认模型"),false),"Model"+L.t(", Default on your computer","，电脑上的默认模型"));
                    assertEquals(0,a.forbiddenActions.get());
                });if(pass==0)scenario.recreate();
                }
            }
    }
    void checkFeedback(String language,String theme,String state)throws Exception{
        boolean failure=state.equals("failure"),scope=state.equals("scope"),pending=state.equals("pending");
        try(ActivityScenario<DemoSettingsRecreationActivity> scenario=launchTheme(language,theme,failure)){
            DemoSettingsRecreationActivity.ControlledWork[] work={null};SettingsActivity.ModeChange[] operation={null};
            try{
                frames(scenario,false);start(scenario,work,operation);assertTrue(work[0].started.await(3,TimeUnit.SECONDS));
                if(scope)scenario.onActivity(a->a.global.put("instanceId","settings-ui-other-instance"));
                if(!pending)complete(work[0],operation[0],failure);
                frames(scenario,!pending);scenario.onActivity(a->assertFeedback(a,state));
                if(!scope){
                    scenario.recreate();frames(scenario,!pending);
                    scenario.onActivity(a->{assertSame(operation[0],a.modeChange);assertEquals(0,a.workFactories);assertFeedback(a,state);});
                    reveal(scenario,false);scenario.onActivity(a->{assertCompleteAndVisible(a,a.modeNoticeView);for(boolean direct:new boolean[]{true,false})assertCompleteAndVisible(a,choice(a,direct));});
                    captureState(scenario,language,theme,state);
                }
                assertEquals(1,work[0].calls.get());
            }finally{if(work[0]!=null)work[0].release.countDown();}
        }
    }
    static void assertFeedback(DemoSettingsRecreationActivity a,String state){
        boolean pending=state.equals("pending"),success=state.equals("success"),scope=state.equals("scope");
        String expected=pending?L.t("Saving…","正在保存…"):success?L.t("Execution preference saved.","执行偏好已保存。"):scope?L.t("The connection changed. Reopen this screen.","连接已改变，请重新打开此页面。"):L.t("Could not save the execution preference.","执行偏好保存失败。");
        assertEquals(expected,a.modeNoticeView.getText().toString());assertEquals(View.VISIBLE,a.modeNoticeView.getVisibility());
        int color=pending?Ui.MUTED:success?Ui.ACCENT:Ui.AMBER;
        assertEquals(state+" uses its semantic tone",color,a.modeNoticeView.getCurrentTextColor());
        assertTrue("Small feedback text stays above 4.5:1",contrast(color,Ui.SURFACE)>=4.5);
        assertEquals(pending,a.busy);assertChoices(a,pending);assertEquals(0,a.forbiddenActions.get());
        if(scope)assertNull(a.modeChange);
    }
    static void assertDescription(View row,String expected){
        AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();try{assertEquals("Action node includes the visible explanation",expected,String.valueOf(node.getContentDescription()));}finally{node.recycle();}
    }
    static void assertTemporaryDescription(DemoSettingsRecreationActivity a,View row,String expected){
        a.body.addView(row);try{assertTrue("Native nodes need an attached view",row.isAttachedToWindow());assertDescription(row,expected);}finally{a.body.removeView(row);}
    }
    static ActivityScenario<DemoSettingsRecreationActivity> launchTheme(String language,String theme,boolean failure){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        return ActivityScenario.launch(new Intent(context,DemoSettingsRecreationNormalActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("fail",failure));
    }
    static double luminance(int color){double result=0;double[] weights={.2126,.7152,.0722};for(int n=0;n<3;n++){double c=((color>>(16-n*8))&255)/255d;result+=weights[n]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));}return result;}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static void captureState(ActivityScenario<DemoSettingsRecreationActivity> scenario,String language,String theme,String state)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureSemanticUi")))return;
        JSONObject[] evidence={null};ViewGroup[] decor={null};android.widget.FrameLayout[] marker={null};
        try{
            scenario.onActivity(a->{try{
                assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertFeedback(a,state);
                evidence[0]=new JSONObject().put("language",language).put("theme",theme).put("state",state).put("activity_font_scale",1).put("forbidden_actions",a.forbiddenActions.get()).put("notice",a.modeNoticeView.getText()).put("notice_color",String.format("#%08X",a.modeNoticeView.getCurrentTextColor())).put("background",String.format("#%08X",Ui.SURFACE)).put("contrast",contrast(a.modeNoticeView.getCurrentTextColor(),Ui.SURFACE)).put("notice_bounds",screenBounds(a.modeNoticeView).toShortString()).put("review_bounds",screenBounds(choice(a,false)).toShortString());
                TextView label=new TextView(a);label.setText(language.equals("zh")?"UI 探针 · 仅内存 · 未发送请求":"UI probe · memory only · no request");label.setTextSize(9);label.setSingleLine(true);label.setGravity(android.view.Gravity.CENTER);label.setTextColor(0xFFFFFFFF);label.setBackgroundColor(0xFF17251D);
                marker[0]=new android.widget.FrameLayout(a);marker[0].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);marker[0].addView(label,new android.widget.FrameLayout.LayoutParams(-1,-1));
                decor[0]=(ViewGroup)a.getWindow().getDecorView();int left=Ui.dp(a,20),width=decor[0].getWidth()-2*left,height=Ui.dp(a,14),top=Ui.dp(a,28);marker[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));marker[0].layout(left,top,left+width,top+height);decor[0].getOverlay().add(marker[0]);
                assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(a.modeNoticeView)));for(boolean direct:new boolean[]{true,false})assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(choice(a,direct))));
            }catch(Exception error){throw new AssertionError(error);}});
            frames(scenario,false);InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/settings-semantics-20261005");assertTrue(directory.isDirectory()||directory.mkdirs());String name="settings-"+language+"-"+theme+"-"+state;
            File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse("Never replace original image evidence",png.exists());assertFalse("Never replace original metadata",json.exists());
            Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(evidence[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{screenshot.recycle();}
        }finally{scenario.onActivity(a->{if(decor[0]!=null&&marker[0]!=null)decor[0].getOverlay().remove(marker[0]);});}
    }
}
