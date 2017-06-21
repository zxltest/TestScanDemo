package com.scan.zxing.base;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.scan.R;
import com.scan.zxing.decode.Utils;

public class ScanActivity extends Activity implements ScanListener, View.OnClickListener {
    private ScanManager scanManager;
    private RelativeLayout layout_container;
    private SurfaceView surfaceview;
    private FrameLayout layout_crop_view;
    private ImageView iv_scan_line;
    private TextView tv_gallery, tv_back, tv_light;
    public int mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_scan);
        initIntent();
        initView();
        //构造出扫描管理器
        scanManager = new ScanManager(this, surfaceview, layout_container, layout_crop_view, iv_scan_line, mMode, this);
    }

    public void initIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            mMode = intent.getIntExtra(ScanConstant.REQUEST_SCAN_MODE, ScanConstant.REQUEST_SCAN_MODE_ALL);
        }
        if (mMode == 0) {
            mMode = ScanConstant.REQUEST_SCAN_MODE_ALL;
        }
    }

    public void initView() {
        layout_container = (RelativeLayout) findViewById(R.id.layout_container);
        surfaceview = (SurfaceView) findViewById(R.id.surfaceview);
        layout_crop_view = (FrameLayout) findViewById(R.id.layout_crop_view);
        iv_scan_line = (ImageView) findViewById(R.id.iv_scan_line);
        tv_gallery = (TextView) findViewById(R.id.tv_gallery);
        tv_back = (TextView) findViewById(R.id.tv_back);
        tv_light = (TextView) findViewById(R.id.tv_light);
        tv_gallery.setOnClickListener(this);
        tv_back.setOnClickListener(this);
        tv_light.setOnClickListener(this);
    }


    @Override
    public void scanResult(Result rawResult, Bundle bundle) {
        Log.e("TAG", "rawResult==" + rawResult.getText());
        Intent resultIntent = new Intent();
        resultIntent.putExtras(bundle);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void scanError(Exception e) {
        String msg = e.getMessage();
        switch (msg) {
            case ScanConstant.OPEN_CAMERA_ERROR + "":
                Toast.makeText(this, "相机打开出错，请检查是否被禁止了该权限！", Toast.LENGTH_SHORT).show();
                break;
            case ScanConstant.OPEN_LOCAL_PHOTO_NULL + "":
                Toast.makeText(this, "找不到图片", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                break;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanManager.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int ids = v.getId();
        if (ids == R.id.tv_gallery) {
            showPictures(ScanConstant.PHOTO_REQUEST_CODE);
        } else if (ids == R.id.tv_back) {
            finish();
        } else if (ids == R.id.tv_light) {
            scanManager.switchLight();
        }
    }

    public void showPictures(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photo_path;
        if (resultCode == RESULT_OK && requestCode == ScanConstant.PHOTO_REQUEST_CODE) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = this.getContentResolver().query(data.getData(), proj, null, null, null);
            if (cursor.moveToFirst()) {
                int colum_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                photo_path = cursor.getString(colum_index);
                if (photo_path == null) {
                    photo_path = Utils.getPath(getApplicationContext(), data.getData());
                }
                Log.e("TAG", "photo_path==" + photo_path);
                scanManager.scanningImage(photo_path);
            }
        }
    }
}
