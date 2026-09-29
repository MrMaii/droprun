package app.droprun;

import android.app.AlertDialog;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

/** Runs real Views, Keystore and outbox files, with synthetic data and no live Relay. */
public class LocalRecoveryTest {
    Context context;

    @Before public void seed() throws Exception {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Never run fixtures in the public package",context.getPackageName().endsWith(".debug"));
        String previousRelay=new Store(context).relay();
        assertTrue("Do not replace a developer's existing connection",previousRelay.isEmpty()||previousRelay.equals("https://preview.example.invalid"));
        stopSync();
        DemoFixture.seed(context,new Intent());
        Store store=new Store(context);
        assertEquals("https://preview.example.invalid",store.relay());
        for(JSONArray pending=store.pending();pending.length()>0;pending=store.pending())store.cancelPending(pending.getJSONObject(0).getString("id"));
    }

    @After public void cleanup() throws Exception {
        stopSync();
        Store store=new Store(context);
        assertEquals("https://preview.example.invalid",store.relay());
        JSONArray pending=store.pending();
        for(int n=0;n<pending.length();n++)store.cancelPending(pending.getJSONObject(n).getString("id"));
    }

    void stopSync(){context.stopService(new Intent(context,TaskSyncService.class));((JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();}
    void testRotation(int rotation,int orientation)throws Exception{
        assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(rotation));long deadline=android.os.SystemClock.elapsedRealtime()+5000;
        while(context.getResources().getConfiguration().orientation!=orientation&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(25);
        assertEquals("Wait for the requested test orientation before launching a screen",orientation,context.getResources().getConfiguration().orientation);
    }

    void seedHomeList(ActivityScenario<DemoHomeActivity> scenario){
        stopSync();scenario.onActivity(activity->{try{
            JSONArray projects=new JSONArray();long now=System.currentTimeMillis();
            for(int n=0;n<80;n++)projects.put(new JSONObject().put("id","local-project-"+n).put("name","Local project "+n).put("task_count",3).put("dispatch_count",5).put("last_status","completed").put("last_dispatch_at",now-n*60000));
            activity.store.prefs.edit().putString("activity",new JSONObject().put("projects",projects).toString()).commit();activity.show();
        }catch(Exception error){throw new AssertionError(error);}});
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity->activity.list.setSelectionFromTop(40,-17));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity->assertEquals(40,activity.list.getFirstVisiblePosition()));
    }
    String[] homePosition(ActivityScenario<DemoHomeActivity> scenario){
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();String[] position=new String[2];
        scenario.onActivity(activity->{int first=activity.list.getFirstVisiblePosition();assertNotNull(activity.list.getChildAt(0));position[0]=activity.items.get(first).optString("id");position[1]=String.valueOf(activity.list.getChildAt(0).getTop());});return position;
    }
    @Test public void homeRefreshAndPendingChangesKeepReadingPosition()throws Exception{
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            seedHomeList(scenario);String[] before=homePosition(scenario);
            scenario.onActivity(activity->{try{JSONArray projects=activity.store.activity();projects.getJSONObject(40).put("active_count",1);activity.store.prefs.edit().putString("activity",new JSONObject().put("projects",projects).toString()).commit();activity.show();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals("Status refresh must retain the visible project and offset",before,homePosition(scenario));
            String id=UUID.randomUUID().toString();scenario.onActivity(activity->{try{activity.store.save(new JSONObject().put("id",id).put("projectId","local-new-project").put("projectName","Local pending project").put("content","Synthetic UI material"));activity.show();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals("A new pending project must not move the reader",before,homePosition(scenario));scenario.onActivity(activity->assertEquals(81,activity.items.size()));
            scenario.onActivity(activity->{try{activity.store.cancelPending(id);activity.show();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals(before,homePosition(scenario));scenario.onActivity(activity->{assertEquals(80,activity.items.size());assertEquals(1,activity.items.get(activity.list.getFirstVisiblePosition()).optInt("active_count"));});
        }
    }
    @Test public void homeRecreationRestoresProjectAndOffset(){
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            boolean[] wasTouch={false};scenario.onActivity(activity->wasTouch[0]=activity.list.isInTouchMode());
            try{for(boolean touch:new boolean[]{false,true}){
                InstrumentationRegistry.getInstrumentation().setInTouchMode(touch);seedHomeList(scenario);String[] before=homePosition(scenario);scenario.recreate();stopSync();
                assertArrayEquals("Restore home reading position in touch mode="+touch,before,homePosition(scenario));scenario.onActivity(activity->assertEquals(80,activity.items.size()));
            }}finally{InstrumentationRegistry.getInstrumentation().setInTouchMode(wasTouch[0]);}
        }
    }
    @Test public void homeAppearanceChangeOnReturnRetainsReadingPosition()throws Exception{
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            seedHomeList(scenario);String[] before=homePosition(scenario);DemoHomeActivity[] previous={null};scenario.onActivity(activity->previous[0]=activity);
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            scenario.onActivity(activity->assertSame(previous[0],activity));assertArrayEquals("Ordinary return must retain the project",before,homePosition(scenario));
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            new Store(context).preferences.edit().putString("appearance","dark").commit();
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);InstrumentationRegistry.getInstrumentation().waitForIdleSync();stopSync();
            scenario.onActivity(activity->{assertNotSame("Returning after an appearance change must rebuild the themed page",previous[0],activity);assertEquals("dark",activity.store.preferences.getString("appearance",""));});
            assertArrayEquals("Appearance recreation must keep the current project",before,homePosition(scenario));captureUi("home-dark-restored-local");
        }
    }
    @Test public void homeSettingsButtonsReturnToTheSameProject()throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor settings=instrumentation.addMonitor(SettingsActivity.class.getName(),null,false);
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            seedHomeList(scenario);String[] before=homePosition(scenario);
            scenario.onActivity(activity->{focusDescription(activity,"Settings");assertTrue(activity.getCurrentFocus().performClick());});
            SettingsActivity opened=(SettingsActivity)settings.waitForActivityWithTimeout(5000);assertNotNull("Settings button must open the real settings Activity",opened);
            awaitSettingsWindow(opened);
            instrumentation.runOnMainSync(()->{focusDescription(opened,"Appearance, Light");opened.getCurrentFocus().requestRectangleOnScreen(new android.graphics.Rect(0,0,1,opened.getCurrentFocus().getHeight()),true);});
            instrumentation.waitForIdleSync();instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);captureUi("settings-appearance-local");instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);
            SettingsActivity[] changed={null};long deadline=android.os.SystemClock.elapsedRealtime()+5000;
            do{instrumentation.runOnMainSync(()->{for(android.app.Activity activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(activity instanceof SettingsActivity&&activity!=opened)changed[0]=(SettingsActivity)activity;});if(changed[0]!=null)break;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<deadline);
            assertNotNull("Appearance choice must resume a newly themed Settings Activity",changed[0]);
            awaitSettingsWindow(changed[0]);
            instrumentation.runOnMainSync(()->{assertEquals("dark",changed[0].store.preferences.getString("appearance",""));focusDescription(changed[0],"Back");assertTrue(changed[0].getCurrentFocus().performClick());});
            instrumentation.waitForIdleSync();stopSync();assertArrayEquals("Settings controls must return to the same project and offset",before,homePosition(scenario));
        }finally{
            instrumentation.runOnMainSync(()->{for(android.app.Activity activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(activity instanceof SettingsActivity)activity.finish();});
            instrumentation.removeMonitor(settings);
        }
    }

    void seedPagedHistory()throws Exception{
        JSONArray rows=new JSONArray();long now=System.currentTimeMillis();
        for(int n=0;n<240;n++)rows.put(new JSONObject().put("id",UUID.nameUUIDFromBytes(("local-history-"+n).getBytes(StandardCharsets.UTF_8)).toString()).put("project_id","demo-studio").put("title","Local history item "+n).put("status","completed").put("created_at",now-n*60000).put("updated_at",1));
        new Store(context).prefs.edit().putString("history:demo-studio",new JSONObject().put("tasks",rows).put("nextCursor","synthetic-earlier-page").toString()).commit();
    }
    ActivityScenario<ProjectHistoryActivity> historyScenario(){return ActivityScenario.launch(new Intent(context,ProjectHistoryActivity.class).putExtra("projectId","demo-studio").putExtra("projectName","Local history fixture"));}
    String[] historyPosition(ActivityScenario<ProjectHistoryActivity> scenario){
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();String[] position=new String[2];
        scenario.onActivity(activity->{int first=activity.list.getFirstVisiblePosition();assertNotNull(activity.list.getChildAt(0));position[0]=activity.rows.get(first).optString("id");position[1]=String.valueOf(activity.list.getChildAt(0).getTop());});return position;
    }
    void scrollHistory(ActivityScenario<ProjectHistoryActivity> scenario){scenario.onActivity(activity->activity.list.setSelectionFromTop(170,-17));InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(activity->assertEquals(170,activity.list.getFirstVisiblePosition()));}
    @Test public void historyStatusRefreshKeepsOlderPagesAndReadingPosition()throws Exception{
        seedPagedHistory();try(ActivityScenario<ProjectHistoryActivity> scenario=historyScenario()){
            scrollHistory(scenario);String[] before=historyPosition(scenario);
            scenario.onActivity(activity->{try{JSONObject updated=new JSONObject(activity.rows.get(activity.list.getFirstVisiblePosition()).toString()).put("status","running").put("updated_at",2);activity.store.prefs.edit().putString("tasks",new JSONObject().put("tasks",new JSONArray().put(updated)).toString()).commit();activity.refresh.run();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals("Status refresh must keep the visible task and pixel offset",before,historyPosition(scenario));
            scenario.onActivity(activity->{assertEquals(240,activity.rows.size());assertEquals("synthetic-earlier-page",activity.cursor);assertEquals("running",activity.rows.get(activity.list.getFirstVisiblePosition()).optString("status"));assertEquals(1f,activity.list.getChildAt(0).getAlpha(),0f);assertEquals(0f,activity.list.getChildAt(0).getTranslationY(),0f);});captureUi("history-status-local");
        }
    }
    @Test public void historyPendingChangesAboveViewportDoNotMoveReadingPosition()throws Exception{
        seedPagedHistory();try(ActivityScenario<ProjectHistoryActivity> scenario=historyScenario()){
            scrollHistory(scenario);String[] before=historyPosition(scenario);String id=UUID.randomUUID().toString();
            scenario.onActivity(activity->{try{activity.store.save(new JSONObject().put("id",id).put("projectId","demo-studio").put("content","Synthetic pending history item"));activity.refresh.run();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals("A new pending item above the viewport must not move the reader",before,historyPosition(scenario));
            scenario.onActivity(activity->{try{assertEquals(241,activity.rows.size());activity.store.cancelPending(id);activity.refresh.run();}catch(Exception error){throw new AssertionError(error);}});
            assertArrayEquals("Removing the pending item must not move the reader",before,historyPosition(scenario));
        }
    }
    @Test public void historyRecreationRestoresOlderPageAndOffset()throws Exception{
        seedPagedHistory();try(ActivityScenario<ProjectHistoryActivity> scenario=historyScenario()){
            boolean[] wasTouch={false};scenario.onActivity(activity->wasTouch[0]=activity.list.isInTouchMode());
            try{for(boolean touch:new boolean[]{false,true}){
                InstrumentationRegistry.getInstrumentation().setInTouchMode(touch);
                scrollHistory(scenario);String[] before=historyPosition(scenario);scenario.recreate();
                assertArrayEquals("Restore task and offset in touch mode="+touch,before,historyPosition(scenario));scenario.onActivity(activity->{assertEquals(240,activity.rows.size());assertTrue(activity.paged);assertEquals("synthetic-earlier-page",activity.cursor);});
            }}finally{InstrumentationRegistry.getInstrumentation().setInTouchMode(wasTouch[0]);}
        }
    }

    void awaitDelivery(ActivityScenario<DemoDeliverablesActivity> scenario,String text)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+4000;boolean[] found={false};
        do{scenario.onActivity(activity->found[0]=findText(activity.page,text)!=null&&activity.page.getAlpha()==1f&&activity.page.getTranslationY()==0f);if(found[0])return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<deadline);
        fail("Delivery UI did not show: "+text);
    }
    void openLocalDelivery(ActivityScenario<DemoDeliverablesActivity> scenario)throws Exception{
        awaitDelivery(scenario,"local-evidence.txt");scenario.onActivity(activity->activity.download(DemoDeliverablesActivity.item()));awaitDelivery(scenario,DemoDeliverablesActivity.CONTENT);
    }
    Intent exportDestination(){return new Intent().setData(android.net.Uri.parse("content://"+context.getPackageName()+".export-fixture/local-output"));}
    @Test public void deliveryPartialWriteIsFailureAndAnExplicitRetryReplacesIt()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);DemoExportProvider.fail=false;DemoExportProvider.partial=true;
        DemoDeliverablesActivity.exportBytes=new byte[2*1024*1024];java.util.Arrays.fill(DemoDeliverablesActivity.exportBytes,(byte)'x');
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            awaitDelivery(scenario,"local-evidence.txt");scenario.onActivity(activity->activity.download(DemoDeliverablesActivity.item()));awaitDelivery(scenario,"Save to phone");
            scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());});awaitDelivery(scenario,"Save failed. Your preview is still available.");
            assertTrue(DemoExportProvider.file(context).length()>0);assertTrue(DemoExportProvider.file(context).length()<DemoDeliverablesActivity.content().length);
            scenario.onActivity(activity->{assertNull(findText(activity.page,"Saved to your chosen location."));assertTrue(findText(activity.page,"Save to phone").isEnabled());assertEquals(DemoDeliverablesActivity.content().length,activity.currentFile.length());});captureUi("export-partial-write-local");
            DemoExportProvider.partial=false;scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());});awaitDelivery(scenario,"Saved to your chosen location.");
            Store.verifyDeliverable(DemoExportProvider.file(context),DemoDeliverablesActivity.item());assertEquals(2,DemoExportProvider.opens.get());
        }finally{DemoExportProvider.partial=false;DemoExportProvider.file(context).delete();DemoDeliverablesActivity.reset();}
    }
    @Test public void deliverySystemPickerCancellationReturnsToPreview()throws Exception{
        DemoDeliverablesActivity.reset();DemoDeliverablesActivity.systemPicker=true;
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(DemoDeliverablesActivity::saveFile);awaitSystemPicker();captureUi("system-picker-local");
            android.view.accessibility.AccessibilityNodeInfo root=InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();StringBuilder tree=new StringBuilder();pickerTree(root,tree);Files.write(new File(context.getExternalFilesDir(null),"system-picker-tree.txt").toPath(),tree.toString().getBytes(StandardCharsets.UTF_8));
            assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));
            captureUi("system-picker-first-back-local");
            root=InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if(root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().contains("documentsui"))assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));
            awaitDelivery(scenario,"Save cancelled. Your preview is still available.");scenario.onActivity(activity->{assertTrue(activity.currentFile.isFile());assertTrue(findText(activity.page,"Save to phone").isEnabled());});assertNull(DemoDeliverablesActivity.savedDestination);
        }finally{DemoDeliverablesActivity.reset();}
    }
    void awaitSystemPicker()throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+6000;
        do{android.view.accessibility.AccessibilityNodeInfo root=InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().contains("documentsui"))return;Thread.sleep(50);}while(android.os.SystemClock.elapsedRealtime()<deadline);
        fail("System document picker did not appear");
    }
    void pickerTree(android.view.accessibility.AccessibilityNodeInfo node,StringBuilder tree){
        if(node==null)return;tree.append(node.getViewIdResourceName()).append(" | ").append(node.getText()).append(" | ").append(node.getContentDescription()).append(" | clickable=").append(node.isClickable()).append('\n');for(int n=0;n<node.getChildCount();n++)pickerTree(node.getChild(n),tree);
    }
    android.view.accessibility.AccessibilityNodeInfo pickerFilename(android.view.accessibility.AccessibilityNodeInfo node){
        if(node==null)return null;if(node.isEditable()&&"android:id/title".equals(node.getViewIdResourceName()))return node;
        for(int n=0;n<node.getChildCount();n++){android.view.accessibility.AccessibilityNodeInfo found=pickerFilename(node.getChild(n));if(found!=null)return found;}return null;
    }
    void awaitPickerFilename(String expected)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+4000;String actual="";
        do{android.view.accessibility.AccessibilityNodeInfo input=pickerFilename(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow());if(input!=null){input.refresh();actual=String.valueOf(input.getText());if(expected.equals(actual))return;}Thread.sleep(50);}while(android.os.SystemClock.elapsedRealtime()<deadline);
        assertEquals("System picker filename",expected,actual);
    }
    @Test public void deliverySystemPickerSavesExactBytesAfterRotation()throws Exception{
        DemoDeliverablesActivity.reset();DemoDeliverablesActivity.systemPicker=true;
        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(DemoDeliverablesActivity::saveFile);awaitSystemPicker();
            String name="droprun-local-"+UUID.randomUUID()+".txt";
            android.view.accessibility.AccessibilityNodeInfo input=pickerFilename(automation.getRootInActiveWindow());assertNotNull(input);
            android.os.Bundle text=new android.os.Bundle();text.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name);assertTrue(input.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,text));
            awaitPickerFilename(name);captureUi("system-picker-named-local");
            int before=DemoDeliverablesActivity.creations.get();assertTrue(automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90));
            long deadline=android.os.SystemClock.elapsedRealtime()+5000;android.graphics.Rect bounds=new android.graphics.Rect();
            do{android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();if(root!=null)root.getBoundsInScreen(bounds);if(bounds.width()>bounds.height()&&pickerFilename(root)!=null)break;Thread.sleep(50);}while(android.os.SystemClock.elapsedRealtime()<deadline);
            assertTrue("The system picker must actually rotate",bounds.width()>bounds.height());awaitSystemPicker();
            captureUi("system-picker-rotated-local");input=pickerFilename(automation.getRootInActiveWindow());assertNotNull(input);String rotatedName=String.valueOf(input.getText());
            assertTrue("Picker must retain either the edited or originally suggested name",name.equals(rotatedName)||DemoDeliverablesActivity.item().getString("name").equals(rotatedName));
            // Some system picker versions reset edited names on rotation. Record, then explicitly confirm the name again.
            Files.write(new File(context.getExternalFilesDir(null),"system-picker-rotation.txt").toPath(),("editedFilenamePreserved="+name.equals(rotatedName)+"\n").getBytes(StandardCharsets.UTF_8));
            assertTrue(input.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,text));awaitPickerFilename(name);clickWindowText("SAVE");
            awaitDelivery(scenario,"Saved to your chosen location.");assertTrue("The hidden preview recreates when it returns from the rotated picker",DemoDeliverablesActivity.creations.get()>before);assertNotNull(DemoDeliverablesActivity.savedDestination);
            assertFalse(DemoDeliverablesActivity.savedDestination.getAuthority().endsWith("export-fixture"));
            try(java.io.InputStream bytes=context.getContentResolver().openInputStream(DemoDeliverablesActivity.savedDestination);java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[256];int n;while((n=bytes.read(buffer))!=-1)output.write(buffer,0,n);assertEquals(DemoDeliverablesActivity.CONTENT,new String(output.toByteArray(),StandardCharsets.UTF_8));}
            captureUi("system-picker-saved-local");assertTrue(android.provider.DocumentsContract.deleteDocument(context.getContentResolver(),DemoDeliverablesActivity.savedDestination));DemoDeliverablesActivity.savedDestination=null;
        }finally{if(DemoDeliverablesActivity.savedDestination!=null)android.provider.DocumentsContract.deleteDocument(context.getContentResolver(),DemoDeliverablesActivity.savedDestination);automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0);DemoDeliverablesActivity.reset();}
    }
    @Test public void deliveryPickerLocksUntilCancelAndRecoversWhenUnavailable()throws Exception{
        DemoDeliverablesActivity.reset();
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(activity->{activity.saveFile();activity.saveFile();assertEquals(1,DemoDeliverablesActivity.pickers.get());assertFalse(findText(activity.page,"Choosing location…").isEnabled());});
            scenario.recreate();awaitDelivery(scenario,"Choosing location…");
            scenario.onActivity(activity->{activity.onActivityResult(20,android.app.Activity.RESULT_CANCELED,null);assertTrue(findText(activity.page,"Save to phone").isEnabled());assertTrue(activity.currentFile.isFile());});
            DemoDeliverablesActivity.failPicker=true;scenario.onActivity(DemoDeliverablesActivity::saveFile);awaitDelivery(scenario,"Could not open save locations.");
            scenario.onActivity(activity->assertTrue(findText(activity.page,"Save to phone").isEnabled()));
        }finally{DemoDeliverablesActivity.reset();}
    }
    @Test public void deliveryLostSaveStateExplainsUncertaintyWithoutWritingAgain()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);
        File cache=File.createTempFile("droprun-delivery-",".bin",new Store(context).cacheDir());Files.write(cache.toPath(),DemoDeliverablesActivity.CONTENT.getBytes(StandardCharsets.UTF_8));
        android.os.Bundle state=new android.os.Bundle();state.putString("file",cache.getPath());state.putString("item",DemoDeliverablesActivity.item().toString());state.putBoolean("savePending",true);
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(new Intent(context,DemoDeliverablesActivity.class).putExtra("fixtureRestore",state))){
            awaitDelivery(scenario,"Save status is unknown.");assertEquals(0,DemoExportProvider.opens.get());
            scenario.recreate();awaitDelivery(scenario,"Save status is unknown.");assertEquals(0,DemoExportProvider.opens.get());
            captureUi("export-interrupted-local");
        }finally{cache.delete();DemoDeliverablesActivity.reset();}
    }
    @Test public void deliveryRevokedFileCannotOpenTheDestination()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);DemoExportProvider.fail=false;
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);DemoDeliverablesActivity.empty=true;
            scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());});awaitDelivery(scenario,"Save failed. Your preview is still available.");
            assertEquals(0,DemoExportProvider.opens.get());
        }finally{DemoDeliverablesActivity.reset();}
    }
    @Test public void deliverySaveShowsProgressAndSurvivesRecreationWithoutDuplicateWrite()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);DemoExportProvider.fail=false;
        java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);DemoDeliverablesActivity.listGate=gate;
            scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());assertNotNull(findText(activity.page,"Saving file…"));assertFalse(findText(activity.page,"Saving file…").isEnabled());});
            scenario.recreate();awaitDelivery(scenario,"Saving file…");
            scenario.onActivity(activity->activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination()));
            captureUi("export-pending-local");
            gate.countDown();awaitDelivery(scenario,"Saved to your chosen location.");
            assertEquals("Recreation and duplicate callbacks must not repeat the write",1,DemoExportProvider.opens.get());
            assertEquals(DemoDeliverablesActivity.CONTENT,new String(Files.readAllBytes(DemoExportProvider.file(context).toPath()),StandardCharsets.UTF_8));
            captureUi("export-success-local");
        }finally{gate.countDown();DemoDeliverablesActivity.reset();DemoExportProvider.file(context).delete();}
    }
    @Test public void deliverySaveFailureRetainsPreviewAndAllowsExplicitRetry()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);DemoExportProvider.fail=true;
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());});
            awaitDelivery(scenario,"Save failed. Your preview is still available.");
            scenario.onActivity(activity->{assertTrue(activity.currentFile.isFile());assertTrue(findText(activity.page,"Save to phone").isEnabled());});
            captureUi("export-failure-local");DemoExportProvider.fail=false;
            scenario.onActivity(activity->{activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());});awaitDelivery(scenario,"Saved to your chosen location.");assertEquals(2,DemoExportProvider.opens.get());
        }finally{DemoDeliverablesActivity.reset();DemoExportProvider.fail=false;DemoExportProvider.file(context).delete();}
    }
    @Test public void deliveryLeavingDuringSaveConfirmsAndRetainsBytesUntilFinished()throws Exception{
        DemoDeliverablesActivity.reset();DemoExportProvider.opens.set(0);DemoExportProvider.fail=false;
        java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);String[] cache={null};
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);DemoDeliverablesActivity.listGate=gate;
            scenario.onActivity(activity->{cache[0]=activity.currentFile.getPath();activity.saveFile();activity.onActivityResult(20,android.app.Activity.RESULT_OK,exportDestination());activity.onBackPressed();});
            clickWindowText("Stay here");scenario.onActivity(activity->{assertFalse(activity.isFinishing());assertFalse(findText(activity.page,"Saving file…").isEnabled());activity.onBackPressed();});
            captureUi("export-leave-local");clickWindowText("Leave preview");
            assertTrue("Leaving must not remove bytes needed by the in-progress save",new File(cache[0]).isFile());
            gate.countDown();long deadline=android.os.SystemClock.elapsedRealtime()+4000;while(new File(cache[0]).exists()&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(25);
            assertFalse("Source is cleaned up after the detached save completes",new File(cache[0]).exists());assertEquals(1,DemoExportProvider.opens.get());
            assertEquals(DemoDeliverablesActivity.CONTENT,new String(Files.readAllBytes(DemoExportProvider.file(context).toPath()),StandardCharsets.UTF_8));
        }finally{gate.countDown();DemoDeliverablesActivity.reset();DemoExportProvider.file(context).delete();}
    }
    @Test public void deliveryLoadingAndFailedListRemainRecoverable()throws Exception{
        DemoDeliverablesActivity.reset();DemoDeliverablesActivity.failFirst=true;java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);DemoDeliverablesActivity.listGate=gate;
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            scenario.onActivity(activity->{assertNotNull(findText(activity.page,"Loading delivery files"));assertNotNull(findText(activity.page,"Back to report"));assertNull(findText(activity.page,"Save to phone"));});
            gate.countDown();awaitDelivery(scenario,"Synthetic delivery list unavailable");
            scenario.onActivity(activity->findText(activity.page,"Try again").performClick());awaitDelivery(scenario,"local-evidence.txt");
            scenario.onActivity(activity->{assertNull(findText(activity.page,"Synthetic delivery list unavailable"));assertNull(activity.currentFile);});
            assertEquals(2,DemoDeliverablesActivity.reads.get());
        }finally{gate.countDown();DemoDeliverablesActivity.reset();}
    }
    @Test public void deliveryEmptyListDoesNotOfferASave()throws Exception{
        DemoDeliverablesActivity.reset();DemoDeliverablesActivity.empty=true;
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            awaitDelivery(scenario,"No delivery files yet");scenario.onActivity(activity->{assertNotNull(findText(activity.page,"Back to report"));assertNull(findText(activity.page,"Save to phone"));});
        }finally{DemoDeliverablesActivity.reset();}
    }
    @Test public void deliveryPreviewAndCancelledSaveSurviveRecreation()throws Exception{
        DemoDeliverablesActivity.reset();String[] cached={null};
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(activity->{cached[0]=activity.currentFile.getPath();activity.onActivityResult(20,android.app.Activity.RESULT_CANCELED,null);assertNotNull(findText(activity.page,"Save to phone"));assertTrue(activity.currentFile.isFile());});
            scenario.recreate();awaitDelivery(scenario,DemoDeliverablesActivity.CONTENT);
            scenario.onActivity(activity->{assertEquals(cached[0],activity.currentFile.getPath());assertNotNull(findText(activity.page,"Save to phone"));});assertEquals(1,DemoDeliverablesActivity.reads.get());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{android.widget.TextView save=findText(activity.page,"Save to phone");save.requestRectangleOnScreen(new android.graphics.Rect(0,0,save.getWidth(),save.getHeight()),true);});
            captureUi("delivery-restored-local");
            scenario.onActivity(activity->{android.widget.TextView save=findText(activity.page,"Save to phone");android.graphics.Rect visible=new android.graphics.Rect();assertTrue(save.getGlobalVisibleRect(visible));assertEquals("Save must be fully reachable by scrolling",save.getHeight(),visible.height());});
        }finally{DemoDeliverablesActivity.reset();}
        assertFalse("Finishing removes this temporary preview",new File(cached[0]).exists());
    }
    @Test public void deliveryRestorationDoesNotCallChangedCacheVerified()throws Exception{
        DemoDeliverablesActivity.reset();String[] cached={null};
        try(ActivityScenario<DemoDeliverablesActivity> scenario=ActivityScenario.launch(DemoDeliverablesActivity.class)){
            openLocalDelivery(scenario);scenario.onActivity(activity->{cached[0]=activity.currentFile.getPath();try{byte[] changed=Files.readAllBytes(activity.currentFile.toPath());changed[0]='X';Files.write(activity.currentFile.toPath(),changed);}catch(Exception error){throw new AssertionError(error);}});
            scenario.recreate();awaitDelivery(scenario,"local-evidence.txt");
            scenario.onActivity(activity->{assertNull("Corrupt cache must not retain a verified preview",activity.currentFile);assertNull(findText(activity.page,"Save to phone"));});
            assertFalse(new File(cached[0]).exists());
            captureUi("delivery-corrupt-cache-local");clickWindowText("Got it");
        }finally{DemoDeliverablesActivity.reset();}
    }

    android.widget.ScrollView settingsScroll(SettingsActivity activity){return (android.widget.ScrollView)activity.body.getParent().getParent();}
    @Test public void settingsModelAndEffortChoicesRetainFocusAndCancelKeepsValues()throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            DemoSettingsActivity[] opened={null};scenario.onActivity(activity->{opened[0]=activity;try{
                JSONObject data=activity.store.projectsData();data.getJSONArray("models").put(new JSONObject().put("id","local-second-model").put("displayName","Local second model").put("defaultEffort","low").put("efforts",new JSONArray().put("low").put("high")));
                activity.store.prefs.edit().putString("projects",data.toString()).commit();activity.store.saveDefaults("example-model","medium");activity.render();
            }catch(Exception error){throw new AssertionError(error);}});awaitSettingsWindow(opened[0]);instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            for(String[] choice:new String[][]{{"Model · Computer default","Model · Local second model"},{"Effort · low","Effort · high"}}){
                scenario.onActivity(activity->assertTrue(findText(activity.body,choice[0]).requestFocus()));instrumentation.waitForIdleSync();
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);captureUi("settings-model-choice-local");instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);captureUi("settings-model-result-local");
                scenario.onActivity(activity->{android.widget.TextView selected=findText(activity.body,choice[1]);assertNotNull(selected);assertSame("The changed model/effort control must retain keyboard focus",selected,activity.getCurrentFocus());android.graphics.Rect visible=new android.graphics.Rect();assertTrue(selected.getGlobalVisibleRect(visible));assertTrue("The full selected control must be visible",visible.height()>=selected.getHeight());assertTrue("Model choices must not return to the page header",settingsScroll(activity).getScrollY()>0);});
            }
            int[] scroll={0};scenario.onActivity(activity->{assertEquals("local-second-model",activity.store.defaultModel());assertEquals("high",activity.store.defaultEffort(activity.store.defaultModel()));scroll[0]=settingsScroll(activity).getScrollY();});
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);captureUi("settings-effort-cancel-local");clickWindowText("Cancel");instrumentation.waitForIdleSync();
            scenario.onActivity(activity->{assertEquals("local-second-model",activity.store.defaultModel());assertEquals("high",activity.store.defaultEffort(activity.store.defaultModel()));assertSame(findText(activity.body,"Effort · high"),activity.getCurrentFocus());assertEquals(scroll[0],settingsScroll(activity).getScrollY());});
            scenario.recreate();captureUi("settings-model-recreated-local");scenario.onActivity(activity->{assertEquals("local-second-model",activity.store.defaultModel());assertEquals("high",activity.store.defaultEffort(activity.store.defaultModel()));assertSame(findText(activity.body,"Effort · high"),activity.getCurrentFocus());});
        }
    }
    void awaitSettingsWindow(SettingsActivity activity)throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();long deadline=android.os.SystemClock.elapsedRealtime()+5000;boolean[] ready={false};
        do{instrumentation.runOnMainSync(()->ready[0]=activity.hasWindowFocus()&&activity.body.getAlpha()==1f&&activity.body.getTranslationY()==0f);if(ready[0])return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<deadline);
        captureUi("settings-window-timeout");String[] detail={""};instrumentation.runOnMainSync(()->detail[0]="focus="+activity.hasWindowFocus()+" alpha="+activity.body.getAlpha()+" translation="+activity.body.getTranslationY()+" destroyed="+activity.isDestroyed());
        fail("Settings did not obtain a settled focused window: "+detail[0]);
    }
    @Test public void settingsRecreationRetainsExpandedAccessAndScroll()throws Exception{
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            scenario.onActivity(activity->{android.view.View header=(android.view.View)findText(activity.body,"3 projects · manage access").getParent();assertTrue(header.performClick());assertTrue(activity.showAccess);});
            captureUi("settings-expanded-local");int[] before={0};scenario.onActivity(activity->{settingsScroll(activity).scrollTo(0,Ui.dp(activity,1000));before[0]=settingsScroll(activity).getScrollY();assertTrue(before[0]>0);});
            scenario.recreate();InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{assertTrue("Project access must remain expanded",activity.showAccess);assertEquals("Recreation must preserve the scroll offset",before[0],settingsScroll(activity).getScrollY());assertTrue(findText(activity.body,"Studio website").isShown());});
        }
    }
    void awaitSettingsRecreation(ActivityScenario<DemoSettingsActivity> scenario,DemoSettingsActivity previous)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+5000;boolean[] changed={false};
        DemoSettingsActivity[] current={null};
        do{scenario.onActivity(activity->{changed[0]=activity!=previous;current[0]=activity;});if(changed[0]){awaitSettingsWindow(current[0]);return;}Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<deadline);
        fail("The settings preference did not recreate its page");
    }
    @Test public void settingsAppearanceAndLanguageKeepTheChosenControlFocused()throws Exception{checkSettingsChoiceFocus(false);}
    @Test public void settingsLandscapeLanguageKeepsTheWholeFocusedControlVisible()throws Exception{
        checkSettingsChoiceFocus(true);
    }
    void checkSettingsChoiceFocus(boolean landscape)throws Exception{
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            try{
            if(landscape)testRotation(android.app.UiAutomation.ROTATION_FREEZE_90,android.content.res.Configuration.ORIENTATION_LANDSCAPE);
            DemoSettingsActivity[] initial={null};scenario.onActivity(activity->initial[0]=activity);awaitSettingsWindow(initial[0]);instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            for(String[] choice:new String[][]{{"Appearance, Light","Appearance, Dark","appearance","dark"},{"Language, English","语言, 简体中文","language","zh"}}){
                DemoSettingsActivity[] previous={null};scenario.onActivity(activity->{previous[0]=activity;focusDescription(activity,choice[0]);});
                instrumentation.waitForIdleSync();instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);captureUi("settings-choice-local");instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN);instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);awaitSettingsRecreation(scenario,previous[0]);captureUi("settings-"+choice[2]+"-restored-local");
                scenario.onActivity(activity->{assertEquals(choice[3],activity.store.preferences.getString(choice[2],""));android.view.View focused=activity.getCurrentFocus();assertNotNull(focused);assertEquals("The changed choice must keep keyboard focus",choice[1],String.valueOf(focused.getContentDescription()));android.graphics.Rect visible=new android.graphics.Rect();assertTrue("The changed choice must stay visible",focused.getGlobalVisibleRect(visible));assertTrue("The complete focused control must fit outside the system bars: "+choice[1]+" visible="+visible+" height="+focused.getHeight(),visible.height()>=focused.getHeight());});
            }
            captureUi("settings-zh-dark-restored-local");
            }finally{if(landscape)testRotation(android.app.UiAutomation.ROTATION_FREEZE_0,android.content.res.Configuration.ORIENTATION_PORTRAIT);}
        }
    }

    @Test public void scrollingSettingsKeepContentInsideSystemBars(){
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                android.view.View column=(android.view.View)activity.body.getParent();
                android.widget.ScrollView scroll=(android.widget.ScrollView)column.getParent();
                scroll.scrollTo(0,Ui.dp(activity,400));
                assertTrue("The scrolling page must reserve the status-bar inset",scroll.getPaddingTop()>0);
                android.graphics.Rect visible=new android.graphics.Rect();assertTrue(column.getGlobalVisibleRect(visible));
                int[] origin=new int[2];scroll.getLocationOnScreen(origin);
                assertTrue("Scrolled content must not overlap the status bar",visible.top>=origin[1]+scroll.getPaddingTop());
                assertTrue("Scrolled content must not overlap navigation",visible.bottom<=origin[1]+scroll.getHeight()-scroll.getPaddingBottom());
            });
        }
    }

    @Test public void retentionShowsUnknownUntilPolicyIsAvailable(){
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            scenario.onActivity(activity->{
                activity.store.prefs.edit().remove("retention").commit();activity.render();
                assertNotNull(findText(activity.body,"Connect and reopen Settings to load your Relay’s retention policy."));
                assertNull(findText(activity.body,"Original uploads · 7 days after a task ends"));
            });
        }
    }

    @Test public void retentionUsesActualPolicyAndCannotCrossInstances(){
        try(ActivityScenario<DemoSettingsActivity> scenario=ActivityScenario.launch(DemoSettingsActivity.class)){
            scenario.onActivity(activity->{
                Store store=activity.store;String relay=store.relay(),instance=store.instanceId;
                try{
                    store.prefs.edit().putString("retention","{\"rawDays\":14,\"artifactDays\":60}").commit();activity.render();
                    assertNotNull(findText(activity.body,"Original uploads · 14 days after a task ends"));
                    assertNotNull(findText(activity.body,"Screenshots & files · 60 days after a task ends"));
                    assertNotNull(findText(activity.body,"Last synced Relay policy"));
                    store.select("https://second.example.invalid","retention-test-"+UUID.randomUUID());activity.render();
                    assertNull(findText(activity.body,"Original uploads · 14 days after a task ends"));
                    assertNotNull(findText(activity.body,"Connect and reopen Settings to load your Relay’s retention policy."));
                }finally{store.select(relay,instance);store.prefs.edit().remove("retention").commit();}
            });
        }
    }

    @Test public void cachedCommandDisappearsWhenItsDeadlinePasses() throws Exception {
        try(ActivityScenario<DemoTaskActivity> scenario=ActivityScenario.launch(DemoTaskActivity.class)){
            scenario.onActivity(activity->{expiryFixture(activity,false);assertNotNull(findText(activity.body,"Allow this command"));});
            Thread.sleep(350);
            scenario.onActivity(activity->{activity.render();assertNull("Expired cached approval must not remain actionable",findText(activity.body,"Allow this command"));});
        }
    }

    @Test public void cachedPreviewChangesToReopenWhenItsDeadlinePasses() throws Exception {
        try(ActivityScenario<DemoTaskActivity> scenario=ActivityScenario.launch(DemoTaskActivity.class)){
            scenario.onActivity(activity->{expiryFixture(activity,true);assertNotNull(findText(activity.body,"Open preview"));});
            Thread.sleep(350);
            scenario.onActivity(activity->{activity.render();assertNull(findText(activity.body,"Open preview"));assertNotNull(findText(activity.body,"Reopen preview"));});
        }
    }

    static void expiryFixture(TaskActivity activity,boolean preview){
        try{
            JSONObject task=new JSONObject(activity.store.task(DemoFixture.TASK).toString());long expires=System.currentTimeMillis()+250;
            if(preview)task.put("preview_url","https://preview.example.invalid/snapshot/example").put("preview_kind","snapshot").put("preview_status","ready").put("preview_expires_at",expires);
            else task.put("status","waiting_for_approval").put("report","").put("approvals",new JSONArray().put(new JSONObject().put("id","local-command").put("expiresAt",expires).put("details",new JSONObject().put("command","echo synthetic approval").put("reason","Local UI test; no command is executed"))));
            cacheTask(activity.store,task);activity.render();
        }catch(Exception error){throw new AssertionError(error);}
    }

    static void cacheTask(Store store,JSONObject task){
        store.prefs.edit().putString("tasks",new JSONObject().toString()).putString("task:"+DemoFixture.TASK,task.toString()).commit();
    }

    @Test public void failedDecisionRemainsVisibleAfterSuccessfulRefresh() throws Exception {
        Store store=new Store(context);JSONObject task=new JSONObject(store.task(DemoFixture.TASK).toString()).put("status","running").put("report","");cacheTask(store,task);
        java.util.concurrent.atomic.AtomicInteger polls=new java.util.concurrent.atomic.AtomicInteger();
        try(ActivityScenario<TaskActivity> scenario=ActivityScenario.launch(new Intent(context,DemoDecisionActivity.class).putExtra("taskId",DemoFixture.TASK))){
            awaitTaskUi(scenario,activity->!activity.loading);
            scenario.onActivity(activity->{
                activity.store=new Store(context){@Override JSONObject refreshTask(String id){polls.incrementAndGet();return task(id);}};
                activity.perform(()->{throw new IOException("Synthetic decision rejected");});
            });
            awaitTaskUi(scenario,activity->polls.get()>0&&!activity.busy&&!activity.loading);
            InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(100,3000);
            android.view.accessibility.AccessibilityNodeInfo root=InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            assertNotNull(root);assertFalse("A refresh must not erase the action error",root.findAccessibilityNodeInfosByText("Synthetic decision rejected").isEmpty());
            captureUi("decision-error");
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        }
    }

    static void awaitTaskUi(ActivityScenario<TaskActivity> scenario,java.util.function.Predicate<TaskActivity> condition)throws InterruptedException {
        boolean[] ready={false};long deadline=android.os.SystemClock.elapsedRealtime()+5000;
        while(android.os.SystemClock.elapsedRealtime()<deadline){scenario.onActivity(activity->ready[0]=condition.test(activity));if(ready[0])return;Thread.sleep(25);}
        fail("Task UI did not reach the expected state");
    }

    @Test public void pendingCopyNeedsConfirmationBeforeLocalRemoval() throws Exception {
        Store store=new Store(context);JSONObject pending=task().put("content","Synthetic UI fixture; no agent work is sent").put("sendError","Phone offline. Retry when connected.");store.save(pending);
        Intent intent=new Intent(context,ProjectHistoryActivity.class).putExtra("projectId","demo-studio").putExtra("projectName","Local UI fixture");
        try(ActivityScenario<ProjectHistoryActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->activity.openSaved(pending));captureUi("pending-details");clickWindowText("Close");
            scenario.onActivity(activity->activity.removeSaved(pending));captureUi("pending-removal");clickWindowText("Keep");
            assertEquals(1,store.pending().length());
            scenario.onActivity(activity->activity.removeSaved(pending));clickWindowText("Remove saved copy");
            long deadline=android.os.SystemClock.elapsedRealtime()+3000;
            while(store.pending().length()>0&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(25);
            assertEquals(0,store.pending().length());
        }
    }

    void captureUi(String name)throws Exception {
        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();automation.waitForIdle(100,3000);
        // Accessibility idle does not include window fade animations; record their settled state.
        Thread.sleep(350);
        android.graphics.Bitmap bitmap=automation.takeScreenshot();assertNotNull(bitmap);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(context.getExternalFilesDir(null),name+".png"))){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }

    static void clickWindowText(String label)throws Exception {
        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();automation.waitForIdle(100,3000);
        long deadline=android.os.SystemClock.elapsedRealtime()+3000;
        do{
            android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();
            if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(label))if(label.contentEquals(node.getText())&&node.isClickable()&&node.isEnabled()&&node.isVisibleToUser()){assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));return;}
            Thread.sleep(25);
        }while(android.os.SystemClock.elapsedRealtime()<deadline);
        fail("No visible action: "+label);
    }

    @Test public void emptyHomeGuidanceIsNotTruncated() {
        Intent intent=new Intent(context,DemoHomeActivity.class).putExtra("empty",true);
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(intent)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                assertEquals(0,activity.list.getCount());
                android.view.View empty=activity.list.getEmptyView();
                android.widget.TextView hint=findText(empty,"Share a link, photo or video from another app. Pick a project. Its progress will find a home here.");
                assertNotNull("The empty state must explain the next action",hint);
                android.text.Layout layout=hint.getLayout();assertNotNull(layout);
                assertEquals(hint.length(),layout.getLineEnd(layout.getLineCount()-1));
                assertTrue("All guidance lines must be laid out, even at 200% font",hint.getHeight()>=layout.getHeight()+hint.getCompoundPaddingTop()+hint.getCompoundPaddingBottom());
                hint.requestRectangleOnScreen(new android.graphics.Rect(0,hint.getHeight()-1,hint.getWidth(),hint.getHeight()),true);
                android.graphics.Rect visible=new android.graphics.Rect();assertTrue(hint.getLocalVisibleRect(visible));
                assertTrue("The final line must be reachable by scrolling",visible.bottom>=hint.getHeight()&&visible.top<=hint.getCompoundPaddingTop()+layout.getLineTop(layout.getLineCount()-1));
            });
        }
    }

    static android.widget.TextView findText(android.view.View view,String text){
        if(view instanceof android.widget.TextView&&text.contentEquals(((android.widget.TextView)view).getText()))return (android.widget.TextView)view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.widget.TextView found=findText(group.getChildAt(i),text);if(found!=null)return found;}}
        return null;
    }

    @Test public void keyboardCanActivateAProjectCard() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor destination=instrumentation.addMonitor(ProjectHistoryActivity.class.getName(),null,true);
        try(ActivityScenario<DemoHomeActivity> scenario=ActivityScenario.launch(DemoHomeActivity.class)){
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->focusDescription(activity,"Settings"));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->assertEquals("Studio website, 3 tasks · 5 dispatches, Delivered",activity.getCurrentFocus().getContentDescription().toString()));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);
            instrumentation.waitForIdleSync();assertEquals(1,destination.getHits());
        }finally{instrumentation.removeMonitor(destination);}
    }

    @Test public void keyboardCanActivateATaskCard() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.app.Instrumentation.ActivityMonitor destination=instrumentation.addMonitor(TaskActivity.class.getName(),null,true);
        Intent intent=new Intent(context,ProjectHistoryActivity.class).putExtra("projectId","demo-studio").putExtra("projectName","Studio website");
        try(ActivityScenario<ProjectHistoryActivity> scenario=ActivityScenario.launch(intent)){
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->focusDescription(activity,"Back to projects"));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->{
                android.view.View focused=activity.getCurrentFocus();assertTrue(focused.isClickable());
                assertNotNull(findText(focused,"Give the homepage room to breathe"));
            });
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);
            instrumentation.waitForIdleSync();assertEquals(1,destination.getHits());
        }finally{instrumentation.removeMonitor(destination);}
    }

    static void focusDescription(android.app.Activity activity,String description){
        ArrayList<android.view.View> matches=new ArrayList<>();
        activity.getWindow().getDecorView().findViewsWithText(matches,description,android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
        assertEquals(1,matches.size());assertTrue(matches.get(0).requestFocus());
    }

    static void shareCatalog(ShareActivity activity){
        try{
            JSONObject data=activity.store.projectsData();JSONArray projects=data.getJSONArray("projects");
            for(int n=0;n<6;n++)projects.put(new JSONObject().put("id","extra-"+n).put("name","Extra project "+n).put("permission",new JSONObject().put("enabled",true)));
            data.put("projects",projects);activity.store.prefs.edit().putString("projects",data.toString()).commit();activity.renderProjects();
        }catch(Exception error){throw new AssertionError(error);}
    }

    @Test public void shareOrdersPendingAndAllRecentProjectsBeforeUnused(){
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                shareCatalog(activity);
                try{
                    JSONArray recent=new JSONArray();
                    for(String id:new String[]{"extra-5","extra-3","demo-studio"})recent.put(new JSONObject().put("id",id).put("name",activity.store.project(id).getString("name")).put("dispatch_count",1).put("last_dispatch_at",500-recent.length()*100));
                    activity.store.prefs.edit().putString("activity",new JSONObject().put("projects",recent).toString()).commit();
                    activity.store.save(new JSONObject().put("id",UUID.randomUUID().toString()).put("projectId","extra-4").put("projectName","Extra project 4").put("createdAt",600));
                }catch(Exception error){throw new AssertionError(error);}
                activity.go(0,1);
                assertNotNull("Newest local handoff precedes older server history",findText(activity.projectList.getChildAt(0),"Extra project 4"));
                assertNotNull(findText(activity.projectList.getChildAt(2),"Extra project 5"));
                assertNotNull(findText(activity.projectList.getChildAt(4),"Extra project 3"));
                activity.selected="demo-journal";activity.go(0,-1);
                assertNotNull(findText(activity.projectList.getChildAt(0),"Field notes"));
            });
        }
    }

    @Test public void shareUnavailableProjectDoesNotEnterTheEditor()throws Exception {
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                JSONObject project=activity.store.project("demo-studio");try{project.put("available",false);}catch(Exception error){throw new AssertionError(error);}
                activity.pick(project);assertFalse("Unavailable project must not start a selection transition",activity.busy);assertEquals("",activity.selected);assertEquals(0,activity.step);assertNull(activity.dialog);assertEquals(0,activity.store.pending().length());
            });
            captureUi("project-unavailable-local");clickWindowText("Got it");
        }
    }

    @Test public void shareSameNameProjectsShowDistinctLabelsAndKeepTheirIds()throws Exception {
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                try{JSONObject data=activity.store.projectsData();data.getJSONArray("projects").put(new JSONObject().put("id","duplicate-studio").put("name","Studio website").put("permission",new JSONObject().put("enabled",true)));activity.store.prefs.edit().putString("projects",data.toString()).commit();}catch(Exception error){throw new AssertionError(error);}
                activity.go(0,1);assertNotNull(findText(activity.projectList,"Studio website · demo-stu"));assertNotNull(findText(activity.projectList,"Studio website · duplicat"));
            });
            captureUi("duplicate-project-choice-local");
            scenario.onActivity(activity->{android.widget.TextView duplicate=findText(activity.projectList,"Studio website · duplicat");((android.view.View)duplicate.getParent()).performClick();});
            long deadline=android.os.SystemClock.elapsedRealtime()+5000;boolean[] entered={false};
            do{scenario.onActivity(activity->entered[0]=activity.step==1);if(!entered[0])Thread.sleep(25);}while(!entered[0]&&android.os.SystemClock.elapsedRealtime()<deadline);
            assertTrue(entered[0]);scenario.onActivity(activity->{assertEquals("duplicate-studio",activity.selected);assertNotNull(findText(activity.stage,"For “Studio website · duplicat”"));assertEquals(0,activity.store.pending().length());});
        }
    }

    @Test public void shareSearchAndExpandedCatalogSurviveRecreation(){
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                shareCatalog(activity);findText(activity.projectList,"Show all 9 projects").performClick();
                assertNotNull(findText(activity.projectList,"Extra project 5"));
                activity.search.setText("oRbIt");
                assertNotNull(findText(activity.projectList,"Orbit"));
                assertNull(findText(activity.projectList,"Studio website"));
            });
            scenario.recreate();
            scenario.onActivity(activity->{
                assertEquals("oRbIt",activity.search.getText().toString());
                assertNotNull(findText(activity.projectList,"Orbit"));
                activity.search.setText("  oRbIt  ");assertNotNull("Search ignores surrounding whitespace and case",findText(activity.projectList,"Orbit"));
                activity.search.setText("");assertNotNull(findText(activity.projectList,"Extra project 5"));
                activity.search.setText("does-not-exist");assertNotNull(findText(activity.projectList,"No matching projects"));
                activity.search.setText("Extra project 5");assertNotNull(findText(activity.projectList,"Extra project 5"));
                assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void sharePermissionActionsRemainReachableAtLargeText()throws Exception {
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                JSONObject project=activity.store.project("demo-studio");
                try{project.getJSONObject("permission").put("enabled",false);}catch(Exception error){throw new AssertionError(error);}
                activity.pick(project);assertNotNull(activity.dialog);
                android.widget.TextView cancel=findText(activity.dialog.card,"Cancel");assertNotNull(cancel);
                cancel.requestRectangleOnScreen(new android.graphics.Rect(0,0,cancel.getWidth(),cancel.getHeight()),true);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                android.widget.TextView cancel=findText(activity.dialog.card,"Cancel");
                cancel.requestRectangleOnScreen(new android.graphics.Rect(0,0,cancel.getWidth(),cancel.getHeight()),true);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            captureUi("permission-cancel-local");
            scenario.onActivity(activity->{
                android.widget.TextView cancel=findText(activity.dialog.card,"Cancel");android.graphics.Rect visible=new android.graphics.Rect();
                assertTrue("Permission Cancel must remain reachable",cancel.getGlobalVisibleRect(visible));
                assertEquals("Permission Cancel must not be clipped",cancel.getHeight(),visible.height());
                int[] origin=new int[2];activity.root.getLocationOnScreen(origin);android.view.WindowInsets insets=activity.root.getRootWindowInsets();
                int bottom=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom:insets.getSystemWindowInsetBottom();
                assertTrue("Permission actions must avoid system navigation",visible.bottom<=origin[1]+activity.root.getHeight()-bottom);
            });
            scenario.onActivity(activity->{
                android.widget.TextView cancel=findText(activity.dialog.card,"Cancel");
                cancel.performClick();assertNull(activity.dialog);assertEquals(0,activity.step);assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void sharePermissionKeyboardStaysInTheDialog(){
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->activity.authorize(activity.store.project("demo-studio")));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->assertTrue(findText(activity.dialog.card,"Allow & continue").requestFocus()));
            for(int n=0;n<4;n++){
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
                scenario.onActivity(activity->{
                    android.view.View focused=activity.getCurrentFocus();assertNotNull(focused);
                    android.view.ViewParent parent=focused.getParent();while(parent!=null&&parent!=activity.dialog.overlay)parent=parent.getParent();
                    assertTrue("Tab must stay in permission, focused: "+focused+" / "+focused.getContentDescription(),focused==activity.dialog.overlay||parent==activity.dialog.overlay);
                });
            }
        }
    }

    @Test public void failedSharePermissionCanRetryAndCancelWithoutAdvancing()throws Exception {
        java.util.concurrent.atomic.AtomicInteger attempts=new java.util.concurrent.atomic.AtomicInteger();
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain"))){
            scenario.onActivity(activity->{
                activity.store=new Store(context){@Override JSONObject setProjectPermission(String id,boolean enabled)throws Exception{attempts.incrementAndGet();throw new IOException("Synthetic permission unavailable");}};
                activity.authorize(activity.store.project("demo-studio"));
            });
            for(int n=1;n<=2;n++){
                scenario.onActivity(activity->findText(activity.dialog.card,"Allow & continue").performClick());
                long deadline=android.os.SystemClock.elapsedRealtime()+5000;boolean[] complete={false};
                do{scenario.onActivity(activity->complete[0]=!activity.busy);if(!complete[0])Thread.sleep(25);}while(!complete[0]&&android.os.SystemClock.elapsedRealtime()<deadline);
                assertTrue(complete[0]);assertEquals(n,attempts.get());
                scenario.onActivity(activity->{
                    assertNotNull(findText(activity.dialog.card,"Synthetic permission unavailable"));
                    assertTrue(findText(activity.dialog.card,"Allow & continue").isEnabled());
                    assertTrue(findText(activity.dialog.card,"Cancel").isEnabled());assertEquals(0,activity.step);assertEquals("",activity.selected);
                });
            }
            captureUi("permission-retry-local");
            scenario.onActivity(activity->{
                android.widget.TextView problem=findText(activity.dialog.card,"Synthetic permission unavailable");android.graphics.Rect visible=new android.graphics.Rect();assertTrue(problem.getGlobalVisibleRect(visible));assertEquals(problem.getHeight(),visible.height());
                findText(activity.dialog.card,"Cancel").performClick();assertNull(activity.dialog);assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void shareKeyboardSkipsDecorativeAndDuplicateStops() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Intent share=new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain");
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share)){
            scenario.onActivity(activity->{activity.selected="demo-studio";activity.go(1,1);});
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);instrumentation.waitForIdleSync();
            scenario.onActivity(activity->{assertFalse(activity.sheet.isFocusable());assertFalse(activity.gauge.isFocusable());assertTrue(activity.note.requestFocus());});
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->assertTrue(activity.getCurrentFocus().getContentDescription().toString().startsWith("Model & effort,")));
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);
            scenario.onActivity(activity->{
                assertTrue(activity.getCurrentFocus() instanceof android.widget.Button);
                assertEquals("Hand off to Codex",((android.widget.Button)activity.getCurrentFocus()).getText().toString());
                assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void followupDraftAndIdentitySurviveRecreationAndCancel() {
        Intent intent=new Intent(context,TaskActivity.class).putExtra("taskId",DemoFixture.TASK);
        String[] id={null};
        try(ActivityScenario<TaskActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(activity->{activity.followup();activity.followupInput.setText("Keep this draft after rotation");id[0]=activity.followupId;activity.expanded.add("Full report & evidence");});
            scenario.recreate();
            scenario.onActivity(activity->{
                assertTrue(activity.followupDialog.isShowing());
                assertEquals("Keep this draft after rotation",activity.followupInput.getText().toString());
                assertEquals(id[0],activity.followupId);
                assertTrue(activity.expanded.contains("Full report & evidence"));
                activity.followupDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                assertEquals(0,activity.store.pending().length());
            });
            scenario.recreate();
            scenario.onActivity(activity->{
                assertTrue(activity.followupDialog==null||!activity.followupDialog.isShowing());
                activity.followup();
                assertEquals("Keep this draft after rotation",activity.followupInput.getText().toString());
                assertEquals(id[0],activity.followupId);
            });
            scenario.onActivity(activity->{
                activity.followupInput.setText(" ");
                activity.followupDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                assertTrue(activity.followupDialog.isShowing());
                assertNotNull(activity.followupInput.getError());
                assertEquals(0,activity.store.pending().length());
            });
        }
    }

    @Test public void interruptedUploadPersistsProgressAndRetriesOnlyMissingAttachment() throws Exception {
        LocalTransport first=new LocalTransport(context);first.failSecondUpload=true;
        JSONObject task=task();JSONArray files=new JSONArray();
        for(String name:new String[]{"one.txt","two.txt"}){
            File file=new File(first.instanceFiles(),name);Files.write(file.toPath(),name.getBytes(StandardCharsets.UTF_8));
            files.put(new JSONObject().put("path",file.getPath()).put("mime","text/plain").put("name",name));
        }
        task.put("localFiles",files);first.save(task);
        try{first.sync();fail("Interrupted upload must remain pending");}catch(IOException expected){assertEquals("Synthetic upload interrupted",expected.getMessage());}
        JSONObject saved=first.pending().getJSONObject(0);
        assertEquals(1,saved.getJSONArray("assets").length());
        assertEquals("Synthetic upload interrupted",saved.getString("sendError"));
        assertTrue(new File(files.getJSONObject(0).getString("path")).exists());
        LocalTransport restored=new LocalTransport(context);restored.sync();
        assertEquals(Collections.singletonList("two.txt"),restored.uploads);
        assertEquals(Collections.singletonList(task.getString("id")),restored.submissions);
        assertEquals(0,restored.pending().length());
        for(int n=0;n<files.length();n++)assertFalse(new File(files.getJSONObject(n).getString("path")).exists());
    }

    @Test public void dismissingShareWithANoteRequiresAnExplicitDiscard() {
        ShareActivity[] shown={null};
        Intent share=new Intent(context,DemoShareActivity.class).setAction(Intent.ACTION_SEND).setType("text/plain");
        try(ActivityScenario<DemoShareActivity> scenario=ActivityScenario.launch(share)){
            scenario.onActivity(activity->{
                shown[0]=activity;
                activity.selected="demo-studio";activity.go(1,1);activity.note.setText("Do not lose this note");
                activity.close();assertTrue(activity.discardDialog.isShowing());assertFalse(activity.closing);
                activity.discardDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            scenario.onActivity(activity->{
                assertEquals("Do not lose this note",activity.note.getText().toString());assertFalse(activity.closing);
                assertEquals(0,activity.store.pending().length());
                activity.close();activity.discardDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue(shown[0].closing);assertEquals(0,shown[0].store.pending().length());
        }
    }

    @Test public void lostAcknowledgementKeepsSameIdAcrossRetry() throws Exception {
        LocalTransport first=new LocalTransport(context);first.loseAcknowledgement=true;
        JSONObject task=task();first.save(task);
        try{first.sync();fail("Missing acknowledgement must keep the retry copy");}catch(IOException expected){assertEquals("Synthetic acknowledgement lost",expected.getMessage());}
        assertEquals(1,first.pending().length());
        LocalTransport restored=new LocalTransport(context);restored.sync();
        assertEquals(first.submissions,restored.submissions);
        assertEquals(Collections.singletonList(task.getString("id")),restored.submissions);
        assertEquals(0,restored.pending().length());
    }

    @Test public void changingInstanceCannotReuseCredentialOrOutbox() throws Exception {
        Store original=new Store(context);original.save(task());String relay=original.relay,instance=original.instanceId;
        try{
            original.preferences.edit().putString("relay","https://second.example.invalid").putString("instanceId",UUID.randomUUID().toString()).commit();
            Store second=new Store(context);
            assertFalse(original.paired());assertFalse(second.paired());assertEquals(0,second.pending().length());
            try{original.api("/tasks","GET",null,null,null);fail("Stale screen must reject the changed connection before network access");}catch(IOException expected){assertTrue(expected.getMessage().contains("connection changed"));}
            try{second.credential();fail("New instance must not inherit the old token");}catch(IOException expected){assertTrue(expected.getMessage().contains("Connect"));}
        }finally{original.preferences.edit().putString("relay",relay).putString("instanceId",instance).commit();}
        Store restored=new Store(context);assertTrue(restored.paired());assertEquals(1,restored.pending().length());
    }

    @Test public void foreignOutboxRecordIsNeverRetaggedOrSent() throws Exception {
        LocalTransport store=new LocalTransport(context);String foreign=UUID.randomUUID().toString();
        JSONObject misplaced=task().put("instanceId",foreign),local=task();
        File misplacedFile=new File(store.outbox(),misplaced.getString("id")+".json");
        // Simulate a damaged/restored outbox entry, bypassing the normal save path.
        Files.write(misplacedFile.toPath(),misplaced.toString().getBytes(StandardCharsets.UTF_8));store.save(local);
        for(int attempt=0;attempt<2;attempt++){
            try{store.sync();fail("Foreign record must remain rejected");}catch(IOException expected){assertTrue(expected.getMessage().contains("another Relay"));}
            JSONObject retained=new JSONObject(new String(Files.readAllBytes(misplacedFile.toPath()),StandardCharsets.UTF_8));
            assertEquals(foreign,retained.getString("instanceId"));
            assertEquals(Collections.singletonList(local.getString("id")),store.submissions);
        }
    }

    JSONObject task() throws Exception {return new JSONObject().put("id",UUID.randomUUID().toString()).put("projectId","demo-studio").put("message","Local recovery fixture").put("content","Synthetic material").put("assets",new JSONArray()).put("localFiles",new JSONArray());}

    /** Replace only transport. Store.sync, persistence and cleanup remain production code. */
    static final class LocalTransport extends Store {
        final List<String> uploads=new ArrayList<>(),submissions=new ArrayList<>();
        boolean failSecondUpload,loseAcknowledgement;
        LocalTransport(Context context){super(context);}
        @Override JSONObject api(String path,String method,byte[] body,String mime,String filename)throws Exception {
            if(path.equals("/uploads")){
                uploads.add(filename);
                if(failSecondUpload&&uploads.size()==2)throw new IOException("Synthetic upload interrupted");
                return new JSONObject().put("id",filename+"-asset");
            }
            if(path.equals("/tasks")&&method.equals("POST")){
                JSONObject task=new JSONObject(new String(body,StandardCharsets.UTF_8));submissions.add(task.getString("id"));
                if(loseAcknowledgement)throw new IOException("Synthetic acknowledgement lost");
                return new JSONObject().put("id",task.getString("id")).put("status","queued");
            }
            if(path.startsWith("/tasks"))return new JSONObject().put("tasks",new JSONArray()).put("syncCursor",1);
            if(path.equals("/projects/activity"))return new JSONObject().put("projects",new JSONArray());
            if(path.equals("/projects"))return new JSONObject().put("projects",new JSONArray());
            if(path.equals("/device/settings"))return new JSONObject().put("directExecution",false);
            throw new AssertionError("Unexpected fixture request: "+method+" "+path);
        }
    }
}
