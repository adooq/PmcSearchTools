package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Jsoup;

import common.handle.model.Criteria;
import selleck.email.pojo.ScienceDirect;
import selleck.email.service.IScienceDirectService;
import selleck.email.service.impl.ScienceDirectServiceImpl;
import selleck.email.update.frame.UpdateScienceDirectFrame;
import selleck.email.update.parser.ScienceDirectArticleParser;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.StringUtils;

public class UpdateScienceDirect {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
	public static String DOMAIN = "http://www.sciencedirect.com";
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	IScienceDirectService scienceDirectService = null;
	List<Integer> years; // 要查询的年份
	StringBuffer yearSB; // yearSB 判断年份的正则表达式
	
	public static void main(String[] args) {
		final UpdateScienceDirectFrame frame = UpdateScienceDirectFrame.getFrame();
		final UpdateScienceDirect updateSD = new UpdateScienceDirect();
		updateSD.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updateSD.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updateSD.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updateSD.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updateSD.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("自动更新ScienceDirect文章")) {
						INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
						String excelName = frame.getExcelText().getText();
						String startEntrez = frame.getPublicationText().getText();
						String endEntrez = frame.getCookieText().getText();
						String pubRange = frame.getPubLabel().getText();
						String dbName = frame.getDbSelect().getSelectedItem().toString();
						updateSD.scienceDirectService = new ScienceDirectServiceImpl(dbName);
						updateSD.loadPubFromExcel(excelName,startEntrez,endEntrez,pubRange);
						// updateSpringer.loadPubFromCSV(frame.getExcelText().getText(),frame.getPublicationText().getText(),frame.getCookieText().getText());
					}
					updateSD.setStartFlag(false);
					frame.stopMouseClicked();
					updateSD.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	/**
	 * 读取期刊的excel，根据期刊名一个个去SciencDirect的期刊页面搜索文章。
	 * 一个期刊细分成Volume.Issue，每一个Issue的url才有文章url。所以，要先找到这个期刊的规定年份内的Issue的url。 
	 * @param excel 纯excel文件名，不需要路径， 形如：SciencDirect publications.xlsx
	 * @param startEntrez  文章时间  2009  SciencDirect只接受年份查询
	 * @param endEntrez   文章时间  2014 or Present SciencDirect只接受年份查询
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	public void loadPubFromExcel(String excel ,String startEntrez, String endEntrez, String pubRange){
		Workbook wb = null;
		// HMARobot robot = new HMARobot(); 暂时不考虑换ip
		try {
			wb = WorkbookFactory.create(new File(excel));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			int pubStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
			int pubEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
			pubEnd = Math.min(pubEnd, rowCount);
			// for (int i = 0; i <= 0; i++) { // for test
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
			for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
				try {
					Row row = sheet.getRow(i);
					String pubNameUrl = row.getCell(0).getStringCellValue().trim(); // 期刊URL
					String pubName = row.getCell(1).getStringCellValue().trim(); // 期刊名
					this.getArticleUrl(pubName,pubNameUrl,startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
					this.queryArticles(pubName); // 按库中搜到的文章url更新文章的内容
					// robot.changeIP(loggerTA); // 搜索完一个期刊，HMA 换个IP
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
			}
		} catch (InvalidFormatException e1) {
			this.loggerTA.append("excel文件格式不正确 \n");
			e1.printStackTrace();
		} catch (IOException e1) {
			this.loggerTA.append("excel文件访问出错 \n");
			e1.printStackTrace();
			return;
		}
		
	}
	
	/**
	 * 根据搜索条件，获得所有文章url，先保存到库里。
	 * 一个期刊细分成Volume和Issue，每一个Issue的url才有文章url。所以，要先找到这个期刊的规定年份内的Issue的url。 
	 * @param journal
	 * @param journal url
	 * @param startEntrez
	 * @param endEntrez
	 */
	public void getArticleUrl(String journal, String journalUrl , String startEntrez , String endEntrez) {
		loggerTA.append("开始查询期刊"+journal+"   "+startEntrez+"-"+endEntrez+"\n");
		
		int startYear = Integer.valueOf(startEntrez);
		int endYear = Integer.valueOf(endEntrez);
		years = new ArrayList<Integer>(); // 要查询的年份
		years.add(startYear);
		yearSB = new StringBuffer("(("+startEntrez+")"); // yearSB 判断年份的正则表达式
		while(startYear <= endYear){
			startYear ++;
			years.add(startYear);
			yearSB.append("|(").append(startYear).append(")");
		}
		yearSB.append(")");
		
		String html = getArticleUrlByIssue(journal , journalUrl);
		
		// 循环查询期刊Issue url
		// <div class="txt currentVolumes"><A HREF="/science/journal/00928674/151/3">Volume 151, Issue 3</A><br>pp. 457-690 (26 October 2012)</div>
		List<String> issues = ParserUtils.findWithPrefixAndSuffix("<div class=\"txt currentVolumes\"><A HREF=" , "</div>" , html);
		
		Set<String> issueUrlList = new HashSet<String>(); // 期刊的每一个Issue的url
		for(String issue : issues){
			Pattern pp = Pattern.compile("\\b"+yearSB.toString()+"\\b");
			Matcher mm = pp.matcher(issue);
			if(mm.find()){
				Pattern p = Pattern.compile("/science/journal(/[\\p{Alnum}\\-]+){2,}+");
				Matcher m = p.matcher(issue);
				if(m.find()){
					issueUrlList.add(DOMAIN + m.group());
				}
			}
		}
		
		
		
		// 循环查询其他Volume url
		Set<String> volumeUrlList = new HashSet<String>();
		Pattern pattern = Pattern.compile("<a href=\"/science/journal/\\p{Alnum}+/\\d+\"\\p{ASCII}+?aria-expanded=\"false\"\\p{ASCII}+?</a>", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(html);
		while(matcher.find()){
			String vHtml = matcher.group();
			String v = Jsoup.parse(vHtml).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			Pattern pp = Pattern.compile("\\b"+yearSB.toString()+"\\b");
			Matcher mm = pp.matcher(v);
			if(mm.find()){
				Pattern p = Pattern.compile("/science/journal/\\p{Alnum}+/\\d+");
				Matcher m = p.matcher(vHtml);
				if(m.find()){
					volumeUrlList.add(DOMAIN + m.group());
				}
			}
		}
		
		// 抓每个volume里的issue url
		for(String volumeUrl : volumeUrlList){
			issueUrlList.addAll(this.getIssueUrl(journal, volumeUrl, startEntrez, endEntrez));
		}
		
		// 抓每个issue里的article url
		for(String issueUrl : issueUrlList){
			getArticleUrlByIssue(journal , issueUrl);
		}
		
		
	}
	
	/**
	 * 按文章的url去访问文章网页并抓取内容。
	 * sciencedirect的文章信息（标题，作者，摘要等）放在另一个请求里，形如http://www.sciencedirect.com/science/frag/S0927024814000440/${SDM.pf.frag.fat}/all
	 * 要先从文章页面获得SDM.pf.frag.fat参数，再访问那个请求来抓取文章信息。
	 * @param pubName excel中的期刊名。
	 */
	public void queryArticles(String pubName){
		Criteria criteria = new Criteria();
		// criteria.setWhereClause(" title = '' and have_read = 0 and (authors = '' and email = '' and full_text = '')  limit 20000,20000");
		criteria.setWhereClause(" (title is null or title = '') and SOURCE_PUBLICATION = '"+StringUtils.toSqlForm(pubName)+"' ");
		List<ScienceDirect> scienceDirects = scienceDirectService.selectBySearchPublicaton(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+scienceDirects.size()+"篇文章需要抓取\n");
		for(ScienceDirect scienceDirect : scienceDirects){
			try{
				String articleUrl = scienceDirect.getUrl();
				// String cookie = "EUID=e64a9024-631c-11e4-b8a0-00000aacb35f; MIAMISESSION=e63af7f4-631c-11e4-b8a0-00000aacb35f:3592447076; CARS_COOKIE=0228e002b508f7f5a9716e7be42a4415bb35c337a923036d327be68643d45f3a48bbe6f2fd6b2683e4c5b9667a491b3ebc7537968b50ab3a; USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674; TARGET_URL=fcf74dd786744d87fbaaaf8652a764ab4a79b0d3ed681139e910692376063105cb1468c63e712961e8908ef471ab6177d378e78873d526a2; DEFAULT_SESSION_SUBJECT=; sid=538a681a-6969-4b8e-9ce0-05fbf018ea3e; optimizelySegments=%7B%22204775011%22%3A%22ff%22%2C%22204658328%22%3A%22false%22%2C%22204736122%22%3A%22direct%22%2C%22204728159%22%3A%22none%22%7D; optimizelyEndUserId=oeu1414993652634r0.9870375613025336; optimizelyBuckets=%7B%7D; s_cc=true; s_fid=1518D78B022E0AA7-28ACA5E95FFD2A1C; s_nr=1414994274851-New; s_vnum=1417363200365%26vn%3D1; s_invisit=true; s_lv=1414994274852; s_lv_s=First%20Visit; gpv_p19=article; s_ppv=-; s_sq=%5B%5BB%5D%5D; gpv_e2=Explicit; __cp=1414994274666; gpv_art_abs_prd=%3BS0092867414012392; gpv_art_abs_dt=1414994274853";
				// String cookieDomain = ".sciencedirect.com";
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null ,null, HTTPUtils.GET , null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String articleHtml = articleMap.get("html");
				if(articleHtml == null || articleHtml.isEmpty()){
					continue;
				}
				
				// 抓取页面上的SDM.pf.frag.fat参数，
				List<String> fats = ParserUtils.findWithPrefixAndSuffix("SDM.pf.frag.fat = '" , "';" , articleHtml);
				if(fats.size() > 0){
					String fat = fats.get(0);
					fat = ParserUtils.trim(fat);
					
					String articleId = articleUrl.substring(articleUrl.lastIndexOf("/") + 1); // 形如S1876285914003295
					String flagAllUrl = DOMAIN + "/science/frag/" + articleId + "/" + fat + "/all";
					// cookie = "EUID=e64a9024-631c-11e4-b8a0-00000aacb35f; MIAMISESSION=e63af7f4-631c-11e4-b8a0-00000aacb35f:3592447157; CARS_COOKIE=0228e002b508f7f5a9716e7be42a4415bb35c337a923036d327be68643d45f3a48bbe6f2fd6b2683e4c5b9667a491b3ebc7537968b50ab3a; USER_STATE_COOKIE=346fa8c434beaa18dbb50e2b7e6f20803c2c344c39e22674; TARGET_URL=fcf74dd786744d87fbaaaf8652a764ab4a79b0d3ed681139e910692376063105cb1468c63e712961e8908ef471ab6177d378e78873d526a2; DEFAULT_SESSION_SUBJECT=; sid=538a681a-6969-4b8e-9ce0-05fbf018ea3e; optimizelySegments=%7B%22204775011%22%3A%22ff%22%2C%22204658328%22%3A%22false%22%2C%22204736122%22%3A%22direct%22%2C%22204728159%22%3A%22none%22%7D; optimizelyEndUserId=oeu1414993652634r0.9870375613025336; optimizelyBuckets=%7B%7D; s_cc=true; s_fid=1518D78B022E0AA7-28ACA5E95FFD2A1C; s_nr=1414994357746-New; s_vnum=1417363200365%26vn%3D1; s_invisit=true; s_lv=1414994357747; s_lv_s=First%20Visit; gpv_p19=article; s_ppv=-; s_sq=%5B%5BB%5D%5D; gpv_e2=Explicit; __cp=1414994357390; gpv_art_abs_prd=%3BS0092867414012392; gpv_art_abs_dt=1414994274853; optimizelyPendingLogEvents=%5B%5D; gpv_art_full_prd=%3BS187628591400343X; gpv_art_full_dt=1414994357748";
					
					Map<String,String> fragMap = HTTPUtils.getCookieUrlAndHtml(flagAllUrl, null ,null, HTTPUtils.GET , null);
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					String fragHtml = fragMap.get("html");
					if(fragHtml == null || fragHtml.isEmpty()){
						continue;
					}
					
					scienceDirect = ScienceDirectArticleParser.parseFromHTML(fragHtml , scienceDirect);
					scienceDirectService.updateScienceDirect(scienceDirect);
					loggerTA.append(scienceDirect.getTitle()+"\n");
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
			
		}
		
	}
	
	/**
	 * 访问期刊里一个Issue的url，抓取该页面上文章url
	 * @param pubName
	 * @param issueUrl 
	 * @return html Issue页面的html
	 */
	public String getArticleUrlByIssue(String pubName , String issueUrl){
		Map<String,String> journalMap = HTTPUtils.getCookieUrlAndHtml(issueUrl, null ,null, HTTPUtils.GET ,null);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(journalMap.size() == 0){
			loggerTA.append("GET 查询失败\n");
			System.out.println("GET 查询失败");
			return "";
		}
		
		String html = journalMap.get("html");
		
		// 检查当前选中的Issue，如果不在年份范围就不抓文章url 了。
		// <div class="txt currentVolumes"><span aria-selected="true" style="color: #0156aa; font-weight: bold">Volume 43, Issue 12</span><span class="txtHidden"> - selected</span><br>pp. 4213-4503 (December 1995)</div>
		List<String> issues = ParserUtils.findWithPrefixAndSuffix("<div class=\"txt currentVolumes\"><span" , "</div>" , html);
		if(issues.size() > 0){
			String issue = issues.get(0);
			Pattern pp = Pattern.compile("\\b"+yearSB.toString()+"\\b");
			Matcher mm = pp.matcher(issue);
			if(!mm.find()){
				return html;
			}
		}
		
		
		// 抓取文章url
		// http://www.sciencedirect.com/science/article/pii/S0092867414012252
		String regex = "http://www.sciencedirect.com/science/article/pii/\\p{Alnum}+\"";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(html);
		while(matcher.find()){
			ScienceDirect sd = new ScienceDirect();
			String url = matcher.group();
			url = url.substring(0, url.length() - 1);
			sd.setUrl(url);
			sd.setSourcePublication(pubName);
			scienceDirectService.saveScienceDirect(sd);
			loggerTA.append(url+"\n");
		}
		
		return html;
	}
	
	
	/**
	 * 抓取volume页面上的IssueURL，并保存volume页面上的文章url
	 * @param journal
	 * @param volumeUrl
	 * @param startEntrez
	 * @param endEntrez
	 * @return
	 */
	public List<String> getIssueUrl(String journal , String volumeUrl , String startEntrez, String endEntrez){
		String html = getArticleUrlByIssue(journal , volumeUrl);
		
		// 循环查询期刊Issue url
		// <div class="txt currentVolumes"><A HREF="/science/journal/00928674/151/3">Volume 151, Issue 3</A><br>pp. 457-690 (26 October 2012)</div>
		List<String> volumes = ParserUtils.findWithPrefixAndSuffix("<div class=\"txt currentVolumes\">" , "</div>" , html);
		int startYear = Integer.valueOf(startEntrez);
		int endYear = Integer.valueOf(endEntrez);
		List<Integer> years = new ArrayList<Integer>(); // 要查询的年份
		years.add(startYear);
		StringBuffer yearSB = new StringBuffer("(("+startEntrez+")");
		while(startYear <= endYear){
			startYear ++;
			years.add(startYear);
			yearSB.append("|(").append(startYear).append(")");
		}
		yearSB.append(")");
		List<String> issueUrlList = new ArrayList<String>(); // 期刊的每一个Issue的url
		for(String v : volumes){
			Pattern pp = Pattern.compile("\\b"+yearSB.toString()+"\\b");
			Matcher mm = pp.matcher(v);
			if(mm.find()){
				Pattern p = Pattern.compile("/science/journal(/[\\p{Alnum}\\-]+){2,}+");
				Matcher m = p.matcher(v);
				if(m.find()){
					issueUrlList.add(DOMAIN + m.group());
				}
			}
		}
		
		return issueUrlList;
	}


	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	
}

/**
 * http 工具类
 * 
 * @author fscai
 * @see http://www.baeldung.com/httpclient-4-cookies
 */
class HTTPUtils {
	public static final String GET = "GET";
	public static final String POST = "POST";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0";
	private static final String ACCEPT_LANGUAGE = "zh-cn,en-us;q=0.8,en;q=0.5,zh;q=0.3";

	/**
	 * 访问一个网址，返回最终url cookie html 信息，可能经过多次302跳转
	 * 
	 * @param targetURL
	 * @param cookie , 如果没有则填null
	 * @param cookieDomain 如果cookie没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @return  Map<String,String> 有三个key，返回url  , cookie , html
	 */
	public static Map<String, String> getCookieUrlAndHtml(String targetURL,String cookie,String cookieDomain, String method, Map<String, String> params) {
		Map<String, String> rs = new HashMap<String, String>();
		CookieStore cookieStore = new BasicCookieStore();
		if(cookie != null){
			String[] cookies = cookie.split(";");
			for(String c : cookies){
				String[] cc = c.split("=");
			    BasicClientCookie clientCookie = new BasicClientCookie(cc[0].trim(), cc.length > 1 ? cc[1].trim() : "");
			    clientCookie.setDomain(cookieDomain);
			    clientCookie.setPath("/");
			    cookieStore.addCookie(clientCookie);
			}
		}
		HttpClientContext httpClientContext = sendRequest(targetURL, cookieStore,method, params);
		if(httpClientContext == null){ // 请求失败
			return rs;
		}
		HttpEntity entity = httpClientContext.getResponse().getEntity();
		List<Cookie> newCookiesList = httpClientContext.getCookieStore().getCookies();
		String newCookies = convertCookieList2Str(newCookiesList);
		newCookies = updateCookieStr(cookie, newCookies);
		try {
			if (entity != null && entity.getContentType().toString().contains("html")) {
				/*
				byte[] contentArray = EntityUtils.toByteArray(entity);
				if (contentArray.length > 0) {
					String contentType = entity.getContentType().toString();
					int charsetIndex = contentType.indexOf("charset=");
					String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex + 8) : null;
					// System.out.println("encoding: "+encoding);
					String htmlStr = new String(contentArray,encoding == null ? "UTF-8" : encoding);
					rs.put("html", htmlStr);
				}
				*/
				
				String contentType = entity.getContentType().toString();
				int charsetIndex = contentType.indexOf("charset=");
				String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex + 8) : "UTF-8";
				String htmlStr = EntityUtils.toString(entity, encoding);
				rs.put("html", htmlStr);
			}else{
				rs.put("html", "");
			}

			rs.put("cookie", newCookies);
			
			// 个人感觉HttpGet内部会自动处理302跳转，然后把经历的跳转都放到httpClientContext.getRedirectLocations()中，集合中最后一个就是最新的请求
			// System.out.println("httpClientContext.getRedirectLocations(): "+httpClientContext.getRedirectLocations());
			if(httpClientContext.getRedirectLocations() == null){
				rs.put("url", targetURL);
			}else{
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
	 * @param cookie , 如果没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @param file , 要下载的目标文件，主要用来传入file的文件名路径。
	 */
	public static void downloadFile(String targetURL, String cookie, String method, Map<String, String> params , File file){
		CookieStore cookieStore = new BasicCookieStore();
		if(cookie != null){
		    BasicClientCookie clientCookie = new BasicClientCookie(cookie.split("=")[0], cookie.split("=")[1]);
		    clientCookie.setDomain("*");
		    clientCookie.setPath("/");
		    cookieStore.addCookie(clientCookie);
		}
		HttpClientContext httpClientContext = sendRequest(targetURL, cookieStore, method, params);
		if(httpClientContext != null){
			HttpEntity httpEntity = httpClientContext.getResponse().getEntity();
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file);
				InputStream input = httpEntity.getContent();
				byte b[] = new byte[1024];
				int j = 0;
				while( (j = input.read(b))!=-1){
					output.write(b,0,j);  
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(output != null){
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
	 * @param cookieStore , 如果没有则填null
	 * @param method  get or post
	 * @param params  body参数，如果是GET,则为null
	 * @return  HttpClientContext，如果访问失败，return null
	 */
	private static HttpClientContext sendRequest(String targetURL, CookieStore cookieStore, String method, Map<String, String> params) {
		System.out.println("sendRequest");
		System.out.println("url: "+targetURL);
		System.out.println("method: "+method);
		
		/*
		if(cookieStore != null && cookieStore.getCookies() != null){
			Iterator<Cookie> iter = cookieStore.getCookies().iterator();
			for(;iter.hasNext();){
				Cookie c = iter.next();
				System.out.println("cookie : "+c.getName() +"="+c.getValue() +"  "+c.getDomain()+"  "+c.getPath());
			}
		}
		*/
		
//		CookieStore newCookieStore = new BasicCookieStore();
//		if(cookieStore != null){
//			Iterator<Cookie> iter2 = cookieStore.getCookies().iterator();
//			for(;iter2.hasNext();){
//				Cookie c = iter2.next();
//				BasicClientCookie clientCookie = new BasicClientCookie(c.getName(), c.getValue());
//			    clientCookie.setDomain(".ashland.edu");
//			    clientCookie.setPath("/");
//			    newCookieStore.addCookie(clientCookie);
//			}
//		}
		
		CloseableHttpClient httpclient = null;
		HttpClientContext httpClientContext = HttpClientContext.create();
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(300000).setConnectTimeout(300000)
				.setSocketTimeout(300000).setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
				.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true).build();
		
		SSLContextBuilder builder = new SSLContextBuilder();
		SSLConnectionSocketFactory sslsf = null;
	    try {
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			sslsf = new SSLConnectionSocketFactory(builder.build() , SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (NoSuchAlgorithmException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (KeyStoreException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		httpclient = HttpClientBuilder.create()
				.setDefaultRequestConfig(requestConfig)
				.setUserAgent(USER_AGENT)
				.setDefaultCookieStore(cookieStore)
				.setSSLSocketFactory(sslsf).build();
		// httpclient = HttpClientBuilder.create().build();
		// sslSetting(httpclient);
		httpClientContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		httpClientContext.setCookieStore(cookieStore);
		
		HttpRequestBase httpRequest;
		if (method.equals(GET)) {
			try{
				httpRequest = new HttpGet(targetURL);
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		} else if (method.equals(POST)) {
			try{
				httpRequest = new HttpPost(targetURL);
			}catch(Exception e){
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
					((HttpPost)httpRequest).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			
		} else {
			return null;
		}

		// 已经 HttpClientBuilder.create().setDefaultCookieStore(cookieStore)了，不需要再设置cookiestore
//		if(cookieStore != null && !cookieStore.getCookies().isEmpty()){
//			CookieStore cookieStore = new BasicCookieStore();
//		    BasicClientCookie clientCookie = new BasicClientCookie(cookie.split("=")[0], cookie.split("=")[1]);
//		    clientCookie.setDomain(".ashland.edu");
//		    clientCookie.setPath("/");
//		    cookieStore.addCookie(clientCookie);
		    // httpClientContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		    
			// This is of course much more error-prone than working with the built in cookie support
			// – for example, notice that we’re no longer setting the domain in this case – which is not correct.
			// httpRequest.setHeader("Cookie", cookie);
//		}
		httpRequest.setHeader("User-Agent", USER_AGENT);
		httpRequest.setHeader("Accept-Language", ACCEPT_LANGUAGE);  
		httpRequest.setHeader("Referer", "http://www.sciencedirect.com/");  
		
		HttpResponse response = null;
		
		httpRequest.setConfig(requestConfig);
		try {
			response = httpclient.execute(httpRequest , httpClientContext);
			// response = httpclient.execute(httpRequest);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			System.out.println("httpclient.execute(httpGet) ClientProtocolException");
			// logger.append(" httpclient.execute(httpGet) ClientProtocolException\n");
			return null;
		}catch (javax.net.ssl.SSLHandshakeException ssle){
			ssle.printStackTrace();
			return null;
		} catch (IOException e1) {
			System.out.println("httpclient.execute(httpGet) IOException -- try again");
			e1.printStackTrace();
			// 重新尝试发一次请求
			try {
				response = httpclient.execute(httpRequest , httpClientContext);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				System.out.println("httpclient.execute(httpGet) IOException -- try again twice");
				e.printStackTrace();
				// 重新尝试发一次请求
				try {
					response = httpclient.execute(httpRequest , httpClientContext);
				} catch (ClientProtocolException ee) {
					ee.printStackTrace();
					return null;
				} catch (IOException ee) {
					System.out.println("httpclient.execute(httpGet) IOException again twice");
					e.printStackTrace();
					return null;
				}
				return null;
			}
		}catch (Exception ee){
			ee.printStackTrace();
			return null;
		}
		System.out.println();
		
//		List<Cookie> newCookiesList;
//		newCookiesList = httpClientContext.getCookieStore() == null? null : httpClientContext.getCookieStore().getCookies();
//		String newCookies = convertCookieList2Str(newCookiesList);
//		newCookies = updateCookieStr(cookie , newCookies);
		// System.out.println("new cookies: "+newCookies);
		
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			return httpClientContext;
			// return null;	
			
		}else if (HttpStatus.SC_MOVED_TEMPORARILY == response.getStatusLine().getStatusCode() || // 302 跳转
				HttpStatus.SC_MOVED_PERMANENTLY == response.getStatusLine().getStatusCode() ) { // 301 跳转
			// 请求成功 取得请求内容
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				System.out.println("  EntityUtils.consume(entity)  IOException");
				// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
				// e.printStackTrace();
			}
			
			Header locationHeader = response.getFirstHeader("location");
			if (locationHeader != null) {
				URI uri = httpRequest.getURI();
				String host = uri.getHost();
				String path = uri.getPath();
				String scheme = uri.getScheme();
				String locationUrl = locationHeader.getValue();
				if(locationUrl.startsWith("http")){
					// locationUrl 就是完整url
				}else if(locationUrl.startsWith("/")){ // 绝对路径
					locationUrl = scheme + "://" + host + locationUrl;
				}else{ // 相对路径
					String relativePath = path.substring(0, path.lastIndexOf("/")==-1 ? 0 : path.lastIndexOf("/"));
					locationUrl = scheme + "://" + host + relativePath + "/" + locationUrl;
				}
				return sendRequest(locationUrl , httpClientContext.getCookieStore(), HTTPUtils.GET, null);
			}
		}else {
			HttpEntity entity = response.getEntity();

			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				System.out.println("  EntityUtils.consume(entity)  IOException");
				// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
				// e.printStackTrace();
			}
			System.out.println("  访问失败 " + response.getStatusLine().getStatusCode());
			// logger.append("  下载请求失败" + "\n");
		}
		
		return null;
	}
	
	/**
	 * 把List<Cookie> 转换成String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @param cookies  List<Cookie>
	 * @return 如果cookies为null 或是空集合，返回""
	 */
	public static String convertCookieList2Str(List<Cookie> cookies){
		if(cookies == null){
			return "";
		}
		StringBuffer cookieStr = new StringBuffer();
		for (Cookie c : cookies) {
			cookieStr.append(c.getName()).append("=").append(c.getValue()).append(";");
		}
		return cookieStr.toString();
	}
	
	

	
	/**
	 * 更新cookie list，即如果新的cookie list不包含老的list中的某个cookie,加入到新的list
	 * @param oldCookies  String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @param newCookies  String, 形如ezproxy=Yx7nHF8gdyyW6ds;JSESSIONID=F6B1B5161B6034B5A79904397F59CB3F;
	 * @return 新的cookie String
	 */
	private static String updateCookieStr(String oldCookies ,String newCookies){
		if(oldCookies == null || oldCookies.trim().isEmpty()){
			return newCookies;
		}
		if(newCookies == null|| newCookies.trim().isEmpty()){
			return oldCookies;
		}
		
		String[] oldCookieArr = oldCookies.split(";");
		String[] newCookieArr = newCookies.split(";");
		for(String oc : oldCookieArr){
			if(oc.isEmpty()){
				continue;
			}
			boolean notInNew = true;
			String oName = oc.split("=")[0];
			for(String nc : newCookieArr){
				if(nc.isEmpty()){
					continue;
				}
				String nName = nc.split("=")[0];
				if(nName.equals(oName)){
					notInNew = false;
					break;
				}
			}
			if(notInNew){
				newCookies = newCookies + oc + ";";
			}
		}
		return newCookies;
	}
	
	public static void main(String[] args){
		String searchUrl = "http://apps.webofknowledge.com.newproxy.downstate.edu/WOS_GeneralSearch.do";
		String cookies = "ezproxy=EKjpggcPLtdyGnm";
		String cookieDomain = ".newproxy.downstate.edu";
		String domain = "apps.webofknowledge.com.newproxy.downstate.edu";
		String sid = "4BZYvdf2QfNJwyaCQKX";
		String range = "YearToDate";
		String pName = "Nucleic Acids Research";
		Map<String, String> params = null;
		Map<String, String>  rsMap = HTTPUtils.getCookieUrlAndHtml(searchUrl, cookies ,cookieDomain, HTTPUtils.POST ,params);
		
		System.out.println(rsMap.get("html"));
	}

	@Deprecated
	private static void sslSetting(HttpClient client){
		try {
			TrustManager easyTrustManager = new X509TrustManager() {
				// To change body of implemented methods use File | Settings |
				// File Templates.
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] x509Certificates,
						String s)
						throws java.security.cert.CertificateException {
				}

				// To change body of implemented methods use File | Settings |
				// File Templates.
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] x509Certificates,
						String s) 
						throws java.security.cert.CertificateException {
				}

				// To change body of implemented methods use File | Settings |
				// File Templates.
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[0];
				}
			};

			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext
					.init(null, new TrustManager[] { easyTrustManager }, null);
			SSLSocketFactory sf = new SSLSocketFactory(sslcontext);

			Scheme sch = new Scheme("https", 443, sf);

			client.getConnectionManager().getSchemeRegistry().register(sch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

