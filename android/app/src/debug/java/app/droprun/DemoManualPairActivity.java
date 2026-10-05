package app.droprun;

import java.io.File;
import org.json.JSONObject;

/** Manual-entry rendering only: no fixture seeding, draft changes or pairing requests. */
public final class DemoManualPairActivity extends PairActivity {
    int confirmations,pairingAttempts,networkAttempts;
    PairingTarget confirmedTarget;

    @Override Store createStore(){
        Store local=new Store(this){
            @Override boolean paired(){return false;}
            @Override File attachments(){return new File(getCacheDir(),"manual-pair-fixture-unused");}
            @Override void pair(PairingTarget target){pairingAttempts++;throw new AssertionError("Manual-entry fixture must not pair");}
            @Override JSONObject api(String path,String method,byte[] body,String mime,String filename){networkAttempts++;throw new AssertionError("Manual-entry fixture must not call an API");}
            @Override JSONObject request(String origin,String path,String method,byte[] body,String mime,String filename,String token){networkAttempts++;throw new AssertionError("Manual-entry fixture must not make a request");}
        };
        local.select("","");return local;
    }
    @Override void build(){L.language(getIntent().getStringExtra("language"));super.build();}
    @Override void confirm(PairingTarget target){confirmedTarget=target;confirmations++;}
    @Override protected void onDestroy(){L.language(store.preferences.getString("language","en"));super.onDestroy();}
}
