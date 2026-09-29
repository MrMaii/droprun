package app.droprun;
import android.os.Bundle;
public final class DemoHomeActivity extends MainActivity {
    @Override public void onCreate(Bundle state){if(state==null)DemoFixture.seed(this);super.onCreate(state);}
    @Override void load(){}
    @Override void show(){super.show();notice.setText(DemoFixture.NOTICE);notice.setVisibility(android.view.View.VISIBLE);notice.setOnClickListener(null);notice.setClickable(false);notice.setFocusable(false);}
}
