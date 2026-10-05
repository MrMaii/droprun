package app.droprun;

import android.app.Activity;
import android.os.Bundle;

/** Rendering fixture only: no Store, pairing, shares or network calls. */
public final class DemoFocusActivity extends Activity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);Ui.configureWindow(this);Ui.page(this);}
}
