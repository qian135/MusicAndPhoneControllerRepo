package com.example.zhang.test3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

    BluetoothAdapter bluetoothAdapter;
    TGDevice tgDevice;
    int subjectContactQuality_last;
    int subjectContactQuality_cnt;

    final boolean rawEnabled = true;

    //算法处理的相关数据
    double task_famil_baseline, task_famil_cur, task_famil_change;
    boolean task_famil_first;
    double task_diff_baseline, task_diff_cur, task_diff_change;
    boolean task_diff_first;

    int Command = 0;          //控制命令
    int Count = 0;
    int m_Count = 0;
    int Flag = 0;
    int attention = 0;        //注意力值，1秒钟1个
    int meditation = 0;       //冥想度值，1秒钟1个
    int m_rawData[] = new int[516];   //画图使用的原始信号
    int rawData[] = new int[516];     //算法处理的原始信号
    int raw_Quality = 200;
    int BaseNum = 250;
    int SpeedNum = 0;

    //输出调试信息
    ScrollView sv;
    TextView tv;

    boolean buttonFlag = true;

    //连接和发送命令按钮
    Button mConnect, mButton1;

    private MyHandler mMyHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMyHandler = new MyHandler(this);
        
        sv = (ScrollView) findViewById(R.id.scrollView1);
        tv = (TextView) findViewById(R.id.textView1);

        //启动后台服务，使用户即使退出APP，也能进行相关的控制功能
        Intent intent = new Intent(MainActivity.this, MyService.class);
        startService(intent);

        //参数的具体含义我不知道
        subjectContactQuality_last = -1;
        subjectContactQuality_cnt = 200;

        // 检查手机是否支持蓝牙
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            //finish();
            return;
        } else {
            tgDevice = new TGDevice(bluetoothAdapter, mMyHandler);
        }

        //参数意思不知道
        task_famil_baseline = task_famil_cur = task_famil_change = 0.0;
        task_famil_first = true;
        task_diff_baseline = task_diff_cur = task_diff_change = 0.0;
        task_diff_first = true;

        //进行连接
        mConnect = (Button) findViewById(R.id.connectButton);
        mConnect.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (buttonFlag) {
                    tgDevice.connect(true);
                    mConnect.setText("断开连接");
                    buttonFlag = false;
                } else {
                    tgDevice.close();
                    mConnect.setText("点击连接");
                    buttonFlag = true;
                }
            }
        });


        //发送命令
        mButton1 = (Button) findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("MyCommand");
                intent.putExtra("Command", 1);
                sendBroadcast(intent);
            }
        });

    }


    /**
     * 实时的处理设备的数据
     * MSG_POOR_SIGNAL达到0才进行原始信号处理（0-200 ， 200最差 ,0最好）
     * 主要处理MSG_RAW_DATA，将达到阈值的数据放到算法中进行计算。
     * 将MSG_ATTENTION发送到MyView
     * 将MSG_MEDITATION发送到MyView
     */
    public class MyHandler extends Handler {

        WeakReference<MainActivity> mMainActivityWeakReference;

        public MyHandler(MainActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case TGDevice.MSG_MODEL_IDENTIFIED:
                    /*
            		 * now there is something connected,
            		 * time to set the configurations we need
            		 */
                    tv.append("Model Identified\n");
                    tgDevice.setBlinkDetectionEnabled(true);
                    tgDevice.setTaskDifficultyRunContinuous(true);
                    tgDevice.setTaskDifficultyEnable(true);
                    tgDevice.setTaskFamiliarityRunContinuous(true);
                    tgDevice.setTaskFamiliarityEnable(true);
                    tgDevice.setRespirationRateEnable(true); /// not allowed on EEG hardware, here to show the override message
                    break;

                case TGDevice.MSG_STATE_CHANGE:

                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
//    	                	tv.append( "Connecting...\n" );
                            break;
                        case TGDevice.STATE_CONNECTED:
                            tv.append("Connected.\n");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            tv.append("Could not connect to any of the paired BT devices.  Turn them on and try again.\n");
                            tv.append("Bluetooth devices must be paired 1st\n");
                            break;
                        case TGDevice.STATE_ERR_NO_DEVICE:
                            tv.append("No Bluetooth devices paired.  Pair your device and try again.\n");
                            break;
                        case TGDevice.STATE_ERR_BT_OFF:
                            tv.append("Bluetooth is off.  Turn on Bluetooth and try again.");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            tv.append("Disconnected.\n");
                    } /* end switch on msg.arg1 */

                    break;

                case TGDevice.MSG_POOR_SIGNAL:
                    raw_Quality = msg.arg1;
                	/* display signal quality when there is a change of state, or every 30 reports (seconds) */
                	/*
                	if (subjectContactQuality_cnt >= 30 || msg.arg1 != subjectContactQuality_last) {
                		if (msg.arg1 == 0) tv.append( "SignalQuality: is Good: " + msg.arg1 + "\n" );
                		else tv.append( "SignalQuality: is POOR: " + msg.arg1 + "\n" );

                		subjectContactQuality_cnt = 0;
                		subjectContactQuality_last = msg.arg1;
                	}
                	else subjectContactQuality_cnt++;
                	*/
                    break;

                case TGDevice.MSG_RAW_DATA:
                    /////

                    if (raw_Quality == 0) {

                        int rawdata = msg.arg1;
                        m_rawData[m_Count] = rawdata;
                        m_Count++;
                        if (Flag == 0) {
                            if (rawdata >= BaseNum) {
                                rawData[Count] = rawdata;
                                Count++;
                                Flag++;
                            }
                        } else {
                            rawData[Count] = rawdata;
                            Count++;
                        }
//                        System.out.println("*" + Count + "*");//
                        if (Count == 400) {
                            System.out.println("Algorithm被调用");//
                            Algorithm(rawData);
                            Count = 0;
                            Flag = 0;
                        }
                        if (m_Count == 512) {
//                            view.set_data(m_rawData);
                            m_Count = 0;
                        }
                    }

                    break;


                case TGDevice.MSG_ATTENTION:

                    attention = msg.arg1;
//                    view.set_attention(attention);
                    //tv.append( "Attention: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_MEDITATION:
                    meditation = msg.arg1;
//                    view.set_meditation(meditation);
                    //tv.append( "Meditation: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_EEG_POWER:
                    TGEegPower e = (TGEegPower) msg.obj;
                    //tv.append("delta: " + e.delta + " theta: " + e.theta + " alpha1: " + e.lowAlpha + " alpha2: " + e.highAlpha + "\n");
                    break;

                case TGDevice.MSG_FAMILIARITY:
                    //task_famil_cur = (Double) msg.obj;
                    //tv.append("task_familiarity"+task_famil_cur);
                    //if (task_famil_first) {
                    //	task_famil_first = false;
                    //}
                    //else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                		/*
                		task_famil_change = calcPercentChange(task_famil_baseline,task_famil_cur);
                		if (task_famil_change > 500.0 || task_famil_change < -500.0 ) {
                			tv.append( "     Familiarity: excessive range\n" );
                			//Log.i( "familiarity: ", "excessive range" );
                		}
                		else {
                			tv.append( "     Familiarity: " + task_famil_change + " %\n" );
                			//Log.i( "familiarity: ", String.valueOf( task_famil_change ) + "%" );
                		}
                		*/
                    //}
                    //task_famil_baseline = task_famil_cur;
                    break;
                case TGDevice.MSG_DIFFICULTY:
                    //task_diff_cur = (Double) msg.obj;
                    //tv.append("Difficulty"+task_diff_cur+"\n");
                	/*if (task_diff_first) {
                		task_diff_first = false;
                	}
                	else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                		/*
                		task_diff_change = calcPercentChange(task_diff_baseline,task_diff_cur);
                		if (task_diff_change > 500.0 || task_diff_change < -500.0 ) {
                			tv.append( "     Difficulty: excessive range %\n" );
                			//Log.i("difficulty: ", "excessive range" );
                		}
                		else {
                			tv.append( "     Difficulty: " +  task_diff_change + " %\n" );
                			//Log.i( "difficulty: ", String.valueOf( task_diff_change ) + "%" );
                		}

                	}
            		*/
                    //task_diff_baseline = task_diff_cur;
                    break;

                case TGDevice.MSG_ZONE:

                    switch (msg.arg1) {

                        case 3:
//                            view.setMaxCount(100);
//                            view.setCurrentCount(100);
                            //   tv.append( "          Zone: Elite\n" );
                            break;
                        case 2:
//                            view.setMaxCount(100);
//                            view.setCurrentCount(70);
                            //    tv.append( "          Zone: Intermediate\n" );
                            break;
                        case 1:
//                            view.setMaxCount(100);
//                            view.setCurrentCount(40);
                            //    tv.append( "          Zone: Beginner\n" );
                            break;
                        default:
                        case 0:
//                            view.setMaxCount(100);
//                            view.setCurrentCount(10);
                            //   tv.append( "          Zone: relax and try to focus\n" );
                            break;

                    }
                    break;

                case TGDevice.MSG_BLINK:
                    //tv.append( "Blink: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_ERR_CFG_OVERRIDE:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            //tv.append("Override: blinkDetect"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            //tv.append("Override: Familiarity"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            //tv.append("Override: Difficulty"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            //tv.append("Override: Positivity"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            //tv.append("Override: Resp Rate"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            //tv.append("Override: code: "+msg.arg1+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case TGDevice.MSG_ERR_NOT_PROVISIONED:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            //tv.append("No Support: blinkDetect"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            //tv.append("No Support: Familiarity"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            //tv.append("No Support: Difficulty"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            //tv.append("No Support: Positivity"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            //tv.append("No Support: Resp Rate"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            //tv.append("No Support: code: "+msg.arg1+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                default:
                    break;

            } /* end switch on msg.what */

            sv.fullScroll(View.FOCUS_DOWN);

        }
        
    }
    

    private double calcPercentChange(double baseline, double current) {
        double change;

        if (baseline == 0.0) baseline = 1.0; //don't allow divide by zero
		/*
		 * calculate the percentage change
		 */
        change = current - baseline;
        change = (change / baseline) * 1000.0 + 0.5;
        change = Math.floor(change) / 10.0;
        return (change);
    }

    //算法处理部分（准确度有待提高和改进）
    void Algorithm(int A[]) {

    	/*
    	OneBlink 1
    	OneBite  2
    	TwoBlink 3
    	TwoBite  4
    	LongBite 5
    	Others  6
    	*/
        int Command = 0;
        int CrossNum = 0;
        int CrossNum2 = 0;
        int SecondTaken = 0;
        int Secondi = 0;
        String Output[] = {"OneBlink", "OneBite", "TwoBlink", "TwoBite", "LongBite", "Others"};

        for (int i = 100; i < 300; i++) {
            if (A[i] >= 500) {
                SecondTaken = 1;
                Secondi = i;
                break;
            }
        }
        for (int i = 0; i < 100; i++) {
            if (A[i] >= 0 && A[i + 1] < 0 ||
                    A[i] <= 0 && A[i + 1] > 0)
                CrossNum++;
        }
        if (SecondTaken > 0) {
            for (int i = Secondi; i < Secondi + 100; i++) {
                if (A[i] >= 0 && A[i + 1] < 0 ||
                        A[i] <= 0 && A[i + 1] > 0)
                    CrossNum2++;
            }
        }
        if (CrossNum2 == 0) {
            if (CrossNum < 5 && CrossNum > 0)
                Command = 1;
            else if (CrossNum == 0)
                Command = 0;
            else
                Command = 2;
        } else {
            if (CrossNum < 5 && CrossNum2 < 5)
                Command = 3;
            else if (CrossNum >= 20 && CrossNum2 >= 20)
                Command = 5;
            else if (CrossNum >= 5 && CrossNum < 20 && CrossNum2 >= 5 && CrossNum2 < 20)
                Command = 4;
            else
                Command = 6;
        }

        if (Command > 0) {
            if (Command >= 1 && Command <= 6)
                tv.append("Command = " + Output[Command - 1] + "\n");
            Intent intent = new Intent();
            intent.setAction("MyCommand");
            intent.putExtra("Command", Command);
            sendBroadcast(intent);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
