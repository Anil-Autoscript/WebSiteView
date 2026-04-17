package com.webviewer.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);
        View progressBar = findViewById(R.id.splash_progress);

        // Animate logo scale + fade in
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        scaleX.setDuration(700);
        scaleY.setDuration(700);
        fadeIn.setDuration(700);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(scaleX, scaleY, fadeIn);

        // Animate title
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
        ObjectAnimator titleTranslate = ObjectAnimator.ofFloat(title, "translationY", 40f, 0f);
        titleFade.setDuration(500);
        titleTranslate.setDuration(500);
        AnimatorSet titleAnim = new AnimatorSet();
        titleAnim.playTogether(titleFade, titleTranslate);
        titleAnim.setStartDelay(400);

        // Animate subtitle
        ObjectAnimator subFade = ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f);
        subFade.setDuration(400);
        subFade.setStartDelay(700);

        // Animate progress
        ObjectAnimator progFade = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progFade.setDuration(300);
        progFade.setStartDelay(900);

        logoAnim.start();
        titleAnim.start();
        subFade.start();
        progFade.start();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, 2200);
    }
}
