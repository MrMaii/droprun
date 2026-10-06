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
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/** Real TaskActivity action callback over a closed synthetic failure. No saved task, API or files. */
public final class DemoTaskFeedbackActivity extends TaskActivity {
    static final String ID="feedback-ui-probe-task-0001",NONEMPTY="Synthetic action failure.";
    enum FailureCase { NULL,EMPTY,WHITESPACE,NONEMPTY }
    final Map<String,Object> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),workRuns=new AtomicInteger(),loads=new AtomicInteger();
    JSONObject sample;TextView marker;String previousLanguage,nonce,phase;
    volatile boolean workWasMain;
    private FailureCase failureCase;
    private Work permittedWork;
    private boolean prepared,consumed;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Task feedback probe forbids "+action);}
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
        });
    }
    @Override public void onCreate(Bundle state){
        previousLanguage=L.chinese()?"zh":"en";
        if(!getPackageName().endsWith(".debug")||!getIntent().getBooleanExtra("taskActionFeedbackOptIn",false))throw forbidden("missing opt-in");
        nonce=getIntent().getStringExtra("evidenceNonce");
        try{if(nonce==null||!UUID.fromString(nonce).toString().equals(nonce))throw forbidden("invalid nonce");}catch(IllegalArgumentException error){throw forbidden("invalid nonce");}
        phase=getIntent().getStringExtra("taskActionFeedbackProbe");String language=getIntent().getStringExtra("language"),theme=getIntent().getStringExtra("appearance");
        if(!"accepted".equals(phase))throw forbidden("unknown phase");
        if(!"en".equals(language)&&!"zh".equals(language))throw forbidden("unknown language");
        if(!"light".equals(theme)&&!"dark".equals(theme))throw forbidden("unknown appearance");
        try{failureCase=FailureCase.valueOf(getIntent().getStringExtra("failureCase"));}catch(RuntimeException error){throw forbidden("unknown failure case");}
        global.put("language",language);global.put("appearance",theme);boolean chinese=language.equals("zh");
        try{sample=new JSONObject().put("id",ID).put("title",chinese?"仅内存操作反馈示例":"Memory action feedback sample").put("project_id","feedback-ui-project").put("project_name",chinese?"本地示例项目":"Local sample project").put("status","running").put("execution_mode","direct").put("created_at",1L).put("report","");}catch(Exception error){throw new AssertionError(error);}
        getIntent().putExtra("taskId",ID);super.onCreate(state);
        marker=Ui.text(this,L.t("UI probe · memory only · no task sent","界面验证 · 仅内存 · 未发送任务"),12,Ui.AMBER);
        LinearLayout page=(LinearLayout)body.getParent();page.addView(marker,page.indexOfChild(notice));
    }
    @Override Store createStore(){return new Store(this){
        @Override JSONObject task(String id){if(!ID.equals(id))throw forbidden("stored task read");return sample;}
        @Override String projectLabel(String id,String name){if(!"feedback-ui-project".equals(id))throw forbidden("unknown project read");return name;}
        @Override boolean canClearUnavailableTask(String id){if(!ID.equals(id))throw forbidden("unknown deletion state");return false;}
        @Override JSONObject refreshTask(String id){throw forbidden("task refresh API");}
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
    void beginSyntheticFailure(){
        if(prepared||busy||workRuns.get()!=0||!foreground)throw forbidden("repeated or inactive synthetic failure");prepared=true;
        final FailureCase fixed=failureCase;
        permittedWork=()->{
            workRuns.incrementAndGet();workWasMain=Looper.myLooper()==Looper.getMainLooper();
            switch(fixed){
                case NULL -> throw new IOException();
                case EMPTY -> throw new IOException("");
                case WHITESPACE -> throw new IOException(" \t ");
                case NONEMPTY -> throw new IOException(NONEMPTY);
            }
        };
        perform(permittedWork);
    }
    @Override void perform(Work work){
        if(work==null||work!=permittedWork||consumed)throw forbidden("unapproved Work");consumed=true;permittedWork=null;super.perform(work);
    }
    @Override void load(){loads.incrementAndGet();}
    @Override void loadThumbnail(){throw forbidden("thumbnail load");}
    @Override void followup(){throw forbidden("follow-up");}
    @Override void openDeliverables(){throw forbidden("delivery navigation");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}
    @Override protected void onDestroy(){super.onDestroy();L.language(previousLanguage);}

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
