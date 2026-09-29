package app.droprun;

import android.os.Bundle;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Native delivery UI with synthetic cache bytes; never calls a Relay or a document provider. */
public final class DemoDeliverablesActivity extends DeliverablesActivity {
    static final String CONTENT="Synthetic delivery file. No agent work was performed.";
    static final AtomicInteger reads=new AtomicInteger();
    static volatile boolean failFirst,empty;
    static volatile CountDownLatch listGate;
    static void reset(){reads.set(0);failFirst=false;empty=false;listGate=null;}
    static JSONObject item(){
        try{return new JSONObject().put("id",Store.scope("synthetic-file","")).put("name","local-evidence.txt").put("sha256",digest(CONTENT.getBytes(StandardCharsets.UTF_8))).put("size",CONTENT.getBytes(StandardCharsets.UTF_8).length).put("kind","artifact");}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    static String digest(byte[] bytes)throws Exception{StringBuilder value=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();}
    @Override public void onCreate(Bundle state){if(state==null)DemoFixture.seed(this);getIntent().putExtra("taskId",DemoFixture.TASK);super.onCreate(state);}
    @Override Store createStore(){return new Store(this){
        @Override JSONObject get(String path)throws Exception{
            if(!path.equals("/tasks/"+DemoFixture.TASK+"/deliverables"))throw new IOException("Unexpected fixture request");
            int attempt=reads.incrementAndGet();CountDownLatch gate=listGate;
            if(gate!=null&&!gate.await(5,TimeUnit.SECONDS))throw new IOException("Local fixture timed out");
            if(failFirst&&attempt==1)throw new IOException("Synthetic delivery list unavailable");
            return new JSONObject().put("deliverables",empty?new JSONArray():new JSONArray().put(item()));
        }
        @Override File downloadDeliverable(String taskId,JSONObject item)throws Exception{
            if(!DemoFixture.TASK.equals(taskId)||!item().getString("id").equals(item.getString("id")))throw new IOException("Unexpected fixture file");
            File file=File.createTempFile("droprun-delivery-",".bin",cacheDir());Files.write(file.toPath(),CONTENT.getBytes(StandardCharsets.UTF_8));return file;
        }
    };}
    @Override void base(String title){super.base(title);page.addView(Ui.text(this,DemoFixture.NOTICE,12,Ui.MUTED));}
}
