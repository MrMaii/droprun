package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/** Private draft: inherited TaskActivity refresh callback, one fixed failure, memory only. */
public final class DemoTaskRefreshFeedbackActivity extends TaskActivity {
    static final String ID="refresh-ui-probe-task-0001",NONEMPTY="Synthetic refresh failure.";
    enum FailureCase { NULL,EMPTY,WHITESPACE,NONEMPTY }
    final Map<String,Object> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),refreshCalls=new AtomicInteger(),refreshFinished=new AtomicInteger();
    final CountDownLatch refreshStarted=new CountDownLatch(1),refreshRelease=new CountDownLatch(1);
    JSONObject sample;TextView marker;String nonce,phase;
    volatile boolean refreshWasMain;
    volatile Thread refreshRunner;
    private FailureCase failureCase;
    private boolean prepared;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Task refresh probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=1f;
        super.attachBaseContext(new ContextWrapper(base.createConfigurationContext(config)){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(name.equals("droprun.preferences"))return memoryPreferences(global);
                if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences(Collections.emptyMap());
                throw forbidden("unknown preferences");
            }
            @Override public File getFilesDir(){throw forbidden("files directory");}
            @Override public File getCacheDir(){throw forbidden("cache directory");}
            @Override public File getExternalFilesDir(String type){throw forbidden("external files directory");}
            @Override public File getExternalCacheDir(){throw forbidden("external cache directory");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service start");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service start");}
            @Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }
    @Override public void onCreate(Bundle state){
        if(!getPackageName().endsWith(".debug")||!getIntent().getBooleanExtra("taskRefreshFeedbackOptIn",false))throw forbidden("missing opt-in");
        nonce=getIntent().getStringExtra("evidenceNonce");
        try{if(nonce==null||!UUID.fromString(nonce).toString().equals(nonce))throw forbidden("invalid nonce");}catch(IllegalArgumentException error){throw forbidden("invalid nonce");}
        phase=getIntent().getStringExtra("taskRefreshFeedbackProbe");String language=getIntent().getStringExtra("language"),theme=getIntent().getStringExtra("appearance");
        if(!"accepted".equals(phase))throw forbidden("unknown phase");
        if(!"en".equals(language)&&!"zh".equals(language))throw forbidden("unknown language");
        if(!"light".equals(theme)&&!"dark".equals(theme))throw forbidden("unknown appearance");
        try{failureCase=FailureCase.valueOf(getIntent().getStringExtra("failureCase"));}catch(RuntimeException error){throw forbidden("unknown failure case");}
        global.put("language",language);global.put("appearance",theme);boolean chinese=language.equals("zh");
        try{sample=new JSONObject().put("id",ID).put("title",chinese?"仅内存刷新反馈示例":"Memory refresh feedback sample").put("project_id","refresh-ui-project").put("project_name",chinese?"本地示例项目":"Local sample project").put("status","running").put("execution_mode","direct").put("created_at",1L).put("report","");}catch(Exception error){throw new AssertionError(error);}
        if(state!=null)throw forbidden("state recreation");prepared=true;
        getIntent().putExtra("taskId",ID);super.onCreate(null);
        marker=Ui.text(this,L.t("UI probe · memory only · no task sent","界面验证 · 仅内存 · 未发送任务"),12,Ui.AMBER);
        LinearLayout page=(LinearLayout)body.getParent();page.addView(marker,page.indexOfChild(notice));
    }
    @Override Store createStore(){return new Store(this){
        @Override JSONObject task(String id){if(!ID.equals(id))throw forbidden("stored task read");return sample;}
        @Override String projectLabel(String id,String name){if(!"refresh-ui-project".equals(id))throw forbidden("unknown project read");return name;}
        @Override boolean canClearUnavailableTask(String id){if(!ID.equals(id))throw forbidden("unknown deletion state");return false;}
        @Override JSONObject refreshTask(String id)throws Exception{
            if(!prepared||!ID.equals(id))throw forbidden("unprepared or unknown refresh");
            if(refreshCalls.incrementAndGet()!=1)throw forbidden("second refresh");
            refreshRunner=Thread.currentThread();refreshWasMain=Looper.myLooper()==Looper.getMainLooper();refreshStarted.countDown();
            try{
                try{if(!refreshRelease.await(10,TimeUnit.SECONDS))throw forbidden("unreleased fixed refresh");}
                catch(InterruptedException error){Thread.currentThread().interrupt();throw forbidden("interrupted fixed refresh");}
                switch(failureCase){
                    case NULL -> throw new IOException();
                    case EMPTY -> throw new IOException("");
                    case WHITESPACE -> throw new IOException(" \t ");
                    case NONEMPTY -> throw new IOException(NONEMPTY);
                }
                throw forbidden("unknown fixed failure");
            }finally{refreshFinished.incrementAndGet();}
        }
        @Override String credential(){throw forbidden("credential read");}
        @Override JSONObject api(String path,String method,byte[] data,String mime,String filename){throw forbidden("API");}
        @Override JSONObject request(String origin,String path,String method,byte[] data,String mime,String filename,String token){throw forbidden("HTTP request");}
        @Override synchronized void save(JSONObject task){throw forbidden("task save");}
        @Override void pair(PairingTarget target){throw forbidden("pairing");}
        @Override File downloadDeliverable(String taskId,JSONObject item){throw forbidden("delivery download");}
        @Override File attachments(){throw forbidden("attachments");}
        @Override File outbox(){throw forbidden("outbox");}
        @Override File cacheDir(){throw forbidden("cache files");}
        @Override File instanceFiles(){throw forbidden("instance files");}
    };}
    // Release only this Activity's already fixed memory failure; cleanup may release the same latch again.
    void releaseSyntheticRefresh(){refreshRelease.countDown();}
    @Override void perform(Work work){throw forbidden("any Work");}
    @Override void loadThumbnail(){throw forbidden("thumbnail load");}
    @Override void followup(){throw forbidden("follow-up");}
    @Override void openDeliverables(){throw forbidden("delivery navigation");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}

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
}
