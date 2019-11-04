package com.complete.recognition.utils;


import java.io.InputStream;
import java.net.URL;

public class Resources {
    public static InputStream getResourceAsStream(String fileName){
        return Resources
                .class
                .getClassLoader()
                .getResourceAsStream(fileName);
    }


    public static URL getResource(String fileName){
        return Resources
                .class
                .getClassLoader()
                .getResource(fileName);
    }
}
