package app.droprun;

import android.content.Context;
import android.content.res.Configuration;

/** Normal-size text belongs only to this diagnostic Activity's base context. */
public final class DemoTaskPreviewNormalActivity extends DemoTaskPreviewActivity {
    @Override protected void attachBaseContext(Context base){
        Configuration configuration=new Configuration(base.getResources().getConfiguration());
        configuration.fontScale=1f;
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }
}
