package com.complete.recognition;

import com.complete.recognition.cv.CVDilate;
import com.complete.recognition.cv.CVGrayTransfer;
import com.complete.recognition.cv.CVRegion;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.highgui.HighGui;


/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:30 2019/10/22 0022
 * @Modified By:
 */
public class Main {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    static class Producer extends CVRegion {

        public Producer(Mat graySrc) {
            super(graySrc);
        }

        /**
         定位矩形位置
         */
        public Rect findMainRect() {
            boolean findBright = false;
            Mat gray = this.grayMat;
            Rect bestRect = new Rect();
            final float fullWidth = gray.cols() - Producer.border * 2;
            boolean chose;

            //先进行dilateBrightRegion,如果未能定位到银行卡域,则执行dilateDarkRegion方法。
            for ( ; ; findBright = true) {
                // TODO 1.二值特征化
                Mat dilate = CVDilate.fastDilate(gray, findBright);

                HighGui.imshow("",dilate);
                HighGui.waitKey(0);

                Rect idRect = null;
                chose = false;
                try {
                    //TODO 2.开始定位图片
                    idRect = this.digitRegion(dilate);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (idRect != null) {
                    if (bestRect.width == 0)
                        chose = true;
                    else if (idRect.width < fullWidth) {
                        if (bestRect.width == fullWidth ||
                                idRect.width > bestRect.width)
                            chose = true;
                    }
                    if (chose) {
                        bestRect = idRect;
                    }
                }
                if (findBright) break;
            }

            if (bestRect.width == 0) {
                System.err.println("OCR Failed.");
                System.exit(1);
            }
            return bestRect;
        }
    }

    public static void main(String[] args) throws Exception {
        Strings.setFileDefault(Strings.FILE_NAME);
        String fileName = Strings.getFilePath();

        Mat gray = CVGrayTransfer.grayTransferBeforeScale(fileName);
        Producer producer = new Producer(gray);
        // 定位卡号矩形区域
        Rect mainRect = producer.findMainRect();



//        // 设置矩形区域
//        //producer.setRectOfDigitRow(mainRect);
//        // 窗口输出
//        // HighGui.imshow("id numbers", new Mat(gray, mainRect));
////        System.out.println(mainRect);
//
////        CVRegion cvRegion = new CVRegion(gray);
////        cvRegion.digitSeparate();
    }
}
