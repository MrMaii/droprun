package app.droprun;

public final class TaskWatchPolicyTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(String state:new String[]{"queued","reading","planning","queued_execution","running"})check(TaskWatchPolicy.needsWatch(state,""),"Keep receiving while task is "+state);
        for(String state:new String[]{"completed","blocked","failed","cancelled","awaiting_plan_approval","waiting_for_approval"})check(!TaskWatchPolicy.needsWatch(state,""),"Do not burn background time after a result or a request for input: "+state);
        check(TaskWatchPolicy.needsWatch("completed","reopening"),"Reopening a completed result must keep receiving until preview resolves");
        check(!TaskWatchPolicy.needsWatch("completed","ready"),"Ready preview must not keep a service alive");
        check("".equals(TaskWatchPolicy.stopReason(0,1000,0)),"All results received: stop without a warning");
        check(TaskWatchPolicy.stopReason(5,TaskWatchPolicy.SESSION_MS-1,0)==null,"Continue receiving queued work inside session window");
        check(TaskWatchPolicy.stopReason(5,TaskWatchPolicy.SESSION_MS,0).contains("电脑任务继续"),"A session timeout must retain work and explain delayed notifications");
        check(TaskWatchPolicy.stopReason(1,TaskWatchPolicy.OFFLINE_MS-1,TaskWatchPolicy.OFFLINE_MS-1)==null,"A short connection loss must retry automatically");
        check(TaskWatchPolicy.stopReason(1,TaskWatchPolicy.OFFLINE_MS,TaskWatchPolicy.OFFLINE_MS).contains("任务仍保留"),"A long connection loss must stop receiving without cancelling work");
        check(TaskWatchPolicy.stopReason(1,20*60*1000,0)==null,"A recovered connection clears offline timeout");
        check("".equals(TaskWatchPolicy.stopReason(0,TaskWatchPolicy.SESSION_MS,TaskWatchPolicy.OFFLINE_MS)),"Completion wins over simultaneous timeout; do not send a misleading pause warning");
        System.out.println("PASS: Android active results, approval stop, offline recovery and bounded receiving");
    }
}
