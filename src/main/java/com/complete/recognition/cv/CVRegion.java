package com.complete.recognition.cv;

import com.complete.recognition.Strings;
import com.complete.recognition.utils.Resources;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.*;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:38 2019/10/22 0022
 * @Modified By:
 */
public class CVRegion extends ImgSeparator{

    public static final int border = 25;
    public static final int blockRect = 30;

    // 定位后卡号区域二值图像
    private Mat binDigitRegion;

    public CVRegion(Mat graySrc) {
        super(graySrc);
        binDigitRegion = null;
    }


    protected Rect cutEdgeOfY(Mat binSingleDigit) {
        return null;
    }

    protected void cutEdgeOfX(Rect rect) {
        Mat dst = new Mat();
        Imgproc.GaussianBlur(grayMat, dst, new Size(13, 13), 0);
        Imgproc.Canny(dst, dst, 300, 600, 5, true);
        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 1);
        Mat m = new Mat(dst, rect);
        byte buff[] = new byte[m.rows() * m.cols()];
        m.get(0, 0, buff);
        int rows = rect.height;
        int cols = rect.width;
        int left = rect.x;
        int right = rect.x + rect.width;
        int w = 0;
        for (int i = 0; i < (cols >> 1); i++) {
            int h = 0;
            for (int j = 0; j < rows; j++) {
                int at = j * cols + i;
                if (buff[at] == 0 && w == 0) {
                    break;
                }
                if (buff[at] != 0) ++h;
            }
            if (w > 0 && h == 0) break;
            if (h == rows) ++w;
            if (w > 0)
                left = rect.x + i;
        }

        byte b[] = new byte[dst.cols() * dst.rows()];
        dst.get(0, 0 ,b);
        if (w > 0) {
            int max = 0;
            for (int i = 0; i < w; i++) {
                int h = extendHeight(b, dst.cols(), left - i, rect.y);
                max = Math.max(max, h);
            }
            // reset
            if (max < rect.height * 1.5)
                left = rect.x;
        }
        // right edge
        w = 0;
        for (int i = cols - 1; i > (cols >> 1); i--) {
            int h = 0;
            for (int j = 0; j < rows; j++) {
                int at = j * cols + i;
                if (buff[at] == 0 && w == 0)
                    break;
                if (buff[at] != 0) ++h;
            }
            if (w > 0 && h == 0) break;
            if (h == rows) w++;
            if (w > 0)
                right = rect.x + i;
        }
        if (w > 0) {
            int max = 0;
            for (int i = 0; i < w; i++) {
                int h = extendHeight(b, dst.cols(), right + i, rect.y);
                max = Math.max(max, h);
            }
            if (max < rect.height * 1.5)
                right = rect.x + rect.width;
        }
        rect.x = left;
        rect.width = right - left;
    }

    /**
     * fill image border with black pix
     *
     * @param m
     */
    public static void fillBorder(Mat m) {
        int cols = m.cols();
        int rows = m.rows();
        byte buff[] = new byte[cols * rows];
        //Mat转byte[s]
        m.get(0, 0, buff);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {

                //过滤边界border=10
                if ((i > border && j > border) &&
                        (i < cols - border && j < rows - border))
                    continue;

                //填充成黑色
                buff[j * cols + i] = 0;
            }
        }
        m.put(0, 0, buff);
    }

    /**
     * loc the digit area
     *
     * @param src mat proc by binary, top-hat, dilate and closed opr
     * @return
     */
    public Rect digitRegion(Mat src) throws Exception {
        if (src.cols() < 20 || src.rows() < 20)
            throw new Exception("error: image.cols() < 20 || image.rows() < 20 in function 'digitRegion(Mat m)'");

        //TODO 3.将边界填充成黑色。
        fillBorder(src);
        // 连通域分析，提取所有白色 (255) 区域
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        //TODO 4.寻找轮廓
        Imgproc.findContours(src, contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //TODO 5.定义轮廓分类器
        Filter filter = new Filter(src);
        Rect rect;
        boolean mode;
        for (mode = true;;mode = false){
            //TODO 6.开始分类
            rect = filter.boundingIdRect(contours,mode);
            if (mode == false || rect != null)
                break;
        }


        if (rect != null) {
            /**
             检测类型为2-16或连串的长条形区域
             参考到第4节
             */
            System.out.println("检测类型为2-16或连串的长条形区域");
        }
        if (rect == null)
            return null;
//        // 详细请移步第4节
//        cutEdgeOfX(rect);
        /**
         成功定位卡号矩形区域
         */
        return rect;
    }

    /**
     * 开始进行字符的分割
     * @throws Exception
     */
    public void digitSeparate() throws Exception {
        //模拟数据
        rectOfDigitRow = new Rect();
        rectOfDigitRow.x = 0;
        rectOfDigitRow.y = 0;
        rectOfDigitRow.width = grayMat.width();
        rectOfDigitRow.height = grayMat.height();

        Mat binDigits = new Mat(grayMat, getRectOfDigitRow()).clone();

        // 浮雕类型字体
            Mat sqKernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT, new Size(5, 5));

            Mat dst0 = new Mat();
            Imgproc.morphologyEx(binDigits, dst0, Imgproc.MORPH_TOPHAT,
                    sqKernel);

            Imgproc.morphologyEx(dst0, dst0, Imgproc.MORPH_GRADIENT,
                    sqKernel);

            Imgproc.threshold(dst0, dst0, 0, 255, Imgproc.THRESH_BINARY |
                    Imgproc.THRESH_OTSU);

            Imgproc.medianBlur(dst0, dst0, 3);

            Mat dst1 = new Mat();
            Imgproc.morphologyEx(binDigits, dst1, Imgproc.MORPH_BLACKHAT,
                    sqKernel);
            Imgproc.morphologyEx(dst1, dst1, Imgproc.MORPH_GRADIENT,
                    sqKernel);
            Imgproc.medianBlur(dst1, dst1, 3);
            Imgproc.threshold(dst1, dst1, 0, 255, Imgproc.THRESH_BINARY |
                    Imgproc.THRESH_OTSU);

            Core.bitwise_or(dst0, dst1, dst1);

            Imgproc.morphologyEx(dst1, dst1, Imgproc.MORPH_CLOSE,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

            Imgproc.dilate(dst1, binDigits,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 5)));



        this.binDigitRegion = binDigits;
//
//        HighGui.imshow("",binDigits);
//        HighGui.waitKey(0);

        // 先忽略
        setSingleDigits();
    }

    /**
     *  新增一个内部类，提供简单的区域定位 (4-4-4-4)。
     */
    final static class Filter extends ImgFilter {
        private Mat src;
        public Filter(Mat src) {
            this.src = src;
        }

        private void sortMap(int [][]a) {
            for (int i = 0; i < a[1].length - 1; i++) {
                int k = i;
                for (int j = i + 1; j < a[1].length; j++) {
                    if (a[1][k] > a[1][j]) {
                        k = j;
                    }
                }
                if (k != i) {
                    a[1][k] = a[1][k] + a[1][i];
                    a[1][i] = a[1][k] - a[1][i];
                    a[1][k] = a[1][k] - a[1][i];
                    a[0][i] = a[0][k] + a[0][i];
                    a[0][k] = a[0][i] - a[0][k];
                    a[0][i] = a[0][i] - a[0][k];
                }
            }
        }

        /**
         * get rect area of id numbers, only work at the 4-4-4-4 type
         * @param contours
         * @return null if rect of id area not found
         */
        public Rect boundingIdRect(List<MatOfPoint> contours,boolean mode) {
            Rect rect;
            List<Rect> rectSet = new ArrayList<Rect>();
            for (int i = 0; i < contours.size(); i++) {
//                MatOfPoint2f srcPoint2f =new MatOfPoint2f(contours.get(i).toArray());
//                MatOfPoint2f dstPoint2f = new MatOfPoint2f();
//                double epsilon = 0.05 * Imgproc.arcLength(srcPoint2f,true);
//                Imgproc.approxPolyDP(srcPoint2f,dstPoint2f,epsilon,true);
//

                if (mode) {
                    //TODO 7.1 模式一,cv的函数boundingRect
                    rect = Imgproc.boundingRect(contours.get(i));
                }else{
                    //TODO 7.2 模式二,自己定义的函数boundingRect
                    rect = boundingRect(contours.get(i));
                }
//                System.out.println("rect " + rect);
//                String fileName = Strings.getFilePath(Strings.FILE_NAME);
//                Mat mat = CVGrayTransfer.grayTransferBeforeScale(fileName);
//                mat = new Mat(mat,rect);
//
//
//                List<MatOfPoint> matOfPoints = new ArrayList<>();
//                MatOfPoint matOfPoint = new MatOfPoint(dstPoint2f.toArray());
//                matOfPoints.add(matOfPoint);
//                Imgproc.polylines(mat,matOfPoints,true,new Scalar(255,0,0));
//
//                HighGui.imshow("",mat);
//                HighGui.waitKey(0);

                rectSet.add(rect);
            }

            String fileName = Strings.getFilePath(Strings.FILE_NAME_5);
            Mat mat = CVGrayTransfer.grayTransferBeforeScale(fileName);

            for (int i = 0; i < rectSet.size(); i++) {
                Imgproc.rectangle(mat,rectSet.get(i),new Scalar(255,255,255));
            }
            HighGui.imshow("",mat);
            HighGui.waitKey(0);


            //开始合并银行卡号

            //以下代码还未解析 16点22分 2019.10.24 吴少聪
//            rect = rectSet.get(0);
//            int dist[][] = new int[2][rectSet.size()];
//            for (int i = 0; i < rectSet.size(); i++) {
//                dist[0][i] = i;
//                dist[1][i] = rectSet.get(i).y - rect.y;
//            }
//
//            System.out.println("rect = rectSet.get(0);->" + rect.toString());
//
//            sortMap(dist);
//
//
//            /**
//             * TODO: 20点38分 2019.11.2 解读
//             */
//            //偏差
//            final int verBias = 15;
//            for (int i = 0; i < dist[1].length - 2; i++) {
//                if (dist[1][i + 2] - dist[1][i] < verBias) {
//                    int k;
//                    /**
//                     * Upper left and lower right corners
//                     */
//                    int sx = src.width();
//                    int sy = src.height();
//                    int mx = -1;
//                    int my = -1;
//                    // max width between these id-digit area
//                    int mw = 0;
//                    int sw = src.width();
//                    for (k = 0; k < 3; k ++) {
//                        rect = rectSet.get(dist[0][k + i]);
//                        if (!isDigitRegion(rect, src.width(),src.height()))
//                            break;
//
//                        sx = Math.min(rect.x, sx);
//                        sy = Math.min(rect.y, sy);
//                        mx = Math.max(rect.x + rect.width, mx);
//                        my = Math.max(rect.y + rect.height, my);
//                        mw = Math.max(rect.width, mw);
//                        sw = Math.min(rect.width, sw);
//                    }
//
//                    // less than 3 area, find next
//                    if (k < 3) {
//                        continue;
//                    }
//
//                    if (i < dist[1].length - 3) {
//                        if (dist[1][i + 3] - dist[1][i] < verBias &&
//                                isDigitRegion(rect =
//                                                rectSet.get(dist[0][i + 3]),
//                                        src.width(), src.height())) {
//                            sx = Math.min(sx, rect.x);
//                            sy = Math.min(sy, rect.y);
//                            mx = Math.max(rect.x + rect.width, mx);
//                            my = Math.max(rect.y + rect.height, my);
//                            // finding out all 4 digit area
//                            return new Rect(sx, sy, mx - sx, my - sy);
//                        }
//                    }
//
//                    // completing 4th digit area
//                    int mg;
//                    //to make the gap largest,avoiding losing digit message
//                    int gap = (mx - sx - sw * 3) >> 1;
//                    Rect rt;
//                    if (sx < (mg = src.width() - mx - gap)) {
//                        rt = mg > mw ?
//                                new Rect(sx, sy, mx + mw + gap - sx , my - sy):
//                                new Rect(10, sy, src.width() - 20, my - sy);
//                    }
//                    else {
//                        mg = sx - gap;
//                        rt = mg > mw ?
//                                new Rect(sx - mw -gap, sy, mx - sx +mw, my -sy):
//                                new Rect(10, sy, src.width() - 20, my - sy);
//                    }
//
//                    return rt;
//                }
//            }

            /**
             * @Modified By WuShaoCong
             * @Modified Time:15点40分 2019.11.03
             */

            //TODO 8.在此编写合并代码
            //0.定义一个集合类包含Rect和Point
            //1.求出所有矩形的中心点pn,放在集合set0之中
            //2.将set0里的数据从小到大开始排序
            //3.定义一个误差变量offset,其大小控制在(+-5)之内
            //4.遍历set0变量,将符合相差offset之内的矩形合并成一个大的矩阵并且剔除原来合并的两个矩形,重新放在set1聚合之中

            //搞他！
            //0.定义一个集合类包含Rect和Point
            class RP{
                public Rect rect;
                public Point centerPoint;

                @Override
                public String toString() {
                    return "RP{" +
                            "rect=" + rect +
                            ", centerPoint=" + centerPoint +
                            '}';
                }
            }
            //1.求出所有矩形的中心点pn,放在集合set0之中
            List<RP> rps = new ArrayList<>();

            Iterator<Rect> rectIterator = rectSet.iterator();
            while (rectIterator.hasNext()){
                Rect next = rectIterator.next();

                Point point = new Point();
                point.x = (next.x + next.width) / 2.0;
                point.y = (next.y + next.height) / 2.0;

                RP rp = new RP();
                rp.centerPoint = point;
                rp.rect = next;

                rps.add(rp);

            }

            //2.将set0里的数据centerPoint.y从小到大开始排序

            Collections.sort(rps, new Comparator<RP>() {
                @Override
                public int compare(RP o1, RP o2) {
                    return (int) (o1.centerPoint.y - o2.centerPoint.y);
                }
            });

            for (RP rp : rps) {
                System.out.println(rp.toString());
            }

            //3.定义一个误差变量offset,其大小控制在(+-5)之内
            int offset = 25;
            //4.遍历set0变量,将符合相差offset之内的矩形合并成一个大的矩阵并且剔除原来合并的两个矩形,重新放在set1聚合之中
            List<RP> combinedRP = new ArrayList<>();

            for (int i = 0; i < rps.size(); i++) {
                for (int j = i + 1; j < rps.size(); j++) {
                    RP current = rps.get(i);
                    RP next = rps.get(j);


                    //符合在误差之内
                    if (next.centerPoint.y - current.centerPoint.y <= offset){
                        //将current.rect和next.rect矩阵合成一个大矩形
                        Rect currentRect = current.rect;
                        Rect nextRect = next.rect;

                        Rect newRect = new Rect();
                        //x
                        if (currentRect.x < nextRect.x)
                            newRect.x = currentRect.x;
                        else
                            newRect.x = nextRect.x;

                        //y
                        if (currentRect.y < nextRect.y)
                            newRect.y = currentRect.y;
                        else
                            newRect.y = nextRect.y;

                        //width
                        if(currentRect.width + currentRect.x > nextRect.width + nextRect.x)
                            newRect.width = currentRect.width + currentRect.x - newRect.x;
                        else
                            newRect.width = nextRect.width + nextRect.x - newRect.x;

                        //height
                        if(currentRect.height + currentRect.y> nextRect.height + nextRect.y)
                            newRect.height = currentRect.height + + currentRect.y - newRect.y;
                        else
                            newRect.height = nextRect.height + nextRect.y - newRect.y;

                        rps.get(i).rect = newRect;

                        continue;
                    }else{
                        combinedRP.add(rps.get(i));
                        i = j;
                    }
                }
            }

            //TODO 9.从集合中筛选出银行卡号码矩形,转换,完成。

            //1.筛选是长比宽很大的长方形的Rect
            List<Rect> rectFilerNumbers = new ArrayList<>();
            for (RP rp : combinedRP) {
                int scale = rp.rect.width / rp.rect.height;

                if (scale > 6){
                    rectFilerNumbers.add(rp.rect);
                }
            }

            Mat mat1 = CVGrayTransfer.grayTransferBeforeScale(fileName);

            for (int i = 0; i < rectFilerNumbers.size(); i++) {
                Imgproc.rectangle(mat1,rectFilerNumbers.get(i),new Scalar(255,255,255));
//                Imgproc.putText(mat1,combinedRP.get(i).centerPoint.y + "",combinedRP.get(i).centerPoint,Imgproc.FONT_HERSHEY_SIMPLEX,0.5,new Scalar(255,255,255));
            }
            HighGui.imshow("",mat1);
            HighGui.waitKey(0);


            //2.如何rectFilerNumbers的集合大小为1，则是银行卡数字号码
            if (rectFilerNumbers.size() == 1)
                return rectFilerNumbers.get(0);

            //3.筛选rectFilerNumbers的矩形,得出最终的银行数字串矩形


            return null;
        }

        /**
         * Designed By WuShaoCong
         * 预处理Rect
         * @param point
         * @return
         */
        private Rect boundingRect(MatOfPoint point){
            //1.先将Rect分割成n个小矩阵
            Rect maxRect = Imgproc.boundingRect(point);
            int n = maxRect.width / blockRect;

            List<Point> pointList = point.toList();

            //平均Y值
            double avgY = 0;
            //平均高度
            double avgHeight = 0;
            //Y值集合
            List<Double> arrY = new ArrayList<>();
            //高度集合
            List<Double> arrHeight = new ArrayList<>();

            //按照X排序
            Collections.sort(pointList, new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    return (int) (o1.x - o2.x);
                }
            });

            int index = 0;
//            Mat mat = CVGrayTransfer.grayTransferBeforeScale("f:\\11.jpg");
            for (int i = 1; i <= n ; i++) {
                int x = maxRect.x + i * blockRect;
                MatOfPoint matOfPoint = new MatOfPoint();
                List<Point> points = new ArrayList<Point>();


                for (int j = index;(j + 1) <= pointList.size();j++){
                    if (pointList.get(j).x > x){
                        index = j + 1;
                        break;
                    }

                    points.add(pointList.get(j));
                }

                if (points.size() != 0){
                    matOfPoint.fromList(points);
                    //求出最接近的矩阵
                    Rect rect = Imgproc.boundingRect(matOfPoint);
                    //System.out.println(rect);
//                    //Rect rect1 = Imgproc.boundingRect(contours);
//                    Mat mat = new Mat(mat0, rect);
//
//                    Imgproc.circle(mat,new Point(rect.x + rect.width / 2,rect.y + rect.height / 2),5,new Scalar(255,0,0));
//
                    arrY.add(rect.y * 1.0);
                    arrHeight.add(rect.height * 1.0);

//                    Imgproc.rectangle(mat,rect,new Scalar(255,255,255));
//                    System.out.println(rect);
                }


            }

//            Collections.sort(arrY);
//            Collections.sort(arrHeight);
//
//            //是否去除极值
//            boolean flag = false;

            //去除arrY和arrHeight的极值
            if (arrY.size() > 5){
                arrY.remove(Collections.max(arrY));
                arrY.remove(Collections.min(arrY));

                arrHeight.remove(Collections.max(arrHeight));
                arrHeight.remove(Collections.min(arrHeight));

                //求arrY和arrHeight的平均值
                double sumY = 0,sumHeight = 0;
                for (Double y : arrY) {
                    sumY += y;
                }

                for (Double height : arrHeight) {
                    sumHeight += height;
                }

                avgY = sumY / (arrY.size());
                avgHeight = sumHeight / (arrHeight.size());


//                System.out.println("原来: " + maxRect);
                maxRect.y = (int) (avgY);
                maxRect.height = (int) (avgHeight);

//                System.out.println("后来: " + maxRect);
            }



//            HighGui.imshow("",mat);
//            HighGui.waitKey(0);
            return maxRect;
        }

    }


    public void setSingleDigits() throws Exception {

        // 垂直坐标投影，获得每一个 x 坐标映射数量
        int []x = calcHistOfXY(binDigitRegion, true);
        // 添加每一个 x 坐标作链表节点
        int cur = 0;
        List<Integer> cutting = new LinkedList<>();

        if (x[cur] > 0)
            cutting.add(cur);

        while (true) {
            //寻找临界线
            int next = findNext(x, cur);
            if (next >= x.length)
                break;

            cutting.add(next);
            System.out.println(next);
            cur = next;
        }
        // 最小方差法获得窗口宽度
        int ref = getDigitWidth(cutting);
        System.out.println(ref);
        if (ref < 0)
            return;

        //System.out.println("size " + cutting.size());

        SplitList splitList = new SplitList(cutting, ref);
        /**
         * 先分割粘连字符，后合并断裂字符
         */
        split(splitList);
        final int upperWidth = (int)(1.2f * ref);
        final int lowerWidth = (int)(0.6f * ref);
        // remove Node that is a complete digit before merging
        SplitList output = splitList.out(upperWidth, lowerWidth);
        // crack into several fragment to merge into a complete digit
        List<SplitList> buckets = splitList.crack(upperWidth);
        for (SplitList elem : buckets) {
            // 整合单一字符
            merge(elem);
            output.addAll(elem.toNodeList());
        }
        // sort Nodes by its id, ensure the origin order of card numbers
        output.sort();

        System.out.println("output:" + output.size());
        /**
         定位到每个数字所在位置后，将它们拷贝到新 Mat 数组中存放
         */
        paintDigits(output.toSimpleList());
    }

    /**
     *
     * @param m      图的矩阵
     * @param axisX  true则返回x[]每一列白色像素总和 否之则返回y[]每一列白色像素总和
     * @return       每一行或每一列白色像素的总和
     */
    public int[] calcHistOfXY(Mat m, boolean axisX) {
        int []calc;
        int rows = m.rows();
        int cols = m.cols();
        byte buff[] = new byte[rows * cols];
        m.get(0, 0, buff);
        if (axisX) {
            calc = new int[cols];
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++)
                    //遇到白色像素加一
                    calc[i] += (buff[i + j * cols] & 0x1);
            }
        } else {
            calc = new int[rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++)
                    calc[i] += (buff[i * cols + j] & 0x1);
            }
        }
        return calc;
    }


    // 分割粘连的字符
    public void split(SplitList splitList) {
        int rows = binDigitRegion.rows();
        int cols = binDigitRegion.cols();
        byte buff[] = new byte[cols * rows];
        binDigitRegion.get(0, 0, buff);
        int upperWidth = (int)(1.38f * splitList.getStandardWidth());
        int lowerWidth = (int)(0.8f * splitList.getStandardWidth());
        int window = upperWidth - lowerWidth;
        for (int i = 0; i < splitList.size(); i++) {
            SplitList.Node node = splitList.get(i);
            if (node.width() > upperWidth) {
                int x = node.getStartPointX() + lowerWidth;
                int spx = splitX(buff, x, x + window);
                if (spx > 0) {
                    splitList.split(i, spx);
                }
            }
        }
    }

    public void merge(SplitList splitList) throws Exception {
        int min = Integer.MAX_VALUE;
        String solution = "";
        System.err.println("merge size: " + splitList.size());
//        if (splitList.size() > 10) {
//            throw new Exception("CVRegion error: splitList.size() is too large and over time limit to merge in function merge(SplitList spl).");
//        }
        List<String> box = new ArrayList<>();
        permutations(splitList.size(), 0, "", box);
        for (int i = 0; i < box.size(); i++) {
            String s = box.get(i);
            int splIndex = 0;
            int score = 0;
            for (int j = 0; j < s.length(); j++) {
                int val = s.charAt(j) - '0';
                int distance = splitList.dist(splIndex, splIndex +val -1);
                splIndex += val;
                score += Math.abs(distance -splitList.getStandardWidth());
            }
            if (score < min) {
                min = score;
                solution = s;
            }
        }
        for (int c = 0, spl = 0; c < solution.length(); c++) {
            int val = solution.charAt(c) - '0';
            splitList.join(spl, spl + val - 1);
            spl++;
        }
    }

    private int splitX(byte []buff, int si, int ei) {
        int max = 0;
        int index = 0;
        int rows = binDigitRegion.rows();
        int cols = binDigitRegion.cols();
        for (int x = si; x <= ei; x++) {
            int len = 0;
            for (int y = 0; y < rows; y++)
                if (buff[y * cols + x] == 0)
                    len++;
            if (max < len) {
                max = len;
                index = x;
            }
        }
        return index;
    }

    private void permutations(int total, int n, String solution, List<String> box) {
        if (total < n)
            return;
        if (total == n) {
            box.add(solution.substring(1) + n);
            return;
        }
        solution += n;
        permutations(total - n, 3, solution, box);
        permutations(total - n, 2, solution, box);
        permutations(total - n, 1, solution, box);
    }

    protected int extendHeight(byte[] buff, int cols, int i, int i1) {
        return -1;
    }

    protected int[] extendY(byte[] buff, int cols, int i, int i1) {
        return null;
    }

    protected void swap(int[] ints, int i, int k) {
        int temp = ints[i];
        ints[i] = ints[k];
        ints[k] = temp;
    }
}
