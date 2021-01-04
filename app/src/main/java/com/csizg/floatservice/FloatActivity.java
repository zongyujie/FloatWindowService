package com.csizg.floatservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 启动模式需要更改为单例singleInstance
 *
 * @author zyj
 */
public class FloatActivity extends AppCompatActivity {
    private ImageView mini, back;

    private TextView time;

    private Handler handler = new Handler();

    public boolean isConnected;

    private FloatVideoWindowService.MyBinder mBinder;

    private boolean isShowFloat = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float);
        initView();
        handler.postDelayed(timeRun, 1000);
    }

    private void initView() {
        mini = findViewById(R.id.iv_mini);
        time = findViewById(R.id.tv_time);
        back = findViewById(R.id.iv_back);
        mini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示悬浮窗
                openFloatWindow();
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShowFloat = false;
                finish();
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isConnected) {
            //不显示悬浮框
            unbindService(mVideoServiceConnection);
            isConnected = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isShowFloat) {
            openFloatWindow();
        }
    }

    /**
     * 最小化当前密会页面，打开悬浮窗服务
     */
    private void openFloatWindow() {
        Intent intent = new Intent(this, FloatVideoWindowService.class);
        isConnected = bindService(intent, mVideoServiceConnection, Context.BIND_AUTO_CREATE);
        //最小化Activity
        moveTaskToBack(true);
    }

    ServiceConnection mVideoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 获取服务的操作对象
            mBinder = (FloatVideoWindowService.MyBinder) service;
            //更新悬浮窗时间
            mBinder.setData(parseTimeSeconds(timeDate));
            // 悬浮窗回调给activity更新麦克风状态的方法
            mBinder.getService().setCallback(new FloatVideoWindowService.CallBack() {
                @Override
                public void onChangedVoiceState(boolean voiceState) {

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private Runnable timeRun = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            handler.postDelayed(timeRun, 1000);
        }
    };

    private int timeDate = 0;

    private void updateTimer() {
        timeDate++;
        time.setText(parseTimeSeconds(timeDate));
        if (mBinder != null) {
            //更新悬浮窗时间
            mBinder.setData(parseTimeSeconds(timeDate));
        }
    }

    public static String parseTimeSeconds(int t) {
        String format = "%02d:%02d";
        String formatHour = "%02d:%02d:%02d";
        int seconds = t % 60;
        int m = t / 60;
        int minutes = m % 60;
        int hours = m / 60;
        if (hours > 0) {
            return String.format(formatHour, hours, minutes, seconds);
        } else {
            return String.format(format, minutes, seconds);
        }
    }

    //禁止返回键关闭此页面
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }
}
