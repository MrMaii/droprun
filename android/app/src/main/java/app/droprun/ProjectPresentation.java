package app.droprun;

import org.json.*;
import java.util.*;

/** Server totals are authoritative; pending work remains distinct until it is acknowledged. */
final class ProjectPresentation {
    static List<JSONObject> sharing(JSONArray catalog,List<JSONObject> recent,String selected,String last){
        Map<String,Integer> rank=new HashMap<>();for(int n=0;n<recent.size();n++)rank.put(recent.get(n).optString("id"),n);
        if(!last.isEmpty()&&!rank.containsKey(last))rank.put(last,-1);
        if(!selected.isEmpty())rank.put(selected,-2);
        List<JSONObject> result=new ArrayList<>();for(int n=0;n<catalog.length();n++){JSONObject project=catalog.optJSONObject(n);if(project!=null)result.add(project);}
        result.sort(Comparator.comparingInt(p->rank.getOrDefault(p.optString("id"),Integer.MAX_VALUE)));return result;
    }
    /** Only ambiguous names need an identity hint. Extend a colliding prefix rather than guessing. */
    static String label(String id,String name,JSONArray catalog,JSONArray history){
        Set<String> others=new HashSet<>();String key=name.trim().toLowerCase(Locale.ROOT);
        for(JSONArray group:new JSONArray[]{catalog,history})for(int n=0;group!=null&&n<group.length();n++){JSONObject project=group.optJSONObject(n);if(project!=null&&!id.equals(project.optString("id"))&&key.equals(project.optString("name").trim().toLowerCase(Locale.ROOT)))others.add(project.optString("id"));}
        if(others.isEmpty())return name;
        int length=Math.min(8,id.length());for(String other:others)while(length<id.length()&&other.startsWith(id.substring(0,length)))length++;
        return name+" · "+id.substring(0,length);
    }
    static List<JSONObject> merge(JSONArray summaries,JSONArray pending,JSONArray acknowledged){
        Map<String,JSONObject> projects=new LinkedHashMap<>();Set<String> known=new HashSet<>();
        for(int n=0;acknowledged!=null&&n<acknowledged.length();n++)known.add(acknowledged.optJSONObject(n).optString("id"));
        try {
            for(int n=0;summaries!=null&&n<summaries.length();n++){JSONObject project=new JSONObject(summaries.getJSONObject(n).toString());if(project.optInt("dispatch_count")>0)projects.put(project.getString("id"),project);}
            for(int n=0;pending!=null&&n<pending.length();n++){
                JSONObject task=pending.getJSONObject(n);if(known.contains(task.optString("id")))continue;String id=task.optString("projectId");if(id.isEmpty())continue;
                JSONObject p=projects.get(id);if(p==null){p=new JSONObject().put("id",id).put("name",task.optString("projectName",id)).put("available",true);projects.put(id,p);}
                p.put("pending_count",p.optInt("pending_count")+1);p.put("last_dispatch_at",Math.max(p.optLong("last_dispatch_at"),task.optLong("createdAt")));
            }
        }catch(JSONException malformed){throw new IllegalArgumentException(malformed);}
        List<JSONObject> result=new ArrayList<>(projects.values());result.sort((a,b)->Long.compare(b.optLong("last_dispatch_at"),a.optLong("last_dispatch_at")));return result;
    }
    static String counts(JSONObject project){int tasks=project.optInt("task_count"),dispatches=project.optInt("dispatch_count");return tasks+L.t(tasks==1?" task · ":" tasks · "," 项任务 · ")+dispatches+L.t(dispatches==1?" dispatch":" dispatches"," 次交办");}
    static String state(JSONObject project){int attention=project.optInt("attention_count"),active=project.optInt("active_count");if(attention>0)return attention+L.t(" need your attention"," 项等你处理");if(active>0)return active+L.t(" in progress"," 项进行中");if(project.optInt("dispatch_count")==0)return L.t("Ready when you're connected","联网后自动发送");return TaskPresentation.status(project.optString("last_status","completed"));}
}
