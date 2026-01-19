package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        findViewById(R.id.logoText)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.neon_pulse));

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, PerformanceActivity.class));
            finish();
        }, 2500);
    }
}