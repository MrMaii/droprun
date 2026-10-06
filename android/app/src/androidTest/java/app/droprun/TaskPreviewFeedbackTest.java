package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Checks real preview copy and action presence only; never opens a link or activates an action. */
public class TaskPreviewFeedbackTest {
    static final String URL="https://preview.example.invalid/s/sample";
    @Test public void missingAndUnavailablePreviewsExplainTheExistingFilesInBothLanguages(){
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language)){
            scenario.onActivity(a->{
                for(String terminal:new String[]{"completed","blocked","failed","cancelled"}){
                    configure(a,terminal,"","",0,"live",false);assertNotNull(find(a.body,L.t("No preview is attached to this handoff. Check screenshots and delivery files below.","本次交办未附预览，可查看下方的截图与交付文件。")));assertActions(a,false,false);assertFiles(a);
                    configure(a,terminal,"unavailable",URL,0,"snapshot",false);assertActions(a,false,true);
                }
                configure(a,"running","","",0,"live",false);assertNull(find(a.body,L.t("Preview","预览")));assertActions(a,false,false);
                for(String url:new String[]{"",URL}){
                    configure(a,"completed","unavailable",url,0,"live",false);
                    assertNotNull(find(a.body,L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。")));
                    assertNull(find(a.body,L.t("Live project preview. Later changes may alter what you see.","当前项目预览；后续修改可能改变内容。")));assertNull(find(a.body,L.t("This preview expired or stopped.","预览已失效或停止。")));assertActions(a,false,!url.isEmpty());assertFiles(a);
                }
                configure(a,"running","unavailable",URL,0,"live",false);assertActions(a,false,false);
                configure(a,"completed","unavailable",URL,0,"snapshot",true);assertActions(a,false,false);
                configure(a,"completed","ready","",0,"snapshot",false);assertNotNull(find(a.body,L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。")));assertActions(a,false,false);
                Object report=a.sample.opt("report");a.sample.remove("report");
                try{
                    configure(a,"running","unavailable",URL,0,"live",false);assertNotNull(find(a.body,L.t("A preview isn't available right now.","预览暂不可用。")));assertNull(find(a.body,L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。")));assertNull(find(a.body,L.t("Screenshots & delivery files","截图与交付文件")));assertActions(a,false,false);
                    configure(a,"completed","unavailable",URL,0,"snapshot",false);assertNotNull(find(a.body,L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。")));assertFiles(a);assertActions(a,false,true);
                }finally{try{a.sample.put("report",report);}catch(Exception error){throw new AssertionError(error);}a.render();}
            });
        }
    }
    @Test public void normalTextPreviewAndDeliveryActionsAreFullyVisibleInBothLanguages()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language,1f)){
            for(String state:new String[]{"","unavailable","ready"}){
                scenario.onActivity(a->{assertEquals(1f,a.getResources().getConfiguration().fontScale,0.01f);configure(a,"completed",state,state.equals("ready")?URL:"",state.equals("ready")?Long.MAX_VALUE:0,state.equals("ready")?"snapshot":"live",false);});frames(scenario);
                scenario.onActivity(a->{TextView preview=find(a.body,L.t("Preview","预览")),followup=find(a.body,L.t("Follow up","继续追问"));assertNotNull(preview);assertNotNull(followup);Rect region=new Rect();preview.getDrawingRect(region);a.body.offsetDescendantRectToMyCoords(preview,region);Rect end=new Rect();followup.getDrawingRect(end);a.body.offsetDescendantRectToMyCoords(followup,end);region.union(end);a.body.requestRectangleOnScreen(region,true);});frames(scenario);
                InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);
                scenario.onActivity(a->{
                    TextView preview=find(a.body,L.t("Preview","预览")),detail=find(a.body,state.equals("ready")?L.t("Fixed snapshot from this handoff.","本次交付的固定快照。"):state.isEmpty()?L.t("No preview is attached to this handoff. Check screenshots and delivery files below.","本次交办未附预览，可查看下方的截图与交付文件。"):L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。")),files=find(a.body,L.t("Screenshots & delivery files","截图与交付文件")),followup=find(a.body,L.t("Follow up","继续追问"));
                    for(TextView text:new TextView[]{preview,detail,files,followup}){assertNotNull(text);assertCompleteAndVisible(a,text);}if(state.equals("ready"))assertCompleteAndVisible(a,find(a.body,L.t("Open preview","打开预览")));assertTrue(files instanceof Button);assertTrue(followup instanceof Button);assertTrue(files.isEnabled());assertTrue(followup.isEnabled());assertActions(a,state.equals("ready"),false);assertSafe(a);
                });capture(language,state.isEmpty()?"missing":state);
            }
        }
    }
    @Test public void systemLargeTextPreviewAndDeliveryActionsRemainIndividuallyReachable()throws Exception{
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language)){
            for(String state:new String[]{"","unavailable"}){
                scenario.onActivity(a->{assertEquals(2f,a.getResources().getConfiguration().fontScale,0.01f);configure(a,"completed",state,"",0,"live",false);});frames(scenario);
                String[] targets={state.isEmpty()?L.t("No preview is attached to this handoff. Check screenshots and delivery files below.","本次交办未附预览，可查看下方的截图与交付文件。"):L.t("A preview isn't available right now. Check screenshots and delivery files below.","预览暂不可用，可查看下方的截图与交付文件。"),L.t("Screenshots & delivery files","截图与交付文件"),L.t("Follow up","继续追问")};
                for(String target:targets){
                    scenario.onActivity(a->{TextView text=find(a.body,target);assertNotNull(text);text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});frames(scenario);
                    scenario.onActivity(a->{TextView text=find(a.body,target);assertCompleteAndVisible(a,text);if(text instanceof Button){assertTrue(text.getWidth()>=Ui.dp(a,48));assertTrue(text.getHeight()>=Ui.dp(a,48));assertTrue(text.isEnabled());}assertActions(a,false,false);assertSafe(a);});
                }
            }
        }
    }
    @Test public void readyReopeningAndExpiredPreviewActionsKeepTheirExistingBehavior(){
        for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language)){
            scenario.onActivity(a->{
                for(String kind:new String[]{"snapshot","live"}){
                    configure(a,"completed","ready",URL,Long.MAX_VALUE,kind,false);assertActions(a,true,false);
                    assertNotNull(find(a.body,kind.equals("snapshot")?L.t("Fixed snapshot from this handoff.","本次交付的固定快照。"):L.t("Live project preview. Later changes may alter what you see.","当前项目预览；后续修改可能改变内容。")));
                    a.busy=true;a.render();assertActions(a,true,false);a.busy=false;a.render();
                    configure(a,"completed","reopening",URL,1,kind,false);assertActions(a,false,false);assertNotNull(find(a.body,kind.equals("snapshot")?L.t("Renewing the saved snapshot link.","正在更新已保存快照的链接。"):L.t("Reopening. Waiting for your computer to provide a new address.","正在重开，等待电脑返回新地址。")));
                    for(String status:new String[]{"expired","stopped"}){
                        configure(a,"completed",status,URL,Long.MAX_VALUE,kind,false);assertActions(a,false,true);assertNotNull(find(a.body,L.t("This preview expired or stopped.","预览已失效或停止。")));
                        configure(a,"running",status,URL,Long.MAX_VALUE,kind,false);assertActions(a,false,false);
                        configure(a,"completed",status,"",0,kind,false);assertActions(a,false,false);
                        configure(a,"completed",status,URL,Long.MAX_VALUE,kind,true);assertActions(a,false,false);
                    }
                }
                configure(a,"completed","ready",URL,1,"snapshot",false);assertActions(a,false,true);
                configure(a,"completed","",URL,0,"live",false);assertActions(a,true,false);
                configure(a,"completed","unavailable",URL,0,"live",false);a.busy=true;a.render();View reopen=find(a.body,L.t("Reopen preview","重开预览"));assertNotNull(reopen);assertFalse(reopen.isEnabled());assertActions(a,false,true);assertSafe(a);
            });
        }
    }
    @Test public void deliverySurfaceGroupsResultAndInspectionAcrossThemesAndTextSizes()throws Exception{
        for(float scale:new float[]{1f,2f})for(String appearance:new String[]{"light","dark"})for(String language:new String[]{"en","zh"})try(ActivityScenario<DemoTaskPreviewActivity> scenario=launchSurface(language,scale,appearance)){
            for(String state:new String[]{"","unavailable","ready"}){
                scenario.onActivity(a->{assertEquals(scale,a.getResources().getConfiguration().fontScale,0.01f);assertEquals(320,a.getResources().getConfiguration().screenWidthDp);assertEquals(appearance.equals("dark"),Ui.dark);configure(a,"completed",state,state.equals("ready")?URL:"",state.equals("ready")?Long.MAX_VALUE:0,state.equals("ready")?"snapshot":"live",false);
                    TextView label=find(a.body,L.t("The result","交付结果")),summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report"))),preview=find(a.body,L.t("Preview","预览")),files=find(a.body,L.t("Screenshots & delivery files","截图与交付文件"));assertNotNull(label);assertNotNull(summary);assertNotNull(preview);assertNotNull(files);ViewGroup surface=(ViewGroup)label.getParent();assertNotSame("Reuse one result surface instead of the page root",a.body,surface);assertSame(a.body,surface.getParent());assertSame(surface,summary.getParent());assertSame(surface,preview.getParent());assertSame(surface,files.getParent());if(state.equals("ready"))assertSame(surface,find(a.body,L.t("Open preview","打开预览")).getParent());assertOutsideSurface(surface,find(a.body,L.t("Follow up","继续追问")));assertOutsideSurface(surface,find(a.body,L.t("Full report & evidence","完整报告与证据")));assertOutsideSurface(surface,find(a.body,L.t("Delete record & material","删除记录与材料")));assertActions(a,state.equals("ready"),false);});frames(scenario);
                String detail=state.equals("ready")?(language.equals("zh")?"本次交付的固定快照。":"Fixed snapshot from this handoff."):state.isEmpty()?(language.equals("zh")?"本次交办未附预览，可查看下方的截图与交付文件。":"No preview is attached to this handoff. Check screenshots and delivery files below."):(language.equals("zh")?"预览暂不可用，可查看下方的截图与交付文件。":"A preview isn't available right now. Check screenshots and delivery files below.");
                scenario.onActivity(a->{TextView summary=find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")));summary.requestRectangleOnScreen(new Rect(0,0,summary.getWidth(),summary.getHeight()),true);});frames(scenario);scenario.onActivity(a->assertCompleteAndVisible(a,find(a.body,TaskPresentation.resultSummary(a.sample.optString("report")))));
                if(scale==1f){
                    scenario.onActivity(a->{TextView preview=find(a.body,L.t("Preview","预览")),followup=find(a.body,L.t("Follow up","继续追问"));Rect region=new Rect();preview.getDrawingRect(region);a.body.offsetDescendantRectToMyCoords(preview,region);Rect end=new Rect();followup.getDrawingRect(end);a.body.offsetDescendantRectToMyCoords(followup,end);region.union(end);a.body.requestRectangleOnScreen(region,true);});frames(scenario);InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);
                    scenario.onActivity(a->{for(String target:new String[]{L.t("Preview","预览"),detail,L.t("Screenshots & delivery files","截图与交付文件"),L.t("Follow up","继续追问")})assertCompleteAndVisible(a,find(a.body,target));if(state.equals("ready"))assertCompleteAndVisible(a,find(a.body,L.t("Open preview","打开预览")));assertSafe(a);});captureSurface(language,state.isEmpty()?"missing":state,appearance);
                }else{
                    String[] targets=state.equals("ready")?new String[]{detail,language.equals("zh")?"打开预览":"Open preview",language.equals("zh")?"截图与交付文件":"Screenshots & delivery files",language.equals("zh")?"继续追问":"Follow up"}:new String[]{detail,language.equals("zh")?"截图与交付文件":"Screenshots & delivery files",language.equals("zh")?"继续追问":"Follow up"};
                    for(String target:targets){scenario.onActivity(a->{TextView text=find(a.body,target);assertNotNull(text);text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);});frames(scenario);scenario.onActivity(a->{TextView text=find(a.body,target);assertCompleteAndVisible(a,text);if(text instanceof Button){assertTrue(text.getWidth()>=Ui.dp(a,48));assertTrue(text.getHeight()>=Ui.dp(a,48));assertTrue(text.isEnabled());}assertActions(a,state.equals("ready"),false);});}
                }
            }
        }
    }
    static ActivityScenario<DemoTaskPreviewActivity> launchSurface(String language,float scale,String appearance){Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Only run in debug package",context.getPackageName().endsWith(".debug"));assertTrue(scale==1f||scale==2f);return ActivityScenario.launch(new Intent(context,scale==1f?DemoTaskPreviewNormalActivity.class:DemoTaskPreviewActivity.class).putExtra("language",language).putExtra("appearance",appearance));}
    static void assertOutsideSurface(ViewGroup surface,View text){assertNotNull(text);android.view.ViewParent ancestor=text.getParent();while(ancestor instanceof View){assertNotSame("Follow-up, report and deletion stay outside this delivery",surface,ancestor);ancestor=ancestor.getParent();}}
    static ActivityScenario<DemoTaskPreviewActivity> launch(String language){Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue("Only run in debug package",context.getPackageName().endsWith(".debug"));return ActivityScenario.launch(new Intent(context,DemoTaskPreviewActivity.class).putExtra("language",language));}
    static ActivityScenario<DemoTaskPreviewActivity> launch(String language,float scale){assertEquals(1f,scale,0f);Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));return ActivityScenario.launch(new Intent(context,DemoTaskPreviewNormalActivity.class).putExtra("language",language));}
    static void configure(DemoTaskPreviewActivity a,String taskState,String previewState,String url,long expires,String kind,boolean cancelled){
        try{a.sample.put("status",taskState).put("preview_status",previewState).put("preview_url",url).put("preview_expires_at",expires).put("preview_kind",kind).put("preview_version","sample-version").put("cancel_requested",cancelled?1:0);a.busy=false;a.render();assertSafe(a);}catch(Exception error){throw new AssertionError(error);}
    }
    static void assertActions(DemoTaskPreviewActivity a,boolean open,boolean reopen){
        assertEquals(open,find(a.body,L.t("Open preview","打开预览"))!=null);assertEquals(reopen,find(a.body,L.t("Reopen preview","重开预览"))!=null);
        TextView primary=open?find(a.body,L.t("Open preview","打开预览")):Store.finished(a.sample.optString("status"))?find(a.body,L.t("Screenshots & delivery files","截图与交付文件")):null;
        assertEquals("One primary delivery action, or none while no result is available",primary==null?0:1,primaryCount(a.body));
        if(primary!=null){assertTrue(primary instanceof Button);assertEquals(Ui.ON_LIME,primary.getTextColors().getColorForState(new int[]{android.R.attr.state_enabled},0));assertEquals(Ui.dp(a,2),primary.getElevation(),0.01f);assertEquals(!a.busy,primary.isEnabled());}
        TextView followup=find(a.body,L.t("Follow up","继续追问"));if(followup!=null){assertTrue(followup instanceof Button);assertEquals(Ui.TEXT,followup.getTextColors().getColorForState(new int[]{android.R.attr.state_enabled},0));assertEquals(0f,followup.getElevation(),0f);assertEquals(!a.busy,followup.isEnabled());}assertSafe(a);
    }
    static int primaryCount(View view){int count=view instanceof Button&&((Button)view).getTextColors().getColorForState(new int[]{android.R.attr.state_enabled},0)==Ui.ON_LIME?1:0;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)count+=primaryCount(((ViewGroup)view).getChildAt(n));return count;}
    static void assertFiles(DemoTaskPreviewActivity a){TextView files=find(a.body,L.t("Screenshots & delivery files","截图与交付文件"));assertNotNull(files);assertTrue(files instanceof Button);assertTrue(files.isEnabled());}
    static void assertSafe(DemoTaskPreviewActivity a){assertEquals("No preference/file/service/navigation/API access",0,a.forbiddenActions.get());assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertTrue(a.thumbnailRequested);assertNull(a.thumbnail);}
    static TextView find(View view,String text){if(view instanceof TextView){String actual=((TextView)view).getText().toString();if(actual.equals(text)||actual.equals(text+"\n"))return (TextView)view;}if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=find(((ViewGroup)view).getChildAt(n),text);if(found!=null)return found;}return null;}
    static void frames(ActivityScenario<DemoTaskPreviewActivity> scenario)throws Exception{CountDownLatch frames=new CountDownLatch(1);scenario.onActivity(a->{a.body.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(a.hasWindowFocus()&&a.body.getWidth()>0&&a.body.getAlpha()==1f&&a.body.getTranslationY()==0f){a.body.getViewTreeObserver().removeOnPreDrawListener(this);a.body.postOnAnimation(()->a.body.postOnAnimation(frames::countDown));}else a.body.postInvalidateOnAnimation();return true;}});a.body.invalidate();});assertTrue("Wait for the real window, settled entrance and two native frames",frames.await(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->{assertTrue(a.hasWindowFocus());assertEquals(1f,a.body.getAlpha(),0f);assertEquals(0f,a.body.getTranslationY(),0f);});}
    static void assertCompleteAndVisible(DemoTaskPreviewActivity a,TextView text){
        Layout layout=text.getLayout();assertNotNull(layout);assertEquals(text.length(),layout.getLineEnd(layout.getLineCount()-1));for(int line=0;line<layout.getLineCount();line++)assertEquals(0,layout.getEllipsisCount(line));assertTrue(text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());
        Rect local=new Rect(),global=new Rect();Point origin=new Point();assertTrue(text.getLocalVisibleRect(local));assertEquals(new Rect(0,0,text.getWidth(),text.getHeight()),local);assertTrue(text.getGlobalVisibleRect(global,origin));global.offset(-origin.x,-origin.y);assertEquals(local,global);
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();a.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);Rect intersection=new Rect(0,0,metrics.widthPixels,metrics.heightPixels),bounds=screenBounds(text);View scroll=(View)a.body.getParent().getParent();Rect viewport=screenBounds(scroll);viewport.left+=scroll.getPaddingLeft();viewport.top+=scroll.getPaddingTop();viewport.right-=scroll.getPaddingRight();viewport.bottom-=scroll.getPaddingBottom();assertTrue(intersection.intersect(viewport));assertTrue(intersection.intersect(bounds));assertEquals("Entire text/control fits the actual scroll and physical screen",bounds,intersection);
    }
    static Rect screenBounds(View view){int[] at=new int[2];view.getLocationOnScreen(at);return new Rect(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());}
    static void capture(String language,String state)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        String series=InstrumentationRegistry.getArguments().getString("captureSeries","baseline");assertTrue("Known diagnostic capture directory only",series.equals("baseline")||series.equals("hierarchy")||series.equals("surface"));if(series.equals("surface"))return;Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/task-preview-"+series);assertTrue(directory.isDirectory()||directory.mkdirs());Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
        try(FileOutputStream out=new FileOutputStream(new File(directory,"task-preview-"+language+"-"+state+"-normal.png"))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}finally{screenshot.recycle();}
    }
    static void captureSurface(String language,String state,String appearance)throws Exception{
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;assertEquals("New surface PNGs use a separate evidence series","surface",InstrumentationRegistry.getArguments().getString("captureSeries"));Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/task-preview-surface");assertTrue(directory.isDirectory()||directory.mkdirs());Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
        try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(new File(directory,"task-preview-"+language+"-"+state+"-"+appearance+"-normal.png"))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}}finally{screenshot.recycle();}
    }
}
