package com.complete.recognition.cv.interfaces;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:55 2019/10/22 0022
 * @Modified By:
 */
public interface RectFilter {
    /**
     * minimum height or size in roi
     */
    int MIN_AREA = 10;
    float MIN_HEIGHT_RATE = 0.038f;
    float MAX_HEIGHT_RATE = 0.15f;
    float MIN_WIDTH_RATE = 0.12f;
    /**
     * filter out irrelevant areas of the credit card
     * @param rect
     * @return
     */
    boolean isDigitRegion(Rect rect, int srcWidth, int srcHeight);

    int HEIGHT_SCORE = 6;
    int WIDTH_SCORE = 3;
    int IDRegionSimilarity(Mat m, Rect r, int rows, int cols);

    float FULL_AREA_RATIO = 0.8f;
    float FRAME_H_RATIO = 0.7f;
    void findMaxRect(Mat m, Rect r);
}
