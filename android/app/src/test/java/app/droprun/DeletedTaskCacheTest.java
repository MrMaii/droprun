package app.droprun;

import android.content.SharedPreferences;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DeletedTaskCacheTest {
    @Test public void confirmedDeletionEvictsEveryCachedPageWithoutLosingItsCursorOrFollowups()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();
        JSONObject deleted=new JSONObject().put("id","deleted-parent").put("project_id","studio").put("report","Private deleted report");
        JSONObject child=new JSONObject().put("id","kept-followup").put("root_task_id","deleted-parent").put("parent_task_id","deleted-parent").put("report","Retained followup report");
        JSONObject similar=new JSONObject().put("id","deleted-parent-other").put("report","Other retained report");
        JSONArray page=new JSONArray();for(int n=0;n<52;n++)page.put(new JSONObject().put("id","older-"+n));page.put(deleted).put(child).put(similar);
        prefs.edit().putString("tasks",new JSONObject().put("tasks",new JSONArray().put(deleted).put(child)).put("taskIds",new JSONArray().put("deleted-parent").put("kept-followup")).put("stats",new JSONObject().put("total",999)).put("syncCursor",1234).toString())
            .putString("history:studio",new JSONObject().put("tasks",page).put("nextCursor","retain-earlier-cursor").put("hasMore",true).toString())
            .putString("history:other",new JSONObject().put("tasks",new JSONArray().put(similar)).put("nextCursor","other-cursor").toString())
            .putString("task:deleted-parent",deleted.toString()).putString("task:kept-followup",child.toString()).putBoolean("previewRequested:deleted-parent",true).commit();
        String untouched=prefs.getString("history:other","");
        Store.deleteCachedTask(prefs,"deleted-parent",()->{assertTrue(Thread.holdsLock(Store.SYNC_LOCK));assertEquals("requested",prefs.getString("deletion:deleted-parent",""));return null;});
        assertEquals("confirmed",prefs.getString("deletion:deleted-parent",""));
        assertFalse(prefs.contains("task:deleted-parent"));assertFalse(prefs.contains("previewRequested:deleted-parent"));assertEquals(child.toString(),prefs.getString("task:kept-followup",""));
        JSONObject latest=new JSONObject(prefs.getString("tasks",""));assertEquals(1,latest.getJSONArray("tasks").length());assertEquals(child.toString(),latest.getJSONArray("tasks").getJSONObject(0).toString());assertEquals("kept-followup",latest.getJSONArray("taskIds").getString(0));assertEquals(1,latest.getJSONArray("taskIds").length());assertEquals(1234,latest.getLong("syncCursor"));assertEquals(999,latest.getJSONObject("stats").getInt("total"));
        JSONObject history=new JSONObject(prefs.getString("history:studio",""));assertEquals(54,history.getJSONArray("tasks").length());assertEquals("retain-earlier-cursor",history.getString("nextCursor"));assertTrue(history.getBoolean("hasMore"));assertEquals(child.toString(),history.getJSONArray("tasks").getJSONObject(52).toString());assertEquals(similar.toString(),history.getJSONArray("tasks").getJSONObject(53).toString());assertEquals(untouched,prefs.getString("history:other",""));
    }
    @Test public void deletionInvalidatesPreReceiptPagesIncludingAnUnknownProjectInOnlyThisInstance()throws Exception{
        MemoryPreferences first=new MemoryPreferences(),second=new MemoryPreferences();
        for(MemoryPreferences prefs:new MemoryPreferences[]{first,second})prefs.edit().putString("historyRequest:uncached-project","in-flight-first-page").putString("historyRequest:loaded-project","in-flight-older-page").putString("credential","private-instance-token").putString("activity","last-synced-server-totals").commit();
        Map<String,?> otherInstance=second.getAll();
        Store.evictDeletedTask(first,"a-task-not-yet-cached");
        assertFalse(first.contains("historyRequest:uncached-project"));assertFalse(first.contains("historyRequest:loaded-project"));
        // loadHistory's publication guard now rejects both responses created before the DELETE receipt.
        assertNotEquals("in-flight-first-page",first.getString("historyRequest:uncached-project",""));assertNotEquals("in-flight-older-page",first.getString("historyRequest:loaded-project",""));
        assertEquals("private-instance-token",first.getString("credential",""));assertEquals("last-synced-server-totals",first.getString("activity",""));assertEquals(otherInstance,second.getAll());
    }
    @Test public void deletionIsIdempotentAndKeepsUnrelatedOrUnreadableCaches()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();
        prefs.edit().putString("tasks","{\"tasks\":[{\"id\":\"keep\"}],\"taskIds\":[\"keep\"]}").putString("history:broken","not-json").putInt("history:wrong-type",3).putString("unrelated","deleted-id is merely text here").commit();
        Map<String,?> before=prefs.getAll();Store.evictDeletedTask(prefs,"deleted-id");Map<String,?> once=prefs.getAll();Store.evictDeletedTask(prefs,"deleted-id");assertEquals(once,prefs.getAll());for(String key:before.keySet())assertEquals(before.get(key),prefs.getAll().get(key));
    }
    @Test public void unknownReceiptKeepsCachedReportButBlocksTheLeftoverOutboxAndSameUuid()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences(),otherInstance=new MemoryPreferences();String id="deletion-local-uuid",child="retained-followup-uuid";
        String report=new JSONObject().put("id",id).put("report","Last synced report").toString();prefs.edit().putString("task:"+id,report).commit();
        java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("droprun-deletion-outbox-");
        try{
            java.nio.file.Files.write(dir.resolve(id+".json"),new JSONObject().put("id",id).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));java.nio.file.Files.write(dir.resolve(child+".json"),new JSONObject().put("id",child).put("rootTaskId",id).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertThrows(java.io.IOException.class,()->Store.deleteCachedTask(prefs,id,()->{throw new java.io.IOException("Synthetic lost DELETE receipt");}));
            assertEquals("requested",prefs.getString("deletion:"+id,""));assertEquals(report,prefs.getString("task:"+id,""));assertTrue(Store.deletionRequested(prefs,id));assertThrows(java.io.IOException.class,()->Store.requireUndeletedTask(prefs,id));Store.requireUndeletedTask(prefs,child);Store.requireUndeletedTask(otherInstance,id);
            JSONArray sendable=Store.pending(dir.toFile(),prefs,false);assertEquals(1,sendable.length());assertEquals(child,sendable.getJSONObject(0).getString("id"));assertEquals(2,Store.pending(dir.toFile(),prefs,true).length());assertEquals(2,Store.pending(dir.toFile(),otherInstance,false).length());
            Store.deleteCachedTask(prefs,id,()->null);assertFalse(prefs.contains("task:"+id));assertEquals("confirmed",prefs.getString("deletion:"+id,""));assertThrows(java.io.IOException.class,()->Store.requireUndeletedTask(prefs,id));assertEquals(1,Store.pending(dir.toFile(),prefs,false).length());assertTrue(java.nio.file.Files.exists(dir.resolve(id+".json")));
        }finally{java.nio.file.Files.deleteIfExists(dir.resolve(id+".json"));java.nio.file.Files.deleteIfExists(dir.resolve(child+".json"));java.nio.file.Files.delete(dir);}
    }
    @Test public void persistenceFailureBeforeRequestCannotSendTheDelete()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();prefs.failCommit=1;java.util.concurrent.atomic.AtomicInteger sent=new java.util.concurrent.atomic.AtomicInteger();
        java.io.IOException error=assertThrows(java.io.IOException.class,()->Store.deleteCachedTask(prefs,"id",()->{sent.incrementAndGet();return null;}));assertEquals(0,sent.get());assertTrue(error.getMessage().contains("Nothing was sent"));assertFalse(prefs.persisted.containsKey("deletion:id"));
    }
    @Test public void postReceiptPersistenceFailureKeepsADurableReplayBlock()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();prefs.edit().putString("task:id","{\"id\":\"id\",\"report\":\"Cached report\"}").commit();prefs.failCommit=3;java.util.concurrent.atomic.AtomicInteger sent=new java.util.concurrent.atomic.AtomicInteger();
        java.io.IOException error=assertThrows(java.io.IOException.class,()->Store.deleteCachedTask(prefs,"id",()->{sent.incrementAndGet();return null;}));assertEquals(1,sent.get());assertTrue(error.getMessage().contains("Relay deleted"));assertEquals("requested",prefs.persisted.get("deletion:id"));
        MemoryPreferences restarted=new MemoryPreferences();restarted.values.putAll(prefs.persisted);assertTrue(Store.deletionRequested(restarted,"id"));assertThrows(java.io.IOException.class,()->Store.requireUndeletedTask(restarted,"id"));
    }
    @Test public void revocationClearsPrivateCacheButRetainsReplayBlocksForTheSameInstance()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();prefs.edit().putString("deletion:requested-id","requested").putString("deletion:confirmed-id","confirmed").putString("credential","private-token").putString("task:confirmed-id","private-report").putString("history:studio","private-history").putString("projects","private-projects").commit();
        Store.clearRevokedCache(prefs);assertEquals(2,prefs.getAll().size());assertEquals("requested",prefs.getString("deletion:requested-id",""));assertEquals("confirmed",prefs.getString("deletion:confirmed-id",""));assertEquals(prefs.getAll(),prefs.persisted);assertThrows(java.io.IOException.class,()->Store.requireUndeletedTask(prefs,"confirmed-id"));
    }
    @Test public void onlyAnExplicitDelete404OffersLocalCleanup()throws Exception{
        for(int status:new int[]{404,401,403,503}){
            MemoryPreferences prefs=new MemoryPreferences();prefs.edit().putString("task:id","cached-report").putBoolean("deleteUnavailable:id",true).commit();Store.HttpFailure response=new Store.HttpFailure(status,"Synthetic HTTP "+status);
            assertSame(response,assertThrows(Store.HttpFailure.class,()->Store.deleteCachedTask(prefs,"id",()->{assertFalse(prefs.getBoolean("deleteUnavailable:id",false));throw response;})));
            assertEquals("Synthetic HTTP "+status,response.getMessage());assertTrue(response instanceof java.io.IOException);assertEquals("requested",prefs.getString("deletion:id",""));assertEquals("cached-report",prefs.getString("task:id",""));assertEquals(status==404,Store.canClearUnavailableTask(prefs,"id"));
            if(status!=404)assertThrows(java.io.IOException.class,()->Store.clearUnavailableTask(prefs,"id"));
        }
        MemoryPreferences prefs=new MemoryPreferences();prefs.edit().putBoolean("deleteUnavailable:id",true).commit();assertThrows(java.io.IOException.class,()->Store.deleteCachedTask(prefs,"id",()->{throw new java.io.IOException("Synthetic network failure");}));assertFalse(Store.canClearUnavailableTask(prefs,"id"));
    }
    @Test public void explicitLocalCleanupHasALocalOnlyMarkerAndSendsNoAdditionalDelete()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();JSONObject row=new JSONObject().put("id","id").put("report","cached-report");prefs.edit().putString("task:id",row.toString()).putString("tasks",new JSONObject().put("tasks",new JSONArray().put(row)).toString()).putString("history:studio",new JSONObject().put("tasks",new JSONArray().put(row)).put("nextCursor","keep-cursor").toString()).commit();java.util.concurrent.atomic.AtomicInteger deletes=new java.util.concurrent.atomic.AtomicInteger();
        assertThrows(Store.HttpFailure.class,()->Store.deleteCachedTask(prefs,"id",()->{deletes.incrementAndGet();throw new Store.HttpFailure(404,"Synthetic inaccessible task");}));assertEquals(row.toString(),prefs.getString("task:id",""));assertEquals("requested",prefs.persisted.get("deletion:id"));
        Store.clearUnavailableTask(prefs,"id");assertEquals(1,deletes.get());assertEquals("local-only",prefs.getString("deletion:id",""));assertFalse(Store.canClearUnavailableTask(prefs,"id"));assertFalse(prefs.contains("task:id"));assertEquals(0,new JSONObject(prefs.getString("tasks","")).getJSONArray("tasks").length());JSONObject history=new JSONObject(prefs.getString("history:studio",""));assertEquals(0,history.getJSONArray("tasks").length());assertEquals("keep-cursor",history.getString("nextCursor"));assertThrows(java.io.IOException.class,()->Store.requireUndeletedTask(prefs,"id"));
    }
    @Test public void confirmedDeleteClearsAnyOldLocalCleanupOffer()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();prefs.edit().putString("deletion:id","requested").putBoolean("deleteUnavailable:id",true).commit();Store.deleteCachedTask(prefs,"id",()->null);assertEquals("confirmed",prefs.getString("deletion:id",""));assertFalse(Store.canClearUnavailableTask(prefs,"id"));assertFalse(prefs.contains("deleteUnavailable:id"));
    }
    @Test public void latestOnlyReportSurvivesA404AndSubsequentListRefreshUntilLocalConfirmation()throws Exception{
        MemoryPreferences prefs=new MemoryPreferences();JSONObject row=new JSONObject().put("id","id").put("report","Last visible report");prefs.edit().putString("tasks",new JSONObject().put("tasks",new JSONArray().put(row)).toString()).commit();assertFalse(prefs.contains("task:id"));
        assertThrows(Store.HttpFailure.class,()->Store.deleteCachedTask(prefs,"id",()->{throw new Store.HttpFailure(404,"Synthetic inaccessible task");}));prefs.edit().putString("tasks","{\"tasks\":[]}").commit();assertEquals(row.toString(),prefs.getString("task:id",""));assertTrue(Store.canClearUnavailableTask(prefs,"id"));assertEquals("requested",prefs.getString("deletion:id",""));Store.clearUnavailableTask(prefs,"id");assertFalse(prefs.contains("task:id"));assertEquals("local-only",prefs.getString("deletion:id",""));
    }
    /** SharedPreferences boundary only: no Activity, device, token vault or network is created. */
    static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>(),persisted=new HashMap<>();int commits,failCommit=-1;
        public Map<String,?> getAll(){return new HashMap<>(values);}
        public String getString(String key,String fallback){Object value=values.get(key);return value==null?fallback:(String)value;}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String key,Set<String> fallback){Object value=values.get(key);return value==null?fallback:new HashSet<>((Set<String>)value);}
        public int getInt(String key,int fallback){Object value=values.get(key);return value==null?fallback:(Integer)value;}
        public long getLong(String key,long fallback){Object value=values.get(key);return value==null?fallback:(Long)value;}
        public float getFloat(String key,float fallback){Object value=values.get(key);return value==null?fallback:(Float)value;}
        public boolean getBoolean(String key,boolean fallback){Object value=values.get(key);return value==null?fallback:(Boolean)value;}
        public boolean contains(String key){return values.containsKey(key);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener){}
        public Editor edit(){return new Editor(){
            final Map<String,Object> changed=new HashMap<>();final Set<String> removed=new HashSet<>();boolean clear;
            Editor put(String key,Object value){removed.remove(key);changed.put(key,value);return this;}
            public Editor putString(String key,String value){return put(key,value);}
            public Editor putStringSet(String key,Set<String> value){return put(key,new HashSet<>(value));}
            public Editor putInt(String key,int value){return put(key,value);}
            public Editor putLong(String key,long value){return put(key,value);}
            public Editor putFloat(String key,float value){return put(key,value);}
            public Editor putBoolean(String key,boolean value){return put(key,value);}
            public Editor remove(String key){changed.remove(key);removed.add(key);return this;}
            public Editor clear(){clear=true;return this;}
            public boolean commit(){if(clear)values.clear();for(String key:removed)values.remove(key);values.putAll(changed);if(++commits==failCommit)return false;persisted.clear();persisted.putAll(values);return true;}
            public void apply(){commit();}
        };}
    }
}
