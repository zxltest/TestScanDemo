package com.scan.zxing.base;

import android.os.Bundle;

import com.google.zxing.Result;

/**
 * 二维码结果监听返回
 */
public interface ScanListener {
    /**
     * 返回扫描结果 存放了截图，或者是空的
     */
    void scanResult(Result rawResult, Bundle bundle);

    /**
     * 扫描抛出的异常
     */
    void scanError(Exception e);

}
