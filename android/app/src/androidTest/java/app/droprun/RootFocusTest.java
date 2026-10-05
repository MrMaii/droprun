package app.droprun;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Focus may highlight a control, but must not recolor its entire page. */
public class RootFocusTest {
    @Test public void pageFocusPreservesSurfaceColors()throws Exception{checkRoot(false);}
    @Test public void overlayRootFocusPreservesSurfaceColors()throws Exception{checkRoot(true);}
    @Test public void buttonKeepsLocalizedFocusAndKeyboardActivation()throws Exception{
        try(ActivityScenario<DemoFocusActivity> scenario=ActivityScenario.launch(DemoFocusActivity.class)){
            android.widget.ImageButton[] button={null};View[] surface={null};ViewGroup[] root={null};int[] colors={0,0};java.util.concurrent.atomic.AtomicInteger clicks=new java.util.concurrent.atomic.AtomicInteger();
            scenario.onActivity(activity->{LinearLayout content=Ui.page(activity);root[0]=(ViewGroup)content.getParent();surface[0]=new View(activity);surface[0].setBackgroundColor(Ui.SURFACE);content.addView(surface[0],new LinearLayout.LayoutParams(-1,Ui.dp(activity,96)));button[0]=Ui.iconButton(activity,R.drawable.ic_gear,"Settings");button[0].setOnClickListener(v->clicks.incrementAndGet());button[0].setFocusable(false);content.addView(button[0],Ui.square(activity,48));root[0].setFocusable(false);});
            awaitFrame(scenario,root[0]);InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->{colors[0]=accentPixels(button[0]);button[0].setFocusable(true);assertTrue(button[0].requestFocus());button[0].jumpDrawablesToCurrentState();});
            awaitFrame(scenario,root[0]);
            scenario.onActivity(activity->{assertTrue("Keep the control's visible focus ring",accentPixels(button[0])>colors[0]);assertEquals("Focus stays local to the button",Ui.SURFACE,surfacePixel(root[0],surface[0]));});
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);assertEquals("The focused button remains keyboard operable",1,clicks.get());
        }
    }
    void checkRoot(boolean overlay)throws Exception{
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        try(ActivityScenario<DemoFocusActivity> scenario=ActivityScenario.launch(DemoFocusActivity.class)){
            ViewGroup[] root={null};View[] surface={null};
            scenario.onActivity(activity->{
                LinearLayout content;
                if(overlay){FrameLayout frame=Ui.frame(activity,true);root[0]=frame;content=Ui.vertical(activity);frame.addView(content,new FrameLayout.LayoutParams(-1,-1));}
                else{content=Ui.page(activity);root[0]=(ViewGroup)content.getParent();}
                surface[0]=new View(activity);surface[0].setBackgroundColor(Ui.SURFACE);content.addView(surface[0],new LinearLayout.LayoutParams(-1,Ui.dp(activity,96)));
                root[0].setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);root[0].setFocusable(false);
            });
            awaitFrame(scenario,root[0]);
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            int[] colors={0,0};
            scenario.onActivity(activity->{assertFalse("Exercise real keyboard focus, not touch mode",root[0].isInTouchMode());colors[0]=surfacePixel(root[0],surface[0]);root[0].setFocusable(true);assertTrue("Structural roots still accept keyboard focus",root[0].requestFocus());assertTrue(root[0].isFocused());root[0].jumpDrawablesToCurrentState();});
            awaitFrame(scenario,root[0]);
            scenario.onActivity(activity->{colors[1]=surfacePixel(root[0],surface[0]);assertEquals("The unfocused surface uses the actual theme token",Ui.SURFACE,colors[0]);assertEquals("Root focus must not tint content",colors[0],colors[1]);});
        }
    }
    static int surfacePixel(ViewGroup root,View surface){
        Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));android.graphics.Rect rect=new android.graphics.Rect();surface.getDrawingRect(rect);root.offsetDescendantRectToMyCoords(surface,rect);
        int color=bitmap.getPixel(rect.centerX(),rect.centerY());bitmap.recycle();return color;
    }
    static int accentPixels(View control){
        Bitmap bitmap=Bitmap.createBitmap(control.getWidth(),control.getHeight(),Bitmap.Config.ARGB_8888);control.draw(new Canvas(bitmap));int count=0;
        for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++){int color=bitmap.getPixel(x,y);if(android.graphics.Color.green(color)>android.graphics.Color.red(color)+15&&android.graphics.Color.green(color)>android.graphics.Color.blue(color)+15)count++;}bitmap.recycle();return count;
    }
    static void awaitFrame(ActivityScenario<DemoFocusActivity> scenario,View root)throws Exception{
        CountDownLatch frame=new CountDownLatch(1);scenario.onActivity(activity->{root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){root.getViewTreeObserver().removeOnPreDrawListener(this);frame.countDown();return true;}});root.invalidate();});
        assertTrue("Wait for native rendering",frame.await(3,TimeUnit.SECONDS));
    }
}
