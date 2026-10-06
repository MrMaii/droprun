package app.droprun;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import org.json.JSONException;

/** Fixed memory report for one real hierarchy-restoration probe; inherits all business denials. */
public final class DemoTaskScrollActivity extends DemoTaskPreviewActivity {
    static final String MIDDLE="Paragraph 12.";
    static final String REPORT=report();
    int seeds,renders,restoreCalls,rendersBeforeRestore;
    String nonce;

    static String report(){
        StringBuilder text=new StringBuilder("## Memory-only reading sample\n\n");
        for(int n=1;n<=24;n++)text.append("Paragraph ").append(n).append(". This fixed local paragraph checks reading position only. No task ran and no material was sent.\n\n");
        return text.toString();
    }
    @Override protected void attachBaseContext(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=1f;
        super.attachBaseContext(base.createConfigurationContext(config));
    }
    @Override public void onCreate(Bundle state){
        if(!getIntent().getBooleanExtra("taskScrollProbe",false))throw forbidden("missing scroll opt-in");
        nonce=getIntent().getStringExtra("nonce");
        if(nonce==null||!nonce.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw forbidden("invalid nonce");
        if(getIntent().hasExtra("thumbnailProbe")||getIntent().hasExtra("snapshotVersionProbe"))throw forbidden("unrelated preview probe");
        super.onCreate(state);
    }
    @Override Store createStore(){
        if(++seeds!=1||sample==null)throw forbidden("unexpected report seed");
        // The parent created this synthetic object, but TaskActivity has not rendered it yet.
        try{sample.put("report",REPORT).put("created_at",1L);}catch(JSONException error){throw new AssertionError(error);}
        return super.createStore();
    }
    @Override void render(){super.render();renders++;}
    @Override protected void onRestoreInstanceState(Bundle state){
        rendersBeforeRestore=renders;super.onRestoreInstanceState(state);restoreCalls++;
    }
}
