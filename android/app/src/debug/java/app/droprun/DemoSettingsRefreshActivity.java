package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Closed memory GET script; exercises inherited Settings refresh and cache logic. */
public final class DemoSettingsRefreshActivity extends SettingsActivity {
    enum Scene {
        OFFLINE_RECOVERY("en","light",1f,2),
        NULL_RECOVERY_FONT2("zh","dark",2f,2),
        CACHED_EMPTY_FAILURE("en","light",1f,2),
        POSITION_AND_DESTROY_FONT2("zh","dark",2f,3);
        final String language,appearance;final float font;final int episodes;
        Scene(String language,String appearance,float font,int episodes){this.language=language;this.appearance=appearance;this.font=font;this.episodes=episodes;}
    }
    private static final class Permit {
        final String nonce;final Scene scene;
        Permit(String nonce,Scene scene){this.nonce=nonce;this.scene=scene;}
    }
    private static final AtomicReference<Permit> nextPermit=new AtomicReference<>();
    static void arm(boolean optIn,String nonce,Scene scene){
        if(!optIn||nonce==null||!UUID.fromString(nonce).toString().equals(nonce)||scene==null)throw new AssertionError("Missing canonical closed-probe permission");
        if(!nextPermit.compareAndSet(null,new Permit(nonce,scene)))throw new AssertionError("A launch permit already exists; stop");
    }
    private Permit permit;private Store closedStore;private String previousLanguage;
    final Map<String,Object> global=new HashMap<>(),cached=new ConcurrentHashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),readEpisodes=new AtomicInteger(),readCompleted=new AtomicInteger(),cacheApplies=new AtomicInteger();
    final AtomicInteger projectsGets=new AtomicInteger(),settingsGets=new AtomicInteger(),retentionGets=new AtomicInteger();
    int controllerStarts,controllerCallbacks,renderCalls,updatesAfterClose;boolean entered,destroyed,observedLoading,workerWasMain;
    volatile Thread readWorker;volatile Episode active;TextView marker;
    private int pathIndex;private Cache expectedCache=Cache.NONE;private String expectedProjects;
    private long settingsReadAt;private boolean editorOpen;
    private enum Cache { NONE,PROJECTS,SETTINGS,RETENTION }
    private static final String RETENTION="{\"rawDays\":7,\"artifactDays\":30}";
    static final class Episode {
        final int number;final CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),readDone=new CountDownLatch(1),callback=new CountDownLatch(1);
        Episode(int number){this.number=number;if(number==1)release.countDown();}
    }
    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Settings refresh probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        Permit approved=nextPermit.get();
        if(approved==null||!UUID.fromString(approved.nonce).toString().equals(approved.nonce))throw forbidden("missing pre-context nonce gate");
        if(!nextPermit.compareAndSet(approved,null))throw forbidden("changed launch permit");
        permit=approved;if(!base.getPackageName().endsWith(".debug"))throw forbidden("public package");
        previousLanguage=L.chinese()?"zh":"en";
        global.put("language",permit.scene.language);global.put("appearance",permit.scene.appearance);
        Configuration configuration=new Configuration(base.getResources().getConfiguration());configuration.fontScale=permit.scene.font;
        super.attachBaseContext(new ContextWrapper(base.createConfigurationContext(configuration)){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(name.equals("droprun.preferences"))return memoryPreferences(global,false);
                if(name.equals("droprun.instance."+ShareImport.UNPAIRED))return memoryPreferences(cached,true);
                throw forbidden("unknown preference scope");
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
    @Override protected void onCreate(Bundle state){
        Intent intent=getIntent();
        if(permit==null||!intent.getBooleanExtra("settingsManualRefreshOptIn",false)||!permit.nonce.equals(intent.getStringExtra("evidenceNonce"))||!permit.scene.name().equals(intent.getStringExtra("scene")))throw forbidden("intent gate mismatch");
        if(state!=null)throw forbidden("recreation");
        super.onCreate(null);
        marker=Ui.text(this,L.t("UI probe · memory only · no setting sent","界面验证 · 仅内存 · 未发送设置"),12,Ui.AMBER);
        ((android.widget.LinearLayout)body.getParent()).addView(marker,((android.widget.LinearLayout)body.getParent()).indexOfChild(body));entered=true;
    }
    @Override void modelSection(){
        if(closedStore==null)closedStore=new Store(this){
            @Override JSONObject get(String path)throws Exception{return scriptedGet(path);}
            @Override String credential(){throw forbidden("credential read");}
            @Override boolean paired(){throw forbidden("pairing/credential state");}
            @Override boolean online(){throw forbidden("network connectivity read");}
            @Override JSONObject api(String path,String method,byte[] data,String mime,String filename){throw forbidden("API");}
            @Override JSONObject request(String origin,String path,String method,byte[] data,String mime,String filename,String token){throw forbidden("HTTP request");}
            @Override void saveDefaults(String model,String effort){throw forbidden("default preference save");}
            @Override JSONObject setDirectExecution(boolean enabled){throw forbidden("execution write");}
            @Override JSONObject setProjectPermission(String id,boolean enabled){throw forbidden("permission write");}
            @Override void pair(PairingTarget target){throw forbidden("pairing");}
            @Override File attachments(){throw forbidden("attachments");}
            @Override File outbox(){throw forbidden("outbox");}
            @Override File cacheDir(){throw forbidden("cache files");}
            @Override File instanceFiles(){throw forbidden("instance files");}
        };
        store=closedStore;super.modelSection();
    }
    @Override void render(){if(destroyed)updatesAfterClose++;renderCalls++;super.render();}
    @Override void updateModelRefresh(){
        if(destroyed)updatesAfterClose++;
        super.updateModelRefresh();
        if(refreshing&&!observedLoading){controllerStarts++;if(controllerStarts>permit.scene.episodes)throw forbidden("extra controller read");active=new Episode(controllerStarts);observedLoading=true;}
        else if(!refreshing&&observedLoading){controllerCallbacks++;observedLoading=false;if(active==null)throw forbidden("callback without read episode");active.callback.countDown();}
    }
    private JSONObject scriptedGet(String path)throws Exception{
        if(permit==null||Looper.myLooper()==Looper.getMainLooper())throw forbidden("unprepared/main-thread read");
        String expected=pathIndex==0?"/projects":pathIndex==1?"/device/settings":"/device/retention";
        if(!expected.equals(path)||editorOpen||expectedCache!=Cache.NONE)throw forbidden("unknown/out-of-order GET or uncommitted cache");
        if(pathIndex==0){
            int number=readEpisodes.incrementAndGet();if(number>permit.scene.episodes||active==null||active.number!=number)throw forbidden("extra or unknown read episode");
            readWorker=Thread.currentThread();workerWasMain=Looper.myLooper()==Looper.getMainLooper();projectsGets.incrementAndGet();pathIndex=1;active.started.countDown();
            if(!active.release.await(10,TimeUnit.SECONDS))throw forbidden("unreleased known read gate");
            if(permit.scene==Scene.NULL_RECOVERY_FONT2&&number==1)throw new IOException();
            if(permit.scene==Scene.CACHED_EMPTY_FAILURE&&number==2)throw new IOException("");
            boolean online=permit.scene!=Scene.OFFLINE_RECOVERY||number!=1;
            int count=!online?0:permit.scene==Scene.POSITION_AND_DESTROY_FONT2&&number>=2?2:1;
            JSONObject data=catalog(online,count);expectedProjects=data.toString();expectedCache=Cache.PROJECTS;return data;
        }
        if(pathIndex==1){settingsGets.incrementAndGet();pathIndex=2;settingsReadAt=System.currentTimeMillis();expectedCache=Cache.SETTINGS;return new JSONObject().put("directExecution",true);}
        retentionGets.incrementAndGet();pathIndex=3;expectedCache=Cache.RETENTION;return new JSONObject(RETENTION);
    }
    private JSONObject catalog(boolean online,int count)throws JSONException{
        JSONArray models=new JSONArray();
        for(int n=0;n<count;n++)models.put(new JSONObject().put("id","closed-model-"+n).put("displayName",permit.scene.language.equals("zh")?"仅内存的长名称模型 · 保留完整名称与推理选择 "+(n+1):"Memory model "+(n+1)).put("isDefault",n==0).put("defaultEffort","medium").put("efforts",new JSONArray().put("low").put("medium").put("high")));
        return new JSONObject().put("name",permit.scene.language.equals("zh")?"仅内存电脑":"Memory computer").put("online",online).put("lastSeen",System.currentTimeMillis()).put("projects",new JSONArray()).put("models",models);
    }
    void releaseKnownRead(){Episode known=active;if(known==null)throw forbidden("release without known episode");known.release.countDown();}
    String nonce(){return permit.nonce;}Scene scene(){return permit.scene;}
    @Override void changeMode(boolean direct){throw forbidden("mode decision");}
    @Override void saveMode(boolean direct){throw forbidden("mode save");}
    @Override java.util.concurrent.Callable<Void> modeWork(boolean direct){throw forbidden("any mode Work");}
    @Override void setPermission(String id,boolean enabled){throw forbidden("project permission");}
    @Override void disconnect(){throw forbidden("disconnect");}
    @Override public void startActivity(Intent intent){throw forbidden("activity navigation");}
    @Override public void startActivity(Intent intent,Bundle options){throw forbidden("activity navigation");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}
    @Override protected void onDestroy(){Episode known=active;if(known!=null)known.release.countDown();super.onDestroy();destroyed=true;L.language(previousLanguage);}

    private SharedPreferences memoryPreferences(Map<String,Object> data,boolean instance){return new SharedPreferences(){
        private void checkRead(String key){if(key.equals("credential")||key.equals("token"))throw forbidden("credential preference read");}
        public Map<String,?> getAll(){return new HashMap<>(data);}
        public String getString(String key,String fallback){checkRead(key);Object value=data.get(key);return value instanceof String?(String)value:fallback;}
        public Set<String> getStringSet(String key,Set<String> fallback){checkRead(key);return fallback;}
        public int getInt(String key,int fallback){checkRead(key);Object value=data.get(key);return value instanceof Number?((Number)value).intValue():fallback;}
        public long getLong(String key,long fallback){checkRead(key);Object value=data.get(key);return value instanceof Number?((Number)value).longValue():fallback;}
        public float getFloat(String key,float fallback){checkRead(key);Object value=data.get(key);return value instanceof Number?((Number)value).floatValue():fallback;}
        public boolean getBoolean(String key,boolean fallback){checkRead(key);Object value=data.get(key);return value instanceof Boolean?(Boolean)value:fallback;}
        public boolean contains(String key){checkRead(key);return data.containsKey(key);}
        public Editor edit(){
            if(!instance||Looper.myLooper()==Looper.getMainLooper()||expectedCache==Cache.NONE||editorOpen)throw forbidden("non-read-cache preferences edit");
            editorOpen=true;final Cache kind=expectedCache;final Episode episode=active;final Map<String,Object> edits=new HashMap<>();final Set<String> removes=new HashSet<>();
            return new Editor(){
                private void add(String key,Object value){if(edits.containsKey(key)||removes.contains(key))throw forbidden("repeated cache key");edits.put(key,value);}
                public Editor putString(String key,String value){
                    if(value!=null&&(kind==Cache.PROJECTS&&key.equals("projects")&&value.equals(expectedProjects)||kind==Cache.RETENTION&&key.equals("retention")&&value.equals(RETENTION))){add(key,value);return this;}
                    throw forbidden("unexpected cache string");
                }
                public Editor putBoolean(String key,boolean value){if(kind!=Cache.SETTINGS||!value||!key.equals("directExecution")&&!key.equals("settingsKnown"))throw forbidden("unexpected cache boolean");add(key,value);return this;}
                public Editor putLong(String key,long value){if(kind!=Cache.SETTINGS||!key.equals("settingsCheckedAt")||value<settingsReadAt||value>System.currentTimeMillis())throw forbidden("unexpected cache timestamp");add(key,value);return this;}
                public Editor remove(String key){if(kind!=Cache.SETTINGS||!key.equals("settingsError")||!removes.add(key)||edits.containsKey(key))throw forbidden("unexpected cache removal");return this;}
                public Editor putStringSet(String key,Set<String> value){throw forbidden("cache string-set write");}
                public Editor putInt(String key,int value){throw forbidden("cache int write");}
                public Editor putFloat(String key,float value){throw forbidden("cache float write");}
                public Editor clear(){throw forbidden("cache clear");}
                public boolean commit(){throw forbidden("cache commit");}
                public void apply(){
                    Set<String> expected=kind==Cache.PROJECTS?Collections.singleton("projects"):kind==Cache.RETENTION?Collections.singleton("retention"):new HashSet<>(Arrays.asList("directExecution","settingsKnown","settingsCheckedAt"));
                    if(!editorOpen||expectedCache!=kind||active!=episode||!edits.keySet().equals(expected)||!removes.equals(kind==Cache.SETTINGS?Collections.singleton("settingsError"):Collections.emptySet()))throw forbidden("incomplete or reused cache edit");
                    for(String key:removes)data.remove(key);data.putAll(edits);cacheApplies.incrementAndGet();editorOpen=false;expectedCache=Cache.NONE;
                    if(kind==Cache.RETENTION){pathIndex=0;readCompleted.incrementAndGet();episode.readDone.countDown();}
                }
            };
        }
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
    };}
}
