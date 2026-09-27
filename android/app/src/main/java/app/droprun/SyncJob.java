package app.droprun;
import android.app.job.*;
import android.content.Context;
import android.content.ComponentName;

public class SyncJob extends JobService {
    public static void schedule(Context c) {
        JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.schedule(new JobInfo.Builder(701,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(15*60*1000).build());
    }
    public static void soon(Context c) {
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(new JobInfo.Builder(702,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(30000,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());
    }
    @Override public boolean onStartJob(JobParameters p) {new Thread(()->{boolean retry=false;try{Store s=new Store(this);if(s.paired())s.sync();}catch(Exception e){retry=true;}jobFinished(p,retry);}).start();return true;}
    @Override public boolean onStopJob(JobParameters p) {return true;}
}
