package app.droprun;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

/** Proposed debug UI probe: permission outcomes are simulated, never requested or saved. */
public final class DemoPermissionMotionActivity extends ShareActivity {
    static final String FIRST="permission-ui-project-a",SECOND="permission-ui-project-b";
    final Map<String,Object> global=new HashMap<>();
    final AtomicInteger forbiddenActions=new AtomicInteger(),noteTransitions=new AtomicInteger();
    final CountDownLatch destroyedSignal=new CountDownLatch(1);
    final List<Ui.Glass> navigationDismissTargets=new ArrayList<>();
    boolean probeReady;int initializationCloses;String previousLanguage;
    Ui.Glass confirmedGlass,replacementGlass;Runnable duringDismiss;

    AssertionError forbidden(String action){forbiddenActions.incrementAndGet();return new AssertionError("Permission-motion probe forbids "+action);}
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
    @Override Bundle recoveryState(Bundle state){Bundle bypass=new Bundle();bypass.putBoolean("sent",true);return bypass;}
    @Override protected void onCreate(Bundle state){
        previousLanguage=L.chinese()?"zh":"en";global.put("language","zh".equals(getIntent().getStringExtra("language"))?"zh":"en");global.put("appearance","light");
        super.onCreate(state);
        if(incoming!=null)throw forbidden("import creation");
        store=new ProbeStore(this);shared="https://example.invalid/permission-ui";draft="";selected="";step=0;sent=false;receiving=false;
        root.addView(marker(),new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        stage.addView(stepProject(),new ViewGroup.LayoutParams(-1,-2));dots.setActive(0,false);back.setVisibility(View.INVISIBLE);probeReady=true;
    }
    TextView marker(){TextView notice=Ui.text(this,L.t("UI probe · simulated permission result · no request sent","界面验证 · 模拟授权结果 · 未发送权限请求"),12,Ui.MUTED);notice.setBackgroundColor(Ui.SURFACE);notice.setPadding(dp(12),dp(6),dp(12),dp(6));notice.setTag("permission-motion-ui-probe");return notice;}
    Ui.Glass openPermission(String id){
        JSONObject project=store.project(id);if(project==null)throw new AssertionError("Unknown in-memory sample");
        authorize(project);if(android.os.Build.VERSION.SDK_INT>=28)dialog.overlay.setAccessibilityPaneTitle(L.t("Simulated permission","模拟授权"));dialog.card.addView(marker(),0);return dialog;
    }
    Ui.CheckView simulateConfirmation(Ui.Glass glass,String id){
        if(dialog!=glass||glass.dismissed)throw new AssertionError("Simulate only the current permission UI");
        selected=id;confirmedGlass=glass;authorized(glass);glass.card.addView(marker());
        for(int n=0;n<glass.card.getChildCount();n++)if(glass.card.getChildAt(n) instanceof Ui.CheckView)return (Ui.CheckView)glass.card.getChildAt(n);
        throw new AssertionError("Actual CheckView is missing");
    }
    @Override void closeDialog(Runnable end){
        Ui.Glass open=dialog;
        if(probeReady&&end!=null)navigationDismissTargets.add(open);
        super.closeDialog(end);
        if(probeReady&&end!=null&&open==confirmedGlass&&duringDismiss!=null){Runnable action=duringDismiss;duringDismiss=null;action.run();}
    }
    @Override void go(int target,int direction){if(probeReady&&target==1)noteTransitions.incrementAndGet();super.go(target,direction);}
    @Override void close(){if(!probeReady){initializationCloses++;return;}throw forbidden("share close");}
    @Override void start(){throw forbidden("import or refresh start");}
    @Override void retryIncoming(){throw forbidden("import retry");}
    @Override boolean submit(){throw forbidden("Send");}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){throw forbidden("activity navigation");}
    @Override protected void onDestroy(){super.onDestroy();L.language(previousLanguage);destroyedSignal.countDown();}

    final class ProbeStore extends Store {
        final JSONArray projects=new JSONArray();
        ProbeStore(Context context){super(context);try{
            projects.put(new JSONObject().put("id",FIRST).put("name",L.t("Local sample A","本地示例 A")).put("available",true).put("permission",new JSONObject().put("enabled",false)));
            projects.put(new JSONObject().put("id",SECOND).put("name",L.t("Local sample B","本地示例 B")).put("available",true).put("permission",new JSONObject().put("enabled",false)));
        }catch(Exception error){throw new AssertionError(error);}}
        @Override boolean paired(){return false;}
        @Override boolean online(){return false;}
        @Override JSONArray projects(){return projects;}
        @Override JSONArray activity(){return new JSONArray();}
        @Override JSONArray pending(){return new JSONArray();}
        @Override JSONArray tasks(){return new JSONArray();}
        @Override JSONArray models(){return new JSONArray();}
        @Override String projectLabel(JSONObject project){return project.optString("name");}
        @Override String defaultModel(){return "";}
        @Override String defaultEffort(String id){return "";}
        @Override String credential(){throw forbidden("credential read");}
        @Override JSONObject setProjectPermission(String id,boolean enabled){throw forbidden("permission request");}
        @Override JSONObject api(String path,String method,byte[] data,String mime,String filename){throw forbidden("API");}
        @Override JSONObject request(String origin,String path,String method,byte[] data,String mime,String filename,String token){throw forbidden("HTTP request");}
        @Override synchronized void save(JSONObject task){throw forbidden("task save");}
        @Override void pair(PairingTarget target){throw forbidden("pairing");}
        @Override void sync(){throw forbidden("sync");}
        @Override File attachments(){throw forbidden("attachments");}
        @Override File instanceFiles(){throw forbidden("instance files");}
        @Override File cacheDir(){throw forbidden("cache files");}
        @Override File outbox(){throw forbidden("outbox");}
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
