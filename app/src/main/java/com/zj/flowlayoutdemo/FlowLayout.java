package com.zj.flowlayoutdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

import androidx.core.view.ViewConfigurationCompat;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {

    private ArrayList<View> lineViews;
    private List<ArrayList<View>> allViews;
    private List<Integer> heights;

    private int measureHeight;
    private int realHeight;
    /**
     * 用来判断是不是一次滑动
     */
    private int mTouchSlop;
    private OverScroller mScroller;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mLastInterceptX;
    private float mLastInterceptY;
    private VelocityTracker mVelocityTracker;
    private float mLastY;
    private boolean isScroller;


    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfiguration);
        mScroller = new OverScroller(context);
        mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
    }

    private void init() {
        lineViews = new ArrayList<>();
        allViews = new ArrayList<>();
        heights = new ArrayList<>();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        measureHeight = heightMeasureSize;
        init();

        // 当前行的宽和高
        int currLineWidth = 0, currLineHeight = 0;
        // 整个流式布局的宽和高
        int flowLayoutWidth = 0, flowLayoutHeight = 0;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {

            View childAt = getChildAt(i);
            measureChild(childAt, widthMeasureSpec, heightMeasureSpec);
            int childWidth = childAt.getMeasuredWidth();
            int childHeight = childAt.getMeasuredHeight();
            LayoutParams layoutParams = childAt.getLayoutParams();
            if (currLineWidth + childWidth > widthMeasureSize) {
                // 当前行宽大于布局测量的宽度
                allViews.add(lineViews);
                heights.add(currLineHeight);
                lineViews = new ArrayList<>();
                flowLayoutHeight += currLineHeight;
                flowLayoutWidth = Math.max(currLineWidth, flowLayoutWidth);
                currLineWidth = 0;
                currLineHeight = 0;
            }

            lineViews.add(childAt);
            currLineWidth += childWidth;
            if (layoutParams.height != LayoutParams.MATCH_PARENT) {
                currLineHeight = Math.max(currLineHeight, childHeight);
            } else {
                int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, childWidth);
                int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, LayoutParams.WRAP_CONTENT);
                childAt.measure(childWidthSpec, childHeightSpec);
                currLineHeight = Math.max(currLineHeight, childAt.getMeasuredHeight());
            }

            if (i == getChildCount() - 1) {
                // 最后一行
                flowLayoutHeight += currLineHeight;
                flowLayoutWidth = Math.max(flowLayoutWidth, currLineWidth);
                heights.add(currLineHeight);
                allViews.add(lineViews);
            }
        }

        if (heightMeasureMode == MeasureSpec.EXACTLY) {
            flowLayoutHeight = Math.max(heightMeasureSize, flowLayoutHeight);
        }
        //FlowLayout最终宽高
        realHeight = flowLayoutHeight;
        isScroller = realHeight > measureHeight;
        setMeasuredDimension(widthMeasureMode == MeasureSpec.EXACTLY ? widthMeasureSize : flowLayoutWidth, flowLayoutHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int curX = 0;
        int curY = 0;
        for (int i = 0; i < allViews.size(); i++) {
            ArrayList<View> views = allViews.get(i);
            Integer lineHeight = heights.get(i);
            for (View view : views) {
                int left = curX;
                int top = curY;
                int right = left + view.getMeasuredWidth();
                int bottom = top + view.getMeasuredHeight();
                view.layout(left, top, right, bottom);
                curX += view.getMeasuredWidth();
            }
            curY += lineHeight;
            curX = 0;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        float interceptX = ev.getX();
        float interceptY = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastInterceptX = ev.getX();
                mLastInterceptY = ev.getY();
                intercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = interceptX - mLastInterceptX;
                float dy = interceptY = mLastInterceptY;
                if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > mTouchSlop) {
                    intercept = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercept = false;
                break;
            default:
                intercept = false;
                break;
        }
        mLastInterceptX = interceptX;
        mLastInterceptY = interceptY;
        Log.e("zhang", "onInterceptTouchEvent: " + intercept);
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScroller) {
            return super.onTouchEvent(event);
        }
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        float currY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastY = currY;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = mLastY - currY;//本次手势滑动了多大距离
                mScroller.startScroll(0, mScroller.getFinalY(), 0, (int) dy);//mCurrY = oldScrollY + dy*scale;
                invalidate();
                Log.e("zhang", "onTouchEvent: " + dy);
                mLastY = currY;
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();

                if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                    fling(-initialVelocity);
                } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                        (realHeight - measureHeight))) {
                    postInvalidateOnAnimation();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            postInvalidate();
        }

    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = measureHeight;
            int bottom = realHeight;

            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, height / 2);

            postInvalidateOnAnimation();
        }
    }
}
