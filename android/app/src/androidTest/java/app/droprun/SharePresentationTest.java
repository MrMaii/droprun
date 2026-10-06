package app.droprun;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import static app.droprun.ShareEditorTest.*;
import static app.droprun.ShareExecutionContextTest.*;
import static org.junit.Assert.*;

/** Opt-in presentation only: six memory windows, four header clicks each and local note buffers. */
public class SharePresentationTest {
    @Test public void materialAndModelKeepClearLocalEntrypoints()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String mode=args.getString("sharePresentationProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String capture=args.getString("captureSharePresentation","");assertTrue(capture.isEmpty()||capture.equals("accepted"));String nonce=args.getString("sharePresentationNonce","");
        if(!capture.isEmpty())assertEquals("Canonical UUID is required for fresh evidence",UUID.fromString(nonce).toString(),nonce);else assertTrue("No unused capture identity",nonce.isEmpty());
        assertTrue(Build.VERSION.SDK_INT>=30);assertFalse("Do not continue after an unknown probe lifetime",unresolvedLifetime);
        File directory=null;if(!capture.isEmpty()){Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));File base=target.getExternalFilesDir(null);assertNotNull(base);directory=new File(base,"ui-probe-evidence/share-presentation-"+nonce);assertFalse("Never overwrite an earlier take",directory.exists());assertTrue(directory.mkdirs());}
        float previous=DemoShareEditorActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean[] destroyed={true};int entered=0,completed=0,closed=0,captured=0;
        try{
            for(String[] config:new String[][]{{"en","light","1"},{"en","dark","1"},{"zh","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
                String language=config[0],theme=config[1],identity="presentation|"+language+"|"+theme+"|font"+config[2];float scale=Float.parseFloat(config[2]);DemoShareEditorActivity.hierarchyFontScale=scale;
                ActivityScenario<DemoShareEditorActivity> scenario=null;Throwable failure=null;DemoShareEditorActivity[] retained={null};String[][] original={null};View[][] controls={null};
                try{
                    scenario=launchProbe(language,theme,destroyed,identity);ready(scenario);
                    scenario.onActivity(a->{safe(a);retained[0]=a;assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);original[0]=values(a);controls[0]=new View[]{a.note,a.gaugeText,send(a,language),a.back,closeButton(a)};});entered++;event(identity,"entered");scenario.onActivity(SharePresentationTest::assertPresentation);
                    scenario.onActivity(SharePresentationTest::materialSummaryCases);
                    scenario.onActivity(a->reveal(a,materialHeader(a)));awaitFrame(scenario);
                    scenario.onActivity(a->{assertSafeBounds(a,screenBounds(materialHeader(a)));readable(a,ShareMaterialDisclosureTest.summary(a));assertTrue(materialHeader(a).performClick());});ShareMaterialDisclosureTest.awaitMaterial(scenario);
                    scenario.onActivity(a->{ShareMaterialDisclosureTest.assertExactMaterial(a);ShareMaterialDisclosureTest.assertNode(a,true);assertEquals(270f,materialArrow(a).getRotation(),0.01f);assertUnchanged(a,original[0],controls[0],language);});
                    if(scale==2f){
                        scenario.onActivity(a->revealTail(a.stage.findViewWithTag("share-material-text")));awaitFrame(scenario);
                        scenario.onActivity(a->{assertSafeBounds(a,tailBounds(a.stage.findViewWithTag("share-material-text")));TextView last=a.stage.findViewWithTag("share-material-file:7");reveal(a,last);});awaitFrame(scenario);
                        scenario.onActivity(a->readable(a,a.stage.findViewWithTag("share-material-file:7")));
                    }
                    scenario.onActivity(a->reveal(a,materialHeader(a)));awaitFrame(scenario);scenario.onActivity(a->{assertSafeBounds(a,screenBounds(materialHeader(a)));assertTrue(materialHeader(a).performClick());});ShareMaterialDisclosureTest.awaitMaterial(scenario);
                    scenario.onActivity(a->reveal(a,modelHeader(a)));awaitFrame(scenario);
                    scenario.onActivity(a->{assertSafeBounds(a,screenBounds(modelHeader(a)));assertTrue(modelHeader(a).performClick());});awaitFrame(scenario);
                    scenario.onActivity(a->{assertTrue(a.panelOpen);assertEquals(View.VISIBLE,a.panel.getVisibility());ShareModelDisclosureTest.assertHeaderNode(a,true);assertEquals(270f,a.modelChevron.getRotation(),0.01f);assertUnchanged(a,original[0],controls[0],language);reveal(a,modelHeader(a));});awaitFrame(scenario);
                    scenario.onActivity(a->{assertSafeBounds(a,screenBounds(modelHeader(a)));assertTrue(modelHeader(a).performClick());});awaitFrame(scenario);
                    scenario.onActivity(a->{assertPresentation(a);assertUnchanged(a,original[0],controls[0],language);});
                    if(scale==2f){
                        for(String target:new String[]{"heading","note-label","model","execution"}){scenario.onActivity(a->reveal(a,target.equals("heading")?heading(a):target.equals("note-label")?noteLabel(a):target.equals("model")?a.gaugeText:title(a)));awaitFrame(scenario);scenario.onActivity(a->readable(a,target.equals("heading")?heading(a):target.equals("note-label")?noteLabel(a):target.equals("model")?a.gaugeText:title(a)));}
                        scenario.onActivity(a->revealTail(detail(a)));awaitFrame(scenario);scenario.onActivity(a->{assertSafeBounds(a,tailBounds(detail(a)));reveal(a,send(a,language));});awaitFrame(scenario);scenario.onActivity(a->{readable(a,send(a,language));reveal(a,a.draftStatus);});awaitFrame(scenario);scenario.onActivity(a->readable(a,a.draftStatus));
                    }else{
                        scenario.onActivity(a->a.scroll.scrollTo(0,0));awaitFrame(scenario);scenario.onActivity(a->normalVisible(a,language));
                        if(directory!=null){captureNormal(scenario,directory,nonce,language,theme);captured++;}
                    }
                    noteLabelAfterEditing(scenario,language,scale);
                    scenario.onActivity(a->{assertUnchanged(a,original[0],controls[0],language);counters(identity,a);});completed++;event(identity,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,identity,retained[0]);if(scenario!=null&&destroyed[0])closed++;}
            }
        }finally{if(destroyed[0]){DemoShareEditorActivity.hierarchyFontScale=previous;L.language(languageBefore);event("presentation","font-and-language-restored");}}
        assertEquals(6,entered);assertEquals(6,completed);assertEquals(6,closed);assertFalse(unresolvedLifetime);assertEquals(directory==null?0:4,captured);
        event("presentation","SUMMARY entered="+entered+" completed="+completed+" DESTROYED="+closed+" captures="+captured+" header_clicks_expected=24 business_actions=not_invoked_by_test");
    }
    static ViewGroup materialHeader(DemoShareEditorActivity a){return (ViewGroup)ShareMaterialDisclosureTest.header(a);}
    static View modelHeader(DemoShareEditorActivity a){return (View)a.gaugeText.getParent();}
    static ImageView materialArrow(DemoShareEditorActivity a){View child=materialHeader(a).getChildAt(1);assertTrue(child instanceof ImageView);return (ImageView)child;}
    static TextView heading(DemoShareEditorActivity a){TextView heading=findText(a.stage,L.t("What should Codex do?","想让 Codex 做什么？"));assertNotNull(heading);return heading;}
    static void materialSummaryCases(DemoShareEditorActivity a){
        safe(a);String[] before=values(a);String shared=a.shared;JSONArray files=a.attachments;View focused=a.getWindow().getDecorView().findFocus();assertEquals(8,files.length());
        try{
            a.attachments=new JSONArray();a.shared="A useful reference.";assertEquals(L.t("Text · A useful reference.","文字 · A useful reference."),a.materialSummary());
            a.shared="  https://example.invalid/posts/alpha?layout=compact#motionA  ";assertEquals(L.t("example.invalid · link","example.invalid · 链接"),a.materialSummary());
            a.attachments=new JSONArray().put(files.getJSONObject(0));a.shared="  ";assertEquals(files.getJSONObject(0).getString("name"),a.materialSummary());
            a.shared="A useful reference. https://example.invalid/posts/alpha";assertEquals(L.t("1 file · text","1 个文件 · 文字"),a.materialSummary());
            a.attachments=new JSONArray().put(files.getJSONObject(0)).put(files.getJSONObject(1));a.shared="  ";assertEquals(L.t("2 files","2 个文件"),a.materialSummary());
            a.attachments=files;a.shared=shared;assertEquals(L.t("8 files · text","8 个文件 · 文字"),a.materialSummary());
        }catch(Exception error){throw new AssertionError(error);}
        finally{a.attachments=files;a.shared=shared;}
        assertArrayEquals(before,values(a));assertSame(files,a.attachments);assertSame(focused,a.getWindow().getDecorView().findFocus());safe(a);
    }
    static void assertPresentation(DemoShareEditorActivity a){
        safe(a);assertNoteLabel(a);assertNull("Readonly material is not another filled input",ShareMaterialDisclosureTest.group(a).getBackground());ShareMaterialDisclosureTest.assertClosed(a);ShareMaterialDisclosureTest.assertNode(a,false);
        TextView summary=ShareMaterialDisclosureTest.summary(a);assertEquals(L.t("8 files · text","8 个文件 · 文字"),summary.getText().toString());assertCompleteText(summary);
        View material=materialHeader(a),model=modelHeader(a);assertTrue(material.getHeight()>=Ui.dp(a,52));assertTrue(material.getWidth()>=Ui.dp(a,48));assertTrue(material.getForeground() instanceof RippleDrawable);assertEquals(180f,materialArrow(a).getRotation(),0.01f);
        ShareDestinationReadingTest.assertDestination(a);View receipt=a.stage.findViewWithTag("share-destination-material");assertEquals(screenBounds(heading(a)).left,screenBounds(receipt).left);assertEquals(screenBounds(heading(a)).left,screenBounds(a.gaugeText).left);
        assertTrue(model.getHeight()>=Ui.dp(a,48));assertTrue(model.getWidth()>=Ui.dp(a,48));assertTrue(model.isFocusable());assertTrue(model.isClickable());assertTrue(model.getForeground() instanceof RippleDrawable);assertFalse(a.gaugeText.isFocusable());assertCompleteText(a.gaugeText);
        assertFalse(a.panelOpen);assertEquals(View.GONE,a.panel.getVisibility());assertNotNull(a.modelChevron);assertEquals(180f,a.modelChevron.getRotation(),0.01f);ShareModelDisclosureTest.assertHeaderNode(a,false);
        assertTrue(send(a,L.chinese()?"zh":"en").getHeight()>=Ui.dp(a,48));assertTrue(send(a,L.chinese()?"zh":"en").getWidth()>=Ui.dp(a,48));
    }
    static void readable(DemoShareEditorActivity a,TextView text){assertCompleteText(text);assertSafeBounds(a,screenBounds(text));}
    static void normalVisible(DemoShareEditorActivity a,String language){
        safe(a);assertPresentation(a);assertEquals(0,a.scroll.getScrollY());assertTrue(a.hasWindowFocus());View decor=a.getWindow().getDecorView();assertTrue(decor.isAttachedToWindow());assertFalse(decor.isLayoutRequested());assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertEquals(1f,a.holder.getAlpha(),0f);assertEquals(0f,a.holder.getTranslationY(),0f);assertEquals(1,a.stage.getChildCount());View pane=a.stage.getChildAt(0);assertEquals(1f,pane.getAlpha(),0f);assertEquals(0f,pane.getTranslationX(),0f);
        WindowInsets insets=decor.getRootWindowInsets();assertNotNull(insets);assertFalse("Reject any keyboard-visible capture",insets.isVisible(WindowInsets.Type.ime()));
        TextView marker=a.sheet.findViewWithTag("hierarchy-marker");assertCompleteText(marker);ShareSettingsHierarchyTest.visible(marker);Rect window=new Rect();decor.getWindowVisibleDisplayFrame(window);assertTrue(window.contains(screenBounds(marker)));assertTrue(screenBounds(marker).bottom<=screenBounds((View)a.back.getParent()).top);
        for(TextView text:new TextView[]{heading(a),noteLabel(a),ShareMaterialDisclosureTest.summary(a),a.gaugeText,title(a),detail(a),send(a,language),a.draftStatus})readable(a,text);assertSafeBounds(a,screenBounds(a.note));assertSafeBounds(a,screenBounds(materialHeader(a)));assertSafeBounds(a,screenBounds(modelHeader(a)));
    }
    static TextView noteLabel(DemoShareEditorActivity a){TextView label=a.stage.findViewWithTag("share-note-label");assertNotNull(label);return label;}
    static void assertNoteLabel(DemoShareEditorActivity a){
        TextView label=noteLabel(a);assertEquals(L.t("Your note · optional","留言 · 可选"),label.getText().toString());assertEquals(Ui.TEXT,label.getCurrentTextColor());assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,13f,a.getResources().getDisplayMetrics()),label.getTextSize(),0.01f);assertEquals(600,label.getTypeface().getWeight());
        assertFalse(label.isClickable());assertFalse(label.isFocusable());assertFalse(label.hasOnClickListeners());assertNull(label.getBackground());assertTrue(a.note.getId()!=View.NO_ID);assertEquals(a.note.getId(),label.getLabelFor());assertSame(a.note.getParent(),label.getParent());assertCompleteText(label);assertEquals(screenBounds(label).left,screenBounds(a.note).left);assertEquals(screenBounds(label).bottom+Ui.dp(a,8),screenBounds(a.note).top);
        assertEquals(L.t("Leave blank for Codex to decide.","留空，让 Codex 判断。"),a.note.getHint().toString());
    }
    static void noteLabelAfterEditing(ActivityScenario<DemoShareEditorActivity> scenario,String language,float scale)throws Throwable{
        String[] before={null,null};View[] focused={null};TextView[] originalLabel={null};Throwable failure=null;String sample=language.equals("zh")?"保留有用的想法。":"Keep the useful part.";
        try{
            scenario.onActivity(a->{safe(a);assertNoteLabel(a);before[0]=a.note.getText().toString();before[1]=a.draft;assertEquals("",before[0]);assertEquals("",before[1]);focused[0]=a.getWindow().getDecorView().findFocus();originalLabel[0]=noteLabel(a);a.note.setText(sample);assertEquals(sample,a.draft);assertSame(focused[0],a.getWindow().getDecorView().findFocus());});awaitFrame(scenario);
            if(scale==2f){
                for(String target:new String[]{"label","note","send","draft"}){scenario.onActivity(a->reveal(a,target.equals("label")?noteLabel(a):target.equals("note")?a.note:target.equals("send")?send(a,language):a.draftStatus));awaitFrame(scenario);scenario.onActivity(a->{safe(a);assertNoteLabel(a);assertSame(originalLabel[0],noteLabel(a));assertEquals(sample,a.note.getText().toString());assertEquals(sample,a.draft);readable(a,target.equals("label")?noteLabel(a):target.equals("note")?a.note:target.equals("send")?send(a,language):a.draftStatus);});}
            }else{
                scenario.onActivity(a->a.scroll.scrollTo(0,0));awaitFrame(scenario);scenario.onActivity(a->{normalVisible(a,language);assertSame(originalLabel[0],noteLabel(a));assertEquals(sample,a.note.getText().toString());assertEquals(sample,a.draft);});
            }
            scenario.onActivity(a->{safe(a);assertSame(focused[0],a.getWindow().getDecorView().findFocus());assertFalse(a.getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime()));});
        }catch(Throwable error){failure=error;throw error;}
        finally{if(before[0]!=null)try{scenario.onActivity(a->a.note.setText(before[0]));awaitFrame(scenario);scenario.onActivity(a->{safe(a);assertNoteLabel(a);assertEquals(before[0],a.note.getText().toString());assertEquals(before[1],a.draft);assertSame(focused[0],a.getWindow().getDecorView().findFocus());});}catch(Throwable restoreError){if(failure!=null)failure.addSuppressed(restoreError);else throw restoreError;}}
    }
    static void captureNormal(ActivityScenario<DemoShareEditorActivity> scenario,File directory,String nonce,String language,String theme)throws Exception{
        awaitFrame(scenario);awaitFrame(scenario);long settledAt=SystemClock.elapsedRealtime();Thread.sleep(2000);ready(scenario);JSONObject[] data={null};
        scenario.onActivity(a->{normalVisible(a,language);assertTrue(SystemClock.elapsedRealtime()-settledAt>=2000);try{data[0]=new JSONObject().put("nonce",nonce).put("language",language).put("theme",theme).put("font_scale",1).put("memory_only",true).put("capture_accepted",false).put("fixture",a.getClass().getSimpleName()).put("state","collapsed after local header checks").put("required_predraws_completed",2).put("settled_at_elapsed_ms",settledAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("window_focus",a.hasWindowFocus()).put("ime_visible",false).put("marker",((TextView)a.sheet.findViewWithTag("hierarchy-marker")).getText()).put("material_header_bounds",screenBounds(materialHeader(a)).toShortString()).put("model_header_bounds",screenBounds(modelHeader(a)).toShortString()).put("send_bounds",screenBounds(send(a,language)).toShortString()).put("title",title(a).getText()).put("detail",detail(a).getText()).put("header_clicks_expected",4).put("forbidden_actions",a.forbiddenActions).put("network_attempts",a.networkAttempts).put("save_attempts",a.saveAttempts).put("submit_attempts",a.submitAttempts).put("pairing_attempts",a.pairingAttempts).put("checkpoint_attempts",a.checkpointAttempts).put("checkpoint_noops",a.checkpointNoops).put("keyboard_bypasses",a.keyboardBypasses).put("system_bar_pixels_require_visual_review",true);}catch(Exception error){throw new AssertionError(error);}});
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String stem="share-presentation-"+language+"-"+theme+"-font1";
        try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(OutputStream out=Files.newOutputStream(new File(directory,stem+".png").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(OutputStream out=Files.newOutputStream(new File(directory,stem+".json").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
