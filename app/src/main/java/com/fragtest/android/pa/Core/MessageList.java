package com.fragtest.android.pa.Core;

import android.os.Bundle;
import android.util.Log;

import com.fragtest.android.pa.DataTypes.IntegerAndBundle;
import com.fragtest.android.pa.MainActivity;

import java.util.ArrayList;

/**
 * Created by ul1021 on 30.10.2018.
 */

public class MessageList {

    private ArrayList<IntegerAndBundle> mMessages;
    private MainActivity mainActivity;
    private String LOG = "MessageList";

    public MessageList(MainActivity context) {
        this.mMessages = new ArrayList<>();
        this.mainActivity = context;
    }

    public void addMessage(int mess, Bundle bundle) {
        this.mMessages.add(new IntegerAndBundle(mess, bundle));
    }

    public void addMessage(int mess) {
        this.mMessages.add(new IntegerAndBundle(mess));
    }

    public void work() {

        for (int iMess = 0; iMess < mMessages.size(); iMess++) {
            Bundle tmp = this.mMessages.get(iMess).getBundle();

            Log.e(LOG, "message: " + this.mMessages.get(iMess).getInteger() + "data: " + tmp);

            try {
                if (tmp.getString("tmp").equalsIgnoreCase("-1")) {
                    this.mainActivity.messageService(this.mMessages.get(iMess).getInteger());
                }
            } catch (Exception e) {
                this.mainActivity.messageService(this.mMessages.get(iMess).getInteger(),
                        this.mMessages.get(iMess).getBundle());
            }
        }

        this.mMessages = new ArrayList<>();
    }

    public int getLength() {
        return this.mMessages.size();
    }

}
