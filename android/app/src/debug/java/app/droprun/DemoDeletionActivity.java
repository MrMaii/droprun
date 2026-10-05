package app.droprun;

import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual deletion and confirmation UI; every read and DELETE stays inside this debug fixture. */
public final class DemoDeletionActivity extends TaskActivity {
    static final AtomicInteger deletes=new AtomicInteger(),reads=new AtomicInteger();
    static volatile boolean freshRecord;
    @Override Store createStore(){return new Store(this){
        @Override JSONObject refreshTask(String id)throws Exception{reads.incrementAndGet();return freshRecord?super.refreshTask(id):task(id);}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename)throws Exception{
            if(method.equals("DELETE")&&path.equals("/tasks/"+DemoFixture.TASK)){deletes.incrementAndGet();throw new HttpFailure(404,"Synthetic task inaccessible; no Relay was contacted");}
            if(method.equals("GET")&&path.equals("/tasks/"+DemoFixture.TASK)&&freshRecord)return task(DemoFixture.TASK);
            throw new java.io.IOException("Unexpected local deletion fixture request");
        }
    };}
    @Override void loadThumbnail(){thumbnailRequested=true;}
}
