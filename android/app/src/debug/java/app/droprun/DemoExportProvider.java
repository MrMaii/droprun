package app.droprun;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

/** Private local destination for export tests; never exposes user files or a network service. */
public final class DemoExportProvider extends ContentProvider {
    static final AtomicInteger opens=new AtomicInteger();
    static volatile boolean fail,partial;
    static File file(android.content.Context context){return new File(context.getCacheDir(),"synthetic-export.txt");}
    @Override public boolean onCreate(){return true;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"/local-output".equals(uri.getPath())||!"w".equals(mode))throw new FileNotFoundException("Unexpected fixture destination");
        opens.incrementAndGet();if(fail)throw new FileNotFoundException("Synthetic save location unavailable");
        if(partial)try{
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();
            new Thread(()->{
                try(java.io.InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);java.io.OutputStream output=new java.io.FileOutputStream(file(getContext()))){
                    byte[] prefix=new byte[4096];int count=input.read(prefix);if(count>0)output.write(prefix,0,count);output.flush();pipe[0].closeWithError("Synthetic destination stopped after a partial write");
                }catch(java.io.IOException ignored){try{pipe[0].closeWithError("Synthetic destination failure");}catch(java.io.IOException closed){}}
            },"local-export-failure").start();
            return pipe[1];
        }catch(java.io.IOException failure){throw new FileNotFoundException(failure.getMessage());}
        return ParcelFileDescriptor.open(file(getContext()),ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE|ParcelFileDescriptor.MODE_WRITE_ONLY);
    }
    @Override public String getType(Uri uri){return "text/plain";}
    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){throw new UnsupportedOperationException();}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
