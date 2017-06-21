package com.scan.zxing.base;

import com.scan.zxing.decode.DecodeThread;

public class ScanConstant {
    public static final String REQUEST_SCAN_MODE = "ScanMode";//扫描类型
    /**
     * 扫描类型和DecodeThread中的数据一致
     */
    public static final int REQUEST_SCAN_MODE_BARCODE = DecodeThread.BARCODE_MODE;//条形码0x100
    public static final int REQUEST_SCAN_MODE_QRCODE = DecodeThread.QRCODE_MODE;  //二维码0x200
    public static final int REQUEST_SCAN_MODE_ALL = DecodeThread.ALL_MODE;        //条形码或者二维码0x300

    public static final int OPEN_CAMERA_ERROR = 0x004;//相机打开出错，请检查是否被禁止了该权限！
    public static final int OPEN_LOCAL_PHOTO_NULL = 0x005;//本地路径为空
    public static final int PHOTO_REQUEST_CODE = 0x006;//相册请求值
    public static final int SCAN_RESULT = 0x007;//扫描结果
    public static final int CREATE_RESULT = 0x007;//创建结果
}
