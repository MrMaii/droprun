package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.res.Configuration;
import android.view.View;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Real mode-operation lifecycle over guarded memory only; never saves a setting or calls a Relay. */
public class DemoSettingsRecreationActivity extends SettingsActivity {
    static volatile float hierarchyFontScale;
    int modelSaves;Store modelProbeStore;TextView hierarchyNotice;
    static volatile ControlledWork finishBeforeAttach;
    final Map<String,Object> global=new HashMap<>(),cached=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger();
    int workFactories;ControlledWork work;String previousLanguage;View firstRenderedChild;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Settings recreation probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        if(hierarchyFontScale!=0f){Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=hierarchyFontScale;base=base.createConfigurationContext(config);}
        super.attachBaseContext(new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(name.equals("droprun.preferences"))return memoryPreferences(global);
                if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences(cached);
                throw forbidden("unknown preferences");
            }
            @Override public File getFilesDir(){throw forbidden("files directory");}
            @Override public File getCacheDir(){throw forbidden("cache directory");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service start");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service start");}
            @Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }
    @Override protected void onCreate(Bundle state){
        previousLanguage=L.chinese()?"zh":"en";
        global.put("language","zh".equals(getIntent().getStringExtra("language"))?"zh":"en");global.put("appearance","dark".equals(getIntent().getStringExtra("appearance"))?"dark":"light");
        cached.put("directExecution",true);
        if(getIntent().getBooleanExtra("modelHierarchy",false))L.language((String)global.get("language"));
        if(getIntent().getBooleanExtra("modelHierarchy",false))try{
            JSONArray models=new JSONArray().put(new JSONObject().put("id","sample-default").put("displayName",L.t("Local UI model","本地界面模型")).put("isDefault",true).put("defaultEffort","medium").put("efforts",new JSONArray().put("low").put("medium").put("high")))
                .put(new JSONObject().put("id","sample-second").put("displayName",L.t("Another UI model","另一个界面模型")).put("defaultEffort","low").put("efforts",new JSONArray().put("low").put("high").put("future-effort")));
            cached.put("projects",new JSONObject().put("models",models).toString());
            if(state!=null){cached.put("defaultModel",state.getString("probeModel",""));cached.put("defaultEffort",state.getString("probeEffort",""));modelSaves=state.getInt("probeSaves");}
        }catch(Exception error){throw new AssertionError(error);}
        if(state!=null&&finishBeforeAttach!=null){ControlledWork finishing=finishBeforeAttach;finishBeforeAttach=null;finishing.release.countDown();try{finishing.runner.join(3000);}catch(InterruptedException error){throw new AssertionError(error);}if(finishing.runner.isAlive())throw new AssertionError("Controlled worker did not finish before attachment");}
        super.onCreate(state);firstRenderedChild=body.getChildAt(0);
    }
    @Override void modelSection(){
        if(getIntent().getBooleanExtra("modelHierarchy",false)&&modelProbeStore==null){
            modelProbeStore=new Store(this){
                @Override void saveDefaults(String model,String effort){modelSaves++;cached.put("defaultModel",model);cached.put("defaultEffort",effort);}
                @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}
                @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("request");}
            };store=modelProbeStore;
        }
        super.modelSection();
        if(getIntent().getBooleanExtra("modelHierarchy",false)){
            hierarchyNotice=Ui.caption(this,L.t("UI probe · memory only","界面探针 · 仅内存"));
            ((android.widget.LinearLayout)body.getChildAt(body.getChildCount()-1)).addView(hierarchyNotice,Ui.margins(this,8,0));
        }
    }
    @Override protected void onSaveInstanceState(Bundle state){if(getIntent().getBooleanExtra("modelHierarchy",false)){state.putString("probeModel",store.defaultModel());state.putString("probeEffort",store.defaultEffort(store.defaultModel()));state.putInt("probeSaves",modelSaves);}super.onSaveInstanceState(state);}
    @Override void refresh(){}
    @Override Callable<Void> modeWork(boolean direct){
        workFactories++;work=new ControlledWork(direct,getIntent().getBooleanExtra("fail",false)?L.t("Could not save the execution preference.","执行偏好保存失败。"):null);return work;
    }
    @Override void setPermission(String id,boolean enabled){throw forbidden("project permission");}
    @Override void disconnect(){throw forbidden("disconnect");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("navigation");}
    @Override protected void onDestroy(){if(isFinishing()&&work!=null)work.release.countDown();super.onDestroy();L.language(previousLanguage);}

    static final class ControlledWork implements Callable<Void>{
        final boolean direct;final String error;
        final CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        final AtomicInteger calls=new AtomicInteger();Thread runner;
        ControlledWork(boolean direct,String error){this.direct=direct;this.error=error;}
        @Override public Void call()throws Exception{runner=Thread.currentThread();calls.incrementAndGet();started.countDown();release.await();if(error!=null)throw new IOException(error);return null;}
    }
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
