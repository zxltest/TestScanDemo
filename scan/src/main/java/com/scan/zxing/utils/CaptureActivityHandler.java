/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scan.zxing.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;
import com.scan.R;
import com.scan.zxing.base.ScanManager;
import com.scan.zxing.camera.CameraManager;
import com.scan.zxing.decode.DecodeThread;

public class CaptureActivityHandler extends Handler {

    private final ScanManager scanManager;
    private final DecodeThread decodeThread;
    private final CameraManager cameraManager;
    private State state;

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

    public CaptureActivityHandler(ScanManager scanManager, CameraManager cameraManager, int decodeMode) {
        this.scanManager = scanManager;
        decodeThread = new DecodeThread(scanManager, decodeMode);
        decodeThread.start();
        state = State.SUCCESS;
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        int what = message.what;
        if (what == R.id.restart_preview) {
            restartPreviewAndDecode();
        } else if (what == R.id.decode_succeeded) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();
            scanManager.handleDecode((Result) message.obj, bundle);
        } else if (message.what == R.id.decode_failed) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            decodeThread.join(500L);
        } catch (InterruptedException e) {
        }
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
        }
    }

    /**
     * 返回当前扫描状态，是否可扫描,State.PREVIEW 是可扫描状态
     */
    public boolean isScanning() {
        if (state == State.PREVIEW) {
            return true;
        }
        return false;
    }

}
