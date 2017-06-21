package com.scan.zxing.base;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.scan.R;
import com.scan.zxing.encoding.EncodingUtils;

public class CreateQrActivity extends Activity implements View.OnClickListener {
    private ImageView iv_qr;
    private EditText et_content;
    private TextView tv_create_qr, tv_create_bar, tv_addhead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_create);
        initView();
    }

    public void initView() {
        iv_qr = (ImageView) findViewById(R.id.iv_qr);
        et_content = (EditText) findViewById(R.id.et_content);
        tv_create_qr = (TextView) findViewById(R.id.tv_create_qr);
        tv_create_bar = (TextView) findViewById(R.id.tv_create_bar);
        tv_addhead = (TextView) findViewById(R.id.tv_addhead);
        tv_create_qr.setOnClickListener(this);
        tv_create_bar.setOnClickListener(this);
        tv_addhead.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int ids = v.getId();
        if (ids == R.id.tv_create_qr) {
            createQr();
        } else if (ids == R.id.tv_create_bar) {
            createBar();
        } else if (ids == R.id.tv_addhead) {
            addhead();
        }
    }

    public void createQr() {
        String contentStr = et_content.getText().toString().trim();
        if (TextUtils.isEmpty(contentStr)) {
            Toast.makeText(this, "空", Toast.LENGTH_SHORT);
            return;
        }
        createQrCode(contentStr, 200, 200, null);
    }

    public void createBar() {
        String contentStr = et_content.getText().toString().trim();
        if (TextUtils.isEmpty(contentStr)) {
            Toast.makeText(this, "空", Toast.LENGTH_SHORT);
            return;
        }
        createBarCode(contentStr, 400, 100);
    }

    public void addhead() {
        String contentStr = et_content.getText().toString().trim();
        if (TextUtils.isEmpty(contentStr)) {
            Toast.makeText(this, "空", Toast.LENGTH_SHORT);
            return;
        }
        createQrCode(contentStr, 200, 200, getHeadBitmap(100));
    }

    public void createBarCode(String key, int width, int height) {
        Bitmap barCode = EncodingUtils.createBarCode(key, width, height);
        if (barCode == null) {
            Toast.makeText(this, "输入的内容条形码不支持！", Toast.LENGTH_SHORT).show();
        } else {
            iv_qr.setImageBitmap(barCode);
        }
    }

    /**
     * 生成二维码
     */
    private void createQrCode(String key, int width, int height, Bitmap logoBm) {
        Bitmap qrCode = EncodingUtils.createQRCode(key, width, height, logoBm);
        if (qrCode == null) {
            Toast.makeText(this, "输入的内容二维码不支持！", Toast.LENGTH_SHORT).show();
        } else {
            iv_qr.setImageBitmap(qrCode);
        }
    }

    /**
     * 初始化头像图片
     */
    private Bitmap getHeadBitmap(int size) {
        try {
            Bitmap portrait = BitmapFactory.decodeResource(getResources(), R.drawable.head);
            // 对原有图片压缩显示大小
            Matrix mMatrix = new Matrix();
            float width = portrait.getWidth();
            float height = portrait.getHeight();
            mMatrix.setScale(size / width, size / height);
            return Bitmap.createBitmap(portrait, 0, 0, (int) width, (int) height, mMatrix, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
