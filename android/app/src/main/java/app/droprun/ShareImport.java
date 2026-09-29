package app.droprun;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Owns one share's private copies until an outbox entry takes ownership. */
final class ShareImport implements Runnable {
    static final String UNPAIRED=Store.scope("","");
    final String id;
    final JSONArray sources;
    final String mime;
    Store store;
    File directory;
    AtomicFile journal;
    JSONObject record;
    volatile boolean finished,cancelled,transferred;
    volatile Exception error;
    volatile Runnable observer;
    private boolean retryRequested;
    int failureIndex=-1;
    private InputStream input;

    ShareImport(Store current,Intent intent,String savedId,String savedScope)throws Exception{
        sources=sources(intent);mime=intent.getType()==null?"application/octet-stream":intent.getType();
        store=current;id=savedId==null?UUID.randomUUID().toString():savedId;
        if(!id.matches("[0-9a-f-]{36}")||!UUID.fromString(id).toString().equals(id))throw invalid();
        if(savedId!=null&&!current.scope.equals(savedScope)&&!UNPAIRED.equals(savedScope))throw changed();
        directory=new File(current.attachments(),id);
        // A pairing confirmed while this Activity was stopped may have already moved the draft.
        if(savedId!=null&&UNPAIRED.equals(savedScope)&&!current.scope.equals(savedScope)&&!directory.exists()){
            File previous=new File(current.context.getFilesDir(),"instances/"+UNPAIRED+"/attachments/"+id);
            if(!previous.renameTo(directory))throw invalid();
        }
        journal=new AtomicFile(new File(directory,"import.json"));
        if(savedId==null){
            if(!directory.mkdir())throw new IOException(L.t("Could not save this share on your phone. Check free space and try again.","无法在手机保存这次分享，请检查剩余空间后重试。"));
            record=new JSONObject().put("sources",sources).put("mime",mime).put("text",String.valueOf(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))).put("files",new JSONArray());
            try{write();}catch(Exception e){directory.delete();throw e;}
        }else{
            // Covers process death between atomic outbox persistence and journal removal.
            JSONArray pending=current.pending();
            for(int n=0;n<pending.length();n++)if(id.equals(pending.getJSONObject(n).optString("id"))){transferred=true;finished=true;journal.delete();return;}
            try{record=new JSONObject(new String(journal.readFully(),StandardCharsets.UTF_8));}catch(IOException|JSONException e){throw invalid();}
            if(!sources.toString().equals(record.getJSONArray("sources").toString())||!mime.equals(record.getString("mime"))||!String.valueOf(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)).equals(record.getString("text")))throw invalid();
            if(record.getJSONArray("files").length()>sources.length())throw invalid();
        }
    }
    static JSONArray sources(Intent intent)throws IOException{
        ArrayList<Uri> uris=new ArrayList<>();
        if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())){ArrayList<Uri> list=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(list!=null)uris.addAll(list);}
        else{Uri uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);if(uri!=null)uris.add(uri);}
        if(uris.isEmpty()&&intent.getClipData()!=null)for(int n=0;n<intent.getClipData().getItemCount();n++){Uri uri=intent.getClipData().getItemAt(n).getUri();if(uri!=null&&"content".equals(uri.getScheme()))uris.add(uri);}
        if(uris.size()>8)throw new IOException(L.t("Share up to 8 attachments at a time.","一次最多 8 个附件"));
        JSONArray result=new JSONArray();
        for(Uri uri:uris){if(uri==null||!"content".equals(uri.getScheme()))throw new IOException(L.t("Send files using the system share menu.","请通过系统分享发送文件。"));result.put(uri.toString());}
        return result;
    }
    static IOException invalid(){return new IOException(L.t("This saved share could not be verified. Please share the material again.","无法核验这次保存的分享，请重新分享材料。"));}
    static IOException changed(){return new IOException(L.t("The connection changed. Share again to choose the intended computer.","连接已改变，请重新分享并选择目标电脑。"));}
    static final class ReceiveFailure extends IOException {
        final String kind;
        ReceiveFailure(String kind){super(switch(kind){
            case "source"->L.t("The source is no longer available. If retry cannot reopen it, return to the source app and share again.","来源已不可用。如果重试仍无法读取，请返回来源 App 重新分享。");
            case "integrity"->L.t("A saved copy could not be verified. Retry to copy that file again.","一份已保存副本未通过校验，请重试复制该文件。");
            case "size"->L.t("A file exceeds the 50 MB limit. Share a smaller version from the source app.","一份文件超过 50 MB，请从来源 App 分享较小版本。");
            case "storage"->L.t("Recovery could not be saved. Free up space on your phone, then retry.","无法保存恢复记录。请释放手机空间后重试。");
            default->L.t("Copying was interrupted. Check free space and try again.","复制中断了，请检查剩余空间后重试。");
        });this.kind=kind;}
    }
    void write()throws IOException{
        FileOutputStream output=null;
        try{output=journal.startWrite();output.write(record.toString().getBytes(StandardCharsets.UTF_8));journal.finishWrite(output);}
        catch(IOException e){if(output!=null)journal.failWrite(output);throw e;}
    }
    File file(int index){return new File(directory,Integer.toString(index));}
    static String hex(byte[] bytes){StringBuilder value=new StringBuilder();for(byte b:bytes)value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();}
    static String digest(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}
        return hex(digest.digest());
    }
    void checkCancelled()throws InterruptedIOException{if(cancelled)throw new InterruptedIOException("Share closed.");}
    synchronized boolean retry()throws IOException{
        if(!finished||error==null||cancelled||transferred)return false;
        if(!store.scope.equals(new Store(store.context).scope))throw changed();
        retryRequested=true;finished=false;error=null;return true;
    }
    int completeCount(){int count=0;JSONArray files=record.optJSONArray("files");for(int n=0;files!=null&&n<files.length();n++){JSONObject item=files.optJSONObject(n);if(item!=null&&item.optBoolean("complete"))count++;}return count;}
    @Override public void run(){
        try{
            if(retryRequested){retryRequested=false;record.remove("failure");write();}
            JSONArray files=record.getJSONArray("files");
            // Validate every retained receipt before describing any copy as ready, even on failure restoration.
            int damaged=-1;
            for(int index=0;index<files.length();index++){
                checkCancelled();JSONObject saved=files.optJSONObject(index);if(saved==null||!saved.optBoolean("complete"))continue;
                File target=file(index);boolean verified=false;
                try{verified=saved.opt("name") instanceof String&&saved.opt("mime") instanceof String&&target.isFile()&&target.length()==saved.getLong("size")&&digest(target).equals(saved.getString("sha256"));}catch(Exception unreadable){}
                if(!verified){saved.put("complete",false);target.delete();if(damaged<0)damaged=index;}
            }
            if(damaged>=0){failureIndex=damaged;throw new ReceiveFailure("integrity");}
            JSONObject previousFailure=record.optJSONObject("failure");
            if(previousFailure!=null){failureIndex=previousFailure.optInt("index",-1);throw new ReceiveFailure(previousFailure.optString("kind","copy"));}
            for(int index=0;index<sources.length();index++){
                checkCancelled();File target=file(index);JSONObject saved=files.optJSONObject(index);
                if(saved!=null&&saved.optBoolean("complete"))continue;
                failureIndex=index;
                Uri uri=Uri.parse(sources.getString(index));String type=store.context.getContentResolver().getType(uri);if(type==null)type=mime;
                String name="shared-file";
                try(Cursor cursor=store.context.getContentResolver().query(uri,null,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int column=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(column>=0&&cursor.getString(column)!=null)name=cursor.getString(column);}}
                files.put(index,new JSONObject().put("name",name).put("mime",type).put("complete",false));write();
                MessageDigest hash=MessageDigest.getInstance("SHA-256");long size=0;
                // The name is derived from the journal ID and index before any bytes are written.
                // After a killed process, truncating this same owned file removes the partial copy.
                try(InputStream source=store.context.getContentResolver().openInputStream(uri);FileOutputStream output=new FileOutputStream(target)){
                    if(source==null)throw new IOException(L.t("The source could not be opened. Share it again.","无法打开来源，请重新分享。"));
                    synchronized(this){input=source;checkCancelled();}
                    byte[] buffer=new byte[65536];int count;
                    while((count=source.read(buffer))!=-1){checkCancelled();size+=count;if(size>50L*1024*1024)throw new ReceiveFailure("size");output.write(buffer,0,count);hash.update(buffer,0,count);}
                    checkCancelled();output.getFD().sync();
                }finally{synchronized(this){input=null;}}
                files.put(index,new JSONObject().put("name",name).put("mime",type).put("size",size).put("sha256",hex(hash.digest())).put("complete",true));write();
            }
            failureIndex=-1;
        }catch(Exception e){
            if(cancelled){error=e;discard();}
            else{
                ReceiveFailure failure=e instanceof ReceiveFailure?(ReceiveFailure)e:new ReceiveFailure(e instanceof FileNotFoundException||e instanceof SecurityException?"source":"copy");error=failure;
                JSONArray files=record.optJSONArray("files");
                for(int index=0;index<sources.length();index++){JSONObject saved=files==null?null:files.optJSONObject(index);if(saved==null||!saved.optBoolean("complete"))file(index).delete();}
                try{record.put("failure",new JSONObject().put("kind",failure.kind).put("index",failureIndex));write();}
                catch(Exception unavailable){error=new ReceiveFailure("storage");}
            }
        }
        finally{
            finished=true;if(cancelled)discard();
            new Handler(Looper.getMainLooper()).post(()->{Runnable callback=observer;if(callback!=null)callback.run();});
        }
    }
    JSONArray attachments()throws JSONException{
        JSONArray result=new JSONArray(),files=record.getJSONArray("files");
        for(int index=0;index<files.length();index++){JSONObject saved=files.optJSONObject(index);if(saved!=null&&saved.optBoolean("complete"))result.put(new JSONObject().put("path",file(index).getAbsolutePath()).put("name",saved.getString("name")).put("mime",saved.getString("mime")));}
        return result;
    }
    /** Only a previously unpaired share may follow an explicitly confirmed first pairing. */
    void bind(Store current)throws IOException{
        if(store.scope.equals(current.scope))return;
        if(!UNPAIRED.equals(store.scope)||!finished||error!=null||cancelled||transferred)throw changed();
        File next=new File(current.attachments(),id);if(!directory.renameTo(next))throw invalid();
        directory=next;journal=new AtomicFile(new File(directory,"import.json"));store=current;
    }
    void save(JSONObject task)throws Exception{
        if(!finished||error!=null||cancelled||transferred)throw invalid();
        if(!store.scope.equals(new Store(store.context).scope))throw changed();
        task.put("id",id).put("localFiles",attachments());store.save(task);
        transferred=true;journal.delete();
        if(sources.length()==0)directory.delete();
    }
    void cancel(){
        cancelled=true;
        synchronized(this){if(input!=null)try{input.close();}catch(IOException ignored){}}
        if(finished)discard();
    }
    void discard(){
        if(transferred)return;
        // Never enumerate/delete unrelated attachments, including legacy flat files.
        for(int index=0;index<sources.length();index++)file(index).delete();
        journal.delete();directory.delete();
    }
}
