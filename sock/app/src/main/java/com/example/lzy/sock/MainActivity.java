package com.example.lzy.sock;

import android.app.Activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends Activity {
    private final static int MSG_ONE=1;
    private TextView lockClock,lockDate;
    private String mHours,mMinute,mMonth,mDay,mWeek;
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_ONE:
                    Calendar calendar=Calendar.getInstance();
                    mMonth=String.valueOf(calendar.get(Calendar.MONTH)+1);
                    mDay=String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
                    mWeek=String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
                    /*if(calendar.get(Calendar.HOUR)<10){
                        mHours="0"+String.valueOf(Calendar.HOUR);
                    }else{
                        mHours=String.valueOf(Calendar.HOUR);
                    }*/
                    if (calendar.get(Calendar.HOUR) < 10) {
                        mHours = "0" + calendar.get(Calendar.HOUR);
                    } else {
                        mHours = String.valueOf(calendar.get(Calendar.HOUR));
                    }
                    if (calendar.get(Calendar.MINUTE) < 10) {
                        mMinute = "0" + calendar.get(Calendar.MINUTE);
                    } else {
                        mMinute = String.valueOf(calendar.get(Calendar.MINUTE));
                    }
                    //mMinute=String.valueOf(Calendar.MINUTE);
                    if("1".equals(mWeek)){
                        mWeek="天";
                    }else if("2".equals(mWeek)){
                        mWeek="一";
                    }else if("3".equals(mWeek)){
                        mWeek="二";
                    }else if("4".equals(mWeek)){
                        mWeek="三";
                    }else if("5".equals(mWeek)){
                        mWeek="四";
                    }else if("6".equals(mWeek)) {
                        mWeek="五";
                    }else{
                        mWeek="六";
                    }
                    lockClock.setText(mHours+":"+mMinute);
                    lockDate.setText("星期"+mWeek+" "+mMonth+"/"+mDay);
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        init();
        new TimeThread().start();
    }
    public void init(){
        lockClock=findViewById(R.id.lockClock);
        lockDate=findViewById(R.id.lockDate);

    }
    public class TimeThread extends Thread{
        @Override
        public void run() {
            super.run();
            do{
                try{
                    Thread.sleep(1000);
                    Message message=new Message();
                    message.what=MSG_ONE;
                    handler.sendMessage(message);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }while(true);
        }
    }
}
