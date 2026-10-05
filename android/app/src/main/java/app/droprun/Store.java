package app.droprun;

import android.content.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

public class Store {
    static final Object SYNC_LOCK=new Object();
    final Context context;
    final android.content.SharedPreferences preferences;
    android.content.SharedPreferences prefs;
    String relay,instanceId,scope;
    TokenVault vault;
    Store(Context c) {
        context=c.getApplicationContext(); preferences=context.getSharedPreferences("droprun.preferences",Context.MODE_PRIVATE);
        L.language(preferences.getString("language","en"));
        select(preferences.getString("relay",""),preferences.getString("instanceId",""));
    }
    void select(String origin,String instance) {
        relay=origin;instanceId=instance;scope=scope(origin,instance);
        prefs=context.getSharedPreferences("droprun.instance."+scope,Context.MODE_PRIVATE);vault=new TokenVault(prefs,scope);
    }
    static String scope(String origin,String instance) {
        try {byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest((origin+"\n"+instance).getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte b:hash)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    String relay(){return relay;}
    boolean paired() { try{return scope.equals(scope(preferences.getString("relay",""),preferences.getString("instanceId","")))&&!relay.isEmpty()&&!instanceId.isEmpty()&&!vault.read().isEmpty();}catch(Exception unavailable){return false;} }
    String credential()throws Exception {String token=vault.read();if(token.isEmpty())throw new IOException(L.t("Connect this phone first.","请先连接这台手机。"));return token;}

    /** Accepts a bare pairing code, a droprun://pair?code= link or the relay's https /pair#code= link. */
    static String codeFromScan(String raw) {
        if(raw==null)return null;
        String text=raw.trim();
        Matcher m=Pattern.compile("code=([0-9A-Za-z-]{20,30})").matcher(text);
        if(m.find())return normalizeCode(m.group(1));
        return normalizeCode(text);
    }
    static String normalizeCode(String value){
        String code=value.toUpperCase(Locale.ROOT).replaceAll("[\\s-]","");
        return code.matches("[0-9A-F]{20}")?code:null;
    }

    JSONObject api(String path, String method, byte[] body, String mime, String filename) throws Exception {
        if(!scope.equals(scope(preferences.getString("relay",""),preferences.getString("instanceId",""))))throw new IOException("The connection changed. Reopen this screen.");
        return request(relay,path,method,body,mime,filename,credential());
    }
    static final class HttpFailure extends IOException {
        final int status;
        HttpFailure(int status,String message){super(message);this.status=status;}
    }
    JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token)throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL(origin+path).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(20000); conn.setReadTimeout(body==null?20000:90000); conn.setRequestMethod(method);
        if(token!=null)conn.setRequestProperty("Authorization", "Bearer "+token);
        if (body!=null) { conn.setDoOutput(true); conn.setRequestProperty("Content-Type", mime); if(filename!=null)conn.setRequestProperty("X-Filename", URLEncoder.encode(filename,"UTF-8").replace("+","%20")); conn.setFixedLengthStreamingMode(body.length); try(OutputStream o=conn.getOutputStream()){o.write(body);} }
        int code=conn.getResponseCode();
        InputStream in=code<400?conn.getInputStream():conn.getErrorStream();
        String text; try(in){ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(in!=null){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);}text=bytes.toString("UTF-8");} finally {conn.disconnect();}
        JSONObject result; try {result=new JSONObject(text);}catch(Exception e){String message=L.t("Service unavailable (","服务暂不可用 (")+code+")";if(code<200||code>=300)throw new HttpFailure(code,message);throw new IOException(message);}
        if(code<200||code>=300)throw new HttpFailure(code,result.optString("error", L.t("Network error ","网络错误 ")+code));
        return result;
    }
    JSONObject get(String path)throws Exception{return api(path,"GET",null,null,null);}
    JSONObject post(String path,JSONObject b)throws Exception{return api(path,"POST",b.toString().getBytes(StandardCharsets.UTF_8),"application/json",null);}

    File downloadDeliverable(String taskId,JSONObject item)throws Exception{
        String id=item.getString("id"),expected=item.getString("sha256");long size=item.getLong("size");
        if(!taskId.matches("[a-zA-Z0-9-]{20,64}")||!id.matches("[a-f0-9]{64}")||!expected.matches("[a-f0-9]{64}")||size<0||size>50L*1024*1024)throw new IOException(L.t("Invalid delivery file information","无效产物信息"));
        HttpURLConnection conn=(HttpURLConnection)new URL(relay+"/tasks/"+taskId+"/deliverables/"+id).openConnection();
        conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(20000);conn.setReadTimeout(90000);conn.setRequestProperty("Authorization","Bearer "+credential());
        File file=File.createTempFile("droprun-delivery-",".bin",cacheDir());boolean ok=false;
        try{
            int status=conn.getResponseCode();if(status!=200)throw new IOException(status==401?L.t("This pairing expired. Connect again.","配对已失效，请重新连接"):status==404?L.t("This file was deleted or belongs to another device.","产物已删除或不属于此手机"):L.t("Download failed: ","产物下载失败：")+status);
            java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long total=0;
            try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){total+=n;if(total>size)throw new IOException(L.t("File size mismatch. The download was not saved.","产物大小不符，未保存"));digest.update(buffer,0,n);out.write(buffer,0,n);}}
            StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            if(total!=size||!actual.toString().equals(expected))throw new IOException(L.t("Integrity check failed. The download was not saved.","产物完整性校验失败，未保存"));ok=true;return file;
        }finally{conn.disconnect();if(!ok)file.delete();}
    }

    /** Cached previews can outlive an Activity; never reuse an old verification label for changed bytes. */
    static void verifyDeliverable(File file,JSONObject item)throws Exception{
        long size=item.optLong("size",-1);String expected=item.optString("sha256");
        if(size<0||size>50L*1024*1024||!expected.matches("[a-f0-9]{64}")||!file.isFile()||file.length()!=size)throw new IOException(L.t("The cached file changed or expired. Download it again.","缓存文件已改变或失效，请重新下载。"));
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long total=0;
        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){total+=n;if(total>size)break;digest.update(buffer,0,n);}}
        StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format(Locale.ROOT,"%02x",b&255));
        if(total!=size||!actual.toString().equals(expected))throw new IOException(L.t("The cached file changed or expired. Download it again.","缓存文件已改变或失效，请重新下载。"));
    }

    // ---- cached state -------------------------------------------------------------------------
    JSONObject projectsData(){try{return new JSONObject(prefs.getString("projects","{}"));}catch(Exception e){return new JSONObject();}}
    JSONArray projects(){JSONArray a=projectsData().optJSONArray("projects");return a==null?new JSONArray():a;}
    String projectLabel(JSONObject project){return projectLabel(project.optString("id"),project.optString("name"));}
    String projectLabel(String id,String name){return ProjectPresentation.label(id,name,projects(),activity());}
    JSONArray models(){JSONArray a=projectsData().optJSONArray("models");return a==null?new JSONArray():a;}
    String computerName(){return projectsData().optString("name",L.t("Computer","电脑"));}
    boolean computerOnline(){JSONObject d=projectsData();return d.optBoolean("online")&&System.currentTimeMillis()-d.optLong("lastSeen")<90000;}
    JSONObject tasksData(){try{return new JSONObject(prefs.getString("tasks","{}"));}catch(Exception e){return new JSONObject();}}
    JSONArray tasks(){JSONArray a=tasksData().optJSONArray("tasks");return a==null?new JSONArray():a;}
    JSONObject stats(){JSONObject s=tasksData().optJSONObject("stats");return s==null?new JSONObject():s;}
    JSONObject task(String id){JSONArray tasks=tasks();for(int n=0;n<tasks.length();n++){JSONObject t=tasks.optJSONObject(n);if(t!=null&&t.optString("id").equals(id))return t;}try{return new JSONObject(prefs.getString("task:"+id,""));}catch(Exception absent){return null;}}
    JSONArray activity(){try{return new JSONObject(prefs.getString("activity","{}")).optJSONArray("projects");}catch(Exception e){return null;}}
    JSONObject history(String projectId){try{return new JSONObject(prefs.getString("history:"+projectId,"{}"));}catch(Exception e){return new JSONObject();}}
    JSONObject loadHistory(String projectId,String cursor)throws Exception {
        String requestKey="historyRequest:"+projectId,requestId=UUID.randomUUID().toString();
        synchronized(SYNC_LOCK){prefs.edit().putString(requestKey,requestId).apply();}
        try{
            JSONObject page=get("/tasks?project_id="+URLEncoder.encode(projectId,"UTF-8")+"&limit=50"+(cursor==null||cursor.isEmpty()?"":"&cursor="+URLEncoder.encode(cursor,"UTF-8")));
            // Network reads never hold the outbox lock. Only the newest request may publish a page.
            synchronized(SYNC_LOCK){
            if(!requestId.equals(prefs.getString(requestKey,"")))return history(projectId);
            JSONArray combined=new JSONArray();LinkedHashMap<String,JSONObject> rows=new LinkedHashMap<>();
            if(cursor!=null&&!cursor.isEmpty()){JSONArray old=history(projectId).optJSONArray("tasks");for(int n=0;old!=null&&n<old.length();n++){JSONObject t=old.getJSONObject(n);rows.put(t.getString("id"),t);}}
            JSONArray next=page.optJSONArray("tasks");android.content.SharedPreferences.Editor edit=prefs.edit();
            for(int n=0;next!=null&&n<next.length();n++){JSONObject t=next.getJSONObject(n);rows.put(t.getString("id"),t);JSONObject current=task(t.getString("id"));if(current==null||current.optLong("updated_at")<=t.optLong("updated_at"))edit.putString("task:"+t.getString("id"),t.toString());}
            for(JSONObject t:rows.values())combined.put(t);page.put("tasks",combined);edit.putString("history:"+projectId,page.toString()).remove(requestKey).apply();return page;
            }
        }catch(Exception e){synchronized(SYNC_LOCK){if(requestId.equals(prefs.getString(requestKey,"")))prefs.edit().remove(requestKey).apply();}throw e;}
    }
    JSONObject project(String id){JSONArray projects=projects();for(int n=0;n<projects.length();n++){JSONObject p=projects.optJSONObject(n);if(p!=null&&p.optString("id").equals(id))return p;}return null;}
    static boolean projectEnabled(JSONObject project){JSONObject p=project==null?null:project.optJSONObject("permission");return p!=null&&p.optBoolean("enabled");}
    static boolean finished(String status){return Arrays.asList("completed","failed","cancelled","blocked").contains(status);}
    int watchCount(){int count=pending().length();JSONArray tasks=tasks();for(int n=0;n<tasks.length();n++){JSONObject t=tasks.optJSONObject(n);if(t!=null&&TaskWatchPolicy.needsWatch(t.optString("status"),t.optString("preview_status")))count++;}return count;}

    /** Model/effort the share sheet preselects: the user's saved default, else the computer's default. */
    String defaultModel(){
        String saved=prefs.getString("defaultModel","");JSONArray models=models();
        for(int n=0;n<models.length();n++){JSONObject m=models.optJSONObject(n);if(m!=null&&m.optString("id").equals(saved))return saved;}
        for(int n=0;n<models.length();n++){JSONObject m=models.optJSONObject(n);if(m!=null&&m.optBoolean("isDefault"))return m.optString("id");}
        return models.length()>0?models.optJSONObject(0).optString("id"):"";
    }
    String defaultEffort(String modelId){
        JSONObject model=model(modelId);String saved=prefs.getString("defaultEffort","");
        if(model==null)return saved;
        JSONArray efforts=model.optJSONArray("efforts");
        if(efforts!=null)for(int n=0;n<efforts.length();n++)if(efforts.optString(n).equals(saved))return saved;
        return model.optString("defaultEffort","");
    }
    JSONObject model(String id){JSONArray models=models();for(int n=0;n<models.length();n++){JSONObject m=models.optJSONObject(n);if(m!=null&&m.optString("id").equals(id))return m;}return null;}
    void saveDefaults(String model,String effort){prefs.edit().putString("defaultModel",model).putString("defaultEffort",effort).apply();}

    // ---- settings ------------------------------------------------------------------------------
    JSONObject getSettings()throws Exception{
        synchronized(SYNC_LOCK){try{return cacheSettings(get("/device/settings"));}
        catch(Exception e){prefs.edit().putString("settingsError",L.t("Cannot confirm the execution setting right now","执行模式暂无法确认")).apply();throw e;}}
    }
    JSONObject cacheSettings(JSONObject settings)throws Exception{
        if(!(settings.opt("directExecution") instanceof Boolean))throw new IOException(L.t("Invalid execution setting response. Refresh and try again.","执行模式响应无效，请刷新后重试。"));
        prefs.edit().putBoolean("directExecution",settings.getBoolean("directExecution")).putBoolean("settingsKnown",true).putLong("settingsCheckedAt",System.currentTimeMillis()).remove("settingsError").apply();return settings;
    }
    boolean directExecution(){return prefs.getBoolean("directExecution",true);}
    JSONObject setDirectExecution(boolean enabled)throws Exception{
        synchronized(SYNC_LOCK){
        if(!online())throw new IOException(L.t("Changing execution mode requires a connection. This change was not saved.","更改执行方式需要联网；本次更改未保存。"));
        JSONObject body=new JSONObject().put("directExecution",enabled);if(enabled)body.put("riskAccepted",true);
        try{JSONObject result=post("/device/settings",body);if(!(result.opt("directExecution") instanceof Boolean)||result.getBoolean("directExecution")!=enabled)throw new IOException(L.t("The execution setting is not confirmed. Refresh to check.","执行方式尚未确认，请刷新查看。"));return cacheSettings(result);}
        catch(Exception e){prefs.edit().putString("settingsError",L.t("The execution setting could not be confirmed. Refresh online.","执行方式更改结果尚未确认，请联网刷新")).apply();throw e;}
        }
    }
    JSONObject setProjectPermission(String projectId,boolean enabled)throws Exception{
        JSONObject result=post("/projects/"+projectId+"/permission",new JSONObject().put("enabled",enabled));
        JSONObject data=get("/projects");prefs.edit().putString("projects",data.toString()).apply();return result;
    }

    // ---- tasks ---------------------------------------------------------------------------------
    JSONObject refreshTask(String taskId)throws Exception{
        synchronized(SYNC_LOCK){JSONObject task=get("/tasks/"+taskId);prefs.edit().putString("task:"+taskId,task.toString()).remove("deleteUnavailable:"+taskId).apply();JSONObject cached=tasksData();JSONArray tasks=cached.optJSONArray("tasks");for(int n=0;tasks!=null&&n<tasks.length();n++)if(tasks.getJSONObject(n).optString("id").equals(taskId))tasks.put(n,task);prefs.edit().putString("tasks",cached.toString()).apply();return task;}
    }
    JSONObject decidePlan(String taskId,String planVersion,boolean approve)throws Exception{
        if(!online())throw new IOException(L.t("Connect to approve or reject a plan. This decision has not been sent.","批准或拒绝计划需要联网；本次决定未发送。"));
        JSONObject task=post("/tasks/"+taskId+"/plan-decision",new JSONObject().put("planVersion",planVersion).put("decision",approve?"approved":"rejected"));
        synchronized(SYNC_LOCK){JSONObject cached=tasksData();JSONArray tasks=cached.optJSONArray("tasks");for(int n=0;tasks!=null&&n<tasks.length();n++)if(tasks.getJSONObject(n).optString("id").equals(taskId)){tasks.put(n,task);break;}prefs.edit().putString("tasks",cached.toString()).apply();}
        ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(taskId.hashCode());return task;
    }
    void decideApproval(String taskId,String approvalId,boolean approve)throws Exception{post("/tasks/"+taskId+"/approvals/"+approvalId,new JSONObject().put("decision",approve?"approved":"denied"));sync();}
    void reopenPreview(String taskId)throws Exception{
        synchronized(SYNC_LOCK){
            JSONObject result=post("/tasks/"+taskId+"/preview/reopen",new JSONObject());
            if(!result.optBoolean("requested"))throw new IOException(L.t("Reopening was not confirmed. Refresh and try again.","重开预览尚未确认，请刷新后重试。"));
            JSONObject data=tasksData();JSONArray tasks=data.optJSONArray("tasks");for(int n=0;tasks!=null&&n<tasks.length();n++){JSONObject t=tasks.optJSONObject(n);if(t!=null&&t.optString("id").equals(taskId))t.put("preview_status","reopening");}
            prefs.edit().putString("tasks",data.toString()).putBoolean("previewRequested:"+taskId,true).apply();
        }
    }
    void cancelTask(String id)throws Exception{post("/tasks/"+id+"/cancel",new JSONObject());sync();}
    void deleteTask(String id)throws Exception{
        synchronized(SYNC_LOCK){
            deleteCachedTask(prefs,id,()->api("/tasks/"+id,"DELETE",null,null,null));
            try{cancelPending(id);}catch(Exception cleanup){prefs.edit().putString("syncError",L.t("The task was deleted. Some phone copies could not be removed; they will not be sent again.","任务已删除。部分手机副本未能移除；它们不会再次发送。")).apply();}
        }
        ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
        // The DELETE receipt is final; aggregate refresh happens independently.
        try{SyncJob.soon(context);}catch(RuntimeException ignored){}
    }
    static boolean deletionRequested(android.content.SharedPreferences prefs,String id){return prefs.contains("deletion:"+id);}
    boolean canClearUnavailableTask(String id){return canClearUnavailableTask(prefs,id);}
    static boolean canClearUnavailableTask(android.content.SharedPreferences prefs,String id){return deletionRequested(prefs,id)&&prefs.getBoolean("deleteUnavailable:"+id,false);}
    void clearUnavailableTask(String id)throws Exception{
        if(!scope.equals(new Store(context).scope))throw ShareImport.changed();
        synchronized(SYNC_LOCK){
            clearUnavailableTask(prefs,id);
            try{cancelPending(id);}catch(Exception cleanup){prefs.edit().putString("syncError",L.t("The cached report was cleared. Some phone files could not be removed; the retry copy will not be sent again.","缓存报告已清除。部分手机文件未能移除；重试副本不会再次发送。")).apply();}
        }
        ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
    }
    static void clearUnavailableTask(android.content.SharedPreferences prefs,String id)throws Exception{
        synchronized(SYNC_LOCK){if(!canClearUnavailableTask(prefs,id))throw new IOException(L.t("Refresh this handoff before clearing its cached copy.","请先刷新这条任务，再清除缓存副本。"));evictTask(prefs,id,"local-only");}
    }
    static void requireUndeletedTask(android.content.SharedPreferences prefs,String id)throws IOException{
        if(deletionRequested(prefs,id))throw new IOException(L.t("Deletion was requested for this handoff. Its saved copy cannot be sent again.","这条任务已请求删除，不能再次发送其保存副本。"));
    }
    static void deleteCachedTask(android.content.SharedPreferences prefs,String id,java.util.concurrent.Callable<?> request)throws Exception{
        synchronized(SYNC_LOCK){
            android.content.SharedPreferences.Editor edit=prefs.edit().putString("deletion:"+id,"requested").remove("deleteUnavailable:"+id);
            // A later full sync may omit the inaccessible row; keep its last visible report until a decision.
            try{JSONArray tasks=new JSONObject(prefs.getString("tasks","{}")).optJSONArray("tasks");for(int n=0;tasks!=null&&n<tasks.length();n++){JSONObject task=tasks.optJSONObject(n);if(task!=null&&id.equals(task.optString("id"))){edit.putString("task:"+id,task.toString());break;}}}catch(JSONException ignored){}
            if(!edit.commit())throw new IOException(L.t("Could not save the deletion request on this phone. Nothing was sent; free up space and try again.","无法在手机保存删除请求。请求尚未发送；请释放空间后重试。"));
            try{request.call();}catch(HttpFailure error){if(error.status==404&&!prefs.edit().putBoolean("deleteUnavailable:"+id,true).commit())throw new IOException(L.t("The record is inaccessible, but this phone could not save the local cleanup option. Free up space and retry.","无法访问记录，但手机未能保存本地清理选项。请释放空间后重试。"));throw error;}
            evictDeletedTask(prefs,id);
        }
    }
    static void evictDeletedTask(android.content.SharedPreferences prefs,String id)throws Exception{
        evictTask(prefs,id,"confirmed");
    }
    private static void evictTask(android.content.SharedPreferences prefs,String id,String marker)throws Exception{
        synchronized(SYNC_LOCK){
            android.content.SharedPreferences.Editor edit=prefs.edit().putString("deletion:"+id,marker).remove("deleteUnavailable:"+id).remove("task:"+id).remove("previewRequested:"+id);
            for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
                String key=entry.getKey();
                // Even an uncached page can have been read before the confirmed deletion.
                if(key.startsWith("historyRequest:")){edit.remove(key);continue;}
                if(!key.equals("tasks")&&!key.startsWith("history:"))continue;
                JSONObject data;try{data=new JSONObject((String)entry.getValue());}catch(Exception invalid){continue;}
                boolean changed=false;
                for(String field:new String[]{"tasks","taskIds"}){
                    JSONArray old=data.optJSONArray(field);if(old==null)continue;JSONArray kept=new JSONArray();
                    for(int n=0;n<old.length();n++){Object row=old.get(n);String rowId=row instanceof JSONObject?((JSONObject)row).optString("id"):String.valueOf(row);if(id.equals(rowId))changed=true;else kept.put(row);}
                    data.put(field,kept);
                }
                if(changed)edit.putString(key,data.toString());
            }
            if(!edit.commit())throw new IOException(marker.equals("confirmed")?L.t("The Relay deleted this task, but the phone could not save the updated history. Its retry copy remains blocked. Free up space and refresh.","中转服务已删除任务，但手机未能保存更新后的历史。重试副本仍禁止发送；请释放空间后刷新。"):L.t("This phone could not save the updated cache. Cloud deletion is still unconfirmed; the retry copy remains blocked. Free up space and try again.","手机未能保存更新后的缓存。尚未确认云端删除；重试副本仍禁止发送。请释放空间后重试。"));
        }
    }
    static void clearRevokedCache(android.content.SharedPreferences prefs)throws IOException{
        synchronized(SYNC_LOCK){
            android.content.SharedPreferences.Editor edit=prefs.edit().clear();
            for(Map.Entry<String,?> entry:prefs.getAll().entrySet())if(entry.getKey().startsWith("deletion:")&&entry.getValue() instanceof String)edit.putString(entry.getKey(),(String)entry.getValue());
            if(!edit.commit())throw new IOException(L.t("The Relay disconnected this phone, but its local cache could not be cleared. Try forgetting the connection on this phone.","中转服务已断开这台手机，但本地缓存未能清理。请尝试仅在手机忘记连接。"));
        }
    }
    void revoke()throws Exception{
        synchronized(SYNC_LOCK){
            post("/device/revoke",new JSONObject());
            JSONArray pending=pending(true);
            for(int i=0;i<pending.length();i++)cancelPending(pending.getJSONObject(i).getString("id"));
            vault.clear();clearRevokedCache(prefs);preferences.edit().remove("relay").remove("instanceId").commit();
            ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        }
    }
    /** Forgetting locally is distinct from a confirmed server revocation. Unsent work stays in its own instance directory. */
    void forgetLocal()throws Exception{
        synchronized(SYNC_LOCK){
            prefs.edit().remove("credential").remove("token").commit();
            if(!preferences.edit().remove("relay").remove("instanceId").commit())throw new IOException(L.t("Could not forget this connection. Try again.","未能忘记此连接，请重试。"));
            try{vault.clear();}catch(Exception ignored){}
            context.stopService(new Intent(context,TaskSyncService.class));
            ((android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        }
    }
    void pair(PairingTarget target)throws Exception {
        synchronized(SYNC_LOCK){
            JSONObject health=request(target.relay,"/health","GET",null,null,null,null);
            if(!target.instanceId.equals(health.optString("instanceId"))||!health.optBoolean("ready")||health.optInt("protocolVersion")<2)throw new IOException(L.t("This Relay does not match the pairing link or is not ready.","中转服务与配对链接不匹配，或尚未就绪。"));
            byte[] body=new JSONObject().put("code",target.code).put("instanceId",target.instanceId).toString().getBytes(StandardCharsets.UTF_8);
            JSONObject result=request(target.relay,"/pair","POST",body,"application/json",null,null);
            if(!target.instanceId.equals(result.optString("instanceId")))throw new IOException("The Relay instance changed during pairing.");
            select(target.relay,target.instanceId);vault.save(result.getString("token"));prefs.edit().putString("deviceId",result.getString("deviceId")).commit();
            if(!preferences.edit().putString("relay",relay).putString("instanceId",instanceId).commit())throw new IOException("Could not save the connection.");
        }
    }
    File instanceFiles(){File dir=new File(context.getFilesDir(),"instances/"+scope);dir.mkdirs();return dir;}
    File cacheDir(){File dir=new File(context.getCacheDir(),"instances/"+scope);dir.mkdirs();return dir;}
    File attachments(){File dir=new File(instanceFiles(),"attachments");dir.mkdirs();return dir;}
    void deleteAttachment(File file)throws IOException{
        file.delete();File parent=file.getCanonicalFile().getParentFile();
        if(parent!=null&&attachments().getCanonicalFile().equals(parent.getParentFile())&&parent.getName().matches("[0-9a-f-]{36}")){
            // An outbox entry may have survived a kill just before its import journal was removed.
            new android.util.AtomicFile(new File(parent,"import.json")).delete();parent.delete();
        }
    }
    File outbox(){File f=new File(instanceFiles(),"outbox");f.mkdirs();return f;}
    synchronized void save(JSONObject task)throws Exception{requireUndeletedTask(prefs,task.getString("id"));task.put("instanceId",instanceId);if(!task.has("createdAt"))task.put("createdAt",System.currentTimeMillis());android.util.AtomicFile f=new android.util.AtomicFile(new File(outbox(),task.getString("id")+".json"));FileOutputStream out=null;try{out=f.startWrite();out.write(task.toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}}
    void clearImportJournal(String id)throws IOException{if(!ShareDrafts.uuid(id))return;File dir=new File(attachments(),id);if(!dir.getCanonicalFile().getParentFile().equals(attachments().getCanonicalFile()))throw new IOException("Invalid saved share directory");android.util.AtomicFile journal=new android.util.AtomicFile(new File(dir,"import.json"));journal.delete();if(journal.getBaseFile().exists()||new File(dir,"import.json.bak").exists())throw new IOException(L.t("Could not remove the saved share journal.","未能移除保存的分享记录。"));dir.delete();}
    void cancelPending(String id)throws Exception {synchronized(SYNC_LOCK){JSONArray all=pending(true);for(int n=0;n<all.length();n++){JSONObject task=all.getJSONObject(n);if(!id.equals(task.optString("id")))continue;clearImportJournal(id);JSONArray files=task.optJSONArray("localFiles");for(int j=0;files!=null&&j<files.length();j++){File f=new File(files.getJSONObject(j).getString("path"));if(f.getCanonicalPath().startsWith(instanceFiles().getCanonicalPath()+File.separator))deleteAttachment(f);}File record=new File(outbox(),id+".json");if(!record.delete()&&record.exists())throw new IOException(L.t("Could not remove the saved retry copy. Try again.","未能移除保存的重试副本，请重试。"));return;}}}
    JSONArray pending(){return pending(false);}
    JSONArray pending(boolean includeDeleted){return pending(outbox(),prefs,includeDeleted);}
    static JSONArray pending(File directory,android.content.SharedPreferences prefs,boolean includeDeleted){JSONArray a=new JSONArray();File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files!=null)for(File f:files)try{JSONObject task=new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));if(includeDeleted||!deletionRequested(prefs,task.optString("id")))a.put(task);}catch(Exception ignored){}return a;}
    boolean online(){android.net.ConnectivityManager manager=(android.net.ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);android.net.Network network=manager.getActiveNetwork();android.net.NetworkCapabilities capabilities=manager.getNetworkCapabilities(network);return capabilities!=null&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);}
    /** Server order is authoritative; unchanged reports stay cached and deleted tasks disappear. */
    static JSONObject mergeTasks(JSONObject cached,JSONObject response)throws Exception{
        JSONArray ids=response.optJSONArray("taskIds");if(ids==null)return response;
        Map<String,JSONObject> byId=new HashMap<>();
        for(JSONArray rows:new JSONArray[]{cached.optJSONArray("tasks"),response.optJSONArray("tasks")})for(int n=0;rows!=null&&n<rows.length();n++){
            JSONObject task=rows.optJSONObject(n);if(task!=null)byId.put(task.getString("id"),task);
        }
        JSONArray merged=new JSONArray();
        for(int n=0;n<ids.length();n++){
            String id=ids.getString(n);JSONObject task=byId.get(id);
            if(task==null)throw new IOException(L.t("The task cache is incomplete. A full refresh is required.","任务缓存缺失，需要重新同步。"));
            merged.put(task);
        }
        return new JSONObject(response.toString()).put("tasks",merged);
    }
    void sync()throws Exception{sync(true);}
    void sync(boolean refreshContext)throws Exception{
        synchronized(SYNC_LOCK){
            if(!paired())return;
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException(L.t("Receiving ended. Unsent handoffs are still saved.","接收已结束，未发送的任务仍保留。"));
            try{
            JSONArray all=pending(true);String pendingError=null;List<JSONObject> acceptedTasks=new ArrayList<>();
            for(int i=0;i<all.length();i++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException(L.t("Receiving ended. Unsent handoffs are still saved.","接收已结束，未发送的任务仍保留。"));
                JSONObject saved=all.getJSONObject(i);
                if(deletionRequested(prefs,saved.optString("id"))){try{cancelPending(saved.optString("id"));}catch(Exception cleanup){pendingError=cleanup.getMessage();}continue;}
                try{
                JSONObject t=all.getJSONObject(i);if(!instanceId.equals(t.optString("instanceId")))throw new IOException("This saved task belongs to another Relay.");JSONArray local=t.optJSONArray("localFiles");JSONArray uploaded=t.optJSONArray("assets");if(uploaded==null)uploaded=new JSONArray();
                if(local!=null)for(int j=uploaded.length();j<local.length();j++){
                    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException(L.t("Receiving ended. Unsent attachments are still saved.","接收已结束，未发送的附件仍保留。"));
                    JSONObject a=local.getJSONObject(j);File file=new File(a.getString("path"));
                    JSONObject result=api("/uploads","POST",Files.readAllBytes(file.toPath()),a.getString("mime"),a.getString("name"));uploaded.put(result.getString("id"));t.put("assets",uploaded);save(t);
                }
                String parent=t.optString("parentTaskId");JSONObject accepted=post(parent.isEmpty()?"/tasks":"/tasks/"+parent+"/followup",t);prefs.edit().putString("task:"+t.getString("id"),accepted.toString()).apply();acceptedTasks.add(t);
                }catch(Exception e){
                    pendingError=e.getMessage();JSONObject failed=all.getJSONObject(i);
                    // save() stamps this instance: never adopt a foreign or damaged outbox entry.
                    if(instanceId.equals(failed.optString("instanceId"))){failed.put("sendError",pendingError==null?L.t("Could not send. Retry when connected.","暂时无法发送，请联网重试。"):pendingError);save(failed);}
                }
            }
            JSONObject response=get(refreshContext?"/tasks":"/tasks?since="+prefs.getLong("tasksCursor",0)),tasks;
            try{tasks=mergeTasks(tasksData(),response);}catch(Exception incomplete){response=get("/tasks");tasks=mergeTasks(new JSONObject(),response);}
            prefs.edit().putString("tasks",tasks.toString()).putLong("tasksCursor",response.optLong("syncCursor",0)).apply();
            JSONObject activity=get("/projects/activity");prefs.edit().putString("activity",activity.toString()).apply();
            for(JSONObject accepted:acceptedTasks){clearImportJournal(accepted.getString("id"));new File(outbox(),accepted.getString("id")+".json").delete();JSONArray files=accepted.optJSONArray("localFiles");for(int n=0;files!=null&&n<files.length();n++)deleteAttachment(new File(files.getJSONObject(n).getString("path")));}
            TaskNotifications.update(context,prefs,tasks.optJSONArray("tasks"));
            if(refreshContext||System.currentTimeMillis()-prefs.getLong("projectsCheckedAt",0)>=30000){
                JSONObject projects=get("/projects");prefs.edit().putString("projects",projects.toString()).putLong("projectsCheckedAt",System.currentTimeMillis()).apply();
            }
            if(refreshContext||System.currentTimeMillis()-prefs.getLong("settingsCheckedAt",0)>=60000)getSettings();
            if(pendingError!=null)throw new IOException(pendingError);
            prefs.edit().remove("syncError").apply();
            }catch(Exception e){prefs.edit().putString("syncError",online()?e.getMessage():L.t("Phone offline. Saved handoffs will send when connected.","手机离线。已保存的任务会在联网后发送。")).apply();throw e;}
        }
    }
}
