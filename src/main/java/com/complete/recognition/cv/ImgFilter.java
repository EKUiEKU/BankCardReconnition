package com.complete.recognition.cv;

import com.complete.recognition.cv.interfaces.RectFilter;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:56 2019/10/22 0022
 * @Modified By:
 */
public class ImgFilter implements RectFilter {
    public boolean isDigitRegion(Rect rect, int srcWidth, int srcHeight) {
        if (rect.width * rect.height < this.MIN_AREA) {
            return false;
        }
        if (srcHeight * this.MIN_HEIGHT_RATE > rect.height ||
                srcHeight * this.MAX_HEIGHT_RATE < rect.height) {
            return false;
        }
        if (srcWidth * this.MIN_WIDTH_RATE > rect.width) {
            return false;
        }
        return true;
    }

    /**
     * 先不管
     */
    public int IDRegionSimilarity(Mat m, Rect r, int rows, int cols) {
        int origin = 0;
        if (r.y < this.MIN_HEIGHT_RATE * rows)
            return origin;
        if (r.y > (1 - this.MIN_HEIGHT_RATE) * rows)
            return origin;
        int y_score = 9;
        int bottom = r.y + r.height;
        if (r.y > rows * 0.8)
            y_score = 5;
        if (bottom > rows * 0.9)
            y_score = 3;
        origin += r.y * y_score;
        float avgSimilarity = 0;
        List<MatOfPoint> cnt = new ArrayList<MatOfPoint>();
        Imgproc.findContours(m, cnt, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : cnt) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.height < MIN_AREA)
                continue;
            double cntArea = Imgproc.contourArea(contour);
            int frameSize = rect.width * r.height;
            avgSimilarity += (cntArea / frameSize);
            origin += cntArea;
        }

        avgSimilarity /= cnt.size();
        origin *= avgSimilarity;
        return origin;
    }

    /**
     * 先不管
     */
    public void findMaxRect(Mat m, Rect r) {
        int mainLeft = (int) (r.width * 0.1f) + r.x;
        int mainRight = (int) (r.width * 0.9f) + r.x;
        int mainCenter = (r.width >> 1) + r.x;
        int minWidth = (int) (m.cols() * MIN_WIDTH_RATE);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(m, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            int center = rect.x + rect.width / 2;
            if (center < mainLeft || center > mainRight ||
                    rect.height < MIN_AREA)
                continue;

            // rect frame 【 】
            int frameSize = rect.width * r.height;
            // white region size in frame 【==】
            int frameArea = Core.countNonZero(new Mat(m,
                    new Rect(rect.x, r.y, rect.width, r.height)));

            if (frameArea < frameSize * FULL_AREA_RATIO ||
                    (rect.height < r.height * FRAME_H_RATIO && rect.width > minWidth)) {

                if (center < mainCenter) {
                    r.width -= (center - r.x);
                    r.x = center;
                } else
                    r.width = center - r.x;

                break;
            }
        }

    }
}
