package app.droprun;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

/** Apply appearance before native controls are created; refresh returning screens after a setting change. */
public class StyledActivity extends Activity {
    private String appearance;
    @Override protected void onCreate(Bundle state) {
        Ui.prepare(this,this instanceof ShareActivity);appearance=Ui.preferenceKey(this);super.onCreate(state);
    }
    @Override protected void onResume() {
        super.onResume();
        if(!Ui.preferenceKey(this).equals(appearance)&&!(this instanceof ShareActivity))recreate();
    }
    @Override public void startActivityForResult(Intent intent,int request,Bundle options){
        super.startActivityForResult(intent,request,options);
        if(!(this instanceof ShareActivity)&&intent.getComponent()!=null&&intent.getComponent().getPackageName().equals(getPackageName()))
            overridePendingTransition(Ui.motionEnabled(this)?R.anim.page_in:0,Ui.motionEnabled(this)?R.anim.page_behind:0);
    }
    @Override public void finish(){
        super.finish();
        if(!(this instanceof ShareActivity))overridePendingTransition(Ui.motionEnabled(this)?R.anim.page_reveal:0,Ui.motionEnabled(this)?R.anim.page_out:0);
    }
}
