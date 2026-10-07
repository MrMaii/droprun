package app.droprun;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Debug-only Back fixture. It proves no download, byte verification, picker, or save. */
public final class DemoDeliveryBackActivity extends DeliverablesActivity {
    static final String TASK_ID="d311beef-0000-4000-8000-000000000001",MARKER="DELIVERY_BACK_MEMORY_ONLY_V1";
    private static final String ITEM_JSON="{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"name\":\"memory-preview.bin\",\"kind\":\"file\",\"size\":0,\"sha256\":\"1111111111111111111111111111111111111111111111111111111111111111\"}";
    private static final String SECOND_ITEM_JSON="{\"id\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"name\":\"memory-change.diff\",\"kind\":\"diff\",\"size\":0,\"sha256\":\"2222222222222222222222222222222222222222222222222222222222222222\"}";
    private final Map<String,String> globalValues=new HashMap<>();
    private boolean created;
    private SaveOperation syntheticSaving;
    String nonce,language,theme,scene;
    final AtomicInteger forbiddenActions=new AtomicInteger();
    int listRenders,previewRenders,taskReads;
    TextView marker;
    private AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Delivery Back fixture forbids "+action);}
    private void require(boolean condition,String action){if(!condition)throw forbidden(action);}
    private void requireMain(){require(Looper.myLooper()==Looper.getMainLooper(),"off-main fixture action");}
    private static JSONObject json(String value){try{return new JSONObject(value);}catch(JSONException error){throw new AssertionError(error);}}

    @Override protected void attachBaseContext(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=1f;
        super.attachBaseContext(new MemoryContext(base.createConfigurationContext(config)));
    }
    @Override public void onCreate(Bundle state){
        requireMain();Intent intent=getIntent();
        require(BuildConfig.DEBUG&&!created&&state==null&&getLastNonConfigurationInstance()==null,"restored or non-debug launch");
        require(intent!=null&&new ComponentName(this,DemoDeliveryBackActivity.class).equals(intent.getComponent())&&"accepted".equals(intent.getStringExtra("deliveryBackProbe"))&&TASK_ID.equals(intent.getStringExtra("taskId"))&&intent.getData()==null&&intent.getClipData()==null,"Intent outside opt-in scope");
        nonce=intent.getStringExtra("evidenceNonce");language=intent.getStringExtra("language");theme=intent.getStringExtra("appearance");scene=intent.getStringExtra("scene");
        boolean canonical=false;try{canonical=nonce!=null&&UUID.fromString(nonce).toString().equals(nonce);}catch(IllegalArgumentException ignored){}
        require(canonical&&("en".equals(language)||"zh".equals(language))&&("light".equals(theme)||"dark".equals(theme))&&("preview".equals(scene)||"picking".equals(scene)),"invalid fixture configuration");
        try{require(!getPackageManager().getActivityInfo(new ComponentName(this,DemoDeliveryBackActivity.class),0).exported,"exported fixture");}
        catch(PackageManager.NameNotFoundException error){throw forbidden("missing nonexported registration");}
        globalValues.put("language",language);globalValues.put("appearance",theme);globalValues.put("relay","");globalValues.put("instanceId","");created=true;
        super.onCreate(null);showMemoryPreview();if("picking".equals(scene))setMemoryPicking(true);
    }
    @Override Store createStore(){return new Store(this){
        @Override boolean paired(){return false;}
        @Override JSONObject task(String id){require(TASK_ID.equals(id),"unknown task read");taskReads++;return json("{\"id\":\""+TASK_ID+"\",\"execution_mode\":\"direct\",\"status\":\"completed\"}");}
        @Override String credential(){throw forbidden("credential");}
        @Override JSONObject get(String path){throw forbidden("network get");}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){throw forbidden("network api");}
        @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){throw forbidden("network request");}
        @Override File downloadDeliverable(String id,JSONObject item){throw forbidden("downloadDeliverable");}
        @Override synchronized void save(JSONObject task){throw forbidden("save");}
        @Override void pair(PairingTarget target){throw forbidden("pair");}
        @Override void revoke(){throw forbidden("revoke");}
        @Override void forgetLocal(){throw forbidden("forgetLocal");}
        @Override File instanceFiles(){throw forbidden("instanceFiles");}
        @Override File cacheDir(){throw forbidden("cacheDir");}
        @Override File attachments(){throw forbidden("attachments");}
        @Override File outbox(){throw forbidden("outbox");}
        @Override void deleteAttachment(File file){throw forbidden("deleteAttachment");}
        @Override boolean online(){throw forbidden("online");}
        @Override void sync(){throw forbidden("sync");}
        @Override void sync(boolean refreshContext){throw forbidden("sync");}
    };}
    @Override void base(String title){super.base(title);marker=Ui.text(this,MARKER,10,Ui.MUTED);page.addView(marker);}
    @Override void loadList(){
        requireMain();if(saveBusy()||picking)return;require(syntheticSaving==null,"list during synthetic saving");
        saving=null;saveMessage="";saveError="";clearFile();
        super.renderList(new JSONArray().put(json(ITEM_JSON)).put(json(SECOND_ITEM_JSON)));listRenders++;
    }
    @Override void clearFile(){currentFile=null;currentItem=null;}
    @Override void download(JSONObject item){throw forbidden("file row download");}
    @Override void restorePreview(){throw forbidden("restorePreview");}
    @Override void saveFile(){throw forbidden("Save or picker");}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){throw forbidden("picker result");}
    @Override protected void onNewIntent(Intent intent){throw forbidden("new Intent");}
    @Override protected void onSaveInstanceState(Bundle state){throw forbidden("state persistence");}
    @Override public Object onRetainNonConfigurationInstance(){return null;}
    @Override public void recreate(){throw forbidden("recreate");}
    void showMemoryPreview(){
        requireMain();require(created&&!isFinishing()&&!isDestroyed()&&!picking&&saving==null&&syntheticSaving==null,"preview outside idle scope");
        currentFile=new NoIoFile();currentItem=json(ITEM_JSON);super.showFile();previewRenders++;
    }
    void beginSyntheticSaving(){
        requireMain();require("preview".equals(scene)&&currentFile instanceof NoIoFile&&currentItem!=null&&!picking&&saving==null&&syntheticSaving==null&&!isFinishing()&&!isDestroyed(),"synthetic save outside preview");
        // Real parent operation; never submitted, given an observer, or run.
        syntheticSaving=new SaveOperation(store,TASK_ID,currentFile,currentItem,Uri.EMPTY);saving=syntheticSaving;updateSaveUi();
    }
    void endSyntheticSaving(){
        requireMain();require(syntheticSaving!=null&&saving==syntheticSaving&&!saving.done&&!saving.cleanup&&saving.observer==null&&!isFinishing()&&!isDestroyed(),"synthetic save changed or escaped");
        saving=null;syntheticSaving=null;updateSaveUi();
    }
    void setMemoryPicking(boolean value){
        requireMain();require(value&&"picking".equals(scene)&&currentFile instanceof NoIoFile&&currentItem!=null&&!picking&&saving==null&&!isFinishing()&&!isDestroyed(),"picking outside fixture scope");
        picking=true;updateSaveUi();
    }
    // Back, finish, and onDestroy remain the real parent/framework implementations.
    @Override public void startActivity(Intent intent){throw forbidden("startActivity");}
    @Override public void startActivity(Intent intent,Bundle options){throw forbidden("startActivity");}
    @Override public void startActivityForResult(Intent intent,int requestCode){throw forbidden("startActivityForResult");}
    @Override public void startActivityForResult(Intent intent,int requestCode,Bundle options){throw forbidden("startActivityForResult");}
    private final class NoIoFile extends File {
        NoIoFile(){super("delivery-back-memory-only");}
        @Override public long length(){return 0;}
        // Caught by the real preview before Files.readAllBytes can receive a Path.
        @Override public Path toPath(){throw new UnsupportedOperationException("Memory preview has no filesystem path");}
        @Override public String getPath(){throw forbidden("file path");}
        @Override public String getCanonicalPath(){throw forbidden("canonical file path");}
        @Override public File getCanonicalFile(){throw forbidden("canonical file");}
        @Override public boolean isFile(){throw forbidden("file stat");}
        @Override public boolean delete(){throw forbidden("file delete");}
    }
    private final class MemoryContext extends ContextWrapper {
        private final SharedPreferences global=new MemoryPreferences(globalValues),instance=new MemoryPreferences(Collections.emptyMap());
        MemoryContext(Context base){super(base);}
        @Override public Context getApplicationContext(){return this;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){
            require(mode==Context.MODE_PRIVATE,"preference mode");
            if("droprun.preferences".equals(name))return global;
            if(("droprun.instance."+Store.scope("","")).equals(name))return instance;
            throw forbidden("unknown preferences");
        }
        @Override public File getFilesDir(){throw forbidden("files directory");}
        @Override public File getCacheDir(){throw forbidden("cache directory");}
        @Override public ComponentName startService(Intent intent){throw forbidden("startService");}
        @Override public ComponentName startForegroundService(Intent intent){throw forbidden("startForegroundService");}
        @Override public boolean stopService(Intent intent){throw forbidden("stopService");}
        @Override public void startActivity(Intent intent){throw forbidden("context startActivity");}
        @Override public void startActivity(Intent intent,Bundle options){throw forbidden("context startActivity");}
    }
    private final class MemoryPreferences implements SharedPreferences {
        private final Map<String,String> values;
        MemoryPreferences(Map<String,String> source){values=Collections.unmodifiableMap(source);}
        @Override public Map<String,?> getAll(){return values;}
        @Override public String getString(String key,String fallback){if(!values.containsKey(key))throw forbidden("unexpected preference read");return values.get(key);}
        @Override public boolean contains(String key){if(!values.containsKey(key))throw forbidden("unexpected preference contains");return true;}
        @Override public Set<String> getStringSet(String key,Set<String> fallback){throw forbidden("preference set read");}
        @Override public int getInt(String key,int fallback){throw forbidden("preference int read");}
        @Override public long getLong(String key,long fallback){throw forbidden("preference long read");}
        @Override public float getFloat(String key,float fallback){throw forbidden("preference float read");}
        @Override public boolean getBoolean(String key,boolean fallback){throw forbidden("preference boolean read");}
        @Override public Editor edit(){throw forbidden("preference write");}
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){throw forbidden("preference listener");}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){throw forbidden("preference listener");}
    }
}
