package com.complete.recognition.cv;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:38 2019/10/22 0022
 * @Modified By:
 */
public class CVDilate {
    public static Mat dilateBrightRegion(Mat gray0) {
        Mat dst = new Mat(); // top-hat enhance contrast
        //top-hat转换
        Imgproc.morphologyEx(gray0, dst, Imgproc.MORPH_TOPHAT, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 3)));
        //高斯模糊
        Imgproc.GaussianBlur(dst, dst, new Size(13, 13), 0);
        //边缘检测
        Imgproc.Canny(dst, dst, 300, 600, 5, true);

        //膨胀
        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 5);
        Size heavy = new Size(35, 5); // apply a second dilate operation to the binary image
        Imgproc.dilate(dst, dst, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, heavy));

        System.out.println("bright");

        return dst;
    }

    public static Mat dilateDarkRegion(Mat gray0) {
        Mat dst = new Mat(); // enhance black area by black-hat
        //black-hat变换
        Imgproc.morphologyEx(gray0, dst, Imgproc.MORPH_BLACKHAT, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(18, 10)));
        //高斯模糊
        Imgproc.GaussianBlur(dst, dst, new Size(13, 13), 0);
        //边缘检测
        Imgproc.Canny(dst, dst, 300, 600, 5, true);

        //膨胀
        Size heavy = new Size(35, 3);
        Imgproc.dilate(dst, dst, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, heavy));

        System.out.println("black");

        return dst;
    }

    public static Mat fastDilate(Mat gray, boolean findBright) {
        if (findBright)
            return dilateBrightRegion(gray);
        else
            return dilateDarkRegion(gray);
    }
}