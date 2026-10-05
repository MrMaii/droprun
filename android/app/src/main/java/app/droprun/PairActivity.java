package app.droprun;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.concurrent.*;

/** Pair only with the self-hosted origin shown to the person holding the phone. */
public class PairActivity extends StyledActivity {
    static final int SCAN=30,CAMERA=31;
    final ExecutorService io=Executors.newSingleThreadExecutor();
    Store store;Button scan,manual,drafts;TextView notice;boolean busy;int failures;AlertDialog manualDialog;EditText[] manualFields;
    @Override protected void onCreate(Bundle state){super.onCreate(state);store=createStore();Ui.configureWindow(this);failures=state==null?0:state.getInt("failures");build();String link=getIntent().getStringExtra("pairingLink");if(state==null&&link!=null)confirm(link);if(state!=null&&state.getBoolean("manualOpen")){manualEntry();String[] values=state.getStringArray("manualValues");if(values!=null)for(int n=0;n<Math.min(values.length,manualFields.length);n++)manualFields[n].setText(values[n]);}}
    Store createStore(){return new Store(this);}
    @Override protected void onResume(){super.onResume();drafts.setVisibility(ShareDrafts.list(store).isEmpty()?View.GONE:View.VISIBLE);if(store.paired()){setResult(RESULT_OK);finish();}}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("failures",failures);if(manualDialog!=null&&manualDialog.isShowing()){out.putBoolean("manualOpen",true);String[] values=new String[manualFields.length];for(int n=0;n<values.length;n++)values[n]=manualFields[n].getText().toString();out.putStringArray("manualValues",values);}}
    void build(){
        LinearLayout page=Ui.page(this);LinearLayout top=Ui.row(this);top.addView(Ui.brandMark(this,28),Ui.square(this,28));Ui.space(top,10);TextView brand=Ui.text(this,"DropRun",17,Ui.TEXT);brand.setTypeface(Ui.medium());top.addView(brand,Ui.grow());
        TextView language=Ui.linkButton(this,L.t("中文","English"));language.setOnClickListener(v->{store.preferences.edit().putString("language",L.chinese()?"en":"zh").apply();recreate();});top.addView(language);page.addView(top,Ui.margins(this,8,24));
        page.addView(Ui.title(this,L.t("Connect your\ncomputer.","连上电脑，\n让灵感接着走。"),32));
        page.addView(Ui.text(this,L.t("Share from your phone into the projects you choose on Windows.","把手机里的好参考，交给 Windows 上你选择的项目。"),17,Ui.MUTED),Ui.margins(this,10,18));
        LinearLayout steps=Ui.card(this);
        setupStep(steps,"1",L.t("Start on Windows","先在电脑准备"),L.t("Open DropRun setup on your computer.","在电脑打开 DropRun 安装向导。"));
        steps.addView(Ui.divider(this));
        setupStep(steps,"2",L.t("Use your own Relay","部署自己的中转服务"),L.t("Finish Cloudflare setup, then open Pair phone.","完成 Cloudflare 配置，打开手机配对页。"));
        steps.addView(Ui.divider(this));
        setupStep(steps,"3",L.t("Scan and confirm","扫码并核对"),L.t("Check the server address matches your computer.","确认服务器地址与电脑显示的一致。"));
        notice=Ui.text(this,"",14,Ui.AMBER);notice.setVisibility(View.GONE);notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(notice,Ui.margins(this,12,0));
        scan=Ui.button(this,L.t("Scan to connect","扫码连接"),true);scan.setOnClickListener(v->startScan());page.addView(scan,Ui.margins(this,20,0));
        manual=Ui.button(this,L.t("Use a pairing link or code","使用配对链接或配对码"),false);manual.setOnClickListener(v->manualEntry());page.addView(manual,Ui.margins(this,10,0));
        drafts=Ui.button(this,L.t("Unfinished shares on this phone","手机上未完成的分享"),false);drafts.setOnClickListener(v->startActivity(new Intent(this,ShareDraftsActivity.class)));page.addView(drafts,Ui.margins(this,10,0));
        page.addView(Ui.label(this,L.t("ONE-TIME SETUP","只需连接一次")),Ui.margins(this,24,0));page.addView(steps);
        page.addView(Ui.caption(this,L.t("No DropRun account. Your Relay carries shared material and reports; Codex credentials stay on your computer. Transcription uses only the provider you configure.","无需 DropRun 账户。材料与报告经你自己的中转服务传递，Codex 凭证留在电脑；转写仅使用你配置的服务。")),Ui.margins(this,20,0));Ui.enter(page);
    }
    void setupStep(LinearLayout parent,String number,String title,String detail){
        LinearLayout row=Ui.row(this);row.setGravity(android.view.Gravity.TOP);row.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,12));
        TextView index=Ui.pill(this,number,Ui.ACCENT);index.setGravity(android.view.Gravity.CENTER);index.setMinimumHeight(Ui.dp(this,28));row.addView(index,new LinearLayout.LayoutParams(Ui.dp(this,32),-2));Ui.space(row,12);
        LinearLayout words=Ui.vertical(this);words.addView(Ui.title(this,title,16));words.addView(Ui.text(this,detail,14,Ui.MUTED));row.addView(words,Ui.grow());parent.addView(row,Ui.fill());
    }
    void startScan(){if(checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},CAMERA);return;}startActivityForResult(new Intent(this,ScanActivity.class),SCAN);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==CAMERA){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startScan();else showNotice(L.t("Camera access is off. You can paste a pairing link instead.","相机权限未开启。也可以粘贴配对链接。"));}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=SCAN)return;if(result==RESULT_OK&&data!=null){confirm(data.getStringExtra("code"));return;}if(data!=null){showNotice(scanMessage(data.getStringExtra("reason")));failures++;if(data.getBooleanExtra("manual",false)||failures>=2)manualEntry();}}
    static String scanMessage(String reason){return "camera".equals(reason)?L.t("Camera unavailable. Use the pairing link or code.","相机暂不可用，请使用配对链接或配对码。"):L.t("No valid DropRun QR code found. Try again, or use the link from your computer.","没有识别到有效的 DropRun 二维码。可重试或使用电脑上的配对链接。");}
    void showNotice(String text){notice.setText(text);notice.setVisibility(View.VISIBLE);}
    EditText field(LinearLayout box,String label,String hint){box.addView(Ui.label(this,label));EditText input=new EditText(this);Ui.styleInput(input);input.setHint(hint);input.setContentDescription(label);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);box.addView(input);return input;}
    void manualEntry(){
        if(busy)return;ScrollView scroll=new ScrollView(this);LinearLayout box=Ui.vertical(this);int p=Ui.dp(this,20);box.setPadding(p,0,p,p);scroll.addView(box);
        EditText link=field(box,L.t("Pairing link","配对链接"),"droprun://pair?relay=…");box.addView(Ui.caption(this,L.t("Paste the full link, or fill in these three values from your computer.","粘贴完整链接，或填写电脑配对页上的以下三项。")),Ui.margins(this,12,2));
        EditText relay=field(box,L.t("Relay address","中转服务地址"),"https://your-relay.workers.dev");EditText instance=field(box,L.t("Instance ID","实例 ID"),"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");EditText code=field(box,L.t("Pairing code","配对码"),"XXXXX-XXXXX-XXXXX-XXXXX");
        TextView problem=Ui.text(this,"",14,Ui.AMBER);problem.setTag("manual-pair-error");problem.setVisibility(View.GONE);problem.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);box.addView(problem,Ui.margins(this,12,0));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(L.t("Connect manually","手动连接")).setView(scroll).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Continue","继续"),null).create();manualDialog=dialog;manualFields=new EditText[]{link,relay,instance,code};
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{PairingTarget target=link.getText().toString().trim().isEmpty()?new PairingTarget(relay.getText().toString(),instance.getText().toString(),code.getText().toString()):PairingTarget.parse(link.getText().toString());if(target==null)throw new IllegalArgumentException();dialog.dismiss();confirm(target);}catch(Exception invalid){problem.setText(L.t("Check the pairing link, or the Relay address, instance ID and code. Nothing was sent; your entries are kept.","请检查完整配对链接，或中转服务地址、实例 ID 和配对码。尚未发送配对请求，输入内容已保留。"));problem.setVisibility(View.VISIBLE);problem.post(()->problem.requestRectangleOnScreen(new android.graphics.Rect(0,0,problem.getWidth(),problem.getHeight()),true));}}));dialog.show();
    }
    void confirm(String link){PairingTarget target=PairingTarget.parse(link);if(target==null){showNotice(L.t("This link needs a Relay address, instance ID and pairing code.","配对链接须包含中转服务地址、实例 ID 和配对码。"));return;}confirm(target);}
    void confirm(PairingTarget target){if(busy)return;new AlertDialog.Builder(this).setTitle(L.t("Connect to your Relay?","连接到你的中转服务？")).setMessage(target.relay+"\n\n"+L.t("Instance ","实例 ")+target.instanceId+"\n\n"+L.t("Check that this matches the pairing page on your computer.","请核对它与电脑配对页显示的一致。" )).setNegativeButton(L.t("Cancel","取消"),null).setPositiveButton(L.t("Connect","连接"),(d,w)->pair(target)).show();}
    void pair(PairingTarget target){busy=true;scan.setEnabled(false);manual.setEnabled(false);showNotice(L.t("Connecting securely…","正在安全连接…"));io.execute(()->{try{store.pair(target);try{store.sync();}catch(Exception ignored){}runOnUiThread(()->{if(isDestroyed())return;setResult(RESULT_OK);finish();});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;busy=false;scan.setEnabled(true);manual.setEnabled(true);showNotice(e.getMessage());});}});}
    @Override protected void onDestroy(){if(manualDialog!=null&&manualDialog.isShowing())manualDialog.dismiss();io.shutdown();super.onDestroy();}
}
