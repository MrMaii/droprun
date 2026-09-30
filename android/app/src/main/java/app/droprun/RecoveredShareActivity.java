package app.droprun;

import android.os.Bundle;
import org.json.JSONObject;

/** Private route: exported share intents cannot select an existing import ID. */
public class RecoveredShareActivity extends ShareActivity {
    android.content.Intent material;
    @Override Bundle recoveryState(Bundle state)throws Exception{
        if(state!=null&&state.getBoolean("sent"))return state;
        ShareDrafts.Entry entry=ShareDrafts.find(store,state==null?getIntent().getStringExtra("draftScope"):state.getString("importScope"),state==null?getIntent().getStringExtra("draftId"):state.getString("importId"));
        JSONObject record=entry.read();material=ShareDrafts.payload(record);return state==null?ShareDrafts.state(entry,record):state;
    }
    @Override android.content.Intent importIntent(){return material==null?getIntent():material;}
}
