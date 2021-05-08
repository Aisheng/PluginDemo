package com.hanami.hook;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author lidaisheng
 * @date 2021-04-28
 */
public class ProxyActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxy_activity_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
