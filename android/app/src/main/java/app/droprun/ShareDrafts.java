package app.droprun;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Only import journals in the current instance (or before first pairing) are recoverable. */
final class ShareDrafts {
    static final class Entry {
        final String id,scope;final Store store;final File directory;
        Entry(Store store,File directory){this.store=store;this.directory=directory;id=directory.getName();scope=store.scope;}
        JSONObject read()throws Exception{ShareImport item=ShareImport.live(id);if(item!=null){synchronized(item){JSONObject saved=readFile();if(item.editorSnapshot!=null)saved.put("editor",new JSONObject(item.editorSnapshot.toString()));return saved;}}return readFile();}
        private JSONObject readFile()throws Exception{return new JSONObject(new String(new AtomicFile(new File(directory,"import.json")).readFully(),StandardCharsets.UTF_8));}
        boolean open(){ShareImport item=ShareImport.live(id);return item!=null&&item.hasOwner();}
    }
    static Store scoped(Store current,String scope)throws IOException{
        if(scope.equals(current.scope))return current;
        if(!ShareImport.UNPAIRED.equals(scope))throw ShareImport.changed();
        Store before=new Store(current.context);before.select("","");return before;
    }
    static boolean uuid(String id){try{return id!=null&&UUID.fromString(id).toString().equals(id);}catch(Exception e){return false;}}
    static List<Entry> list(Store current){
        List<Entry> entries=new ArrayList<>();List<Store> stores=new ArrayList<>();stores.add(current);
        if(!ShareImport.UNPAIRED.equals(current.scope))try{stores.add(scoped(current,ShareImport.UNPAIRED));}catch(IOException ignored){}
        for(Store store:stores){File[] dirs=store.attachments().listFiles();if(dirs==null)continue;for(File dir:dirs){
            String id=dir.getName();if(!uuid(id)||!dir.isDirectory()||(!new File(dir,"import.json").isFile()&&!new File(dir,"import.json.bak").isFile()))continue;
            if(Store.deletionRequested(store.prefs,id)||new File(store.outbox(),id+".json").exists()||store.task(id)!=null)continue;
            try{if(!dir.getCanonicalFile().getParentFile().equals(store.attachments().getCanonicalFile()))continue;}catch(IOException e){continue;}
            entries.add(new Entry(store,dir));
        }}
        entries.sort((a,b)->Long.compare(b.directory.lastModified(),a.directory.lastModified()));return entries;
    }
    static Entry find(Store current,String scope,String id)throws IOException{
        if(!current.scope.equals(new Store(current.context).scope))throw ShareImport.changed();
        for(Entry entry:list(current))if(entry.scope.equals(scope)&&entry.id.equals(id))return entry;
        throw ShareImport.invalid();
    }
    static Intent payload(JSONObject record)throws Exception{
        JSONArray sources=record.getJSONArray("sources");ArrayList<Uri> uris=new ArrayList<>();for(int n=0;n<sources.length();n++)uris.add(Uri.parse(sources.getString(n)));
        Intent intent=new Intent(uris.size()>1?Intent.ACTION_SEND_MULTIPLE:Intent.ACTION_SEND).setType(record.getString("mime"));
        if(uris.size()>1)intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);else if(uris.size()==1)intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));
        String text=record.getString("text");if(record.optBoolean("hasText",!"null".equals(text)))intent.putExtra(Intent.EXTRA_TEXT,text);
        ShareImport.sources(intent);return intent;
    }
    static Bundle state(Entry entry,JSONObject record){
        Bundle state=new Bundle();state.putString("importId",entry.id);state.putString("importScope",entry.scope);
        JSONObject editor=record.optJSONObject("editor");if(editor!=null){for(String key:new String[]{"selected","draft","model","effort","query"})state.putString(key,editor.optString(key,""));state.putInt("step",Math.min(1,editor.optInt("step",0)));state.putBoolean("showAll",editor.optBoolean("showAll"));}
        return state;
    }
    static void discard(Store current,Entry entry)throws Exception{
        Entry checked=find(current,entry.scope,entry.id);ShareImport item=ShareImport.live(entry.id);
        if(item!=null){if(item.hasOwner())throw new IOException(L.t("Close this share's editor before discarding it here.","请先关闭这份分享的编辑页面，再在此放弃。"));item.cancel();return;}
        File[] files=checked.directory.listFiles();if(files==null)throw ShareImport.invalid();
        for(File file:files)if(!file.isFile()||!file.getName().matches("[0-7]|import\\.json(?:\\.bak|\\.new)?")||!file.getCanonicalFile().getParentFile().equals(checked.directory.getCanonicalFile()))throw ShareImport.invalid();
        for(File file:files)if(file.getName().matches("[0-7]")&&!file.delete())throw new IOException(L.t("Could not remove a saved copy. Try again.","未能移除一份保存副本，请重试。"));
        new AtomicFile(new File(checked.directory,"import.json")).delete();checked.directory.delete();
        if(new File(checked.directory,"import.json").exists())throw new IOException(L.t("Could not discard this draft. Try again.","未能放弃这份草稿，请重试。"));
    }
}
