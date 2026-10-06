package app.droprun;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static app.droprun.ShareEditorTest.assertCompleteText;
import static app.droprun.ShareEditorTest.findText;
import static app.droprun.ShareSettingsHierarchyTest.bounds;
import static app.droprun.ShareSettingsHierarchyTest.visible;
import static org.junit.Assert.*;

/** Three synthetic rows only. Refresh is a memory render; no task/card/remove/Send navigation. */
public class HistoryIdentityTest {
    String phase(){return InstrumentationRegistry.getArguments().getString("captureHistoryUi","");}
    Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    ActivityScenario<DemoHistoryIdentityActivity> launch(String language,String theme,float scale,boolean extreme){
        DemoHistoryIdentityActivity.fontScale=scale;assertTrue(context().getPackageName().endsWith(".debug"));
        return ActivityScenario.launch(new Intent(context(),DemoHistoryIdentityActivity.class).putExtra("historyIdentity",true).putExtra("language",language).putExtra("appearance",theme).putExtra("projectId",DemoHistoryIdentityActivity.ID).putExtra("projectName",DemoHistoryIdentityActivity.sampleName(language.equals("zh"),extreme)).putExtra("revoked",extreme));
    }
    @Test public void historyIdentityAndRecycledStatusStayLegible()throws Exception{
        float previous=DemoHistoryIdentityActivity.fontScale;boolean red=phase().equals("red");
        try{for(String language:red?new String[]{"en"}:new String[]{"en","zh"})for(String theme:red?new String[]{"light"}:new String[]{"light","dark"})for(float scale:red?new float[]{1}:new float[]{1,2})for(boolean extreme:red?new boolean[]{false}:new boolean[]{false,true}){
            try(ActivityScenario<DemoHistoryIdentityActivity> scenario=launch(language,theme,scale,extreme)){
                ready(scenario);String prefix="history-"+language+"-"+theme+"-font"+(int)scale+(extreme?"-extreme":"-normal");capture(scenario,prefix,null);
                if(red){scenario.onActivity(a->{TextView title=findText(a.getWindow().getDecorView(),a.projectName);ProjectHistoryActivity.Holder holder=first(a);assertTrue("Original identity is clipped inside navigation: width="+title.getWidth()+", ellipsis="+title.getLayout().getEllipsisCount(0)+"; running pill actual="+Integer.toHexString(tint(holder.status))+", expected="+Integer.toHexString(tintFor(TaskPresentation.statusColor("running"))),a.findViewById(android.R.id.content).findViewWithTag("history-project-name")!=null&&tint(holder.status)==tintFor(TaskPresentation.statusColor("running")));});continue;}
                scenario.onActivity(a->{
                    TextView title=a.findViewById(android.R.id.content).findViewWithTag("history-project-name");View info=a.findViewById(android.R.id.content).findViewWithTag("history-project-info");assertNotNull(title);assertNotNull(info);assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,24,a.getResources().getDisplayMetrics()),title.getTextSize(),.01f);assertEquals(scale>=1.5f?3:2,title.getMaxLines());assertEquals(a.projectName,title.getText().toString());assertEquals(a.projectName,String.valueOf(title.getContentDescription()));assertTrue(title.getWidth()>=a.dp(220));assertTrue(info.getHeight()>=a.dp(48));assertTrue(info.getWidth()>=a.dp(48));visible(info);assertTrue(info.isClickable());assertTrue(info.isFocusable());
                    if(!extreme)assertCompleteText(title);assertNotNull(findText(a.getWindow().getDecorView(),L.t("Handoffs","交办")));String label=a.store.projectLabel(a.projectId,a.projectName);assertFalse(label.equals(a.projectName));assertNotNull(findText(a.getWindow().getDecorView(),L.t("Project · ","项目 · ")+label.substring(a.projectName.length()+3)));
                    assertEquals(0,a.list.getHeaderViewsCount());assertEquals(1,a.list.getFooterViewsCount());assertEquals(3,a.rows.size());assertTrue(a.adapter.hasStableIds());assertTrue("Fixed identity leaves at least one actual task card viewport",a.list.getHeight()-a.list.getPaddingTop()-a.list.getPaddingBottom()>=first(a).card.getHeight());assertTrue(first(a).card.getHeight()>=a.dp(48));assertSafe(a);
                    if(extreme){assertEquals(0,a.probe.catalog.length());assertEquals(2,a.probe.summaries.length());}else assertEquals(2,a.probe.catalog.length());
                });
                scenario.onActivity(a->{View info=a.findViewById(android.R.id.content).findViewWithTag("history-project-info");assertTrue(info.performClick());});dialogReady();
                final String[] message={null};InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{TextView text=dialogText();assertNotNull(text);message[0]=text.getText().toString();assertEquals(DemoHistoryIdentityActivity.sampleName(language.equals("zh"),extreme)+"\n\n"+L.t("Project ID: ","项目 ID：")+DemoHistoryIdentityActivity.ID,message[0]);assertCompleteText(text);Rect last=new Rect();text.getLayout().getLineBounds(text.getLayout().getLineForOffset(text.length()-1),last);last.offset(0,text.getTotalPaddingTop());text.requestRectangleOnScreen(last,true);});idle();
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{TextView text=dialogText();Rect last=new Rect();text.getLayout().getLineBounds(text.getLayout().getLineForOffset(text.length()-1),last);last.offset(0,text.getTotalPaddingTop());Rect clipped=new Rect();assertTrue(text.getLocalVisibleRect(clipped));assertTrue(clipped.top<=last.top&&clipped.bottom>=last.bottom);Button close=dialogRoot().findViewById(android.R.id.button1);assertTrue(close.getHeight()>=48);Rect actual=new Rect();assertTrue(close.getLocalVisibleRect(actual));assertEquals(new Rect(0,0,close.getWidth(),close.getHeight()),actual);assertTrue(bounds(dialogRoot()).contains(bounds(close)));});
                if(extreme)capture(scenario,prefix+"-info",message[0]);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{Button close=dialogRoot().findViewById(android.R.id.button1);assertTrue(close.performClick());});ready(scenario);
                scenario.onActivity(a->{assertNull(dialogText());assertEquals(0,a.forbiddenActions);assertEquals(3,a.rows.size());testStatusCycle(a);a.list.setSelectionFromTop(1,-a.dp(8));});ready(scenario);
                String[] anchor={null};int[] offset={0};Rect[] box={null};View[] focus={null};scenario.onActivity(a->{anchor[0]=first(a).id;offset[0]=a.list.getChildAt(0).getTop();box[0]=bounds(a.list);focus[0]=findDescription(a.getWindow().getDecorView(),L.t("Refresh project history","刷新项目历史"));assertNotNull(focus[0]);assertTrue(focus[0].requestFocusFromTouch());try{a.probe.entries.getJSONObject(0).put("updated_at",100);}catch(Exception e){throw new AssertionError(e);}assertTrue(focus[0].performClick());});ready(scenario);
                scenario.onActivity(a->{assertEquals(anchor[0],first(a).id);assertEquals(offset[0],a.list.getChildAt(0).getTop());assertEquals(box[0],bounds(a.list));assertTrue(focus[0].hasFocus());assertTrue(a.memoryRefreshes>=2);assertSafe(a);});
            }
        }}finally{DemoHistoryIdentityActivity.fontScale=previous;}
    }
    static int tintFor(int color){return (color&0x00ffffff)|0x1f000000;}
    static int tint(TextView view){return ((GradientDrawable)view.getBackground()).getColor().getDefaultColor();}
    static ProjectHistoryActivity.Holder first(DemoHistoryIdentityActivity a){return (ProjectHistoryActivity.Holder)a.list.getChildAt(0).getTag();}
    static void testStatusCycle(DemoHistoryIdentityActivity a){View row=a.list.getChildAt(0);ProjectHistoryActivity.Holder holder=(ProjectHistoryActivity.Holder)row.getTag();JSONObject task=a.rows.get(0);try{for(String state:new String[]{"completed","running","waiting_for_approval","failed","pending","completed"}){boolean pending=state.equals("pending");task.put("status",pending?"queued":state);if(pending)a.pendingIds.add(task.optString("id"));else a.pendingIds.remove(task.optString("id"));assertSame(row,a.adapter.getView(0,row,a.list));assertSame(holder,row.getTag());int color=pending?Ui.AMBER:TaskPresentation.statusColor(state);assertEquals(color,holder.status.getCurrentTextColor());assertEquals(tintFor(color),tint(holder.status));assertEquals(pending?L.t("Saved on this phone","已保存在手机"):TaskPresentation.status(state),holder.status.getText().toString());}assertSafe(a);}catch(JSONException e){throw new AssertionError(e);}}
    static void assertSafe(DemoHistoryIdentityActivity a){assertEquals(0,a.forbiddenActions);assertFalse(a.busy);assertNull(a.removal);assertEquals(3,a.rows.size());assertTrue(a.pendingIds.isEmpty());}
    static View findDescription(View root,String value){if(value.contentEquals(String.valueOf(root.getContentDescription())))return root;if(root instanceof ViewGroup)for(int n=0;n<((ViewGroup)root).getChildCount();n++){View result=findDescription(((ViewGroup)root).getChildAt(n),value);if(result!=null)return result;}return null;}
    static void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    static void ready(ActivityScenario<DemoHistoryIdentityActivity> scenario)throws Exception{long end=SystemClock.elapsedRealtime()+3000;boolean[] ok={false};do{idle();scenario.onActivity(a->ok[0]=a.hasWindowFocus()&&a.list.getHeight()>0&&a.list.getChildCount()>0&&!a.list.isLayoutRequested()&&!a.getWindow().getDecorView().isLayoutRequested());if(ok[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("History native layout did not settle");}
    static View dialogRoot(){for(View view:WindowInspector.getGlobalWindowViews())if(view.findViewById(android.R.id.message)!=null)return view;return null;}
    static TextView dialogText(){View root=dialogRoot();return root==null?null:root.findViewById(android.R.id.message);}
    static void dialogReady()throws Exception{long end=SystemClock.elapsedRealtime()+3000;boolean[] ok={false};do{idle();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{View root=dialogRoot();ok[0]=root!=null&&root.hasWindowFocus()&&!root.isLayoutRequested()&&dialogText().getLayout()!=null;});if(ok[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("Project information dialog did not settle");}
    @Test public void commonDisclosureTurnsOnlyAQuarterTurn()throws Exception{
        float previous=DemoHistoryIdentityActivity.fontScale;JSONArray reports=new JSONArray();
        try{for(String[] config:new String[][]{{"en","light"},{"zh","dark"}}){try(ActivityScenario<DemoHistoryIdentityActivity> scenario=launch(config[0],config[1],1,false)){ready(scenario);
            for(boolean opened:new boolean[]{false,true}){AlertDialog[] dialog={null};View[] header={null},arrow={null},body={null};List<Boolean> callbacks=new ArrayList<>();scenario.onActivity(a->{LinearLayout content=Ui.vertical(a);content.addView(Ui.caption(a,L.t("Synthetic disclosure content","合成折叠内容")));body[0]=content;LinearLayout group=Ui.disclosure(a,L.t("Material details","材料详情"),content,opened,callbacks::add);header[0]=group.getChildAt(0);arrow[0]=((ViewGroup)header[0]).getChildAt(1);dialog[0]=new AlertDialog.Builder(a).setTitle(L.t("UI probe · memory only","界面探针 · 仅内存")).setView(group).setPositiveButton(L.t("Close","关闭"),null).show();});idle();Thread.sleep(100);
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue("Enabled-motion path is actually active",Ui.motionEnabled(header[0].getContext()));assertEquals(opened?270f:180f,arrow[0].getRotation(),.01f);assertEquals(opened?View.VISIBLE:View.GONE,body[0].getVisibility());assertEquals(opened,header[0].getTag());});
                if(!opened){reports.put(trace(header[0],arrow[0],false,true,config,"expand"));disclosureState(header[0],body[0],callbacks,true,1);reports.put(trace(header[0],arrow[0],false,false,config,"collapse"));disclosureState(header[0],body[0],callbacks,false,2);reports.put(trace(header[0],arrow[0],true,true,config,"reverse"));disclosureState(header[0],body[0],callbacks,false,4);}
                else reports.put(trace(header[0],arrow[0],false,false,config,"initial-open-collapse"));
                scenario.onActivity(a->{assertEquals(180f,arrow[0].getRotation(),.01f);assertEquals(opened?1:4,callbacks.size());assertEquals(Boolean.FALSE,header[0].getTag());assertEquals(View.GONE,body[0].getVisibility());assertTrue(String.valueOf(header[0].getContentDescription()).endsWith(L.t(", collapsed",", 已折叠")));assertSafe(a);dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();});ready(scenario);
            }
        }}}finally{DemoHistoryIdentityActivity.fontScale=previous;writeJson("common-disclosure-samples",new JSONObject().put("traces",reports));}
    }
    static void disclosureState(View header,View body,List<Boolean> callbacks,boolean open,int count){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertEquals(open?View.VISIBLE:View.GONE,body.getVisibility());assertEquals(open,header.getTag());assertEquals(count,callbacks.size());assertEquals(open,callbacks.get(callbacks.size()-1));assertTrue(String.valueOf(header.getContentDescription()).endsWith(open?L.t(", expanded",", 已展开"):L.t(", collapsed",", 已折叠")));});}
    static JSONObject trace(View header,View arrow,boolean reverse,boolean opening,String[] config,String name)throws Exception{
        JSONArray values=new JSONArray();List<Float> angles=new ArrayList<>();long began=SystemClock.elapsedRealtime();long[] reversed={-1};int[] reverseIndex={-1};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertEquals(opening?180f:270f,arrow.getRotation(),.01f);assertTrue(header.performClick());});
        do{InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{float angle=arrow.getRotation();long elapsed=SystemClock.elapsedRealtime()-began;try{angles.add(angle);values.put(new JSONObject().put("elapsed_ms",elapsed).put("rotation",angle));if(reverse&&reversed[0]<0&&elapsed>=60&&angle>180f&&angle<270f){reversed[0]=elapsed;reverseIndex[0]=angles.size()-1;assertTrue(header.performClick());}}catch(JSONException e){throw new AssertionError(e);}});Thread.sleep(16);}while(SystemClock.elapsedRealtime()-began<500);
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;boolean interior=false;for(int n=0;n<angles.size();n++){float v=angles.get(n);min=Math.min(min,v);max=Math.max(max,v);interior|=v>180f&&v<270f;assertTrue("Sampled quarter-turn bounds",v>=180f&&v<=270f);if(n>0){boolean increasing=opening&&(!reverse||n<=reverseIndex[0]);if(increasing)assertTrue("Opening samples increase",v>=angles.get(n-1)-.01f);else assertTrue("Closing samples decrease",v<=angles.get(n-1)+.01f);}}
        assertTrue("A real intermediate angle was sampled",interior);if(reverse)assertTrue("Reverse was requested during an observed interior angle",reversed[0]>=60);assertEquals(reverse||!opening?180f:270f,angles.get(angles.size()-1),.01f);
        return new JSONObject().put("language",config[0]).put("theme",config[1]).put("phase",name).put("motion_enabled",Ui.motionEnabled(header.getContext())).put("samples",values).put("min",min).put("max",max).put("reverse_elapsed_ms",reversed[0]).put("reverse_sample_index",reverseIndex[0]);
    }
    void capture(ActivityScenario<DemoHistoryIdentityActivity> scenario,String name,String info)throws Exception{
        if(phase().isEmpty())return;Thread.sleep(2000);JSONObject[] data={null};scenario.onActivity(a->{try{TextView title=findText(a.getWindow().getDecorView(),a.projectName);ProjectHistoryActivity.Holder holder=first(a);TextView marker=a.findViewById(android.R.id.content).findViewWithTag("history-memory-marker");assertCompleteText(marker);if(info==null)visible(marker);data[0]=new JSONObject().put("project_name",a.projectName).put("project_id",a.projectId).put("font_scale",a.getResources().getConfiguration().fontScale).put("identity_present",a.findViewById(android.R.id.content).findViewWithTag("history-project-name")!=null).put("title_bounds",bounds(title).toShortString()).put("title_width",title.getWidth()).put("title_lines",title.getLineCount()).put("title_max_lines",title.getMaxLines()).put("title_last_line_ellipsis",title.getLayout().getEllipsisCount(title.getLineCount()-1)).put("list_bounds",bounds(a.list).toShortString()).put("list_viewport_height",a.list.getHeight()-a.list.getPaddingTop()-a.list.getPaddingBottom()).put("first_card_height",holder.card.getHeight()).put("status",holder.status.getText()).put("status_color",Integer.toHexString(holder.status.getCurrentTextColor())).put("status_tint",Integer.toHexString(tint(holder.status))).put("marker",marker.getText()).put("forbidden_actions",a.forbiddenActions).put("memory_only",true).put("info_message",info==null?JSONObject.NULL:info);assertSafe(a);}catch(Exception e){throw new AssertionError(e);}});writeJson(name,data[0]);Bitmap png=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(png);try{assertEquals(320,png.getWidth());assertEquals(640,png.getHeight());try(FileOutputStream output=new FileOutputStream(file(name,"png"))){assertTrue(png.compress(Bitmap.CompressFormat.PNG,100,output));}}finally{png.recycle();}
    }
    File file(String name,String extension){File directory=new File(context().getExternalFilesDir(null),"ui-probe-evidence/history-identity-"+phase()+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());File result=new File(directory,name+"."+extension);assertFalse(result.exists());return result;}
    void writeJson(String name,JSONObject value)throws Exception{if(!phase().isEmpty())try(FileOutputStream output=new FileOutputStream(file(name,"json"))){output.write(value.toString(2).getBytes(StandardCharsets.UTF_8));}}
}
