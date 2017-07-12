package com.fragtest.android.pa.Questionnaire;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.MainActivity;
import com.fragtest.android.pa.Menu.MenuPage;
import com.fragtest.android.pa.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by ulrikkowalk on 28.02.17.
 */

public class QuestionnairePagerAdapter extends PagerAdapter {

    final ViewPager mViewPager;
    private final MainActivity MainActivity;
    private final Context mContext;
    private final Handler timerHandler = new Handler();
    private final int mUpdateRate = 1000;
    private final int mDurVibrationMs = 0 * 200;
    // Stores all active Views
    ArrayList<QuestionViewActive> mListOfActiveViews;
    // Stores all Views
    ArrayList<QuestionViewActive> mListOfViewsStorage;
    private String LOG_STRING = "Quest..PagerAdapter";
    private int mNUM_PAGES;
    private Questionnaire mQuestionnaire;
    private MenuPage mMenuPage;
    private boolean runCountDown = true;
    private int mCountDownInterval = 30;
    private int mSecondsRemaining = 120;
    private ArrayList<String> mQuestionList;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (runCountDown) {
                mSecondsRemaining -= mUpdateRate / 1000;
                updateCountDown(mSecondsRemaining);
                timerHandler.postDelayed(this, mUpdateRate);
            }
        }
    };

    public QuestionnairePagerAdapter(Context context, ViewPager viewPager) {

        mContext = context;
        MainActivity = (MainActivity) context;
        handleControls();
        mViewPager = viewPager;
    }

    public void createMenu() {
        // Instantiates a MenuPage Object based on Contents of raw XML File
        mMenuPage = new MenuPage(MainActivity, this);
        mNUM_PAGES = 1;
        mViewPager.setOffscreenPageLimit(0);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createMenuLayout();
        setControlsMenu();
    }

    public void createQuestionnaire(ArrayList<String> questionList) {

        mQuestionList = questionList;

        stopCountDown();
        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(MainActivity, this);
        mQuestionnaire.setUp(questionList);
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();
    }

    private void createQuestionnaire() {

        stopCountDown();
        // Instantiates a Questionnaire Object based on Contents of raw XML File
        mQuestionnaire = new Questionnaire(MainActivity, this);
        mQuestionnaire.setUp(mQuestionList);
        mNUM_PAGES = mQuestionnaire.getNumPages();
        mViewPager.setOffscreenPageLimit(1);

        mListOfActiveViews = new ArrayList<>();
        mListOfViewsStorage = new ArrayList<>();

        createQuestionnaireLayout();
        setControlsQuestionnaire();
        // Creates and destroys views based on filter id settings
        mQuestionnaire.checkVisibility();

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
        setArrows(0);
        setQuestionnaireProgressBar();
    }

    public void setQuestionnaireProgressBar(int position) {
        // Set the horizontal Indicator at the Top to follow Page Position
        int nAccuracy = 100;

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = (float) (position + 1) / mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    void setQuestionnaireProgressBar() {

        int nAccuracy = 100;

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = (float) (mViewPager.getCurrentItem() + 1) /
                mViewPager.getAdapter().getCount() * nAccuracy;
        float nRegress = (nAccuracy - nProgress);

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    // Set the horizontal Indicator at the Top to follow Page Position
    private void setQuestionnaireProgressBar(float fraction) {

        View progress = MainActivity.mProgress;
        View regress = MainActivity.mRegress;

        float nProgress = fraction;
        float nRegress = 1f - nProgress;

        LinearLayout.LayoutParams progParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nProgress
        );
        LinearLayout.LayoutParams regParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                nRegress
        );

        progress.setLayoutParams(progParams);
        regress.setLayoutParams(regParams);
    }

    public void setArrows(int position) {

        if (position == 0) {
            MainActivity.mArrowBack.setVisibility(View.INVISIBLE);
        } else if (MainActivity.mArrowBack.getVisibility() == View.INVISIBLE) {
            MainActivity.mArrowBack.setVisibility(View.VISIBLE);
        }

        if (position == mViewPager.getAdapter().getCount() - 1) {
            MainActivity.mArrowForward.setVisibility(View.INVISIBLE);
        } else if (MainActivity.mArrowForward.getVisibility() == View.INVISIBLE) {
            MainActivity.mArrowForward.setVisibility(View.VISIBLE);
        }
    }

    public int addView(View view, int position, int positionInRaw, boolean mandatory,
                       List<Answer> listOfAnswers) {
        mListOfActiveViews.add(new QuestionViewActive(view, view.getId(), positionInRaw, mandatory,
                listOfAnswers));
        // Sort the Views by their id (implicitly their determined order)
        Collections.sort(mListOfActiveViews);
        return position;
    }

    private void createMenuLayout() {

        LinearLayout layout = mMenuPage.generateView();

        layout.setId(0);
        // Adds the Layout to List carrying all ACTIVE Views
        mListOfActiveViews.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));
        // Adds the Layout to List storing ALL Views
        mListOfViewsStorage.add(new QuestionViewActive(layout, layout.getId(),
                0, true, null));

        notifyDataSetChanged();
        mViewPager.setCurrentItem(0);
    }

    private void setControlsMenu() {

        MainActivity.mArrowForward.setVisibility(View.INVISIBLE);
        MainActivity.mArrowBack.setVisibility(View.INVISIBLE);
        MainActivity.mRevert.setVisibility(View.INVISIBLE);
        MainActivity.mProgress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeRed));
        MainActivity.mRegress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeGray));
        MainActivity.mLogo.setEnabled(false);
    }

    private void setControlsQuestionnaire() {
        MainActivity.mArrowForward.setVisibility(View.VISIBLE);
        MainActivity.mArrowBack.setVisibility(View.VISIBLE);
        MainActivity.mRevert.setVisibility(View.VISIBLE);
        MainActivity.mProgress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeRed));
        MainActivity.mRegress.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.JadeGray));
        MainActivity.mLogo.setEnabled(true);
    }

    private void createQuestionnaireLayout() {
        // Generate a view for each page/question and collect them in ArrayList
        for (int iQuestion = 0; iQuestion < mNUM_PAGES; iQuestion++) {
            // Extracts Question Details from Questionnaire and creates Question
            Question question = mQuestionnaire.createQuestion(iQuestion);
            // Inflates Question Layout based on Question Details
            LinearLayout layout = mQuestionnaire.generateView(question);
            // Sets Layout Id to Question Id
            layout.setId(mQuestionnaire.getId(question));
            // Adds the Layout to List carrying all ACTIVE Views
            mListOfActiveViews.add(new QuestionViewActive(layout, layout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
            // Adds the Layout to List storing ALL Views
            mListOfViewsStorage.add(new QuestionViewActive(layout, layout.getId(),
                    iQuestion, question.isMandatory(), question.getAnswers()));
        }

    }

    private void handleControls() {

        MainActivity.mLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //createMenu();
                sendMessage(ControlService.MSG_NEW_ALARM);
            }
        });
        MainActivity.mArrowBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() != 0) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                }
            }
        });
        MainActivity.mArrowForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager.getCurrentItem() < mViewPager.getAdapter().getCount() - 1) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                }
            }
        });
        MainActivity.mRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity, R.string.infoTextRevert, Toast.LENGTH_SHORT).show();
                createQuestionnaire();
            }
        });
    }

    private void updateCountDown(int seconds) {
        if (seconds > 0) {
            mMenuPage.updateCountdownText(seconds);
            setQuestionnaireProgressBar((float) seconds / mCountDownInterval);
        } else {
            ((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE)).vibrate(mDurVibrationMs);
            stopCountDown();
        }
    }

    public void setCountDownInterval(int interval) {
        mCountDownInterval = interval;
    }

    public void startCountDown() {
        runCountDown = true;
        resetCountDown();
        timerHandler.postDelayed(runnable, 0);
    }

    private void resetCountDown() {
        mSecondsRemaining = mCountDownInterval;
    }

    private void stopCountDown() {
        runCountDown = false;
    }

    int removeView(int position) {

        int nCurrentItem = mViewPager.getCurrentItem();
        mViewPager.setAdapter(null);
        mListOfActiveViews.remove(position);
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(nCurrentItem);
        return position;
    }

    int getPositionFromId(int iId) {
        for (int iItem = 0; iItem < mListOfActiveViews.size(); iItem++) {
            if (mListOfActiveViews.get(iItem).getId() == iId) {
                return iItem;
            }
        }
        return -1;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        View view = mListOfActiveViews.get(position).getView();
        collection.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        if (!(mListOfActiveViews == null) && !(mListOfActiveViews.size() == 0)) {
            mNUM_PAGES = mListOfActiveViews.size();
        } else {
            mNUM_PAGES = 0;
        }
        return mNUM_PAGES;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        int index = mListOfActiveViews.indexOf(object);

        if (index == -1)
            return POSITION_NONE;
        else
            return index;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    public void sendMessage(int what) {
        MainActivity.messageService(what);
    }

}