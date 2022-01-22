package com.rommelbendel.scanQ.appIntro;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.airbnb.lottie.LottieAnimationView;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.ShadowTransformer;
import com.rommelbendel.scanQ.additional.TinyDB;

import java.util.Timer;

public class AppIntro extends AppCompatActivity implements View.OnClickListener {

    private ViewPager mViewPager;
    private CardPagerAdapter mCardAdapter;
    private ShadowTransformer mCardShadowTransformer;

    private static LottieAnimationView lottieScale;
    boolean isCheckDone = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_intro);
        TinyDB tb = new TinyDB(getApplicationContext());

        mViewPager = findViewById(R.id.viewPager);
        lottieScale = findViewById(R.id.lottie_max_min);
        lottieScale.setOnClickListener(this);

        mCardAdapter = new CardPagerAdapter(this);
        mCardAdapter.addCardItem(new CardItem(R.id.info_ll1));
        mCardAdapter.addCardItem(new CardItem(R.id.info_ll2));
        mCardAdapter.addCardItem(new CardItem(R.id.info_ll3));
        mCardAdapter.addCardItem(new CardItem(R.id.info_ll4));
        if(tb.getString("username").trim().length() == 0)
            mCardAdapter.addCardItem(new CardItem(R.id.info_ll5));

        mCardShadowTransformer = new ShadowTransformer(mViewPager, mCardAdapter);

        mViewPager.setAdapter(mCardAdapter);
        mViewPager.setPageTransformer(false, mCardShadowTransformer);
        mViewPager.setOffscreenPageLimit(3);

        mCardShadowTransformer.enableScaling(true);
    }

    @Override
    public void onClick(View v) {

        if (isCheckDone) {
            //since lottie animation checked_done.json has only one animation for checked,
            // so for unchecking animation, we will play reverse animation
            lottieScale.setSpeed(-0.9f);
            lottieScale.playAnimation();
            isCheckDone = false;
        } else {
            lottieScale.setSpeed(0.9f);
            lottieScale.playAnimation();
            isCheckDone = true;
        }

        Timer t = new Timer();
        t.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                mCardShadowTransformer.enableScaling(isCheckDone);
                t.cancel();
            }
        }, 400);
    }
}
