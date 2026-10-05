package app.droprun;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Real IME and touch input on the existing in-memory editor. Never activates Send. */
public class ShareKeyboardTest {
    final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();

    @Test public void keyboardTouchScrollingAndSystemBackKeepTheNoteInBothLanguages()throws Exception{
        for(String language:new String[]{"en","zh"})checkEditor(language,"keep this note");
    }
    @Test public void multilineKeyboardEntryKeepsTheCaretAndSendReachable()throws Exception{
        checkEditor("en","line one\nline two\nline three\nline four\nline five\nline six\nline seven\nline eight");
    }
    void checkEditor(String language,String message)throws Exception{
        Context context=ShareEditorTest.fixtureContext();assertTrue("Real IME inset verification requires API30+",Build.VERSION.SDK_INT>=30);
        String label=language+(message.contains("\n")?"-multiline":"-short");
        try(ActivityScenario<DemoShareEditorActivity> scenario=ShareEditorTest.launch(context,language,0)){
            try{
                ShareEditorTest.ready(scenario);touchReveal(scenario,false);tapNote(scenario);awaitIme(scenario,true);
                instrumentation.sendStringSync(message);instrumentation.waitForIdleSync();
                scenario.onActivity(a->{assertTrue(a.note.hasFocus());assertEquals(message,a.note.getText().toString());assertEquals(message,a.draft);assertCaretVisible(a);ShareEditorTest.assertSafe(a);});capture(scenario,label+"-typing");
                touchReveal(scenario,true);scenario.onActivity(a->{assertTrue(imeVisible(a));assertTrue("Send must fit above the actual keyboard",viewport(a).contains(ShareEditorTest.screenBounds(ShareEditorTest.send(a,language))));assertEquals(message,a.note.getText().toString());ShareEditorTest.assertSafe(a);});capture(scenario,label+"-send-visible");
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);awaitIme(scenario,false);
                scenario.onActivity(a->{assertEquals("First system Back hides the keyboard without changing the step",1,a.step);assertNull(a.discardDialog);assertFalse(a.closing);assertFalse(a.isFinishing());assertEquals(message,a.note.getText().toString());assertEquals(message,a.draft);ShareEditorTest.assertSafe(a);});capture(scenario,label+"-back");
                touchReveal(scenario,false);tapNote(scenario);awaitIme(scenario,true);
                scenario.onActivity(a->{assertEquals(1,a.step);assertEquals(message,a.note.getText().toString());assertEquals(message,a.draft);ShareEditorTest.assertSafe(a);});capture(scenario,label+"-reopened");
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);awaitIme(scenario,false);
            }catch(Exception|AssertionError failure){try{capture(scenario,label+"-failure");}catch(Exception|AssertionError evidence){failure.addSuppressed(evidence);}throw failure;}
        }
    }
    boolean imeVisible(DemoShareEditorActivity a){WindowInsets insets=a.root.getRootWindowInsets();return insets!=null&&insets.isVisible(WindowInsets.Type.ime())&&insets.getInsets(WindowInsets.Type.ime()).bottom>0;}
    void awaitIme(ActivityScenario<DemoShareEditorActivity> scenario,boolean visible)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+5000;boolean[] matched={false};
        do{instrumentation.waitForIdleSync();scenario.onActivity(a->matched[0]=imeVisible(a)==visible);if(matched[0]){ShareEditorTest.awaitFrame(scenario);return;}Thread.sleep(40);}while(SystemClock.elapsedRealtime()<deadline);
        scenario.onActivity(a->diagnostics(a,"ime-timeout"));fail(visible?"A real software keyboard must become visible":"The real software keyboard must close");
    }
    Rect viewport(DemoShareEditorActivity a){
        Rect safe=ShareEditorTest.screen(a);WindowInsets insets=a.root.getRootWindowInsets();if(insets!=null)safe.bottom-=insets.getInsets(WindowInsets.Type.ime()).bottom;
        assertTrue(safe.intersect(ShareEditorTest.screenBounds(a.root)));assertTrue(safe.intersect(ShareEditorTest.screenBounds(a.scroll)));return safe;
    }
    void touchReveal(ActivityScenario<DemoShareEditorActivity> scenario,boolean send)throws Exception{
        for(int attempt=0;attempt<12;attempt++){
            boolean[] ready={false};float[] gesture=new float[5];
            scenario.onActivity(a->{View target=send?ShareEditorTest.send(a,L.chinese()?"zh":"en"):a.note;Rect safe=viewport(a),bounds=ShareEditorTest.screenBounds(target);ready[0]=safe.contains(bounds);
                if(!send&&bounds.height()>safe.height()){Rect visible=new Rect(bounds);ready[0]=visible.intersect(safe)&&visible.height()>=Ui.dp(a,48);}
                gesture[0]=safe.right-Ui.dp(a,6);gesture[1]=safe.bottom-Ui.dp(a,12);gesture[2]=safe.top+Ui.dp(a,12);gesture[3]=bounds.top<safe.top?1:0;gesture[4]=safe.height();});
            if(ready[0])return;assertTrue("The editor retains room for a touch scroll",gesture[4]>48);
            swipe(gesture[0],gesture[3]==1?gesture[2]:gesture[1],gesture[3]==1?gesture[1]:gesture[2]);instrumentation.waitForIdleSync();ShareEditorTest.awaitFrame(scenario);
        }
        scenario.onActivity(a->diagnostics(a,"touch-scroll-timeout"));fail("Touch scrolling must reach "+(send?"Send":"the note"));
    }
    void tapNote(ActivityScenario<DemoShareEditorActivity> scenario){
        float[] point=new float[2];scenario.onActivity(a->{Rect touch=ShareEditorTest.screenBounds(a.note);assertTrue(touch.intersect(viewport(a)));point[0]=touch.centerX();point[1]=touch.centerY();});
        long down=SystemClock.uptimeMillis();inject(MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,point[0],point[1],0));SystemClock.sleep(40);inject(MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,point[0],point[1],0));
    }
    void swipe(float x,float from,float to)throws Exception{
        long down=SystemClock.uptimeMillis();inject(MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,from,0));
        for(int n=1;n<=8;n++){Thread.sleep(25);inject(MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,x,from+(to-from)*n/8f,0));}
        inject(MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,to,0));
    }
    void inject(MotionEvent event){try{event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);assertTrue("Inject an actual touch event",instrumentation.getUiAutomation().injectInputEvent(event,true));}finally{event.recycle();}}
    void assertCaretVisible(DemoShareEditorActivity a){
        Layout layout=a.note.getLayout();assertNotNull(layout);int offset=a.note.getSelectionEnd();assertTrue(offset>=0);int line=layout.getLineForOffset(offset);int[] location=new int[2];a.note.getLocationOnScreen(location);
        int x=location[0]+a.note.getTotalPaddingLeft()+Math.round(layout.getPrimaryHorizontal(offset))-a.note.getScrollX(),y=location[1]+a.note.getTotalPaddingTop()-a.note.getScrollY();
        Rect caret=new Rect(x,y+layout.getLineTop(line),x+1,y+layout.getLineBottom(line));assertTrue("The active caret line must remain above the real keyboard",viewport(a).contains(caret));
    }
    void diagnostics(DemoShareEditorActivity a,String phase){
        WindowInsets insets=a.root.getRootWindowInsets();Bundle status=new Bundle();status.putString("stream","ImeProbe "+phase+" visible="+imeVisible(a)+" bottom="+(insets==null?-1:insets.getInsets(WindowInsets.Type.ime()).bottom)+" rootPadding="+a.root.getPaddingBottom()+" viewport="+viewport(a)+" scrollY="+a.scroll.getScrollY()+" note="+ShareEditorTest.screenBounds(a.note)+" selection="+a.note.getSelectionStart()+":"+a.note.getSelectionEnd()+"\n");instrumentation.sendStatus(0,status);
    }
    void capture(ActivityScenario<DemoShareEditorActivity> scenario,String name)throws Exception{
        scenario.onActivity(a->diagnostics(a,name));if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureUi")))return;
        ShareEditorTest.awaitFrame(scenario);instrumentation.waitForIdleSync();Bitmap screenshot=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(screenshot);
        File base=instrumentation.getTargetContext().getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ime-probe-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}finally{screenshot.recycle();}
    }
}
