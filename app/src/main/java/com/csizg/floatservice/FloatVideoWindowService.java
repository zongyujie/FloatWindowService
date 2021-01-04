package com.csizg.floatservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * 显示缩小视频画面的service
 *
 * @author zyj
 */
public class FloatVideoWindowService extends Service {
    private WindowManager mWindowManager;

    private WindowManager.LayoutParams wmParams;

    private LayoutInflater inflater;

    // 浮动布局
    private View mFloatingLayout;

    //容器父布局
    private LinearLayout smallSizePreviewLayout;

    /**
     * 显示时间
     */
    private TextView time;

    /**
     * 记录麦克风状态值
     */
    private boolean isShowVoice;

    private CallBack callback = null;

    private volatile static FloatVideoWindowService service;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public FloatVideoWindowService getService() {
            return FloatVideoWindowService.this;
        }

        //更新时间
        public void setData(String data) {
            time.setText(data);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        //设置悬浮窗基本参数（位置、宽高等）
        initWindow();
        //悬浮框点击事件的处理
        initFloating();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        service = null;
        smallSizePreviewLayout.removeAllViews();
        if (mFloatingLayout != null) {
            // 移除悬浮窗口
            mWindowManager.removeView(mFloatingLayout);
        }
    }

    /**
     * 设置悬浮框基本参数（位置、宽高等）
     */
    private void initWindow() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //设置好悬浮窗的参数
        wmParams = getParams();
        // 悬浮窗默认显示以左上角为起始坐标
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
        wmParams.x = 70;
        wmParams.y = 210;
        //此方法解决悬浮窗背景没有圆角的问题
        wmParams.format = PixelFormat.RGBA_8888;
        //得到容器，通过这个inflater来获得悬浮窗控件
        inflater = LayoutInflater.from(TheApplication.getAppContext());
        // 获取浮动窗口视图所在布局
        mFloatingLayout = inflater.inflate(R.layout.alert_float_video_layout, null);
        // 添加悬浮窗的视图
        mWindowManager.addView(mFloatingLayout, wmParams);
    }

    private WindowManager.LayoutParams getParams() {
        wmParams = new WindowManager.LayoutParams();
        //设置window type 下面变量2002是在屏幕区域显示，2003则可以显示在状态栏之上
//        wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        //设置可以显示在状态栏上
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                         WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                         WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return wmParams;
    }

    private void initFloating() {
        smallSizePreviewLayout = mFloatingLayout.findViewById(R.id.small_size_preview);
        time = mFloatingLayout.findViewById(R.id.tv_time);
        //悬浮框点击事件
        smallSizePreviewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //在这里实现点击重新回到Activity
                Intent intent = new Intent(TheApplication.getAppContext(), FloatActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                TheApplication.getAppContext().startActivity(intent);
            }
        });
        //悬浮框触摸事件，设置悬浮框可拖动
        smallSizePreviewLayout.setOnTouchListener(new FloatingListener());
    }

    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private int mTouchStartX, mTouchStartY, mTouchCurrentX, mTouchCurrentY;

    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private int mStartX, mStartY, mStopX, mStopY;

    //判断悬浮窗口是否移动，这里做个标记，防止移动后松手触发了点击事件
    private boolean isMove;

    private class FloatingListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isMove = false;
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mStartX = (int) event.getRawX();
                    mStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mTouchCurrentX = (int) event.getRawX();
                    mTouchCurrentY = (int) event.getRawY();
                    wmParams.x += mTouchCurrentX - mTouchStartX;
                    wmParams.y += mTouchCurrentY - mTouchStartY;
                    mWindowManager.updateViewLayout(mFloatingLayout, wmParams);

                    mTouchStartX = mTouchCurrentX;
                    mTouchStartY = mTouchCurrentY;
                    break;
                case MotionEvent.ACTION_UP:
                    mStopX = (int) event.getRawX();
                    mStopY = (int) event.getRawY();
                    //判断移动距离大于20就算移动，否则算是悬浮窗的点击事件
                    if (Math.abs(mStartX - mStopX) >= 20 || Math.abs(mStartY - mStopY) >= 20) {
                        isMove = true;
                    }
                    break;
                default:
                    break;
            }

            //如果是移动事件不触发OnClick事件，防止移动的时候一放手形成点击事件
            return isMove;
        }
    }

    public interface CallBack {
        /**
         * 改变麦克风状态并回调给activity的麦克风状态
         *
         * @param voiceState 麦克风状态
         */
        void onChangedVoiceState(boolean voiceState);
    }

    /**
     * 服务是否存活
     *
     * @return true: 存活
     */
    public static boolean isServiceRunning() {
        return service != null;
    }
}