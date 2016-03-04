package com.example.zhang.test3;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

//后台服务程序
public class MyService extends Service {

    private int count;

    int mCommand;

    boolean quit = false;

    AudioManager mAudioManager;
    TelephonyManager mTelephonyManager;


    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("I get!" + intent.getExtras().getInt("Command"));
            mCommand = intent.getExtras().getInt("Command");

            //播放启动音乐器及接听电话
            if (mCommand == 1) {
                if (mTelephonyManager.getCallState() ==
                        TelephonyManager.CALL_STATE_RINGING) {
                    try {
                        Method method = Class.forName(
                                "android.os.ServiceManager")
                                .getMethod("getService"
                                        , String.class);
                        // 获取远程TELEPHONY_SERVICE的IBinder对象的代理
                        IBinder binder = (IBinder) method.invoke(null,
                                new Object[]{Service.TELEPHONY_SERVICE});
                        // 将IBinder对象的代理转换为ITelephony对象
                        ITelephony telephony = ITelephony.Stub
                                .asInterface(binder);
                        System.out.println("myAnswerRingingCall()自动接通电话");
                        myAnswerRingingCall();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                long eventtime = SystemClock.uptimeMillis();

                            /*PLAY*/
                Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent downEvent = new KeyEvent(eventtime, eventtime,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                sendOrderedBroadcast(downIntent, null);

                Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                KeyEvent upEvent = new KeyEvent(eventtime, eventtime,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
                upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                sendOrderedBroadcast(upIntent, null);
            }

            //负责挂断电话和音乐的暂停（挂断电话有两种情况，一种是打来直接挂断，一种是通话完毕进行挂断）
            if (mCommand == 3) {
                System.out.println("123");

                if (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                    System.out.println("1234");
                    try {
                        Method method = Class.forName(
                                "android.os.ServiceManager")
                                .getMethod("getService"
                                        , String.class);
                        // 获取远程TELEPHONY_SERVICE的IBinder对象的代理
                        IBinder binder = (IBinder) method.invoke(null,
                                new Object[]{Service.TELEPHONY_SERVICE});
                        // 将IBinder对象的代理转换为ITelephony对象
                        ITelephony telephony = ITelephony.Stub
                                .asInterface(binder);
                        // 挂断电话
                        System.out.println("12345*");
                        telephony.endCall();
                        System.out.println("telephony.endCall()挂断电话");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
                    System.out.println("占1234");
                    try {
                        Method method = Class.forName(
                                "android.os.ServiceManager")
                                .getMethod("getService"
                                        , String.class);
                        // 获取远程TELEPHONY_SERVICE的IBinder对象的代理
                        IBinder binder = (IBinder) method.invoke(null,
                                new Object[]{Service.TELEPHONY_SERVICE});
                        // 将IBinder对象的代理转换为ITelephony对象
                        ITelephony telephony = ITelephony.Stub
                                .asInterface(binder);
                        // 挂断电话
                        System.out.println("12345*");
                        telephony.endCall();
                        System.out.println("telephony.endCall()挂断电话");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mAudioManager.isMusicActive()) {
                    System.out.println("123456");
                    long eventtime = SystemClock.uptimeMillis();

                            /*PAUSE*/
                    Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent downEvent = new KeyEvent(eventtime, eventtime,
                            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                    downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                    sendOrderedBroadcast(downIntent, null);

                    Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                    upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                    sendOrderedBroadcast(upIntent, null);
                }
            }

            //上一首歌
            if (mCommand == 4) {
                System.out.println("#");

                System.out.println("PREVIOUS");
                if (mAudioManager.isMusicActive()) {
                    System.out.println("***");
                    long eventtime = SystemClock.uptimeMillis();

                    Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                    downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                    sendOrderedBroadcast(downIntent, null);

                    Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                    upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                    sendOrderedBroadcast(upIntent, null);
                }
            }

            //下一首歌
            if (mCommand == 2) {
                if (mAudioManager.isMusicActive()) {
                    long eventtime = SystemClock.uptimeMillis();

                    Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                    downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
                    sendOrderedBroadcast(downIntent, null);

                    Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                    KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                    upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
                    sendOrderedBroadcast(upIntent, null);
                }
            }

        }
    };

    IntentFilter mIntentFilter = new IntentFilter("MyCommand");

    @Override
    public void onCreate() {

        registerReceiver(mBroadcastReceiver, mIntentFilter);

        //获取音频管理
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //获取通话管理
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);

    }

    //接电话
    private void myAnswerRingingCall() {

        //放开耳机按钮
        Intent localIntent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent localKeyEvent2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
        localIntent3.putExtra("android.intent.extra.KEY_EVENT", localKeyEvent2);
        sendOrderedBroadcast(localIntent3, "android.permission.CALL_PRIVILEGED");

        //插耳机
        Intent localIntent1 = new Intent(Intent.ACTION_HEADSET_PLUG);
        localIntent1.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        localIntent1.putExtra("state", 1);
        localIntent1.putExtra("microphone", 1);
        localIntent1.putExtra("name", "Headset");
        sendOrderedBroadcast(localIntent1, "android.permission.CALL_PRIVILEGED");
        //按下耳机按钮
        Intent localIntent2 = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent localKeyEvent1 = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK);
        localIntent2.putExtra("android.intent.extra.KEY_EVENT", localKeyEvent1);
        sendOrderedBroadcast(localIntent2, "android.permission.CALL_PRIVILEGED");
        //放开耳机按钮
        localIntent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);
        localKeyEvent2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
        localIntent3.putExtra("android.intent.extra.KEY_EVENT", localKeyEvent2);
        sendOrderedBroadcast(localIntent3, "android.permission.CALL_PRIVILEGED");
        //拔出耳机
        Intent localIntent4 = new Intent(Intent.ACTION_HEADSET_PLUG);
        localIntent4.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        localIntent4.putExtra("state", 0);
        localIntent4.putExtra("microphone", 1);
        localIntent4.putExtra("name", "Headset");
        sendOrderedBroadcast(localIntent4, "android.permission.CALL_PRIVILEGED");

    }

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
