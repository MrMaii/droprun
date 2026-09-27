package app.droprun;

import org.json.*;

public final class TaskCacheTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        JSONObject cached=new JSONObject("{\"tasks\":[{\"id\":\"old\",\"status\":\"completed\",\"report\":\"large original report\"},{\"id\":\"active\",\"status\":\"running\"},{\"id\":\"deleted\",\"status\":\"failed\"}]}");
        JSONObject delta=new JSONObject("{\"tasks\":[{\"id\":\"active\",\"status\":\"completed\",\"report\":\"verified result\"}],\"taskIds\":[\"active\",\"old\"],\"syncCursor\":9000,\"stats\":{\"total\":2,\"active\":0}}");
        JSONObject result=Store.mergeTasks(cached,delta);JSONArray rows=result.getJSONArray("tasks");
        check(rows.length()==2,"A removed task must leave the cache");
        check(rows.getJSONObject(0).getString("id").equals("active")&&rows.getJSONObject(0).getString("report").equals("verified result"),"Apply final report and server display order together");
        check(rows.getJSONObject(1).getString("report").equals("large original report"),"An unchanged report survives a tiny status response");
        check(result.getJSONObject("stats").getInt("active")==0&&result.getLong("syncCursor")==9000,"Stats and cursor belong to the returned delta");
        check(cached.getJSONArray("tasks").length()==3&&cached.getJSONArray("tasks").getJSONObject(1).getString("status").equals("running"),"Do not mutate cached state before the response is complete");
        JSONObject unchanged=new JSONObject("{\"tasks\":[],\"taskIds\":[\"active\",\"old\"],\"syncCursor\":10000}");
        check(Store.mergeTasks(result,unchanged).getJSONArray("tasks").getJSONObject(0).getString("report").equals("verified result"),"No changes must not empty the task list");
        boolean missing=false;try{Store.mergeTasks(new JSONObject(),unchanged);}catch(java.io.IOException expected){missing=true;}
        check(missing,"A lost local cache must demand a full refresh instead of silently losing active tasks");
        JSONObject legacy=new JSONObject("{\"tasks\":[{\"id\":\"legacy\"}]}");
        check(Store.mergeTasks(result,legacy)==legacy,"Old relay responses must remain fully compatible");
        System.out.println("PASS: Android task delta merging, report retention, deletion and cache recovery");
    }
}
