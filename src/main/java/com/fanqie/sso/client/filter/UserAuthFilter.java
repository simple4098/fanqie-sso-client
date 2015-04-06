package com.fanqie.sso.client.filter;

import com.fanqie.sso.client.authentication.AuthenticationFanQieFilter;
import com.fanqie.sso.client.session.SingleSignOutFanQieFilter;
import com.fanqie.sso.client.util.Constants;
import com.fanqie.sso.client.util.FanQieSsoClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.AssertionHolder;
import org.jasig.cas.client.util.AssertionThreadLocalFilter;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
    private  static  final Logger logger =   Logger.getLogger(UserAuthFilter.class);
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
    private String artifactParameterName = "ticket";


    //退出过滤器
    private SingleSignOutFanQieFilter signOutFilter=null;
    //登录认证，未登录用户导向CAS Server进行认证
    private AuthenticationFanQieFilter authenticationFilter=null;
    //单点登录cas_client 提供的票据验证过滤器
    private Cas20ProxyReceivingTicketValidationFilter ticketValidationFilter = null;
    private Cas20ServiceTicketValidator cas20ServiceTicketValidator=null;
    // 存放Assertion到ThreadLocal中
    private AssertionThreadLocalFilter threadLocalFilter=null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        signOutFilter = new SingleSignOutFanQieFilter();
        authenticationFilter = new AuthenticationFanQieFilter();
        ticketValidationFilter = new Cas20ProxyReceivingTicketValidationFilter();
        threadLocalFilter = new AssertionThreadLocalFilter();

        try {
            Properties p = new Properties();
            String excludeUrl=filterConfig.getInitParameter("excludes");
            String envValue = filterConfig.getInitParameter("env");
            projectHostName = filterConfig.getInitParameter("projectUrl");
            projectIndex= filterConfig.getInitParameter("projectIndex");
            InputStream in = null;
            if (StringUtils.isNotEmpty(envValue)){
                if (envValue.equals("dev")){
                    in = this.getClass().getResourceAsStream("/development/sso.properties");
                }
                if (envValue.equals("pro")){
                    in = this.getClass().getResourceAsStream("/production/sso.properties");
                }
            }else {
                in = this.getClass().getResourceAsStream("/development/sso.properties");
            }
            if (StringUtils.isEmpty(excludeUrl)){
                throw  new RuntimeException("UserAuthFilter 过滤器;exclude 参数不能为空");
            }
            excludeUrls=StringUtils.split(excludeUrl,";");
            p.load(in);
            ssoHostName = p.getProperty("sso.hostname.url");
            ssoLogin = p.getProperty("sso.login");
            ssoLogout = p.getProperty("sso.logout");
            loginUrl = FanQieSsoClient.loginUrl(ssoHostName, ssoLogin, projectHostName, projectIndex);
            logoutUrl = FanQieSsoClient.logout(ssoHostName, ssoLogout, projectHostName, projectIndex);
            authenticationFilter.setCasServerLoginUrl(ssoHostName+ssoLogin);
            authenticationFilter.setService(projectHostName+projectIndex);
            cas20ServiceTicketValidator = new Cas20ServiceTicketValidator(ssoHostName);
            ticketValidationFilter.setTicketValidator(cas20ServiceTicketValidator);
            ticketValidationFilter.setServerName(projectHostName);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse)response;
        HttpSession session = httpServletRequest.getSession();
        httpServletRequest.setAttribute("loginUrl",loginUrl);
        httpServletRequest.setAttribute("logoutUrl",logoutUrl);
        String uri = httpServletRequest.getRequestURI();
        if (!ArrayUtils.contains(excludeUrls,uri)){
            signOutFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
            Assertion assertion = session != null ? (Assertion) session.getAttribute(Constants.CONST_CAS_ASSERTION) : null;
            ((HttpServletRequest) request).getSession().setAttribute("userInfo",assertion);
            if (assertion==null){
                //获取验证票据
                String ticket = CommonUtils.safeGetParameter(httpServletRequest, artifactParameterName);
                if (CommonUtils.isEmpty(ticket) ) {
                    //authenticationFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
                    String serviceUrl = authenticationFilter.constructServiceUrl(httpServletRequest, httpServletResponse);
                    String urlToRedirectTo = authenticationFilter.urlToRedirectTo(serviceUrl);
                    httpServletResponse.sendRedirect(urlToRedirectTo);
                    return;
                }else {
                    ticketValidationFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
                }
            }else {
                assertion = (Assertion) (session == null ? request
                        .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : session
                        .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION));
                ((HttpServletRequest) request).getSession().setAttribute("userInfo",assertion);
                AssertionHolder.setAssertion(assertion);
                filterChain.doFilter(request,response);
                return;

            }
            //threadLocalFilter.doFilter(httpServletRequest,httpServletResponse, filterChain);
        }else {
            httpServletRequest.getRequestDispatcher(uri).forward(httpServletRequest, httpServletResponse);
            return;
        }
    }
    @Override
    public void destroy() {

    }


}
