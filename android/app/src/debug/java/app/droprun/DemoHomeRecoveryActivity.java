package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real home recovery controls over memory only. No saved settings, files, jobs or API. */
public final class DemoHomeRecoveryActivity extends MainActivity {
    static final String ISSUE="UI sample: POST /tasks returned 403.\nThis sample project no longer accepts handoffs. Existing saved history remains available.";
    final Map<String,Object> global=new ConcurrentHashMap<>(),instance=new ConcurrentHashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger();
    final CountDownLatch cacheReadStarted=new CountDownLatch(1),cacheReadRelease=new CountDownLatch(1);
    volatile boolean cacheOnlyProbe;
    ProbeStore probeStore;TextView demoNotice;String previousLanguage;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Home recovery probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        super.attachBaseContext(new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(name.equals("droprun.preferences"))return memoryPreferences(global);
                if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences(instance);
                throw forbidden("unknown preferences");
            }
            @Override public File getFilesDir(){throw forbidden("files directory");}
            @Override public File getCacheDir(){throw forbidden("cache directory");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service start");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service start");}
        });
    }
    @Override public void onCreate(Bundle state){
        previousLanguage=L.chinese()?"zh":"en";
        global.put("language","zh".equals(getIntent().getStringExtra("language"))?"zh":"en");global.put("appearance","light");
        instance.put("syncError",ISSUE);instance.put("receiverNotice","UI probe · no network or stored data");
        super.onCreate(state);
    }
    @Override Store createStore(){probeStore=new ProbeStore(this);return probeStore;}
    @Override boolean backgroundSyncEnabled(){
        // UI lifecycle and refresh callbacks stay disabled; only the explicit IO probe may wait.
        if(cacheOnlyProbe&&Looper.myLooper()!=Looper.getMainLooper()){
            cacheReadStarted.countDown();try{cacheReadRelease.await();}catch(InterruptedException error){Thread.currentThread().interrupt();throw new AssertionError(error);}return true;
        }
        return false;
    }
    @Override void build(){
        super.build();demoNotice=Ui.text(this,L.t("UI probe · memory only · no network","界面验证 · 仅内存 · 不连接网络"),11,Ui.MUTED);
        demoNotice.setPadding(dp(24),0,dp(24),dp(6));demoNotice.setTag("home-recovery-ui-probe");root.addView(demoNotice,1,Ui.fill());
    }
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}
    @Override protected void onDestroy(){cacheReadRelease.countDown();if(probeStore!=null)probeStore.release.countDown();super.onDestroy();L.language(previousLanguage);}

    SharedPreferences memoryPreferences(Map<String,Object> data){return new SharedPreferences(){
        public Map<String,?> getAll(){return new HashMap<>(data);}
        public String getString(String key,String fallback){Object value=data.get(key);return value instanceof String?(String)value:fallback;}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}
        public int getInt(String key,int fallback){Object value=data.get(key);return value instanceof Number?((Number)value).intValue():fallback;}
        public long getLong(String key,long fallback){Object value=data.get(key);return value instanceof Number?((Number)value).longValue():fallback;}
        public float getFloat(String key,float fallback){Object value=data.get(key);return value instanceof Number?((Number)value).floatValue():fallback;}
        public boolean getBoolean(String key,boolean fallback){Object value=data.get(key);return value instanceof Boolean?(Boolean)value:fallback;}
        public boolean contains(String key){return data.containsKey(key);}
        public Editor edit(){throw forbidden("preferences edit");}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    };}

    final class ProbeStore extends Store {
        final JSONArray projects=new JSONArray(),summaries=new JSONArray();
        final AtomicInteger syncCalls=new AtomicInteger();
        final CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        volatile boolean success;
        ProbeStore(Context context){
            super(context);
            try{for(int n=0;n<3;n++){
                String id="home-ui-project-"+n,name=L.chinese()?"本地示例项目 "+(n+1):"Local sample project "+(n+1);
                projects.put(new JSONObject().put("id",id).put("name",name));
                summaries.put(new JSONObject().put("id",id).put("name",name).put("task_count",n+1).put("dispatch_count",n+2).put("last_status","completed").put("last_dispatch_at",System.currentTimeMillis()-(n+1)*60000L).put("available",true));
            }}catch(Exception error){throw new AssertionError(error);}
        }
        @Override boolean paired(){return true;}
        @Override boolean online(){return false;}
        @Override boolean computerOnline(){return true;}
        @Override JSONArray projects(){return projects;}
        @Override JSONArray activity(){return summaries;}
        @Override JSONArray tasks(){return new JSONArray();}
        @Override JSONArray pending(){return new JSONArray();}
        @Override JSONObject history(String id){return new JSONObject();}
        @Override JSONObject task(String id){throw forbidden("task detail read");}
        @Override File attachments(){return new File("unused-home-ui-probe"){@Override public File[] listFiles(){return new File[0];}};}
        @Override File instanceFiles(){throw forbidden("instance files");}
        @Override File cacheDir(){throw forbidden("cache files");}
        @Override File outbox(){throw forbidden("outbox");}
        @Override String credential(){throw forbidden("credential read");}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}
        @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP request");}
        @Override synchronized void save(JSONObject task){throw forbidden("task save");}
        @Override void pair(PairingTarget target){throw forbidden("pairing");}
        @Override void sync()throws Exception{
            syncCalls.incrementAndGet();started.countDown();
            release.await();
            if(success)instance.remove("syncError");else{instance.put("syncError",ISSUE);throw new IOException(ISSUE);}
        }
    }
}
