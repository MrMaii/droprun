package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit fixed memory reads through the real ProjectHistoryActivity executor/callback. */
public final class DemoHistoryLoadStateActivity extends ProjectHistoryActivity {
    static float fontScale;
    static final String ID="history-state-memory-project",CURSOR="memory-next-only";
    final Map<String,Object> preferences=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),memoryReads=new AtomicInteger();
    String nonce;boolean cached;MemoryStore memory;volatile MemoryRead read=new MemoryRead(0);
    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("History state probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        if(fontScale!=1f&&fontScale!=2f)throw forbidden("missing local font opt-in");
        android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());config.fontScale=fontScale;
        super.attachBaseContext(new ContextWrapper(base.createConfigurationContext(config)){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){if(name.equals("droprun.preferences")||name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences();throw forbidden("unknown preferences");}
            @Override public File getFilesDir(){throw forbidden("files");}@Override public File getCacheDir(){throw forbidden("cache");}
            @Override public File getExternalFilesDir(String type){throw forbidden("external files");}@Override public File getExternalCacheDir(){throw forbidden("external cache");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service");}
            @Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }
    @Override protected void onCreate(Bundle state){
        nonce=getIntent().getStringExtra("nonce");String language=getIntent().getStringExtra("language"),theme=getIntent().getStringExtra("appearance");
        if(!getIntent().getBooleanExtra("historyLoadStateProbe",false)||nonce==null||!nonce.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw forbidden("missing explicit probe/nonce");
        if(!("en".equals(language)&&"light".equals(theme))&&!("zh".equals(language)&&"dark".equals(theme)))throw forbidden("unknown configuration");
        cached=getIntent().getBooleanExtra("cached",false);if(cached&&(fontScale!=1f||!"en".equals(language)))throw forbidden("unknown cached configuration");
        if(!ID.equals(getIntent().getStringExtra("projectId"))||!"Memory history".equals(getIntent().getStringExtra("projectName")))throw forbidden("unknown project");
        preferences.put("language",language);preferences.put("appearance",theme);super.onCreate(state);
        LinearLayout page=(LinearLayout)list.getParent().getParent(),header=(LinearLayout)page.getChildAt(0);TextView marker=Ui.text(this,L.t("History state · memory only","历史状态 · 仅内存"),11,Ui.MUTED);marker.setTag("history-state-marker");header.addView(marker,0);
    }
    @Override Store createStore(){memory=new MemoryStore(this);return memory;}
    void nextRead(int outcome){
        int count=memoryReads.get();if(busy||read.finished.getCount()!=0||cached||!((count==1&&outcome==1)||(count==2&&outcome==2&&fontScale==1f)))throw forbidden("unknown memory sequence");
        read=new MemoryRead(outcome);
    }
    void releaseRead(){read.release.countDown();}
    @Override void removeSaved(JSONObject task){throw forbidden("remove");}@Override void openSaved(JSONObject task){throw forbidden("retry sending");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("navigation");}
    SharedPreferences memoryPreferences(){return new SharedPreferences(){
        public Map<String,?> getAll(){return new HashMap<>(preferences);}public String getString(String key,String fallback){Object value=preferences.get(key);return value instanceof String?(String)value:fallback;}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}public int getInt(String key,int fallback){return fallback;}public long getLong(String key,long fallback){return fallback;}public float getFloat(String key,float fallback){return fallback;}public boolean getBoolean(String key,boolean fallback){return fallback;}public boolean contains(String key){return preferences.containsKey(key);}public Editor edit(){throw forbidden("preferences edit");}public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    };}
    static final class MemoryRead {final int outcome;final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);MemoryRead(int outcome){this.outcome=outcome;}}
    final class MemoryStore extends Store {
        final JSONObject history=new JSONObject();final JSONArray entries=new JSONArray(),catalog=new JSONArray();
        MemoryStore(Context context){super(context);try{
            catalog.put(new JSONObject().put("id",ID).put("name","Memory history"));
            if(cached)for(int n=0;n<3;n++)entries.put(new JSONObject().put("id","memory-state-task-"+n).put("title","Cached memory row "+(n+1)).put("status","completed").put("created_at",1700000000000L+n).put("updated_at",n));
            history.put("tasks",entries).put("nextCursor","");
        }catch(Exception error){throw new AssertionError(error);}}
        @Override JSONArray projects(){return catalog;}@Override JSONArray activity(){return catalog;}@Override JSONObject history(String id){if(!ID.equals(id))throw forbidden("unknown history");return history;}
        @Override JSONArray tasks(){return entries;}@Override JSONArray pending(){return new JSONArray();}@Override JSONObject task(String id){return null;}
        @Override JSONObject loadHistory(String id,String cursor)throws Exception{
            MemoryRead pending=read;if(!ID.equals(id)||!((pending.outcome==2?CURSOR:"").equals(cursor))||pending.entered.getCount()!=1)throw forbidden("unexpected memory read");
            memoryReads.incrementAndGet();pending.entered.countDown();
            try{
                if(!pending.release.await(10,TimeUnit.SECONDS))throw forbidden("memory gate timeout");
                if(pending.outcome==0)throw new IOException(cached?(String)null:fontScale==2f?"":"Synthetic history read failure.");
                if(pending.outcome==2)throw new IOException((String)null);
                if(pending.outcome!=1)throw forbidden("unknown memory result");return history;
            }finally{pending.finished.countDown();}
        }
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}@Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP");}@Override String credential(){throw forbidden("credential");}
        @Override File instanceFiles(){throw forbidden("instance files");}@Override File cacheDir(){throw forbidden("cache");}@Override File outbox(){throw forbidden("outbox");}@Override File attachments(){throw forbidden("attachments");}
        @Override File downloadDeliverable(String id,JSONObject item){throw forbidden("download");}
        @Override synchronized void save(JSONObject task){throw forbidden("save");}@Override void pair(PairingTarget target){throw forbidden("pair");}@Override void cancelPending(String id){throw forbidden("cancel pending");}
    }
}
