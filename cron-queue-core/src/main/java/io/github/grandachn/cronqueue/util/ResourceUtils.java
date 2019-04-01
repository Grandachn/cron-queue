package io.github.grandachn.cronqueue.util;

import io.github.grandachn.cronqueue.exception.CommonException;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @Author by guanda
 * @Date 2019/4/1 14:47
 */
public class ResourceUtils {
    private static final ResourceBundle resourceBundle;

    static{
        resourceBundle = ResourceBundle.getBundle("cronQueueConfig");
    }

    public static String getString(String key){
        try {
            return resourceBundle.getString(key);
        }catch (MissingResourceException e){
            throw CommonException.build(e, "miss config: {}", key);
        }
    }

    public static String getString(String key, String def){
        try {
            return resourceBundle.getString(key);
        }catch (MissingResourceException e){
            return def;
        }
    }

    public static int getInt(String key){
        try {
            return Integer.valueOf(resourceBundle.getString(key));
        }catch (Exception e){
            throw CommonException.build(e, "error config: {}={} , the value must be int", key, resourceBundle.getString(key));
        }
    }

    public static int getInt(String key, int def){
        try {
            return Integer.valueOf(resourceBundle.getString(key));
        }catch (Exception e){
            return def;
        }
    }

    public static long getLong(String key){
        try {
            return Long.valueOf(resourceBundle.getString(key));
        }catch (Exception e){
            throw CommonException.build(e, "error config: {}={} , the value must be long", key, resourceBundle.getString(key));
        }
    }

    public static long getLong(String key, long def){
        try {
            return Long.valueOf(resourceBundle.getString(key));
        }catch (Exception e){
            return def;
        }
    }
}
