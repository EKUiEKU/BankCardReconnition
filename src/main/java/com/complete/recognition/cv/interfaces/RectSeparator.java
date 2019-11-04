package com.complete.recognition.cv.interfaces;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:49 2019/10/22 0022
 * @Modified By:
 */
public interface RectSeparator {
    List<Rect> rectSeparate(Mat src, Rect region) throws Exception;
}
