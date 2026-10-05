package app.droprun;

import android.content.Context;
import android.content.res.Configuration;

/** Local Activity text scale only; system/app preferences remain unchanged. */
public final class DemoSettingsRecreationNormalActivity extends DemoSettingsRecreationActivity {
    @Override protected void attachBaseContext(Context base){
        Configuration configuration=new Configuration(base.getResources().getConfiguration());configuration.fontScale=1f;
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }
}
