package com.charlie.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 公告View
 * Created by Charlie on 2017/8/22.
 * xu0xyc@outlook.com
 */
public class NoticeView extends View {
    private static final String TAG = NoticeView.class.getSimpleName();

    private static final int HOR_SCROLL_SPEED = 25;

    private static final int DEF_TEXT_SIZE = 15;// sp
    private static final int DEF_TEXT_COLOR = Color.BLACK;
    private static final int DEF_VER_SCROLL_DURATION = 500;
    private static final int DEF_HOR_SCROLL_DELAY = 1000;
    private static final int DEF_VER_SCROLL_DELAY = 2500;

    private Context mContext;

    private Paint mPaint;
    private int mTextSize;
    private int mTextColor;
    private int verScrollDuration;
    private int horScrollDelay;
    private int verScrollDelay;
    private int mTextHeight;
    private int mDeltaOfBaseLine;// 距离基线的差

    private List<CharSequence> mNotices;
    private CharSequence mCurrNotice;
    private CharSequence mNextNotice;

    private Scroller mScroller;
    private int mStartScrollerX;
    private int mStartScrollerY;
    private int mScrollerX;
    private int mScrollerY;
    private int mDistanceOfHorScroll;// 需要水平滚动的距离

    private boolean isHorScrolling;
    private boolean isVerScrolling;
    private boolean isHadHorScrolled;// 已经水平滚过了（默认只滚一次）
    private boolean isHavePendingRunnable;// 是否有即将运行的Runnable

    private boolean isAttachedWindow;
    private boolean isVisiable;

    private int currIndex;
    private int mWidth;
    private int mHeight;
    private Rect mPaddingedRect;

    public NoticeView(Context context) {
        this(context, null);
    }

    public NoticeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NoticeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        TypedArray ta = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.NoticeView, defStyleAttr, 0);
        mTextSize = ta.getDimensionPixelSize(R.styleable.NoticeView_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEF_TEXT_SIZE, getResources().getDisplayMetrics()));
        mTextColor = ta.getColor(R.styleable.NoticeView_textColor, DEF_TEXT_COLOR);
        verScrollDuration = ta.getInteger(R.styleable.NoticeView_verScrollDuration, DEF_VER_SCROLL_DURATION);
        horScrollDelay = ta.getInteger(R.styleable.NoticeView_horScrollDelay, DEF_HOR_SCROLL_DELAY);
        verScrollDelay = ta.getInteger(R.styleable.NoticeView_verScrollDelay, DEF_VER_SCROLL_DELAY);
        CharSequence[] notices = ta.getTextArray(R.styleable.NoticeView_notices);
        if (null != notices) {
            mNotices = Arrays.asList(notices);
        }

        ta.recycle();

        initial();
    }

    private void initial() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);

        Paint.FontMetricsInt fontMetricsInt = mPaint.getFontMetricsInt();
        int bt = fontMetricsInt.bottom - fontMetricsInt.top;
        int da = fontMetricsInt.descent - fontMetricsInt.ascent;
        if (bt > da) {
            mTextHeight = bt;
            mDeltaOfBaseLine = fontMetricsInt.bottom;
        } else {
            mTextHeight = da;
            mDeltaOfBaseLine = fontMetricsInt.descent;
        }

        mScroller = new Scroller(getContext(), new LinearInterpolator());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.d(TAG, "onMeasure-->widthMeasureSpec:" + widthMeasureSpec + ", heightMeasureSpec:" + heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            heightSize = getPaddingTop() + mTextHeight + getPaddingBottom();
        }

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
//        Log.d(TAG, "onSizeChanged-->w:" + w + ", h:" + h + ", oldw:" + oldw + ", oldh:" + oldh);
        mWidth = w;
        mHeight = h;

        mScrollerX = mStartScrollerX = getPaddingLeft();
        mScrollerY = mStartScrollerY = (mHeight + mTextHeight)/2 - mDeltaOfBaseLine;

        mPaddingedRect = new Rect(getPaddingLeft(), getPaddingTop(), mWidth - getPaddingRight(), mHeight - getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        Log.d(TAG, "onDraw ");

        if (isInEditMode() || null == mNotices || mNotices.size()==0) {
            return;
        }

        canvas.clipRect(mPaddingedRect);

        if (!isVerScrolling && !isHorScrolling) {
            int size = mNotices.size();
            currIndex = currIndex % size;
            mCurrNotice = mNotices.get(currIndex);

            int nextIndex = (currIndex+1) % size;
            if (currIndex == nextIndex) {
                mNextNotice = null;
            } else {
                mNextNotice = mNotices.get(nextIndex);
            }
        }

        if (null == mNextNotice) {
            mPaint.setAlpha(255);
            canvas.drawText(mCurrNotice, 0, mCurrNotice.length(), mScrollerX, mScrollerY, mPaint);
            if (!isHorScrolling) {
                if(isHavePendingRunnable) return;
                int noticeLength = (int)mPaint.measureText(mCurrNotice, 0, mCurrNotice.length());
                int avaiableWidth = mWidth - getPaddingLeft() - getPaddingRight();
                mDistanceOfHorScroll = noticeLength-avaiableWidth > 0 ? noticeLength-avaiableWidth : 0;
                if (mDistanceOfHorScroll > 0) {
                    isHavePendingRunnable = true;
                    postDelayed(mHorScrollRunnable, horScrollDelay);
                }
            }

        } else {
            if (isVerScrolling || isHorScrolling) {

                if (isVerScrolling) {
                    int alpha = (int) (255f * (mStartScrollerY - mScrollerY) / ((mHeight + mTextHeight) / 2));
                    mPaint.setAlpha(255-alpha);
                    canvas.drawText(mCurrNotice, 0, mCurrNotice.length(), mScrollerX, mScrollerY, mPaint);
                    mPaint.setAlpha(alpha);
                    canvas.drawText(mNextNotice, 0, mNextNotice.length(), mStartScrollerX, mScrollerY + (mHeight + mTextHeight) / 2, mPaint);
                }

                if (isHorScrolling) {
                    mPaint.setAlpha(255);
                    canvas.drawText(mCurrNotice, 0, mCurrNotice.length(), mScrollerX, mScrollerY, mPaint);
                }

            } else {
                canvas.drawText(mCurrNotice, 0, mCurrNotice.length(), mScrollerX, mScrollerY, mPaint);
                if(isHavePendingRunnable) return;
                int noticeLength = (int)mPaint.measureText(mCurrNotice, 0, mCurrNotice.length());
                int avaiableWidth = mWidth - getPaddingLeft() - getPaddingRight();
                mDistanceOfHorScroll = noticeLength-avaiableWidth > 0 ? noticeLength-avaiableWidth : 0;
                if (!isHadHorScrolled && mDistanceOfHorScroll > 0) {
                    isHadHorScrolled = true;
                    isHavePendingRunnable = true;
                    postDelayed(mHorScrollRunnable, horScrollDelay);
                } else {
                    isHavePendingRunnable = true;
                    postDelayed(mVerScrollRunnable, verScrollDelay);
                }
            }
        }
    }

    private Runnable mHorScrollRunnable = new Runnable() {
        @Override
        public void run() {
            isHavePendingRunnable = false;
            isHorScrolling = true;
            mScroller.startScroll(mStartScrollerX, mStartScrollerY, -mDistanceOfHorScroll, 0, mDistanceOfHorScroll * HOR_SCROLL_SPEED);
            invalidate();
        }
    };

    private Runnable mVerScrollRunnable = new Runnable() {
        @Override
        public void run() {
            isHavePendingRunnable = false;
            isVerScrolling = true;
            mScroller.startScroll(mScrollerX, mScrollerY, 0, -(mHeight+mTextHeight)/2, verScrollDuration);
            invalidate();
        }
    };

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollerX = mScroller.getCurrX();
            mScrollerY = mScroller.getCurrY();
            invalidate();
        } else if(isVerScrolling) {
            isVerScrolling = false;

            isHadHorScrolled = false;
            mScrollerX = mStartScrollerX;
            mScrollerY = mStartScrollerY;

            currIndex++;
        } else if (isHorScrolling) {
            isHorScrolling = false;
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
//        Log.d(TAG, "onVisibilityChanged-->visibility:" + visibility);
        isVisiable = visibility == VISIBLE && getVisibility() == VISIBLE;
        if (isAttachedWindow) {
            if (isVisiable) {
                invalidate();
            } else {
                stopAnim();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        Log.d(TAG, "onAttachedToWindow-->isVisiable:" + isVisiable);
        isAttachedWindow = true;
        if (isVisiable) {
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//      Log.d(TAG, "onDetachedFromWindow");
        isAttachedWindow = false;
        stopAnim();
    }

    private void stopAnim() {
        removeCallbacks(mHorScrollRunnable);
        removeCallbacks(mVerScrollRunnable);
        isHavePendingRunnable = false;

        mScroller.abortAnimation();
        isVerScrolling = false;
        isHadHorScrolled = false;
        isHorScrolling = false;

        currIndex = 0;
        mScrollerX = mStartScrollerX;
        mScrollerY = mStartScrollerY;
    }

    /**
     * 设置展示的notices
     * @param notices
     */
    public void setNotices(List<CharSequence> notices) {
//        Log.d(TAG, "setNotices");
        if (isAttachedWindow && isVisiable) {
            stopAnim();
            mNotices = new ArrayList<>(notices);
            invalidate();
        } else {
            mNotices = new ArrayList<>(notices);
        }
    }

}
