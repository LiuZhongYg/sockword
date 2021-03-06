package com.example.lzy.sock;

import android.app.Activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.assetsbasedata.AssetsDatabaseManager;
import com.example.lzy.greendao.entity.greendao.CET4Entity;
import com.example.lzy.greendao.entity.greendao.CET4EntityDao;
import com.example.lzy.greendao.entity.greendao.DaoMaster;
import com.example.lzy.greendao.entity.greendao.DaoSession;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements SynthesizerListener,View.OnClickListener,RadioGroup.OnCheckedChangeListener,InitListener {
    private final static int MSG_ONE = 1;
    private TextView lockClock, lockDate;
    private TextView mWord, phonetic_symbols;
    private RadioButton option_A, option_B, option_C;
    private String mHours, mMinute, mMonth, mDay, mWeek;
    private DaoMaster mDaoMaster, dbMaster;
    private DaoSession mDaoSession, dbSession;
    private CET4EntityDao questionDao, dbDao;
    private SQLiteDatabase db;
    private SpeechSynthesizer mTts;
    List<CET4Entity> datas;
    List<Integer> showOrder;
    private int currentNumber;
    private ImageView playVoice;
    private RadioGroup radioGroup;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    /**
     * (x1,y1)按下时的坐标
     *（x2,y2）抬手时的坐标
     */
    float x1=0;
    float y1=0;
    float x2=0;
    float y2=0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_ONE:
                    Calendar calendar = Calendar.getInstance();
                    mMonth = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                    mDay = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
                    mWeek = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
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
                    if ("1".equals(mWeek)) {
                        mWeek = "天";
                    } else if ("2".equals(mWeek)) {
                        mWeek = "一";
                    } else if ("3".equals(mWeek)) {
                        mWeek = "二";
                    } else if ("4".equals(mWeek)) {
                        mWeek = "三";
                    } else if ("5".equals(mWeek)) {
                        mWeek = "四";
                    } else if ("6".equals(mWeek)) {
                        mWeek = "五";
                    } else {
                        mWeek = "六";
                    }
                    lockClock.setText(mHours + ":" + mMinute);
                    lockDate.setText("星期" + mWeek + " " + mMonth + "/" + mDay);
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
        SpeechUtility.createUtility(MainActivity.this, SpeechConstant.APPID +"=5e40182c");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        init();
        new TimeThread().start();
        //显示第一个单词
        setWord(currentNumber);
        setOption(datas);
    }

    public void init() {

        //初始化控件
        playVoice = findViewById(R.id.play_voice);
        lockClock = findViewById(R.id.lockClock);
        lockDate = findViewById(R.id.lockDate);
        mWord = findViewById(R.id.mWord);
        phonetic_symbols = findViewById(R.id.phonetic_symbols);
        option_A = findViewById(R.id.option_A);
        option_B = findViewById(R.id.option_B);
        option_C = findViewById(R.id.option_C);
        radioGroup=findViewById(R.id.radio_Group);

        //设置监听
        playVoice.setOnClickListener(this);
        radioGroup.setOnCheckedChangeListener(this);

        //初始化数据库
        AssetsDatabaseManager.initManager(this);
        AssetsDatabaseManager mg = AssetsDatabaseManager.getManager();
        //单词数据库
        SQLiteDatabase db1 = mg.getDatabase("word.db");
        mDaoMaster = new DaoMaster(db1);
        mDaoSession = mDaoMaster.newSession();
        questionDao = mDaoSession.getCET4EntityDao();
        //创建错题库
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "wrong.db", null);
        db = helper.getWritableDatabase();
        dbMaster = new DaoMaster(db);
        dbSession = dbMaster.newSession();
        dbDao = dbSession.getCET4EntityDao();

        //初始化轻量级数据库
        sharedPreferences=getSharedPreferences("share",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //科大讯飞在线语音合成
        mTts = SpeechSynthesizer.createSynthesizer(getApplicationContext(),this);
        //设置科大讯飞参数
        setPara();


        //单词显示顺序
        showOrder = new ArrayList<Integer>();
        Random r = new Random();
        int i;
        while (showOrder.size() < 10) {
            i = r.nextInt(20);
            while (!showOrder.contains(i)) {
                showOrder.add(i);
            }
        }
        currentNumber=0;
        datas = questionDao.queryBuilder().list();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.play_voice:
                mTts.startSpeaking(mWord.getText().toString(), this);
                Log.e("onClick",".........................");
                break;
        }
    }

    @Override
    public void onSpeakBegin() {

    }

    @Override
    public void onBufferProgress(int i, int i1, int i2, String s) {

    }

    @Override
    public void onSpeakPaused() {

    }

    @Override
    public void onSpeakResumed() {

    }

    @Override
    public void onSpeakProgress(int i, int i1, int i2) {

    }

    @Override
    public void onCompleted(SpeechError speechError) {

    }

    @Override
    public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        //radioGroup.setClickable(false);
        Log.e("before switch",".........");
        switch (i){
            case R.id.option_A:
                String msg=option_A.getText().toString().substring(2);
                judgeAnswer(msg,option_A);
                Log.e("选择了A",".........");
                break;
            case R.id.option_B:
                String msg1=option_B.getText().toString().substring(2);
                judgeAnswer(msg1,option_B);
                Log.e("选择了A",".........");
                break;
            case R.id.option_C:
                String msg2=option_C.getText().toString().substring(2);
                judgeAnswer(msg2,option_C);
                Log.e("选择了A",".........");
                break;
        }
    }
    @Override
    public void onInit(int code) {
        if (code != ErrorCode.SUCCESS) {
            Log.d("fjj", "初始化失败,错误码：" + code);
        }
        Log.d("fjj", "初始化失败,q错误码：" + code);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            x1=event.getX();
            y1=event.getY();
        }else if(event.getAction()==MotionEvent.ACTION_UP){
            x2=event.getX();
            y2=event.getY();
            if(y2-y1>200){

            }
        }
        return super.onTouchEvent(event);
    }

    public void judgeAnswer(String msg, RadioButton btn){
        Log.e("judgeAnswer", "msg为"+msg+",datas(k)为"+datas.get(currentNumber).getChina());
        if(msg.equals(getDatas(currentNumber).getChina())){
            btn.setTextColor(Color.GREEN);

        }else{
            btn.setTextColor(Color.RED);
            saveWrongData();
            int wrong=sharedPreferences.getInt("wrong",0);
            editor.putInt("wrong",wrong+1);
            editor.putString("wrongId",","+getDatas(currentNumber).getId());
            editor.commit();
            Log.e("答错了----", "----------------------");
        }
    }
    public void saveWrongData(){
        String word = getDatas(currentNumber).getWord();       //获取答错这道题的单词
        String english = getDatas(currentNumber).getEnglish();  //获取答错这道题的音标
        String china = getDatas(currentNumber).getChina();       //获取答错这道题的汉语意思
        String sign = getDatas(currentNumber).getSign();       //获取答错这道题的标记
        CET4Entity data = new CET4Entity(Long.valueOf(dbDao.count()),
                word, english, china, sign);
        dbDao.insertOrReplace(data);                   //把这些字段存到数据库
    }
//实时更新时间
    public class TimeThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                    Message message = new Message();
                    message.what = MSG_ONE;
                    handler.sendMessage(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    public CET4Entity getDatas(int order) {
        int k = showOrder.get(order);
        return datas.get(k);
    }
    public void setWord(int order){
        mWord.setText(getDatas(order).getWord());
        phonetic_symbols.setText(getDatas(order).getEnglish());
    }
    private void setOption(List<CET4Entity> datas) {
        Random r = new Random();
        int i = r.nextInt(3);
        if (i == 0) {
            option_A.setText("A:"+ getDatas(currentNumber).getChina());
            if (currentNumber - 1 >= 0) {
                option_B.setText("B:" + getDatas(currentNumber - 1).getChina());
            } else {
                option_B.setText("B:" + getDatas(currentNumber + 2).getChina());
            }
            if (currentNumber + 1 >= 20) {
                option_C.setText("C:" + getDatas(currentNumber - 2).getChina());
            } else {
                option_C.setText("C:" + getDatas(currentNumber + 1).getChina());
            }
        } else if (i == 1) {
            option_B.setText("B:" + getDatas(currentNumber).getChina());
            if (currentNumber - 1 >= 0) {
                option_A.setText("A:" + getDatas(currentNumber - 1).getChina());
            } else {
                option_A.setText("A:" + getDatas(currentNumber + 2).getChina());
            }
            if (currentNumber + 1 >= 20) {
                option_C.setText("C:" + getDatas(currentNumber - 2).getChina());
            } else {
                option_C.setText("C:" + getDatas(currentNumber + 1).getChina());
            }
        } else {
            option_C.setText("C:" + getDatas(currentNumber).getChina());
            if (currentNumber - 1 >= 0) {
                option_B.setText("B:" + getDatas(currentNumber - 1).getChina());
            } else {
                option_B.setText("B:" + getDatas(currentNumber + 2).getChina());
            }
            if (currentNumber + 1 >= 20) {
                option_A.setText("A:" + getDatas(currentNumber - 2).getChina());
            } else {
                option_A.setText("A:" + getDatas(currentNumber + 1).getChina());
            }
        }
    }
    public void setPara(){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 引擎类型 网络
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        // 设置语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        // 设置音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        // 设置音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");
        // 设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/KRobot/wavaudio.pcm");
        // 背景音乐  1有 0 无
        // mTts.setParameter("bgs", "1");
    }
}
