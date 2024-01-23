package com.dta.r0dbg.util;

import java.io.File;
import java.net.URL;

public final class FileHelper {
    public static File getResourceFile(Class<?> cls, String sourcePath){
        URL resource = cls.getClassLoader().getResource(sourcePath);
        if (resource == null){
            return  null;
        }
        String path = resource.getPath();
        return new File(path);
    }
}
