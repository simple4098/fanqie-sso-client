package com.fanqie.sso.client.filter;

import com.fanqie.sso.client.util.FanQieSsoClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DESC : 客户端-不用过滤的url
 * @author : 番茄木-ZLin
 * @data : 2015/3/19
 * @version: v1.0.0
 */
public class UserAuthFilter implements Filter {
    private  static  final Logger log =   Logger.getLogger(UserAuthFilter.class);
    //不需要过滤的url
    private String[] excludeUrls;
    //认证中心hostname
    private String ssoHostName;
    //认证中心登录uri
    private String ssoLogin;
    //退出认证
    private String ssoLogout;
    //客户端hostName
    private String projectHostName;
    //客户端首页
    private String projectIndex;
    //登录url
    private String loginUrl;
    //退出url
    private String logoutUrl;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            String excludeUrl=filterConfig.getInitParameter("exclude");
            if(StringUtils.isNotBlank(excludeUrl)){
                excludeUrls=StringUtils.split(excludeUrl,";");
            }else {
                InputStream in = this.getClass().getResourceAsStream("/service.properties");
                Properties p = new Properties();
                p.load(in);
                excludeUrl = p.getProperty("project.excludeUrls");
                if (StringUtils.isNotEmpty(excludeUrl)) {
                    excludeUrls = StringUtils.split(excludeUrl, ";");
                }
                ssoHostName = p.getProperty("sso.hostname.url");
                ssoLogin = p.getProperty("sso.login");
                projectHostName = p.getProperty("project.home.url");
                projectIndex = p.getProperty("project.index.url");
                ssoLogout = p.getProperty("sso.logout");
                loginUrl = FanQieSsoClient.loginUrl(ssoHostName, ssoLogin, projectHostName, projectIndex);
                logoutUrl = FanQieSsoClient.logout(ssoHostName, ssoLogout, projectHostName, projectIndex);
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse)response;
        String uri = httpServletRequest.getRequestURI();
        if (!ArrayUtils.contains(excludeUrls,uri)){
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }else {
            httpServletRequest.setAttribute("loginUrl",loginUrl);
            httpServletRequest.setAttribute("logoutUrl",logoutUrl);
            httpServletRequest.getRequestDispatcher(uri).forward(httpServletRequest,httpServletResponse);
            return;
        }
    }
    @Override
    public void destroy() {

    }


}
