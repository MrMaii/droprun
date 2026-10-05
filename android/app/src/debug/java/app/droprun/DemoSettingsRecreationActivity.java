package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
    static volatile ControlledWork finishBeforeAttach;
    final Map<String,Object> global=new HashMap<>(),cached=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger();
    int workFactories;ControlledWork work;String previousLanguage;View firstRenderedChild;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Settings recreation probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
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
        global.put("language","zh".equals(getIntent().getStringExtra("language"))?"zh":"en");global.put("appearance","light");
        cached.put("directExecution",true);
        if(state!=null&&finishBeforeAttach!=null){ControlledWork finishing=finishBeforeAttach;finishBeforeAttach=null;finishing.release.countDown();try{finishing.runner.join(3000);}catch(InterruptedException error){throw new AssertionError(error);}if(finishing.runner.isAlive())throw new AssertionError("Controlled worker did not finish before attachment");}
        super.onCreate(state);firstRenderedChild=body.getChildAt(0);
    }
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
