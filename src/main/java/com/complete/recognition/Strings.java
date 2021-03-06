package com.complete.recognition;

import com.complete.recognition.utils.Resources;

/**
 * @Author: ShaocongWU
 * @Description:
 * @Date: Created in 下午 6:51 2019/10/24 0024
 * @Modified By:
 */
public class Strings {
    public static final String FILE_NAME = "11.jpg";
    //浮雕字体
    public static final String FILE_NAME_2 = "12.png";
    public static final String FILE_NAME_3 = "13.png";
    public static final String FILE_NAME_5 = "15.jpg";

    //印刷体
    public static final String FILE_NAME_4 = "14.png";


    private static String FILE_DEFAULT = FILE_NAME;


    public static String getFileDefault() {
        return FILE_DEFAULT;
    }

    public static void setFileDefault(String fileDefault) {
        FILE_DEFAULT = fileDefault;
    }

    public static String getFilePath(){
        String filePath= Resources.getResource(
                getFileDefault()
        ).getPath().substring(1);

        return filePath;
    }
}
