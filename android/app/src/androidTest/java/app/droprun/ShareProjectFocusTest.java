package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
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
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Two explicit memory windows. Rebuild only; no read request, business click or input. */
public class ShareProjectFocusTest {
    static boolean unresolvedLifetime;
    static final String SECOND="memory-second-project";
    static final String SCOPE="Two-project font1 memory selector after a local row rebuild; no read request, input or business click";
    int guardSnapshots;

    @Test public void projectRebuildKeepsKeyboardDestination()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String phase=args.getString("shareProjectFocusProbe","");if(phase.isEmpty())return;assertTrue(phase.equals("baseline")||phase.equals("accepted"));
        String nonce=args.getString("evidenceNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);assertTrue(Build.VERSION.SDK_INT>=30);assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);assertEquals(0f,DemoShareEditorActivity.hierarchyFontScale,0f);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoShareEditorActivity.class),0).exported);
        File base=target.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/share-project-focus-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());
        String oldLanguage=L.chinese()?"zh":"en";boolean oldDark=Ui.dark;int[] oldPalette=HistoryShareStateTest.palette();float oldFont=DemoShareEditorActivity.hierarchyFontScale;
        int entered=0,completed=0,closed=0,captured=0,rebuilds=0,preserved=0,touchRestores=0;boolean destroyed=true;
        try{for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){
            String language=config[0],theme=config[1],identity=language+"-"+theme;assertTrue(destroyed);assertFalse(unresolvedLifetime);DemoShareEditorActivity.hierarchyFontScale=1f;
            ActivityScenario<DemoShareEditorActivity> scenario=null;DemoShareEditorActivity[] retained={null};Boolean[] priorTouch={null};boolean touchAttempted=false;Throwable failure=null;String[][] fields={null};View[] oldRow={null},search={null},refresh={null};boolean[] kept={false};
            try{
                Intent intent=new Intent(target,DemoShareEditorActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("modelDisclosure",true).putExtra("compactHeight",true).putExtra("direct",true).putExtra("confirmed",true);
                destroyed=false;unresolvedLifetime=true;event(nonce,phase,identity,"launch-attempt",new JSONObject());scenario=ActivityScenario.launch(intent);
                scenario.onActivity(a->{retained[0]=a;priorTouch[0]=a.root.isInTouchMode();event(nonce,phase,identity,"returned-handle",data("prior_touch",priorTouch[0]));});frame(scenario);
                scenario.onActivity(a->{
                    ShareDestinationReadingTest.guards(a);assertEquals(1,a.store.projects().length());
                    try{a.store.projects().put(new JSONObject().put("id",SECOND).put("name",L.t("Another UI sample","另一个界面示例")).put("available",true).put("permission",new JSONObject().put("enabled",true)));}catch(Exception error){throw new AssertionError(error);}
                    a.query=L.t("sample","示例");a.step=0;a.stage.removeAllViews();a.stage.addView(a.stepProject(),new ViewGroup.LayoutParams(-1,-2));a.dots.setActive(0,false);a.back.setVisibility(View.INVISIBLE);a.search.setShowSoftInputOnFocus(false);a.scroll.scrollTo(0,0);
                });frame(scenario);
                scenario.onActivity(a->{safe(a,language,theme,phase);fields[0]=fields(a);search[0]=a.search;refresh[0]=a.projectRefresh;event(nonce,phase,identity,"entered",metadata(a,language,theme));});entered++;
                touchAttempted=true;InstrumentationRegistry.getInstrumentation().setInTouchMode(false);frame(scenario);
                scenario.onActivity(a->{unchanged(a,language,theme,phase,fields[0]);oldRow[0]=second(a);assertTrue(oldRow[0].requestFocus());assertFalse(oldRow[0].isInTouchMode());assertTrue(oldRow[0].hasFocus());});frame(scenario);
                scenario.onActivity(a->{unchanged(a,language,theme,phase,fields[0]);assertSame(oldRow[0],a.projectList.findFocus());event(nonce,phase,identity,"row-focused",metadata(a,language,theme));a.renderProjects();});frame(scenario);
                scenario.onActivity(a->{unchanged(a,language,theme,phase,fields[0]);View replacement=second(a);assertNotSame(oldRow[0],replacement);assertNull(oldRow[0].getParent());kept[0]=replacement.hasFocus();if(phase.equals("accepted")){assertTrue("Same full project ID keeps focus after row replacement",kept[0]);assertSame(replacement,a.projectList.findFocus());assertWhole(a,replacement);}event(nonce,phase,identity,"rows-rebuilt",extend(metadata(a,language,theme),"old_row_detached",oldRow[0].getParent()==null,"same_id_focus",kept[0]));});rebuilds++;if(kept[0])preserved++;
                capture(scenario,directory,nonce,phase,identity,language,theme,fields[0]);captured++;
                scenario.onActivity(a->{assertSame(search[0],a.search);assertFalse(a.search.getShowSoftInputOnFocus());assertTrue(a.search.requestFocus());assertSame(a.search,a.root.findFocus());a.renderProjects();});frame(scenario);
                scenario.onActivity(a->{unchanged(a,language,theme,phase,fields[0]);assertSame(search[0],a.search);assertSame(a.search,a.root.findFocus());event(nonce,phase,identity,"search-focus-kept",metadata(a,language,theme));assertSame(refresh[0],a.projectRefresh);assertTrue(a.projectRefresh.requestFocus());assertSame(a.projectRefresh,a.root.findFocus());a.renderProjects();});frame(scenario);
                scenario.onActivity(a->{unchanged(a,language,theme,phase,fields[0]);assertSame(refresh[0],a.projectRefresh);assertSame(a.projectRefresh,a.root.findFocus());event(nonce,phase,identity,"refresh-focus-kept",metadata(a,language,theme));event(nonce,phase,identity,"full-completed",metadata(a,language,theme));});completed++;
            }catch(Throwable error){failure=error;throw error;}
            finally{if(scenario!=null)try{
                scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);assertTrue(retained[0].io.awaitTermination(3,TimeUnit.SECONDS));DemoShareEditorActivity a=retained[0];
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{ShareDestinationReadingTest.guards(a);assertTrue(a.isDestroyed());assertTrue(a.io.isTerminated());guardSnapshots++;event(nonce,phase,identity,"DESTROYED",ShareDestinationReadingTest.identityClosed(a));});closed++;
                assertNotNull(priorTouch[0]);if(touchAttempted){InstrumentationRegistry.getInstrumentation().setInTouchMode(priorTouch[0]);touchRestores++;event(nonce,phase,identity,"touch-mode-restored",data("prior_touch",priorTouch[0],"setInTouchMode_returned",true));}
                destroyed=true;unresolvedLifetime=false;
            }catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(destroyed&&!unresolvedLifetime){DemoShareEditorActivity.hierarchyFontScale=oldFont;HistoryShareStateTest.restore(oldLanguage,oldDark,oldPalette);assertEquals(oldFont,DemoShareEditorActivity.hierarchyFontScale,0f);assertEquals(oldLanguage,L.chinese()?"zh":"en");assertEquals(oldDark,Ui.dark);assertArrayEquals(oldPalette,HistoryShareStateTest.palette());event(nonce,phase,"all","statics-restored",data("font_scale",DemoShareEditorActivity.hierarchyFontScale,"language",L.chinese()?"zh":"en","dark",Ui.dark,"palette_equal",java.util.Arrays.equals(oldPalette,HistoryShareStateTest.palette())));}}
        assertEquals(2,entered);assertEquals(2,completed);assertEquals(2,closed);assertEquals(2,captured);assertEquals(2,rebuilds);assertEquals(2,touchRestores);assertFalse(unresolvedLifetime);if(phase.equals("accepted"))assertEquals(2,preserved);
        Bundle summary=new Bundle();summary.putString("stream","SHARE_PROJECT_FOCUS_SUMMARY\t"+nonce+"\t"+phase+"\t"+data("entered",entered,"full_completed",completed,"DESTROYED",closed,"captures",captured,"focused_row_rebuilds",rebuilds,"same_id_focus_observed",preserved,"touch_mode_restore_returns",touchRestores,"guard_snapshots",guardSnapshots,"statics_restored",true,"business_actions","not_invoked_by_test","scope",SCOPE)+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,summary);
    }

    static View second(DemoShareEditorActivity a){String expected=L.t("Another UI sample, allowed","另一个界面示例，已授权");View found=null;for(int n=0;n<a.projectList.getChildCount();n++){View child=a.projectList.getChildAt(n);if(expected.equals(String.valueOf(child.getContentDescription()))){assertNull(found);found=child;}}assertNotNull(found);return found;}
    static void safe(DemoShareEditorActivity a,String language,String theme,String phase){
        ShareDestinationReadingTest.guards(a);assertTrue(a.probeReady);assertFalse(TaskSyncService.running);assertTrue(a.direct);assertTrue(a.confirmed);assertEquals(language.equals("zh"),L.chinese());assertEquals(theme.equals("dark"),Ui.dark);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertTrue(a.getIntent().getBooleanExtra("modelDisclosure",false));assertTrue(a.getIntent().getBooleanExtra("compactHeight",false));assertFalse(a.getIntent().getBooleanExtra("materialReview",false));assertFalse(a.store.paired());assertFalse(a.store.online());assertEquals(0,a.step);assertNull(a.projectRead);assertFalse(a.projectReadFailed);assertNull(a.dialog);assertNull(a.discardDialog);assertFalse(a.panelOpen);assertFalse(a.materialOpen);assertEquals(2,a.store.projects().length());assertEquals(L.t("sample","示例"),a.query);assertEquals(a.query,a.search.getText().toString());assertEquals(View.VISIBLE,a.search.getVisibility());assertFalse(a.search.getShowSoftInputOnFocus());assertEquals(View.INVISIBLE,a.back.getVisibility());assertTrue(a.projectRefresh.isEnabled());
        TextView marker=a.sheet.findViewWithTag("hierarchy-marker");assertNotNull(marker);assertEquals(L.t("UI probe · memory only","界面探针 · 仅内存"),marker.getText().toString());ShareDestinationReadingTest.assertComplete(marker);
        int rows=0;for(int n=0;n<a.projectList.getChildCount();n++){View row=a.projectList.getChildAt(n);if(!row.isClickable())continue;String id=rows==0?"ui-probe-project":SECOND;assertEquals(phase.equals("accepted")?"project:"+id:null,row.getTag());assertTrue(row.isFocusable());assertTrue(row.isEnabled());assertTrue(row.getMinimumHeight()>=a.dp(48));rows++;}assertEquals(2,rows);
        WindowInsets insets=a.getWindow().getDecorView().getRootWindowInsets();assertNotNull(insets);assertFalse("No IME in this local focus diagnostic",insets.isVisible(WindowInsets.Type.ime()));
    }
    static String[] fields(DemoShareEditorActivity a){return new String[]{a.selected,a.model,a.effort,a.shared,a.attachments.toString(),a.draft,a.note.getText().toString(),a.query,a.store.projects().toString(),""+a.showAll,""+a.materialOpen,""+a.step};}
    static void unchanged(DemoShareEditorActivity a,String language,String theme,String phase,String[] expected){safe(a,language,theme,phase);assertArrayEquals(expected,fields(a));}
    static void frame(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{ShareDestinationReadingTest.frame(scenario);}
    static void assertWhole(DemoShareEditorActivity a,View view){Rect bounds=ShareDestinationReadingTest.bounds(view),visible=new Rect();assertTrue(view.getGlobalVisibleRect(visible));assertEquals(bounds,visible);ShareDestinationReadingTest.assertSafeRect(a,bounds,true);}
    JSONObject metadata(DemoShareEditorActivity a,String language,String theme){
        ShareDestinationReadingTest.guards(a);guardSnapshots++;View row=second(a),focus=a.root.findFocus();String focused=focus==row?"second-project":focus==a.search?"search":focus==a.projectRefresh?"refresh":focus==null?"none":"other";Rect safe=new Rect(),root=new Rect(),viewport=new Rect();a.root.getGlobalVisibleRect(root);a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);assertTrue(safe.intersect(root));assertTrue(a.scroll.getGlobalVisibleRect(viewport));assertTrue(safe.intersect(viewport));Rect bounds=ShareDestinationReadingTest.bounds(row);
        return data("language",language,"theme",theme,"font_scale",a.getResources().getConfiguration().fontScale,"fixture",a.getClass().getSimpleName(),"memory_only",true,"marker",((TextView)a.sheet.findViewWithTag("hierarchy-marker")).getText().toString(),"fields",new JSONArray(java.util.Arrays.asList(fields(a))),"project_count",a.store.projects().length(),"focus_role",focused,"focus_tag",focus==null||focus.getTag()==null?JSONObject.NULL:focus.getTag().toString(),"second_tag",row.getTag()==null?JSONObject.NULL:row.getTag().toString(),"second_focused",row.hasFocus(),"second_bounds",ShareDestinationReadingTest.rect(bounds),"safe_viewport",ShareDestinationReadingTest.rect(safe),"whole_second_row",safe.contains(bounds),"touch_mode",row.isInTouchMode(),"scroll_y",a.scroll.getScrollY(),"incoming_null",a.incoming==null,"destroyed",a.isDestroyed(),"executor_terminated",a.io.isTerminated(),"initialization_closes",a.initializationCloses,"checkpoint_noops",a.checkpointNoops,"guards",data("forbidden",a.forbiddenActions,"network",a.networkAttempts,"save",a.saveAttempts,"pairing",a.pairingAttempts,"submit",a.submitAttempts,"start",a.startAttempts,"checkpoint_persistence",a.checkpointAttempts,"keyboard_bypasses",a.keyboardBypasses));
    }
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,File directory,String nonce,String phase,String identity,String language,String theme,String[] expected)throws Exception{
        frame(scenario);frame(scenario);long settled=SystemClock.elapsedRealtime();Thread.sleep(2000);frame(scenario);JSONObject[] receipt={null};
        scenario.onActivity(a->{unchanged(a,language,theme,phase,expected);TextView marker=a.sheet.findViewWithTag("hierarchy-marker");ShareDestinationReadingTest.assertSafeRect(a,ShareDestinationReadingTest.bounds(marker),false);assertEquals(1f,a.getWindow().getAttributes().alpha,0f);if(phase.equals("accepted")){assertTrue(second(a).hasFocus());assertWhole(a,second(a));}assertTrue(SystemClock.elapsedRealtime()-settled>=2000);receipt[0]=extend(metadata(a,language,theme),"nonce",nonce,"phase",phase,"identity",identity,"capture_scope",SCOPE,"capture_accepted",false,"ui_automation_screenshot",true,"required_predraws_completed",2,"settled_at_elapsed_ms",settled,"capture_at_elapsed_ms",SystemClock.elapsedRealtime(),"business_actions","not_invoked_by_test","system_bar_pixels_require_visual_review",true);});
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String stem="share-project-focus-"+identity+"-font1";File png=new File(directory,stem+".png"),json=new File(directory,stem+".json");try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(OutputStream out=Files.newOutputStream(png.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(OutputStream out=Files.newOutputStream(json.toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(receipt[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
        event(nonce,phase,identity,"capture-written",extend(receipt[0],"png",png.getName(),"json",json.getName(),"png_bytes",png.length(),"json_bytes",json.length()));
    }
    static JSONObject data(Object... values){return extend(new JSONObject(),values);}
    static JSONObject extend(JSONObject value,Object... values){return ShareDestinationReadingTest.extend(value,values);}
    static void event(String nonce,String phase,String identity,String name,JSONObject value){Bundle status=new Bundle();status.putString("stream","SHARE_PROJECT_FOCUS_EVENT\t"+nonce+"\t"+phase+"\t"+identity+"\t"+name+"\t"+value+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
}
