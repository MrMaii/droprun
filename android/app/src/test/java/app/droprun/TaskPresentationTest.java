package app.droprun;

public final class TaskPresentationTest {
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args) {
        check(TaskPresentation.status("planning").contains("规划"),"Planning must describe planning rather than execution");
        check(TaskPresentation.status("awaiting_plan_approval").contains("批准计划"),"Plan approval must be distinct from command approval");
        check(TaskPresentation.status("queued_execution").contains("已批准"),"An approved plan must show it is waiting for the computer");
        check(TaskPresentation.needsUser("awaiting_plan_approval")&&TaskPresentation.needsUser("waiting_for_approval")&&!TaskPresentation.needsUser("running"),"Only approval states need the user");
        check(TaskPresentation.mode("direct").contains("直接"),"Direct task must disclose direct execution");
        check(TaskPresentation.mode("legacy-isolated").contains("历史任务"),"Old isolated tasks must not appear to execute directly");
        check(TaskPresentation.noReport("awaiting_plan_approval",true).contains("还没开始做"),"Understanding report is not delivery evidence");
        check(TaskPresentation.noReport("awaiting_plan_approval",false).contains("刷新"),"Missing plan must not invite blind approval");
        check(TaskPresentation.noReport("blocked",false).contains("不会自动恢复"),"Blocked task must not tell user to keep waiting");
        for(String terminal:new String[]{"blocked","failed","cancelled","completed"})check(!TaskPresentation.noReport(terminal,false).contains("正在"),"Terminal task cannot pretend execution continues: "+terminal);
        String plan=TaskPresentation.notificationKey("task-1","awaiting_plan_approval","plan-1","");
        check(!plan.equals(TaskPresentation.notificationKey("task-1","completed","plan-1","")),"Plan notification must not suppress later delivery notification");
        check(!plan.equals(TaskPresentation.notificationKey("task-1","awaiting_plan_approval","plan-2","")),"A revised plan needs its own notification");
        check(TaskPresentation.notificationKey("task-1","waiting_for_approval","","command-1").equals("task-1:command-1"),"Command notification identity must survive upgrades");
        check(TaskPresentation.elapsed(0,30000).equals("刚刚")&&TaskPresentation.elapsed(0,5*60000).equals("5 分钟前")&&TaskPresentation.elapsed(0,3*3600000).equals("3 小时前")&&TaskPresentation.elapsed(0,50*3600000).equals("2 天前"),"Elapsed time buckets");
        check(TaskPresentation.clip("  a  b\n c ",3).equals("a b…"),"Clip collapses whitespace and truncates");
        check(TaskPresentation.previewStatus("ready","https://preview",2000,1000).equals("ready"),"A live unexpired preview can be opened");
        check(TaskPresentation.previewStatus("ready","https://preview",1000,1000).equals("expired"),"The client must expire links even before its next sync");
        check(TaskPresentation.previewStatus("stopped","https://preview",2000,1000).equals("stopped"),"A stopped preview must not look live because its old expiry is in the future");
        check(TaskPresentation.previewStatus("reopening","https://old-preview",2000,1000).equals("reopening"),"A reopened preview must not offer the stale old link");
        check(TaskPresentation.previewStatus("ready","",2000,1000).equals("unavailable"),"Ready metadata without a URL is not usable");
        check(TaskPresentation.previewStatus("","https://legacy-preview",2000,1000).equals("ready"),"Existing relay versions retain valid preview links");
        check(TaskPresentation.previewStatus("","",0,1000).isEmpty(),"Tasks without previews must not show a broken preview card");
        check(TaskPresentation.previewStatus("null","",0,1000).isEmpty(),"JSON null from older tasks must not create a preview card");
        for(String terminal:new String[]{"blocked","failed","cancelled","completed"})check(TaskPresentation.finished(terminal),"Terminal status must count as finished: "+terminal);
        for(String live:new String[]{"queued","reading","planning","awaiting_plan_approval","queued_execution","running","waiting_for_approval"})check(!TaskPresentation.finished(live),"Live status must not count as finished: "+live);
        check(TaskPresentation.duration(42000).equals("42 秒")&&TaskPresentation.duration(394000).equals("6 分 34 秒")&&TaskPresentation.duration(72*60000).equals("1 小时 12 分"),"Duration buckets: seconds, minutes with seconds, hours with minutes");
        check(TaskPresentation.duration(360000).equals("6 分")&&TaskPresentation.duration(3600000).equals("1 小时"),"A zero trailing unit is dropped");
        check(TaskPresentation.duration(-5000).equals("0 秒")&&TaskPresentation.duration(999).equals("0 秒"),"Clock skew and sub-second spans clamp to zero instead of going negative");
        check(TaskPresentation.processingTime("completed",1000,395000,999999999).equals("用时 6 分 34 秒"),"Finished tasks show the total processing time from creation to last update");
        check(TaskPresentation.processingTime("failed",0,42000,999999999).equals("用时 42 秒")&&TaskPresentation.processingTime("cancelled",0,42000,999999999).equals("用时 42 秒")&&TaskPresentation.processingTime("blocked",0,42000,999999999).equals("用时 42 秒"),"Every terminal status reports how long it took");
        check(TaskPresentation.processingTime("completed",0,0,3*3600000).equals("3 小时前"),"A finished task without a usable update time falls back to when it was created instead of claiming 0 seconds");
        check(TaskPresentation.processingTime("running",0,0,3*60000).equals("已进行 3 分钟")&&TaskPresentation.processingTime("queued",0,0,59*60000).equals("已进行 59 分钟"),"Active tasks show how long they have been running in minutes");
        check(TaskPresentation.processingTime("running",0,0,75*60000).equals("已进行 1 小时 15 分"),"Long-running tasks switch to hours");
        check(TaskPresentation.processingTime("reading",0,0,30000).equals("刚刚开始"),"A fresh task is not shown as 0 minutes");
        check(TaskPresentation.processingTime("running",0,0,-60000).equals("刚刚开始"),"A creation time ahead of the phone clock does not go negative");
        check(TaskPresentation.processingTime("awaiting_plan_approval",0,0,3600000).equals("等你处理")&&TaskPresentation.processingTime("waiting_for_approval",0,3600000,3600000).equals("等你处理"),"Tasks waiting on the user say so instead of running a timer");
        System.out.println("PASS: Android plan, delivery, blocked-state, processing-time and notification presentation");
    }
}
