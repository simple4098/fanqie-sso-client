package com.fanqie.sso.client.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fanqie.sso.client.authentication.AuthenticationFanQieFilter;
import com.fanqie.sso.client.session.SingleSignOutFanQieFilter;
import com.fanqie.sso.client.util.Constants;
import com.fanqie.sso.client.util.FanQieSsoClient;
import com.fanqie.sso.client.util.HttpClientUtil;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.AssertionHolder;
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
import java.net.InetSocketAddress;
import java.util.*;

/**
 * DESC : 客户端-不用过滤的url
 *
 * @author : 番茄木-ZLin
 * @data : 2015/3/19
 * @version: v1.0.0
 */
public class UserAuthFilter implements Filter {
    private static final Logger logger = Logger.getLogger(UserAuthFilter.class);
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
    private String loginValidate;
    private XMemcachedClientBuilder xMemcachedClientBuilder;
    private MemcachedClient memcachedClient;


    //退出过滤器
    private SingleSignOutFanQieFilter signOutFilter = null;
    //登录认证，未登录用户导向CAS Server进行认证
    private AuthenticationFanQieFilter authenticationFilter = null;
    //单点登录cas_client 提供的票据验证过滤器
    private Cas20ProxyReceivingTicketValidationFilter ticketValidationFilter = null;
    private Cas20ServiceTicketValidator cas20ServiceTicketValidator = null;
    // 存放Assertion到ThreadLocal中
   /* private AssertionThreadLocalFilter threadLocalFilter=null;*/

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        signOutFilter = new SingleSignOutFanQieFilter();
        authenticationFilter = new AuthenticationFanQieFilter();
        ticketValidationFilter = new Cas20ProxyReceivingTicketValidationFilter();
        // threadLocalFilter = new AssertionThreadLocalFilter();
        try {
            Properties p = new Properties();
            String excludeUrl = filterConfig.getInitParameter("excludes");
            String envValue = filterConfig.getInitParameter("env");
            projectHostName = filterConfig.getInitParameter("projectUrl");
            projectIndex = filterConfig.getInitParameter("projectIndex");
            InputStream in = null;
            if (StringUtils.isNotEmpty(envValue)) {
                if (envValue.equals("dev")) {
                    in = this.getClass().getResourceAsStream("/development/sso.properties");
                }
                if (envValue.equals("pro")) {
                    in = this.getClass().getResourceAsStream("/production/sso.properties");
                }
                //线上test
                if (envValue.equals("test")) {
                    in = this.getClass().getResourceAsStream("/test/sso.properties");
                }
            } else {
                in = this.getClass().getResourceAsStream("/development/sso.properties");
            }
            p.load(in);

            if (StringUtils.isEmpty(excludeUrl) || StringUtils.isEmpty(projectHostName) || StringUtils.isEmpty(projectIndex)) {
                throw new RuntimeException("UserAuthFilter 过滤器;excludes、projectUrl、projectIndex 参数不能为空");
            }
            if (StringUtils.isNotEmpty(excludeUrl)) {
                excludeUrls = StringUtils.split(excludeUrl, ";");
            }
            loginValidate = p.getProperty("pms.authenticate");
            String hostName = p.getProperty("memcached.hostName");
            String port = p.getProperty("memcached.port");
            InetSocketAddress inetSocketAddress = new InetSocketAddress(hostName, Integer.valueOf(port));
            List<InetSocketAddress> addressList = new ArrayList<InetSocketAddress>();
            addressList.add(inetSocketAddress);
            xMemcachedClientBuilder = new XMemcachedClientBuilder(addressList);
            xMemcachedClientBuilder.setConnectionPoolSize(2);
            xMemcachedClientBuilder.setCommandFactory(new BinaryCommandFactory());
            xMemcachedClientBuilder.setTranscoder(new SerializingTranscoder());
            memcachedClient = xMemcachedClientBuilder.build();

            ssoHostName = p.getProperty("sso.hostname.url");
            ssoLogin = p.getProperty("sso.login");
            ssoLogout = p.getProperty("sso.logout");
            loginUrl = FanQieSsoClient.loginUrl(ssoHostName, ssoLogin, projectHostName, projectIndex);
            logoutUrl = FanQieSsoClient.logout(ssoHostName, ssoLogout, projectHostName, projectIndex);
            authenticationFilter.setCasServerLoginUrl(ssoHostName + ssoLogin);
            authenticationFilter.setService(projectHostName + projectIndex);
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
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        /*httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");*/
        String token = request.getParameter("token");
        String appId = request.getParameter("appId");
        String innId = request.getParameter("innId");
        String timestamp = request.getParameter("timestamp");
        String userCode = request.getParameter("userCode");

        //token 是客户端登陆 不进单点登录
        if (StringUtils.isEmpty(token)) {
            HttpSession session = httpServletRequest.getSession();
            httpServletRequest.setAttribute("loginUrl", loginUrl);
            httpServletRequest.setAttribute("logoutUrl", logoutUrl);
            String uri = httpServletRequest.getRequestURI();
            boolean isStaticUrl = FanQieSsoClient.matcherStaticUrl(uri);
            //如果是静态文件路径，直接直接跳转下去
            if (isStaticUrl) {
                filterChain.doFilter(request, response);
                return;
            }
            boolean b = FanQieSsoClient.matcherUrl(excludeUrls, uri);
            if (!b) {
                signOutFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
                //Assertion assertion = session != null ? (Assertion) session.getAttribute(Constants.CONST_CAS_ASSERTION) : null;
                Assertion assertion = (Assertion) (session == null ? request
                        .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : session
                        .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION));
                if (assertion == null) {
                    //获取验证票据
                    String ticket = CommonUtils.safeGetParameter(httpServletRequest, artifactParameterName);
                    if (CommonUtils.isEmpty(ticket)) {
                        //当项目中的session失效后,就设置当前的url为登录后跳转的页面
                        authenticationFilter.setService(projectHostName + uri);
                        String serviceUrl = authenticationFilter.constructServiceUrl(httpServletRequest, httpServletResponse);
                        String urlToRedirectTo = authenticationFilter.urlToRedirectTo(serviceUrl);
                        httpServletResponse.sendRedirect(urlToRedirectTo);
                        return;
                    } else {
                        ticketValidationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
                    }
                } else {
                    AssertionHolder.setAssertion(assertion);
                    filterChain.doFilter(request, response);
                    return;
                }
            } else {
                httpServletRequest.getRequestDispatcher(uri).forward(httpServletRequest, httpServletResponse);
                return;
            }
            //pms 去验证token值， 并保持在
        } else {
            Map<String, String> map = new HashMap<String, String>();
            map.put("innId", innId);
            map.put("userCode", userCode);
            map.put("appId", appId);
            map.put("timestamp", timestamp);
            map.put("token", token);
            String s = HttpClientUtil.httpPost(loginValidate, map);
            logger.debug("===============loginValidate=============： "+ s);
            JSONObject jsonObject = JSON.parseObject(s);
            if (Constants.SUCCESS.equals(jsonObject.get("status").toString())) {
                //存入memcached token-key userCode-value
                try {
                    memcachedClient.setWithNoReply(token, 30 * 24 * 60 * 60, userCode.concat(",").concat(innId));
                    filterChain.doFilter(request, response);
                } catch (InterruptedException e) {
                    Map<String, Object> param = new HashMap<>();
                    param.put("message", "更新 Memcached 缓存被中断");
                    param.put("status", "400");
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().print(JSON.toJSONString(param));
                } catch (MemcachedException e) {
                    Map<String, Object> param = new HashMap<>();
                    param.put("message", "更新 Memcached 缓存错误");
                    param.put("status", "400");
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().print(JSON.toJSONString(param));
                }

            } else {
                Map<String, Object> param = new HashMap<>();
                param.put("message", "客户端登陆验证不通过");
                param.put("status", "400");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().print(JSON.toJSONString(param));
            }

        }
    }

    @Override
    public void destroy() {

    }


}
