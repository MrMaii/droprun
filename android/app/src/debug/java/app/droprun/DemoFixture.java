package app.droprun;

import android.app.Activity;
import org.json.*;

/** Screenshot fixtures only. Compiled exclusively into the debug application. */
final class DemoFixture {
    static final String TASK="d054b3c9-cbda-49f5-9bf0-7785143664a1";
    static final String NOTICE="Demo data · no agent work is sent";
    static void seed(Activity activity){
        try {
            Store store=new Store(activity);String origin="https://preview.example.invalid",instance="39d6b0c8-5560-4e03-8e29-383933d389d1";
            store.preferences.edit().putString("relay",origin).putString("instanceId",instance).putString("language",activity.getIntent().getStringExtra("language")==null?"en":activity.getIntent().getStringExtra("language")).putString("appearance",activity.getIntent().getStringExtra("appearance")==null?"light":activity.getIntent().getStringExtra("appearance")).commit();
            store.select(origin,instance);store.vault.save("debug-fixture-not-a-server-credential");long now=System.currentTimeMillis();
            JSONArray projects=new JSONArray();for(String[] item:new String[][]{{"demo-studio","Studio website"},{"demo-journal","Field notes"},{"demo-orbit","Orbit"}})projects.put(new JSONObject().put("id",item[0]).put("name",item[1]).put("permission",new JSONObject().put("enabled",true).put("version","demo")));
            JSONArray models=new JSONArray().put(new JSONObject().put("id","example-model").put("displayName","Computer default").put("isDefault",true).put("defaultEffort","medium").put("efforts",new JSONArray().put("low").put("medium").put("high")));
            JSONObject context=new JSONObject().put("projects",projects).put("models",models).put("name","My Windows PC").put("online",true).put("lastSeen",now);
            JSONArray summaries=new JSONArray();for(int n=0;n<projects.length();n++){JSONObject p=projects.getJSONObject(n);summaries.put(new JSONObject().put("id",p.getString("id")).put("name",p.getString("name")).put("task_count",new int[]{3,2,1}[n]).put("dispatch_count",new int[]{5,3,1}[n]).put("active_count",0).put("attention_count",0).put("last_status","completed").put("available",true).put("last_dispatch_at",now-n*3600000));}
            JSONObject task=new JSONObject().put("id",TASK).put("root_task_id",TASK).put("project_id","demo-studio").put("project_name","Studio website").put("title","Give the homepage room to breathe").put("status","completed").put("execution_mode","review").put("created_at",now-26*60000).put("updated_at",now-20*60000).put("thread_id","demo-thread").put("content","https://example.com/design-reference").put("message","Bring this quiet spacing and softer hierarchy to our homepage.").put("report","## Demonstration report\nThis sample shows how a delivery is presented. No agent ran and no project files were changed.\n\n## What a real report includes\nThe material actually read, where the idea applies, the work performed and the commands or screenshots that verify it.");
            JSONArray tasks=new JSONArray().put(task);
            store.prefs.edit().putString("projects",context.toString()).putString("activity",new JSONObject().put("projects",summaries).toString()).putString("tasks",new JSONObject().put("tasks",tasks).toString()).putString("task:"+TASK,task.toString()).putString("history:demo-studio",new JSONObject().put("tasks",tasks).put("hasMore",false).toString()).putBoolean("directExecution",false).putBoolean("settingsKnown",true).putString("deviceId","demo-device").putBoolean("notificationsInitialized",true).remove("syncError").remove("receiverNotice").commit();
        } catch(Exception e){throw new IllegalStateException("Could not prepare debug fixtures",e);}
    }
}
