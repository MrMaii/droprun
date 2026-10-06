package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONObject;

/** Explicit synthetic fallback UI probe; never downloads or verifies a real file. */
public final class DemoDeliveryVerificationActivity extends DeliverablesActivity {
    static float probeFontScale;
    static final String HASH="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    final Map<String,Object> global=new HashMap<>();
    int forbiddenActions,blockedPathReads,previewRenders,initialLists;
    TextView marker;
    AssertionError forbidden(String action){forbiddenActions++;return new AssertionError("Delivery UI fixture forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        if(probeFontScale==1f||probeFontScale==2f){Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=probeFontScale;base=base.createConfigurationContext(config);}
        super.attachBaseContext(new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(name.equals("droprun.preferences"))return memoryPreferences(global);
                if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences(Collections.emptyMap());
                throw forbidden("unknown preferences");
            }
            @Override public File getFilesDir(){throw forbidden("files directory");}
            @Override public File getCacheDir(){throw forbidden("cache directory");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service start");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service start");}
            @Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }
    @Override public void onCreate(Bundle state){
        if(!getIntent().getBooleanExtra("verificationUiProbe",false))throw forbidden("missing explicit opt-in");
        global.put("language","zh".equals(getIntent().getStringExtra("language"))?"zh":"en");
        global.put("appearance","dark".equals(getIntent().getStringExtra("appearance"))?"dark":"light");
        verificationExpanded=state!=null&&state.getBoolean("verificationExpanded");
        getIntent().putExtra("taskId","delivery-verification-probe-0001");
        // UI-only recreation deliberately bypasses real cache/path/picker restoration.
        super.onCreate(null);
    }
    @Override Store createStore(){return new Store(this){
        @Override String credential(){throw forbidden("credential read");}
        @Override JSONObject get(String path){throw forbidden("Relay get");}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}
        @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP request");}
        @Override File downloadDeliverable(String task,JSONObject item){throw forbidden("download");}
        @Override synchronized void save(JSONObject task){throw forbidden("task save");}
        @Override void pair(PairingTarget target){throw forbidden("pairing");}
        @Override File attachments(){throw forbidden("attachments");}
        @Override File outbox(){throw forbidden("outbox");}
        @Override File cacheDir(){throw forbidden("cache");}
        @Override File instanceFiles(){throw forbidden("instance files");}
    };}
    @Override void loadList(){
        if(initialLists++!=0)throw forbidden("Back/list business action");
        try{currentItem=new JSONObject().put("name",L.chinese()?"仅内存界面示例.probe":"memory-ui-example.probe").put("sha256",HASH).put("size",0).put("kind","artifact");}catch(Exception e){throw new AssertionError(e);}
        currentFile=new NoIoFile();previewRenders++;super.showFile();addMarker();
    }
    @Override void clearFile(){if(currentFile!=null&&!(currentFile instanceof NoIoFile))throw forbidden("non-memory file cleanup");currentFile=null;currentItem=null;}
    @Override void download(JSONObject item){throw forbidden("download action");}
    @Override void restorePreview(){throw forbidden("real cache restoration");}
    @Override void saveFile(){throw forbidden("Save/business file action");}
    @Override public void onBackPressed(){throw forbidden("Back navigation");}
    @Override public void startActivity(Intent intent){throw forbidden("navigation");}
    @Override public void startActivity(Intent intent,Bundle options){throw forbidden("navigation");}
    @Override public void startActivityForResult(Intent intent,int request){throw forbidden("picker/navigation");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("picker/navigation");}
    @Override protected void onSaveInstanceState(Bundle state){
        File memory=currentFile;if(memory!=null&&!(memory instanceof NoIoFile))throw forbidden("non-memory file state");currentFile=null;try{super.onSaveInstanceState(state);}finally{currentFile=memory;}
    }
    void addMarker(){
        marker=Ui.text(this,L.t("Synthetic UI · no download or verification","合成界面 · 仅内存 · 未下载或校验"),12,Ui.MUTED);
        page.addView(marker,1,Ui.margins(this,8,8));
    }
    final class NoIoFile extends File {
        NoIoFile(){super("memory-only.probe");}
        @Override public long length(){return 0;}
        @Override public Path toPath(){blockedPathReads++;throw new UnsupportedOperationException("Synthetic file has no filesystem path");}
        @Override public String getPath(){throw forbidden("real file path");}
        @Override public String getAbsolutePath(){throw forbidden("absolute path");}
        @Override public String getCanonicalPath(){throw forbidden("canonical path");}
        @Override public File getAbsoluteFile(){throw forbidden("absolute file");}
        @Override public File getCanonicalFile(){throw forbidden("canonical file");}
        @Override public boolean exists(){throw forbidden("file existence");}
        @Override public boolean isFile(){throw forbidden("file stat");}
        @Override public boolean delete(){throw forbidden("file delete");}
        @Override public boolean createNewFile(){throw forbidden("file write");}
    }
    SharedPreferences memoryPreferences(Map<String,Object> data){return new SharedPreferences(){
        public Map<String,?> getAll(){return new HashMap<>(data);}
        public String getString(String key,String fallback){Object value=data.get(key);return value instanceof String?(String)value:fallback;}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}
        public int getInt(String key,int fallback){return fallback;}public long getLong(String key,long fallback){return fallback;}
        public float getFloat(String key,float fallback){return fallback;}public boolean getBoolean(String key,boolean fallback){return fallback;}
        public boolean contains(String key){return data.containsKey(key);}
        public Editor edit(){throw forbidden("preferences edit");}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    };}
}
