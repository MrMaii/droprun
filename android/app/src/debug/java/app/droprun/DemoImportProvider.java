package app.droprun;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Private synthetic share source. Never reads user files or contacts a Relay. */
public final class DemoImportProvider extends ContentProvider {
    static final AtomicInteger opens=new AtomicInteger();
    static volatile CountDownLatch firstChunk=new CountDownLatch(1),resume=new CountDownLatch(0);
    static File file(android.content.Context context){return new File(context.getCacheDir(),"synthetic-import.bin");}
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
        MatrixCursor cursor=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});cursor.addRow(new Object[]{"local-evidence.bin",file(getContext()).length()});return cursor;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        String path=uri.getPath();if(!"r".equals(mode)||!("/local-input".equals(path)||"/failed-input".equals(path)||"/paused-input".equals(path)))throw new FileNotFoundException("Unexpected synthetic input");
        opens.incrementAndGet();if("/local-input".equals(path))return ParcelFileDescriptor.open(file(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);
        try{
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();File source=file(getContext());CountDownLatch gate=resume;
            new Thread(()->{
                try(InputStream input=new FileInputStream(source);OutputStream output=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){
                    byte[] buffer=new byte[4096];int count=input.read(buffer);if(count>0)output.write(buffer,0,count);output.flush();firstChunk.countDown();
                    if("/failed-input".equals(path)){pipe[1].closeWithError("Synthetic source interrupted");return;}
                    gate.await();while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
                }catch(Exception error){try{pipe[1].closeWithError("Synthetic source closed");}catch(IOException ignored){}}
            },"local-import-source").start();return pipe[0];
        }catch(IOException error){throw new FileNotFoundException(error.getMessage());}
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
