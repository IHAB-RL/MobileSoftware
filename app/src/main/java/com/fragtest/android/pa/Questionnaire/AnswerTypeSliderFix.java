package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fragtest.android.pa.Core.Units;
import com.fragtest.android.pa.DataTypes.StringAndInteger;
import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ulrikkowalk on 04.04.17.
 */

public class AnswerTypeSliderFix extends AppCompatActivity {

    public static String LOG_STRING = "AnswerTypeSliderFix";
    public final AnswerLayout parent;
    private final List<Integer> mListOfIds = new ArrayList<>();
    private final Context mContext;
    private final List<StringAndInteger> mListOfAnswers;
    private final LinearLayout mHorizontalContainer;
    private final LinearLayout mAnswerListContainer;
    private final View mResizeView;
    private final View mRemainView;
    private final RelativeLayout mSliderContainer;
    private final int width;
    private final int mUsableHeight;
    private final int mQuestionId;
    private final Questionnaire mQuestionnaire;
    private int mDefaultAnswer = -1;
    private int nTextViewHeight;

    public AnswerTypeSliderFix(Context context, Questionnaire questionnaire, AnswerLayout qParent, int nQuestionId) {

        mContext = context;
        parent = qParent;
        mQuestionnaire = questionnaire;
        mListOfAnswers = new ArrayList<>();
        mQuestionId = nQuestionId;

        // Slider Layout is predefined in XML
        LayoutInflater inflater = LayoutInflater.from(context);
        width = Units.getScreenWidth();
        //answerLayoutPadding = Units.getAnswerLayoutPadding();

        parent.scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.f
        ));

        mUsableHeight = (new Units(mContext)).getUsableSliderHeight();

        //  |           mHorizontalContainer          |
        //  | mSliderContainer | mAnswerListContainer |
        // mHorizontalContainer is parent to both slider and answer option containers
        mHorizontalContainer = (LinearLayout) inflater.inflate(
                R.layout.answer_type_slider, parent.scrollContent, false);

        // ACHTUNG _ MAGIC NUMBER!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        mHorizontalContainer.setOrientation(LinearLayout.HORIZONTAL);
        mHorizontalContainer.setLayoutParams(new LinearLayout.LayoutParams(
                width,
                mUsableHeight - 140,
                1.f
        ));
        mHorizontalContainer.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.BackgroundColor));

        // mSliderContainer is host to slider on the left
        mSliderContainer = (RelativeLayout) mHorizontalContainer.findViewById(
                R.id.SliderContainer);
        mSliderContainer.setBackgroundColor(ContextCompat.getColor(
                mContext, R.color.BackgroundColor));

        // mAnswerListContainer is host to vertical array of answer options
        mAnswerListContainer = (LinearLayout) mHorizontalContainer.
                findViewById(R.id.AnswerTextContainer);

        mAnswerListContainer.setOrientation(LinearLayout.VERTICAL);
        mAnswerListContainer.setBackgroundColor(ContextCompat.getColor(
                mContext, R.color.BackgroundColor));
        mAnswerListContainer.setLayoutParams(new LinearLayout.LayoutParams(
                width - 100,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.f
        ));

        mResizeView = mHorizontalContainer.findViewById(R.id.ResizeView);
        mRemainView = mHorizontalContainer.findViewById(R.id.RemainView);

        mResizeView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.JadeRed));
    }

    public boolean buildView() {

        // Iterate over all options and create a TextView for each one
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            TextView textMark = new TextView(mContext);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.0f);

            textParams.setMargins(
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextBottomMargin_Left),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextBottomMargin_Top),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextBottomMargin_Right),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextBottomMargin_Bottom));
            textMark.setPadding(
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextPadding_Left),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextPadding_Top),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextPadding_Right),
                    (int) mContext.getResources().getDimension(R.dimen.SliderTextPadding_Bottom));
            textMark.setText(mListOfAnswers.get(iAnswer).getText());
            textMark.setId(mListOfAnswers.get(iAnswer).getId());
            textMark.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));

            // Adaptive size and lightness of font of in-between tick marks
            if (mListOfAnswers.size() > 6) {
                int textSize = (int) (mContext.getResources().getDimension(R.dimen.textSizeAnswer)) * 7 / mListOfAnswers.size();
                if (iAnswer % 2 == 1) {
                    textSize -= 2;
                    textMark.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor_Light));
                }
                textMark.setTextSize(textSize);
            } else {
                textMark.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
            }

            textMark.setGravity(Gravity.CENTER_VERTICAL);
            textMark.setLayoutParams(textParams);
            textMark.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));

            mAnswerListContainer.addView(textMark);
        }
        parent.layoutAnswer.addView(mHorizontalContainer);
        return true;
    }

    public boolean addAnswer(int nAnswerId, String sAnswer, boolean isDefault) {
        mListOfAnswers.add(new StringAndInteger(sAnswer, nAnswerId));
        // index of default answer if present
        if (isDefault) {
            // If default present, this element is the one
            mDefaultAnswer = mListOfAnswers.size() - 1;
            // Handles default id if existent
            setProgressItem(mDefaultAnswer);

        }
        mListOfIds.add(nAnswerId);
        return true;
    }

    public boolean addClickListener() {

        final TextView tvTemp = (TextView) mAnswerListContainer.findViewById(mListOfAnswers.get(0).getId());
        tvTemp.post(new Runnable() {
            @Override
            public void run() {
                nTextViewHeight = tvTemp.getHeight();
                // Handles default id if existent
                if (mDefaultAnswer == -1) {
                    setProgressItem((int) ((mListOfAnswers.size() - 1) / 2.0f));
                    mQuestionnaire.addIdToEvaluationList(mQuestionId,
                            mListOfAnswers.get(mListOfAnswers.size() / 2).getId());
                } else {
                    setProgressItem(mDefaultAnswer);
                    mQuestionnaire.addIdToEvaluationList(mQuestionId,
                            mListOfAnswers.get(mDefaultAnswer).getId());
                }
            }
        });

        // Enables clicking on option directly
        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            final int numAnswer = iAnswer;
            final int currentId = mListOfAnswers.get(iAnswer).getId();
            TextView tv = (TextView) mAnswerListContainer.findViewById(currentId);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setProgressItem(numAnswer);
                    mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                    mQuestionnaire.addIdToEvaluationList(mQuestionId, currentId);
                }
            });
        }

        // Enables dragging of slider
        mResizeView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case (MotionEvent.ACTION_DOWN):
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        return rescaleSliderOnline(event);
                    case (MotionEvent.ACTION_UP):
                        return rescaleSliderFinal(event);
                    case (MotionEvent.ACTION_CANCEL):
                        return true;
                    case (MotionEvent.ACTION_OUTSIDE):
                        Log.d("Motion", "Movement occurred outside bounds " +
                                "of current screen element");
                        return true;
                    default:
                        break;
                }
                return true;
            }
        });

        // Enables clicking in area above slider (remainView) to adjust
        mRemainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case (MotionEvent.ACTION_DOWN):
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        return rescaleSliderOnline(event);
                    case (MotionEvent.ACTION_UP):
                        return rescaleSliderFinal(event);
                    case (MotionEvent.ACTION_CANCEL):
                        return true;
                    case (MotionEvent.ACTION_OUTSIDE):
                        Log.d("Motion", "Movement occurred outside bounds " +
                                "of current screen element");
                        return true;
                    default:
                        break;
                }
                return true;
            }
        });
        return true;
    }

    // Set progress  bar according to user input
    private boolean rescaleSliderFinal(MotionEvent motionEvent) {
        int nValueSelected = (int) clipValuesToRange(motionEvent.getRawY());
        int nItem = mapValuesToItems(nValueSelected);
        nItem = clipItemsToRange(nItem);
        try {
            setProgressItem(nItem);
            mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
            mQuestionnaire.addIdToEvaluationList(mQuestionId, mListOfAnswers.get(nItem).getId());
            mQuestionnaire.checkVisibility();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    // Set progress  bar according to user input
    private boolean rescaleSliderOnline(MotionEvent motionEvent) {
        int nValueSelected = (int) clipValuesToRange(motionEvent.getRawY());
        int nItem = mapValuesToItems(nValueSelected);
        nItem = clipItemsToRange(nItem);
        try {
            setProgressItem(nItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    // Ensure values inside slider boundaries
    private float clipValuesToRange(float inVal) {
        int nPad = (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Bottom);
        if (inVal < Units.getScreenHeight() - mUsableHeight - nPad) {
            inVal = Units.getScreenHeight() - mUsableHeight - nPad;
        } else if (inVal > Units.getScreenHeight() - nPad) {
            inVal = Units.getScreenHeight() - nPad;
        }
        return inVal;
    }

    // Ensure valid item numbers
    private int clipItemsToRange(int item) {
        if (item > mListOfAnswers.size() - 1) {
            item = mListOfAnswers.size() - 1;
        } else if (item < 0) {
            item = 0;
        }
        return item;
    }

    // Enables quantisation of values (fixed choice options), counting from 0 (lowest)
    private int mapValuesToItems(float inVal) {
        return (int) ((inVal - (Units.getScreenHeight() -
                mUsableHeight - (int) mContext.getResources().getDimension(
                R.dimen.answerLayoutPadding_Bottom))) / (nTextViewHeight));
    }

    // Set progress/slider according to number of selected item (counting from 0)
    public void setProgressItem(int numItem) {
        mResizeView.getLayoutParams().height =
                (int) ((2 * (mListOfAnswers.size() - numItem) - 1) / 2.0f * nTextViewHeight);
        mResizeView.setLayoutParams(mResizeView.getLayoutParams());
    }
}


