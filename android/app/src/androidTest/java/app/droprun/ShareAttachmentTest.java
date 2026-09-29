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
    @After public void cleanup(){DemoImportProvider.resume.countDown();DemoImportProvider.file(context).delete();}
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
        long end=android.os.SystemClock.elapsedRealtime()+5000;while(!files().equals(before)&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(25);assertEquals("Unsent copies are cleaned, preexisting files untouched",before,files());
    }
    @Test public void completeCopyRestoresWithoutReopeningSource()throws Exception{
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("local-input"))){
            awaitReceive(scenario);String[] path={null};scenario.onActivity(a->{try{assertEquals(1,a.attachments.length());path[0]=a.attachments.getJSONObject(0).getString("path");assertArrayEquals(source,Files.readAllBytes(new File(path[0]).toPath()));}catch(Exception e){throw new AssertionError(e);}});
            DemoImportProvider.file(context).delete();scenario.recreate();awaitReceive(scenario);
            scenario.onActivity(a->{try{assertEquals(path[0],a.attachments.getJSONObject(0).getString("path"));assertArrayEquals(source,Files.readAllBytes(new File(path[0]).toPath()));assertEquals(0,a.store.pending().length());}catch(Exception e){throw new AssertionError(e);}});assertEquals(1,DemoImportProvider.opens.get());
        }awaitCleanup();
    }
    @Test public void interruptedSecondCopyRemovesPartialAndEarlierCopies()throws Exception{
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("local-input","failed-input"))){
            awaitReceive(scenario);scenario.onActivity(a->{assertEquals(0,a.attachments.length());assertEquals(-1,a.step);assertEquals(0,a.store.pending().length());});assertEquals(2,DemoImportProvider.opens.get());awaitCleanup();
        }
    }
    @Test public void recreationDuringCopyKeepsOnlyTheRestoredCopy()throws Exception{
        DemoImportProvider.resume=new CountDownLatch(1);
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share("paused-input"))){
            assertTrue(DemoImportProvider.firstChunk.await(5,TimeUnit.SECONDS));scenario.recreate();DemoImportProvider.resume.countDown();awaitReceive(scenario);
            scenario.onActivity(a->{try{assertEquals(1,a.attachments.length());assertArrayEquals(source,Files.readAllBytes(new File(a.attachments.getJSONObject(0).getString("path")).toPath()));assertEquals(0,a.store.pending().length());}catch(Exception e){throw new AssertionError(e);}});assertEquals(2,DemoImportProvider.opens.get());
        }finally{DemoImportProvider.resume.countDown();}awaitCleanup();
    }
}
