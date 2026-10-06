package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/** Actual result/preview rendering over memory only. No saved task, settings, file, service or API. */
public class DemoTaskPreviewActivity extends TaskActivity {
    static final String ID="preview-ui-probe-task-0001";
    final Map<String,Object> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger();
    JSONObject sample;String previousLanguage;int thumbnailOpens;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Task preview probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
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
        });
    }
    @Override public void onCreate(Bundle state){
        previousLanguage=L.chinese()?"zh":"en";
        boolean chinese="zh".equals(getIntent().getStringExtra("language"));global.put("language",chinese?"zh":"en");global.put("appearance","dark".equals(getIntent().getStringExtra("appearance"))?"dark":"light");
        try{sample=new JSONObject().put("id",ID).put("title",chinese?"本地预览示例":"Local preview sample").put("project_id","preview-ui-project").put("project_name",chinese?"本地示例项目":"Local sample project").put("status","completed").put("execution_mode","direct").put("thread_id","ui-preview-thread").put("created_at",System.currentTimeMillis()).put("report",chinese?"## 交付结果\n仅展示内存中的示例结果，没有执行任务。":"## The result\nThis in-memory sample shows a result. No task was executed.");}catch(Exception error){throw new AssertionError(error);}
        if(getIntent().getBooleanExtra("thumbnailProbe",false)){
            boolean wide="wide".equals(getIntent().getStringExtra("bitmapShape"));int width=wide?1200:600,height=wide?40:400;
            thumbnail=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas=new android.graphics.Canvas(thumbnail);android.graphics.Paint paint=new android.graphics.Paint();
            paint.setColor(0xFF365A24);canvas.drawRect(0,0,width,height,paint);paint.setColor(0xFFB8EF73);canvas.drawRect(0,0,width/3f,height,paint);paint.setColor(0xFFEAF5DD);canvas.drawRect(width*2/3f,0,width,height,paint);
            thumbnailRequested=true;
        }
        String snapshotVersion=getIntent().getStringExtra("snapshotVersionProbe");
        if(snapshotVersion!=null)try{sample.put("preview_kind","snapshot").put("preview_status","ready").put("preview_url","https://preview.example.invalid/s/sample").put("preview_expires_at",Long.MAX_VALUE).put("preview_version",snapshotVersion);}catch(Exception error){throw new AssertionError(error);}
        getIntent().putExtra("taskId",ID);super.onCreate(state);
    }
    @Override Store createStore(){return new Store(this){
        @Override JSONObject task(String id){if(!ID.equals(id))throw forbidden("stored task read");return sample;}
        @Override String projectLabel(String id,String name){return name;}
        @Override boolean canClearUnavailableTask(String id){return false;}
        @Override String credential(){throw forbidden("credential read");}
        @Override JSONObject api(String path,String method,byte[] data,String mime,String filename){throw forbidden("API");}
        @Override JSONObject request(String origin,String path,String method,byte[] data,String mime,String filename,String token){throw forbidden("HTTP request");}
        @Override synchronized void save(JSONObject task){throw forbidden("task save");}
        @Override void pair(PairingTarget target){throw forbidden("pairing");}
        @Override File attachments(){throw forbidden("attachments");}
        @Override File outbox(){throw forbidden("outbox");}
        @Override File cacheDir(){throw forbidden("cache files");}
        @Override File instanceFiles(){throw forbidden("instance files");}
    };}
    @Override void load(){}
    @Override void loadThumbnail(){thumbnailRequested=true;}
    @Override void render(){super.render();notice.setText(L.t("UI probe · memory only · no work sent","界面验证 · 仅内存 · 未发送任务"));notice.setVisibility(android.view.View.VISIBLE);}
    @Override void perform(Work work){throw forbidden("decision or preview action");}
    @Override void followup(){throw forbidden("follow-up");}
    @Override void openDeliverables(){if(getIntent().getBooleanExtra("thumbnailProbe",false)){thumbnailOpens++;return;}throw forbidden("delivery navigation");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}
    @Override protected void onDestroy(){super.onDestroy();if(getIntent().getBooleanExtra("thumbnailProbe",false)&&thumbnail!=null)thumbnail.recycle();L.language(previousLanguage);}

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
