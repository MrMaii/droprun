package app.droprun;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import org.json.JSONObject;

/** Displays real save-feedback controls only. Nothing is imported, saved, submitted or paired. */
public final class DemoShareFeedbackActivity extends ShareActivity {
    boolean probeReady,phoneOnline,notificationsEnabled;
    int initializationCloses,closeRequests,networkAttempts,saveAttempts,pairingAttempts;
    long landedAt,closedAt;
    String landedStatus="",landedTitle="";
    final CountDownLatch landedSignal=new CountDownLatch(1),closedSignal=new CountDownLatch(1),destroyedSignal=new CountDownLatch(1);

    @Override Bundle recoveryState(Bundle state){Bundle display=new Bundle();display.putBoolean("sent",true);return display;}
    @Override void build(){L.language(getIntent().getStringExtra("language"));super.build();}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(incoming!=null)throw new AssertionError("Feedback probe must never open an import");
        phoneOnline=getIntent().getBooleanExtra("online",true);notificationsEnabled=getIntent().getBooleanExtra("notifications",true);
        store=new Store(this){
            @Override boolean online(){return phoneOnline;}
            @Override boolean directExecution(){return true;}
            @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){networkAttempts++;throw new AssertionError("Feedback probe must not call an API");}
            @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){networkAttempts++;throw new AssertionError("Feedback probe must not make a request");}
            @Override void save(JSONObject task){saveAttempts++;throw new AssertionError("Feedback probe must not save a task");}
            @Override void pair(PairingTarget target){pairingAttempts++;throw new AssertionError("Feedback probe must not pair");}
            @Override File attachments(){throw new AssertionError("Feedback probe must not access drafts");}
            @Override File outbox(){throw new AssertionError("Feedback probe must not access the outbox");}
        };
        store.select("","");L.language(getIntent().getStringExtra("language"));
        shared="Local feedback UI sample";draft="Local feedback UI note";step=2;receiving=false;
        TextView notice=Ui.text(this,L.t("UI probe · nothing was saved or sent.","界面探针 · 未保存或发送任何分享。"),12,0xFFFFFFFF);notice.setPadding(dp(20),dp(4),dp(20),0);root.addView(notice,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        stage.addView(stepSend(),new ViewGroup.LayoutParams(-1,-2));dots.setActive(2,false);back.setVisibility(View.INVISIBLE);probeReady=true;
    }
    @Override String projectName(){return L.t("Local UI probe","本地界面探针");}
    @Override boolean notificationsAllowed(){return notificationsEnabled;}
    @Override boolean submit(){throw new AssertionError("Feedback probe must never submit");}
    @Override void landed(){landedAt=SystemClock.uptimeMillis();super.landed();landedStatus=status.getText().toString();landedTitle=sendTitle.getText().toString();landedSignal.countDown();}
    @Override void close(){
        if(!probeReady){initializationCloses++;return;}
        if(closing)return;closeRequests++;closedAt=SystemClock.uptimeMillis();super.close();closedSignal.countDown();
    }
    @Override protected void onDestroy(){L.language(store.preferences.getString("language","en"));super.onDestroy();destroyedSignal.countDown();}
}
