package com.scan.zxing.base;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.scan.R;
import com.scan.zxing.camera.CameraManager;
import com.scan.zxing.decode.DecodeThread;
import com.scan.zxing.decode.PhotoScanHandler;
import com.scan.zxing.decode.RGBLuminanceSource;
import com.scan.zxing.utils.BeepManager;
import com.scan.zxing.utils.BitmapUtil;
import com.scan.zxing.utils.CaptureActivityHandler;
import com.scan.zxing.utils.InactivityTimer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class ScanManager implements SurfaceHolder.Callback {
    private boolean isHasSurface = false;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;//用于拍摄扫描的handler
    private PhotoScanHandler photoScanHandler;//用于照片扫描的handler,不可共用，图片扫描是不需要摄像机的
    private Rect mCropRect = null;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private SurfaceView scanPreview = null;
    private View scanContainer;
    private View scanCropView;
    private ImageView scanLine;
    private Activity activity;
    private ScanListener listener;
    private boolean isOpenLight = false;

    private int scanMode;//扫描模型（条形，二维码，全部）

    /**
     * 用于启动照相机扫描二维码，在activity的onCreate里面构造出来
     * 在activity的生命周期中调用此类相对应的生命周期方法
     *
     * @param activity      扫描的activity
     * @param scanPreview   预览的SurfaceView
     * @param scanContainer 扫描的布局，全屏布局
     * @param scanCropView  扫描的矩形区域
     * @param scanLine      扫描线
     */
    public ScanManager(Activity activity, SurfaceView scanPreview, View scanContainer, View scanCropView, ImageView scanLine, int scanMode, ScanListener listener) {
        this.activity = activity;
        this.scanPreview = scanPreview;
        this.scanContainer = scanContainer;
        this.scanCropView = scanCropView;
        this.scanLine = scanLine;
        this.listener = listener;
        this.scanMode = scanMode;
        startAnimation(scanLine);
    }

    /**
     * 启动动画
     */
    public void startAnimation(ImageView scanLine) {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.9f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    /**
     * 用于图片扫描的构造函数
     *
     * @param listener 结果的监听回调
     */
    public ScanManager(ScanListener listener) {
        this.listener = listener;
    }

    public void onResume() {
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
        cameraManager = new CameraManager(activity.getApplicationContext());
        handler = null;
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            scanPreview.getHolder().addCallback(this);
        }
        inactivityTimer.onResume();
    }

    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
    }

    public void onDestroy() {
        inactivityTimer.shutdown();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, scanMode);
            }

            initCrop();
        } catch (IOException ioe) {
            listener.scanError(new Exception(ScanConstant.OPEN_CAMERA_ERROR + ""));
        } catch (RuntimeException e) {
            listener.scanError(new Exception(ScanConstant.OPEN_CAMERA_ERROR + ""));
        }
    }

    /**
     * 开关闪关灯
     */
    public void switchLight() {
        if (isOpenLight) {
            cameraManager.offLight();
        } else {
            cameraManager.openLight();
        }
        isOpenLight = !isOpenLight;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public Rect getCropRect() {
        return mCropRect;
    }

    /**
     * 扫描成功的结果回调
     */
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();//扫描成功播放声音滴一下，可根据需要自行确定什么时候播
        bundle.putInt("width", mCropRect.width());
        bundle.putInt("height", mCropRect.height());
        bundle.putString("result", rawResult.getText());
        listener.scanResult(rawResult, bundle);
    }

    public void handleDecodeError(Exception e) {
        listener.scanError(e);
    }

    /**
     * 初始化截取的矩形区域
     */
    public void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    /**
     * 获取状态栏高度
     */
    public int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return activity.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 用于扫描本地图片二维码或者一维码
     */
    public void scanningImage(final String photo_path2) {
        if (TextUtils.isEmpty(photo_path2)) {
            listener.scanError(new Exception(ScanConstant.OPEN_LOCAL_PHOTO_NULL + ""));
        }
        photoScanHandler = new PhotoScanHandler(this);
        new Thread(new Runnable() {

            @Override
            public void run() {
                Map<DecodeHintType, Object> hints = DecodeThread.getHints();//获取初始化的设置器
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
                Bitmap bitmap = BitmapUtil.decodeBitmapFromPath(photo_path2, 600, 600);
                RGBLuminanceSource source = new RGBLuminanceSource(bitmap);
                BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
                QRCodeReader reader = new QRCodeReader();
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                try {
                    Message msg = Message.obtain();
                    msg.what = PhotoScanHandler.PHOTODECODEOK;
                    msg.obj = multiFormatReader.decode(bitmap1, hints);
                    photoScanHandler.sendMessage(msg);
                } catch (Exception e) {
                    Message msg = Message.obtain();
                    msg.what = PhotoScanHandler.PHOTODECODEERROR;
                    msg.obj = new Exception("图片有误，或者图片模糊！");
                    photoScanHandler.sendMessage(msg);
                }
            }
        }).start();
    }

    /**
     * 扫描一次后，如需再次扫描，请调用这个方法
     */
    public void reScan() {
        if (handler != null) {
            handler.sendEmptyMessage(R.id.restart_preview);
        }
    }

    public boolean isScanning() {
        if (handler != null) {
            return handler.isScanning();
        }
        return false;
    }

}
