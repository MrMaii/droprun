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
        boolean originalChinese=L.chinese();java.util.TimeZone originalZone=java.util.TimeZone.getDefault();java.util.Locale originalFormat=java.util.Locale.getDefault(java.util.Locale.Category.FORMAT);
        try{
            java.util.Locale.setDefault(java.util.Locale.Category.FORMAT,java.util.Locale.FRANCE);java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
            long now=java.time.Instant.parse("2026-01-02T12:00:00Z").toEpochMilli(),week=7*86400000L;
            L.language("en");
            check(TaskPresentation.listDate(0,now).equals("Date unavailable")&&TaskPresentation.listDate(-1,now).equals("Date unavailable"),"Absent or invalid list timestamps must not pretend to be a 1970 date");
            check(TaskPresentation.listDate(now,now).equals("Just now")&&TaskPresentation.listDate(now+60000,now).equals("Just now"),"Current and future list timestamps keep existing clock-skew clamping");
            check(TaskPresentation.listDate(now-60000,now).equals("1 min ago")&&TaskPresentation.listDate(now-3*3600000L,now).equals("3 hr ago")&&TaskPresentation.listDate(now-2*86400000L,now).equals("2 days ago"),"Recent list dates retain minutes, hours and days");
            check(TaskPresentation.listDate(now-week+1,now).equals("6 days ago"),"The final millisecond before seven days remains relative");
            check(TaskPresentation.listDate(now-week,now).equals("Dec 26, 2025")&&TaskPresentation.listDate(now-week-1,now).equals("Dec 26, 2025"),"At seven days list dates become explicit English calendar dates despite French system formatting");
            L.language("zh");
            check(TaskPresentation.listDate(0,now).equals("日期未知")&&TaskPresentation.listDate(-1,now).equals("日期未知"),"Unknown list dates have explicit Chinese copy");
            check(TaskPresentation.listDate(now+60000,now).equals("刚刚")&&TaskPresentation.listDate(now-week+1,now).equals("6 天前")&&TaskPresentation.listDate(now-week,now).equals("2025年12月26日"),"Chinese copy retains future/recent clamping and includes the full older calendar date");
            long midnight=java.time.Instant.parse("2026-01-01T00:30:00Z").toEpochMilli(),later=java.time.Instant.parse("2026-01-10T12:00:00Z").toEpochMilli();
            L.language("en");check(TaskPresentation.listDate(midnight,later).equals("Jan 1, 2026"),"UTC uses the stored instant's calendar date");
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"));
            check(TaskPresentation.listDate(midnight,later).equals("Dec 31, 2025"),"A changed system timezone can move a stored instant into the previous year");
            L.language("zh");check(TaskPresentation.listDate(midnight,later).equals("2025年12月31日"),"Chinese dates use the same system timezone rather than UTC");
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
            check(TaskPresentation.listDate(midnight,later).equals("2026年1月1日"),"A later timezone change must not reuse an earlier cached date");
            L.language("en");check(TaskPresentation.listDate(midnight,later).equals("Jan 1, 2026"),"Product language switching changes the explicit date format immediately");
        }finally{L.language(originalChinese?"zh":"en");java.util.TimeZone.setDefault(originalZone);java.util.Locale.setDefault(java.util.Locale.Category.FORMAT,originalFormat);}
        System.out.println("PASS: Android plan, delivery, blocked-state, processing-time, list-date and notification presentation");
    }
}
