package com.complete.recognition.cv;

import com.complete.recognition.cv.interfaces.DigitSeparator;
import com.complete.recognition.cv.interfaces.RectFilter;
import com.complete.recognition.cv.interfaces.RectSeparator;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 4:44 2019/10/22 0022
 * @Modified By:
 */
public abstract class ImgSeparator implements RectSeparator, DigitSeparator {

    public Mat grayMat;

    // 用于存放单个字符的列表(第 2 章会用到)
    protected List<Mat> matListOfDigit;

    // 得到的卡号矩形区域
    protected Rect rectOfDigitRow;

    public ImgSeparator(Mat graySrc) {
        this.grayMat = graySrc;
        matListOfDigit = new ArrayList<Mat>();
        rectOfDigitRow = null;
    }


    public Rect getRectOfDigitRow(){
        return rectOfDigitRow;
    }

    public List<Rect> rectSeparate(Mat src, Rect region) throws Exception {

        if (src.channels() != 1)
            throw new Exception("error: image.channels() != 1 in function 'rectSeparate(Mat m,Rect r)'");

        // fist step, remove abnormal height area, fill with 0
        int cols = src.cols();
        int rows = src.rows();
        byte buff[] = new byte[cols * rows];
        src.get(0, 0, buff);
        List<Rect> stack = new LinkedList<Rect>();
        List<Rect> separates = new ArrayList<Rect>();
        stack.add(region);

        while (!stack.isEmpty()) {
            Rect ret = new Rect();
            Rect head;
            Rect scan = findEdge(buff, cols, rows,
                    head = stack.remove(0), ret);
            if (ret.x > 0 && ret.y > 0) {
                separates.add(ret);
            }
            // separate region
            int upper = scan.y - head.y;
            int lower = head.y + head.height - scan.y - scan.height;
            if (upper > 0) {
                stack.add(new Rect(head.x, head.y, head.width, upper));
            }
            if (lower > 0) {
                stack.add(new Rect(head.x, scan.y + scan.height,
                        head.width,  lower));
            }

        }
        return separates;
    }

    /**
     * return rect scanned bounding, remove it for avoiding scanning overtimes
     * <p>if finding failed, out.x = out.y = -1</p>
     * @param buff
     * @param cols
     * @param rows
     * @param region
     * @param out
     * @return 扫描出的近似长矩形区域
     */
    private Rect findEdge(byte buff[], int cols, int rows, Rect region, Rect out) {
        // thresh of `thin`
        final int thinH = (int)(RectFilter.MIN_HEIGHT_RATE * rows);
        out.x = out.y = -1;
        if (region.height < thinH) {
            return region.clone();
        }

        int w = region.x + region.width;
        int h = region.y + region.height;
        int pivot[] = new int[3]; // the longest continuous line
        int len = 0; // length of the line
        //找到最长白线作基线
        for (int i = region.y; i < h; i++) {
            int tLen = 0;
            int start = 0;
            int gap = 0;
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                if (buff[index] != 0) {
                    if (tLen++ == 0)
                        start = j;
                    if (tLen > len) {
                        len = tLen;
                        pivot[0] = start; // start x-pos
                        pivot[1] = i;
                        pivot[2] = j; // end x-pos
                    }
                    gap = 0;
                } else if (++gap > RectFilter.MIN_WIDTH_RATE * cols) {
                    tLen = 0;
                }
            }
        }

        int line = pivot[2] - pivot[0];
        if (len < cols * (RectFilter.MIN_WIDTH_RATE * 3)) { // too short
            return region.clone();
        }

        int upperY, lowerY, cnt;
        upperY = lowerY = cnt = 0;
        int []ha = new int[line];
        for (int i = 0; i < line; i++) {
            ha[i] = extendHeight(buff, cols,i + pivot[0], pivot[1]);
        }

        final int normalH = (int)(RectFilter.MAX_HEIGHT_RATE * rows);
        // when continuous thin area is too long, assert fail
        final int thinW = (int)(RectFilter.MIN_WIDTH_RATE * len);
        final int normalW = (int)(0.1 * len);
        int cw = 0; // continuous width that fitted normal height
        int ctl = 0; // continuous thin len
        int y2[][] = new int[2][line];
        byte next = -1;
        // 扩展 Y 方向获得高度
        for (int c = 0; c < line; c++) {
            int []ey2 = extendY(buff, cols, c + pivot[0], pivot[1]);
            if (ha[c] < normalH) {
                if (ha[c] < thinH) {
                    ++ctl;
                    if (ctl > thinW) {
                        next = 0; // cannot be changed
                    }
                } else {
                    ctl = 0;

                    cw ++;
                    upperY += ey2[0];
                    lowerY += ey2[1];
                    cnt++;
                    if (cw > normalW && next != 0) {
                        next = 1;
                    }

                }
            } else {
                cw = 0;
            }
            y2[0][c] = ey2[0];
            y2[1][c] = ey2[1];
        }

        // find median
        Arrays.sort(y2[0]);
        Arrays.sort(y2[1]);
        int my1, my2, b = y2[0].length >> 1;
        my1 = y2[0][b];
        my2 = y2[1][b];
        if ((y2[0].length & 0x1) == 0) {
            my1 = (y2[0][b] + y2[0][b - 1]) >> 1;
            my2 = (y2[1][b] + y2[1][b - 1]) >> 1;
        }
        Rect scanRect = new Rect(region.x, my1, region.width, my2-my1 +1);
        if (next < 1) {
            return scanRect;
        }

        upperY /= cnt;
        lowerY /= cnt;
        //Debug.log("upper: " + upperY + ", lower: " + lowerY);
        out.x = pivot[0];
        out.y = upperY;
        out.width = line;
        out.height = lowerY - upperY + 1;
        return scanRect;
    }

    protected abstract int extendHeight(byte[] buff, int cols, int i, int i1);

    protected abstract int[] extendY(byte[] buff, int cols, int i, int i1);

    abstract protected Rect cutEdgeOfY(Mat binSingleDigit);
    abstract protected void cutEdgeOfX(Rect rect);

    public void setRectOfDigitRow(Rect rectOfDigitRow) {
        this.rectOfDigitRow = rectOfDigitRow;
    }

    protected int findNext(int v[], int index) {
        int i;
        boolean get = false;

        //判断刚开始的index是否为临界点
        if (index < v.length && v[index] != 0)
            get = true;

        for (i = index; i < v.length; i++) {
            //找到一整条垂直的黑线则为分割线
            if (get && v[i] == 0)
                break;


            if (!get && v[i] != 0)
                break;
        }
        return i;
    }

    /**
     * descending sort
     * @param a array with index and value
     */
    private void sortMap(int [][]a) {
        for (int i = 0; i < a[1].length - 1; i++) {
            int k = i;
            for (int j = i + 1; j < a[1].length; j++) {
                if (a[1][k] < a[1][j]) {
                    k = j;
                }
            }
            if (k != i) {
                swap(a[0], i, k);
                swap(a[1], i, k);
            }
        }
    }

    protected abstract void swap(int[] ints, int i, int k);


    /**
     * get the average width of id region digits
     * @param cutting
     * @return
     */
    protected int getDigitWidth(List<Integer> cutting) throws Exception {
        if ((cutting.size() & 0x1) == 1) {
            System.err.println("ImgSeparator error: cutting.size() cannot be odd number in function getDigitWidth(List<Integer> c");

            cutting.remove(cutting.size() - 1);
        }

        final int window = 5;
        int [][]width = new int[2][cutting.size() >> 1];
        if (width[0].length <= window) {
            return -1;
        }
        for (int i = 1, j = 0; i < cutting.size(); i+= 2, j++) {
            width[1][j] = cutting.get(i) - cutting.get(i - 1);
            width[0][j] = j;
        }

        sortMap(width);
        int ms = -1;
        float m = Float.MAX_VALUE;
        int sum = 0;
        for (int i = 0; i < window; i++)
            sum += width[1][i];
        for (int i = window; i < width[0].length; i++) {
            float diff = 0;
            if (i > window)
                sum += (- width[1][i - window - 1] + width[1][i]);
            float avg = sum / window;
            for (int j = 0; j < window; j++) {
                diff += Math.pow(width[1][i - j] - avg, 2);
            }
            // get the min square difference
            if (diff < m) {
                ms = i - window;
                m = diff;
            }
        }

        int corrWidth = 0;
        for (int i = window; i > 0; i--)
            corrWidth += width[1][ms + i - 1];
        return corrWidth / window;
    }

    protected void paintDigits(List<Integer> cuttingList) {
        for (int i = 1; i < cuttingList.size(); i++) {
            if ((i & 0x1) == 0)
                continue;
            int x1 = cuttingList.get(i - 1);
            int x2 = cuttingList.get(i);
            Mat crop = new Mat(grayMat, new Rect(x1 + rectOfDigitRow.x,
                    rectOfDigitRow.y, x2 - x1, rectOfDigitRow.height));

            byte buff[] = new byte[crop.rows() * crop.cols()];
            crop.get(0, 0, buff);
            Mat dst = Mat.zeros(new Size(rectOfDigitRow.width,
                    rectOfDigitRow.height), grayMat.type());

            byte out[] = new byte[dst.cols() * dst.rows()];
            for (int j = 0; j < crop.rows(); j++)
                System.arraycopy(buff, j * crop.cols(), out, j *dst.cols(),
                        crop.cols());

            dst.put(0, 0, out);
            dst = CVGrayTransfer.resizeMat(dst, 380);

            HighGui.imshow("",dst);
            HighGui.waitKey(0);

            matListOfDigit.add(dst);
        }
    }

    //补充代码

//    private int[] extendY(byte[] buff, int cols, int i, int i1) {
//
//    }
//
//    private int extendHeight(byte[] buff, int cols, int i, int i1) {
//
//    }
//
//    private void swap(int[] ints, int i, int k) {
//
//    }

    //补充结束

}
