package app.droprun;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;

/** ContentResolver copies using private synthetic providers, never a real handoff. */
public class ShareAttachmentTest {
    Context context;Store store;byte[] source;Set<String> before;
    @Before public void prepare()throws Exception{
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        String previous=new Store(context).relay();assertTrue(previous.isEmpty()||previous.equals("https://preview.example.invalid"));
        context.stopService(new Intent(context,TaskSyncService.class));((JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
        DemoFixture.seed(context,new Intent());store=new Store(context);assertEquals(0,store.pending().length());before=files();
        source=new byte[256*1024];for(int n=0;n<source.length;n++)source[n]=(byte)(n%251);Files.write(DemoImportProvider.file(context).toPath(),source);
        DemoImportProvider.opens.set(0);DemoImportProvider.firstChunk=new CountDownLatch(1);DemoImportProvider.resume=new CountDownLatch(0);
    }
    @After public void cleanup()throws Exception{DemoImportProvider.resume.countDown();DemoImportProvider.file(context).delete();awaitCleanup();}
    Set<String> files(){String[] names=store.attachments().list();return new HashSet<>(names==null?Collections.emptyList():Arrays.asList(names));}
    Intent share(String... paths){
        Intent intent=new Intent(context,DemoShareActivity.class).setType("application/octet-stream");ArrayList<Uri> uris=new ArrayList<>();
        for(String path:paths)uris.add(Uri.parse("content://"+context.getPackageName()+".import-fixture/"+path));
        if(uris.size()==1)intent.setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM,uris.get(0));
        else intent.setAction(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);return intent;
    }
    void awaitReceive(ActivityScenario<DemoShareActivity> scenario)throws Exception{
        long end=android.os.SystemClock.elapsedRealtime()+5000;boolean[] receiving={true};
        do{scenario.onActivity(a->receiving[0]=a.receiving);if(!receiving[0])return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);fail("Synthetic input did not finish");
    }
    void awaitCleanup()throws Exception{
        for(String id:files())if(!before.contains(id)&&ShareDrafts.uuid(id)){ShareImport live=ShareImport.live(id);if(live!=null)live.cancel();else ShareDrafts.discard(store,ShareDrafts.find(store,store.scope,id));}
        long end=android.os.SystemClock.elapsedRealtime()+5000;while(!files().equals(before)&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(25);assertEquals("Unsent copies are cleaned, preexisting files untouched",before,files());
    }
    void awaitSheet(ActivityScenario<DemoShareActivity> scenario)throws Exception{
        long end=android.os.SystemClock.elapsedRealtime()+3000;boolean[] settled={false};
        do{scenario.onActivity(a->settled[0]=a.holder.getAlpha()==1f&&a.holder.getTranslationY()==0f);if(settled[0])return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);fail("Share entrance animation did not finish");
    }
    @Test public void completeCopyRestoresWithoutReopeningSource()throws Exception{
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("local-input"))){
            awaitReceive(scenario);String[] path={null};scenario.onActivity(a->{try{assertEquals(1,a.attachments.length());path[0]=a.attachments.getJSONObject(0).getString("path");assertArrayEquals(source,Files.readAllBytes(new File(path[0]).toPath()));}catch(Exception e){throw new AssertionError(e);}});
            DemoImportProvider.file(context).delete();scenario.recreate();awaitReceive(scenario);
            scenario.onActivity(a->{try{assertEquals(path[0],a.attachments.getJSONObject(0).getString("path"));assertArrayEquals(source,Files.readAllBytes(new File(path[0]).toPath()));assertEquals(0,a.store.pending().length());}catch(Exception e){throw new AssertionError(e);}});assertEquals(1,DemoImportProvider.opens.get());
            // A completion posted before recreation can arrive after the new screen adopted it.
            scenario.onActivity(a->{a.selected="demo-studio";a.go(1,1);a.note.setText("Keep this note");a.importObserver.run();assertEquals(1,a.step);assertEquals("Keep this note",a.note.getText().toString());});
        }awaitCleanup();
    }
    @Test public void interruptedSecondCopyKeepsCompleteBytesUntilConfirmedDiscard()throws Exception{
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("local-input","failed-input"))){
            awaitReceive(scenario);scenario.onActivity(a->{try{assertEquals(0,a.attachments.length());assertEquals(-1,a.step);assertEquals(0,a.store.pending().length());assertEquals(1,a.incoming.completeCount());assertArrayEquals(source,Files.readAllBytes(a.incoming.file(0).toPath()));assertFalse(a.incoming.file(1).exists());assertNotNull(a.sheet.findViewWithTag("retry-import"));}catch(Exception e){throw new AssertionError(e);}});assertEquals(2,DemoImportProvider.opens.get());
            scenario.recreate();awaitReceive(scenario);assertEquals("Recreation does not retry a failed provider",2,DemoImportProvider.opens.get());
            awaitSheet(scenario);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{for(String tag:new String[]{"retry-import","discard-import"}){android.view.View action=a.sheet.findViewWithTag(tag);android.graphics.Rect visible=new android.graphics.Rect();assertTrue(action.getHeight()>0);assertTrue(action.getGlobalVisibleRect(visible));assertTrue("Failure actions stay fully visible without scrolling",visible.height()>=action.getHeight());}});
            scenario.onActivity(a->{a.sheet.findViewWithTag("discard-import").performClick();assertTrue(a.discardDialog.isShowing());a.discardDialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();assertFalse(a.closing);assertTrue(a.incoming.file(0).exists());a.sheet.findViewWithTag("discard-import").performClick();a.discardDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();});
        }awaitCleanup();
    }
    @Test public void recreationDuringCopyKeepsOnlyTheRestoredCopy()throws Exception{
        DemoImportProvider.resume=new CountDownLatch(1);
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("paused-input"))){
            assertTrue(DemoImportProvider.firstChunk.await(5,TimeUnit.SECONDS));scenario.recreate();DemoImportProvider.resume.countDown();awaitReceive(scenario);
            scenario.onActivity(a->{try{assertEquals(1,a.attachments.length());assertArrayEquals(source,Files.readAllBytes(new File(a.attachments.getJSONObject(0).getString("path")).toPath()));assertEquals(0,a.store.pending().length());}catch(Exception e){throw new AssertionError(e);}});assertEquals("Rotation keeps one receive operation",1,DemoImportProvider.opens.get());
        }finally{DemoImportProvider.resume.countDown();}awaitCleanup();
    }
    @Test public void receiveFailureRemainsReadableAndActionableInLandscape()throws Exception{
        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try(ActivityScenario<DemoHomeActivity> background=ActivityScenario.launch(DemoHomeActivity.class);ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("local-input","failed-input"))){
            awaitReceive(scenario);String[] id={null};scenario.onActivity(a->id[0]=a.incoming.id);
            try{for(int rotation:new int[]{android.app.UiAutomation.ROTATION_FREEZE_90,android.app.UiAutomation.ROTATION_FREEZE_0}){
                assertTrue(automation.setRotation(rotation));int orientation=rotation==android.app.UiAutomation.ROTATION_FREEZE_90?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT;
                long end=android.os.SystemClock.elapsedRealtime()+3000;boolean[] ready={false};do{scenario.onActivity(a->ready[0]=a.getResources().getConfiguration().orientation==orientation&&!a.holder.isLayoutRequested());if(ready[0])break;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);assertTrue("Wait for requested orientation and layout: "+orientation,ready[0]);awaitSheet(scenario);
                // Window-manager rotation runs outside the View animation checked by awaitSheet.
                automation.waitForIdle(100,3000);Thread.sleep(700);
                scenario.onActivity(a->a.scroll.scrollTo(0,0));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                android.graphics.Bitmap image=automation.takeScreenshot();assertNotNull(image);try(java.io.FileOutputStream output=new java.io.FileOutputStream(new File(context.getExternalFilesDir(null),"receive-failure-rotation-"+rotation+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));}finally{image.recycle();}
                scenario.onActivity(a->{assertEquals(id[0],a.incoming.id);assertEquals(1,a.incoming.completeCount());assertEquals(0,a.store.pending().length());assertTrue("Failure explanation needs a readable scrolling viewport; height="+a.scroll.getHeight(),a.scroll.getHeight()>=a.dp(72));
                    if(orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE){android.widget.TextView problem=a.receiveContent.findViewWithTag("receive-problem");android.graphics.Rect visible=new android.graphics.Rect();assertTrue("Show the cause before decoration on short screens",problem.getGlobalVisibleRect(visible));assertTrue(visible.height()>=problem.getLineHeight());}
                    for(String tag:new String[]{"retry-import","discard-import"}){android.view.View action=a.sheet.findViewWithTag(tag);android.graphics.Rect visible=new android.graphics.Rect();assertTrue(action.getGlobalVisibleRect(visible));assertTrue("Failure action must stay fully visible",visible.height()>=action.getHeight()&&visible.width()>=action.getWidth());}
                    a.scroll.fullScroll(android.view.View.FOCUS_DOWN);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{a.sheet.findViewWithTag("discard-import").performClick();assertTrue(a.discardDialog.isShowing());a.discardDialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();assertFalse(a.closing);assertTrue(a.incoming.file(0).exists());});
            }}finally{automation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);}
            assertEquals("Rotating a failure must not reopen providers",2,DemoImportProvider.opens.get());
        }awaitCleanup();
    }
    @Test public void journalRecoveryReusesCompletedCopyAndReplacesOnlyPartial()throws Exception{
        Intent intent=share("local-input","paused-input");ShareImport interrupted=new ShareImport(store,intent,null,null);
        // A durable complete receipt plus an unreceipted partial is the killed-process state.
        Files.write(interrupted.file(0).toPath(),source);Files.write(interrupted.file(1).toPath(),Arrays.copyOf(source,4096));
        interrupted.record.getJSONArray("files").put(new JSONObject().put("name","first.bin").put("mime","application/octet-stream").put("size",source.length).put("sha256",ShareImport.digest(interrupted.file(0))).put("complete",true));interrupted.write();
        ShareImport recovered=new ShareImport(store,intent,interrupted.id,store.scope);
        try{
            recovered.run();assertNull(recovered.error);assertEquals(2,recovered.attachments().length());assertEquals("Only unfinished source reopened",1,DemoImportProvider.opens.get());
            assertArrayEquals(source,Files.readAllBytes(recovered.file(0).toPath()));assertArrayEquals(source,Files.readAllBytes(recovered.file(1).toPath()));
            assertEquals(interrupted.file(1),recovered.file(1));assertEquals(0,store.pending().length());
        }finally{recovered.cancel();}awaitCleanup();
    }
    @Test public void journalRejectsChangedSourceAndDifferentInstance()throws Exception{
        Intent intent=share("local-input");ShareImport original=new ShareImport(store,intent,null,null);original.run();assertNull(original.error);
        try{
            assertThrows(java.io.IOException.class,()->new ShareImport(store,share("failed-input"),original.id,store.scope));
            Store other=new Store(context);other.select("https://preview.example.invalid","other-local-fixture");
            assertThrows(java.io.IOException.class,()->new ShareImport(other,intent,original.id,store.scope));
            assertArrayEquals(source,Files.readAllBytes(original.file(0).toPath()));assertEquals(1,DemoImportProvider.opens.get());
        }finally{original.cancel();}awaitCleanup();
    }
    @Test public void closingAfterLocalOutboxTransferPreservesPendingBytes()throws Exception{
        Intent intent=share("local-input");ShareImport original=new ShareImport(store,intent,null,null);original.run();assertNull(original.error);
        try{
            byte[] receipt=original.journal.readFully();
            original.save(new JSONObject().put("projectId","demo-studio").put("content","Local ownership test only").put("assets",new JSONArray()));
            Files.write(original.journal.getBaseFile().toPath(),receipt); // persisted outbox, interrupted journal removal
            original.cancel();assertEquals(1,store.pending().length());assertArrayEquals(source,Files.readAllBytes(original.file(0).toPath()));
            ShareImport recovered=new ShareImport(store,intent,original.id,store.scope);assertTrue(recovered.transferred);recovered.cancel();
            assertFalse(recovered.journal.getBaseFile().exists());
            assertArrayEquals(source,Files.readAllBytes(original.file(0).toPath()));
            Files.write(original.journal.getBaseFile().toPath(),receipt); // cancellation also handles this crash window
        }finally{store.cancelPending(original.id);}awaitCleanup();
    }
    @Test public void firstPairingMovesOnlyThisUnpairedDraft()throws Exception{
        Store unpaired=new Store(context);unpaired.select("","");Intent intent=share("local-input");ShareImport original=new ShareImport(unpaired,intent,null,null);original.run();assertNull(original.error);File previous=original.directory;
        try{
            original.bind(store);assertFalse(previous.exists());assertArrayEquals(source,Files.readAllBytes(original.file(0).toPath()));
            ShareImport recovered=new ShareImport(store,intent,original.id,ShareImport.UNPAIRED);recovered.run();assertNull(recovered.error);assertEquals(1,DemoImportProvider.opens.get());recovered.cancel();
        }finally{original.cancel();}awaitCleanup();
    }
    @Test public void modifiedCompleteCopyFailsVerificationWithoutReopeningSource()throws Exception{
        Intent intent=share("local-input");ShareImport original=new ShareImport(store,intent,null,null);original.run();assertNull(original.error);
        byte[] changed=source.clone();changed[100]^=1;Files.write(original.file(0).toPath(),changed);
        ShareImport recovered=new ShareImport(store,intent,original.id,store.scope);recovered.run();assertNotNull(recovered.error);assertEquals(0,recovered.completeCount());assertFalse(recovered.file(0).exists());assertEquals(1,DemoImportProvider.opens.get());
        try{assertTrue(recovered.retry());recovered.run();assertNull(recovered.error);assertArrayEquals(source,Files.readAllBytes(recovered.file(0).toPath()));}finally{recovered.cancel();}awaitCleanup();
    }
    @Test public void closingDuringCopyCleansItsOwnedPartial()throws Exception{
        DemoImportProvider.resume=new CountDownLatch(1);
        File[] owned={null};try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("paused-input"))){assertTrue(DemoImportProvider.firstChunk.await(5,TimeUnit.SECONDS));scenario.onActivity(a->{owned[0]=a.incoming.directory;a.close();});}
        finally{DemoImportProvider.resume.countDown();}long end=android.os.SystemClock.elapsedRealtime()+3000;while(owned[0].exists()&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(25);assertFalse("Explicit Close discards without test cleanup",owned[0].exists());awaitCleanup();assertEquals(0,store.pending().length());
    }
    @Test public void independentSharesDoNotCleanEachOthersCopies()throws Exception{
        Intent intent=share("local-input");ShareImport first=new ShareImport(store,intent,null,null),second=new ShareImport(store,intent,null,null);
        try{first.run();second.run();assertNull(first.error);assertNull(second.error);first.cancel();assertArrayEquals(source,Files.readAllBytes(second.file(0).toPath()));}
        finally{first.cancel();second.cancel();}awaitCleanup();
    }
    @Test public void retryReadsOnlyMissingMaterialAndIgnoresRepeatedTap()throws Exception{
        File gate=new File(context.getCacheDir(),"synthetic-import.fail"),first=new File(context.getCacheDir(),"synthetic-import-first.bin");Files.write(first.toPath(),source);assertTrue(gate.createNewFile());
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("first-input","flaky-input"))){
            awaitReceive(scenario);assertEquals(2,DemoImportProvider.opens.get());scenario.onActivity(a->assertNotNull(a.incoming.error));assertTrue(first.delete());assertTrue(gate.delete());
            scenario.onActivity(a->{android.view.View retry=a.sheet.findViewWithTag("retry-import");assertNotNull(retry);retry.performClick();retry.performClick();assertTrue(a.receiving);});awaitReceive(scenario);
            scenario.onActivity(a->{try{assertNull(a.incoming.error);assertEquals(0,a.step);assertEquals(2,a.attachments.length());assertArrayEquals(source,Files.readAllBytes(a.incoming.file(0).toPath()));assertArrayEquals(source,Files.readAllBytes(a.incoming.file(1).toPath()));assertEquals(0,a.store.pending().length());}catch(Exception e){throw new AssertionError(e);}});assertEquals(3,DemoImportProvider.opens.get());
        }finally{gate.delete();first.delete();}awaitCleanup();
    }
    @Test public void persistedFailureIsVerifiedWithoutAutomaticProviderRetry()throws Exception{
        Intent intent=share("local-input","failed-input");ShareImport original=new ShareImport(store,intent,null,null);original.run();assertNotNull(original.error);assertEquals(1,original.completeCount());assertEquals(2,DemoImportProvider.opens.get());
        ShareImport recovered=new ShareImport(store,intent,original.id,store.scope);
        try{recovered.run();assertNotNull(recovered.error);assertEquals(1,recovered.failureIndex);assertEquals(1,recovered.completeCount());assertEquals(2,DemoImportProvider.opens.get());assertTrue(recovered.retry());assertFalse(recovered.retry());recovered.run();assertNotNull(recovered.error);assertEquals(3,DemoImportProvider.opens.get());assertArrayEquals(source,Files.readAllBytes(recovered.file(0).toPath()));}
        finally{recovered.cancel();}awaitCleanup();
    }
    @Test public void sourceRevocationKeepsCompleteMaterialAndHonestFailure()throws Exception{
        Intent intent=share("local-input","revoked-input");ShareImport original=new ShareImport(store,intent,null,null);
        try{original.run();assertNotNull(original.error);assertEquals("source",((ShareImport.ReceiveFailure)original.error).kind);assertEquals(1,original.completeCount());assertFalse(original.file(1).exists());assertArrayEquals(source,Files.readAllBytes(original.file(0).toPath()));assertThrows(java.io.IOException.class,()->original.save(new JSONObject()));assertEquals(0,store.pending().length());}
        finally{original.cancel();}awaitCleanup();
    }
    @Test public void incompleteReceiptIsNotReadyAndDoesNotHideOtherValidCopies()throws Exception{
        Intent intent=share("local-input","local-input");ShareImport original=new ShareImport(store,intent,null,null);original.run();assertNull(original.error);
        original.record.getJSONArray("files").getJSONObject(0).remove("sha256");original.write();assertTrue(DemoImportProvider.file(context).delete());
        ShareImport recovered=new ShareImport(store,intent,original.id,store.scope);
        try{recovered.run();assertEquals("integrity",((ShareImport.ReceiveFailure)recovered.error).kind);assertEquals(1,recovered.completeCount());assertFalse(recovered.file(0).exists());assertArrayEquals(source,Files.readAllBytes(recovered.file(1).toPath()));assertEquals(2,DemoImportProvider.opens.get());assertEquals(0,store.pending().length());}
        finally{recovered.cancel();}awaitCleanup();
    }
}
