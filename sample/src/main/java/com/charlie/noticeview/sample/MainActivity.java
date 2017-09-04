package com.charlie.noticeview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout ll_notice;
    private ImageView iv_close_notice;
    private TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ll_notice = (LinearLayout) findViewById(R.id.ll_notice);
        iv_close_notice = (ImageView) findViewById(R.id.iv_close_notice);
        tv_content = (TextView) findViewById(R.id.tv_content);

        iv_close_notice.setOnClickListener(this);
        tv_content.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_close_notice:
                ll_notice.setVisibility(View.GONE);
                break;
            case R.id.tv_content:
                if (!ll_notice.isShown()) {
                    ll_notice.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
}
