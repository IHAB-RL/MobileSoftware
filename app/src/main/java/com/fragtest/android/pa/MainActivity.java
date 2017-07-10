package com.fragtest.android.pa;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.fragtest.android.pa.Questionnaire.QuestionnairePagerAdapter;


public class MainActivity extends AppCompatActivity {

    static final String LOG = "MainActivity";

    public ViewPager mViewPager = null;
    public TextView mLogo;
    public View mArrowBack, mArrowForward, mRevert, mProgress, mRegress;
    private QuestionnairePagerAdapter mAdapter;
    private boolean mServiceIsBound;
    private Messenger mServiceMessenger;


    class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG, "Received Message: " + msg.what);

            switch (msg.what) {

                case ControlService.MSG_ALARM_RECEIVED:
                    mAdapter.createQuestionnaire();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


    final Messenger mMessageHandler = new Messenger(new MessageHandler());


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            messageService(ControlService.MSG_REGISTER_CLIENT);
            messageService(ControlService.MSG_GET_STATUS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
        }
    };


    // Send message to connected client
    private void messageService(int what) {

        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.replyTo = mMessageHandler;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
            }
        }
    }

    void doBindService() {
        bindService(new Intent(this, ControlService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mServiceIsBound = true;
    }


    void doUnbindService() {
        if (mServiceIsBound) {
            unbindService(mConnection);
            mServiceIsBound = false;
        }
    }


    public void handleNewPagerAdapter() {

        mViewPager = null;
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mAdapter = new QuestionnairePagerAdapter(this, mViewPager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(myOnPageChangeListener);
    }


    private ViewPager.OnPageChangeListener myOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrollStateChanged(int state) {
                }
                @Override
                public void onPageScrolled(int position,
                                           float positionOffset, int positionOffsetPixels) {
                }
                @Override
                public void onPageSelected(int position) {
                    mAdapter.setQuestionnaireProgressBar(position);
                    mAdapter.setArrows(position);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        doBindService();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mLogo = (TextView) findViewById(R.id.Action_Logo);
        mArrowBack = findViewById(R.id.Action_Back);
        mArrowForward = findViewById(R.id.Action_Forward);
        mRevert = findViewById(R.id.Action_Revert);
        mProgress = findViewById(R.id.progress);
        mRegress = findViewById(R.id.regress);

        handleNewPagerAdapter();
        //mAdapter.createQuestionnaire();
        mAdapter.createMenu();
        //mAdapter.startTimer();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        doUnbindService();
    }


    /*public int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }*/


}