package app.droprun;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Unsent material is separate from project history and never auto-submitted. */
public class ShareDraftsActivity extends StyledActivity {
    final List<Button> actions=new ArrayList<>();final ExecutorService io=Executors.newSingleThreadExecutor();Store store;LinearLayout content;TextView notice;boolean busy;
    @Override protected void onCreate(Bundle state){super.onCreate(state);store=new Store(this);Ui.configureWindow(this);LinearLayout page=Ui.page(this);ImageButton back=Ui.iconButton(this,R.drawable.ic_chevron_left,L.t("Back","返回"));back.setOnClickListener(v->finish());LinearLayout header=Ui.topBar(this,page,back,L.t("Unfinished shares","未完成的分享"),false,null);for(int n=0;n<header.getChildCount();n++)if(header.getChildAt(n) instanceof TextView heading){heading.setSingleLine(false);heading.setMaxLines(2);}page.addView(Ui.caption(this,L.t("Saved on this phone. Nothing here has been handed off. Files are checked when you continue.","保存在这台手机，尚未交办。继续时会核验已保存文件。")),Ui.margins(this,12,16));notice=Ui.text(this,"",14,Ui.AMBER);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);notice.setVisibility(View.GONE);page.addView(notice);content=Ui.vertical(this);page.addView(content);}
    @Override protected void onResume(){super.onResume();show();}
    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
    void show(){
        if(busy)return;if(!store.scope.equals(new Store(this).scope)){finish();return;}
        content.removeAllViews();actions.clear();List<ShareDrafts.Entry> entries=ShareDrafts.list(store);
        if(entries.isEmpty()){content.addView(Ui.title(this,L.t("You're all caught up","没有未完成的分享"),22));return;}
        for(ShareDrafts.Entry entry:entries){
            LinearLayout card=Ui.card(this);String label=L.t("Saved share","已保存的分享");boolean readable=true;
            try{JSONObject record=entry.read();JSONObject editor=record.optJSONObject("editor");String note=editor==null?"":editor.optString("draft","");String text=record.optString("text","");label=!note.trim().isEmpty()?note:!text.isEmpty()&&!"null".equals(text)?text:L.t(record.getJSONArray("sources").length()+" incoming files",record.getJSONArray("sources").length()+" 份待处理附件");}catch(Exception e){readable=false;label=L.t("This saved share could not be read. Its files have been kept.","无法读取这份保存的分享，文件仍然保留。");}
            TextView title=Ui.title(this,TaskPresentation.clip(label,180),18);title.setMaxLines(4);card.addView(title);
            card.addView(Ui.caption(this,ShareImport.UNPAIRED.equals(entry.scope)?L.t("Saved before connecting a computer","连接电脑前保存"):store.computerName()),Ui.margins(this,8,12));
            Button open=Ui.button(this,entry.open()?L.t("Return to open share","返回正在编辑的分享"):L.t("Continue share","继续分享"),true);open.setEnabled(readable);open.setOnClickListener(v->open(entry));card.addView(open);actions.add(open);
            Button discard=Ui.button(this,L.t("Discard saved share","放弃已保存分享"),false);discard.setEnabled(!entry.open());actions.add(discard);discard.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L.t("Discard this saved share?","放弃这份已保存分享？")).setMessage(L.t("Only this share's phone copies and note will be removed. Original files in the source app stay there.","仅移除这份分享在手机上的副本和留言，来源 App 中的原文件不受影响。")).setNegativeButton(L.t("Keep","保留"),null).setPositiveButton(L.t("Discard","放弃"),(d,w)->discard(entry)).show());card.addView(discard,Ui.margins(this,8,0));content.addView(card,Ui.margins(this,0,14));
        }
    }
    void open(ShareDrafts.Entry entry){
        if(busy)return;ShareImport item=ShareImport.live(entry.id);Object owner=item==null?null:item.currentOwner();if(owner instanceof ShareActivity active&&!active.isFinishing()&&!active.isDestroyed()){active.startActivity(new Intent(active,active.getClass()).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP));return;}Runnable resume=()->startActivity(new Intent(this,RecoveredShareActivity.class).putExtra("draftId",entry.id).putExtra("draftScope",entry.scope));
        if(ShareImport.UNPAIRED.equals(entry.scope)&&!entry.scope.equals(store.scope))new AlertDialog.Builder(this).setTitle(L.t("Continue with this computer?","使用这台电脑继续？")).setMessage(store.computerName()+"\n"+store.relay()).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Continue","继续"),(d,w)->resume.run()).show();else resume.run();
    }
    void discard(ShareDrafts.Entry entry){
        if(busy)return;busy=true;notice.setVisibility(View.VISIBLE);notice.setText(L.t("Discarding saved share…","正在放弃已保存分享…"));for(Button action:actions)action.setEnabled(false);
        io.execute(()->{String error="";try{ShareDrafts.discard(store,entry);}catch(Exception e){error=e.getMessage();}String result=error;runOnUiThread(()->{if(isDestroyed())return;busy=false;notice.setText(result);notice.setVisibility(result==null||result.isEmpty()?View.GONE:View.VISIBLE);show();});});
    }
}
