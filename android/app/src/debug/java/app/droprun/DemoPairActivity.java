package app.droprun;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.io.IOException;

/** Actual pairing UI with an unpaired local fixture; never connects to a Relay. */
public final class DemoPairActivity extends PairActivity {
    @Override protected void onCreate(Bundle state){if(state==null)DemoFixture.seed(this);super.onCreate(state);}
    @Override Store createStore(){return new Store(this){
        @Override boolean paired(){return false;}
        @Override void pair(PairingTarget target)throws Exception{throw new IOException("Local UI demonstration: no pairing request was sent.");}
    };}
    @Override void build(){
        super.build();
        ScrollView scroll=(ScrollView)((ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        LinearLayout page=(LinearLayout)scroll.getChildAt(0);
        page.addView(Ui.text(this,DemoFixture.NOTICE,12,Ui.AMBER),1);
    }
}
