package app.droprun;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Native typography checks against the debug-only fixture, without sending or removing shares. */
public class TaskTypographyTest {
    @Test public void longQueuedStatusAndDateRemainReadableAtLargeText()throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        String relay=new Store(context).relay();
        assertTrue("Do not replace an existing real connection",relay.isEmpty()||relay.equals("https://preview.example.invalid"));
        assertEquals("Run this focused check at 200% font size",2f,context.getResources().getConfiguration().fontScale,0.01f);
        try(ActivityScenario<DemoTaskActivity> scenario=ActivityScenario.launch(new Intent(context,DemoTaskActivity.class))){
            String[] original={null,null};
            try{
                scenario.onActivity(activity->{try{
                    original[0]=activity.store.task(activity.taskId).toString();
                    original[1]=activity.store.prefs.getString("tasks","");
                    JSONObject task=new JSONObject(original[0]).put("status","queued_execution");
                    JSONObject list=new JSONObject(original[1]);org.json.JSONArray tasks=list.getJSONArray("tasks");
                    for(int n=0;n<tasks.length();n++)if(activity.taskId.equals(tasks.getJSONObject(n).optString("id")))tasks.put(n,task);
                    activity.store.prefs.edit().putString("task:"+activity.taskId,task.toString()).putString("tasks",list.toString()).commit();
                    assertEquals("queued_execution",activity.store.task(activity.taskId).optString("status"));activity.render();
                }catch(Exception error){throw new AssertionError(error);}});
                CountDownLatch frame=new CountDownLatch(1);
                scenario.onActivity(activity->{View root=activity.getWindow().getDecorView();root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();return true;}});root.invalidate();});
                assertTrue("Wait for native text layout",frame.await(3,TimeUnit.SECONDS));
                scenario.onActivity(activity->{
                    assertTrue("Exercise a compact phone width",activity.body.getWidth()<=Ui.dp(activity,320));
                    TextView status=find(activity.body,"Approved · waiting for computer"),date=find(activity.body,"Shared ");
                    assertNotNull("Render the queued execution status",status);assertNotNull("Render the share date",date);assertSame(status.getParent(),date.getParent());
                    assertEquals(LinearLayout.VERTICAL,((LinearLayout)date.getParent()).getOrientation());
                    for(TextView text:new TextView[]{status,date}){
                        assertTrue("Status and date both need visible width",text.getWidth()>Ui.dp(activity,80));
                        android.text.Layout layout=text.getLayout();assertNotNull(layout);
                        assertEquals("No status or date text is omitted",text.length(),layout.getLineEnd(layout.getLineCount()-1));
                        assertTrue("Every rendered text line fits vertically",text.getHeight()>=layout.getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());
                        for(int line=0;line<layout.getLineCount();line++)assertEquals("Do not ellipsize a long status",0,layout.getEllipsisCount(line));
                    }
                    assertTrue("The date follows the full status",date.getTop()>=status.getBottom());
                });
            }finally{scenario.onActivity(activity->{if(original[0]!=null&&original[1]!=null){activity.store.prefs.edit().putString("task:"+activity.taskId,original[0]).putString("tasks",original[1]).commit();activity.render();}});}
        }
    }
    static TextView find(View view,String prefix){
        if(view instanceof TextView&&((TextView)view).getText().toString().startsWith(prefix))return (TextView)view;
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){TextView found=find(((ViewGroup)view).getChildAt(n),prefix);if(found!=null)return found;}
        return null;
    }
}
