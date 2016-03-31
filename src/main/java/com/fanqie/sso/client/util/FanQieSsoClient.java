package com.fanqie.sso.client.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;

/**
 * DESC : 处理客服端的url
 * @author : 番茄木-ZLin
 * @data : 2015/3/25
 * @version: v1.0.0
 */
public class FanQieSsoClient {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private  static  final Logger log =   Logger.getLogger(FanQieSsoClient.class);
    private FanQieSsoClient() {
    }

    /**
     * @param url 请求url
     */
    public  static  String urlEncode(String url) throws UnsupportedEncodingException {
        if (StringUtils.isNotEmpty(url)){
            return   URLEncoder.encode(url, "utf-8");
        }
        return null;
    }

    /**
     *
     * @param hostName sso 主机名
     * @param login sso 登录uri
     * @param projectHostName  客户端主机名
     * @param projectIndex   客户端首页
     */
    public static String loginUrl(String hostName,String login,String projectHostName,String projectIndex){
        try {

            isTrue(hostName,login,projectHostName,projectIndex);
            String service = urlEncode(projectHostName + projectIndex);
            StringBuilder url = new StringBuilder();
            return url.append(hostName).append(login).append("?service=").append(service).toString();
        } catch (UnsupportedEncodingException e) {
           log.error("url urlEncode 异常 "+e);
        }
        return  null;
    }
    /**
     *
     * @param hostName sso 主机名
     * @param logout sso 退出uri
     * @param projectHostName  客户端主机名
     * @param projectIndex   客户端首页
     */
    public static  String logout(String hostName,String logout,String projectHostName,String projectIndex) throws UnsupportedEncodingException{
        isTrue(hostName,logout,projectHostName,projectIndex);
        StringBuilder url = new StringBuilder();
        String returnUrl = urlEncode(projectHostName+projectIndex);
        return url.append(hostName).append(logout).append("?url=").append(returnUrl).toString();
    }

    public static void isTrue(String ... arrays){
        for (String v:arrays){
           if (StringUtils.isEmpty(v)){
               throw  new RuntimeException("参数不能为空!");
           }
        }


    }

    /**
     *
     * @param excludeUrls 客服端 不需要过滤url数组；或者正则匹配
     * @param uri 当前uri
     * @return 如果包含或者正则匹配成功的url 返回 true;反之返回false
     */
    public static boolean matcherUrl(String[] excludeUrls, String uri) {
        if(excludeUrls!=null){
            for (String uriV : excludeUrls){
                /*Pattern p = Pattern.compile(uriV);
                Matcher m = p.matcher(uri);*/
                boolean b =  antPathMatcher.match(uriV,uri);
                if (b) {
                    return true;
                }
            }
            if (ArrayUtils.contains(excludeUrls, uri)){
                return true;
            }
        }
        return false;
    }
    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    /**
     * 过滤静态文件
     * @param url
     */
    public static boolean matcherStaticUrl(String url)  {
        if (StringUtils.isNotEmpty(url)){
            String end = url.substring(url.lastIndexOf(".")+1, url.length());
            String string = ResourceBundleUtil.getString("sso.matcherStaticUrl");
            String[] split = string.split(",");
            for (String s:split){
                if (s.equalsIgnoreCase(end)){
                    return true;
                }
            }
        }
        return false;


    }

    public static void main(String[] args) throws UnsupportedEncodingException {
       /* System.out.println(urlEncode("http://web1.app.com:8081/home"));
        String ssoHostName="http://my.app.com:8080";
        String login = "/login";
        String logout="/logout";
        String projectHost="http://web1.app.com:8081";
        String index = "/home";
        System.out.println(loginUrl(ssoHostName,login,projectHost,index));
        System.out.println(logout(ssoHostName,logout,projectHost,index));*/
        String url = "http://www.dsds.com/index";
        /*String end = url.substring(url.lastIndexOf("."), url.length());
        System.out.println(end.equalsIgnoreCase(".jpeg"));*/
        String end = url.substring(url.lastIndexOf(".")+1, url.length());
        String string = ResourceBundleUtil.getString("sso.matcherStaticUrl");
        String[] split = string.split(",");
        for (String s:split){
            if (s.equalsIgnoreCase(end)){
                System.out.print(s+"=============");
            }
        }
    }


}
