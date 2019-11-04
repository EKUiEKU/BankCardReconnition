package com.complete.recognition.cv;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:35 2019/10/22 0022
 * @Modified By:
 */
public class CVGrayTransfer {
    public static Mat grayTransferBeforeScale(String fileName) {
        Mat src = Imgcodecs.imread(fileName);
        final int mw = src.width() > 1024 ? 1024 : src.width();
        return grayTransferBeforeScale(src, mw);
    }

    public static Mat grayTransferBeforeScale(Mat m, int resizeWidth) {
        Mat resize;
        resize = resizeMat(m, resizeWidth);
        Mat dst = new Mat();
        Imgproc.cvtColor(resize, dst, Imgproc.COLOR_BGR2GRAY); //灰度化
        return dst;
    }

    public static Mat resizeMat(Mat m, int resizeWidth) {
        Mat scaleMat = new Mat();
        Imgproc.resize(m, scaleMat, new Size(resizeWidth,
                (float)m.height() / m.width() * resizeWidth), 0, 0, Imgproc.INTER_AREA);

        return scaleMat;
    }
}
