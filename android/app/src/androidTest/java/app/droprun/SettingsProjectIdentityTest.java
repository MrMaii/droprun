package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.UUID;
import org.json.*;
import org.junit.Test;
import static app.droprun.SettingsReadingPresentationTest.*;
import static app.droprun.HistoryShareStateTest.palette;
import static app.droprun.HistoryShareStateTest.restore;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareEditorTest.findText;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static org.junit.Assert.*;

/** Four opt-in memory windows. No permission/model/Refresh/navigation click; optional two normal captures. */
public class SettingsProjectIdentityTest {
    @Test public void projectIdentitySeparatesNameFromIdWithoutChangingAccess()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String phase=args.getString("settingsProjectIdentityProbe","");if(phase.isEmpty())return;assertEquals("accepted",phase);
        String nonce=args.getString("settingsProjectIdentityNonce","");assertEquals(UUID.fromString(nonce).toString(),nonce);String captureMode=args.getString("captureIdentity","");assertTrue(captureMode.isEmpty()||captureMode.equals("accepted"));
        assertTrue(Build.VERSION.SDK_INT>=30);assertFalse(unresolvedLifetime);assertNull(DemoSettingsRecreationActivity.finishBeforeAttach);assertFalse(TaskSyncService.running);
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));assertFalse(target.getPackageManager().getActivityInfo(new ComponentName(target,DemoSettingsRecreationActivity.class),0).exported);
        File directory=null;if(!captureMode.isEmpty()){File base=target.getExternalFilesDir(null);assertNotNull(base);directory=new File(base,"ui-probe-evidence/settings-project-identity-"+nonce);assertFalse(directory.exists());assertTrue(directory.mkdirs());}
        float priorFont=DemoSettingsRecreationActivity.hierarchyFontScale;String priorLanguage=L.chinese()?"zh":"en";boolean priorDark=Ui.dark,destroyed=true;int[] priorPalette=palette();int entered=0,completed=0,closed=0,captured=0,visibleLines=0;
        try{for(String[] config:new String[][]{{"en","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
            String identity="project-identity|"+nonce+"|"+String.join("|",config);float scale=Float.parseFloat(config[2]);assertTrue(destroyed);assertFalse(unresolvedLifetime);DemoSettingsRecreationActivity.hierarchyFontScale=scale;
            ActivityScenario<DemoSettingsRecreationActivity> scenario=null;DemoSettingsRecreationActivity[] retained={null};Boolean[] priorTouch={null};boolean touchChanged=false;Throwable failure=null;
            try{
                destroyed=false;unresolvedLifetime=true;event(identity,"launch-attempt");scenario=ActivityScenario.launch(new Intent(target,DemoSettingsRecreationActivity.class).putExtra("language",config[0]).putExtra("appearance",config[1]).putExtra("modelHierarchy",true));event(identity,"returned-handle");
                scenario.onActivity(a->{retained[0]=a;priorTouch[0]=a.body.isInTouchMode();fields(a);assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(config[0].equals("zh"),L.chinese());assertEquals(config[1].equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);a.showAccess=true;seed(a);});frame(scenario);ready(scenario);entered++;event(identity,"entered");
                for(String id:IDS){
                    int[] lines={0};scenario.onActivity(a->{checkIdentity(a,id,scale);lines[0]=projectName(a,id).getLineCount();});
                    for(int index=0;index<lines[0];index++){final int n=index;scenario.onActivity(a->{TextView name=projectName(a,id);name.requestRectangleOnScreen(lineRect(name,n),true);});frame(scenario);ready(scenario);scenario.onActivity(a->{assertLineVisible(a,projectName(a,id),n);fields(a);event(identity,"line-visible id="+id+" line="+n);});visibleLines++;}
                    scenario.onActivity(a->{TextView chip=projectChip(a,id);chip.requestRectangleOnScreen(new Rect(0,0,chip.getWidth(),chip.getHeight()),true);});frame(scenario);ready(scenario);scenario.onActivity(a->{SettingsRecreationTest.assertCompleteAndVisible(a,projectChip(a,id));fields(a);});
                }
                if(scale==1f&&directory!=null){capture(scenario,directory,nonce,phase,config[0],config[1]);captured++;}
                touchChanged=true;InstrumentationRegistry.getInstrumentation().setInTouchMode(false);View[] previous={null};
                scenario.onActivity(a->{TextView chip=projectChip(a,IDS[1]);assertFalse(chip.isInTouchMode());assertTrue(chip.requestFocus());assertSame(chip,a.getCurrentFocus());previous[0]=chip;a.render();});frame(scenario);ready(scenario);
                scenario.onActivity(a->{TextView chip=projectChip(a,IDS[1]);assertNotSame(previous[0],chip);assertSame(chip,a.getCurrentFocus());assertEquals("project-access:"+IDS[1],chip.getTag());SettingsRecreationTest.assertCompleteAndVisible(a,chip);fields(a);guards(identity,"focus-restored-same-id",a);});
                uniqueControl(scenario,identity);collisionControl(scenario,identity);scenario.onActivity(a->{for(String id:IDS)checkIdentity(a,id,scale);fields(a);guards(identity,"before-close",a);});completed++;event(identity,"full-completed");
            }catch(Throwable error){failure=error;event(identity,"failed:"+error.getClass().getSimpleName());throw error;}
            finally{if(scenario!=null)try{scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertNotNull(retained[0]);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue(retained[0].isDestroyed());assertTrue(retained[0].io.isShutdown());destroyedFields(retained[0]);guards(identity,"after-destroyed",retained[0]);});event(identity,"DESTROYED");if(touchChanged){assertNotNull(priorTouch[0]);InstrumentationRegistry.getInstrumentation().setInTouchMode(priorTouch[0]);event(identity,"touch-mode-restored");}destroyed=true;unresolvedLifetime=false;closed++;}catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}}
        }}finally{if(destroyed&&!unresolvedLifetime){DemoSettingsRecreationActivity.hierarchyFontScale=priorFont;restore(priorLanguage,priorDark,priorPalette);assertEquals(priorFont,DemoSettingsRecreationActivity.hierarchyFontScale,0f);assertEquals(priorLanguage,L.chinese()?"zh":"en");assertEquals(priorDark,Ui.dark);assertArrayEquals(priorPalette,palette());event("project-identity|"+nonce,"font-language-dark-palette-restored");}}
        assertEquals(4,entered);assertEquals(4,completed);assertEquals(4,closed);assertEquals(directory==null?0:2,captured);assertFalse(unresolvedLifetime);event("project-identity|"+nonce,"SUMMARY entered="+entered+" completed="+completed+" DESTROYED="+closed+" captures="+captured+" individually_visible_lines="+visibleLines+" business_actions=not_invoked_by_test");
    }
    static void fields(DemoSettingsRecreationActivity a){safe(a);assertNull(a.work);assertNull(a.modeChange);assertNull(a.shownModeResult);assertFalse(a.busy);assertFalse(a.refreshing);assertTrue(a.switching.isEmpty());assertFalse(TaskSyncService.running);}
    static void destroyedFields(DemoSettingsRecreationActivity a){assertEquals(0,a.forbiddenActions.get());assertEquals(0,a.workFactories);assertEquals(0,a.modelSaves);assertNull(a.work);assertNull(a.modeChange);assertNull(a.shownModeResult);assertFalse(a.busy);assertFalse(a.refreshing);assertTrue(a.switching.isEmpty());assertFalse(TaskSyncService.running);}
    static String permissionSuffix(boolean enabled){return enabled?L.t(", allowed, tap to revoke access","，已允许，点按停止转发"):L.t(", not allowed, tap to allow","，未允许，点按允许");}
    static void checkIdentity(DemoSettingsRecreationActivity a,String id,float scale){
        fields(a);TextView name=projectName(a,id),chip=projectChip(a,id);String label=a.store.projectLabel(id,sampleName()),expected=sampleName()+"\n"+id.substring(0,8);int start=sampleName().length()+1;
        assertEquals(sampleName()+" · "+id.substring(0,8),label);assertEquals(expected,name.getText().toString());assertEquals(label,String.valueOf(name.getContentDescription()));assertEquals(label+permissionSuffix(id.equals(IDS[0])),String.valueOf(chip.getContentDescription()));assertCompleteText(name);
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,15,a.getResources().getDisplayMetrics()),name.getTextSize(),.01f);assertEquals(600,name.getTypeface().getWeight());assertFalse(name.getTypeface().isItalic());assertEquals(Ui.TEXT,name.getCurrentTextColor());assertTrue(name.getText() instanceof Spanned);
        Spanned text=(Spanned)name.getText();AbsoluteSizeSpan[] sizes=text.getSpans(start,text.length(),AbsoluteSizeSpan.class);ForegroundColorSpan[] colors=text.getSpans(start,text.length(),ForegroundColorSpan.class);assertEquals(1,sizes.length);assertEquals(1,colors.length);
        for(Object span:new Object[]{sizes[0],colors[0]}){assertEquals(start,text.getSpanStart(span));assertEquals(text.length(),text.getSpanEnd(span));}assertEquals(0,text.getSpans(0,start-1,AbsoluteSizeSpan.class).length);assertEquals(0,text.getSpans(0,start-1,ForegroundColorSpan.class).length);
        int pixels=Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,12,a.getResources().getDisplayMetrics()));assertFalse(sizes[0].getDip());assertEquals(pixels,sizes[0].getSize());assertEquals(Ui.MUTED,colors[0].getForegroundColor());TextPaint paint=new TextPaint(name.getPaint());sizes[0].updateMeasureState(paint);colors[0].updateDrawState(paint);assertEquals((float)pixels,paint.getTextSize(),0f);assertEquals(Ui.MUTED,paint.getColor());
        Layout layout=name.getLayout();int idLine=layout.getLineForOffset(start);assertEquals(start,layout.getLineStart(idLine));assertEquals(idLine,layout.getLineForOffset(text.length()-1));assertEquals("project-access:"+id,chip.getTag());assertTrue(chip.isEnabled());assertTrue(chip.isClickable());assertTrue(chip.isFocusable());assertTrue(chip.getWidth()>=Ui.dp(a,48));assertTrue(chip.getHeight()>=Ui.dp(a,48));
        if(scale==2f){assertEquals(Ui.dp(a,248),name.getWidth());assertSame(name.getParent(),chip.getParent());assertEquals(LinearLayout.VERTICAL,((LinearLayout)name.getParent()).getOrientation());assertEquals(name.getLeft(),chip.getLeft());assertTrue(chip.getTop()>=name.getBottom()+Ui.dp(a,8));}
    }
    static Rect lineRect(TextView name,int index){assertCompleteText(name);Layout layout=name.getLayout();return new Rect(name.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(index)),name.getCompoundPaddingTop()+layout.getLineTop(index),name.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(index)),name.getCompoundPaddingTop()+layout.getLineBottom(index));}
    static void assertLineVisible(DemoSettingsRecreationActivity a,TextView name,int index){Rect local=new Rect(),line=lineRect(name,index);assertTrue(name.getLocalVisibleRect(local));assertTrue(local.contains(line));int[] at=new int[2];name.getLocationOnScreen(at);line.offset(at[0],at[1]);View scroll=a.findViewById(R.id.settings_scroll);Rect viewport=bounds(scroll),safeScreen=new Rect();viewport.left+=scroll.getPaddingLeft();viewport.top+=scroll.getPaddingTop();viewport.right-=scroll.getPaddingRight();viewport.bottom-=scroll.getPaddingBottom();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safeScreen);assertTrue(safeScreen.intersect(viewport));assertTrue(safeScreen.contains(line));}
    static void uniqueControl(ActivityScenario<DemoSettingsRecreationActivity> scenario,String identity)throws Throwable{
        Object[] original={null};boolean[] changed={false};Throwable failure=null;scenario.onActivity(a->{fields(a);original[0]=a.cached.get("projects");assertTrue(original[0] instanceof String);});
        try{scenario.onActivity(a->{try{JSONObject data=new JSONObject((String)original[0]);JSONArray projects=data.getJSONArray("projects");assertEquals(2,projects.length());for(int n=0;n<2;n++)assertEquals(IDS[n],projects.getJSONObject(n).getString("id"));projects.getJSONObject(1).put("name",sampleName()+L.t(" · unique memory note"," · 唯一内存便笺"));changed[0]=true;a.cached.put("projects",data.toString());a.render();}catch(JSONException error){throw new AssertionError(error);}});frame(scenario);ready(scenario);
            scenario.onActivity(a->{for(int n=0;n<2;n++){JSONObject project=a.store.projects().optJSONObject(n);String name=project.optString("name");assertEquals(n==0?sampleName():sampleName()+L.t(" · unique memory note"," · 唯一内存便笺"),name);assertEquals(name,a.store.projectLabel(IDS[n],name));TextView title=findText(a.body,name);assertNotNull(title);assertEquals(name,title.getText().toString());assertEquals(name,String.valueOf(title.getContentDescription()));assertFalse(title.getText().toString().contains("\n"));if(title.getText() instanceof Spanned)assertEquals(0,((Spanned)title.getText()).getSpans(0,title.length(),AbsoluteSizeSpan.class).length);TextView chip=a.body.findViewWithTag("project-access:"+IDS[n]);assertNotNull(chip);assertEquals(name+permissionSuffix(n==0),String.valueOf(chip.getContentDescription()));assertEquals(n==0,Store.projectEnabled(project));}fields(a);guards(identity,"unique-name-positive-control",a);});
        }catch(Throwable error){failure=error;throw error;}finally{if(changed[0])try{scenario.onActivity(a->{a.cached.put("projects",original[0]);a.render();assertSame(original[0],a.cached.get("projects"));fields(a);});frame(scenario);ready(scenario);event(identity,"duplicate-cache-restored");}catch(Throwable restoreError){if(failure!=null)failure.addSuppressed(restoreError);else throw restoreError;}}
    }
    static void collisionControl(ActivityScenario<DemoSettingsRecreationActivity> scenario,String identity)throws Throwable{
        String[] ids={"memory-collision-a-tail","memory-collision-b-tail"},prefixes={"memory-collision-a","memory-collision-b"};Object[] original={null};boolean[] changed={false};Throwable failure=null;scenario.onActivity(a->{fields(a);original[0]=a.cached.get("projects");assertTrue(original[0] instanceof String);});
        try{scenario.onActivity(a->{try{JSONObject data=new JSONObject((String)original[0]);JSONArray projects=data.getJSONArray("projects");assertEquals(2,projects.length());for(int n=0;n<2;n++){assertEquals(IDS[n],projects.getJSONObject(n).getString("id"));projects.getJSONObject(n).put("id",ids[n]).put("name",sampleName()+L.t(" · memory archive"," · 内存归档"));}changed[0]=true;a.cached.put("projects",data.toString());a.render();}catch(JSONException error){throw new AssertionError(error);}});frame(scenario);ready(scenario);
            scenario.onActivity(a->{for(int n=0;n<2;n++){JSONObject project=a.store.projects().optJSONObject(n);String name=sampleName()+L.t(" · memory archive"," · 内存归档"),label=name+" · "+prefixes[n],display=name+"\n"+prefixes[n];assertEquals(ids[n],project.optString("id"));assertEquals(name,project.optString("name"));assertTrue(prefixes[n].length()>8);assertTrue(ids[n].startsWith(prefixes[n]));assertEquals(ids[0].substring(0,8),ids[1].substring(0,8));assertEquals(label,a.store.projectLabel(ids[n],name));TextView title=findText(a.body,display);assertNotNull(title);assertEquals(display,title.getText().toString());assertEquals(label,String.valueOf(title.getContentDescription()));assertCompleteText(title);assertTrue(title.getText() instanceof Spanned);Spanned text=(Spanned)title.getText();AbsoluteSizeSpan[] spans=text.getSpans(0,text.length(),AbsoluteSizeSpan.class);assertEquals(1,spans.length);assertEquals(name.length()+1,text.getSpanStart(spans[0]));assertEquals(text.length(),text.getSpanEnd(spans[0]));TextView chip=a.body.findViewWithTag("project-access:"+ids[n]);assertNotNull(chip);assertEquals(label+permissionSuffix(n==0),String.valueOf(chip.getContentDescription()));assertEquals(n==0,Store.projectEnabled(project));}fields(a);guards(identity,"colliding-prefix-positive-control",a);});
        }catch(Throwable error){failure=error;throw error;}finally{if(changed[0])try{scenario.onActivity(a->{a.cached.put("projects",original[0]);a.render();assertSame(original[0],a.cached.get("projects"));fields(a);});frame(scenario);ready(scenario);event(identity,"duplicate-cache-restored");}catch(Throwable restoreError){if(failure!=null)failure.addSuppressed(restoreError);else throw restoreError;}}
    }
}
