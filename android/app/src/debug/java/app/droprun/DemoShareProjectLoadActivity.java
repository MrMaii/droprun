package app.droprun;

import android.content.*;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Two fixed memory sequences through the real project refresh executor and callback. */
public final class DemoShareProjectLoadActivity extends ShareActivity {
    static final String ALPHA="share-load-alpha",BETA="share-load-beta",MARKER="SHARE_PROJECT_LOAD_MEMORY_ONLY";
    static final String EMPTY="{\"projects\":[],\"models\":[]}",ACTIVITY="{\"projects\":[]}";
    static final String CATALOG="{\"projects\":[{\"id\":\"share-load-alpha\",\"name\":\"Memory Alpha\",\"available\":true,\"permission\":{\"enabled\":true}},{\"id\":\"share-load-beta\",\"name\":\"Memory Beta\",\"available\":true,\"permission\":{\"enabled\":true}}],\"models\":[{\"id\":\"memory-model\",\"displayName\":\"Memory model\",\"isDefault\":true,\"defaultEffort\":\"medium\",\"efforts\":[\"low\",\"medium\",\"high\"]}]}";
    final Map<String,String> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),projectReads=new AtomicInteger(),activityReads=new AtomicInteger(),cacheWrites=new AtomicInteger();
    String nonce,language,theme,scene;boolean ready,closeAllowed;int initializationCloses,finishCalls,checkpointNoops,keyboardNoops,renderCalls,statusCalls,gaugeCalls;
    TextView marker;MemoryStore memory;volatile Read read=new Read(0);
    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Project load fixture forbids "+action);}
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
        require(intent!=null&&new ComponentName(this,DemoShareProjectLoadActivity.class).equals(intent.getComponent())&&"accepted".equals(intent.getStringExtra("shareProjectLoadProbe"))&&intent.getData()==null&&intent.getClipData()==null,"missing opt-in");
        nonce=intent.getStringExtra("evidenceNonce");language=intent.getStringExtra("language");theme=intent.getStringExtra("appearance");scene=intent.getStringExtra("scene");
        boolean canonical=false;try{canonical=nonce!=null&&UUID.fromString(nonce).toString().equals(nonce);}catch(IllegalArgumentException ignored){}
        require(canonical&&("en".equals(language)&&"light".equals(theme)&&"empty".equals(scene)||"zh".equals(language)&&"dark".equals(theme)&&"cached".equals(scene)),"unknown fixed window");
        try{require(!getPackageManager().getActivityInfo(new ComponentName(this,DemoShareProjectLoadActivity.class),0).exported,"exported fixture");}catch(android.content.pm.PackageManager.NameNotFoundException error){throw forbidden("missing registration");}
        global.put("language",language);global.put("appearance",theme);global.put("relay","");global.put("instanceId","");super.onCreate(null);
        require(incoming==null&&initializationCloses==1&&!closing,"unexpected import/initial close");store=memory=new MemoryStore(this);
        shared="https://example.invalid/memory-reference";attachments=new JSONArray().put(json("{\"name\":\"memory-reference.txt\",\"mime\":\"text/plain\"}"));selected=BETA;model="memory-model";effort="high";draft="Preserve this memory note. 保留留言。";step=0;receiving=false;sent=false;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        marker=Ui.text(this,MARKER,10,Ui.MUTED);sheet.addView(marker,0);stage.addView(stepProject(),new ViewGroup.LayoutParams(-1,-2));dots.setVisibility(View.VISIBLE);dots.setActive(0,false);ready=true;refreshProjects(true);
    }
    @Override Bundle recoveryState(Bundle state){require(state==null&&!ready,"recovery");Bundle skip=new Bundle();skip.putBoolean("sent",true);return skip;}
    @Override void close(){require(!ready&&initializationCloses==0&&sent&&incoming==null,"close");initializationCloses++;}
    void allowScenarioClose(){main();require(ready&&!closeAllowed&&!isDestroyed(),"duplicate cleanup");closeAllowed=true;}
    @Override public void finish(){require(closeAllowed&&finishCalls==0,"finish outside scenario cleanup");finishCalls++;super.finish();}
    @Override void finishShare(){throw forbidden("finishShare");}@Override void start(){throw forbidden("start/import");}@Override boolean submit(){throw forbidden("submit");}@Override void authorize(JSONObject project){throw forbidden("permission");}
    @Override void checkpoint(){require(incoming==null,"draft persistence");checkpointNoops++;}
    @Override void draftFeedback(){if(draftStatus!=null){draftStatus.setText("Memory note · not saved");retryDraft.setVisibility(View.GONE);}}
    @Override void hideKeyboard(){keyboardNoops++;}
    @Override View stepNote(){View view=super.stepNote();note.setShowSoftInputOnFocus(false);return view;}
    @Override void renderProjects(){require(!isDestroyed(),"render after destruction");renderCalls++;super.renderProjects();}
    @Override void updateProjectRead(){require(!isDestroyed(),"status after destruction");statusCalls++;super.updateProjectRead();}
    @Override void updateGauge(){require(!isDestroyed(),"gauge after destruction");gaugeCalls++;super.updateGauge();}
    @Override protected void onActivityResult(int request,int result,Intent data){throw forbidden("picker/pair result");}@Override protected void onNewIntent(Intent intent){throw forbidden("new Intent");}
    @Override public void startActivity(Intent intent){throw forbidden("navigation");}@Override public void startActivity(Intent intent,Bundle options){throw forbidden("navigation");}
    @Override public void startActivityForResult(Intent intent,int request){throw forbidden("navigation");}@Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("navigation");}
    void nextRead(){main();int next=read.index+1;require(!gone()&&projectRead==null&&read.finished.getCount()==0&&next<=(scene.equals("empty")?2:1)&&memory.writeKey==null&&!memory.editorOpen,"unexpected read sequence");read=new Read(next);}
    void releaseRead(){read.release.countDown();}
    static final class Read {final int index;int paths;final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);Read(int index){this.index=index;}}
    final class MemoryStore extends Store {
        final Map<String,String> cache=new ConcurrentHashMap<>();Thread worker;String writeKey,writeValue;boolean editorOpen;
        MemoryStore(Context context){super(context);cache.put("projects",json(scene.equals("cached")?CATALOG:EMPTY).toString());cache.put("activity",json(ACTIVITY).toString());prefs=new Preferences(cache,true);}
        @Override boolean paired(){return false;}@Override boolean online(){throw forbidden("online");}@Override JSONArray pending(){return new JSONArray();}@Override JSONArray tasks(){return new JSONArray();}
        @Override JSONObject get(String path)throws Exception{
            Read current=read;require(Looper.myLooper()!=Looper.getMainLooper()&&writeKey==null&&!editorOpen,"unexpected read thread/write");
            if(worker==null)worker=Thread.currentThread();require(worker==Thread.currentThread(),"different worker");
            if(path.equals("/projects")){
                require(current.paths++==0,"duplicate projects read");projectReads.incrementAndGet();current.entered.countDown();
                require(current.release.await(10,TimeUnit.SECONDS),"memory gate timeout");if(current.index==0)throw new IOException("Synthetic project read failure.");
                return response("projects",scene.equals("empty")&&current.index==1?EMPTY:CATALOG);
            }
            require(path.equals("/projects/activity")&&current.paths++==1,"unknown path/order");activityReads.incrementAndGet();
            try{if(scene.equals("empty")&&current.index==1)throw new IOException("Synthetic activity read failure.");return response("activity",ACTIVITY);}finally{current.finished.countDown();}
        }
        JSONObject response(String key,String source){JSONObject value=json(source);writeKey=key;writeValue=value.toString();return value;}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("API");}@Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("HTTP");}@Override String credential(){throw forbidden("credential");}
        @Override File instanceFiles(){throw forbidden("instance files");}@Override File cacheDir(){throw forbidden("cache directory");}@Override File attachments(){throw forbidden("attachments");}@Override File outbox(){throw forbidden("outbox");}
        @Override synchronized void save(JSONObject task){throw forbidden("save");}@Override void pair(PairingTarget target){throw forbidden("pair");}@Override void revoke(){throw forbidden("revoke");}@Override void forgetLocal(){throw forbidden("forgetLocal");}@Override File downloadDeliverable(String id,JSONObject item){throw forbidden("download");}
    }
    final class Preferences implements SharedPreferences {
        final Map<String,String> values;final boolean writableCache;
        Preferences(Map<String,String> values,boolean writableCache){this.values=values;this.writableCache=writableCache;}
        public Map<String,?> getAll(){return new HashMap<>(values);}public String getString(String key,String fallback){String value=values.get(key);return value==null?fallback:value;}public boolean contains(String key){return values.containsKey(key);}
        public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}public int getInt(String key,int fallback){return fallback;}public long getLong(String key,long fallback){return fallback;}public float getFloat(String key,float fallback){return fallback;}public boolean getBoolean(String key,boolean fallback){return fallback;}
        public Editor edit(){require(writableCache&&memory!=null&&Thread.currentThread()==memory.worker&&memory.writeKey!=null&&!memory.editorOpen,"preferences edit");memory.editorOpen=true;return new CacheEdit(values,memory.writeKey,memory.writeValue);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    }
    final class CacheEdit implements SharedPreferences.Editor {
        final Map<String,String> values;final String key,value;boolean put,applied;
        CacheEdit(Map<String,String> values,String key,String value){this.values=values;this.key=key;this.value=value;}
        void valid(){require(!applied&&memory.editorOpen&&Thread.currentThread()==memory.worker&&key.equals(memory.writeKey)&&value.equals(memory.writeValue),"stale/foreign cache editor");}
        public SharedPreferences.Editor putString(String key,String value){valid();require(!put&&this.key.equals(key)&&this.value.equals(value),"unexpected cache value");put=true;return this;}
        public void apply(){valid();require(put,"empty cache write");values.put(key,value);applied=true;memory.writeKey=null;memory.writeValue=null;memory.editorOpen=false;cacheWrites.incrementAndGet();}
        public boolean commit(){throw forbidden("cache commit");}public SharedPreferences.Editor remove(String key){throw forbidden("preferences remove");}public SharedPreferences.Editor clear(){throw forbidden("preferences clear");}
        public SharedPreferences.Editor putStringSet(String key,Set<String> value){throw forbidden("preferences set write");}public SharedPreferences.Editor putInt(String key,int value){throw forbidden("preferences int write");}public SharedPreferences.Editor putLong(String key,long value){throw forbidden("preferences long write");}public SharedPreferences.Editor putFloat(String key,float value){throw forbidden("preferences float write");}public SharedPreferences.Editor putBoolean(String key,boolean value){throw forbidden("preferences boolean write");}
    }
}
