package com.fanqie.sso.client.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * DESC : 处理客服端的url
 * @author : 番茄木-ZLin
 * @data : 2015/3/25
 * @version: v1.0.0
 */
public class FanQieSsoClient {
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
    public static  String logout(String hostName,String logout,String projectHostName,String projectIndex){
        isTrue(hostName,logout,projectHostName,projectIndex);
        StringBuilder url = new StringBuilder();
        return url.append(hostName).append(logout).append("?url=").append(projectHostName).append(projectIndex).toString();
    }

    public static void isTrue(String ... arrays){
        for (String v:arrays){
            Assert.hasText(v);
        }


    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(urlEncode("http://web1.app.com:8081/home"));
        String ssoHostName="http://my.app.com:8080";
        String login = "/login";
        String logout="/logout";
        String projectHost="http://web1.app.com:8081";
        String index = "/home";
        System.out.println(loginUrl(ssoHostName,login,projectHost,index));
        System.out.println(logout(ssoHostName,logout,projectHost,index));
    }
}
