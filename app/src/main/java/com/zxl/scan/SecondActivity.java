package com.zxl.scan;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.scan.zxing.base.CreateQrActivity;
import com.scan.zxing.base.ScanActivity;
import com.scan.zxing.base.ScanConstant;

public class SecondActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTvScan, mTvCreate;
    private EditText et_result;
    private ImageView iv_result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        initView();
    }

    public void initView() {
        mTvScan = (TextView) findViewById(R.id.tv_scan);
        mTvCreate = (TextView) findViewById(R.id.tv_create);
        et_result = (EditText) findViewById(R.id.et_result);
        iv_result = (ImageView) findViewById(R.id.iv_result);
        mTvScan.setOnClickListener(this);
        mTvCreate.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_scan:
                clickTv1();
                break;
            case R.id.tv_create:
                clickTv2();
                break;
        }
    }

    public void clickTv1() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivityForResult(intent, ScanConstant.SCAN_RESULT);
    }

    public void clickTv2() {
        Intent intent = new Intent(this, CreateQrActivity.class);
        startActivityForResult(intent, ScanConstant.CREATE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ScanConstant.SCAN_RESULT) {
                et_result.setVisibility(View.VISIBLE);
                iv_result.setVisibility(View.GONE);
                Bundle bundle = data.getExtras();
                String scanResult = bundle.getString("result");
                et_result.setText(scanResult);
            } else if (requestCode == ScanConstant.CREATE_RESULT) {
                et_result.setVisibility(View.GONE);
                iv_result.setVisibility(View.VISIBLE);
            } else {
                et_result.setVisibility(View.GONE);
                iv_result.setVisibility(View.GONE);
            }
        }
    }
}
