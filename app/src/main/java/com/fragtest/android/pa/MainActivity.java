package com.fragtest.android.pa;

import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.graphics.Color;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.fragtest.android.pa.R.id.regress;

public class MainActivity extends AppCompatActivity {

    public ViewPager mViewPager = null;
    private QuestionnairePagerAdapter pagerAdapter = null;

    private View mArrowBack;
    private View mArrowForward;
    private View mRevert;
    private int mPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new QuestionnairePagerAdapter(this, mViewPager));
        mViewPager.setCurrentItem(0);

        mViewPager.addOnPageChangeListener(myOnPageChangeListener);

        mArrowBack = findViewById(R.id.Action_Back);
        mArrowBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPosition != 0) {
                    mViewPager.setCurrentItem(mPosition-1);
                }
            }
        });

        mArrowForward = findViewById(R.id.Action_Forward);
        mArrowForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPosition < mViewPager.getAdapter().getCount()-1) {
                    mViewPager.setCurrentItem(mPosition+1);
                }
            }
        });

        mRevert = findViewById(R.id.Action_Revert);
        mRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Data was reverted.",Toast.LENGTH_SHORT).show();
                mViewPager.setCurrentItem(0);
            }
        });

        /*
        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setOnPageChangeListener(myOnPageChangeListener);
        tabs.setViewPager(mViewPager);
        tabs.setTextColor(Color.WHITE);
        tabs.setDividerPadding(0);
        tabs.setTabPaddingLeftRight(0);
        tabs.setShouldExpand(true);
*/
        setQuestionnaireProgBar(0);
        setArrows(0);



    }

    private ViewPager.OnPageChangeListener myOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrollStateChanged(int state) {
                    //Called when the scroll state changes.

                }

                @Override
                public void onPageScrolled(int position,
                                           float positionOffset, int positionOffsetPixels) {
                    //This method will be invoked when the current page is scrolled,
                    //either as part of a programmatically initiated smooth scroll
                    //or a user initiated touch scroll.
                }

                @Override
                public void onPageSelected(int position) {
                    //This method will be invoked when a new page becomes selected.
                    //Log.i("onPageSelected","" + position);
                    //mViewPager.setCurrentItem(position);

                    setQuestionnaireProgBar(position);
                    setArrows(position);
                }
            };

            // Set the horizontal Indicator at the Top to follow Page Position
            public void setQuestionnaireProgBar(int position) {

                mPosition = position;
                int nAccuracy = 100;

                View progress = findViewById(R.id.progress);
                View regress = findViewById(R.id.regress);

                float nProgress = (float) (position+1)/mViewPager.getAdapter().getCount()*nAccuracy;
                float nRegress = (nAccuracy-nProgress);

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

            public void setArrows(int position) {
                View arrowBack = findViewById(R.id.Action_Back);
                if (position == 0) {
                    arrowBack.setVisibility(View.INVISIBLE);
                } else if (arrowBack.getVisibility() == View.INVISIBLE) {
                    arrowBack.setVisibility(View.VISIBLE);
                }

                View arrowForward = findViewById(R.id.Action_Forward);
                if (position == mViewPager.getAdapter().getCount()-1) {
                    arrowForward.setVisibility(View.INVISIBLE);
                } else if (arrowForward.getVisibility() == View.INVISIBLE) {
                    arrowForward.setVisibility(View.VISIBLE);
                }
            }
}