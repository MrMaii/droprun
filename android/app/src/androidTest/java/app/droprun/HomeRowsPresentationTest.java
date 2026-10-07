package app.droprun;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
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
import static app.droprun.HomeRecoveryFeedbackTest.*;
import static app.droprun.HomeStatusFooterTest.*;
import static app.droprun.HomeProjectHierarchyTest.bounds;
import static org.junit.Assert.*;

/** Six opt-in memory windows. Read/scroll/rebind only; no click, sync, task or navigation. */
public class HomeRowsPresentationTest {
    @Test public void continuousRowsKeepIdentityAndReadPosition()throws Throwable{
        Bundle args=InstrumentationRegistry.getArguments();String mode=args.getString("homeRowsPresentationProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String capture=args.getString("captureHomeRowsPresentation","");assertTrue(capture.isEmpty()||capture.equals("accepted"));String nonce=args.getString("homeRowsPresentationNonce","");
        if(!capture.isEmpty())assertEquals(UUID.fromString(nonce).toString(),nonce);else assertTrue(nonce.isEmpty());assertTrue(Build.VERSION.SDK_INT>=30);assertFalse("Stop after any unresolved probe lifetime",unresolvedLifetime);
        File directory=null;if(!capture.isEmpty()){Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(target.getPackageName().endsWith(".debug"));File base=target.getExternalFilesDir(null);assertNotNull(base);directory=new File(base,"ui-probe-evidence/home-rows-presentation-"+nonce);assertFalse("Do not overwrite an earlier take",directory.exists());assertTrue(directory.mkdirs());}
        float previous=DemoHomeRecoveryActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean[] destroyed={true};int entered=0,completed=0,closed=0,captured=0,rebound=0;
        try{
            for(String[] config:new String[][]{{"en","light","1"},{"en","dark","1"},{"zh","light","1"},{"zh","dark","1"},{"en","light","2"},{"zh","dark","2"}}){
                String language=config[0],theme=config[1],identity="rows|"+language+"|"+theme+"|font"+config[2];float scale=Float.parseFloat(config[2]);DemoHomeRecoveryActivity.hierarchyFontScale=scale;
                ActivityScenario<DemoHomeRecoveryActivity> scenario=null;Throwable failure=null;DemoHomeRecoveryActivity[] retained={null};
                try{
                    scenario=launchProbe(language,theme,destroyed,identity);ready(scenario);
                    scenario.onActivity(a->{safe(a);retained[0]=a;assertProjects(a);assertEquals(scale,a.getResources().getConfiguration().fontScale,0f);assertEquals(theme.equals("dark"),Ui.dark);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);});entered++;lifecycle(identity,"entered");
                    for(int position=0;position<3;position++){
                        final int row=position;View[] outer={null};MainActivity.HomeHolder[] holder={null};String[] id={null},name={null},counts={null};long[] stable={0};
                        scenario.onActivity(a->a.list.setSelectionFromTop(row,0));awaitLayout(scenario);
                        scenario.onActivity(a->{JSONObject project=a.items.get(row);outer[0]=rowById(a,project.optString("id"));holder[0]=(MainActivity.HomeHolder)outer[0].getTag();id[0]=holder[0].id;name[0]=project.optString("name");counts[0]=ProjectPresentation.counts(project);stable[0]=a.adapter.getItemId(row);assertRow(a,holder[0],project);});
                        for(String part:new String[]{"name","counts","state","date"}){
                            scenario.onActivity(a->{TextView text=part(holder[0],part);text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});awaitLayout(scenario);
                            scenario.onActivity(a->{assertSame(outer[0],rowById(a,id[0]));readable(a,part(holder[0],part));safe(a);});
                        }
                        for(int phase=0;phase<(row==0?4:1);phase++){
                            final int step=phase;String[] before={null};int[] top={0};View[] focused={null};
                            scenario.onActivity(a->{try{
                                safe(a);before[0]=anchor(a);top[0]=offset(a);focused[0]=a.getWindow().getDecorView().findFocus();JSONObject project=a.items.get(row);
                                if(row==0){project.remove("active_count");project.remove("attention_count");if(step==1)project.put("active_count",2);else if(step==2)project.put("attention_count",3);}
                                assertSame(outer[0],a.adapter.getView(row,outer[0],a.list));assertSame(holder[0],outer[0].getTag());assertEquals(stable[0],a.adapter.getItemId(row));assertEquals(id[0],holder[0].id);assertEquals(name[0],holder[0].name.getText().toString());assertEquals(counts[0],holder[0].counts.getText().toString());
                            }catch(org.json.JSONException error){throw new AssertionError(error);}});awaitLayout(scenario);
                            scenario.onActivity(a->{assertSame(outer[0],rowById(a,id[0]));assertAnchor(a,before[0],top[0]);assertSame("A native bind keeps the actual focused View",focused[0],a.getWindow().getDecorView().findFocus());assertRow(a,holder[0],a.items.get(row));safe(a);});rebound++;
                        }
                    }
                    boolean[] namePresent=new boolean[2],namesChanged={false};Object[] priorNames=new Object[2];int[] priorPosition={0},priorTop={0};String[] priorAnchor={null};Throwable nameFailure=null;
                    scenario.onActivity(a->{safe(a);priorPosition[0]=a.list.getFirstVisiblePosition();priorTop[0]=offset(a);priorAnchor[0]=anchor(a);for(int n=0;n<2;n++){JSONObject project=a.items.get(n);namePresent[n]=project.has("name");priorNames[n]=project.opt("name");}});
                    String prefix=L.t("Studio reference archive — interaction and motion research for the ","工作室交互参考资料归档与研究 · 手机分享与客户端体验设计 · 项目方向：");String[] longNames={prefix+L.t("Mobile client","移动客户端"),prefix+L.t("Desktop client","桌面客户端")};
                    try{
                        scenario.onActivity(a->{try{safe(a);namesChanged[0]=true;for(int n=0;n<2;n++)a.items.get(n).put("name",longNames[n]);a.adapter.notifyDataSetChanged();}catch(org.json.JSONException error){throw new AssertionError(error);}});awaitLayout(scenario);
                        for(int position=0;position<2;position++){
                            final int row=position;scenario.onActivity(a->a.list.setSelectionFromTop(row,0));awaitLayout(scenario);
                            scenario.onActivity(a->{JSONObject project=a.items.get(row);MainActivity.HomeHolder holder=(MainActivity.HomeHolder)rowById(a,project.optString("id")).getTag();assertRow(a,holder,project);assertEquals(longNames[row],holder.name.getText().toString());assertTrue("Independent-name suffix is beyond the previous two-line cap",holder.name.getLayout().getLineForOffset(prefix.length())>=2);});
                            for(boolean tail:new boolean[]{false,true}){
                                Rect[] requested={null};scenario.onActivity(a->{TextView name=((MainActivity.HomeHolder)rowById(a,a.items.get(row).optString("id")).getTag()).name;requested[0]=nameLine(name,tail);name.requestRectangleOnScreen(new Rect(requested[0]),true);});awaitLayout(scenario);
                                scenario.onActivity(a->{TextView name=((MainActivity.HomeHolder)rowById(a,a.items.get(row).optString("id")).getTag()).name;assertEquals(longNames[row],name.getText().toString());assertCompleteText(name);Rect shown=new Rect();assertTrue(name.getLocalVisibleRect(shown));assertTrue("The requested first or final long-name line is visible: row="+row+" tail="+tail+" requested="+requested[0].toShortString()+" shown="+shown.toShortString(),shown.contains(requested[0]));Rect physical=new Rect(requested[0]),textBounds=bounds(name),safeFrame=new Rect();physical.offset(textBounds.left,textBounds.top);a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safeFrame);assertTrue(safeFrame.intersect(bounds(a.list)));assertTrue("The complete requested line fits the list and window",safeFrame.contains(physical));safe(a);});
                            }
                        }
                    }catch(Throwable error){nameFailure=error;throw error;}
                    finally{if(namesChanged[0])try{
                        scenario.onActivity(a->{try{safe(a);for(int n=0;n<2;n++){JSONObject project=a.items.get(n);if(namePresent[n])project.put("name",priorNames[n]);else project.remove("name");assertEquals(namePresent[n],project.has("name"));assertSame(priorNames[n],project.opt("name"));}a.adapter.notifyDataSetChanged();a.list.setSelectionFromTop(priorPosition[0],priorTop[0]);}catch(org.json.JSONException error){throw new AssertionError(error);}});awaitLayout(scenario);
                        scenario.onActivity(a->{assertAnchor(a,priorAnchor[0],priorTop[0]);assertProjects(a);safe(a);});
                    }catch(Throwable restoreError){if(nameFailure!=null)nameFailure.addSuppressed(restoreError);else throw restoreError;}}
                    if(scale==1f){scenario.onActivity(a->a.list.setSelectionFromTop(0,0));awaitLayout(scenario);scenario.onActivity(HomeRowsPresentationTest::normalVisible);if(directory!=null){captureNormal(scenario,directory,nonce,language,theme);captured++;}}
                    scenario.onActivity(a->{safe(a);guards(identity,"before-close",a);});completed++;lifecycle(identity,"full-completed");
                }catch(Throwable error){failure=error;throw error;}
                finally{closeKnown(scenario,failure,destroyed,identity);if(scenario!=null&&destroyed[0]){closed++;if(retained[0]!=null){guards(identity,"after-destroyed",retained[0]);if(failure==null){assertEquals(0,retained[0].forbiddenActions.get());assertEquals(0,retained[0].probeStore.syncCalls.get());}}}}
            }
        }finally{if(destroyed[0]){DemoHomeRecoveryActivity.hierarchyFontScale=previous;L.language(languageBefore);lifecycle("rows","font-and-language-restored");}}
        assertEquals(6,entered);assertEquals(6,completed);assertEquals(6,closed);assertEquals(36,rebound);assertEquals(directory==null?0:4,captured);assertFalse(unresolvedLifetime);
        lifecycle("rows","SUMMARY entered="+entered+" completed="+completed+" DESTROYED="+closed+" completed_rebinds="+rebound+" captures="+captured+" business_actions=not_invoked_by_test");
    }
    static View rowById(DemoHomeRecoveryActivity a,String id){for(int n=0;n<a.list.getChildCount();n++){View row=a.list.getChildAt(n);if(row.getTag() instanceof MainActivity.HomeHolder&&id.equals(((MainActivity.HomeHolder)row.getTag()).id))return row;}fail("The requested stable project is not laid out: "+id);return null;}
    static Rect nameLine(TextView text,boolean tail){assertCompleteText(text);android.text.Layout layout=text.getLayout();int line=tail?layout.getLineForOffset(text.length()-1):0;return new Rect(text.getCompoundPaddingLeft()+(int)Math.floor(layout.getLineLeft(line)),text.getCompoundPaddingTop()+layout.getLineTop(line),text.getCompoundPaddingLeft()+(int)Math.ceil(layout.getLineRight(line)),text.getCompoundPaddingTop()+layout.getLineBottom(line));}
    static TextView part(MainActivity.HomeHolder holder,String part){return part.equals("name")?holder.name:part.equals("counts")?holder.counts:part.equals("state")?holder.state:holder.date;}
    static void assertRow(DemoHomeRecoveryActivity a,MainActivity.HomeHolder holder,JSONObject project){
        safe(a);assertCard(a,holder,project);assertTrue(holder.card.getBackground() instanceof GradientDrawable);GradientDrawable surface=(GradientDrawable)holder.card.getBackground();assertNotNull(surface.getColor());assertEquals(Ui.SURFACE,surface.getColor().getDefaultColor());assertEquals(Ui.dp(a,Ui.RADIUS_CARD),surface.getCornerRadius(),0f);assertEquals(0f,holder.card.getElevation(),0f);assertTrue(holder.card.getForeground() instanceof RippleDrawable);assertTrue(holder.card.getMinimumHeight()>=Ui.dp(a,48));
        boolean large=a.getResources().getConfiguration().fontScale>=1.5f;int edge=bounds(a.list).left+a.list.getPaddingLeft()+Ui.dp(a,16);
        assertEquals(Ui.dp(a,16),holder.card.getPaddingLeft());assertEquals(Ui.dp(a,16),holder.card.getPaddingRight());assertEquals(Ui.dp(a,16),holder.card.getPaddingTop());assertEquals(Ui.dp(a,16),holder.card.getPaddingBottom());
        assertEquals(edge,bounds(holder.state).left);assertEquals(edge,bounds(holder.name).left);assertEquals(bounds(holder.name).left,bounds(holder.counts).left);assertEquals(Ui.dp(a,232),holder.name.getWidth());
        assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,20,a.getResources().getDisplayMetrics()),holder.name.getTextSize(),0.01f);assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,13,a.getResources().getDisplayMetrics()),holder.counts.getTextSize(),0.01f);
        assertEquals(project.optString("name"),holder.name.getText().toString());assertEquals(ProjectPresentation.counts(project),holder.counts.getText().toString());assertEquals(ProjectPresentation.state(project),holder.state.getText().toString());for(String part:new String[]{"name","counts","state","date"})assertCompleteText(part(holder,part));
        int color=project.optInt("attention_count")>0?Ui.AMBER:project.optInt("active_count")>0?Ui.ACCENT:Ui.MUTED;assertEquals(color,holder.state.getCurrentTextColor());assertTrue(holder.state.getBackground() instanceof GradientDrawable);GradientDrawable pill=(GradientDrawable)holder.state.getBackground();assertNotNull(pill.getColor());assertEquals((color&0x00ffffff)|0x1F000000,pill.getColor().getDefaultColor());
        LinearLayout footer=(LinearLayout)holder.state.getParent();assertSame(footer,holder.date.getParent());assertEquals(Ui.dp(a,12),bounds(footer).top-bounds(holder.counts).bottom);
        assertEquals(large?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL,footer.getOrientation());assertEquals(large?Gravity.START:Gravity.END,holder.date.getGravity()&Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);if(large){assertEquals(bounds(holder.state).left,bounds(holder.date).left);assertTrue(bounds(holder.date).top>=bounds(holder.state).bottom+Ui.dp(a,8));assertEquals(Integer.MAX_VALUE,holder.name.getMaxLines());}
        ViewGroup outer=(ViewGroup)holder.card.getParent();assertEquals(1,outer.getChildCount());assertSame(holder.card,outer.getChildAt(0));assertEquals(Ui.dp(a,12),outer.getPaddingBottom());assertEquals(Ui.dp(a,12),bounds(outer).bottom-bounds(holder.card).bottom);
    }
    static void readable(DemoHomeRecoveryActivity a,TextView text){assertCompleteText(text);assertFullVisibility(text);Rect safe=new Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(safe);assertTrue(safe.intersect(bounds(a.list)));assertTrue("The complete target fits the actual list and window",safe.contains(bounds(text)));}
    static void normalVisible(DemoHomeRecoveryActivity a){
        safe(a);assertProjects(a);assertEquals(1f,a.getResources().getConfiguration().fontScale,0f);assertEquals("home-ui-project-0",anchor(a));assertEquals(0,offset(a));assertTrue(a.hasWindowFocus());View decor=a.getWindow().getDecorView();assertTrue(decor.isAttachedToWindow());assertFalse(decor.isLayoutRequested());assertFalse(a.list.isLayoutRequested());assertEquals(1f,a.getWindow().getAttributes().alpha,0f);assertEquals(1f,a.root.getAlpha(),0f);assertEquals(0f,a.root.getTranslationY(),0f);
        WindowInsets insets=decor.getRootWindowInsets();assertNotNull(insets);assertFalse("Reject a keyboard-visible capture",insets.isVisible(WindowInsets.Type.ime()));assertCompleteText(a.demoNotice);assertFullVisibility(a.demoNotice);assertCompleteText(a.notice);assertFullVisibility(a.notice);
        TextView heading=findText(a.root,L.t("Recent handoffs","最近交办"));assertNotNull(heading);assertCompleteText(heading);assertFullVisibility(heading);View gear=NavigationIconCaptureTest.button(a,"home");assertFullVisibility(gear);assertTrue(gear.getHeight()>=Ui.dp(a,48));assertTrue(gear.getWidth()>=Ui.dp(a,48));
        MainActivity.HomeHolder holder=(MainActivity.HomeHolder)rowById(a,"home-ui-project-0").getTag();assertRow(a,holder,a.items.get(0));for(String part:new String[]{"name","counts","state","date"})readable(a,part(holder,part));
    }
    static void guards(String identity,String phase,DemoHomeRecoveryActivity retained){Bundle status=new Bundle();status.putString("stream","HOME_ROWS_GUARDS\t"+identity+"\t"+phase+"\tforbidden="+retained.forbiddenActions.get()+"\tsync_calls="+retained.probeStore.syncCalls.get()+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static void captureNormal(ActivityScenario<DemoHomeRecoveryActivity> scenario,File directory,String nonce,String language,String theme)throws Exception{
        awaitLayout(scenario);awaitLayout(scenario);long settledAt=SystemClock.elapsedRealtime();Thread.sleep(2000);ready(scenario);JSONObject[] data={null};
        scenario.onActivity(a->{normalVisible(a);assertTrue(SystemClock.elapsedRealtime()-settledAt>=2000);try{JSONArray rows=new JSONArray();for(int n=0;n<a.list.getChildCount();n++){View outer=a.list.getChildAt(n);if(!(outer.getTag() instanceof MainActivity.HomeHolder))continue;MainActivity.HomeHolder holder=(MainActivity.HomeHolder)outer.getTag();rows.put(new JSONObject().put("id",holder.id).put("name",holder.name.getText()).put("counts",holder.counts.getText()).put("state",holder.state.getText()).put("date",holder.date.getText()).put("name_width",holder.name.getWidth()).put("name_bounds",bounds(holder.name).toShortString()).put("row_bounds",bounds(holder.card).toShortString()));}data[0]=new JSONObject().put("nonce",nonce).put("language",language).put("theme",theme).put("font_scale",1).put("memory_only",true).put("capture_accepted",false).put("fixture",a.getClass().getSimpleName()).put("marker",a.demoNotice.getText()).put("marker_bounds",bounds(a.demoNotice).toShortString()).put("required_predraws_completed",2).put("settled_at_elapsed_ms",settledAt).put("capture_at_elapsed_ms",SystemClock.elapsedRealtime()).put("window_focus",a.hasWindowFocus()).put("ime_visible",false).put("anchor",anchor(a)).put("offset",offset(a)).put("rows",rows).put("forbidden_actions",a.forbiddenActions.get()).put("sync_calls",a.probeStore.syncCalls.get()).put("business_actions","not_invoked_by_test").put("system_bar_pixels_require_visual_review",true);}catch(Exception error){throw new AssertionError(error);}});
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);String stem="home-rows-"+language+"-"+theme+"-font1";
        try{assertEquals(320,image.getWidth());assertEquals(640,image.getHeight());try(OutputStream out=Files.newOutputStream(new File(directory,stem+".png").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}try(OutputStream out=Files.newOutputStream(new File(directory,stem+".json").toPath(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){out.write(data[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{image.recycle();}
    }
}
