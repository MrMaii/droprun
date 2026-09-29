package app.droprun;
import android.os.Bundle;
public final class DemoSettingsActivity extends SettingsActivity {
    @Override protected void onCreate(Bundle state){if(state==null)DemoFixture.seed(this);super.onCreate(state);}
    @Override void refresh(){}
    @Override void render(){notice=DemoFixture.NOTICE;super.render();}
    @Override void setPermission(String id,boolean enabled){notice=DemoFixture.NOTICE;render();}
    @Override void saveMode(boolean direct){notice=DemoFixture.NOTICE;render();}
    @Override void disconnect(){notice=DemoFixture.NOTICE;render();}
}
