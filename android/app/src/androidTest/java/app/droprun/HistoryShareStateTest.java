package app.droprun;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Five fixed windows. Real history callbacks and project-search rendering; no business/IME/capture actions. */
public class HistoryShareStateTest {
    static boolean unresolvedLifetime;
    @Test public void actualHistoryReadKeepsUnknownEmptyAndFailureDistinct()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("historyShareProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=nonce();assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=context(DemoHistoryLoadStateActivity.class);float previous=DemoHistoryLoadStateActivity.fontScale;String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] palette=palette(),counts=new int[5];
        try{for(String config:new String[]{"en|light|1|empty","zh|dark|2|empty","en|light|1|cached"}){
            String[] part=config.split("\\|");boolean cached=part[3].equals("cached");ActivityScenario<DemoHistoryLoadStateActivity> scenario=null;DemoHistoryLoadStateActivity[] held={null};Throwable failure=null;int[] notifications={0};Boolean[] priorTouch={null};
            try{
                assertFalse(unresolvedLifetime);DemoHistoryLoadStateActivity.fontScale=Float.parseFloat(part[2]);unresolvedLifetime=true;event(nonce,"history",config,"launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(context,DemoHistoryLoadStateActivity.class).putExtra("historyLoadStateProbe",true).putExtra("nonce",nonce).putExtra("language",part[0]).putExtra("appearance",part[1]).putExtra("projectId",DemoHistoryLoadStateActivity.ID).putExtra("projectName","Memory history").putExtra("cached",cached));
                scenario.onActivity(a->{held[0]=a;priorTouch[0]=a.getWindow().getDecorView().isInTouchMode();event(nonce,"history",config,"returned-handle",data("prior_touch",priorTouch[0]));});historyReady(scenario,true);
                DataSetObserver observer=new DataSetObserver(){@Override public void onChanged(){notifications[0]++;}@Override public void onInvalidated(){notifications[0]++;}};
                scenario.onActivity(a->{safe(a,nonce,part);a.adapter.registerDataSetObserver(observer);counts[0]++;event(nonce,"history",config,"entered",history(a,counts,notifications[0]));});
                if(cached){
                    scenario.onActivity(a->a.list.setSelectionFromTop(1,-a.dp(8)));historyReady(scenario,true);
                    String[] anchor={null},cache={null};int[] top={0};View[] list={null},focus={null};Object[] adapter={null};
                    scenario.onActivity(a->{safe(a,nonce,part);assertEquals(3,a.rows.size());assertEquals(View.GONE,a.empty.getVisibility());anchor[0]=holder(a).id;top[0]=a.list.getChildAt(0).getTop();list[0]=a.list;adapter[0]=a.adapter;cache[0]=a.memory.history.toString();focus[0]=refresh(a);assertTrue(focus[0].requestFocusFromTouch());assertTrue(focus[0].hasFocus());event(nonce,"history",config,"pending",history(a,counts,notifications[0]));a.releaseRead();});historyReady(scenario,false);
                    scenario.onActivity(a->{safe(a,nonce,part);assertEquals(1,a.memoryReads.get());assertEquals(0,notifications[0]);assertSame(list[0],a.list);assertSame(adapter[0],a.adapter);assertEquals(anchor[0],holder(a).id);assertEquals(top[0],a.list.getChildAt(0).getTop());assertSame(focus[0],a.getWindow().getDecorView().findFocus());assertEquals(cache[0],a.memory.history.toString());assertEquals(View.GONE,a.empty.getVisibility());assertEquals(genericFailure(),a.readError);assertEquals(View.VISIBLE,a.notice.getVisibility());assertTrue(a.notice.isClickable());assertTrue(a.notice.isFocusable());assertTrue(a.notice.getHeight()>=a.dp(48));ShareEditorTest.assertCompleteText(a.notice);assertFullyVisible(a.notice);event(nonce,"history",config,"cached-null-failure",history(a,counts,notifications[0]));});
                }else{
                    scenario.onActivity(a->{safe(a,nonce,part);assertCopy(a,"Loading handoffs…","正在读取交办…","Checking this project's history.","正在读取这个项目的交办记录。");event(nonce,"history",config,"pending",history(a,counts,notifications[0]));a.releaseRead();});historyReady(scenario,false);
                    scenario.onActivity(a->{safe(a,nonce,part);assertEquals(1,a.memoryReads.get());assertEquals(0,notifications[0]);assertCopy(a,"History could not load","暂时无法读取历史","Use Refresh to try again when connected.","联网后点按刷新，重新读取历史。");assertEquals(part[0].equals("zh")?genericFailure():"Synthetic history read failure.",a.readError);assertEquals(View.VISIBLE,a.notice.getVisibility());assertTrue(a.notice.isClickable());event(nonce,"history",config,"read-failed",history(a,counts,notifications[0]));a.nextRead(1);assertTrue("Real Refresh accepts the memory retry",refresh(a).performClick());});historyReady(scenario,true);
                    scenario.onActivity(a->{assertCopy(a,"Loading handoffs…","正在读取交办…","Checking this project's history.","正在读取这个项目的交办记录。");event(nonce,"history",config,"retry-pending",history(a,counts,notifications[0]));a.releaseRead();});historyReady(scenario,false);
                    scenario.onActivity(a->{safe(a,nonce,part);assertEquals(2,a.memoryReads.get());assertEquals(0,notifications[0]);assertCopy(a,"Nothing here yet","这里还没有交办","Refresh when connected, or share something to this project.","联网后刷新，或先分享内容到这个项目。");assertEquals("",a.readError);assertEquals(View.GONE,a.notice.getVisibility());event(nonce,"history",config,"confirmed-zero",history(a,counts,notifications[0]));});
                    if(part[0].equals("en")){
                        scenario.onActivity(a->{assertFalse(a.paged);a.nextRead(2);a.cursor=DemoHistoryLoadStateActivity.CURSOR;a.load(true);});historyReady(scenario,true);
                        scenario.onActivity(a->{event(nonce,"history",config,"append-pending",history(a,counts,notifications[0]));a.releaseRead();});historyReady(scenario,false);
                        scenario.onActivity(a->{safe(a,nonce,part);assertEquals(3,a.memoryReads.get());assertFalse("Null-detail failed append must not become a successful page",a.paged);assertEquals(genericFailure(),a.readError);assertEquals(View.VISIBLE,a.notice.getVisibility());assertTrue(a.notice.isClickable());assertCopy(a,"History could not load","暂时无法读取历史","Use Refresh to try again when connected.","联网后点按刷新，重新读取历史。");assertEquals(0,notifications[0]);event(nonce,"history",config,"append-null-failed",history(a,counts,notifications[0]));});
                    }
                }
                scenario.onActivity(a->{safe(a,nonce,part);a.adapter.unregisterDataSetObserver(observer);counts[1]++;event(nonce,"history",config,"full-completed",history(a,counts,notifications[0]));});
            }catch(Throwable error){failure=error;throw error;}
            finally{closeHistory(scenario,held[0],failure,nonce,config,counts,notifications[0],priorTouch[0]);}
        }assertEquals(3,counts[0]);assertEquals(3,counts[1]);assertEquals(3,counts[2]);assertEquals(6,counts[4]);summary(nonce,"history",counts);
        }finally{if(!unresolvedLifetime){DemoHistoryLoadStateActivity.fontScale=previous;restore(languageBefore,darkBefore,palette);}}
    }

    @Test public void visibleProjectIdentityAndFullIdAreSearchable()throws Throwable{
        String mode=InstrumentationRegistry.getArguments().getString("historyShareProbe","");if(mode.isEmpty())return;assertEquals("accepted",mode);
        String nonce=nonce();assertFalse(unresolvedLifetime);assertFalse(TaskSyncService.running);
        Context context=context(DemoShareEditorActivity.class);float previous=DemoShareEditorActivity.hierarchyFontScale;String languageBefore=L.chinese()?"zh":"en";boolean darkBefore=Ui.dark;int[] palette=palette(),counts=new int[5];
        try{for(String config:new String[]{"en|light|1","zh|dark|2"}){
            String[] part=config.split("\\|");ActivityScenario<DemoShareEditorActivity> scenario=null;DemoShareEditorActivity[] held={null};Throwable failure=null;TextView[] search={null};String[] unchanged={null};Boolean[] priorTouch={null};
            try{
                assertFalse(unresolvedLifetime);DemoShareEditorActivity.hierarchyFontScale=Float.parseFloat(part[2]);unresolvedLifetime=true;event(nonce,"share",config,"launch-attempt",new JSONObject());
                scenario=ActivityScenario.launch(new Intent(context,DemoShareEditorActivity.class).putExtra("language",part[0]).putExtra("appearance",part[1]).putExtra("modelDisclosure",true));
                scenario.onActivity(a->{held[0]=a;priorTouch[0]=a.getWindow().getDecorView().isInTouchMode();event(nonce,"share",config,"returned-handle",data("prior_touch",priorTouch[0]));});shareReady(scenario);
                scenario.onActivity(a->{safe(a,part);try{JSONArray catalog=a.store.projects();while(catalog.length()>0)catalog.remove(0);for(String[] row:new String[][]{{"01234567A-mobile","Studio"},{"01234567B-desktop","Studio"},{"unique-lab","Lab"}})catalog.put(new JSONObject().put("id",row[0]).put("name",row[1]).put("available",true).put("permission",new JSONObject().put("enabled",true)));}catch(Exception error){throw new AssertionError(error);}a.selected="01234567B-desktop";a.query="";a.step=0;a.stage.removeAllViews();a.stage.addView(a.stepProject(),new ViewGroup.LayoutParams(-1,-2));search[0]=a.search;unchanged[0]=unchanged(a);counts[0]++;event(nonce,"share",config,"entered",share(a,counts));});shareReady(scenario);
                scenario.onActivity(a->{assertLabels(a,"Studio · 01234567B","Studio · 01234567A","Lab");safe(a,part);});
                String[] queries={"01234567A","studio · 01234567b","01234567A-mobile","  STUDIO  ","absent-memory-project",""};
                String[][] expected={{"Studio · 01234567A"},{"Studio · 01234567B"},{"Studio · 01234567A"},{"Studio · 01234567B","Studio · 01234567A"},{},{"Studio · 01234567B","Studio · 01234567A","Lab"}};
                for(int n=0;n<queries.length;n++){
                    final int query=n;scenario.onActivity(a->a.search.setText(queries[query]));shareReady(scenario);
                    scenario.onActivity(a->{safe(a,part);assertLabels(a,expected[query]);assertEquals(queries[query],a.query);assertEquals(queries[query],a.search.getText().toString());assertSame(search[0],a.search);assertEquals(unchanged[0],unchanged(a));assertTrue(a.search.isEnabled());assertTrue(a.search.isFocusable());if(!queries[query].isEmpty()){assertEquals(View.VISIBLE,a.search.getVisibility());assertTrue(a.search.getHeight()>=a.dp(48));}if(expected[query].length==0)assertNotNull(ShareEditorTest.findText(a.projectList,L.t("No matching projects","没有匹配的项目")));event(nonce,"share",config,"query-"+query,share(a,counts));});
                    if(expected[query].length==1){scenario.onActivity(a->{TextView label=ShareEditorTest.findText(a.projectList,expected[query][0]);assertNotNull(label);label.requestRectangleOnScreen(new Rect(0,0,label.getWidth(),label.getHeight()),true);});shareReady(scenario);scenario.onActivity(a->{TextView label=ShareEditorTest.findText(a.projectList,expected[query][0]);ShareEditorTest.assertCompleteText(label);assertFullyVisible(label);});}
                }
                scenario.onActivity(a->{safe(a,part);counts[1]++;event(nonce,"share",config,"full-completed",share(a,counts));});
            }catch(Throwable error){failure=error;throw error;}
            finally{closeShare(scenario,held[0],failure,nonce,config,counts,priorTouch[0]);}
        }assertEquals(2,counts[0]);assertEquals(2,counts[1]);assertEquals(2,counts[2]);summary(nonce,"share",counts);
        }finally{if(!unresolvedLifetime){DemoShareEditorActivity.hierarchyFontScale=previous;restore(languageBefore,darkBefore,palette);}}
    }

    static String nonce(){String value=InstrumentationRegistry.getArguments().getString("historyShareNonce","");assertTrue(value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));return value;}
    static Context context(Class<?> fixture)throws Exception{Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));assertFalse(context.getPackageManager().getActivityInfo(new ComponentName(context,fixture),0).exported);return context;}
    static String genericFailure(){return L.t("Couldn't refresh history. Use Refresh to try again.","暂时无法刷新历史，请点按刷新重试。");}
    static TextView title(DemoHistoryLoadStateActivity a){return (TextView)a.empty.getChildAt(0);}static TextView detail(DemoHistoryLoadStateActivity a){return (TextView)a.empty.getChildAt(1);}
    static void assertCopy(DemoHistoryLoadStateActivity a,String en,String zh,String enDetail,String zhDetail){assertEquals(View.VISIBLE,a.empty.getVisibility());assertEquals(L.t(en,zh),title(a).getText().toString());assertEquals(L.t(enDetail,zhDetail),detail(a).getText().toString());ShareEditorTest.assertCompleteText(title(a));ShareEditorTest.assertCompleteText(detail(a));assertFullyVisible(title(a));assertFullyVisible(detail(a));}
    static ProjectHistoryActivity.Holder holder(DemoHistoryLoadStateActivity a){assertTrue(a.list.getChildAt(0).getTag() instanceof ProjectHistoryActivity.Holder);return (ProjectHistoryActivity.Holder)a.list.getChildAt(0).getTag();}
    static View refresh(DemoHistoryLoadStateActivity a){View view=HistoryIdentityTest.findDescription(a.getWindow().getDecorView(),L.t("Refresh project history","刷新项目历史"));assertNotNull(view);assertTrue(view.isClickable());assertTrue(view.isFocusable());assertTrue(view.getHeight()>=a.dp(48));assertTrue(view.getWidth()>=a.dp(48));assertFullyVisible(view);return view;}
    static void safe(DemoHistoryLoadStateActivity a,String nonce,String[] config){assertEquals(nonce,a.nonce);assertEquals(0,a.forbiddenActions.get());assertEquals(config[0],L.chinese()?"zh":"en");assertEquals(config[1].equals("dark"),Ui.dark);assertEquals(Float.parseFloat(config[2]),a.getResources().getConfiguration().fontScale,0f);assertEquals(DemoHistoryLoadStateActivity.ID,a.projectId);assertNull(a.removal);assertTrue(a.pendingIds.isEmpty());assertFalse(TaskSyncService.running);refresh(a);}
    static void safe(DemoShareEditorActivity a,String[] config){ShareEditorTest.assertSafe(a);assertEquals(0,a.forbiddenActions);assertEquals(0,a.checkpointAttempts);assertEquals(config[0],L.chinese()?"zh":"en");assertEquals(config[1].equals("dark"),Ui.dark);assertEquals(Float.parseFloat(config[2]),a.getResources().getConfiguration().fontScale,0f);assertNull(a.dialog);assertFalse(a.busy);assertFalse(TaskSyncService.running);}
    static String unchanged(DemoShareEditorActivity a){return a.store.projects()+"|"+a.selected+"|"+a.model+"|"+a.effort+"|"+a.draft+"|"+a.shared+"|"+a.attachments+"|"+a.showAll;}
    static List<String> labels(DemoShareEditorActivity a){List<String> labels=new ArrayList<>();for(int n=0;n<a.projectList.getChildCount();n++){View row=a.projectList.getChildAt(n);if(row instanceof ViewGroup&&row.isClickable()){String description=String.valueOf(row.getContentDescription()),label=null;for(String known:new String[]{"Studio · 01234567A","Studio · 01234567B","Lab"})if(description.startsWith(known)){label=known;break;}assertNotNull(label);labels.add(label);}}return labels;}
    static void assertLabels(DemoShareEditorActivity a,String... expected){assertArrayEquals(expected,labels(a).toArray(new String[0]));for(int n=0;n<a.projectList.getChildCount();n++){View row=a.projectList.getChildAt(n);if(row instanceof ViewGroup&&row.isClickable()){String label=null;for(String known:expected)if(String.valueOf(row.getContentDescription()).startsWith(known)){label=known;break;}assertNotNull(label);TextView text=ShareEditorTest.findText(row,label);assertNotNull(text);ShareEditorTest.assertCompleteText(text);assertTrue(row.getHeight()>=a.dp(48));assertTrue(row.isFocusable());}}}
    static void assertFullyVisible(View view){Rect visible=new Rect(),safe=new Rect();assertTrue(view.getGlobalVisibleRect(visible));view.getWindowVisibleDisplayFrame(safe);Rect bounds=ShareSettingsHierarchyTest.bounds(view);assertEquals(bounds,visible);assertTrue(safe.contains(bounds));}
    static void historyReady(ActivityScenario<DemoHistoryLoadStateActivity> scenario,boolean pending)throws Exception{long end=SystemClock.elapsedRealtime()+3000;do{boolean[] ready={false};scenario.onActivity(a->{ready[0]=a.hasWindowFocus()&&!a.getWindow().getDecorView().isLayoutRequested()&&(a.list.getVisibility()!=View.VISIBLE||!a.list.isLayoutRequested())&&(a.empty.getVisibility()!=View.VISIBLE||!a.empty.isLayoutRequested())&&a.busy==pending&&(pending?a.read.entered.getCount()==0:a.read.finished.getCount()==0);if(!pending&&ready[0])a.handler.removeCallbacks(a.refresh);});if(ready[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("History memory callback/layout did not settle");}
    static void shareReady(ActivityScenario<DemoShareEditorActivity> scenario)throws Exception{long end=SystemClock.elapsedRealtime()+3000;do{boolean[] ready={false};scenario.onActivity(a->ready[0]=a.hasWindowFocus()&&!a.getWindow().getDecorView().isLayoutRequested()&&!a.stage.isLayoutRequested()&&!a.sheet.isLayoutRequested());if(ready[0])return;Thread.sleep(20);}while(SystemClock.elapsedRealtime()<end);fail("Share memory project layout did not settle");}
    static void closeHistory(ActivityScenario<DemoHistoryLoadStateActivity> scenario,DemoHistoryLoadStateActivity a,Throwable failure,String nonce,String config,int[] counts,int notifications,Boolean priorTouch)throws Throwable{
        if(scenario==null)return;try{assertNotNull(a);a.releaseRead();scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertTrue(a.io.awaitTermination(3,TimeUnit.SECONDS));assertTrue(a.local.awaitTermination(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue(a.isDestroyed());assertFalse(a.foreground);assertFalse(a.handler.hasCallbacks(a.refresh));counts[2]++;counts[4]+=a.memoryReads.get();event(nonce,"history",config,"DESTROYED",history(a,counts,notifications));assertEquals(0,a.forbiddenActions.get());assertFalse(TaskSyncService.running);});assertNotNull(priorTouch);InstrumentationRegistry.getInstrumentation().setInTouchMode(priorTouch);event(nonce,"history",config,"touch-mode-restored",data("prior_touch",priorTouch,"setInTouchMode_returned",true));unresolvedLifetime=false;}catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static void closeShare(ActivityScenario<DemoShareEditorActivity> scenario,DemoShareEditorActivity a,Throwable failure,String nonce,String config,int[] counts,Boolean priorTouch)throws Throwable{
        if(scenario==null)return;try{assertNotNull(a);scenario.close();assertEquals(Lifecycle.State.DESTROYED,scenario.getState());assertTrue(a.io.awaitTermination(3,TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{assertTrue(a.isDestroyed());counts[2]++;event(nonce,"share",config,"DESTROYED",share(a,counts));assertEquals(0,a.forbiddenActions+a.networkAttempts+a.saveAttempts+a.pairingAttempts+a.submitAttempts+a.startAttempts);assertFalse(TaskSyncService.running);});assertNotNull(priorTouch);InstrumentationRegistry.getInstrumentation().setInTouchMode(priorTouch);event(nonce,"share",config,"touch-mode-restored",data("prior_touch",priorTouch,"setInTouchMode_returned",true));unresolvedLifetime=false;}catch(Throwable closeError){if(failure!=null)failure.addSuppressed(closeError);else throw closeError;}
    }
    static JSONObject history(DemoHistoryLoadStateActivity a,int[] counts,int notifications){counts[3]++;return data("memory_only",true,"forbidden_actions",a.forbiddenActions.get(),"memory_reads",a.memoryReads.get(),"busy",a.busy,"rows",a.rows.size(),"empty_visible",a.empty.getVisibility()==View.VISIBLE,"title",title(a).getText().toString(),"detail",detail(a).getText().toString(),"read_error",a.readError,"notice_visible",a.notice.getVisibility()==View.VISIBLE,"notice",a.notice.getText().toString(),"paged",a.paged,"cursor",a.cursor,"adapter_notifications",notifications,"destroyed",a.isDestroyed(),"io_terminated",a.io.isTerminated(),"local_terminated",a.local.isTerminated(),"service_running",TaskSyncService.running);}
    static JSONObject share(DemoShareEditorActivity a,int[] counts){counts[3]++;return data("memory_only",true,"forbidden_actions",a.forbiddenActions,"network",a.networkAttempts,"save",a.saveAttempts,"pair",a.pairingAttempts,"submit",a.submitAttempts,"start",a.startAttempts,"checkpoint_attempts",a.checkpointAttempts,"checkpoint_noops",a.checkpointNoops,"query",a.query,"selected",a.selected,"labels",new JSONArray(labels(a)),"destroyed",a.isDestroyed(),"io_terminated",a.io.isTerminated(),"service_running",TaskSyncService.running);}
    static JSONObject data(Object... pairs){JSONObject value=new JSONObject();try{for(int n=0;n<pairs.length;n+=2)value.put((String)pairs[n],pairs[n+1]);return value;}catch(Exception error){throw new AssertionError(error);}}
    static void event(String nonce,String kind,String config,String event,JSONObject value){Bundle status=new Bundle();status.putString("stream","HISTORY_SHARE_EVENT\t"+nonce+"\t"+kind+"\t"+config+"\t"+event+"\t"+value+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static void summary(String nonce,String kind,int[] counts){Bundle status=new Bundle();status.putString("stream","HISTORY_SHARE_SUMMARY\t"+nonce+"\t"+kind+"\t"+data("entered",counts[0],"full_completed",counts[1],"DESTROYED",counts[2],"actual_guard_snapshots",counts[3],"actual_memory_reads",counts[4],"note","UI memory only; no Store/cache/network/recovery/IME/performance claim")+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,status);}
    static int[] palette(){return new int[]{Ui.BG,Ui.SURFACE,Ui.SURFACE_2,Ui.SURFACE_3,Ui.LINE,Ui.LINE_STRONG,Ui.TEXT,Ui.MUTED,Ui.DIM,Ui.LIME_SOFT,Ui.LIME_LINE,Ui.ACCENT,Ui.DANGER,Ui.AMBER,Ui.SCRIM};}
    static void restore(String language,boolean dark,int[] p){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{L.language(language);Ui.dark=dark;Ui.BG=p[0];Ui.SURFACE=p[1];Ui.SURFACE_2=p[2];Ui.SURFACE_3=p[3];Ui.LINE=p[4];Ui.LINE_STRONG=p[5];Ui.TEXT=p[6];Ui.MUTED=p[7];Ui.DIM=p[8];Ui.LIME_SOFT=p[9];Ui.LIME_LINE=p[10];Ui.ACCENT=p[11];Ui.DANGER=p[12];Ui.AMBER=p[13];Ui.SCRIM=p[14];});}
}
