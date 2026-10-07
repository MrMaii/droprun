package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Native editor/layout only. All samples stay in memory; Send is never activated. */
public class ShareEditorTest {
    static boolean noteUnresolvedLifetime;
    @Test public void twoLineNoteKeepsIntentAndActionsReachable()throws Throwable{
        android.os.Bundle args=InstrumentationRegistry.getArguments();String mode=args.getString("shareNoteProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=args.getString("shareNoteNonce","");assertEquals("Canonical UUID is required before any context or fixture change",java.util.UUID.fromString(nonce).toString(),nonce);
        assertTrue(android.os.Build.VERSION.SDK_INT>=30);assertFalse(noteUnresolvedLifetime);assertFalse(ShareExecutionContextTest.unresolvedLifetime);assertFalse(TaskSyncService.running);
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context target=instrumentation.getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(target.getPackageManager().getActivityInfo(new android.content.ComponentName(target,DemoShareEditorActivity.class),0).exported);
        float fontBefore=DemoShareEditorActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] paletteBefore=notePalette();int entered=0,completed=0,closed=0,states=0,reveals=0;
        try{for(String[] config:new String[][]{{"en","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
            String language=config[0],theme=config[1],scene=language+"|"+theme+"|font"+config[2];float scale=Float.parseFloat(config[2]);assertFalse(noteUnresolvedLifetime);DemoShareEditorActivity.hierarchyFontScale=scale;
            ActivityScenario<DemoShareEditorActivity> scenario=null;DemoShareEditorActivity[] retained={null};TextView[] editor={null};String[][] original={null};Throwable failure=null;
            try{
                Intent intent=new Intent(target,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("materialReview",true).putExtra("compactHeight",true).putExtra("direct",true).putExtra("confirmed",true);
                noteUnresolvedLifetime=true;noteEvent(nonce,scene,"launch-attempt",new org.json.JSONObject());scenario=ActivityScenario.launch(intent);noteEvent(nonce,scene,"returned-handle",new org.json.JSONObject());scenario.onActivity(a->retained[0]=a);ready(scenario);
                scenario.onActivity(a->{noteSafe(a);assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(language,L.chinese()?"zh":"en");assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);editor[0]=a.note;original[0]=new String[]{a.selected,a.model,a.effort,a.shared,a.attachments.toString()};assertEquals("",a.draft);assertNoteMinimum(a);noteEvent(nonce,scene,"entered",noteGeometry(a));});entered++;
                String shortNote=language.equals("zh")?"保留有用的想法。":"Keep the useful part.";
                String longNote=language.equals("zh")?"检查第 1 行。\n检查第 2 行。\n检查第 3 行。\n检查第 4 行。\n检查第 5 行。\n检查第 6 行。\n检查第 7 行。\n检查第 8 行。\n留言结尾完整保留。":"Review line 1.\nReview line 2.\nReview line 3.\nReview line 4.\nReview line 5.\nReview line 6.\nReview line 7.\nReview line 8.\nKeep the complete note tail.";
                String[] samples={"",shortNote,longNote},names={"empty","short","long"};
                for(int n=0;n<samples.length;n++){
                    String sample=samples[n],state=names[n];scenario.onActivity(a->{noteSafe(a);a.note.setText(sample);});awaitFrame(scenario);
                    scenario.onActivity(a->{noteSafe(a);assertSame(editor[0],a.note);assertArrayEquals(original[0],new String[]{a.selected,a.model,a.effort,a.shared,a.attachments.toString()});assertEquals(sample,a.note.getText().toString());assertEquals(sample,a.draft);assertNoteMinimum(a);SharePresentationTest.assertNoteLabel(a);if(scale==1f&&!state.equals("long"))a.scroll.scrollTo(0,0);});awaitFrame(scenario);
                    scenario.onActivity(a->{if(scale==1f&&!state.equals("long"))SharePresentationTest.normalVisible(a,language);reveal(a,send(a,language));});awaitFrame(scenario);
                    scenario.onActivity(a->{noteSafe(a);noteVisible(a,send(a,language));assertTrue(send(a,language).isEnabled());assertTrue(send(a,language).isClickable());assertTrue(send(a,language).isFocusable());assertTrue(send(a,language).getHeight()>=Ui.dp(a,48));assertEquals(sample,a.note.getText().toString());assertEquals(sample,a.draft);noteEvent(nonce,scene,state+"|send-visible",noteGeometry(a));});reveals++;
                    scenario.onActivity(a->reveal(a,a.draftStatus));awaitFrame(scenario);
                    scenario.onActivity(a->{noteSafe(a);noteVisible(a,a.draftStatus);assertEquals(L.t("UI sample · note not saved.","界面示例 · 留言未保存。"),a.draftStatus.getText().toString());assertEquals(sample,a.note.getText().toString());assertEquals(sample,a.draft);noteEvent(nonce,scene,state+"|draft-visible",noteGeometry(a));});reveals++;states++;
                }
                scenario.onActivity(a->{noteSafe(a);assertEquals(longNote,a.note.getText().toString());assertEquals(longNote,a.draft);assertTrue(a.note.getLayout().getLineCount()>6);noteEvent(nonce,scene,"full-completed",noteGeometry(a));});completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{if(scenario!=null)try{
                scenario.close();assertEquals("Known closure before another window or static restoration",androidx.lifecycle.Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);assertTrue("The known memory worker must finish",retained[0].io.awaitTermination(3,TimeUnit.SECONDS));noteUnresolvedLifetime=false;closed++;
                instrumentation.runOnMainSync(()->{DemoShareEditorActivity a=retained[0];assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());assertNull(a.scrimAnimator);ShareExecutionContextTest.guardCounters(a);assertFalse(TaskSyncService.running);try{noteEvent(nonce,scene,"DESTROYED",noteCounters(a).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("scrim_cleared",a.scrimAnimator==null));}catch(org.json.JSONException error){throw new AssertionError(error);}});
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(!noteUnresolvedLifetime)instrumentation.runOnMainSync(()->{DemoShareEditorActivity.hierarchyFontScale=fontBefore;L.language(languageBefore);Ui.dark=darkBefore;Ui.BG=paletteBefore[0];Ui.SURFACE=paletteBefore[1];Ui.SURFACE_2=paletteBefore[2];Ui.SURFACE_3=paletteBefore[3];Ui.LINE=paletteBefore[4];Ui.LINE_STRONG=paletteBefore[5];Ui.TEXT=paletteBefore[6];Ui.MUTED=paletteBefore[7];Ui.DIM=paletteBefore[8];Ui.LIME_SOFT=paletteBefore[9];Ui.LIME_LINE=paletteBefore[10];Ui.ACCENT=paletteBefore[11];Ui.DANGER=paletteBefore[12];Ui.AMBER=paletteBefore[13];Ui.SCRIM=paletteBefore[14];assertEquals(fontBefore,DemoShareEditorActivity.hierarchyFontScale,0f);assertEquals(languageBefore,L.chinese()?"zh":"en");assertEquals(darkBefore,Ui.dark);assertArrayEquals(paletteBefore,notePalette());try{noteEvent(nonce,"all","globals-restored",new org.json.JSONObject().put("fixture_font",DemoShareEditorActivity.hierarchyFontScale).put("language",L.chinese()?"zh":"en").put("dark",Ui.dark).put("palette_restored",java.util.Arrays.equals(paletteBefore,notePalette())));}catch(org.json.JSONException error){throw new AssertionError(error);}});}
        assertFalse(noteUnresolvedLifetime);assertEquals(4,entered);assertEquals(4,completed);assertEquals(4,closed);assertEquals(12,states);assertEquals(24,reveals);
        noteEvent(nonce,"all","summary",new org.json.JSONObject().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("draft_states",states).put("native_reveals",reveals));
    }
    static void noteSafe(DemoShareEditorActivity a){ShareExecutionContextTest.safe(a);assertFalse(TaskSyncService.running);assertFalse(a.busy);assertTrue(a.direct);assertTrue(a.confirmed);assertFalse(a.getWindow().getDecorView().getRootWindowInsets().isVisible(android.view.WindowInsets.Type.ime()));}
    static void assertNoteMinimum(DemoShareEditorActivity a){
        assertEquals(2,a.note.getMinLines());assertEquals(6,a.note.getMaxLines());assertEquals(1,a.note.getFilters().length);assertTrue(a.note.getFilters()[0] instanceof android.text.InputFilter.LengthFilter);assertEquals(15000,((android.text.InputFilter.LengthFilter)a.note.getFilters()[0]).getMax());
        assertTrue("Naturally measured input must contain at least two line heights plus its existing padding",a.note.getHeight()>=2*a.note.getLineHeight()+a.note.getCompoundPaddingTop()+a.note.getCompoundPaddingBottom());assertNotNull(a.note.getLayout());assertEquals(a.note.length(),a.note.getLayout().getLineEnd(a.note.getLayout().getLineCount()-1));
    }
    static void noteVisible(DemoShareEditorActivity a,TextView view){assertCompleteText(view);ShareExecutionContextTest.assertSafeBounds(a,screenBounds(view));Rect visible=new Rect();assertTrue(view.getGlobalVisibleRect(visible));assertEquals(screenBounds(view),visible);}
    static org.json.JSONObject noteCounters(DemoShareEditorActivity a){try{return new org.json.JSONObject().put("forbidden_actions",a.forbiddenActions).put("network",a.networkAttempts).put("save",a.saveAttempts).put("pairing",a.pairingAttempts).put("submit",a.submitAttempts).put("start",a.startAttempts).put("checkpoint_attempts",a.checkpointAttempts).put("keyboard_bypasses",a.keyboardBypasses).put("checkpoint_noops",a.checkpointNoops).put("initialization_closes",a.initializationCloses).put("service_running",TaskSyncService.running);}catch(org.json.JSONException error){throw new AssertionError(error);}}
    static org.json.JSONObject noteGeometry(DemoShareEditorActivity a){try{return noteCounters(a).put("fixture",a.getClass().getSimpleName()).put("font_scale",a.getResources().getConfiguration().fontScale).put("language",L.chinese()?"zh":"en").put("dark",Ui.dark).put("min_lines",a.note.getMinLines()).put("max_lines",a.note.getMaxLines()).put("note_width",a.note.getWidth()).put("note_height",a.note.getHeight()).put("line_height",a.note.getLineHeight()).put("padding_top",a.note.getCompoundPaddingTop()).put("padding_bottom",a.note.getCompoundPaddingBottom()).put("layout_lines",a.note.getLayout().getLineCount()).put("text",a.note.getText().toString()).put("draft",a.draft).put("scroll_y",a.scroll.getScrollY()).put("note_bounds",screenBounds(a.note).toShortString()).put("send_bounds",screenBounds(send(a,L.chinese()?"zh":"en")).toShortString()).put("draft_bounds",screenBounds(a.draftStatus).toShortString()).put("viewport",screenBounds(a.scroll).toShortString()).put("draft_feedback",a.draftStatus.getText().toString()).put("send_enabled",send(a,L.chinese()?"zh":"en").isEnabled());}catch(org.json.JSONException error){throw new AssertionError(error);}}
    static int[] notePalette(){return new int[]{Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM};}
    static void noteEvent(String nonce,String scene,String event,org.json.JSONObject data){android.os.Bundle status=new android.os.Bundle();status.putString("stream","SHARE_NOTE_EVENT\t"+nonce+"\taccepted\t"+scene+"\t"+event+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    @Test public void initialEditorAndSendRemainReachableAtLargeText()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})for(int setting=0;setting<3;setting++)try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,setting)){
            ready(scenario);scenario.onActivity(a->{assertEquals(0,a.scroll.getScrollY());assertEquals("",a.note.getText().toString());assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);assertSafe(a);assertCompleteText(findText(a.stage,language.equals("zh")?"想让 Codex 做什么？":"What should Codex do?"));});
            capture(scenario,language,setting,"initial");scrollToSend(scenario,language);
            scenario.onActivity(a->{assertTrue("Large-text editor requires real scrolling",a.scroll.getScrollY()>0);assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,setting,"send");
        }
    }
    @Test public void noteAndModelChoicesStayLocalWithoutSubmitting()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,0)){
            ready(scenario);scenario.onActivity(a->{a.note.setText(language.equals("zh")?"先整理这条材料的可用想法。":"Find the useful ideas in this material first.");assertEquals(a.note.getText().toString(),a.draft);reveal(a,(View)a.gaugeText.getParent());});awaitFrame(scenario);
            scenario.onActivity(a->{View gauge=(View)a.gaugeText.getParent();assertTrue(gauge.isFocusable());assertTrue(gauge.performClick());});awaitFrame(scenario);
            scenario.onActivity(a->{View model=a.panel.findViewWithTag("model:probe-thorough");assertNotNull(model);reveal(a,model);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(a.panel.findViewWithTag("model:probe-thorough").performClick());assertEquals("probe-thorough",a.model);assertEquals("high",a.effort);View effort=a.panel.findViewWithTag("effort:xhigh");assertNotNull(effort);reveal(a,effort);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(a.panel.findViewWithTag("effort:xhigh").performClick());assertEquals("xhigh",a.effort);assertTrue("Preserve the optional note during parameter changes",a.note.getText().length()>0);View gauge=(View)a.gaugeText.getParent();reveal(a,gauge);});awaitFrame(scenario);
            scenario.onActivity(a->{assertTrue(((View)a.gaugeText.getParent()).performClick());});awaitFrame(scenario);scrollToSend(scenario,language);
            scenario.onActivity(a->{assertFalse(a.panelOpen);assertTrue("Describe effort in the chosen language while retaining the exact request value",a.gaugeText.getText().toString().endsWith(language.equals("zh")?" · 最深":" · Most thorough"));assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,0,"edited");
        }
    }
    @Test public void cachedExecutionSettingIsReadableBeforeSendInBothLanguages()throws Exception{
        Context context=fixtureContext();
        for(String language:new String[]{"en","zh"})for(int setting=0;setting<3;setting++)try(ActivityScenario<DemoShareEditorActivity> scenario=launch(context,language,setting)){
            final int variant=setting;
            ready(scenario);scenario.onActivity(a->{
                View cue=a.stage.findViewWithTag("share-execution-setting");assertNotNull("Show the cached/unknown execution preference before Send",cue);
                TextView title=a.stage.findViewWithTag("share-execution-title"),detail=a.stage.findViewWithTag("share-execution-detail");assertNotNull(title);assertNotNull(detail);
                String expected=variant==2?(language.equals("zh")?"执行设置尚未确认":"Execution setting not confirmed"):variant==1?(language.equals("zh")?"已保存设置 · 先看计划":"Saved setting · Plan review"):(language.equals("zh")?"已保存设置 · 直接执行":"Saved setting · Direct execution");
                assertEquals("Distinguish a saved preference from an unconfirmed fallback",expected,title.getText().toString());
                String explanation=detail.getText().toString();assertTrue("Explain the first-receipt boundary",explanation.contains(language.equals("zh")?"首次接收":"first accepts"));
                String consequence=variant==2?(language.equals("zh")?"设置中查看":"Settings before sending"):variant==1?(language.equals("zh")?"批准计划":"Approve a plan"):(language.equals("zh")?"修改项目文件":"edit project files");
                assertTrue("Explain the saved preference's consequence or how to check it",explanation.contains(consequence));assertCompleteText(title);assertCompleteText(detail);
                Rect cueBounds=new Rect(),sendBounds=new Rect();cue.getDrawingRect(cueBounds);a.stage.offsetDescendantRectToMyCoords(cue,cueBounds);Button send=send(a,language);send.getDrawingRect(sendBounds);a.stage.offsetDescendantRectToMyCoords(send,sendBounds);assertTrue("Place the decision cue before Send",cueBounds.bottom<=sendBounds.top);
                assertTrue("Unknown settings do not prevent saving a share",send.isEnabled());reveal(a,title);assertSafe(a);
            });awaitFrame(scenario);scenario.onActivity(a->{assertCompleteAndVisible(a.stage.findViewWithTag("share-execution-title"));reveal(a,a.stage.findViewWithTag("share-execution-detail"));});awaitFrame(scenario);
            scenario.onActivity(a->assertCompleteAndVisible(a.stage.findViewWithTag("share-execution-detail")));capture(scenario,language,setting,"setting");scrollToSend(scenario,language);scenario.onActivity(a->{assertCompleteAndVisible(send(a,language));assertSafe(a);});capture(scenario,language,setting,"send");
        }
    }
    static Context fixtureContext(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        float font="1".equals(InstrumentationRegistry.getArguments().getString("fontScale"))?1f:2f;
        assertEquals("Run at the requested font scale",font,context.getResources().getConfiguration().fontScale,0.01f);assertEquals("Run at a compact 320dp width",320,context.getResources().getConfiguration().screenWidthDp);return context;
    }
    static ActivityScenario<DemoShareEditorActivity> launch(Context context,String language,int setting){return ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",language).putExtra("direct",setting!=1).putExtra("confirmed",setting!=2));}
    static void ready(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{awaitFrame(scenario);scenario.onActivity(a->{assertTrue(a.hasWindowFocus());assertSafe(a);assertTrue(a.sheet.getWidth()<=Ui.dp(a,320));});}
    static void awaitFrame(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(a->{a.root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){
            if(a.hasWindowFocus()&&a.holder.getWidth()>0&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f&&(a.panel==null||a.panel.getTag(R.id.expand_animation)==null)){a.root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();}else a.root.postInvalidateOnAnimation();return true;
        }});a.root.invalidate();});assertTrue("Wait for actual native layout and sheet/panel animations",frame.await(3,TimeUnit.SECONDS));
    }
    static void scrollToSend(ActivityScenario<DemoShareEditorActivity> scenario,String language)throws Exception{scenario.onActivity(a->reveal(a,send(a,language)));awaitFrame(scenario);}
    static void reveal(DemoShareEditorActivity activity,View view){assertNotNull(view);view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true);}
    static Button send(DemoShareEditorActivity activity,String language){TextView view=findText(activity.stage,language.equals("zh")?"交给 Codex":"Hand off to Codex");assertTrue("The actual primary action is present",view instanceof Button);return (Button)view;}
    static TextView findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=findText(((ViewGroup)view).getChildAt(n),text);if(found!=null)return found;}return null;}
    static void assertCompleteText(TextView text){
        assertNotNull(text);Layout layout=text.getLayout();assertNotNull(layout);assertEquals("Render every character",text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int line=0;line<layout.getLineCount();line++)assertEquals("Do not ellipsize decision copy",0,layout.getEllipsisCount(line));assertTrue("All lines fit vertically",text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());
    }
    static void assertCompleteAndVisible(TextView text){
        assertCompleteText(text);DemoShareEditorActivity activity=(DemoShareEditorActivity)text.getContext();diagnostics(activity,"visibility");
        Rect visible=new Rect(),rootVisible=new Rect(),scrollVisible=new Rect();assertTrue(text.getGlobalVisibleRect(visible));assertTrue(activity.root.getGlobalVisibleRect(rootVisible));assertTrue(activity.scroll.getGlobalVisibleRect(scrollVisible));
        Rect bounds=screenBounds(text),intersection=new Rect(bounds);assertTrue("Intersect the control with the root viewport",intersection.intersect(rootVisible));assertTrue("Intersect the control with the scroll viewport",intersection.intersect(scrollVisible));assertTrue("Intersect the control with the physical screen",intersection.intersect(screen(activity)));
        assertEquals("The entire control must fit inside all actual viewports",bounds,intersection);assertEquals("Framework visibility must cover the entire control",bounds,visible);
    }
    static Rect screenBounds(View view){int[] location=new int[2];view.getLocationOnScreen(location);return new Rect(location[0],location[1],location[0]+view.getWidth(),location[1]+view.getHeight());}
    static Rect screen(DemoShareEditorActivity activity){android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);assertEquals("Physical screen is 320dp wide",320f,metrics.widthPixels/metrics.density,0.01f);assertEquals("Physical screen is 640dp tall",640f,metrics.heightPixels/metrics.density,0.01f);return new Rect(0,0,metrics.widthPixels,metrics.heightPixels);}
    static void diagnostics(DemoShareEditorActivity activity,String phase){
        Button send=send(activity,L.chinese()?"zh":"en");Rect visible=new Rect();boolean shown=send.getGlobalVisibleRect(visible);
        String line="EditorProbe "+phase+" root="+activity.root.getHeight()+" holder="+activity.holder.getHeight()+" sheet="+activity.sheet.getHeight()+" scroll="+activity.scroll.getHeight()+" stage="+activity.stage.getHeight()+" scrollY="+activity.scroll.getScrollY()+" sendVisible="+shown+":"+visible+" sendScreen="+screenBounds(send)+" rootScreen="+screenBounds(activity.root)+" viewport="+screenBounds(activity.scroll)+" screen="+screen(activity)+"\n";
        android.os.Bundle status=new android.os.Bundle();status.putString("stream",line);InstrumentationRegistry.getInstrumentation().sendStatus(0,status);
    }
    static void assertSafe(DemoShareEditorActivity activity){assertNull(activity.incoming);assertEquals(1,activity.initializationCloses);assertEquals(0,activity.startAttempts);assertEquals(0,activity.networkAttempts);assertEquals(0,activity.saveAttempts);assertEquals(0,activity.pairingAttempts);assertEquals(0,activity.submitAttempts);assertFalse(activity.sent);}
    static void capture(ActivityScenario<DemoShareEditorActivity> scenario,String language,int setting,String stage)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        CountDownLatch frames=new CountDownLatch(1);scenario.onActivity(a->{a.root.postOnAnimation(()->{a.root.postOnAnimation(frames::countDown);a.root.invalidate();});a.root.invalidate();});assertTrue("Wait two native frame callbacks before capturing",frames.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->diagnostics(a,"capture-"+stage));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();java.io.File base=context.getExternalFilesDir(null);assertNotNull(base);java.io.File directory=new java.io.File(base,"ui-probe-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String name="share-editor-"+language+"-"+(setting==2?"unknown":setting==1?"plan":"direct")+"-"+stage+"-motion-"+(Ui.motionEnabled(context)?"on":"off")+(context.getResources().getConfiguration().fontScale==1f?"-normal-text":"")+".png";
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,name))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
}
