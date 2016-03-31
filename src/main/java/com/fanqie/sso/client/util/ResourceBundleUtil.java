package com.fanqie.sso.client.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * <h3>Class name</h3>
 * <h4>Description</h4>
 * <p/>
 * <h4>Special Notes</h4>
 *
 * @author mowei
 */
public class ResourceBundleUtil {
    protected static ResourceBundle bundle = ResourceBundle.getBundle("config");

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

    public static int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public static String getString(String key, String resource) {
        bundle = ResourceBundle.getBundle(resource);
        try {
            return bundle.getString(key);
        } catch (Exception e) {
        }
        return key;
    }

    public static int getInt(String key, String resource) {
        return Integer.parseInt(getString(key, resource));
    }

    public static boolean getBoolean(String key, String resource) {
        return Boolean.parseBoolean(getString(key, resource));
    }

    @SuppressWarnings("static-access")
    public static String getString(String key, Object... params) {
        try {
            String value = bundle.getString(key);
            MessageFormat form = new MessageFormat(value);
            return form.format(value, params);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

    }

    /**
     * @param key      需要获得的key值
     * @param resource 资源文件名称
     * @return 返回value值
     */
    @SuppressWarnings("static-access")
    public static String getString(String key, String resource, Object... params) {
        ResourceBundle bundles = ResourceBundle.getBundle(resource);
        try {
            String value = bundles.getString(key);
            MessageFormat form = new MessageFormat(value);
            return form.format(value, params);
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }


}
