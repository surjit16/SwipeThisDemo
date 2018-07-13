package com.example.swipethis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

@SuppressLint("RtlHardcoded")
public class SwipeThis extends ViewGroup {

    public static final int NORMAL = 0;
    public static final int FROM_LEFT = 1;
    public static final int FROM_RIGHT = 2;
    public static final int MODE_SAME_LEVEL = 1;
    private static final String SAVED_INSTANCE_STATE = "saved_state";
    private static final int DEFAULT_SWAP_VELOCITY = 300;
    private static final int DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT = 1;

    private View mMainView, mSecondaryView;
    private Rect mRectMainOpen = new Rect();
    private Rect mRectMainClose = new Rect();
    private Rect mRectSecondaryOpen = new Rect();
    private Rect mRectSecondaryClose = new Rect();
    private int mMinDistRequestDisallowParent = 0;
    private boolean mIsOpenBeforeInit = false;
    private volatile boolean mIsScrolling = false;
    private volatile boolean mLockDrag = false;
    private int mMinFlingVelocity = DEFAULT_SWAP_VELOCITY;
    private int mMode = NORMAL;
    private int mDragEdge = FROM_LEFT;
    private final GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        boolean isDisallowed = false;

        @Override
        public boolean onDown(MotionEvent e) {
            mIsScrolling = false;
            isDisallowed = false;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mIsScrolling = true;
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mIsScrolling = true;

            if (getParent() != null) {
                boolean shouldDisallow;

                if (!isDisallowed) {
                    shouldDisallow = getDistToClosestEdge() >= mMinDistRequestDisallowParent;
                    if (shouldDisallow) {
                        isDisallowed = true;
                    }
                } else {
                    shouldDisallow = true;
                }

                getParent().requestDisallowInterceptTouchEvent(shouldDisallow);
            }

            return false;
        }
    };
    private float mDragDist = 0;
    private float mPrevX = -1;
    private ViewDragHelper mDragHelper;
    private final ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {

            if (mLockDrag)
                return false;

            mDragHelper.captureChildView(mMainView, pointerId);
            return false;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            switch (mDragEdge) {
                case FROM_LEFT:
                    return Math.max(Math.min(left, mRectMainClose.left + mSecondaryView.getWidth()), mRectMainClose.left);

                case FROM_RIGHT:
                    return Math.max(Math.min(left, mRectMainClose.left), mRectMainClose.left - mSecondaryView.getWidth());

                default:
                    return child.getLeft();
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xVel, float yVel) {
            final boolean velRightExceeded = pxToDpi((int) xVel) >= mMinFlingVelocity;
            final boolean velLeftExceeded = pxToDpi((int) xVel) <= -mMinFlingVelocity;

            final int pivotHorizontal = getHalfwayPivotHorizontal();

            switch (mDragEdge) {
                case FROM_LEFT:
                    if (velRightExceeded) {
                        open(true);
                    } else if (velLeftExceeded) {
                        close(true);
                    } else {
                        if (mMainView.getLeft() < pivotHorizontal) {
                            close(true);
                        } else {
                            open(true);
                        }
                    }
                    break;

                case FROM_RIGHT:
                    if (velRightExceeded) {
                        close(true);
                    } else if (velLeftExceeded) {
                        open(true);
                    } else {
                        if (mMainView.getRight() < pivotHorizontal) {
                            open(true);
                        } else {
                            close(true);
                        }
                    }
                    break;

            }
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            super.onEdgeDragStarted(edgeFlags, pointerId);

            if (mLockDrag) return;
            boolean edgeStartRight = (mDragEdge == FROM_LEFT) && edgeFlags == ViewDragHelper.EDGE_RIGHT;
            boolean edgeStartLeft = (mDragEdge == FROM_RIGHT) && edgeFlags == ViewDragHelper.EDGE_LEFT;

            if (edgeStartLeft || edgeStartRight)
                mDragHelper.captureChildView(mMainView, pointerId);
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            if (mMode == MODE_SAME_LEVEL) {
                if (mDragEdge == FROM_LEFT || mDragEdge == FROM_RIGHT) {
                    mSecondaryView.offsetLeftAndRight(dx);
                } else {
                    mSecondaryView.offsetTopAndBottom(dy);
                }
            }
            ViewCompat.postInvalidateOnAnimation(SwipeThis.this);
        }
    };
    private GestureDetectorCompat mGestureDetector;

    public SwipeThis(Context context) {
        super(context);
        init(context, null);
    }

    public SwipeThis(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwipeThis(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private int pxToDpi(int px) {
        Resources resources = getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SAVED_INSTANCE_STATE, super.onSaveInstanceState());
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        state = bundle.getParcelable(SAVED_INSTANCE_STATE);
        super.onRestoreInstanceState(state);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isDragLocked())
            return super.onInterceptTouchEvent(ev);

        mDragHelper.processTouchEvent(ev);
        mGestureDetector.onTouchEvent(ev);
        accumulateDragDist(ev);

        boolean couldBecomeClick = couldBecomeClick(ev);
        boolean settling = mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING;
        boolean idleAfterScrolled = mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE && mIsScrolling;

        mPrevX = ev.getX();
        return !couldBecomeClick && (settling || idleAfterScrolled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() >= 2) {
            mSecondaryView = getChildAt(0);
            mMainView = getChildAt(1);
        } else if (getChildCount() == 1)
            mMainView = getChildAt(0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int index = 0; index < getChildCount(); index++) {
            final View child = getChildAt(index);

            int left, right, top, bottom;
            left = right = top = bottom = 0;

            final int minLeft = getPaddingLeft();
            final int maxRight = Math.max(r - getPaddingRight() - l, 0);
            final int minTop = getPaddingTop();
            final int maxBottom = Math.max(b - getPaddingBottom() - t, 0);

            int measuredChildHeight = child.getMeasuredHeight();
            int measuredChildWidth = child.getMeasuredWidth();

            final LayoutParams childParams = child.getLayoutParams();
            boolean matchParentHeight = false;
            boolean matchParentWidth = false;

            if (childParams != null) {
                matchParentHeight = (childParams.height == LayoutParams.MATCH_PARENT) || (childParams.height == MATCH_PARENT);
                matchParentWidth = (childParams.width == LayoutParams.MATCH_PARENT) || (childParams.width == MATCH_PARENT);
            }

            if (matchParentHeight) {
                measuredChildHeight = maxBottom - minTop;
                childParams.height = measuredChildHeight;
            }

            if (matchParentWidth) {
                measuredChildWidth = maxRight - minLeft;
                childParams.width = measuredChildWidth;
            }

            switch (mDragEdge) {
                case FROM_LEFT:
                    left = Math.min(getPaddingLeft(), maxRight);
                    top = Math.min(getPaddingTop(), maxBottom);
                    right = Math.min(measuredChildWidth + getPaddingLeft(), maxRight);
                    bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
                    break;

                case FROM_RIGHT:
                    left = Math.max(r - measuredChildWidth - getPaddingRight() - l, minLeft);
                    top = Math.min(getPaddingTop(), maxBottom);
                    right = Math.max(r - getPaddingRight() - l, minLeft);
                    bottom = Math.min(measuredChildHeight + getPaddingTop(), maxBottom);
                    break;
            }

            child.layout(left, top, right, bottom);
        }

        if (mMode == MODE_SAME_LEVEL) {
            switch (mDragEdge) {
                case FROM_LEFT:
                    mSecondaryView.offsetLeftAndRight(-mSecondaryView.getWidth());
                    break;

                case FROM_RIGHT:
                    mSecondaryView.offsetLeftAndRight(mSecondaryView.getWidth());
                    break;
            }
        }

        initRect();

        if (mIsOpenBeforeInit) {
            open(false);
        } else {
            close(false);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() < 2)
            throw new RuntimeException("Minimum two children's required");

        final LayoutParams params = getLayoutParams();

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int desiredWidth = 0;
        int desiredHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            desiredWidth = Math.max(child.getMeasuredWidth(), desiredWidth);
            desiredHeight = Math.max(child.getMeasuredHeight(), desiredHeight);
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredWidth, widthMode);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredHeight, heightMode);

        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams childParams = child.getLayoutParams();

            if (childParams != null) {
                if (childParams.height == LayoutParams.MATCH_PARENT)
                    child.setMinimumHeight(measuredHeight);

                if (childParams.width == LayoutParams.MATCH_PARENT)
                    child.setMinimumWidth(measuredWidth);
            }

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            desiredWidth = Math.max(child.getMeasuredWidth(), desiredWidth);
            desiredHeight = Math.max(child.getMeasuredHeight(), desiredHeight);
        }

        desiredWidth += getPaddingLeft() + getPaddingRight();
        desiredHeight += getPaddingTop() + getPaddingBottom();

        if (widthMode == MeasureSpec.EXACTLY)
            desiredWidth = measuredWidth;
        else {
            if (params.width == LayoutParams.MATCH_PARENT)
                desiredWidth = measuredWidth;

            if (widthMode == MeasureSpec.AT_MOST)
                desiredWidth = (desiredWidth > measuredWidth) ? measuredWidth : desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY)
            desiredHeight = measuredHeight;
        else {
            if (params.height == LayoutParams.MATCH_PARENT)
                desiredHeight = measuredHeight;

            if (heightMode == MeasureSpec.AT_MOST)
                desiredHeight = (desiredHeight > measuredHeight) ? measuredHeight : desiredHeight;
        }

        setMeasuredDimension(desiredWidth, desiredHeight);
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true))
            ViewCompat.postInvalidateOnAnimation(this);
    }

    public void open(boolean animation) {
        mIsOpenBeforeInit = true;

        if (animation)
            mDragHelper.smoothSlideViewTo(mMainView, mRectMainOpen.left, mRectMainOpen.top);
        else {
            mDragHelper.abort();
            mMainView.layout(mRectMainOpen.left, mRectMainOpen.top, mRectMainOpen.right, mRectMainOpen.bottom);
            mSecondaryView.layout(mRectSecondaryOpen.left, mRectSecondaryOpen.top, mRectSecondaryOpen.right, mRectSecondaryOpen.bottom);
        }

//        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void close(boolean animation) {
        mIsOpenBeforeInit = false;

        if (animation)
            mDragHelper.smoothSlideViewTo(mMainView, mRectMainClose.left, mRectMainClose.top);
        else {
            mDragHelper.abort();
            mMainView.layout(mRectMainClose.left, mRectMainClose.top, mRectMainClose.right, mRectMainClose.bottom);
            mSecondaryView.layout(mRectSecondaryClose.left, mRectSecondaryClose.top, mRectSecondaryClose.right, mRectSecondaryClose.bottom);
        }

//        ViewCompat.postInvalidateOnAnimation(this);
    }

    public boolean isDragLocked() {
        return mLockDrag;
    }

    private int getMainOpenLeft() {
        switch (mDragEdge) {
            case FROM_LEFT:
                return mRectMainClose.left + mSecondaryView.getWidth();

            case FROM_RIGHT:
                return mRectMainClose.left - mSecondaryView.getWidth();

            default:
                return 0;
        }
    }

    private int getMainOpenTop() {
        switch (mDragEdge) {
            case FROM_LEFT:
                return mRectMainClose.top;

            case FROM_RIGHT:
                return mRectMainClose.top;


            default:
                return 0;
        }
    }

    private int getSecondaryOpenLeft() {
        return mRectSecondaryClose.left;
    }

    private int getSecondaryOpenTop() {
        return mRectSecondaryClose.top;
    }

    private void initRect() {
        mRectMainClose.set(mMainView.getLeft(), mMainView.getTop(), mMainView.getRight(), mMainView.getBottom());

        mRectSecondaryClose.set(mSecondaryView.getLeft(), mSecondaryView.getTop(), mSecondaryView.getRight(), mSecondaryView.getBottom());

        mRectMainOpen.set(getMainOpenLeft(), getMainOpenTop(), getMainOpenLeft() + mMainView.getWidth(), getMainOpenTop() + mMainView.getHeight());

        mRectSecondaryOpen.set(getSecondaryOpenLeft(), getSecondaryOpenTop(), getSecondaryOpenLeft() + mSecondaryView.getWidth(), getSecondaryOpenTop() + mSecondaryView.getHeight());
    }

    private boolean couldBecomeClick(MotionEvent ev) {
        return isInMainView(ev) && !shouldInitiateADrag();
    }

    private boolean isInMainView(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();

        boolean withinVertical = mMainView.getTop() <= y && y <= mMainView.getBottom();
        boolean withinHorizontal = mMainView.getLeft() <= x && x <= mMainView.getRight();

        return withinVertical && withinHorizontal;
    }

    private boolean shouldInitiateADrag() {
        float minDistToInitiateDrag = mDragHelper.getTouchSlop();
        return mDragDist >= minDistToInitiateDrag;
    }

    private void accumulateDragDist(MotionEvent ev) {
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mDragDist = 0;
            return;
        }

        float dragged = Math.abs(ev.getX() - mPrevX);

        mDragDist += dragged;
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null && context != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwipeThis, 0, 0);

            mDragEdge = a.getInteger(R.styleable.SwipeThis_From, FROM_LEFT);
            mMode = NORMAL;
            mMinFlingVelocity = DEFAULT_SWAP_VELOCITY;
            mMinDistRequestDisallowParent = DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT;
        }

        mDragHelper = ViewDragHelper.create(this, 1.0f, mDragHelperCallback);
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL);

        mGestureDetector = new GestureDetectorCompat(context, mOnGestureListener);
    }

    private int getDistToClosestEdge() {
        switch (mDragEdge) {
            case FROM_LEFT:
                final int pivotRight = mRectMainClose.left + mSecondaryView.getWidth();
                return Math.min(mMainView.getLeft() - mRectMainClose.left, pivotRight - mMainView.getLeft());

            case FROM_RIGHT:
                final int pivotLeft = mRectMainClose.right - mSecondaryView.getWidth();
                return Math.min(mMainView.getRight() - pivotLeft, mRectMainClose.right - mMainView.getRight());
        }

        return 0;
    }

    private int getHalfwayPivotHorizontal() {
        if (mDragEdge == FROM_LEFT)
            return mRectMainClose.left + mSecondaryView.getWidth() / 2;
        else
            return mRectMainClose.right - mSecondaryView.getWidth() / 2;
    }
}
