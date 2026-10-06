package app.droprun;

import android.os.Bundle;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real editor controls with in-memory samples. Never opens an import, saves or submits. */
public final class DemoShareEditorActivity extends ShareActivity {
    static volatile float hierarchyFontScale;
    final Map<String,Object> hierarchyPreferences=new HashMap<>();
    int forbiddenActions,keyboardBypasses,checkpointAttempts,checkpointNoops;
    boolean probeReady,direct,confirmed;
    int initializationCloses,networkAttempts,saveAttempts,pairingAttempts,submitAttempts,startAttempts;
    String originalLanguage;

    AssertionError forbidden(String action){forbiddenActions++;return new AssertionError("Share hierarchy probe forbids "+action);}
    @Override protected void attachBaseContext(Context base){
        if(hierarchyFontScale==0f){super.attachBaseContext(base);return;}
        Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=hierarchyFontScale;
        super.attachBaseContext(new ContextWrapper(base.createConfigurationContext(config)){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){
                if(!name.equals("droprun.preferences")&&!name.equals("droprun.instance."+ShareImport.UNPAIRED))throw forbidden("unknown preferences");
                return new SharedPreferences(){
                    public Map<String,?> getAll(){return new HashMap<>(hierarchyPreferences);}
                    public String getString(String key,String fallback){Object value=hierarchyPreferences.get(key);return value instanceof String?(String)value:fallback;}
                    public Set<String> getStringSet(String key,Set<String> fallback){return fallback;}
                    public int getInt(String key,int fallback){return fallback;}
                    public long getLong(String key,long fallback){return fallback;}
                    public float getFloat(String key,float fallback){return fallback;}
                    public boolean getBoolean(String key,boolean fallback){return fallback;}
                    public boolean contains(String key){return hierarchyPreferences.containsKey(key);}
                    public Editor edit(){throw forbidden("preferences edit");}
                    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
                    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
                };
            }
            @Override public File getFilesDir(){throw forbidden("files directory");}
            @Override public File getCacheDir(){throw forbidden("cache directory");}
            @Override public android.content.ComponentName startService(Intent intent){throw forbidden("service start");}
            @Override public android.content.ComponentName startForegroundService(Intent intent){throw forbidden("foreground service start");}
            @Override public boolean stopService(Intent intent){throw forbidden("service stop");}
        });
    }

    @Override Bundle recoveryState(Bundle state){Bundle display=new Bundle();display.putBoolean("sent",true);return display;}
    @Override void build(){L.language(getIntent().getStringExtra("language"));super.build();}
    @Override protected void onCreate(Bundle state){
        if(hierarchyFontScale!=0f){hierarchyPreferences.put("language",getIntent().getStringExtra("language"));hierarchyPreferences.put("appearance",getIntent().getStringExtra("appearance"));}
        super.onCreate(state);
        if(incoming!=null)throw new AssertionError("Editor probe must never open an import");
        originalLanguage=store.preferences.getString("language","en");direct=getIntent().getBooleanExtra("direct",true);confirmed=getIntent().getBooleanExtra("confirmed",true);
        final JSONArray projects=new JSONArray(),models=new JSONArray();
        try{
            projects.put(new JSONObject().put("id","ui-probe-project").put("name",L.t("Local UI sample","本地界面示例")).put("available",true).put("permission",new JSONObject().put("enabled",true)));
            models.put(new JSONObject().put("id","probe-fast").put("displayName","Codex · UI sample").put("isDefault",true).put("defaultEffort","medium").put("efforts",new JSONArray().put("low").put("medium").put("high")));
            models.put(new JSONObject().put("id","probe-thorough").put("displayName","Codex · Another sample").put("defaultEffort","high").put("efforts",new JSONArray().put("low").put("medium").put("high").put("xhigh")));
            if(getIntent().getBooleanExtra("modelDisclosure",false))models.getJSONObject(1).getJSONArray("efforts").put("future-effort");
        }catch(Exception e){throw new AssertionError(e);}
        store=new Store(this){
            @Override boolean paired(){return false;}
            @Override boolean online(){return false;}
            @Override boolean directExecution(){return direct;}
            @Override JSONArray projects(){return projects;}
            @Override JSONArray models(){return models;}
            @Override JSONArray activity(){return hierarchyFontScale==0f?super.activity():new JSONArray();}
            @Override JSONArray pending(){return hierarchyFontScale==0f?super.pending():new JSONArray();}
            @Override JSONArray tasks(){return hierarchyFontScale==0f?super.tasks():new JSONArray();}
            @Override String projectLabel(JSONObject project){return project.optString("name");}
            @Override String defaultEffort(String id){return model(id).optString("defaultEffort");}
            @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){networkAttempts++;throw new AssertionError("Editor probe must not call an API");}
            @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){networkAttempts++;throw new AssertionError("Editor probe must not make a request");}
            @Override void save(JSONObject task){saveAttempts++;throw new AssertionError("Editor probe must not save a task");}
            @Override void pair(PairingTarget target){pairingAttempts++;throw new AssertionError("Editor probe must not pair");}
            @Override File attachments(){throw new AssertionError("Editor probe must not access drafts");}
            @Override File outbox(){throw new AssertionError("Editor probe must not access the outbox");}
        };
        store.select("","");L.language(getIntent().getStringExtra("language"));
        shared="https://example.invalid/ui-sample";selected="ui-probe-project";model="probe-fast";effort="medium";draft="";step=1;receiving=false;sent=false;
        if(getIntent().getBooleanExtra("materialReview",false))materialSample("mixed");
        TextView notice=Ui.text(this,L.t("UI probe · nothing saved or sent.","界面探针 · 未保存或发送任何分享。"),12,0xFFFFFFFF);notice.setPadding(dp(20),dp(4),dp(20),0);root.addView(notice,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        if(hierarchyFontScale!=0f){notice.setText(L.t("UI probe · memory only","界面探针 · 仅内存"));notice.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,12);notice.setTag("hierarchy-marker");if(getIntent().getBooleanExtra("compactHeight",false)&&getIntent().getBooleanExtra("modelDisclosure",false)){root.removeView(notice);notice.setTextColor(Ui.MUTED);notice.setPadding(0,0,0,dp(4));sheet.addView(notice,0);}}
        stage.addView(stepNote(),new ViewGroup.LayoutParams(-1,-2));dots.setActive(1,false);back.setVisibility(View.VISIBLE);probeReady=true;
    }
    void materialSample(String kind){
        if(hierarchyFontScale==0f||!getIntent().getBooleanExtra("materialReview",false)||!getIntent().getBooleanExtra("modelDisclosure",false))throw forbidden("material sample outside opt-in memory probe");
        StringBuilder text=new StringBuilder("  Received sample text, including its original spacing.\n");
        for(int n=1;n<=12;n++)text.append("Source line ").append(n).append(": keep the complete text and distinguish the final destination. 参考内容保留。\n");
        text.append("SOURCE_TAIL — 原文结尾完整保留。  \n");
        String links="https://example.invalid/posts/alpha?layout=compact&focus=card%20one#motionA\nhttps://example.invalid/posts/beta?layout=wide&focus=card%20two#motionB\n";
        shared=kind.equals("files")?"":kind.equals("link")?"  https://example.invalid/posts/alpha?layout=compact#motionA  ":kind.equals("text")?text.toString():links+text;
        attachments=new JSONArray();
        if(kind.equals("mixed")||kind.equals("files"))try{for(int n=1;n<=8;n++)attachments.put(new JSONObject().put("name","Interface reference collection - shared prefix - 完整文件名 🟢 - final variant 0"+n+".png").put("mime","image/png"));}catch(Exception error){throw new AssertionError(error);}
    }
    boolean executionSettingConfirmed(){return confirmed;}
    @Override void draftFeedback(){if(draftStatus!=null){draftStatus.setText(L.t("UI sample · note not saved.","界面示例 · 留言未保存。"));retryDraft.setVisibility(View.GONE);}}
    @Override void start(){startAttempts++;throw new AssertionError("Editor probe must never start an import or refresh");}
    @Override boolean submit(){submitAttempts++;return false;}
    @Override void checkpoint(){if(getIntent().getBooleanExtra("modelDisclosure",false)){if(incoming!=null){checkpointAttempts++;throw forbidden("checkpoint persistence");}checkpointNoops++;return;}super.checkpoint();}
    @Override void hideKeyboard(){if(hierarchyFontScale!=0f){keyboardBypasses++;return;}super.hideKeyboard();}
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){if(hierarchyFontScale!=0f)throw forbidden("navigation");super.startActivityForResult(intent,request,options);}
    @Override void close(){if(!probeReady){initializationCloses++;return;}super.close();}
    @Override protected void onDestroy(){L.language(originalLanguage);super.onDestroy();}
}
