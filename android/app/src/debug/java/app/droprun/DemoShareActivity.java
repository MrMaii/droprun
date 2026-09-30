package app.droprun;
import android.os.Bundle;
public final class DemoShareActivity extends ShareActivity {
    @Override protected void onCreate(Bundle state){
        if(state==null)DemoFixture.seed(this);
        if(getIntent().getBooleanExtra("twoLocalInputs",false)||getIntent().getBooleanExtra("localReceiveFailure",false)){
            java.util.ArrayList<android.net.Uri> sources=new java.util.ArrayList<>();
            for(String path:new String[]{"first-input",getIntent().getBooleanExtra("localReceiveFailure",false)?"flaky-input":"held-input"})sources.add(android.net.Uri.parse("content://"+getPackageName()+".import-fixture/"+path));
            getIntent().setAction(android.content.Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream").putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM,sources);
        }
        if(!android.content.Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))getIntent().setAction(android.content.Intent.ACTION_SEND);
        if(getIntent().getType()==null)getIntent().setType("text/plain");getIntent().putExtra(android.content.Intent.EXTRA_TEXT,"https://example.com/design-inspiration");super.onCreate(state);sheet.addView(Ui.caption(this,DemoFixture.NOTICE),0);
    }
    @Override void start(){dots.setVisibility(android.view.View.VISIBLE);last="demo-studio";model=store.defaultModel();effort=store.defaultEffort(model);go(0,1);}
    @Override void received(){super.received();if(getIntent().getBooleanExtra("draftRecoveryProbe",false)&&step==0){selected="demo-studio";go(1,1);note.setText("Keep this local process-recovery note.");effort="high";updateGauge();}}
    @Override boolean submit(){android.widget.Toast.makeText(this,"UI preview only. No handoff was sent.",android.widget.Toast.LENGTH_LONG).show();return false;}
}
