package app.droprun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static app.droprun.TaskPreviewFeedbackTest.frames;
import static app.droprun.TaskPreviewFeedbackTest.screenBounds;
import static org.junit.Assert.*;

/** Only memory bitmaps and a memory click counter; no delivery file or business navigation. */
public class TaskThumbnailTest {
    @Test public void bitmapTargetKeepsItsAspectAndAtLeast48dp()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})for(String shape:new String[]{"wide","standard"})
            try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language,theme,shape,1f)){
                frames(scenario);
                if("true".equals(InstrumentationRegistry.getArguments().getString("trialMinimumHeight")))scenario.onActivity(a->picture(target(a)).setMinimumHeight(Ui.dp(a,48)));
                frames(scenario);reveal(scenario);capture(scenario,language,theme,shape,"idle");
                scenario.onActivity(a->{View target=target(a);assertGeometry(a,target);assertState(a,target,false);assertSafe(a);});
            }
    }
    @Test public void busyThumbnailIgnoresClicksAndRestoresItsEntry()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})
            try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language,theme,"standard",1f)){
                frames(scenario);scenario.onActivity(a->{
                    View idle=target(a);assertState(a,idle,false);idle.performClick();assertEquals("Idle activation counts only in memory",1,a.thumbnailOpens);
                    a.busy=true;idle.performClick();int stale=a.thumbnailOpens-1;a.render();View pending=target(a);pending.performClick();
                    Bundle observed=new Bundle();observed.putString("thumbnail_busy_probe","enabled="+pending.isEnabled()+", focusable="+pending.isFocusable()+", stale_clicks="+stale+", total_busy_clicks="+(a.thumbnailOpens-1));InstrumentationRegistry.getInstrumentation().sendStatus(0,observed);
                    assertEquals("Busy stale/current callbacks must not dispatch",1,a.thumbnailOpens);assertState(a,pending,true);assertSafe(a);
                });frames(scenario);reveal(scenario);capture(scenario,language,theme,"standard","busy");
                scenario.onActivity(a->{a.busy=false;a.render();View restored=target(a);assertState(a,restored,false);restored.performClick();assertEquals(2,a.thumbnailOpens);assertSafe(a);});
                scenario.recreate();frames(scenario);reveal(scenario);scenario.onActivity(a->{assertEquals(0,a.thumbnailOpens);assertState(a,target(a),false);assertGeometry(a,target(a));assertSafe(a);});
            }
    }
    @Test public void systemLargeTextKeepsEachThumbnailAndFilesControlReachable()throws Exception{
        for(String language:new String[]{"en","zh"})for(String theme:new String[]{"light","dark"})
            try(ActivityScenario<DemoTaskPreviewActivity> scenario=launch(language,theme,"standard",2f)){
                frames(scenario);reveal(scenario);scenario.onActivity(a->{assertEquals(2f,a.getResources().getConfiguration().fontScale,0f);assertGeometry(a,target(a));assertState(a,target(a),false);assertSafe(a);});
                scenario.onActivity(a->{TextView files=files(a);files.requestRectangleOnScreen(new Rect(0,0,files.getWidth(),files.getHeight()),true);});frames(scenario);
                scenario.onActivity(a->{TaskPreviewFeedbackTest.assertCompleteAndVisible(a,files(a));assertTrue(files(a).getHeight()>=Ui.dp(a,48));assertSafe(a);});
                scenario.recreate();frames(scenario);reveal(scenario);scenario.onActivity(a->{assertGeometry(a,target(a));assertState(a,target(a),false);assertSafe(a);});
            }
    }
    static ActivityScenario<DemoTaskPreviewActivity> launch(String language,String theme,String shape,float scale){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();assertTrue(context.getPackageName().endsWith(".debug"));
        return ActivityScenario.launch(new Intent(context,scale==1f?DemoTaskPreviewNormalActivity.class:DemoTaskPreviewActivity.class).putExtra("language",language).putExtra("appearance",theme).putExtra("thumbnailProbe",true).putExtra("bitmapShape",shape));
    }
    static View target(DemoTaskPreviewActivity a){View target=findTarget(a.body);assertNotNull("Find actual thumbnail action",target);return target;}
    static View findTarget(View view){
        if(L.t("Verified screenshot from this handoff. Open all delivery files.","本次交办的已校验截图。打开全部交付文件。").contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){View found=findTarget(((ViewGroup)view).getChildAt(n));if(found!=null)return found;}return null;
    }
    static ImageView picture(View view){if(view instanceof ImageView)return (ImageView)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){ImageView found=picture(((ViewGroup)view).getChildAt(n));if(found!=null)return found;}return null;}
    static TextView files(DemoTaskPreviewActivity a){TextView files=TaskPreviewFeedbackTest.find(a.body,L.t("Screenshots & delivery files","截图与交付文件"));assertNotNull(files);return files;}
    static void assertState(DemoTaskPreviewActivity a,View target,boolean busy){
        assertEquals(!busy,target.isEnabled());assertEquals(!busy,target.isFocusable());assertEquals(!busy,files(a).isEnabled());
        AccessibilityNodeInfo node=target.createAccessibilityNodeInfo();try{assertEquals(!busy,node.isEnabled());assertEquals(!busy,node.isFocusable());assertEquals(L.t("Verified screenshot from this handoff. Open all delivery files.","本次交办的已校验截图。打开全部交付文件。"),String.valueOf(node.getContentDescription()));}finally{node.recycle();}
    }
    static void assertGeometry(DemoTaskPreviewActivity a,View target){
        assertTrue("Thumbnail action width >=48dp, actual="+target.getWidth(),target.getWidth()>=Ui.dp(a,48));assertTrue("Thumbnail action height >=48dp, actual="+target.getHeight(),target.getHeight()>=Ui.dp(a,48));assertVisible(a,target);
        ImageView image=picture(target);assertNotNull(image);assertEquals(ImageView.ScaleType.FIT_CENTER,image.getScaleType());assertTrue(image.getAdjustViewBounds());
        float[] matrix=new float[9];image.getImageMatrix().getValues(matrix);assertEquals("Uniform scale preserves the bitmap aspect",matrix[Matrix.MSCALE_X],matrix[Matrix.MSCALE_Y],.0001f);
        RectF bitmap=new RectF(0,0,image.getDrawable().getIntrinsicWidth(),image.getDrawable().getIntrinsicHeight());image.getImageMatrix().mapRect(bitmap);
        assertTrue(bitmap.left>=-1f&&bitmap.top>=-1f&&bitmap.right<=image.getWidth()-image.getPaddingLeft()-image.getPaddingRight()+1f&&bitmap.bottom<=image.getHeight()-image.getPaddingTop()-image.getPaddingBottom()+1f);
        if(a.thumbnail.getHeight()==400)assertTrue("Ordinary screenshots keep their natural larger height",target.getHeight()>Ui.dp(a,48));
    }
    static void assertVisible(DemoTaskPreviewActivity a,View target){
        Rect local=new Rect(),global=new Rect();Point origin=new Point();assertTrue(target.getLocalVisibleRect(local));assertEquals(new Rect(0,0,target.getWidth(),target.getHeight()),local);assertTrue(target.getGlobalVisibleRect(global,origin));global.offset(-origin.x,-origin.y);assertEquals(local,global);
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();a.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);Rect safe=new Rect(0,0,metrics.widthPixels,metrics.heightPixels),bounds=screenBounds(target);View scroll=(View)a.body.getParent().getParent();Rect viewport=screenBounds(scroll);viewport.left+=scroll.getPaddingLeft();viewport.top+=scroll.getPaddingTop();viewport.right-=scroll.getPaddingRight();viewport.bottom-=scroll.getPaddingBottom();assertTrue(safe.intersect(viewport));assertTrue(safe.intersect(bounds));assertEquals(bounds,safe);
    }
    static void assertSafe(DemoTaskPreviewActivity a){assertEquals(0,a.forbiddenActions.get());assertEquals("",a.store.relay);assertEquals("",a.store.instanceId);assertEquals(ShareImport.UNPAIRED,a.store.scope);assertNotNull(a.thumbnail);assertFalse(a.thumbnail.isRecycled());assertTrue(a.thumbnailRequested);}
    static void reveal(ActivityScenario<DemoTaskPreviewActivity> scenario)throws Exception{scenario.onActivity(a->{View target=target(a);target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);});frames(scenario);scenario.onActivity(a->assertVisible(a,target(a)));}
    static void capture(ActivityScenario<DemoTaskPreviewActivity> scenario,String language,String theme,String shape,String state)throws Exception{
        String phase=InstrumentationRegistry.getArguments().getString("captureThumbnailUi","");if(phase.isEmpty())return;assertTrue(phase.equals("red")||phase.equals("minimum")||phase.equals("green"));
        JSONObject[] metadata={null};ViewGroup[] decor={null};android.widget.FrameLayout[] marker={null};
        try{
            scenario.onActivity(a->{try{
                View target=target(a);ImageView image=picture(target);assertVisible(a,target);assertSafe(a);
                metadata[0]=new JSONObject().put("language",language).put("theme",theme).put("shape",shape).put("state",state).put("bitmap_width",a.thumbnail.getWidth()).put("bitmap_height",a.thumbnail.getHeight()).put("image_width",image.getWidth()).put("image_height",image.getHeight()).put("target_width",target.getWidth()).put("target_height",target.getHeight()).put("target_bounds",screenBounds(target).toShortString()).put("image_bounds",screenBounds(image).toShortString()).put("enabled",target.isEnabled()).put("focusable",target.isFocusable()).put("forbidden_actions",a.forbiddenActions.get()).put("memory_navigation_count",a.thumbnailOpens);
                TextView label=new TextView(a);label.setText(language.equals("zh")?"UI 探针 · 内存图片 · 未执行任务":"UI probe · memory bitmap · no task");label.setTextSize(9);label.setSingleLine(true);label.setGravity(android.view.Gravity.CENTER);label.setTextColor(0xFFFFFFFF);label.setBackgroundColor(0xFF17251D);
                marker[0]=new android.widget.FrameLayout(a);marker[0].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);marker[0].addView(label,new android.widget.FrameLayout.LayoutParams(-1,-1));decor[0]=(ViewGroup)a.getWindow().getDecorView();int left=Ui.dp(a,20),width=decor[0].getWidth()-2*left,height=Ui.dp(a,14),top=Ui.dp(a,28);marker[0].measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));marker[0].layout(left,top,left+width,top+height);decor[0].getOverlay().add(marker[0]);assertFalse(Rect.intersects(screenBounds(marker[0]),screenBounds(target)));
            }catch(Exception error){throw new AssertionError(error);}});
            frames(scenario);InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300,3000);Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File base=context.getExternalFilesDir(null);assertNotNull(base);File directory=new File(base,"ui-probe-evidence/task-thumbnail-"+phase+"-20261006");assertTrue(directory.isDirectory()||directory.mkdirs());String name="thumbnail-"+language+"-"+theme+"-"+shape+"-"+state;File png=new File(directory,name+".png"),json=new File(directory,name+".json");assertFalse(png.exists());assertFalse(json.exists());
            Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);try{assertEquals(320,screenshot.getWidth());assertEquals(640,screenshot.getHeight());try(FileOutputStream out=new FileOutputStream(png)){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}try(FileOutputStream out=new FileOutputStream(json)){out.write(metadata[0].toString(2).getBytes(StandardCharsets.UTF_8));}}finally{screenshot.recycle();}
        }finally{scenario.onActivity(a->{if(decor[0]!=null&&marker[0]!=null)decor[0].getOverlay().remove(marker[0]);});}
    }
}
