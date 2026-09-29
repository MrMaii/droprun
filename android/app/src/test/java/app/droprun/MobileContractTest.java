package app.droprun;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class MobileContractTest {
    private static final String INSTANCE="a02b08d5-850d-4d7c-884a-a14b8559d6fc";
    private static final String CODE="ABCDEF0123456789ABCD";
    private static String link(String origin){return "droprun://pair?relay="+origin+"&instance="+INSTANCE+"&code="+CODE;}
    @Test public void pairingRequiresAnExplicitHttpsInstance(){
        PairingTarget target=PairingTarget.parse(link("https%3A%2F%2Fexample.workers.dev"));assertNotNull(target);assertEquals("https://example.workers.dev",target.relay);assertEquals(INSTANCE,target.instanceId);assertEquals(CODE,target.code);
        assertNull(PairingTarget.parse(CODE));assertNull(PairingTarget.parse("droprun://pair?code="+CODE));assertNull(PairingTarget.parse(link("http://example.com")));
        assertNull(PairingTarget.parse(link("https://user@example.com")));assertNull(PairingTarget.parse(link("https://example.com/path")));assertNull(PairingTarget.parse(link("https://example.com?token=x")));
        assertNull(PairingTarget.parse(link("https://example.com")+"&code="+CODE));assertNull(PairingTarget.parse(link("https://example.com").replace(INSTANCE,"bad-instance")));
    }
    @Test public void cacheScopeBindsBothServerAndInstance(){
        assertEquals(Store.scope("https://one.example",INSTANCE),Store.scope("https://one.example",INSTANCE));
        assertNotEquals(Store.scope("https://one.example",INSTANCE),Store.scope("https://two.example",INSTANCE));
        assertNotEquals(Store.scope("https://one.example",INSTANCE),Store.scope("https://one.example","e15da0e8-c4e6-4f2c-a852-d6982a6b134a"));
    }
    @Test public void projectTotalsComeFromTheServerAndRetriesDoNotCount()throws Exception {
        JSONArray summaries=new JSONArray().put(new JSONObject().put("id","a").put("name","Same name").put("task_count",203).put("dispatch_count",250).put("last_dispatch_at",10)).put(new JSONObject().put("id","unused").put("dispatch_count",0));
        JSONArray pending=new JSONArray().put(new JSONObject().put("id","saved").put("projectId","b").put("projectName","Same name").put("createdAt",20)).put(new JSONObject().put("id","acked").put("projectId","a"));
        List<JSONObject> result=ProjectPresentation.merge(summaries,pending,new JSONArray().put(new JSONObject().put("id","acked")));
        assertEquals(2,result.size());assertEquals("b",result.get(0).getString("id"));assertEquals(1,result.get(0).getInt("pending_count"));assertEquals("a",result.get(1).getString("id"));assertEquals(250,result.get(1).getInt("dispatch_count"));assertEquals(203,result.get(1).getInt("task_count"));assertEquals(0,result.get(1).optInt("pending_count"));assertFalse(summaries.getJSONObject(0).has("pending_count"));
    }
    @Test public void localeChangesProductCopyWithoutChangingData()throws Exception {
        JSONObject summary=new JSONObject().put("task_count",3).put("dispatch_count",5);L.language("en");assertEquals("3 tasks · 5 dispatches",ProjectPresentation.counts(summary));assertEquals("Review your plan",TaskPresentation.status("awaiting_plan_approval"));L.language("zh");assertEquals("3 项任务 · 5 次交办",ProjectPresentation.counts(summary));assertEquals(5,summary.getInt("dispatch_count"));L.language("en");
    }
    @Test public void immutableSnapshotsAreNeverLabeledAsLive(){assertTrue(TaskPresentation.snapshot("snapshot"));assertTrue(TaskPresentation.snapshot("static"));assertFalse(TaskPresentation.snapshot("live"));assertFalse(TaskPresentation.snapshot(""));}
    @Test public void shareOrderingUsesCompleteSummariesAndKeepsCatalogIdentity()throws Exception {
        JSONArray catalog=new JSONArray().put(new JSONObject().put("id","unused")).put(new JSONObject().put("id","old")).put(new JSONObject().put("id","recent"));
        JSONArray summaries=new JSONArray().put(new JSONObject().put("id","old").put("dispatch_count",201).put("last_dispatch_at",1)).put(new JSONObject().put("id","recent").put("dispatch_count",301).put("last_dispatch_at",2)).put(new JSONObject().put("id","removed").put("dispatch_count",1).put("last_dispatch_at",3));
        List<JSONObject> recent=ProjectPresentation.merge(summaries,new JSONArray(),new JSONArray());List<JSONObject> ordered=ProjectPresentation.sharing(catalog,recent,"","old");
        assertEquals(3,ordered.size());assertSame(catalog.getJSONObject(2),ordered.get(0));assertEquals("old",ordered.get(1).getString("id"));assertEquals("unused",ordered.get(2).getString("id"));
        assertEquals("unused",ProjectPresentation.sharing(catalog,recent,"unused","old").get(0).getString("id"));assertEquals("unused",catalog.getJSONObject(0).getString("id"));
    }
    @Test public void duplicateProjectLabelsExtendCollidingIdsAndRetainHistoricalIdentity()throws Exception {
        JSONArray catalog=new JSONArray().put(new JSONObject().put("id","01234567-aaaa").put("name","Studio")).put(new JSONObject().put("id","01234567-bbbb").put("name","Studio"));
        assertEquals("Studio · 01234567-a",ProjectPresentation.label("01234567-aaaa","Studio",catalog,null));assertEquals("Studio · 01234567-b",ProjectPresentation.label("01234567-bbbb","Studio",catalog,null));
        assertEquals("Renamed",ProjectPresentation.label("01234567-aaaa","Renamed",catalog,null));
        JSONArray historical=new JSONArray().put(new JSONObject().put("id","removed-project").put("name","Archive")).put(new JSONObject().put("id","kept-project").put("name","Archive"));
        assertEquals("Archive · removed-",ProjectPresentation.label("removed-project","Archive",new JSONArray(),historical));assertEquals("Archive",ProjectPresentation.label("removed-project","Archive",null,new JSONArray().put(historical.getJSONObject(0))));
    }
    @Test public void existingCacheAndExecutionContractsStillHold()throws Exception {
        L.language("zh");try{TaskCacheTest.main(new String[0]);TaskPresentationTest.main(new String[0]);TaskWatchPolicyTest.main(new String[0]);}finally{L.language("en");}
    }
}
