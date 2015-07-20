package com.fanqie.sso.client.util;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DESC : HttpClientUtil 连接Util工具类
 * @author : 番茄木-ZLin
 * @data : 2015/4/22
 * @version: v1.0.0
 */
public class HttpClientUtil {
	private final  static  int TIME_OUT = 15000;
	private final  static  int REQUEST_SOCKET_TIME = 20000;
	private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
	private HttpClientUtil(){}

	public static HttpClient obtHttpClient(String proxyIp,int proxyPort){
		HttpClientBuilder httpClientBuilder =HttpClientBuilder.create();
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(HttpClientUtil.TIME_OUT)
				.setSocketTimeout(HttpClientUtil.REQUEST_SOCKET_TIME).build();
		httpClientBuilder.setDefaultRequestConfig(requestConfig);
		//设置代理
		if(!StringUtils.isEmpty(proxyIp) && 0!=proxyPort){
			HttpHost proxy = new HttpHost(proxyIp, proxyPort);
			httpClientBuilder.setProxy(proxy);
		}
		CloseableHttpClient httpClient = httpClientBuilder.build();
		return httpClient;
	}

	public static HttpClient obtHttpClient(){
		HttpClientBuilder httpClientBuilder =HttpClientBuilder.create();
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(HttpClientUtil.TIME_OUT)
				.setSocketTimeout(HttpClientUtil.REQUEST_SOCKET_TIME).build();
		httpClientBuilder.setDefaultRequestConfig(requestConfig);
		return httpClientBuilder.build();
	}




	public static String  httpPost(String url,Map<String,String> map) throws IOException {
		HttpClient httpClient = obtHttpClient();
		HttpPost httpPost = new HttpPost(url);
		List<NameValuePair> nameValuePairs = commonParam(map);
		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String value="";
		if (entity != null) {
			InputStream stream = entity.getContent();
			value  = HttpClientUtil.convertStreamToString(stream);
			httpPost.abort();
		}
		return value;
	}

	public static List<NameValuePair> commonParam(Map<String,String> map){
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("innId", map.get("innId")));
		nameValuePairs.add(new BasicNameValuePair("userCode",map.get("userCode")));
		nameValuePairs.add(new BasicNameValuePair("appId", map.get("appId")));
		nameValuePairs.add(new BasicNameValuePair("userCode",map.get("userCode")));
		nameValuePairs.add(new BasicNameValuePair("timestamp", map.get("timestamp")));
		nameValuePairs.add(new BasicNameValuePair("token", map.get("token")));
		return  nameValuePairs;

	}
	public static String convertStreamToString(InputStream is) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is,"utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			logger.error("convertStreamToString "+e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.error("convertStreamToString exception"+e);
			}
		}
		return sb.toString();
	}


}
