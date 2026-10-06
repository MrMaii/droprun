package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit memory-only history UI probe: no cache, outbox, network, removal or task navigation. */
public final class DemoHistoryIdentityActivity extends ProjectHistoryActivity {
    static float fontScale;
    static final String ID="history-ui-project-mobile",OTHER_ID="history-ui-project-desktop";
    final Map<String,Object> preferences=new HashMap<>();
    int forbiddenActions,memoryRefreshes;String previousLanguage;ProbeStore probe;
    AssertionError forbidden(String action){forbiddenActions++;return new AssertionError("History identity probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        if(fontScale==1f||fontScale==2f){android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());config.fontScale=fontScale;base=base.createConfigurationContext(config);}
        super.attachBaseContext(new ContextWrapper(base){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){if(name.equals("droprun.preferences")||name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences();throw forbidden("unknown preferences");}
            @Override public File getFilesDir(){throw forbidden("files");}
            @Override public File getCacheDir(){throw forbidden("cache");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service");}
        });
    }
    static String sampleName(boolean chinese,boolean extreme){String base=chinese?"工作室项目 · 移动端":"Studio workspace — Mobile";return extreme?base+" — "+"Reference collection / 参考设计档案 / ".repeat(8)+"FINAL_PROJECT_NAME_END":base;}
    @Override public void onCreate(Bundle state){
        if(!getIntent().getBooleanExtra("historyIdentity",false))throw forbidden("missing explicit opt-in");
        previousLanguage=L.chinese()?"zh":"en";preferences.put("language",getIntent().getStringExtra("language"));preferences.put("appearance",getIntent().getStringExtra("appearance"));
        super.onCreate(state);LinearLayout page=(LinearLayout)list.getParent().getParent(),header=(LinearLayout)page.getChildAt(0);TextView marker=Ui.text(this,L.t("UI probe · memory only","界面探针 · 仅内存"),11,Ui.MUTED);marker.setTag("history-memory-marker");header.addView(marker,0);
    }
    @Override Store createStore(){probe=new ProbeStore(this);return probe;}
    @Override void load(boolean append){if(append)throw forbidden("paging action");memoryRefreshes++;render();}
    @Override void removeSaved(JSONObject task){throw forbidden("remove");}
    @Override void openSaved(JSONObject task){throw forbidden("retry");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("navigation");}
    @Override protected void onDestroy(){super.onDestroy();L.language(previousLanguage);}
    SharedPreferences memoryPreferences(){return new SharedPreferences(){
        public Map<String,?> getAll(){return new HashMap<>(preferences);}public String getString(String key,String fallback){Object v=preferences.get(key);return v instanceof String?(String)v:fallback;}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}public int getInt(String key,int fallback){return fallback;}public long getLong(String key,long fallback){return fallback;}public float getFloat(String key,float fallback){return fallback;}public boolean getBoolean(String key,boolean fallback){return fallback;}public boolean contains(String key){return preferences.containsKey(key);}public Editor edit(){throw forbidden("preferences edit");}public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    };}
    final class ProbeStore extends Store {
        final JSONArray entries=new JSONArray(),catalog=new JSONArray(),summaries=new JSONArray();final JSONObject history=new JSONObject();
        ProbeStore(Context context){super(context);try{
            String name=getIntent().getStringExtra("projectName");for(String id:new String[]{ID,OTHER_ID}){JSONObject project=new JSONObject().put("id",id).put("name",name).put("available",!getIntent().getBooleanExtra("revoked",false));summaries.put(project);if(!getIntent().getBooleanExtra("revoked",false))catalog.put(project);}
            for(int n=0;n<3;n++)entries.put(new JSONObject().put("id","history-ui-task-"+n).put("title",L.t("Layout sample ","布局示例 ")+(n+1)).put("status",n==0?"running":"completed").put("created_at",1700000000000L+n).put("updated_at",n));
            history.put("tasks",entries).put("nextCursor","");
        }catch(Exception error){throw new AssertionError(error);}}
        @Override JSONArray projects(){return catalog;}@Override JSONArray activity(){return summaries;}@Override JSONObject history(String id){return history;}@Override JSONArray tasks(){return entries;}@Override JSONArray pending(){return new JSONArray();}@Override JSONObject task(String id){return null;}
        @Override JSONObject loadHistory(String id,String cursor){throw forbidden("history API");}@Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}@Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP");}@Override String credential(){throw forbidden("credentials");}
        @Override File instanceFiles(){throw forbidden("instance files");}@Override File cacheDir(){throw forbidden("cache");}@Override File outbox(){throw forbidden("outbox");}@Override synchronized void save(JSONObject task){throw forbidden("save");}@Override void pair(PairingTarget target){throw forbidden("pair");}@Override void cancelPending(String id){throw forbidden("cancel pending");}
    }
}
