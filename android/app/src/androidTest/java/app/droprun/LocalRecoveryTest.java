package app.droprun;

import android.app.AlertDialog;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/** Runs real Views, Keystore and outbox files, with synthetic data and no live Relay. */
public class LocalRecoveryTest {
    Context context;

    @Before public void seed() throws Exception {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        String previousRelay=new Store(context).relay();
        assertTrue("Do not replace a developer's existing connection",previousRelay.isEmpty()||previousRelay.equals("https://preview.example.invalid"));
        try(ActivityScenario<DemoHomeActivity> fixture=ActivityScenario.launch(DemoHomeActivity.class)){}
        stopSync();
        Store store=new Store(context);
        assertEquals("https://preview.example.invalid",store.relay());
        for(JSONArray pending=store.pending();pending.length()>0;pending=store.pending())store.cancelPending(pending.getJSONObject(0).getString("id"));
    }

    @After public void cleanup() throws Exception {
        stopSync();
        Store store=new Store(context);
        assertEquals("https://preview.example.invalid",store.relay());
        JSONArray pending=store.pending();
        for(int n=0;n<pending.length();n++)store.cancelPending(pending.getJSONObject(n).getString("id"));
    }

    void stopSync(){context.stopService(new Intent(context,TaskSyncService.class));((JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();}

    @Test public void emptyHomeGuidanceIsNotTruncated() {
        Intent intent=new Intent(context,DemoHomeActivity.class).putExtra("empty",true);
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(intent)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                assertEquals(0,activity.list.getCount());
                android.view.View empty=activity.list.getEmptyView();
                android.widget.TextView hint=findText(empty,"Share a link, photo or video from another app. Pick a project. Its progress will find a home here.");
                assertNotNull("The empty state must explain the next action",hint);
                android.text.Layout layout=hint.getLayout();assertNotNull(layout);
                assertEquals(hint.length(),layout.getLineEnd(layout.getLineCount()-1));
                assertTrue("All guidance lines must be laid out, even at 200% font",hint.getHeight()>=layout.getHeight()+hint.getCompoundPaddingTop()+hint.getCompoundPaddingBottom());
                hint.requestRectangleOnScreen(new android.graphics.Rect(0,hint.getHeight()-1,hint.getWidth(),hint.getHeight()),true);
                android.graphics.Rect visible=new android.graphics.Rect();assertTrue(hint.getLocalVisibleRect(visible));
                assertTrue("The final line must be reachable by scrolling",visible.bottom>=hint.getHeight()&&visible.top<=hint.getCompoundPaddingTop()+layout.getLineTop(layout.getLineCount()-1));
            });
        }
    }

    static android.widget.TextView findText(android.view.View view,String text){
        if(view instanceof android.widget.TextView&&text.contentEquals(((android.widget.TextView)view).getText()))return (android.widget.TextView)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.widget.TextView found=findText(group.getChildAt(i),text);if(found!=null)return found;}}
        return null;
    }

    @Test public void keyboardCanActivateAProjectCard() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor destination=instrumentation.addMonitor(ProjectHistoryActivity.class.getName(),null,true);
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->focusDescription(activity,"Settings"));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->assertEquals("Studio website, 3 tasks · 5 dispatches, Delivered",activity.getCurrentFocus().getContentDescription().toString()));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);
            instrumentation.waitForIdleSync();assertEquals(1,destination.getHits());
        }finally{instrumentation.removeMonitor(destination);}
    }

    @Test public void keyboardCanActivateATaskCard() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor destination=instrumentation.addMonitor(TaskActivity.class.getName(),null,true);
        Intent intent=new Intent(context,ProjectHistoryActivity.class).putExtra("projectId","demo-studio").putExtra("projectName","Studio website");
        try(ActivityScenario<ProjectHistoryActivity> scenario=ActivityScenario.launch(intent)){
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->focusDescription(activity,"Back to projects"));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->{
                android.view.View focused=activity.getCurrentFocus();assertTrue(focused.isClickable());
                assertNotNull(findText(focused,"Give the homepage room to breathe"));
            });
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);
            instrumentation.waitForIdleSync();assertEquals(1,destination.getHits());
        }finally{instrumentation.removeMonitor(destination);}
    }

    static void focusDescription(android.app.Activity activity,String description){
        ArrayList<android.view.View> matches=new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(matches,description,android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        assertEquals(1,matches.size());assertTrue(matches.get(0).requestFocus());
    }

    @Test public void shareKeyboardSkipsDecorativeAndDuplicateStops() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Intent share=new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain");
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share)){
            scenario.onActivity(activity->{activity.selected="demo-studio";activity.go(1,1);});
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->{assertFalse(activity.sheet.isFocusable());assertFalse(activity.gauge.isFocusable());assertTrue(activity.note.requestFocus());});
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->assertTrue(activity.getCurrentFocus().getContentDescription().toString().startsWith("Model & effort,")));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->{
                assertTrue(activity.getCurrentFocus() instanceof android.widget.Button);
                assertEquals("Hand off to Codex",((android.widget.Button)activity.getCurrentFocus()).getText().toString());
                assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void followupDraftAndIdentitySurviveRecreationAndCancel() {
        Intent intent=new Intent(context,TaskActivity.class).putExtra("taskId",DemoFixture.TASK);
        String[] id={null};
        try(ActivityScenario<TaskActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{activity.followup();activity.followupInput.setText("Keep this draft after rotation");id[0]=activity.followupId;activity.expanded.add("Full report & evidence");});
            scenario.recreate();
            scenario.onActivity(activity->{
                assertTrue(activity.followupDialog.isShowing());
                assertEquals("Keep this draft after rotation",activity.followupInput.getText().toString());
                assertEquals(id[0],activity.followupId);
                assertTrue(activity.expanded.contains("Full report & evidence"));
                activity.followupDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                assertEquals(0,activity.store.pending().length());
            });
            scenario.recreate();
            scenario.onActivity(activity->{
                assertTrue(activity.followupDialog==null||!activity.followupDialog.isShowing());
                activity.followup();
                assertEquals("Keep this draft after rotation",activity.followupInput.getText().toString());
                assertEquals(id[0],activity.followupId);
            });
            scenario.onActivity(activity->{
                activity.followupInput.setText(" ");
                activity.followupDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                assertTrue(activity.followupDialog.isShowing());
                assertNotNull(activity.followupInput.getError());
                assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void interruptedUploadPersistsProgressAndRetriesOnlyMissingAttachment() throws Exception {
        LocalTransport first=new LocalTransport(context);first.failSecondUpload=true;
        JSONObject task=task();JSONArray files=new JSONArray();
        for(String name:new String[]{"one.txt","two.txt"}){
            File file=new File(first.instanceFiles(),name);Files.write(file.toPath(),name.getBytes(StandardCharsets.UTF_8));
            files.put(new JSONObject().put("path",file.getPath()).put("mime","text/plain").put("name",name));
        }
        task.put("localFiles",files);first.save(task);
        try{first.sync();fail("Interrupted upload must remain pending");}catch(IOException expected){assertEquals("Synthetic upload interrupted",expected.getMessage());}
        JSONObject saved=first.pending().getJSONObject(0);
        assertEquals(1,saved.getJSONArray("assets").length());
        assertEquals("Synthetic upload interrupted",saved.getString("sendError"));
        assertTrue(new File(files.getJSONObject(0).getString("path")).exists());
        LocalTransport restored=new LocalTransport(context);restored.sync();
        assertEquals(Collections.singletonList("two.txt"),restored.uploads);
        assertEquals(Collections.singletonList(task.getString("id")),restored.submissions);
        assertEquals(0,restored.pending().length());
        for(int n=0;n<files.length();n++)assertFalse(new File(files.getJSONObject(n).getString("path")).exists());
    }

    @Test public void dismissingShareWithANoteRequiresAnExplicitDiscard() {
        ShareActivity[] shown={null};
        Intent share=new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain");
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share)){
            scenario.onActivity(activity->{
                shown[0]=activity;
                activity.selected="demo-studio";activity.go(1,1);activity.note.setText("Do not lose this note");
                activity.close();assertTrue(activity.discardDialog.isShowing());assertFalse(activity.closing);
                activity.discardDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            scenario.onActivity(activity->{
                assertEquals("Do not lose this note",activity.note.getText().toString());assertFalse(activity.closing);
                assertEquals(0,activity.store.pending().length());
                activity.close();activity.discardDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue(shown[0].closing);assertEquals(0,shown[0].store.pending().length());
        }
    }

    @Test public void lostAcknowledgementKeepsSameIdAcrossRetry() throws Exception {
        LocalTransport first=new LocalTransport(context);first.loseAcknowledgement=true;
        JSONObject task=task();first.save(task);
        try{first.sync();fail("Missing acknowledgement must keep the retry copy");}catch(IOException expected){assertEquals("Synthetic acknowledgement lost",expected.getMessage());}
        assertEquals(1,first.pending().length());
        LocalTransport restored=new LocalTransport(context);restored.sync();
        assertEquals(first.submissions,restored.submissions);
        assertEquals(Collections.singletonList(task.getString("id")),restored.submissions);
        assertEquals(0,restored.pending().length());
    }

    @Test public void changingInstanceCannotReuseCredentialOrOutbox() throws Exception {
        Store original=new Store(context);original.save(task());String relay=original.relay,instance=original.instanceId;
        try{
            original.preferences.edit().putString("relay","https://second.example.invalid").putString("instanceId",UUID.randomUUID().toString()).commit();
            Store second=new Store(context);
            assertFalse(original.paired());assertFalse(second.paired());assertEquals(0,second.pending().length());
            try{original.api("/tasks","GET",null,null,null);fail("Stale screen must reject the changed connection before network access");}catch(IOException expected){assertTrue(expected.getMessage().contains("connection changed"));}
            try{second.credential();fail("New instance must not inherit the old token");}catch(IOException expected){assertTrue(expected.getMessage().contains("Connect"));}
        }finally{original.preferences.edit().putString("relay",relay).putString("instanceId",instance).commit();}
        Store restored=new Store(context);assertTrue(restored.paired());assertEquals(1,restored.pending().length());
    }

    @Test public void foreignOutboxRecordIsNeverRetaggedOrSent() throws Exception {
        LocalTransport store=new LocalTransport(context);String foreign=UUID.randomUUID().toString();
        JSONObject misplaced=task().put("instanceId",foreign),local=task();
        File misplacedFile=new File(store.outbox(),misplaced.getString("id")+".json");
        // Simulate a damaged/restored outbox entry, bypassing the normal save path.
        Files.write(misplacedFile.toPath(),misplaced.toString().getBytes(StandardCharsets.UTF_8));store.save(local);
        for(int attempt=0;attempt<2;attempt++){
            try{store.sync();fail("Foreign record must remain rejected");}catch(IOException expected){assertTrue(expected.getMessage().contains("another Relay"));}
            JSONObject retained=new JSONObject(new String(Files.readAllBytes(misplacedFile.toPath()),StandardCharsets.UTF_8));
            assertEquals(foreign,retained.getString("instanceId"));
            assertEquals(Collections.singletonList(local.getString("id")),store.submissions);
        }
    }

    JSONObject task() throws Exception {return new JSONObject().put("id",UUID.randomUUID().toString()).put("projectId","demo-studio").put("message","Local recovery fixture").put("content","Synthetic material").put("assets",new JSONArray()).put("localFiles",new JSONArray());}

    /** Replace only transport. Store.sync, persistence and cleanup remain production code. */
    static final class LocalTransport extends Store {
        final List<String> uploads=new ArrayList<>(),submissions=new ArrayList<>();
        boolean failSecondUpload,loseAcknowledgement;
        LocalTransport(Context context){super(context);}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename)throws Exception {
            if(path.equals("/uploads")){
                uploads.add(filename);
                if(failSecondUpload&&uploads.size()==2)throw new IOException("Synthetic upload interrupted");
                return new JSONObject().put("id",filename+"-asset");
            }
            if(path.equals("/tasks")&&method.equals("POST")){
                JSONObject task=new JSONObject(new String(body,StandardCharsets.UTF_8));submissions.add(task.getString("id"));
                if(loseAcknowledgement)throw new IOException("Synthetic acknowledgement lost");
                return new JSONObject().put("id",task.getString("id")).put("status","queued");
            }
            if(path.startsWith("/tasks"))return new JSONObject().put("tasks",new JSONArray()).put("syncCursor",1);
            if(path.equals("/projects/activity"))return new JSONObject().put("projects",new JSONArray());
            if(path.equals("/projects"))return new JSONObject().put("projects",new JSONArray());
            if(path.equals("/device/settings"))return new JSONObject().put("directExecution",false);
            throw new AssertionError("Unexpected fixture request: "+method+" "+path);
        }
    }
}
