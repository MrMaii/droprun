package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Six opt-in memory windows. No Send, editor, model, permission, import or OS action. */
public class ShareDestinationReadingTest {
    static boolean unresolvedLifetime;
    int guardSnapshots;
    static final String PHASE="accepted",ID="memory-project-a";

    @Test public void completeDestinationLabelAndSendStayReachable()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String mode=args.getString("shareDestinationReadingProbe","");if(mode.isEmpty())return;assertEquals(PHASE,mode);
        String nonce=args.getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);
        assertTrue(Build.VERSION.SDK_INT>=30);assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);assertEquals(0f,DemoShareEditorActivity.hierarchyFontScale,0f);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));
        assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoShareEditorActivity.class),0).exported);
        File base=target.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/share-destination-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());
        String priorLanguage=L.chinese()?"zh":"en";float priorFont=DemoShareEditorActivity.hierarchyFontScale;boolean destroyed=true;
        int entered=0,completed=0,closed=0,captured=0,linesRead=0,longLabelWindows=0,idCharactersRead=0;
        try{for(String[] config:new String[][]{{"en","light","1"},{"en","dark","1"},{"zh","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
            String language=config[0],theme=config[1],identity=language+"|"+theme+"|font"+config[2];float scale=Float.parseFloat(config[2]);boolean longName=scale==2f;
            assertTrue(destroyed);assertFalse(unresolvedLifetime);DemoShareEditorActivity.hierarchyFontScale=scale;
            ActivityScenario<DemoShareEditorActivity> scenario=null;Throwable failure=null;DemoShareEditorActivity[] retained={null};String[][] fields={null};View[] focus={null};String[] hint={""};
            try{
                Intent intent=new Intent(target,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("compactHeight",true).putExtra("direct",true).putExtra("confirmed",true);
                destroyed=false;unresolvedLifetime=true;event(nonce,identity,"launch-attempt",new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale));
                scenario=ActivityScenario.launch(intent);event(nonce,identity,"returned-handle",new JSONObject());frame(scenario);
                scenario.onActivity(a->{retained[0]=a;safe(a,language,theme,scale);if(longName)hint[0]=seedFullLabel(a,language);});frame(scenario);
                scenario.onActivity(a->{safe(a,language,theme,scale);fields[0]=fields(a);focus[0]=a.getWindow().getDecorView().findFocus();assertDestination(a);});entered++;
                scenario.onActivity(a->event(nonce,identity,"entered",extend(metadata(a,language,theme,scale),"id_hint",hint[0],"label_scope",longName?"ProjectPresentation.label output placed into fixture memory project.name; not catalog integration":"preset fixture project")));
                if(!longName){capture(scenario,directory,nonce,identity,language,theme);captured++;}
                int[] total={0},readId={0};scenario.onActivity(a->total[0]=destination(a).getLayout().getLineCount());
                for(int index=0;index<total[0];index++){
                    final int line=index;scenario.onActivity(a->{unchanged(a,fields[0],focus[0]);TextView text=destination(a);text.requestRectangleOnScreen(localLine(text,line),true);});frame(scenario);
                    scenario.onActivity(a->{unchanged(a,fields[0],focus[0]);TextView text=destination(a);assertComplete(text);Rect shown=screenLine(text,line);assertSafeRect(a,shown,true);Layout layout=text.getLayout();String value=text.getText().toString();
                        int begin=hint[0].isEmpty()?-1:value.lastIndexOf(hint[0]),end=begin<0?-1:begin+hint[0].length();int chars=begin<0?0:Math.max(0,Math.min(end,layout.getLineEnd(line))-Math.max(begin,layout.getLineStart(line)));readId[0]+=chars;
                        event(nonce,identity,"destination-line",extend(metadata(a,language,theme,scale),"line_index",line,"line_start",layout.getLineStart(line),"line_end",layout.getLineEnd(line),"line_text",value.substring(layout.getLineStart(line),layout.getLineEnd(line)),"line_bounds",rect(shown),"line_visible",true,"id_characters_on_line",chars));
                    });linesRead++;
                }
                if(longName){assertTrue(total[0]>1);assertEquals(hint[0].length(),readId[0]);longLabelWindows++;idCharactersRead+=readId[0];}
                scenario.onActivity(a->{unchanged(a,fields[0],focus[0]);Button button=send(a);button.requestRectangleOnScreen(new Rect(0,0,button.getWidth(),button.getHeight()),true);});frame(scenario);
                scenario.onActivity(a->{unchanged(a,fields[0],focus[0]);Button button=send(a);assertComplete(button);assertSafeRect(a,bounds(button),true);assertTrue(button.isEnabled());assertTrue(button.isClickable());assertTrue(button.isFocusable());assertTrue(button.getWidth()>=Ui.dp(a,48));assertTrue(button.getHeight()>=Ui.dp(a,48));
                    TextView text=destination(a);assertTrue("Send remains a separate control after the complete destination",bounds(button).top>bounds(text).bottom);
                    event(nonce,identity,"send-reachable",extend(metadata(a,language,theme,scale),"send_bounds",rect(bounds(button)),"send_enabled",button.isEnabled(),"send_clicked",false));
                });
                scenario.onActivity(a->{unchanged(a,fields[0],focus[0]);event(nonce,identity,"full-completed",extend(metadata(a,language,theme,scale),"lines_read",total[0],"id_hint",hint[0],"id_characters_read",readId[0],"capture_count",longName?0:1));});completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{if(scenario!=null)try{
                scenario.close();assertEquals("Known handle must reach DESTROYED",Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);assertTrue(retained[0].io.awaitTermination(3,TimeUnit.SECONDS));
                DemoShareEditorActivity a=retained[0];InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{safe(a,language,theme,scale);assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());event(nonce,identity,"DESTROYED",metadata(a,language,theme,scale));});destroyed=true;unresolvedLifetime=false;closed++;
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(destroyed){DemoShareEditorActivity.hierarchyFontScale=priorFont;L.language(priorLanguage);event(nonce,"all","font-and-language-restored",new JSONObject().put("font_scale",DemoShareEditorActivity.hierarchyFontScale).put("language",L.chinese()?"zh":"en"));}}
        assertEquals(6,entered);assertEquals(6,completed);assertEquals(6,closed);assertEquals(4,captured);assertEquals(2,longLabelWindows);assertFalse(unresolvedLifetime);assertEquals(priorFont,DemoShareEditorActivity.hierarchyFontScale,0f);
        Bundle summary=new Bundle();summary.putString("stream","SHARE_DESTINATION_READING_SUMMARY\t"+nonce+"\t"+PHASE+"\t"+new JSONObject().put("entered",entered).put("full_completed",completed).put("DESTROYED",closed).put("captures",captured).put("lines_read",linesRead).put("long_label_windows",longLabelWindows).put("id_characters_read",idCharactersRead).put("guard_snapshots",guardSnapshots).put("business_actions","not_invoked_by_test").put("catalog_integration","not exercised")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,summary);
    }

    static String seedFullLabel(DemoShareEditorActivity a,String language){
        String name=language.equals("zh")?"DropRun 产品体验 — 分享目的地与交付流程的完整项目名称，包含界面可读性、操作反馈与长名称尾部识别信息":"DropRun product experience — extended project destination for interface and delivery refinements with a complete name";
        try{JSONArray catalog=new JSONArray().put(new JSONObject().put("id",ID).put("name",name)).put(new JSONObject().put("id","memory-project-b").put("name",name));String label=ProjectPresentation.label(ID,name,catalog,new JSONArray());assertTrue(label.startsWith(name+" · "));String hint=label.substring((name+" · ").length());assertFalse(hint.isEmpty());assertTrue(ID.startsWith(hint));
            a.store.project(a.selected).put("name",label);a.stage.removeAllViews();a.stage.addView(a.stepNote(),new ViewGroup.LayoutParams(-1,-2));a.scroll.scrollTo(0,0);return hint;
        }catch(Exception error){throw new AssertionError(error);}
    }
    static TextView destination(DemoShareEditorActivity a){assertEquals(1,a.stage.getChildCount());assertTrue(a.stage.getChildAt(0) instanceof LinearLayout);View text=a.stage.findViewWithTag("share-destination-name");assertTrue(text instanceof TextView);return (TextView)text;}
    static Button send(DemoShareEditorActivity a){View view=a.stage.findViewWithTag("share-send");assertTrue(view instanceof Button);return (Button)view;}
    static void assertDestination(DemoShareEditorActivity a){TextView text=destination(a);assertEquals(L.t("For “","转发到「")+a.projectName()+L.t("”","」"),text.getText().toString());assertEquals(Ui.TEXT,text.getCurrentTextColor());assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,17f,a.getResources().getDisplayMetrics()),text.getTextSize(),0.01f);assertEquals(600,text.getTypeface().getWeight());assertComplete(text);assertFalse(text.isClickable());assertFalse(text.isFocusable());assertReceipt(a,text);}
    static void assertReceipt(DemoShareEditorActivity a,TextView text){
        View receipt=a.stage.findViewWithTag("share-destination-material"),identity=a.stage.findViewWithTag("share-destination-identity");assertTrue(receipt instanceof LinearLayout);assertTrue(identity instanceof LinearLayout);
        assertSame(receipt,identity.getParent());assertSame(identity,text.getParent());assertSame(receipt,ShareMaterialDisclosureTest.group(a).getParent());assertFalse(receipt.isClickable());assertFalse(receipt.isFocusable());assertFalse(identity.isClickable());assertFalse(identity.isFocusable());
        int left=bounds(receipt).left+receipt.getPaddingLeft(),right=bounds(receipt).right-receipt.getPaddingRight();assertEquals(left,bounds(identity).left);assertEquals(right,bounds(identity).right);assertEquals(left,bounds(ShareMaterialDisclosureTest.summary(a)).left);
        LinearLayout row=(LinearLayout)identity;int textLeft=left;
        if(a.getResources().getConfiguration().fontScale>=1.5f){assertEquals("Larger text has no decorative initial or spacer",1,row.getChildCount());assertSame(text,row.getChildAt(0));}
        else{
            assertEquals(3,row.getChildCount());assertTrue(row.getChildAt(0) instanceof TextView);TextView tile=(TextView)row.getChildAt(0);View gap=row.getChildAt(1);assertSame(text,row.getChildAt(2));
            assertEquals(Ui.projectInitial(a.projectName()),tile.getText().toString());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,tile.getImportantForAccessibility());assertFalse(tile.isClickable());assertFalse(tile.isFocusable());assertEquals(left,bounds(tile).left);assertEquals(Ui.dp(a,32),tile.getWidth());assertEquals(Ui.dp(a,32),tile.getHeight());assertEquals(Ui.dp(a,12),gap.getWidth());textLeft+=tile.getWidth()+gap.getWidth();
        }
        assertEquals(textLeft,bounds(text).left);assertEquals(right,bounds(text).right);assertTrue("Complete destination stays before material in its confirmation group",bounds(text).bottom<=bounds(ShareMaterialDisclosureTest.header(a)).top);
    }
    static String[] fields(DemoShareEditorActivity a){return new String[]{a.selected,a.model,a.effort,a.shared,a.attachments.toString(),a.draft,a.note.getText().toString(),a.query,a.projectName()};}
    static void unchanged(DemoShareEditorActivity a,String[] fields,View focus){assertArrayEquals(fields,fields(a));assertSame(focus,a.getWindow().getDecorView().findFocus());guards(a);assertDestination(a);assertEquals("",a.draft);assertEquals("",a.note.getText().toString());assertFalse(a.panelOpen);assertFalse(a.materialOpen);assertEquals(1,a.step);}
    static void guards(DemoShareEditorActivity a){assertEquals(0,a.forbiddenActions);assertEquals(0,a.networkAttempts);assertEquals(0,a.saveAttempts);assertEquals(0,a.pairingAttempts);assertEquals(0,a.submitAttempts);assertEquals(0,a.startAttempts);assertEquals(0,a.checkpointAttempts);assertEquals(0,a.keyboardBypasses);assertNull(a.incoming);assertFalse(a.sent);assertFalse(a.busy);assertFalse(a.receiving);assertEquals(1,a.initializationCloses);}
    static void safe(DemoShareEditorActivity a,String language,String theme,float scale){guards(a);assertTrue(a.probeReady);assertTrue(a.direct);assertTrue(a.confirmed);assertEquals(language.equals("zh"),L.chinese());assertEquals(theme.equals("dark"),Ui.dark);assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertTrue(a.getIntent().getBooleanExtra("modelDisclosure",false));assertTrue(a.getIntent().getBooleanExtra("compactHeight",false));assertFalse(a.getIntent().getBooleanExtra("materialReview",false));assertFalse(a.store.paired());assertFalse(a.store.online());assertEquals("ui-probe-project",a.selected);assertEquals("probe-fast",a.model);assertEquals("medium",a.effort);assertEquals("https://example.invalid/ui-sample",a.shared);assertEquals(0,a.attachments.length());assertEquals("",a.draft);assertEquals("",a.note.getText().toString());assertEquals(1,a.step);assertNull(a.dialog);assertNull(a.discardDialog);assertFalse(a.panelOpen);assertFalse(a.materialOpen);TextView marker=a.sheet.findViewWithTag("hierarchy-marker");assertNotNull(marker);assertEquals(L.t("UI probe · memory only","界面探针 · 仅内存"),marker.getText().toString());assertComplete(marker);}
    JSONObject metadata(DemoShareEditorActivity a,String language,String theme,float scale){guards(a);guardSnapshots++;TextView text=destination(a),marker=a.sheet.findViewWithTag("hierarchy-marker");try{return new JSONObject().put("language",language).put("theme",theme).put("font_scale",scale).put("fixture",a.getClass().getSimpleName()).put("memory_only",true).put("marker",marker.getText().toString()).put("project_id",a.selected).put("project_label",a.projectName()).put("destination_text",text.getText().toString()).put("destination_bounds",rect(bounds(text))).put("destination_lines",text.getLineCount()).put("destination_text_size_px",text.getTextSize()).put("destination_color",text.getCurrentTextColor()).put("destination_weight",text.getTypeface().getWeight()).put("model",a.model).put("effort",a.effort).put("note",a.note.getText().toString()).put("draft",a.draft).put("shared",a.shared).put("attachments",a.attachments.length()).put("step",a.step).put("direct",a.direct).put("confirmed",a.confirmed).put("incoming_null",a.incoming==null).put("initialization_closes",a.initializationCloses).put("checkpoint_noops",a.checkpointNoops).put("destroyed",a.isDestroyed()).put("executor_terminated",a.io.isTerminated()).put("guards",new JSONObject().put("forbidden",a.forbiddenActions).put("network",a.networkAttempts).put("save",a.saveAttempts).put("pairing",a.pairingAttempts).put("submit",a.submitAttempts).put("start",a.startAttempts).put("checkpoint_persistence",a.checkpointAttempts).put("keyboard_bypasses",a.keyboardBypasses));}catch(Exception error){throw new AssertionError(error);}}
    static void assertComplete(TextView text){assertNotNull(text);Layout layout=text.getLayout();assertNotNull(layout);assertTrue(layout.getLineCount()>0);assertEquals(text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int n=0;n<layout.getLineCount();n++)assertEquals(0,layout.getEllipsisCount(n));assertTrue(text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());}
    static Rect localLine(TextView text,int line){assertComplete(text);Layout layout=text.getLayout();Rect result=new Rect(text.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(line)),text.getCompoundPaddingTop()+layout.getLineTop(line),text.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(line)),text.getCompoundPaddingTop()+layout.getLineBottom(line));assertTrue(new Rect(0,0,text.getWidth(),text.getHeight()).contains(result));return result;}
    static Rect screenLine(TextView text,int line){Rect result=localLine(text,line);int[] point=new int[2];text.getLocationOnScreen(point);result.offset(point[0],point[1]);return result;}
    static Rect bounds(View view){int[] point=new int[2];view.getLocationOnScreen(point);return new Rect(point[0],point[1],point[0]+view.getWidth(),point[1]+view.getHeight());}
    static JSONArray rect(Rect value){return new JSONArray().put(value.left).put(value.top).put(value.right).put(value.bottom);}
    static void assertSafeRect(DemoShareEditorActivity a,Rect target,boolean inScroll){Rect root=new Rect(),safe=new Rect();assertTrue(a.root.getGlobalVisibleRect(root));a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);assertTrue(safe.intersect(root));if(inScroll){Rect scroll=new Rect();assertTrue(a.scroll.getGlobalVisibleRect(scroll));assertTrue(safe.intersect(scroll));}assertTrue("Requested complete rectangle is visible in actual root, scroll and window",safe.contains(target));}
    static void frame(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{CountDownLatch next=new CountDownLatch(1);scenario.onActivity(a->{a.root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(a.hasWindowFocus()&&a.holder.getWidth()>0&&a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f&&!a.stage.isLayoutRequested()){a.root.getViewTreeObserver().removeOnPreDrawListener(this);next.countDown();}else a.root.postInvalidateOnAnimation();return true;}});a.root.invalidate();});assertTrue("Bounded wait for settled real native frame",next.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,File directory,String nonce,String identity,String language,String theme)throws Exception{
        scenario.onActivity(a->a.scroll.scrollTo(0,0));frame(scenario);frame(scenario);long settled=SystemClock.elapsedRealtime();Thread.sleep(2000);frame(scenario);JSONObject[] data={null};
        scenario.onActivity(a->{safe(a,language,theme,1f);assertDestination(a);assertSafeRect(a,bounds(destination(a)),true);TextView marker=a.sheet.findViewWithTag("hierarchy-marker");assertSafeRect(a,bounds(marker),false);WindowInsets insets=a.getWindow().getDecorView().getRootWindowInsets();assertNotNull(insets);assertFalse(insets.isVisible(WindowInsets.Type.ime()));assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertTrue(SystemClock.elapsedRealtime()-settled>=2000);
            data[0]=extend(metadata(a,language,theme,1f),"nonce",nonce,"phase",PHASE,"identity",identity,"capture_scope","Native memory-only editor destination and blank note; not a complete handoff or signed-package capture","ui_automation_screenshot",true,"required_predraws_completed",2,"settled_at_elapsed_ms",settled,"capture_at_elapsed_ms",SystemClock.elapsedRealtime(),"business_actions","not_invoked_by_test","system_bar_pixels_require_visual_review",true);});
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String name="share-destination-"+language+"-"+theme+"-font1";File png=new File(directory,name+".png"),json=new File(directory,name+".json");try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(OutputStream out=Files.newOutputStream(png.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(OutputStream out=Files.newOutputStream(json.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
        event(nonce,identity,"capture-written",data[0].put("png",png.getName()).put("json",json.getName()).put("png_bytes",png.length()).put("json_bytes",json.length()));
    }
    static JSONObject extend(JSONObject data,Object... fields){try{for(int n=0;n<fields.length;n+=2)data.put((String)fields[n],fields[n+1]);return data;}catch(Exception error){throw new AssertionError(error);}}
    static void event(String nonce,String identity,String name,JSONObject data){Bundle status=new Bundle();status.putString("stream","SHARE_DESTINATION_READING_EVENT\t"+nonce+"\t"+PHASE+"\t"+identity+"\t"+name+"\t"+data+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
