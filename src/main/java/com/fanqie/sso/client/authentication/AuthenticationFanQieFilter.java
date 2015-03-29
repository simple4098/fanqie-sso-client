
package com.fanqie.sso.client.authentication;

import org.apache.log4j.Logger;
import org.jasig.cas.client.authentication.*;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


public class AuthenticationFanQieFilter {
    private  static  final Logger logger =   Logger.getLogger(AuthenticationFanQieFilter.class);
    public static final String CONST_CAS_ASSERTION = "_const_cas_assertion_";
    /**
     * The URL to the CAS Server login.
     */
    private String casServerLoginUrl;
    /**
     * Whether to send the renew request or not.
     */
    private boolean renew = false;
    /**
     * Whether to send the gateway request or not.
     */
    private boolean gateway = false;
    /** Defines the parameter to look for for the artifact. */
    private String artifactParameterName = "ticket";
    /** Defines the parameter to look for for the service. */
    private String serviceParameterName = "service";
    /** Sets where response.encodeUrl should be called on service urls when constructed. */
    private boolean encodeServiceUrl = true;
    private String serverName;
    /** The exact url of the service. */
    private String service;
    private GatewayResolver gatewayStorage = new DefaultGatewayResolverImpl();

    private AuthenticationRedirectStrategy authenticationRedirectStrategy = new DefaultAuthenticationRedirectStrategy();
    
    private UrlPatternMatcherStrategy ignoreUrlPatternMatcherStrategyClass = null;

    public void init() {
        CommonUtils.assertNotNull(this.casServerLoginUrl, "casServerLoginUrl cannot be null.");
    }

    public  void doFilter( ServletRequest servletRequest,  ServletResponse servletResponse,
             FilterChain filterChain) throws IOException, ServletException {
        init();
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (isRequestUrlExcluded(request)) {
            logger.debug("Request is ignored.");
            filterChain.doFilter(request, response);
            return;
        }
        final HttpSession session = request.getSession(false);
        final Assertion assertion = session != null ? (Assertion) session.getAttribute(CONST_CAS_ASSERTION) : null;
        if (assertion != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String serviceUrl = constructServiceUrl(request, response);
        final String ticket = retrieveTicketFromRequest(request);
        final boolean wasGatewayed = this.gateway && this.gatewayStorage.hasGatewayedAlready(request, serviceUrl);
        if (CommonUtils.isNotBlank(ticket) || wasGatewayed) {
            filterChain.doFilter(request, response);
            return;
        }
        final String modifiedServiceUrl;
        logger.debug("no ticket and no assertion found");
        if (this.gateway) {
            logger.debug("setting gateway attribute in session");
            modifiedServiceUrl = this.gatewayStorage.storeGatewayInformation(request, serviceUrl);
        } else {
            modifiedServiceUrl = serviceUrl;
        }
        final String urlToRedirectTo = CommonUtils.constructRedirectUrl(this.casServerLoginUrl,
                this.serviceParameterName, modifiedServiceUrl, this.renew, this.gateway);

        response.sendRedirect(urlToRedirectTo);
    }
    public String urlToRedirectTo(String serviceUrl){
        return  CommonUtils.constructRedirectUrl(this.casServerLoginUrl,
                this.serviceParameterName, serviceUrl, this.renew, this.gateway);

    }

    public   String constructServiceUrl( HttpServletRequest request,  HttpServletResponse response) {
        return CommonUtils.constructServiceUrl(request, response, this.service, this.serverName,
                this.artifactParameterName, this.encodeServiceUrl);
    }

    protected String retrieveTicketFromRequest(final HttpServletRequest request) {
        return CommonUtils.safeGetParameter(request, this.artifactParameterName);
    }
    public final void setRenew(final boolean renew) {
        this.renew = renew;
    }

    public final void setGateway(final boolean gateway) {
        this.gateway = gateway;
    }

    public final void setCasServerLoginUrl(final String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }

    public final void setGatewayStorage(final GatewayResolver gatewayStorage) {
        this.gatewayStorage = gatewayStorage;
    }
        
    private boolean isRequestUrlExcluded(final HttpServletRequest request) {
        if (this.ignoreUrlPatternMatcherStrategyClass == null) {
            return false;
        }
        
        final StringBuffer urlBuffer = request.getRequestURL();
        if (request.getQueryString() != null) {
            urlBuffer.append("?").append(request.getQueryString());
        }
        final String requestUri = urlBuffer.toString();
        return this.ignoreUrlPatternMatcherStrategyClass.matches(requestUri);
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setService(String service) {
        this.service = service;
    }
}
