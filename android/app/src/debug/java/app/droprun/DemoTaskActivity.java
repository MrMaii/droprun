package app.droprun;
import android.os.Bundle;
public final class DemoTaskActivity extends TaskActivity {
    @Override public void onCreate(Bundle state){DemoFixture.seed(this);getIntent().putExtra("taskId",DemoFixture.TASK);super.onCreate(state);}
    @Override void load(){}
    @Override void loadThumbnail(){thumbnailRequested=true;}
    @Override void render(){super.render();notice.setText(DemoFixture.NOTICE);notice.setVisibility(android.view.View.VISIBLE);}
    @Override void perform(Work work){notice(DemoFixture.NOTICE);}
    @Override void followup(){notice(DemoFixture.NOTICE);}
    @Override void openDeliverables(){notice(DemoFixture.NOTICE);}
}
