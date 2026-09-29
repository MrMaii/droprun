package app.droprun;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises Android JobInfo equality without registering work with the real scheduler. */
public class SyncScheduleTest {
    static final class Scheduler extends JobScheduler {
        final Map<Integer,JobInfo> jobs=new HashMap<>();int registrations;
        @Override public int schedule(JobInfo job){registrations++;jobs.put(job.getId(),job);return RESULT_SUCCESS;}
        @Override public int enqueue(JobInfo job,JobWorkItem work){throw new AssertionError("Unexpected enqueue");}
        @Override public void cancel(int id){jobs.remove(id);}
        @Override public void cancelAll(){jobs.clear();}
        @Override public List<JobInfo> getAllPendingJobs(){return new ArrayList<>(jobs.values());}
        @Override public JobInfo getPendingJob(int id){return jobs.get(id);}
    }
    Context context(Scheduler scheduler){return new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()){
        @Override public Object getSystemService(String name){return JOB_SCHEDULER_SERVICE.equals(name)?scheduler:super.getSystemService(name);}
    };}
    @Test public void reopeningHomePreservesRegisteredPeriodicSync(){
        Scheduler scheduler=new Scheduler();Context context=context(scheduler);
        SyncJob.schedule(context);JobInfo registered=scheduler.getPendingJob(701);
        assertNotNull(registered);assertTrue(registered.isPeriodic());assertTrue(registered.isPersisted());assertEquals(15*60*1000,registered.getIntervalMillis());
        for(int n=0;n<20;n++)SyncJob.schedule(context);
        assertEquals("Reopening home must not cancel/re-register an unchanged job",1,scheduler.registrations);
        assertSame(registered,scheduler.getPendingJob(701));
    }
    @Test public void missingOrObsoletePeriodicSyncIsRegisteredAgain(){
        Scheduler scheduler=new Scheduler();Context context=context(scheduler);
        scheduler.jobs.put(701,new JobInfo.Builder(701,new ComponentName(context,SyncJob.class)).setPeriodic(30*60*1000).build());
        SyncJob.schedule(context);assertEquals(1,scheduler.registrations);assertEquals(15*60*1000,scheduler.getPendingJob(701).getIntervalMillis());
        scheduler.cancel(701);SyncJob.schedule(context);assertEquals(2,scheduler.registrations);
        SyncJob.schedule(context);assertEquals(2,scheduler.registrations);
    }
}
