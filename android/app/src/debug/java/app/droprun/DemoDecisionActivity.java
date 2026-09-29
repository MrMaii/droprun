package app.droprun;

import org.json.JSONObject;

/** Real decision/error UI with a local refresh response, independent of DNS or a Relay. */
public final class DemoDecisionActivity extends TaskActivity {
    @Override Store createStore(){return new Store(this){
        @Override JSONObject refreshTask(String id){
            if(!DemoFixture.TASK.equals(id))throw new IllegalArgumentException("Unexpected fixture task");
            return task(id);
        }
    };}
}
