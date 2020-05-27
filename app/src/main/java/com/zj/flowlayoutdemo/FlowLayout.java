package com.zj.flowlayoutdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

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


    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
}
