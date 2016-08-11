package selleck.email.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPage;
import selleck.email.pojo.YellowPageWebsiteEmail;
import selleck.email.service.IYellowPageService;
import selleck.email.service.IYellowPageWebsiteEmailService;
import selleck.email.service.impl.YellowPageServiceImpl;
import selleck.email.service.impl.YellowPageWebsiteEmailServiceImpl;
import selleck.utils.Constants;
import selleck.utils.HTMLUtils;
import selleck.utils.StringUtils;

public class FindEmailsFromYellowPageWebsite {
	private static final IYellowPageService yellowPageService = new YellowPageServiceImpl();
	private static final IYellowPageWebsiteEmailService ypweService = new YellowPageWebsiteEmailServiceImpl(Constants.LIFE_SCIENCE_DB);
	private static final int TIER = 2; // 网页抓取深度层级
	

	/** 访问YellowPage(emailhunter.search_yellowpage_by_keyword)的website，爬取若干层，抓取email。
	 * 存入emailhunter.selleck_edm_yellowpage_email
	 * @param args
	 */
	public static void main(String[] args) {
		Criteria criteria = new Criteria();
		criteria.setWhereClause(" website is not null and website != '' GROUP BY website order by id "); 
		// criteria.setOrderByClause("id");
		// criteria.setWhereClause(" id = 122099  "); 
		// criteria.setWhereClause(" website is not null and website != '' GROUP BY website");
		List<YellowPage> yellowpages = yellowPageService.selectByExample(criteria);
		for(YellowPage yp : yellowpages){
			String website = yp.getWebsite();
			System.out.println("yellowpage website: "+website + "  id: "+yp.getId());
			try{
				Set<String> allLinkSet = new HashSet<String>();
				getEmail(website, website , 1 , allLinkSet);
			}catch (Exception e){
				e.printStackTrace();
				continue;
			}
		}
		
	}
	
	/**
	 * @param url
	 * @param website 网站首页
	 * @param tier 抓取深度层级，控制迭代的次数
	 * @return
	 */
	private static void getEmail(String url , String website , int tier , Set<String> allLinkSet){
		Map<String,String> htmlMap = selleck.utils.HTTPUtils.getCookieUrlAndHtml(url, null,null, HTTPUtils.GET, null);
		String html = htmlMap.get("html");
		String realUrl = htmlMap.get("url");
		if(html == null || html.isEmpty()){
			return;
		}
		allLinkSet.add(url);
		
		html = StringEscapeUtils.unescapeHtml4(html); // 去转义字符，  &gt;  转换成>符号
		try {
			html = java.net.URLDecoder.decode(html, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		// 查找email
		Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
		Matcher matcher = p.matcher(html);
		while(matcher.find()){
			String email = matcher.group();
			YellowPageWebsiteEmail ypwe = new YellowPageWebsiteEmail();
			ypwe.setEmail(email);
			ypwe.setTier(tier);
			ypwe.setRealUrl(url);
			ypwe.setWebsite(website);
			ypweService.saveYellowPageWebsiteEmail(ypwe);
			
			System.out.println("find email: "+email);
		}
		
		if(tier >= TIER){
			return;
		}
		
		tier ++;
		
		// 查找link
		Set<String> links = HTMLUtils.catchUrlList(html);
		links.removeAll(allLinkSet);
		allLinkSet.addAll(links);
		
		// String domain = getDomainFromUrl(realUrl);
		URI uri = null;
		try {
			uri = new URI(realUrl);
			String domain = uri.getHost();
			domain = domain.startsWith("www.") ? domain.substring(4) : domain;
			String host = uri.getHost();
			String path = uri.getPath();
			String scheme = uri.getScheme();
			for(String link : links){
				try{
					if(link.startsWith("http")){
						if(link.contains(domain) && !link.equals(website)){
							getEmail(link , website , tier , allLinkSet);
						}
					}else{
						if(!link.equals(website)){
							if(link.startsWith("/")){ // 绝对路径
								getEmail(scheme + "://" + host + link , website , tier , allLinkSet);
							}else{ // 相对路径
								String relativePath = path.substring(0, path.lastIndexOf("/")==-1 ? 0 : path.lastIndexOf("/"));
								getEmail(scheme + "://" + host + relativePath + "/" + link , website , tier , allLinkSet);
							}
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
	}



}
