package app.droprun;

import android.app.Activity;
import android.os.Bundle;

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
}
