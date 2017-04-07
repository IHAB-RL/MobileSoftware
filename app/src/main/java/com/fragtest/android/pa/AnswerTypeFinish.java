package com.fragtest.android.pa;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeFinish extends AppCompatActivity {

    public Button mAnswerButton;
    public LinearLayout.LayoutParams answerParams;
    public AnswerLayout parent;
    private int mAnswerID;
    private Context mContext;
    private FileIO fileIO;

    public AnswerTypeFinish(Context context, int ID, AnswerLayout qParent) {

        mContext = context;
        mAnswerID = ID;
        parent = qParent;
        mAnswerButton = new Button(context);
        mAnswerButton.setId(mAnswerID);
        mAnswerButton.setText(R.string.buttonTextFinish);
        mAnswerButton.setTextSize(Units.getTextSizeAnswer());
        mAnswerButton.setGravity(Gravity.CENTER_HORIZONTAL);
        mAnswerButton.setTextColor(ContextCompat.getColor(context, R.color.TextColor));
        mAnswerButton.setBackgroundColor(ContextCompat.getColor(context, R.color.BackgroundColor));
        mAnswerButton.setAllCaps(false);
        mAnswerButton.setTypeface(null, Typeface.NORMAL);

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mAnswerButton.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.button));
        } else {
            mAnswerButton.setBackground(ContextCompat.getDrawable(context, R.drawable.button));
        }

        // Parameters of Answer Button
        answerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        int[] AnswerFinishMargins = Units.getAnswerFinishMargin();
        answerParams.setMargins(AnswerFinishMargins[0], AnswerFinishMargins[1],
                AnswerFinishMargins[2], AnswerFinishMargins[3]);
    }

    public boolean addAnswer() {
        parent.layoutAnswer.addView(mAnswerButton, answerParams);
        return true;
    }

    public void addClickListener(final Context context, final MetaData metaData,
                                 final AnswerIDs answerIDs, final AnswerTexts answerTexts) {
        mAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                metaData.finalise(answerIDs, answerTexts);
                fileIO = new FileIO();
                fileIO.saveDataToFile(mContext, metaData.getFileName(), metaData.getData());
                //Toast.makeText(mContext,R.string.infoTextSave,Toast.LENGTH_SHORT).show();
                ((Activity)context).finish();

            }
        });
    }
}