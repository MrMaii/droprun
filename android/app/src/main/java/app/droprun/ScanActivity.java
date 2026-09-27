package app.droprun;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Camera2 preview with on-device QR decoding. A successful scan returns RESULT_OK with the "code"
 * extra. When scanning is not working (two foreign QR codes, 20 s without any decode, or a camera
 * error) a frosted prompt offers the pairing code instead: RESULT_CANCELED with manual=true and
 * reason=wrong|timeout|camera. A plain cancel after such trouble still carries the reason.
 */
public class ScanActivity extends StyledActivity {
    static final long TIMEOUT_MS=20000,WRONG_REPEAT_MS=2000;
    static String hintText(){return L.t("Point at the QR code on your computer","对准电脑屏幕上的二维码");}
    FrameLayout root,scene;TextureView preview;Finder finder;TextView hint;
    CameraDevice camera;CameraCaptureSession session;ImageReader reader;HandlerThread thread;Handler handler;Size size;
    final QRCodeReader decoder=new QRCodeReader();volatile boolean decoded=false;
    final Handler main=new Handler(Looper.getMainLooper());final Runnable timeout=()->trouble("timeout");
    Ui.Glass prompt;String lastReason,lastWrongText;long lastWrongAt=0;int wrongDecodes=0;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);Ui.configureWindow(this);
        // The preview bleeds under the system bars; only the chrome layer is inset.
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG);setContentView(root);
        scene=new FrameLayout(this);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));
        preview=new TextureView(this);scene.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        finder=new Finder(this);scene.addView(finder,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout chrome=new FrameLayout(this);scene.addView(chrome,new FrameLayout.LayoutParams(-1,-1));Ui.applyInsets(chrome,false);
        hint=Ui.text(this,hintText(),15,0xFFFFFFFF);hint.setGravity(Gravity.CENTER);hint.setPadding(dp(16),dp(9),dp(16),dp(9));
        hint.setBackground(Ui.outlined(this,0xB3000000,0,Ui.RADIUS,0));hint.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        FrameLayout.LayoutParams hintParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);hintParams.setMargins(dp(20),dp(16),dp(20),0);chrome.addView(hint,hintParams);
        Button cancel=Ui.button(this,L.t("Cancel","取消"),false);cancel.setOnClickListener(v->cancelScan());
        FrameLayout.LayoutParams cancelParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);cancelParams.setMargins(dp(20),0,dp(20),dp(20));chrome.addView(cancel,cancelParams);
    }
    int dp(int value){return Ui.dp(this,value);}

    @Override protected void onResume(){
        super.onResume();thread=new HandlerThread("droprun-scan");thread.start();handler=new Handler(thread.getLooper());
        if(preview.isAvailable())openCamera();
        else preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h){openCamera();}
            public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){applyTransform();}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture t){return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture t){}
        });
        if(prompt==null)restartTimer();
    }
    @Override protected void onPause(){main.removeCallbacks(timeout);closeCamera();if(thread!=null){thread.quitSafely();thread=null;handler=null;}super.onPause();}
    @Override public void onBackPressed(){if(prompt!=null)resumeScanning();else cancelScan();}
    void cancelScan(){setResult(RESULT_CANCELED,lastReason==null?null:new Intent().putExtra("reason",lastReason));finish();}

    // ---- camera --------------------------------------------------------------------------------
    void openCamera(){
        CameraManager manager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            String chosen=null;
            for(String id:manager.getCameraIdList()){Integer facing=manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);if(facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){chosen=id;break;}if(chosen==null)chosen=id;}
            if(chosen==null)throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED);
            StreamConfigurationMap map=manager.getCameraCharacteristics(chosen).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            size=chooseSize(map.getOutputSizes(ImageFormat.YUV_420_888));
            reader=ImageReader.newInstance(size.getWidth(),size.getHeight(),ImageFormat.YUV_420_888,2);
            reader.setOnImageAvailableListener(this::onFrame,handler);
            applyTransform();
            if(checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){setResult(RESULT_CANCELED,new Intent().putExtra("reason","camera"));finish();return;}
            manager.openCamera(chosen,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice device){camera=device;startPreview();}
                public void onDisconnected(CameraDevice device){device.close();camera=null;}
                public void onError(CameraDevice device,int error){device.close();camera=null;fail(L.t("Cannot open camera (","相机无法打开（")+error+"）");}
            },handler);
        }catch(Exception e){fail(L.t("Camera unavailable: ","相机不可用：")+e.getMessage());}
    }
    static Size chooseSize(Size[] sizes){
        Size best=null;
        for(Size s:sizes){if(s.getWidth()>1280||s.getHeight()>960)continue;if(best==null||(long)s.getWidth()*s.getHeight()>(long)best.getWidth()*best.getHeight())best=s;}
        return best!=null?best:sizes[0];
    }
    void startPreview(){
        try{
            SurfaceTexture texture=preview.getSurfaceTexture();if(texture==null||camera==null)return;
            texture.setDefaultBufferSize(size.getWidth(),size.getHeight());
            Surface surface=new Surface(texture);
            CaptureRequest.Builder request=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            request.addTarget(surface);request.addTarget(reader.getSurface());
            request.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            camera.createCaptureSession(Arrays.asList(surface,reader.getSurface()),new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession s){session=s;try{s.setRepeatingRequest(request.build(),null,handler);}catch(Exception e){fail(L.t("Preview failed: ","预览失败：")+e.getMessage());}}
                public void onConfigureFailed(CameraCaptureSession s){fail(L.t("Could not prepare the preview","预览配置失败"));}
            },handler);
        }catch(Exception e){fail(L.t("Preview failed: ","预览失败：")+e.getMessage());}
    }
    void applyTransform(){
        if(size==null)return;
        runOnUiThread(()->{
            float vw=preview.getWidth(),vh=preview.getHeight();if(vw==0||vh==0)return;
            // Portrait activity: the sensor buffer appears rotated, so its aspect on screen is width/height inverted.
            float targetH=vw*size.getWidth()/(float)size.getHeight();
            Matrix matrix=new Matrix();matrix.setScale(1f,targetH/vh,vw/2f,vh/2f);
            float fill=Math.max(1f,vh/targetH);matrix.postScale(fill,fill,vw/2f,vh/2f);
            preview.setTransform(matrix);
        });
    }
    void onFrame(ImageReader source){
        Image image;try{image=source.acquireLatestImage();}catch(IllegalStateException closed){return;}
        if(image==null)return;
        try{
            if(decoded)return;
            Image.Plane plane=image.getPlanes()[0];ByteBuffer buffer=plane.getBuffer();
            int stride=plane.getRowStride(),width=image.getWidth(),height=image.getHeight();
            byte[] data=new byte[stride*height];buffer.get(data,0,Math.min(buffer.remaining(),data.length));
            LuminanceSource luminance=new PlanarYUVLuminanceSource(data,stride,height,0,0,width,height,false);
            Result result;
            try{result=decoder.decode(new BinaryBitmap(new HybridBinarizer(luminance)));}
            catch(NotFoundException|ChecksumException|FormatException e){return;}
            finally{decoder.reset();}
            String text=result.getText(),code=PairingTarget.parse(text)==null?null:text;
            if(code==null){runOnUiThread(()->onForeign(text));return;}
            decoded=true;
            runOnUiThread(()->{setResult(RESULT_OK,new Intent().putExtra("code",code));finish();});
        }catch(RuntimeException dropped){
            // The reader was closed by onPause mid-frame, or the frame was torn; the next one retries.
        }finally{image.close();}
    }
    void fail(String message){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;closeCamera();hint.setText(message);hint.setTextColor(Ui.AMBER);finder.setAccent(Ui.AMBER);trouble("camera");});}
    void closeCamera(){
        if(session!=null){try{session.close();}catch(Exception ignored){}session=null;}
        if(camera!=null){camera.close();camera=null;}
        if(reader!=null){reader.close();reader=null;}
    }

    // ---- trouble detection and the pairing-code prompt -----------------------------------------
    void restartTimer(){main.removeCallbacks(timeout);main.postDelayed(timeout,TIMEOUT_MS);}
    void onForeign(String text){
        if(prompt!=null||decoded||isFinishing())return;
        long now=SystemClock.uptimeMillis();
        // The same wrong code decodes on every frame; count it once per two seconds so "twice" means two real attempts.
        if(text.equals(lastWrongText)&&now-lastWrongAt<WRONG_REPEAT_MS)return;
        lastWrongText=text;lastWrongAt=now;wrongDecodes++;
        hint.setText(L.t("This is not a DropRun pairing link. Scan the code on your computer.","这不是 DropRun 的配对码，请扫描电脑配对页上的二维码"));hint.setTextColor(Ui.AMBER);finder.setAccent(Ui.AMBER);
        restartTimer();
        if(wrongDecodes>=2)trouble("wrong");
    }
    /** Shown once per scanning cycle; "继续扫码" starts a new cycle. */
    void trouble(String reason){
        if(prompt!=null||decoded||isFinishing()||isDestroyed())return;
        lastReason=reason;main.removeCallbacks(timeout);
        if(reason.equals("timeout")){hint.setText(L.t("No QR code found yet","还没识别到二维码"));hint.setTextColor(Ui.AMBER);}
        prompt=Ui.glass(this,root,scene);
        LinearLayout card=prompt.card;
        card.addView(Ui.title(this,L.t("Trouble scanning?","扫码遇到困难？"),19));
        card.addView(Ui.text(this,L.t("Use the pairing link or code shown on your computer instead.","可以改用电脑配对页上显示的配对码。"),14,Ui.MUTED));
        Ui.space(card,14);
        Button manual=Ui.button(this,L.t("Use pairing code","输入配对码"),true);
        manual.setOnClickListener(v->{setResult(RESULT_CANCELED,new Intent().putExtra("manual",true).putExtra("reason",reason));finish();});
        card.addView(manual,Ui.fill());Ui.space(card,6);
        Button again=Ui.button(this,L.t("Keep scanning","继续扫码"),false);Ui.styleGhost(again);again.setOnClickListener(v->resumeScanning());
        card.addView(again,Ui.fill());
    }
    void resumeScanning(){
        Ui.Glass shown=prompt;if(shown==null)return;prompt=null;shown.dismiss(null);
        wrongDecodes=0;lastWrongText=null;hint.setText(hintText());hint.setTextColor(0xFFFFFFFF);finder.setAccent(Ui.LIME);
        if(camera==null&&handler!=null){closeCamera();openCamera();}
        restartTimer();
    }

    /** Dim mask with a clear 232dp rounded viewfinder and corner brackets in the accent color. */
    static final class Finder extends View {
        final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG),edge=new Paint(Paint.ANTI_ALIAS_FLAG),bracket=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path hole=new Path(),corners=new Path();final RectF box=new RectF();float radius;
        Finder(Context context){
            super(context);mask.setColor(0x99000000);
            edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(Math.max(1,Ui.dp(context,1)));edge.setColor(Ui.LINE_STRONG);
            bracket.setStyle(Paint.Style.STROKE);bracket.setStrokeWidth(Ui.dpf(context,3));bracket.setColor(Ui.LIME);bracket.setStrokeCap(Paint.Cap.ROUND);bracket.setStrokeJoin(Paint.Join.ROUND);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        void setAccent(int color){bracket.setColor(color);invalidate();}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            float side=Math.min(Ui.dpf(getContext(),232),Math.min(w,h)-Ui.dpf(getContext(),48));
            float left=(w-side)/2f,top=(h-side)/2f,right=left+side,bottom=top+side,arm=Ui.dpf(getContext(),16);
            radius=Ui.dpf(getContext(),20);float r=radius;box.set(left,top,right,bottom);
            hole.reset();hole.setFillType(Path.FillType.EVEN_ODD);hole.addRect(0,0,w,h,Path.Direction.CW);hole.addRoundRect(box,r,r,Path.Direction.CW);
            corners.reset();
            corners.moveTo(left,top+r+arm);corners.lineTo(left,top+r);corners.arcTo(left,top,left+2*r,top+2*r,180,90,false);corners.lineTo(left+r+arm,top);
            corners.moveTo(right-r-arm,top);corners.lineTo(right-r,top);corners.arcTo(right-2*r,top,right,top+2*r,270,90,false);corners.lineTo(right,top+r+arm);
            corners.moveTo(right,bottom-r-arm);corners.lineTo(right,bottom-r);corners.arcTo(right-2*r,bottom-2*r,right,bottom,0,90,false);corners.lineTo(right-r-arm,bottom);
            corners.moveTo(left+r+arm,bottom);corners.lineTo(left+r,bottom);corners.arcTo(left,bottom-2*r,left+2*r,bottom,90,90,false);corners.lineTo(left,bottom-r-arm);
        }
        @Override protected void onDraw(Canvas canvas){canvas.drawPath(hole,mask);canvas.drawRoundRect(box,radius,radius,edge);canvas.drawPath(corners,bracket);}
    }
}
