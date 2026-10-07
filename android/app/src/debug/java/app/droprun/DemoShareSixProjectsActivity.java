package app.droprun;

import android.content.*;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Six fixed cached projects; one held catalog failure and one fixed activity response. */
public final class DemoShareSixProjectsActivity extends ShareActivity {
    static final String MARKER="MEMORY ONLY · 6 projects",ACTIVITY="{\"projects\":[]}";
    static final String[] NAMES={"Memory Alpha","Memory Beta","Memory Gamma","Memory Delta","Memory Epsilon","Memory Zeta"};
    final Map<String,String> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),projectReads=new AtomicInteger(),activityReads=new AtomicInteger(),cacheApplies=new AtomicInteger();
    final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
    String nonce,language,theme,catalog;boolean ready,closeAllowed;int initializationCloses,finishCalls,checkpointNoops,renderCalls,statusCalls;
    TextView marker;MemoryStore memory;
    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Six-project fixture forbids "+action);}
    void require(boolean condition,String action){if(!condition)throw forbidden(action);}
    void main(){require(Looper.myLooper()==Looper.getMainLooper(),"off-main control");}
    static JSONObject json(String value){try{return new JSONObject(value);}catch(JSONException error){throw new AssertionError(error);}}
    @Override protected void attachBaseContext(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=1f;
        super.attachBaseContext(new ContextWrapper(base.createConfigurationContext(config)){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){require(mode==MODE_PRIVATE,"preference mode");if(name.equals("droprun.preferences"))return new Preferences(global,false);if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return new Preferences(Collections.emptyMap(),false);throw forbidden("unknown preferences");}
            @Override public File getFilesDir(){throw forbidden("files");}@Override public File getCacheDir(){throw forbidden("cache");}
            @Override public ComponentName startService(Intent intent){throw forbidden("service");}@Override public ComponentName startForegroundService(Intent intent){throw forbidden("foreground service");}@Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }
    @Override protected void onCreate(Bundle state){
        main();Intent intent=getIntent();require(BuildConfig.DEBUG&&state==null&&getLastNonConfigurationInstance()==null,"restoration/non-debug launch");
        require(intent!=null&&new ComponentName(this,DemoShareSixProjectsActivity.class).equals(intent.getComponent())&&"accepted".equals(intent.getStringExtra("shareSixProjectsProbe"))&&intent.getData()==null&&intent.getClipData()==null,"missing opt-in");
        nonce=intent.getStringExtra("evidenceNonce");language=intent.getStringExtra("language");theme=intent.getStringExtra("appearance");
        boolean canonical=false;try{canonical=nonce!=null&&UUID.fromString(nonce).toString().equals(nonce);}catch(IllegalArgumentException ignored){}
        require(canonical&&("en".equals(language)&&"light".equals(theme)||"zh".equals(language)&&"dark".equals(theme)),"unknown fixed window");
        try{require(!getPackageManager().getActivityInfo(new ComponentName(this,DemoShareSixProjectsActivity.class),0).exported,"exported fixture");}catch(android.content.pm.PackageManager.NameNotFoundException error){throw forbidden("missing registration");}
        global.put("language",language);global.put("appearance",theme);global.put("relay","");global.put("instanceId","");super.onCreate(null);
        require(incoming==null&&initializationCloses==1&&!closing,"unexpected import/initial close");
        try{JSONArray projects=new JSONArray();for(int i=0;i<NAMES.length;i++)projects.put(new JSONObject().put("id","memory-six-"+i).put("name",NAMES[i]).put("available",true).put("permission",new JSONObject().put("enabled",true)));catalog=new JSONObject().put("projects",projects).put("models",new JSONArray()).toString();}catch(JSONException error){throw new AssertionError(error);}
        store=memory=new MemoryStore(this);shared="https://example.invalid/memory-reference";draft="Memory note retained.";model="memory-model";effort="high";step=0;receiving=false;sent=false;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // The existing Back/Close row is already 48dp. This label uses its vacant lower middle.
        FrameLayout middle=(FrameLayout)dots.getParent();middle.setMinimumHeight(dp(48));
        marker=Ui.text(this,MARKER,10,Ui.MUTED);marker.setPadding(0,0,0,0);marker.setSingleLine(true);marker.setGravity(Gravity.CENTER);middle.addView(marker,new FrameLayout.LayoutParams(-1,dp(14),Gravity.BOTTOM));
        stage.addView(stepProject(),new ViewGroup.LayoutParams(-1,-2));dots.setVisibility(View.VISIBLE);dots.setActive(0,false);ready=true;refreshProjects(false);
    }
    @Override Bundle recoveryState(Bundle state){require(state==null&&!ready,"recovery");Bundle skip=new Bundle();skip.putBoolean("sent",true);return skip;}
    @Override void close(){require(!ready&&initializationCloses==0&&sent&&incoming==null,"close");initializationCloses++;}
    void allowScenarioClose(){main();require(ready&&!closeAllowed&&!isDestroyed(),"duplicate cleanup");closeAllowed=true;}
    @Override public void finish(){require(closeAllowed&&finishCalls==0,"finish outside scenario cleanup");finishCalls++;super.finish();}
    @Override void finishShare(){throw forbidden("finishShare");}@Override void start(){throw forbidden("start/import");}@Override boolean submit(){throw forbidden("submit");}@Override void authorize(JSONObject project){throw forbidden("permission");}
    @Override void pick(JSONObject project){throw forbidden("project selection");}@Override void go(int next,int direction){throw forbidden("step navigation");}
    @Override void checkpoint(){require(incoming==null,"draft persistence");checkpointNoops++;}
    @Override void hideKeyboard(){throw forbidden("keyboard");}
    @Override void renderProjects(){require(!isDestroyed(),"render after destruction");renderCalls++;super.renderProjects();}
    @Override void updateProjectRead(){require(!isDestroyed(),"status after destruction");statusCalls++;super.updateProjectRead();}
    @Override protected void onActivityResult(int request,int result,Intent data){throw forbidden("picker/pair result");}@Override protected void onNewIntent(Intent intent){throw forbidden("new Intent");}
    @Override public void startActivity(Intent intent){throw forbidden("navigation");}@Override public void startActivity(Intent intent,Bundle options){throw forbidden("navigation");}
    @Override public void startActivityForResult(Intent intent,int request){throw forbidden("navigation");}@Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("navigation");}
    void releaseRead(){release.countDown();}
    final class MemoryStore extends Store {
        final Map<String,String> cache=new ConcurrentHashMap<>();Thread worker;boolean activityReturned,editorOpen,applied;
        MemoryStore(Context context){super(context);cache.put("projects",catalog);cache.put("activity",ACTIVITY);prefs=new Preferences(cache,true);}
        @Override boolean paired(){return false;}@Override JSONArray pending(){return new JSONArray();}@Override JSONArray tasks(){return new JSONArray();}
        @Override JSONObject get(String path)throws Exception{
            require(Looper.myLooper()!=Looper.getMainLooper()&&!editorOpen&&!applied,"unexpected read thread/write");if(worker==null)worker=Thread.currentThread();require(worker==Thread.currentThread(),"different worker");
            if(path.equals("/projects")){require(projectReads.incrementAndGet()==1&&activityReads.get()==0,"duplicate catalog read");entered.countDown();require(release.await(15,TimeUnit.SECONDS),"memory gate timeout");throw new IOException("Synthetic catalog failure.");}
            require(path.equals("/projects/activity")&&projectReads.get()==1&&activityReads.incrementAndGet()==1,"unknown path/order");activityReturned=true;return json(ACTIVITY);
        }
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}@Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP");}@Override String credential(){throw forbidden("credential");}
        @Override File instanceFiles(){throw forbidden("instance files");}@Override File cacheDir(){throw forbidden("cache directory");}@Override File attachments(){throw forbidden("attachments");}@Override File outbox(){throw forbidden("outbox");}
        @Override synchronized void save(JSONObject task){throw forbidden("save");}@Override void pair(PairingTarget target){throw forbidden("pair");}@Override void revoke(){throw forbidden("revoke");}@Override void forgetLocal(){throw forbidden("forgetLocal");}@Override File downloadDeliverable(String id,JSONObject item){throw forbidden("download");}
    }
    final class Preferences implements SharedPreferences {
        final Map<String,String> values;final boolean writableCache;
        Preferences(Map<String,String> values,boolean writableCache){this.values=values;this.writableCache=writableCache;}
        public Map<String,?> getAll(){return new HashMap<>(values);}public String getString(String key,String fallback){String value=values.get(key);return value==null?fallback:value;}public boolean contains(String key){return values.containsKey(key);}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}public int getInt(String key,int fallback){return fallback;}public long getLong(String key,long fallback){return fallback;}public float getFloat(String key,float fallback){return fallback;}public boolean getBoolean(String key,boolean fallback){return fallback;}
        public Editor edit(){require(writableCache&&memory!=null&&Thread.currentThread()==memory.worker&&memory.activityReturned&&!memory.editorOpen&&!memory.applied,"preferences edit");memory.editorOpen=true;return new CacheEdit(values);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    }
    final class CacheEdit implements SharedPreferences.Editor {
        final Map<String,String> values;boolean put;
        CacheEdit(Map<String,String> values){this.values=values;}
        void valid(){require(memory.editorOpen&&!memory.applied&&Thread.currentThread()==memory.worker,"stale/foreign cache editor");}
        public SharedPreferences.Editor putString(String key,String value){valid();require(!put&&key.equals("activity")&&value.equals(ACTIVITY),"unexpected cache value");put=true;return this;}
        public void apply(){valid();require(put,"empty cache write");values.put("activity",ACTIVITY);memory.applied=true;memory.editorOpen=false;cacheApplies.incrementAndGet();finished.countDown();}
        public boolean commit(){throw forbidden("cache commit");}public SharedPreferences.Editor remove(String key){throw forbidden("preferences remove");}public SharedPreferences.Editor clear(){throw forbidden("preferences clear");}
        public SharedPreferences.Editor putStringSet(String key,Set<String> value){throw forbidden("preferences set write");}public SharedPreferences.Editor putInt(String key,int value){throw forbidden("preferences int write");}public SharedPreferences.Editor putLong(String key,long value){throw forbidden("preferences long write");}public SharedPreferences.Editor putFloat(String key,float value){throw forbidden("preferences float write");}public SharedPreferences.Editor putBoolean(String key,boolean value){throw forbidden("preferences boolean write");}
    }
}
