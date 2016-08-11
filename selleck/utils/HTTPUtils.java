package selleck.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * http 工具类
 * 
 * @author fscai
 * @see http://www.baeldung.com/httpclient-4-cookies
 */

public class HTTPUtils {
	public static final String GET = "GET";
	public static final String POST = "POST";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko";
	// private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1;
	// WOW64; rv:38.0) Gecko/20100101 Firefox/38.0";
	// private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1;
	// WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135
	// Safari/537.36";
	private static final String ACCEPT_LANGUAGE = "en-us,zh-cn;q=0.8,en;q=0.5,zh;q=0.3";
	// private static final String ACCEPT_ENCODING = "gzip, deflate";
	private static final String ACCEPT_ENCODING = "*";
	private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	public static String REFERER = null; // 有时候服务器对feferer有要求，要先设置一下，用后记得再重置成null。

	/**
	 * 访问一个网址，返回最终url cookie html 信息，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie
	 *            , 如果没有则填null
	 * @param cookieDomain
	 *            如果cookie没有则填null
	 * @param method
	 *            get or post
	 * @param params
	 *            body参数，如果是GET,则为null
	 * @return Map<String,String> 有三个key，返回url , cookie , html
	 */

	public static Map<String, String> getCookieUrlAndHtml(String targetURL, String cookie, String cookieDomain,
			String method, Map<String, String> params) {
		Map<String, String> rs = new HashMap<String, String>();
		CookieStore cookieStore = new BasicCookieStore();
		if (cookie != null) {
			String[] cookies = cookie.split(";");
			for (String c : cookies) {
				String key = c.substring(0, c.indexOf("="));
				String value = c.substring(c.indexOf("=") + 1);
				BasicClientCookie clientCookie = new BasicClientCookie(key, value);
				clientCookie.setDomain(cookieDomain);
				clientCookie.setPath("/");
				cookieStore.addCookie(clientCookie);
			}
		}
		HttpClientContext httpClientContext = sendRequest(targetURL, cookieStore, method, params);
		if (httpClientContext == null) { // 请求失败
			return rs;
		}
		HttpEntity entity = httpClientContext.getResponse().getEntity();
		List<Cookie> newCookiesList = httpClientContext.getCookieStore().getCookies();
		String newCookies = convertCookieList2Str(newCookiesList);
		newCookies = updateCookieStr(cookie, newCookies);
		try {
			if (entity != null && entity.getContentType().toString().contains("html")) {
				/*
				 * byte[] contentArray = EntityUtils.toByteArray(entity); if
				 * (contentArray.length > 0) { String contentType =
				 * entity.getContentType().toString(); int charsetIndex =
				 * contentType.indexOf("charset="); String encoding =
				 * charsetIndex > -1 ? contentType.substring(charsetIndex + 8) :
				 * null; // System.out.println("encoding: "+encoding); String
				 * htmlStr = new String(contentArray,encoding == null ? "UTF-8"
				 * : encoding); rs.put("html", htmlStr); }
				 */

				String contentType = entity.getContentType().toString();
				int charsetIndex = contentType.indexOf("charset=");
				String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex + 8) : "UTF-8";
				String htmlStr = EntityUtils.toString(entity, encoding);
				rs.put("html", htmlStr);
			} else {
				rs.put("html", "");
			}

			rs.put("cookie", newCookies);

			// 个人感觉HttpGet内部会自动处理302跳转，然后把经历的跳转都放到httpClientContext.getRedirectLocations()中，集合中最后一个就是最新的请求
			// System.out.println("httpClientContext.getRedirectLocations():
			// "+httpClientContext.getRedirectLocations());
			if (httpClientContext.getRedirectLocations() == null) {
				rs.put("url", targetURL);
			} else {
				int lastLocationIndex = httpClientContext.getRedirectLocations().size() - 1;
				rs.put("url", httpClientContext.getRedirectLocations().get(lastLocationIndex).toURL().toString());
			}

			return rs;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return rs;
	}


	/**
	 * 访问一个网址，下载文件，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie
	 *            , 如果没有则填null
	 * @param method
	 *            get or post
	 * @param params
	 *            body参数，如果是GET,则为null
	 * @param file
	 *            , 要下载的目标文件，主要用来传入file的文件名路径。
	 */
	public static void downloadFile(String targetURL, String cookie, String cookieDomain, String method,
			Map<String, String> params, File file) {
		CookieStore cookieStore = new BasicCookieStore();
		if (cookie != null) {
			String[] cookies = cookie.split(";");
			for (String c : cookies) {
				String key = c.substring(0, c.indexOf("="));
				String value = c.substring(c.indexOf("=") + 1);
				BasicClientCookie clientCookie = new BasicClientCookie(key, value);
				clientCookie.setDomain(cookieDomain);
				clientCookie.setPath("/");
				cookieStore.addCookie(clientCookie);
			}
		}
		HttpClientContext httpClientContext = sendRequest(targetURL, cookieStore, method, params);
		if (httpClientContext != null) {
			HttpEntity httpEntity = httpClientContext.getResponse().getEntity();
			if (httpEntity.getContentType().toString().contains("html")) { // 如果是html就不下载了
				return;
			}
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
				InputStream input = httpEntity.getContent();
				byte b[] = new byte[1024];
				int j = 0;
				while ((j = input.read(b)) != -1) {
					output.write(b, 0, j);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					EntityUtils.consume(httpEntity);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 访问一个网址，返回最终HttpClientContext信息，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookieStore
	 *            , 如果没有则填null
	 * @param method
	 *            get or post
	 * @param params
	 *            body参数，如果是GET,则为null
	 * @return HttpClientContext，如果访问失败，return null
	 */
	private static HttpClientContext sendRequest(String targetURL, CookieStore cookieStore, String method,
			Map<String, String> params) {
		 System.out.println(new SimpleDateFormat("HH:mm:ssyyyy-MM-dd").format(new Date()) + " sendRequest");
		 System.out.println(" url: " + targetURL);

		// if(cookieStore != null && cookieStore.getCookies() != null){
		// Iterator<Cookie> iter = cookieStore.getCookies().iterator();
		// for(;iter.hasNext();){
		// Cookie c = iter.next();
		// System.out.println(" cookie : "+c.getName() +"="+c.getValue() +"
		// "+c.getDomain()+" "+c.getPath());
		// }
		// }
		// CookieStore newCookieStore = new BasicCookieStore();
		// if(cookieStore != null){
		// Iterator<Cookie> iter2 = cookieStore.getCookies().iterator();
		// for(;iter2.hasNext();){
		// Cookie c = iter2.next();
		// BasicClientCookie clientCookie = new BasicClientCookie(c.getName(),
		// c.getValue());
		// clientCookie.setDomain(".ashland.edu");
		// clientCookie.setPath("/");
		// newCookieStore.addCookie(clientCookie);
		// }
		// }

		CloseableHttpClient httpclient = null;
		HttpClientContext httpClientContext = HttpClientContext.create();
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(300000)
				.setConnectTimeout(300000).setSocketTimeout(300000).setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
				.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

		SSLContextBuilder builder = new SSLContextBuilder();
		SSLConnectionSocketFactory sslsf = null;
		try {
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			sslsf = new SSLConnectionSocketFactory(builder.build(),
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (NoSuchAlgorithmException e2) {
			e2.printStackTrace();
		} catch (KeyStoreException e2) {
			e2.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setUserAgent(USER_AGENT)
				.setDefaultCookieStore(cookieStore).setSSLSocketFactory(sslsf).build();
		httpClientContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		httpClientContext.setCookieStore(cookieStore);

		HttpRequestBase httpRequest;
		if (method.equals(GET)) {
			try {
				httpRequest = new HttpGet(targetURL);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else if (method.equals(POST)) {
			try {
				httpRequest = new HttpPost(targetURL);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			if (params != null) {
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				Set<String> keySet = params.keySet();
				for (String key : keySet) {
					nvps.add(new BasicNameValuePair(key, params.get(key)));
				}
				try {
					((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

		} else {
			return null;
		}

		// 已经
		// HttpClientBuilder.create().setDefaultCookieStore(cookieStore)了，不需要再设置cookiestore
		// if(cookieStore != null && !cookieStore.getCookies().isEmpty()){
		// CookieStore cookieStore = new BasicCookieStore();
		// BasicClientCookie clientCookie = new
		// BasicClientCookie(cookie.split("=")[0], cookie.split("=")[1]);
		// clientCookie.setDomain(".ashland.edu");
		// clientCookie.setPath("/");
		// cookieStore.addCookie(clientCookie);
		// httpClientContext.setAttribute(HttpClientContext.COOKIE_STORE,
		// cookieStore);

		// This is of course much more error-prone than working with the built
		// in cookie support
		// – for example, notice that we’re no longer setting the domain in this
		// case – which is not correct.
		// httpRequest.setHeader("Cookie", cookie);
		// }
		httpRequest.setHeader("User-Agent", USER_AGENT);
		httpRequest.setHeader("Accept-Language", ACCEPT_LANGUAGE);
		httpRequest.setHeader("Accept-Encoding", ACCEPT_ENCODING);
		httpRequest.setHeader("Accept", ACCEPT);
		if (REFERER != null) {
			httpRequest.setHeader("Referer", REFERER);
		}

		HttpResponse response = null;

		httpRequest.setConfig(requestConfig);
		try {
			response = httpclient.execute(httpRequest, httpClientContext);
			// response = httpclient.execute(httpRequest);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			// System.out.println("httpclient.execute(httpGet)
			// ClientProtocolException");
			// logger.append(" httpclient.execute(httpGet)
			// ClientProtocolException\n");
			return null;
		} catch (javax.net.ssl.SSLHandshakeException ssle) {
			ssle.printStackTrace();
			return null;
		} catch (IOException e1) {
			// System.out.println("httpclient.execute(httpGet) IOException --
			// try again");
			e1.printStackTrace();
			// 重新尝试发一次请求
			try {
				response = httpclient.execute(httpRequest, httpClientContext);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// System.out.println("httpclient.execute(httpGet) IOException
				// -- try again twice");
				e.printStackTrace();
				// 重新尝试发一次请求
				try {
					response = httpclient.execute(httpRequest, httpClientContext);
				} catch (ClientProtocolException ee) {
					ee.printStackTrace();
					return null;
				} catch (IOException ee) {
					// System.out.println("httpclient.execute(httpGet)
					// IOException again twice");
					e.printStackTrace();
					return null;
				}
				return null;
			}
		} catch (Exception ee) {
			ee.printStackTrace();
			return null;
		}

		// List<Cookie> newCookiesList;
		// newCookiesList = httpClientContext.getCookieStore() == null? null :
		// httpClientContext.getCookieStore().getCookies();
		// String newCookies = convertCookieList2Str(newCookiesList);
		// newCookies = updateCookieStr(cookie , newCookies);
		// System.out.println("new cookies: "+newCookies);
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			return httpClientContext;

		} else if (HttpStatus.SC_MOVED_TEMPORARILY == response.getStatusLine().getStatusCode() || // 302
																									// 跳转
		HttpStatus.SC_MOVED_PERMANENTLY == response.getStatusLine().getStatusCode() || // 301
																						// 跳转
		HttpStatus.SC_SEE_OTHER == response.getStatusLine().getStatusCode()) { // 303
																				// 跳转
			// 请求成功 取得请求内容
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				// System.out.println(" EntityUtils.consume(entity)
				// IOException");
			}

			Header locationHeader = response.getFirstHeader("location");
			if (locationHeader != null) {
				return sendRequest(locationHeader.getValue(), httpClientContext.getCookieStore(), HTTPUtils.GET, null);
			}
		} else {
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				// System.out.println(" EntityUtils.consume(entity)
				// IOException");
			}
			// System.out.println(" 访问失败 " +
			// response.getStatusLine().getStatusCode());
		}
		return null;
	}

	/**
	 * 把List<Cookie> 转换成String,
	 * 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * 
	 * @param cookies
	 *            List<Cookie>
	 * @return 如果cookies为null 或是空集合，返回""
	 */
	public static String convertCookieList2Str(List<Cookie> cookies) {
		if (cookies == null) {
			return "";
		}
		StringBuffer cookieStr = new StringBuffer();
		for (Cookie c : cookies) {
			cookieStr.append(c.getName()).append("=").append(c.getValue()).append(";");
		}
		return cookieStr.toString();
	}

	/**
	 * 更新cookie list。如果新的cookie list不包含老的list中的某个cookie,加入到新的list
	 * 
	 * @param oldCookies
	 * @param newCookies
	 */
	@SuppressWarnings("unused")
	private static List<Cookie> updateCookieList(List<Cookie> oldCookies, List<Cookie> newCookies) {
		if (oldCookies == null) {
			return newCookies;
		}
		if (newCookies == null) {
			return oldCookies;
		}
		for (Cookie oc : oldCookies) {
			boolean notInNew = true;
			for (Cookie nc : newCookies) {
				if (nc.getName().equals(oc.getName())) {
					notInNew = false;
					break;
				}
			}
			if (notInNew) {
				newCookies.add(oc);
			}
		}
		return newCookies;
	}

	/**
	 * 更新cookie list，即如果新的cookie list不包含老的list中的某个cookie,加入到新的list
	 * 
	 * @param oldCookies
	 *            String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=
	 *            F6B1B5161B6034B5A79904397F59CB3F;
	 * @param newCookies
	 *            String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=
	 *            F6B1B5161B6034B5A79904397F59CB3F;
	 * @return 新的cookie String
	 */
	public static String updateCookieStr(String oldCookies, String newCookies) {
		if (oldCookies == null || oldCookies.trim().isEmpty()) {
			return newCookies;
		}
		if (newCookies == null || newCookies.trim().isEmpty()) {
			return oldCookies;
		}

		String[] oldCookieArr = oldCookies.split(";");
		String[] newCookieArr = newCookies.split(";");
		for (String oc : oldCookieArr) {
			if (oc.isEmpty()) {
				continue;
			}
			boolean notInNew = true;
			String oName = oc.split("=")[0];
			for (String nc : newCookieArr) {
				if (nc.isEmpty()) {
					continue;
				}
				String nName = nc.split("=")[0];
				if (nName.equals(oName)) {
					notInNew = false;
					break;
				}
			}
			if (notInNew) {
				newCookies = newCookies + oc + ";";
			}
		}
		return newCookies;
	}

	/**
	 * 设置查询请求的post body，特别
	 * 
	 * @param sid
	 *            url里的SID
	 * @param pName
	 *            要查询的期刊名
	 * @param domain
	 *            主域名 http://lib-proxy.pnc.edu:2311
	 * @param range
	 *            时间 4week YearToDate 等
	 * @return
	 */
	public static Map<String, String> getRangeParameter(String sid, String pName, String domain, String range) {
		Map<String, String> params = getBaseHttpRequestParameter(sid, pName, domain);
		params.put("range", range);
		params.put("period", "Range Selection");
		params.put("startYear", "1992");
		params.put("endYear", "2014");
		return params;
	}

	/**
	 * 设置查询请求的post body，包含大多数通用参数。
	 * 
	 * @param sid
	 *            url里的SID
	 * @param pName
	 *            要查询的期刊名
	 * @param domain
	 *            主域名 http://lib-proxy.pnc.edu:2311
	 * @return
	 */
	private static Map<String, String> getBaseHttpRequestParameter(String sid, String pName, String domain) {
		Map<String, String> reqParam = new IdentityHashMap<String, String>();
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// String sysEndDate = sdf.format(new Date());
		// SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy");
		// String year = sdf2.format(new Date());

		reqParam.put("fieldCount", "1");
		reqParam.put("action", "search");
		reqParam.put("product", "WOS");
		reqParam.put("search_mode", "GeneralSearch");
		reqParam.put("SID", sid); // SID in url
		reqParam.put("max_field_count", "25");

		/*
		 * not urlDecoded yet reqParam.put("max_field_notice",
		 * "%E6%B3%A8%E6%84%8F%3A+%E6%97%A0%E6%B3%95%E6%B7%BB%E5%8A%A0%E5%8F%A6%E4%B8%80%E5%AD%97%E6%AE%B5%E3%80%82"
		 * ); // unknown param reqParam.put("input_invalid_notice",
		 * "%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E8%AF%B7%E8%BE%93%E5%85%A5%E6%A3%80%E7%B4%A2%E8%AF%8D%E3%80%82"
		 * ); reqParam.put("exp_notice",
		 * "%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E4%B8%93%E5%88%A9%E6%A3%80%E7%B4%A2%E8%AF%8D%E5%8F%AF%E5%9C%A8%E5%A4%9A%E4%B8%AA%E5%AE%B6%E6%97%8F%E4%B8%AD%E6%89%BE%E5%88%B0+%28"
		 * ); // unknown param reqParam.put("input_invalid_notice_limits",
		 * "+%3Cbr%2F%3E%E6%B3%A8%3A+%E6%BB%9A%E5%8A%A8%E6%A1%86%E4%B8%AD%E6%98%BE%E7%A4%BA%E7%9A%84%E5%AD%97%E6%AE%B5%E5%BF%85%E9%A1%BB%E8%87%B3%E5%B0%91%E4%B8%8E%E4%B8%80%E4%B8%AA%E5%85%B6%E4%BB%96%E6%A3%80%E7%B4%A2%E5%AD%97%E6%AE%B5%E7%9B%B8%E7%BB%84%E9%85%8D%E3%80%82"
		 * ); // unknown param reqParam.put("sa_params",
		 * "WOS%7C%7C1ByB9OWWlCEAOeYCKIn%7Chttp%3A%2F%2Flib-proxy.pnc.edu%3A2311%7C%27&"
		 * );
		 */

		// urlDecoded
		reqParam.put("max_field_notice", "注意: 无法添加另一字段。"); // unknown param
		reqParam.put("input_invalid_notice", "请输入检索词。"); // unknown param
		reqParam.put("exp_notice", "检索错误: 专利检索词可在多个家族中找到 ("); // unknown param
		reqParam.put("input_invalid_notice_limits", "滚动框中显示的字段必须至少与一个其他检索字段相组配。"); // unknown
																					// param
		reqParam.put("sa_params", "WOS||" + sid + "|" + domain + "|'");

		reqParam.put("formUpdated", "true");
		reqParam.put("value(input1)", pName);
		reqParam.put("value(select1)", "SO"); // may be publication select
		// reqParam.put("x","77");
		// reqParam.put("y","18");
		reqParam.put("value(hidInput1)", "");
		reqParam.put("limitStatus", "expanded");
		reqParam.put("ss_lemmatization", "On");
		reqParam.put("ss_spellchecking", "Suggest");
		reqParam.put("SinceLastVisit_UTC", "");
		reqParam.put("SinceLastVisit_DATE", "");
		// reqParam.put("range","ALL"); // ALL or 4week
		// ，在getRangeParameter或getYearParameter中指定
		// reqParam.put("period","Year Range"); // Year Range or Range+Selection
		// ， 在getRangeParameter或getYearParameter中指定
		// reqParam.put("startYear",startYear); // 在getYearParameter中指定
		// reqParam.put("endYear",endYear); // 在getYearParameter中指定
		reqParam.put("editions", "SCI");
		reqParam.put("ssStatus", "display:none");
		reqParam.put("ss_showsuggestions", "ON");
		reqParam.put("ss_numDefaultGeneralSearchFields", "1");
		reqParam.put("ss_query_language", "");
		reqParam.put("rs_sort_by", "PY.D;LD.D;SO.A;VL.D;PG.A;AU.A");

		return reqParam;

	}

}
