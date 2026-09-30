package app.droprun;

import android.content.*;
import android.os.Bundle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Local journals, private ContentProvider bytes and native screens; no task submission. */
public class ShareDraftRecoveryTest {
    Context context;Store store;Set<String> original=new HashSet<>();byte[] source=new byte[65536];
    @Before public void before()throws Exception{
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));String relay=new Store(context).relay();assertTrue(relay.isEmpty()||relay.equals("https://preview.example.invalid"));
        context.stopService(new Intent(context,TaskSyncService.class));((android.app.job.JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();DemoFixture.seed(context,new Intent());store=new Store(context);assertEquals(0,store.pending().length());
        for(ShareDrafts.Entry entry:ShareDrafts.list(store))original.add(entry.scope+entry.id);
        Arrays.fill(source,(byte)71);Files.write(DemoImportProvider.file(context).toPath(),source);DemoImportProvider.opens.set(0);DemoImportProvider.firstChunk=new CountDownLatch(1);DemoImportProvider.resume=new CountDownLatch(0);
    }
    @After public void after()throws Exception{
        DemoImportProvider.resume.countDown();DemoFixture.seed(context,new Intent());store=new Store(context);
        for(ShareDrafts.Entry entry:ShareDrafts.list(store))if(!original.contains(entry.scope+entry.id))ShareDrafts.discard(store,entry);
        DemoImportProvider.file(context).delete();context.stopService(new Intent(context,TaskSyncService.class));((android.app.job.JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();assertEquals(0,store.pending().length());
    }
    Intent input(String path){return new Intent(context,ShareActivity.class).setAction(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_TEXT,"Local draft recovery fixture").putExtra(Intent.EXTRA_STREAM,android.net.Uri.parse("content://"+context.getPackageName()+".import-fixture/"+path));}
    Intent resume(String id){return new Intent(context,RecoveredShareActivity.class).putExtra("draftId",id).putExtra("draftScope",store.scope);}
    void receive(ActivityScenario<? extends ShareActivity> scenario)throws Exception{long end=android.os.SystemClock.elapsedRealtime()+5000;boolean[] ready={false};do{scenario.onActivity(a->ready[0]=!a.receiving);if(ready[0])return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);fail("Receive did not finish");}
    void flush()throws Exception{ShareImport.drafts.submit(()->{}).get(3,TimeUnit.SECONDS);}
    @Test public void endedPageCanRecoverNoteChoicesAndVerifiedBytes()throws Exception{
        String[] id={null};
        try(ActivityScenario<ShareActivity> page=ActivityScenario.launch(input("local-input"))){receive(page);page.onActivity(a->{id[0]=a.incoming.id;a.selected="demo-studio";a.go(1,1);a.note.setText("Keep the note after the page is gone");a.effort="high";a.checkpoint();});flush();page.onActivity(a->a.finish());}
        ShareDrafts.Entry entry=ShareDrafts.find(store,store.scope,id[0]);assertEquals("Keep the note after the page is gone",entry.read().getJSONObject("editor").getString("draft"));DemoImportProvider.file(context).delete();
        try(ActivityScenario<RecoveredShareActivity> page=ActivityScenario.launch(resume(id[0]))){receive(page);page.onActivity(a->{assertEquals(id[0],a.incoming.id);assertEquals("demo-studio",a.selected);assertEquals("high",a.effort);assertEquals("Keep the note after the page is gone",a.note.getText().toString());try{assertArrayEquals(source,Files.readAllBytes(a.incoming.file(0).toPath()));}catch(Exception e){throw new AssertionError(e);}a.close();assertTrue(a.discardDialog.isShowing());a.discardDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();assertFalse(a.closing);});}
        assertEquals(1,DemoImportProvider.opens.get());assertEquals(0,store.pending().length());
    }
    @Test public void duplicateEditorCannotAcquireOrDeleteTheSameImport()throws Exception{
        try(ActivityScenario<ShareActivity> page=ActivityScenario.launch(input("local-input"))){receive(page);page.onActivity(a->{assertThrows(IOException.class,()->ShareImport.open(a.store,a.getIntent(),a.incoming.id,a.store.scope,new Object()));assertThrows(IOException.class,()->ShareDrafts.discard(a.store,ShareDrafts.find(a.store,a.store.scope,a.incoming.id)));assertTrue(a.incoming.file(0).exists());});}
    }
    @Test public void unfinishedCopyContinuesAsOneOperationAfterPageEnds()throws Exception{
        DemoImportProvider.resume=new CountDownLatch(1);String[] id={null};ShareImport[] originalImport={null};
        try(ActivityScenario<ShareActivity> page=ActivityScenario.launch(input("paused-input"))){assertTrue(DemoImportProvider.firstChunk.await(5,TimeUnit.SECONDS));page.onActivity(a->{id[0]=a.incoming.id;originalImport[0]=a.incoming;a.finish();});}
        try(ActivityScenario<RecoveredShareActivity> page=ActivityScenario.launch(resume(id[0]))){page.onActivity(a->assertSame(originalImport[0],a.incoming));DemoImportProvider.resume.countDown();receive(page);page.onActivity(a->{assertNull(a.incoming.error);try{assertArrayEquals(source,Files.readAllBytes(a.incoming.file(0).toPath()));}catch(Exception e){throw new AssertionError(e);}});}
        assertEquals(1,DemoImportProvider.opens.get());
    }
    @Test public void anotherPairedInstanceCannotListOrOpenTheDraft()throws Exception{
        ShareImport draft=new ShareImport(store,input("local-input"),null,null);draft.run();
        try{store.preferences.edit().putString("instanceId","different-local-instance").commit();Store other=new Store(context);assertFalse(ShareDrafts.list(other).stream().anyMatch(e->e.id.equals(draft.id)));assertThrows(IOException.class,()->ShareDrafts.find(other,store.scope,draft.id));assertThrows(IOException.class,()->ShareDrafts.find(store,store.scope,draft.id));assertTrue(draft.file(0).exists());}finally{draft.cancel();DemoFixture.seed(context,new Intent());}
    }
    @Test public void lateEditorWritesCannotRecreateDiscardedOrTransferredJournals()throws Exception{
        ShareImport draft=new ShareImport(store,input("local-input"),null,null);draft.run();draft.checkpoint(new JSONObject().put("draft","Queued local note"));draft.cancel();flush();assertFalse(draft.directory.exists());
        ShareImport transferred=new ShareImport(store,new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Local transfer fixture"),null,null);transferred.run();try{transferred.checkpoint(new JSONObject().put("draft","Queued before transfer"));transferred.save(new JSONObject().put("projectId","demo-studio").put("content","Local only"));flush();assertFalse(transferred.journal.getBaseFile().exists());assertFalse(ShareDrafts.list(store).stream().anyMatch(e->e.id.equals(transferred.id)));}finally{store.cancelPending(transferred.id);}
    }
    @Test public void outboxOwnedTextJournalCannotReappearAfterLocalRemoval()throws Exception{
        ShareImport draft=new ShareImport(store,new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Synthetic interrupted transfer"),null,null);draft.run();store.save(new JSONObject().put("id",draft.id).put("projectId","demo-studio").put("content","Synthetic only"));try{assertFalse(ShareDrafts.list(store).stream().anyMatch(e->e.id.equals(draft.id)));store.cancelPending(draft.id);assertFalse(draft.journal.getBaseFile().exists());assertFalse(ShareDrafts.list(store).stream().anyMatch(e->e.id.equals(draft.id)));}finally{store.cancelPending(draft.id);draft.cancel();}
    }
    @Test public void homeEntryOpensDraftsAndDiscardRequiresConfirmation()throws Exception{
        ShareImport draft=new ShareImport(store,input("local-input"),null,null);draft.run();
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(ShareDraftsActivity.class.getName(),null,false);
        try(ActivityScenario<DemoHomeActivity> home=ActivityScenario.launch(DemoHomeActivity.class)){
            home.onActivity(a->{assertEquals(android.view.View.VISIBLE,a.draftsNotice.getVisibility());a.draftsNotice.performClick();});ShareDraftsActivity screen=(ShareDraftsActivity)monitor.waitForActivityWithTimeout(3000);assertNotNull(screen);
            instrumentation.runOnMainSync(()->{assertEquals(1,screen.content.getChildCount());assertEquals(2,screen.actions.size());screen.actions.get(1).performClick();});LocalRecoveryTest.clickWindowText("Keep");assertTrue(draft.file(0).exists());
            instrumentation.runOnMainSync(()->screen.actions.get(1).performClick());LocalRecoveryTest.clickWindowText("Discard");long end=android.os.SystemClock.elapsedRealtime()+3000;while(draft.directory.exists()&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(25);assertFalse(draft.directory.exists());instrumentation.runOnMainSync(screen::finish);
        }finally{instrumentation.removeMonitor(monitor);draft.cancel();}
    }
    @Test public void editorWriteFailureStaysVisibleUntilExplicitRetry()throws Exception{
        try(ActivityScenario<ShareActivity> page=ActivityScenario.launch(input("local-input"))){receive(page);flush();java.util.concurrent.atomic.AtomicBoolean fail=new java.util.concurrent.atomic.AtomicBoolean(true);
            page.onActivity(a->{a.selected="demo-studio";a.go(1,1);});flush();
            page.onActivity(a->{a.incoming.journal=new android.util.AtomicFile(a.incoming.journal.getBaseFile()){@Override public FileOutputStream startWrite()throws IOException{if(fail.getAndSet(false))throw new IOException("Synthetic draft write failure");return super.startWrite();}};a.note.setText("Keep the unsaved text for retry");});flush();
            page.onActivity(a->{assertNotNull(a.incoming.editorError);assertEquals(android.view.View.VISIBLE,a.retryDraft.getVisibility());assertTrue(a.draftStatus.getText().toString().contains("not saved"));a.retryDraft.performClick();});flush();
            page.onActivity(a->{assertNull(a.incoming.editorError);assertEquals(android.view.View.GONE,a.retryDraft.getVisibility());try{assertEquals("Keep the unsaved text for retry",ShareDrafts.find(a.store,a.store.scope,a.incoming.id).read().getJSONObject("editor").getString("draft"));}catch(Exception e){throw new AssertionError(e);}});
        }
    }
    @Test public void draftListReturnsToExistingEditorWithoutAnotherCopy()throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();ShareActivity[] originalPage={null};
        try(ActivityScenario<ShareActivity> page=ActivityScenario.launch(input("local-input"))){receive(page);page.onActivity(a->{originalPage[0]=a;a.selected="demo-studio";a.go(1,1);a.note.setText("Return to this editor");});
            try(ActivityScenario<ShareDraftsActivity> list=ActivityScenario.launch(ShareDraftsActivity.class)){list.onActivity(a->{assertEquals("Return to open share",a.actions.get(0).getText().toString());assertFalse(a.actions.get(1).isEnabled());a.actions.get(0).performClick();});
                long end=android.os.SystemClock.elapsedRealtime()+3000;boolean[] resumed={false};do{instrumentation.runOnMainSync(()->resumed[0]=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(originalPage[0])==androidx.test.runner.lifecycle.Stage.RESUMED);if(resumed[0])break;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);assertTrue("Return must bring back the existing editor",resumed[0]);page.onActivity(a->assertEquals("Return to this editor",a.note.getText().toString()));assertEquals(1,DemoImportProvider.opens.get());
            }
        }
    }
    @Test public void unreadableOwnedJournalCanBeExplicitlyDiscardedWithoutTouchingOtherFiles()throws Exception{
        ShareImport draft=new ShareImport(store,input("local-input"),null,null);draft.run();File untouched=new File(store.attachments(),"local-unrelated-draft-test-file");Files.write(untouched.toPath(),new byte[]{9});
        try{Files.write(draft.journal.getBaseFile().toPath(),"broken json".getBytes(java.nio.charset.StandardCharsets.UTF_8));ShareDrafts.Entry entry=ShareDrafts.find(store,store.scope,draft.id);assertThrows(JSONException.class,entry::read);ShareDrafts.discard(store,entry);assertFalse(draft.directory.exists());assertArrayEquals(new byte[]{9},Files.readAllBytes(untouched.toPath()));}finally{untouched.delete();draft.cancel();}
    }
    @Test public void prePairingDraftRequiresConfirmationBeforeChangingItsOwner()throws Exception{
        Store before=ShareDrafts.scoped(store,ShareImport.UNPAIRED);ShareImport draft=new ShareImport(before,input("local-input"),null,null);draft.run();draft.checkpoint(new JSONObject().put("draft","Saved before pairing"));flush();
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(RecoveredShareActivity.class.getName(),null,false);RecoveredShareActivity[] recovered={null};
        try(ActivityScenario<ShareDraftsActivity> list=ActivityScenario.launch(ShareDraftsActivity.class)){
            list.onActivity(a->a.actions.get(0).performClick());long windowDeadline=android.os.SystemClock.elapsedRealtime()+3000;boolean targetVisible=false;do{android.view.accessibility.AccessibilityNodeInfo window=instrumentation.getUiAutomation().getRootInActiveWindow();targetVisible=window!=null&&window.findAccessibilityNodeInfosByText("My Windows PC").stream().anyMatch(node->"My Windows PC\nhttps://preview.example.invalid".contentEquals(node.getText()));if(targetVisible)break;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<windowDeadline);assertTrue("Confirmation must identify the computer and Relay",targetVisible);LocalRecoveryTest.clickWindowText("Cancel");assertTrue(draft.directory.exists());assertEquals(0,monitor.getHits());
            list.onActivity(a->a.actions.get(0).performClick());LocalRecoveryTest.clickWindowText("Continue");recovered[0]=(RecoveredShareActivity)monitor.waitForActivityWithTimeout(3000);assertNotNull(recovered[0]);
            long end=android.os.SystemClock.elapsedRealtime()+5000;boolean[] ready={false};do{instrumentation.runOnMainSync(()->ready[0]=!recovered[0].receiving);if(ready[0])break;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);assertTrue(ready[0]);
            instrumentation.runOnMainSync(()->{assertEquals(store.scope,recovered[0].incoming.store.scope);assertEquals("Saved before pairing",recovered[0].draft);assertEquals(draft.id,recovered[0].incoming.id);assertNull(recovered[0].incoming.error);});assertFalse(draft.directory.exists());assertEquals(1,DemoImportProvider.opens.get());assertEquals(0,store.pending().length());
        }finally{if(recovered[0]!=null){instrumentation.runOnMainSync(recovered[0]::finish);long end=android.os.SystemClock.elapsedRealtime()+3000;while(recovered[0].incoming!=null&&recovered[0].incoming.hasOwner()&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(25);assertTrue("Finished editor must release draft ownership",recovered[0].incoming==null||!recovered[0].incoming.hasOwner());}instrumentation.removeMonitor(monitor);draft.cancel();}
    }
}
