package app.droprun;

/** Bounds result fetching; expiry never cancels the user's work on the computer. */
final class TaskWatchPolicy {
    static final long POLL_MS=8000, OFFLINE_MS=10*60*1000, SESSION_MS=90*60*1000;
    static boolean needsWatch(String status,String previewStatus) {
        if(previewStatus.equals("reopening"))return true;
        return switch(status) {
            case "queued", "reading", "planning", "queued_execution", "running" -> true;
            default -> false;
        };
    }
    static String stopReason(int active,long elapsed,long offlineElapsed) {
        if(active==0)return "";
        if(elapsed>=SESSION_MS)return L.t("This live receiving session reached 90 minutes. Your computer keeps working; results will sync periodically. Open DropRun to resume live receiving.","本次即时接收已满 90 分钟。电脑任务继续，结果会由系统周期同步；打开 DropRun 可恢复即时接收。");
        if(offlineElapsed>=OFFLINE_MS)return L.t("The phone or computer has been disconnected for 10 minutes. Handoffs are retained and will sync periodically. Open DropRun to resume live receiving.","手机或电脑连接已中断 10 分钟。任务仍保留，联网后由系统周期同步；打开 DropRun 可恢复即时接收。");
        return null;
    }
}
