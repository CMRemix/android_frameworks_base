/*
 * Copyright (C) 2016 AllianceROM, ~Morningstar
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

package com.android.internal.util.cmremix;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.util.CMRemixUtils;

/**
 * @hide
 */
public class DexoptDialog extends Dialog {

    private final Context mContext;
    private final PackageManager mPackageManager;

    private ImageView mAppIcon;
    private TextView mPrimaryText;
    private TextView mPackageName;
    private ProgressBar mProgress;

    private boolean mWasApk;

    private int mTotal;

    private ImageView mLogo;
    private ImageView mLogoShadow;
    private ImageView mLogoText;
    private ImageView mLogoTextShadow;

    private RelativeLayout mAppIconFrame;

    public static DexoptDialog create(Context context) {
        return create(context,  WindowManager.LayoutParams.TYPE_BOOT_PROGRESS);
    }

    public static DexoptDialog create(Context context, int windowType) {
        final PackageManager pm = context.getPackageManager();
        final int theme = com.android.internal.R.style.Theme_Material_Light;
        return new DexoptDialog(context, theme, windowType);
    }

    private DexoptDialog(Context context, int themeResId, int windowType) {
        super(context, themeResId);
        mContext = context;
        mPackageManager = context.getPackageManager();

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View rootView = inflater.inflate(com.android.internal.R.layout.dexopt_layout, null, false);
        mPrimaryText = (TextView) rootView.findViewById(R.id.dexopt_message);
        mPackageName = (TextView) rootView.findViewById(R.id.dexopt_message_detail);
        mAppIcon = (ImageView) rootView.findViewById(R.id.dexopt_icon);
        mProgress = (ProgressBar) rootView.findViewById(R.id.dexopt_progress);
        mLogo = (ImageView) rootView.findViewById(R.id.dexopt_logo);
        mLogoShadow = (ImageView) rootView.findViewById(R.id.dexopt_logo_shadow);
        mLogoText = (ImageView) rootView.findViewById(R.id.dexopt_logo_text);
        mLogoTextShadow = (ImageView) rootView.findViewById(R.id.dexopt_logo_text_shadow);
        mAppIconFrame = (RelativeLayout) rootView.findViewById(R.id.dexopt_icon_frame);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(rootView);

        if (windowType != 0) {
            getWindow().setType(windowType);
        }
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final WindowManager.LayoutParams lp = getWindow().getAttributes();
        // turn off button lights when dexopting
        lp.buttonBrightness = 0;
        lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        getWindow().setAttributes(lp);
        setCancelable(false);
        show();

        rootView.post(new Runnable() {
            @Override public void run() {
                mAppIcon.setImageDrawable(null);

                // start the marquee
                mPrimaryText.setSelected(true);
                mPackageName.setSelected(true);
            }
        });
        startLogoAnimation();
    }

    public void setProgress(final ApplicationInfo info, final int current, final int total) {
        boolean isApk = false;
        String msg = "";

        // if we initialized with an invalid total, get it from the valid dexopt messages
        if (mTotal != total && total > 0) {
            mTotal = total;
            mProgress.setMax(mTotal);
        }

        if (info == null) {
            if (current == Integer.MIN_VALUE) {
                msg = mContext.getResources().getString(com.android.internal.R.string.android_upgrading_starting_apps);
                mAppIconFrame.setVisibility(View.GONE);
            } else if (current == (Integer.MIN_VALUE + 1)) {
                msg = mContext.getResources().getString(com.android.internal.R.string.android_upgrading_fstrim);
            } else if (current == (Integer.MIN_VALUE + 3)) {
                msg = mContext.getResources().getString(com.android.internal.R.string.android_upgrading_complete);
            }
        } else if (current == (Integer.MIN_VALUE + 2)) {
            final CharSequence label = info.loadLabel(mContext.getPackageManager());
            msg = mContext.getResources().getString(com.android.internal.R.string.android_preparing_apk, label);
        } else {
            isApk = true;
            msg = mContext.getResources().getString(com.android.internal.R.string.android_upgrading_apk, current, total);
            mProgress.setProgress(current);
            if ((current + 1) <= total) {
                mProgress.setSecondaryProgress(current + 1);
            }
        }

        // check if the state has changed
        if (mWasApk != isApk) {
            mWasApk = isApk;
            if (isApk) {
                mPackageName.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mPackageName.setVisibility(View.GONE);
                mProgress.setVisibility(View.INVISIBLE);
            }
        }

        // if we are processing an apk, load its icon and set the message details
        if (isApk) {
            mAppIcon.setImageDrawable(info.loadIcon(mPackageManager));
            mPackageName.setText(String.format("(%s)", info.packageName));
        } else {
            mAppIcon.setImageDrawable(null);
        }
        mPrimaryText.setText(msg);
    }

    // This dialog will consume all events coming in to
    // it, to avoid it trying to do things too early in boot.

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return true;
    }

    private void startLogoAnimation() {
        setInitialState();

        final AnimationSet shadowSet = new AnimationSet(false);
        shadowSet.setRepeatCount(Animation.INFINITE);
        shadowSet.setRepeatMode(Animation.REVERSE);

        Animation alphaAnim = new AlphaAnimation(1.0f, 0.5f);
        alphaAnim.setDuration(2500);
        alphaAnim.setInterpolator(new LinearInterpolator());
        alphaAnim.setRepeatCount(Animation.INFINITE);
        alphaAnim.setRepeatMode(Animation.REVERSE);
        shadowSet.addAnimation(alphaAnim);

        Animation scaleAnim = new ScaleAnimation(1f, 0.7f, 1f, 0.7f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1.0f);
        scaleAnim.setDuration(2500);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setRepeatCount(Animation.INFINITE);
        scaleAnim.setRepeatMode(Animation.REVERSE);
        shadowSet.addAnimation(scaleAnim);

        final Animation transAnim = new TranslateAnimation(0, 0, 0, -CMRemixUtils.dpToPx(mContext, 30));
        transAnim.setDuration(2500);
        transAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        transAnim.setRepeatMode(Animation.REVERSE);
        transAnim.setRepeatCount(Animation.INFINITE);

        AnimationSet dropShadowSet = new AnimationSet(true);
        dropShadowSet.setInterpolator(new BounceInterpolator());
        dropShadowSet.setFillAfter(true);

        Animation dropAlpha = new AlphaAnimation(0.0f, 1.0f);
        dropAlpha.setDuration(2000);
        dropShadowSet.addAnimation(dropAlpha);

        Animation dropScale = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1.0f);
        dropScale.setDuration(2000);
        dropShadowSet.addAnimation(dropScale);

        Animation dropAnim = new TranslateAnimation(0, 0, -CMRemixUtils.dpToPx(mContext, 300), 0);
        dropAnim.setDuration(2000);
        dropAnim.setInterpolator(new BounceInterpolator());
        dropAnim.setFillAfter(true);
        dropAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //nothing
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mLogoShadow.startAnimation(shadowSet);
                mLogo.startAnimation(transAnim);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
                //nothing
            }
        });

        mLogo.startAnimation(dropAnim);
        mLogoShadow.startAnimation(dropShadowSet);
    }

    public void startFinalAnimation(final Animation.AnimationListener listener) {
        final AnimationSet animOutSet = new AnimationSet(true);
        animOutSet.setInterpolator(new LinearInterpolator());
        animOutSet.setDuration(2000);
        animOutSet.setFillAfter(true);
        animOutSet.setAnimationListener(listener);

        Animation alphaOutAnim = new AlphaAnimation(1.0f, 0.0f);
        alphaOutAnim.setDuration(2000);
        alphaOutAnim.setFillAfter(true);
        animOutSet.addAnimation(alphaOutAnim);

        Animation scaleOutAnim = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleOutAnim.setDuration(2000);
        scaleOutAnim.setFillAfter(true);
        animOutSet.addAnimation(scaleOutAnim);

        final AnimationSet animInSet = new AnimationSet(false);
        animInSet.setDuration(2500);
        animInSet.setFillAfter(true);

        Animation alphaInAnim = new AlphaAnimation(0.0f, 1.0f);
        alphaInAnim.setDuration(2500);
        alphaInAnim.setInterpolator(new LinearInterpolator());
        alphaInAnim.setFillAfter(true);
        animInSet.addAnimation(alphaInAnim);

        Animation scaleInAnim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleInAnim.setDuration(2500);
        scaleInAnim.setInterpolator(new OvershootInterpolator());
        scaleInAnim.setFillAfter(true);
        animInSet.addAnimation(scaleInAnim);

        animInSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //nothing
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                animInSet.cancel();
                mLogoText.startAnimation(animOutSet);
                mLogoTextShadow.startAnimation(animOutSet);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
                //nothing
            }
        });

        mLogoText.startAnimation(animInSet);
        mLogoTextShadow.startAnimation(animInSet);
    }

    private void setInitialState() {
        Animation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
        alphaAnim.setDuration(0);
        alphaAnim.setFillAfter(true);

        Animation transAnim = new TranslateAnimation(0, 0, 0, -CMRemixUtils.dpToPx(mContext, 300));
        transAnim.setDuration(0);
        transAnim.setFillAfter(true);

        Animation scaleAnim = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1.0f);
        scaleAnim.setDuration(0);
        scaleAnim.setFillAfter(true);

        AnimationSet shadowSet = new AnimationSet(true);
        shadowSet.addAnimation(alphaAnim);
        shadowSet.addAnimation(scaleAnim);

        mLogoText.startAnimation(alphaAnim);
        mLogoTextShadow.startAnimation(alphaAnim);
        mLogo.startAnimation(transAnim);
        mLogoShadow.startAnimation(shadowSet);
    }
}
