package com.gane.layercrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.gane.layercrop.edge.Edge;
import com.gane.layercrop.handle.Handle;
import com.gane.layercrop.utils.AspectRatioUtil;
import com.gane.layercrop.utils.HandleUtil;
import com.gane.layercrop.utils.PaintUtil;

/**
 *
 */
public class LayerCropView extends View {

    /** ---------------- 需要截图的View ---------------- */
    private View mAttachView;

    /** ---------------中间需要截图的区域 ----------------- */
    private RectF mCropRect = null;

    /**------------- 绘制整块灰色区域的画笔 -------------*/
    private Paint mOverlayPaint;
    private int mOverlayColor = Color.parseColor("#B0000000");

    /**---------------------- 边线 ---------------------- */
    private Paint mBorderPaint;
    private float mBorderThickness = px(3); // 边线的厚度
    private int mBorderColor = Color.BLUE; // 边线的颜色


    /** ---------------- 之间区域的分块线 ---------------- */
    private Paint mGuidelinePaint;
    private float mGuidelineThickness = 1;
    private int mGuidelineColor = Color.GREEN;
    // 分割线的显示动作
    private int mGuideAction = GUIDE_ACTION_OFF;
    public static final int GUIDE_ACTION_OFF = -1;
    public static final int GUIDE_ACTION_ON = 0;
    public static final int GUIDE_ACTION_TOUCH = 1;



    /** ---------------------- 4个角落 ------------------- */
    private Paint mCornerPaint;
    private float mCornerThickness = px(5); // 厚度
    private float mCornerLength = px(20); // 长度
    private int mCornerColor = Color.RED;


    private float mHandleRadius = px(24);
    private float mSnapRadius = px(3);
    /** 拖拽的点 */
    private PointF mTouchOffset = new PointF();
    private Handle mPressedHandle;

    private boolean mFixAspectRatio;
    private int mAspectRatioX = 1;
    private int mAspectRatioY = 1;

    public Bitmap crop(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        float left = Edge.LEFT.getCoordinate() - leftMargin;
        float top = Edge.TOP.getCoordinate() - topMargin;
        float right = Edge.RIGHT.getCoordinate() - rightMargin;
        float bottom = Edge.BOTTOM.getCoordinate() - bottomMargin;

        if (left < 0) left = 0;
        if (top < 0) top = 0;

        Bitmap cache = mAttachView.getDrawingCache();

        if (cache == null) return null;

        int width = (int) (right - left);
        int height = (int) (bottom - top);
        if (width <= 0 || width > cache.getWidth()) width = cache.getWidth();
        if (height <= 0 || height > cache.getHeight()) height = cache.getHeight();


        Bitmap bitmap = Bitmap.createBitmap(cache, (int) left, (int) top , width, height);

        return bitmap;
    }

    public LayerCropView(Context context) {
        super(context);
    }

    /**
     * 显示截图View
     *
     * @param attach 截图依附与的View视图
     */
    public void attach(View attach) {
        mAttachView = attach;
        mAttachView.setDrawingCacheEnabled(true); // 开启drawable cache 才能截图

        ViewGroup viewGroup = (ViewGroup) attach.getParent();
        viewGroup.addView(this);

        mCropRect = new RectF(attach.getX(), attach.getY(), attach.getMeasuredWidth(), attach.getMeasuredHeight());

        mBorderPaint = PaintUtil.newBorderPaint(mBorderThickness, mBorderColor);
        mGuidelinePaint = PaintUtil.newGuidelinePaint(mGuidelineThickness, mGuidelineColor);
        mOverlayPaint = PaintUtil.newSurroundingAreaOverlayPaint(mOverlayColor);
        mCornerPaint = PaintUtil.newCornerPaint(mCornerThickness, mCornerColor);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initCropWindow(mCropRect);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawDarkenedSurroundingArea(canvas);
        drawGuidelines(canvas);
        drawBorder(canvas);
        drawCorners(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false; // view被禁用

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onActionDown(event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onActionUp();
                return true;

            case MotionEvent.ACTION_MOVE:
                onActionMove(event.getX(), event.getY());
                // 告诉 parent view 自己处理改事件
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            default: return false;
        }
    }

//    public void setGuidelines(int guidelinesMode) {
//        mGuidelinesMode = guidelinesMode;
//        invalidate(); // Request onDraw() to get called again.
//    }

    public void setFixAspectRatio(boolean fixAspectRatio) {
        mFixAspectRatio = fixAspectRatio;
        requestLayout(); // measure/layout
    }

    public void setAspectRatio(int aspectRatioX, int aspectRatioY) {
        if (aspectRatioX <= 0 || aspectRatioY <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        }
        mAspectRatioX = aspectRatioX;
        mAspectRatioY = aspectRatioY;

        if (mFixAspectRatio) {
            requestLayout(); // Request measure/layout to be run again.
        }
    }

    /**
     * 初始化中间截图区域的范围
     * */
    private void initCropWindow(RectF bitmapRect) {
        if (mFixAspectRatio) {
            initCropWindowWithFixedAspectRatio(bitmapRect);
        } else {
            final float horizontalPadding = 0.1f * bitmapRect.width();
            final float verticalPadding = 0.1f * bitmapRect.height();

            Edge.LEFT.setCoordinate(bitmapRect.left + horizontalPadding);
            Edge.TOP.setCoordinate(bitmapRect.top + verticalPadding);
            Edge.RIGHT.setCoordinate(bitmapRect.right - horizontalPadding);
            Edge.BOTTOM.setCoordinate(bitmapRect.bottom - verticalPadding);
        }
    }

    private void initCropWindowWithFixedAspectRatio(RectF bitmapRect) {
        if (AspectRatioUtil.calculateAspectRatio(bitmapRect) > getTargetAspectRatio()) {
            final float cropWidth = AspectRatioUtil.calculateWidth(bitmapRect.height(), getTargetAspectRatio());

            Edge.LEFT.setCoordinate(bitmapRect.centerX() - cropWidth / 2f);
            Edge.TOP.setCoordinate(bitmapRect.top);
            Edge.RIGHT.setCoordinate(bitmapRect.centerX() + cropWidth / 2f);
            Edge.BOTTOM.setCoordinate(bitmapRect.bottom);
        } else {
            final float cropHeight = AspectRatioUtil.calculateHeight(bitmapRect.width(), getTargetAspectRatio());

            Edge.LEFT.setCoordinate(bitmapRect.left);
            Edge.TOP.setCoordinate(bitmapRect.centerY() - cropHeight / 2f);
            Edge.RIGHT.setCoordinate(bitmapRect.right);
            Edge.BOTTOM.setCoordinate(bitmapRect.centerY() + cropHeight / 2f);
        }
    }

    private float getTargetAspectRatio() {
        return mAspectRatioX / (float) mAspectRatioY;
    }


    /**
     * 绘制四边灰色的区域(也就是底层)
     */
    private void drawDarkenedSurroundingArea(Canvas canvas) {
        final RectF bitmapRect = mCropRect;

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();
        /*-
          -------------------------------------
          |                top                |
          -------------------------------------
          |      |                    |       |
          |      |                    |       |
          | left |        bitmap      | right |
          |      |                    |       |
          |      |                    |       |
          -------------------------------------
          |              bottom               |
          -------------------------------------
         */
        canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, top, mOverlayPaint);
        canvas.drawRect(bitmapRect.left, bottom, bitmapRect.right, bitmapRect.bottom, mOverlayPaint);
        canvas.drawRect(bitmapRect.left, top, left, bottom, mOverlayPaint);
        canvas.drawRect(right, top, bitmapRect.right, bottom, mOverlayPaint);
    }

    /**
     * 绘制中间分块的横线
     */
    private void drawGuidelines(Canvas canvas) {
        if (!shouldGuidelinesBeShown()) return;

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        // 分3块， 计算每块的宽度
        final float oneThirdCropWidth = Edge.getWidth() / 3;

        final float x1 = left + oneThirdCropWidth;
        canvas.drawLine(x1, top, x1, bottom, mGuidelinePaint);

        final float x2 = right - oneThirdCropWidth;
        canvas.drawLine(x2, top, x2, bottom, mGuidelinePaint);

        final float oneThirdCropHeight = Edge.getHeight() / 3;

        // 分3块， 计算每块的高度
        final float y1 = top + oneThirdCropHeight;

        canvas.drawLine(left, y1, right, y1, mGuidelinePaint);
        final float y2 = bottom - oneThirdCropHeight;
        canvas.drawLine(left, y2, right, y2, mGuidelinePaint);
    }

    private boolean shouldGuidelinesBeShown() {
        return ((mGuideAction == GUIDE_ACTION_ON) || ((mGuideAction == GUIDE_ACTION_TOUCH) && (mPressedHandle != null)));
    }

    /**
     * 绘制边线
     */
    private void drawBorder(Canvas canvas) {
        canvas.drawRect(
                Edge.LEFT.getCoordinate(),
                Edge.TOP.getCoordinate(),
                Edge.RIGHT.getCoordinate(),
                Edge.BOTTOM.getCoordinate(),
                mBorderPaint);
    }

    /**
     * 绘制4个角落
     */
    private void drawCorners(Canvas canvas) {
        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        final float lateralOffset = (mCornerThickness - mBorderThickness) / 2f;
        final float startOffset = mCornerThickness - (mBorderThickness / 2f);

        // 绘制 Top-left left 边线
        canvas.drawLine(left - lateralOffset, top - startOffset, left - lateralOffset, top + mCornerLength, mCornerPaint);
        // 绘制 Top-left top 边线
        canvas.drawLine(left - startOffset, top - lateralOffset, left + mCornerLength, top - lateralOffset, mCornerPaint);


        // Top-right right
        canvas.drawLine(right + lateralOffset, top - startOffset, right + lateralOffset, top + mCornerLength, mCornerPaint);
        // Top-right top
        canvas.drawLine(right + startOffset, top - lateralOffset, right - mCornerLength, top - lateralOffset, mCornerPaint);

        // Bottom-left left
        canvas.drawLine(left - lateralOffset, bottom + startOffset, left - lateralOffset, bottom - mCornerLength, mCornerPaint);
        // Bottom-left bottom
        canvas.drawLine(left - startOffset, bottom + lateralOffset, left + mCornerLength, bottom + lateralOffset, mCornerPaint);

        // Bottom-right  right
        canvas.drawLine(right + lateralOffset, bottom + startOffset, right + lateralOffset, bottom - mCornerLength, mCornerPaint);
        // Bottom-right  bottom
        canvas.drawLine(right + startOffset, bottom + lateralOffset, right - mCornerLength, bottom + lateralOffset, mCornerPaint);
    }

    /**
     * 手指按下
     */
    private void onActionDown(float x, float y) {
        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        mPressedHandle = HandleUtil.getPressedHandle(x, y, left, top, right, bottom, mHandleRadius);
        if (mPressedHandle != null) {
            HandleUtil.getOffset(mPressedHandle, x, y, left, top, right, bottom, mTouchOffset);
            invalidate();
        }
    }

    private void onActionUp() {
        if (mPressedHandle != null) {
            mPressedHandle = null;
            invalidate();
        }
    }

    private void onActionMove(float x, float y) {
        if (mPressedHandle == null) return;

        x += mTouchOffset.x;
        y += mTouchOffset.y;

        if (mFixAspectRatio) {
            mPressedHandle.updateCropWindow(x, y, getTargetAspectRatio(), mCropRect, mSnapRadius);
        } else {
            mPressedHandle.updateCropWindow(x, y, mCropRect, mSnapRadius);
        }
        invalidate();
    }





    private float px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return dp * density + 0.5f;
    }



}
