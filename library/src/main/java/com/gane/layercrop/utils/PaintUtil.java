/*
 * Copyright 2013, Edmodo, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */

package com.gane.layercrop.utils;

import android.graphics.Color;
import android.graphics.Paint;

public class PaintUtil {


    /**
     * 创建边框的画笔
     * @param borderThickness 边框厚度
     * @param borderColor 边框颜色
     */
    public static Paint newBorderPaint(float borderThickness, int borderColor) {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderThickness);
        paint.setColor(borderColor);
        return paint;
    }

    /**
     * 创建中间线条的画笔
     */
    public static Paint newGuidelinePaint(float guidelineThickness, int guidelineColor) {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(guidelineThickness);
        paint.setColor(guidelineColor);
        return paint;
    }

    /**
     * 创建周边区域的画笔
     */
    public static Paint newSurroundingAreaOverlayPaint(int overlayColor) {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(overlayColor);
        return paint;
    }

    /**
     * 创建四个角落的画笔
     */
    public static Paint newCornerPaint(float cornerThickness, int cornerColor) {
        final Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cornerThickness);
        paint.setColor(cornerColor);
        return paint;
    }
}
