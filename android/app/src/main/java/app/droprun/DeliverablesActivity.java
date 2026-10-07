package app.droprun;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.*;
import org.json.*;

/** Native preview of a task's private deliverables: external HTML, scripts and patches are never executed. */
public class DeliverablesActivity extends StyledActivity {
    final ExecutorService io=Executors.newSingleThreadExecutor();
    Store store;String taskId;LinearLayout page;File currentFile;JSONObject currentItem;
    Button saveButton,fileBack;TextView saveNotice,saveDetail;boolean picking,verificationExpanded;
    String saveMessage="",saveError="";SaveOperation saving;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=createStore();taskId=getIntent().getStringExtra("taskId");Ui.configureWindow(this);
        if(taskId==null||!taskId.matches("[a-zA-Z0-9-]{20,64}")){finish();return;}
        saving=(SaveOperation)getLastNonConfigurationInstance();
        if(saving!=null&&!saving.store.scope.equals(store.scope)){saving.cleanup=true;if(saving.done)saving.file.delete();saving=null;}
        if(state!=null){verificationExpanded=state.getBoolean("verificationExpanded");picking=state.getBoolean("picking");saveMessage=state.getString("saveMessage","");saveError=state.getString("saveError","");
            if(saving==null&&state.getBoolean("savePending")){saveMessage=L.t("Save status is unknown.","保存状态尚未确认。");saveError=L.t("The app restarted during saving. Check your chosen location before saving again; a partial file may remain.","App 在保存期间重新启动。再次保存前，请检查所选位置；那里可能留有不完整文件。");}}
        if(saving!=null)saving.observer=this::updateSaveUi;
        if(state!=null)try{
            File restored=new File(state.getString("file",""));
            if(restored.isFile()&&restored.getCanonicalFile().getParentFile().equals(store.cacheDir().getCanonicalFile())&&restored.getName().startsWith("droprun-delivery-")){currentFile=restored;currentItem=new JSONObject(state.getString("item"));restorePreview();return;}
        }catch(Exception ignored){}
        picking=false;if(saving!=null){saving.observer=null;saving.cleanup=true;if(saving.done)saving.file.delete();saving=null;}
        loadList();
    }
    Store createStore(){return new Store(this);}
    void restorePreview(){
        base(L.t("File preview","文件预览"));information(L.t("Checking saved preview","正在核对已保存的预览"),L.t("The cached file is checked again before its contents or Save action appear.","重新核对缓存文件后，才会显示内容和保存操作。"));secondary(L.t("Back to report","返回报告"),this::finish);
        File file=currentFile;JSONObject item=currentItem;
        io.execute(()->{
            try{Store.verifyDeliverable(file,item);runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())showFile();});}
            catch(Exception e){runOnUiThread(()->{if(isDestroyed()||isFinishing())return;loadList();error(e);});}
        });
    }
    int dp(int value){return Ui.dp(this,value);}
    /** Fresh page for each state (list, download, preview): black ground, back chevron, screen title. */
    void base(String title){
        saveButton=null;fileBack=null;saveNotice=null;saveDetail=null;
        page=Ui.page(this);
        ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back","返回"));back.setOnClickListener(v->onBackPressed());
        Ui.topBar(this,page,back,title,false,null);Ui.enter(page);
    }
    void primary(String title,Runnable action){add(Ui.button(this,title,true),action);}
    void secondary(String title,Runnable action){add(Ui.button(this,title,false),action);}
    void add(Button view,Runnable action){view.setOnClickListener(v->action.run());page.addView(view,Ui.margins(this,8,0));}
    LinearLayout information(String title,String value){
        LinearLayout card=Ui.card(this);
        TextView heading=Ui.text(this,title,14,Ui.TEXT);heading.setTypeface(Ui.medium());card.addView(heading);
        TextView detail=Ui.text(this,value,13,Ui.MUTED);detail.setTextIsSelectable(true);card.addView(detail);
        page.addView(card,Ui.cardParams(this));return card;
    }
    static String size(long bytes){
        if(bytes<1024)return bytes+L.t(" bytes"," 字节");
        if(bytes<1024*1024)return String.format(Locale.ROOT,"%.1f KB",bytes/1024.0);
        return String.format(Locale.ROOT,"%.1f MB",bytes/1048576.0);
    }
    void error(Exception error){if(!isDestroyed())new AlertDialog.Builder(this).setTitle(L.t("File action failed","文件操作失败")).setMessage(error.getMessage()).setPositiveButton(L.t("Got it","知道了"),null).show();}
    void clearFile(){if(currentFile!=null)currentFile.delete();currentFile=null;currentItem=null;}

    // ---- list -----------------------------------------------------------------------------------
    void loadList(){
        if(saveBusy()||picking)return;
        saving=null;saveMessage="";saveError="";
        clearFile();base(L.t("Delivery files","交付文件"));information(L.t("Loading delivery files","正在读取清单"),L.t("Checking the private delivery files available to this phone…","正在确认这台手机可下载的私有交付文件…"));secondary(L.t("Back to report","返回报告"),this::finish);
        io.execute(()->{
            try{JSONArray items=store.get("/tasks/"+taskId+"/deliverables").getJSONArray("deliverables");runOnUiThread(()->{if(isDestroyed()||isFinishing())return;renderList(items);});}
            catch(Exception e){runOnUiThread(()->{
                if(isDestroyed()||isFinishing())return;
                base(L.t("Delivery files","交付文件"));information(L.t("Could not load this content","暂时无法读取"),e.getMessage()==null?L.t("The connection could not complete. Try again.","连接尚未完成，请重试。"):e.getMessage());primary(L.t("Try again","重新读取"),this::loadList);secondary(L.t("Back to report","返回报告"),this::finish);
            });}
        });
    }
    void renderList(JSONArray items){
        base(L.t("Delivery files","交付文件"));JSONObject task=store.task(taskId);String mode=task==null?"":task.optString("execution_mode");
        information(L.t("Private delivery · verified downloads","私有交付 · 下载后校验"),L.t("Only this paired phone can download these files. Each download is checked with SHA-256.\n","仅此配对手机可下载，文件下载后经 SHA-256 核对。\n")+("review".equals(mode)||"direct".equals(mode)?L.t("This handoff runs in the original project. Check the report for changes already made.","此任务在原项目执行，请结合报告核对已经发生的改动。"):"legacy-isolated".equals(mode)?L.t("This legacy task ran in an isolated copy. It was not automatically merged into the original project.","此历史任务在隔离副本执行，未自动合并原项目。"):L.t("Check the task report for where work was performed.","请查看任务报告，确认实际执行位置。")));
        if(items.length()==0)information(L.t("No delivery files yet","暂无交付文件"),L.t("No files were uploaded for this handoff. Older files may remain on your computer; an analysis-only task may have no files.","此任务没有上传交付文件。旧任务的产物仍保存在电脑；仅分析任务可能没有文件。"));
        else{
            page.addView(Ui.label(this,items.length()+L.t(" files"," 个文件")));
            LinearLayout list=Ui.card(this);list.setPadding(dp(12),dp(4),dp(12),dp(4));
            for(int i=0;i<items.length();i++){
                JSONObject item=items.optJSONObject(i);if(item==null)continue;
                if(list.getChildCount()>0)list.addView(Ui.divider(this));
                list.addView(fileRow(item));
            }
            page.addView(list,Ui.cardParams(this));
        }
        secondary(L.t("Back to report","返回报告"),this::finish);
    }
    LinearLayout fileRow(JSONObject item){
        boolean diff=item.optString("kind").equals("diff");String name=item.optString("name"),size=size(item.optLong("size"));
        LinearLayout row=Ui.listRow(this,name,size,Ui.pill(this,diff?L.t("Diff","差异"):L.t("File","文件"),Ui.MUTED));
        // The row's first text is the file name; ellipsize it in the middle so the extension stays visible.
        ((TextView)((LinearLayout)row.getChildAt(0)).getChildAt(0)).setEllipsize(TextUtils.TruncateAt.MIDDLE);
        row.setContentDescription((diff?L.t("View diff ","查看差异 "):L.t("View file ","查看文件 "))+name+"，"+size);row.setOnClickListener(v->download(item));return row;
    }

    // ---- download + preview ---------------------------------------------------------------------
    void download(JSONObject item){
        base(L.t("Preparing preview","正在准备预览"));information(L.t("Downloading & verifying","正在下载并核对"),item.optString("name")+L.t("\nContent appears only after SHA-256 verification.","\n完成 SHA-256 校验后才会显示文件内容。"));
        io.execute(()->{
            try{File file=store.downloadDeliverable(taskId,item);runOnUiThread(()->{if(isDestroyed()||isFinishing()){file.delete();return;}clearFile();currentFile=file;currentItem=item;showFile();});}
            catch(Exception e){runOnUiThread(()->{if(isDestroyed()||isFinishing())return;loadList();error(e);});}
        });
    }
    void showFile(){
        base(L.t("File preview","文件预览"));
        TextView filename=Ui.title(this,currentItem.optString("name"),18);filename.setTextIsSelectable(true);page.addView(filename);
        saveNotice=Ui.text(this,"",14,Ui.TEXT);saveNotice.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(saveNotice,Ui.margins(this,8,0));
        saveDetail=Ui.text(this,"",13,Ui.MUTED);page.addView(saveDetail,Ui.margins(this,4,0));
        saveButton=Ui.button(this,L.t("Save to phone","保存到手机"),true);add(saveButton,this::saveFile);
        fileBack=Ui.button(this,L.t("Back to delivery files","返回交付列表"),false);add(fileBack,this::loadList);updateSaveUi();
        LinearLayout evidence=Ui.vertical(this);
        evidence.addView(Ui.caption(this,"SHA-256"));
        TextView hash=Ui.text(this,currentItem.optString("sha256"),11,Ui.MUTED);hash.setTypeface(Typeface.MONOSPACE);hash.setTextIsSelectable(true);evidence.addView(hash);
        TextView detail=Ui.text(this,L.t("Preview displays content only. HTML, scripts and patches are not executed.","预览只显示内容，不执行 HTML、脚本或补丁。"),13,Ui.MUTED);detail.setTextIsSelectable(true);evidence.addView(detail,Ui.margins(this,8,8));
        page.addView(Ui.disclosure(this,L.t("Verified file · ","文件已核对 · ")+size(currentItem.optLong("size")),evidence,verificationExpanded,open->verificationExpanded=open),Ui.margins(this,8,0));
        page.addView(Ui.label(this,L.t("File contents","文件内容")));
        boolean previewed=false;
        try{
            String name=currentItem.optString("name").toLowerCase(Locale.ROOT);
            if(name.matches(".*\\.(png|jpg|jpeg|webp)$")){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(currentFile.getPath(),options);options.inSampleSize=1;
                while(options.outWidth/options.inSampleSize>1080||options.outHeight/options.inSampleSize>2048)options.inSampleSize*=2;
                options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeFile(currentFile.getPath(),options);
                if(bitmap!=null){
                    ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setImageBitmap(bitmap);image.setContentDescription(currentItem.optString("name")+L.t(", image preview","，图片预览"));
                    image.setPadding(dp(8),dp(8),dp(8),dp(8));image.setBackground(Ui.surface(this,Ui.SURFACE));image.setClipToOutline(true);page.addView(image,Ui.fill());previewed=true;
                }
            }
            if(!previewed&&currentFile.length()<=1024*1024){
                String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(Files.readAllBytes(currentFile.toPath()))).toString();
                if(value.indexOf('\0')<0){
                    TextView content=Ui.text(this,value.isEmpty()?L.t("(Empty file)","（空文件）"):value,13,Ui.TEXT);content.setTypeface(Typeface.MONOSPACE);content.setTextIsSelectable(true);
                    content.setPadding(dp(14),dp(12),dp(14),dp(12));content.setBackground(Ui.surface(this,Ui.SURFACE));page.addView(content,Ui.fill());previewed=true;
                }
            }
        }catch(Exception ignored){}
        if(!previewed)information(L.t("Save to open","可保存后查看"),L.t("This format has no inline preview, or its text is larger than 1 MiB. Save it and open it in an appropriate app.","此文件暂不支持内嵌预览，或文本超过 1 MiB。可保存后用相应应用打开。"));
    }

    // ---- save through the system file picker ---------------------------------------------------
    boolean saveBusy(){return saving!=null&&!saving.done;}
    void updateSaveUi(){
        if(saveButton==null||isDestroyed()||isFinishing())return;
        if(saving!=null&&saving.done&&saving.invalidCache){String reason=saving.error;loadList();error(new IOException(reason));return;}
        boolean busy=saveBusy();
        if(saving!=null&&saving.done){saveMessage=saving.error.isEmpty()?L.t("Saved to your chosen location.","已保存到你选择的位置。"):L.t("Save failed. Your preview is still available.","保存失败，预览仍然保留。");saveError=saving.error.isEmpty()?"":saving.error+L.t("\nCheck the selected location for a partial file before trying again.","\n重试前，请检查所选位置是否留有不完整文件。");}
        String message=busy?L.t("Checking access and saving to your chosen location.","正在核对权限并保存到所选位置。"):picking?L.t("Choose a location in the system file picker.","请在系统文件选择器中选择保存位置。"):saveMessage;
        saveNotice.setText(message);saveNotice.setVisibility(message.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
        saveDetail.setText(saveError);saveDetail.setVisibility(saveError.isEmpty()||busy||picking?android.view.View.GONE:android.view.View.VISIBLE);
        saveButton.setText(busy?L.t("Saving file…","正在保存…"):picking?L.t("Choosing location…","正在选择位置…"):L.t("Save to phone","保存到手机"));saveButton.setEnabled(!busy&&!picking);fileBack.setEnabled(!busy&&!picking);
    }
    void saveFile(){
        if(picking||saveBusy()||currentFile==null||currentItem==null)return;
        saving=null;saveMessage="";saveError="";picking=true;updateSaveUi();
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/octet-stream");intent.putExtra(Intent.EXTRA_TITLE,currentItem.optString("name").replace('\\','/').replaceAll(".*/",""));
        try{startActivityForResult(intent,20);}catch(RuntimeException e){picking=false;saveMessage=L.t("Could not open save locations.","无法打开保存位置。");saveError=e.getMessage()==null?"":e.getMessage();updateSaveUi();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode!=20||!picking||saveBusy())return;
        picking=false;
        if(resultCode!=RESULT_OK||data==null||data.getData()==null){saveMessage=L.t("Save cancelled. Your preview is still available.","已取消保存，预览仍然保留。");updateSaveUi();return;}
        Uri destination=data.getData();File file=currentFile;JSONObject item=currentItem;
        if(!"content".equals(destination.getScheme())){updateSaveUi();error(new IOException(L.t("Choose a location from the system file picker.","请选择系统文件选择器提供的保存位置")));return;}
        if(file==null||item==null){updateSaveUi();error(new IOException(L.t("The cached download expired. Download it again.","下载缓存已失效，请重新下载")));return;}
        saving=new SaveOperation(store,taskId,file,item,destination);saving.observer=this::updateSaveUi;saveMessage="";saveError="";updateSaveUi();io.execute(saving);
    }
    /** Work uses application context; the UI observer is detached when its Activity is destroyed. */
    static final class SaveOperation implements Runnable {
        final Store store;final String taskId;final File file;final JSONObject item;final Uri destination;
        boolean done,cleanup,invalidCache;String error="";Runnable observer;
        SaveOperation(Store store,String taskId,File file,JSONObject item,Uri destination){this.store=store;this.taskId=taskId;this.file=file;this.item=item;this.destination=destination;}
        @Override public void run(){String failure="";try{
            JSONArray allowed=store.get("/tasks/"+taskId+"/deliverables").getJSONArray("deliverables");boolean found=false;
            for(int i=0;i<allowed.length();i++)if(allowed.getJSONObject(i).optString("id").equals(item.optString("id"))&&allowed.getJSONObject(i).optString("sha256").equals(item.optString("sha256")))found=true;
            if(!found)throw new IOException(L.t("This file was deleted or is no longer available to this phone.","产物已删除或不再允许下载"));
            try{Store.verifyDeliverable(file,item);}catch(Exception changed){invalidCache=true;throw changed;}
            try(InputStream in=new FileInputStream(file);OutputStream out=store.context.getContentResolver().openOutputStream(destination,"w")){if(out==null)throw new IOException(L.t("Could not open the save location.","无法打开保存位置"));byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
        }catch(Exception e){failure=e.getMessage()==null?L.t("Could not complete the save.","未能完成保存。"):e.getMessage();}
            String result=failure;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{error=result;done=true;if(cleanup)file.delete();if(observer!=null)observer.run();});
        }
    }
    @Override public void onBackPressed(){
        if(saveBusy()){new AlertDialog.Builder(this).setTitle(L.t("Save in progress","正在保存文件")).setMessage(L.t("Leaving closes this preview while the save continues. Check the selected location for the result.","离开会关闭预览，保存仍将继续。请到所选位置检查结果。")).setNegativeButton(L.t("Stay here","留在此页"),null).setPositiveButton(L.t("Leave preview","离开预览"),(dialog,which)->finish()).show();}
        else if(fileBack!=null&&!picking)loadList();
        else super.onBackPressed();
    }
    @Override public Object onRetainNonConfigurationInstance(){return saving;}
    @Override protected void onSaveInstanceState(Bundle state){if(currentFile!=null&&currentItem!=null){state.putString("file",currentFile.getPath());state.putString("item",currentItem.toString());}state.putBoolean("verificationExpanded",verificationExpanded);state.putBoolean("picking",picking);state.putBoolean("savePending",saveBusy());state.putString("saveMessage",saveMessage);state.putString("saveError",saveError);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){if(saving!=null)saving.observer=null;if(isFinishing()){if(saveBusy())saving.cleanup=true;else clearFile();}io.shutdown();super.onDestroy();}
}
