package app.droprun;

final class TaskPresentation {
    /** Prefer the actual outcome section over the report's source-analysis introduction. */
    static String resultSummary(String report){
        String clean=report.replaceAll("(?s)```droprun\\s.*?```", "").trim();
        java.util.regex.Matcher section=java.util.regex.Pattern.compile("(?im)^#{1,6}\\s*(?:我做了什么|交付结果|结果|What changed|What I did|The result|Summary)\\s*[:：]?\\s*\\n").matcher(clean);
        if(section.find()){
            String remaining=clean.substring(section.end());java.util.regex.Matcher next=java.util.regex.Pattern.compile("(?m)^#{1,6}\\s").matcher(remaining);
            clean=next.find()?remaining.substring(0,next.start()):remaining;
        }else clean=clean.replaceAll("(?m)^#{1,6}[^\\n]*\\n?", "");
        clean=clean.replace("**", "").trim();
        int paragraph=clean.indexOf("\n\n");if(paragraph>0)clean=clean.substring(0,paragraph);
        return clip(clean,200);
    }
    static String status(String value) {
        return switch(value) {
            case "queued" -> L.t("Waiting for computer","等待电脑接收");
            case "reading" -> L.t("Reading material","正在提取材料");
            case "planning" -> L.t("Preparing a plan","理解与规划中");
            case "awaiting_plan_approval" -> L.t("Review your plan","等你批准计划");
            case "queued_execution" -> L.t("Approved · waiting for computer","已批准，等待电脑");
            case "running" -> L.t("Codex is working","Codex 工作中");
            case "waiting_for_approval" -> L.t("Approve a command","等你批准命令");
            case "completed" -> L.t("Delivered","已完成");
            case "blocked" -> L.t("Needs attention","需要处理");
            case "failed" -> L.t("Failed","失败");
            case "cancelled" -> L.t("Cancelled","已取消");
            default -> value;
        };
    }
    static boolean needsUser(String status) { return status.equals("awaiting_plan_approval") || status.equals("waiting_for_approval"); }
    /** Same terminal set as {@code Store.finished}, kept here so this class stays JVM-testable without Android. */
    static boolean finished(String status) {
        return switch(status) {
            case "completed", "failed", "cancelled", "blocked" -> true;
            default -> false;
        };
    }
    static int statusColor(String status) {
        return switch(status) {
            case "awaiting_plan_approval", "waiting_for_approval" -> Ui.AMBER;
            case "blocked", "failed" -> Ui.DANGER;
            case "completed", "cancelled" -> Ui.MUTED;
            default -> Ui.ACCENT;
        };
    }
    static String mode(String value) {
        return switch(value) {
            case "direct" -> L.t("Act on the idea","直接执行");
            case "review" -> L.t("Plan approval required","先计划后批准");
            default -> L.t("Legacy task · isolated copy","历史任务 · 隔离副本");
        };
    }
    static String noReport(String status,boolean hasPlan) {
        return switch(status) {
            case "blocked" -> L.t("This handoff is blocked. Check the reason above; waiting alone will not resume it.","任务受阻。看看上面的原因；等待不会自动恢复。");
            case "failed" -> L.t("Execution failed. Check the reason above.","执行失败。看看上面的原因。");
            case "cancelled" -> L.t("This handoff was cancelled. There is no report.","任务已取消，没有报告。");
            case "completed" -> L.t("The handoff finished, but its report is unavailable. Refresh shortly.","已完成，报告暂时读不到，稍后刷新。");
            case "planning" -> L.t("Codex is reading and preparing a plan. It will wait here for your approval before editing.","Codex 正在只读地理解材料并拟定计划，完成后会在这里等你批准。");
            case "awaiting_plan_approval" -> hasPlan?L.t("This is a plan. Work has not started; approve it to proceed.","以上是计划，还没开始做。批准后才执行。"):L.t("The plan is unavailable. Refresh while connected.","计划暂时读不到，请联网刷新。");
            case "queued_execution" -> L.t("Plan approved. Waiting for your computer to begin.","计划已批准，等待电脑开始执行。");
            case "waiting_for_approval" -> L.t("Waiting for your command decision. Review the request above.","正在等你批准一条命令，请核对上面的请求。");
            case "queued" -> L.t("Submitted. Waiting for your computer; it will stay queued while the computer is offline.","已提交，等待电脑接收。电脑离线时会排队。");
            case "reading" -> L.t("Retrieving available material. The report will say which text, audio or frames were actually obtained.","正在提取可获取的材料。报告会说明实际读到了文字、音频还是画面。");
            case "running" -> L.t("Codex is working in your project. Follow along on your computer; the report will arrive here.","Codex 正在项目里工作。可以去电脑的 Codex 看进展；完成后报告会显示在这里。");
            default -> L.t("Waiting for an update.","等待状态更新。");
        };
    }
    static String notificationKey(String id,String status,String planVersion,String approvalId) {
        if(status.equals("awaiting_plan_approval"))return id+":plan:"+planVersion;
        if(status.equals("waiting_for_approval")&&!approvalId.isEmpty())return id+":"+approvalId;
        return id;
    }
    static String previewStatus(String status,String url,long expiresAt,long now) {
        if(status==null||status.equals("null"))status="";
        if(!status.isEmpty()&&!status.equals("ready"))return status;
        if(url.isEmpty())return status.isEmpty()?"":"unavailable";
        return expiresAt>0&&expiresAt<=now?"expired":"ready";
    }
    static boolean snapshot(String kind){return "snapshot".equals(kind)||"static".equals(kind);}
    static String elapsed(long createdAt,long now) {
        long minutes=Math.max(0,(now-createdAt)/60000);
        if(minutes<1)return L.t("Just now","刚刚");
        if(minutes<60)return minutes+L.t(" min ago"," 分钟前");
        long hours=minutes/60;
        if(hours<24)return hours+L.t(" hr ago"," 小时前");
        return (hours/24)+L.t(" days ago"," 天前");
    }
    /** "42 秒", "6 分 34 秒", "1 小时 12 分"; a zero trailing unit is dropped and negative spans clamp to zero. */
    static String duration(long ms) {
        long seconds=Math.max(0,ms/1000);
        if(seconds<60)return seconds+L.t(" sec"," 秒");
        long minutes=seconds/60;
        if(minutes<60)return minutes+L.t(" min"," 分")+(seconds%60==0?"":" "+seconds%60+L.t(" sec"," 秒"));
        long hours=minutes/60;
        return hours+L.t(" hr"," 小时")+(minutes%60==0?"":" "+minutes%60+L.t(" min"," 分"));
    }
    /** Second line of a list row: total time of a finished task, running time of an active one, or that it waits for the user. */
    static String processingTime(String status,long createdAt,long updatedAt,long now) {
        if(needsUser(status))return L.t("Needs your attention","等你处理");
        if(finished(status))return updatedAt>createdAt?L.t("Elapsed ","用时 ")+duration(updatedAt-createdAt):elapsed(createdAt,now);
        long minutes=Math.max(0,(now-createdAt)/60000);
        if(minutes<1)return L.t("Just started","刚刚开始");
        return L.t("Elapsed ","已进行 ")+(minutes<60?minutes+L.t(" min"," 分钟"):duration(minutes*60000));
    }
    static String clip(String value,int max) {
        String text=value==null?"":value.trim().replaceAll("\\s+"," ");
        return text.length()>max?text.substring(0,max)+"…":text;
    }
}
