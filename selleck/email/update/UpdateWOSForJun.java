package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import selleck.email.pojo.WOS;
import selleck.email.service.IWOSService;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.email.update.frame.UpdateWOSFrame;
import selleck.email.update.parser.WOSArticleParser;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.wosaccount.BasicAccount;
import selleck.email.wosaccount.IAccount;
import selleck.utils.Constants;
import selleck.utils.HTTPUtils;


/**
 * 更新WOS文章。根据一个期刊excel，以25个期刊一次查询去WOS搜索。
 * 可以设置更新的期限，wos账号，要搜索的期刊在excel中的范围，间隔等参数。
 * @author fscai
 *
 */
public class UpdateWOSForJun {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	
	
	IWOSService wosService = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final UpdateWOSFrame frame = UpdateWOSFrame.getFrame();
		final UpdateWOSForJun updateWOS = new UpdateWOSForJun();
		updateWOS.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updateWOS.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updateWOS.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updateWOS.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updateWOS.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
					String excelName = frame.getExcelText().getText();
					String account = frame.getAccountSelect().getSelectedItem().toString();
					String pubRange = frame.getPubLabel().getText();
					String dbName = frame.getDbSelect().getSelectedItem().toString();
					updateWOS.wosService = new WOSServiceImpl(Constants.JUN);
					if (select.equals("最近4周")) {
						// updateWOS.loadPubFromCSV(frame.getExcelText().getText(),"4week",frame.getAccountSelect().getSelectedItem().toString());
						updateWOS.loadPubFromExcel(excelName,"4week",account,pubRange);
					}else if(select.equals("最近2周")){
						updateWOS.loadPubFromExcel(excelName,"2week",account,pubRange);
					}else if(select.equals("本年迄今")){
						updateWOS.loadPubFromExcel(excelName,"YearToDate",account,pubRange);
					}else if(select.equals("最近5年")){
						updateWOS.loadPubFromExcel(excelName,"Latest5Years",account,pubRange);
					}else if(select.equals("所有年份")){
						updateWOS.loadPubFromExcel(excelName,"ALL",account,pubRange);
					}
					updateWOS.setStartFlag(false);
					frame.stopMouseClicked();
					updateWOS.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
	}
	
	/**
	 * 读取期刊的excel，根据期刊名一个个去WOS搜索文章。
	 * @param excel 纯excel文件名，不需要路径， 形如： wos.xlsx
	 * @param range 时间跨度  4week等
	 * @param account 账号名
	 * @param pubRange 期刊在excel里的范围   形如: 1-200
	 */
	public void loadPubFromExcel(String excel ,String range, String account, String pubRange){
		Workbook wb = null;
		try {
			Map<String,String> loginMap = this.loginAccount(account);
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(loginMap == null){
				System.out.println("登陆失败");
				this.getLoggerTA().append("登陆失败\n");
				return;
			}
			System.out.println("登陆成功");
			this.getLoggerTA().append("登陆成功\n");
			
	 		String cookies = loginMap.get("cookie");
			String url = loginMap.get("url");
			
			wb = WorkbookFactory.create(new File(excel));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			int pubStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
			int pubEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
			pubEnd = Math.min(pubEnd, rowCount);
			List<String> pubNames = new ArrayList<String>(25); // 一次搜索25个期刊，减少查询次数，貌似查询请求太多会封号。
			// for (int i = 0; i <= 0; i++) { // for test
			for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
				Row row = sheet.getRow(i);
				String pubName = row.getCell(0).getStringCellValue().trim(); // 期刊名
				pubNames.add(pubName);
				if(pubNames.size() >= 25){
					try {
						this.updateByPubAndRange(url, cookies, pubNames, range); // 根据期刊名和时间range去搜索
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}finally{
						pubNames.clear();
					}
				}
			}
			if(pubNames.size() > 0){
				try {
					this.updateByPubAndRange(url, cookies, pubNames, range); // 根据期刊名和时间range去搜索
				} catch (Exception e) {
					e.printStackTrace();
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
	
	public void loadPubFromCSV(String csvName, String range, String account){
		BufferedReader br = null;
		try {
			File csv = new File(csvName); // CSV文件
			if (!csv.exists()) {
				this.loggerTA.append("CSV 文件不存在 \n");
				return;
			}
			
			Map<String,String> loginMap = this.loginAccount(account);
			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(loginMap == null){
				System.out.println("登陆失败");
				this.getLoggerTA().append("登陆失败\n");
				return;
			}
			System.out.println("登陆成功");
			this.getLoggerTA().append("登陆成功\n");
			
	 		String cookies = loginMap.get("cookie");
			String url = loginMap.get("url");
			
			br = new BufferedReader(new FileReader(csv));
			// 读取直到最后一行
			String line = "";
			List<String> pubNames = new ArrayList<String>(25);
			while ((line = br.readLine()) != null) {
				pubNames.add(line);
				try{
					if(pubNames.size() >= 25){
						this.updateByPubAndRange(url, cookies, pubNames, range); // 根据期刊名和时间range去搜索
						pubNames.clear();
					}
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			}
			if(pubNames.size() > 0){
				this.updateByPubAndRange(url, cookies, pubNames, range); // 根据期刊名和时间range去搜索
			}
		} catch (FileNotFoundException e) {
			this.loggerTA.append("CSV 文件不存在 \n");
			e.printStackTrace();
		} catch (IOException e) {
			this.loggerTA.append("excel文件访问出错 \n");
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 登陆WOS账号
	 * @param account 账号名
	 * @return Map<String,String> 有三个key，返回url  , cookie , html
	 */
	public Map<String,String> loginAccount(String account){ 
		Properties p = new Properties();  
		try {
			p.load(new BufferedInputStream(new FileInputStream("wos accounts.properties")));
		} catch (FileNotFoundException e) {
			this.getLoggerTA().append("账号文件读取失败\n");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			this.getLoggerTA().append("账号文件读取失败\n");
			e.printStackTrace();
			return null;
		}
		
		IAccount wosAccount = null;
		try {
			String firstChar = account.substring(0, 1);
			String className = account.replaceFirst(firstChar, firstChar.toUpperCase()); // account 首字母大写
			Class<? extends IAccount> accountClass = (Class<? extends IAccount>)Class.forName("selleck.email.wosaccount."+className);
			wosAccount = accountClass.newInstance();
		} catch (ClassNotFoundException e1) {
			// e1.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		if(wosAccount == null){
			wosAccount = new BasicAccount();
		}
		
		wosAccount.setLoginUrl(p.getProperty(account+".loginUrl"));
		wosAccount.setUserName(p.getProperty(account+".user"));
		wosAccount.setPassword(p.getProperty(account+".pass"));
		
		return wosAccount.login();
	}
	
	/**
	 * 按期刊条件搜索wos文章，保存到表search_wos_by_publication
	 * @param url 登陆后获得的url
	 * @param cookies 登陆后获得的cookie
	 * @param pulicationNames  期刊名
	 * @param range 时间 4week等
	 */
	public void updateByPubAndRange(String url , String cookies , List<String> pulicationNames , String range){
		this.getLoggerTA().append("开始搜索期刊:"+pulicationNames+"\n");
		String sid = "";
		String domain = "";
		String cookieDomain = "";
		
		Pattern p;
		Matcher matcher;
		
		// 选择wos核心库，把url中的product=UA替换成product=WOS
		// http://lib-proxy.pnc.edu:2311/UA _GeneralSearch_input.do?product=UA &SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		// 转换成  --->
		// http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch_input.do?product=WOS&SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		url = url.replaceAll("product=UA", "product=WOS").replaceAll("UA _GeneralSearch_input", "WOS_GeneralSearch_input");
		
		// 获取url里的SID  e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = 	matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}
		
		// 获取url里的domain  e.g "http://lib-proxy.pnc.edu:2311"
		p = Pattern.compile("http[s]?://(.)+?/");
		matcher = p.matcher(url);
		if (matcher.find()) {
			domain = matcher.group().replaceAll("/\\z", "");
			cookieDomain = domain.replaceAll("http://","").replaceAll(":\\d+", "");
			cookieDomain = cookieDomain.substring(cookieDomain.indexOf(".")); // 把domain的第一个次级域名去掉作为cookie的domain
		}
//		System.out.println("sid: "+ sid);
//		System.out.println("domain: "+ domain);
//		System.out.println("cookieDomain: "+ cookieDomain); // 形如  ".pnc.edu"
		
		// 发送查询请求
		String searchUrl = domain + "/WOS_GeneralSearch.do"; // http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch.do
		Map<String,String> params = getRangeParameter(sid, pulicationNames,domain, range);
		Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(searchUrl, cookies ,cookieDomain, HTTPUtils.POST ,params);
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cookies = searchMap.get("cookie");
		url = searchMap.get("url");
		String htmlStr = searchMap.get("html");
		if(htmlStr == null || htmlStr.isEmpty()){
			return;
		}
		
		
		// 在页面上获取qid
		String parentQid = "1";
		p = Pattern.compile("name=\"parentQid\" value=\"\\d+\"");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			parentQid = matcher.group().replaceAll("name=\"parentQid\" value=\"", "").replaceAll("\"", "");
		}
		
		// 获取url里的SID  e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = 	matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}
		
		
		// 发送refine请求，过滤出 articel review letter 三个类型的文章
		String refineUrl = domain + "/Refine.do"; // http://lib-proxy.pnc.edu:2311/Refine.do 
		Map<String,String> refineMap = HTTPUtils.getCookieUrlAndHtml(refineUrl, cookies ,cookieDomain, HTTPUtils.POST ,getRefineParams(sid, parentQid));
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		htmlStr =  refineMap.get("html");
		
		// id="trueFinalResultCount">612</span>
		int count = 0; // 文章总数
		p = Pattern.compile("id=\"trueFinalResultCount\">(\\d)+</span>");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			String countStr = matcher.group().replaceAll("id=\"trueFinalResultCount\">", "").replaceAll("</span>", "");
			if(countStr.matches("\\d+")){
				count = Integer.valueOf(countStr);
			}
		}
		System.out.println("文章总数:"+count);
		this.loggerTA.append("文章总数:"+count+"\n");
		
		String articleUrl = ""; // 形如: /full_record.do?product=WOS&search_mode=GeneralSearch&qid=1&SID=4FAffivmBEE7pthQi3e&page=1&doc=
		p = Pattern.compile("/full_record.do?(.)+doc=");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			articleUrl = matcher.group();
			articleUrl = StringEscapeUtils.unescapeHtml4(articleUrl);
		}
		
		// 一篇一篇抓取文章,文章url = articleUrl + 序号
		// e.g  /full_record.do?product=WOS&amp;search_mode=GeneralSearch&amp;qid=1&amp;SID=4FAffivmBEE7pthQi3e&amp;page=1&amp;doc=6
		
		for(int i = 1; i <= count;i++){
		// for(int i = 2; i <= 2 ; i++){ // for test  16364
			// String aUrl = StringEscapeUtils.unescapeHtml4(articleUrl + i);
			String aUrl = domain + articleUrl + i;
			Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(aUrl, cookies ,cookieDomain , HTTPUtils.GET ,null);
			if(articleMap.get("html") != null && !articleMap.get("html").isEmpty()){
				try{
					String articleHtml = articleMap.get("html");
					WOS wos = WOSArticleParser.parseWOSFromHTML(articleHtml);
					System.out.println("title: " + wos.getTitle());
					this.loggerTA.append(wos.getTitle()+"\n");
					wosService.saveWOS(wos);
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			}
			// System.out.println("articleHtml " + articleHtml);

			try {
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("finished");
	}
	
	/* @deprecated
	public void updateWOS(){
		Organization o = new Organization();
		
		// ashland
		o.setLoginUrl("http://proxy.ashland.edu:2048/login");
		o.setUserName("1164663");
		o.setPassword("Garcia");
		
		// login
		Map<String,String> params = new HashMap<String,String>();
		// params.put("url", "");
		params.put("url", "http://www.webofknowledge.com");
		params.put("user", o.getUserName());
		params.put("pass", o.getPassword());
	
//		params.put("user.email", o.getUserName());
//		params.put("user.password", o.getPassword());
//		params.put("userIP", "116.231.109.148");
//		params.put("formerurl", "http://www.selleckchem.com/index.html");
		
		Map<String,String> loginMap = HTTPUtils.getCookieUrlAndHtml(o.getLoginUrl(), null ,null, HTTPUtils.POST ,params);
		String cookies = loginMap.get("cookie");
		String url = loginMap.get("url");
		System.out.println("final url: " + url);
		System.out.println("final cookies: " + cookies);
		// System.out.println("final html: " + loginMap.get("html"));
		
		if(loginMap.size() == 0 || !url.contains("GeneralSearch_input.do")){
			System.out.println("登陆失败");
			return;
		}
		
		String sid = "";
		String domain = "";
		String cookieDomain = "";
		
		Pattern p;
		Matcher matcher;
		
		// 选择wos核心库，把url中的product=UA替换成product=WOS
		// http://lib-proxy.pnc.edu:2311/UA _GeneralSearch_input.do?product=UA &SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		// 转换成  --->
		// http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch_input.do?product=WOS&SID=4Ff3v7CxaSyzVBaFefX&search_mode=GeneralSearch
		url = url.replaceAll("product=UA", "product=WOS").replaceAll("UA _GeneralSearch_input", "WOS_GeneralSearch_input");
		
		// 获取url里的SID  e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = 	matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}
		
		// 获取url里的domain  e.g "http://lib-proxy.pnc.edu:2311"
		p = Pattern.compile("http[s]?://(.)+?/");
		matcher = p.matcher(url);
		if (matcher.find()) {
			domain = matcher.group().replaceAll("/\\z", "");
			cookieDomain = domain.replaceAll("http://","").replaceAll(":\\d+", "");
			cookieDomain = cookieDomain.substring(cookieDomain.indexOf("."));
		}
		System.out.println("sid: "+ sid);
		System.out.println("domain: "+ domain);
		System.out.println("cookieDomain: "+ cookieDomain); // 形如  ".pnc.edu"
		
		String pulicationName = "SCIENCE";
		// 发送查询请求
		String searchUrl = domain + "/WOS_GeneralSearch.do"; // http://lib-proxy.pnc.edu:2311/WOS_GeneralSearch.do
		params = getRangeParameter(sid, pulicationName,domain, "4week");
		Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(searchUrl, cookies ,cookieDomain, HTTPUtils.POST ,params);
		
		cookies = searchMap.get("cookie");
		url = searchMap.get("url");
		String htmlStr = searchMap.get("html");
		
		// 在页面上获取qid
		String parentQid = "1";
		p = Pattern.compile("name=\"parentQid\" value=\"\\d+\"");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			parentQid = matcher.group().replaceAll("name=\"parentQid\" value=\"", "").replaceAll("\"", "");
		}
		
		// 获取url里的SID  e.g "4Ff3v7CxaSyzVBaFefX"
		p = Pattern.compile("&SID=(\\w)+&");
		matcher = p.matcher(url);
		if (matcher.find()) {
			sid = 	matcher.group().replaceAll("&SID=", "").replaceAll("&", "");
		}
		
		
		// 发送refine请求，过滤出 articel review letter 三个类型的文章
		String refineUrl = domain + "/Refine.do"; // http://lib-proxy.pnc.edu:2311/Refine.do 
		Map<String,String> refineMap = HTTPUtils.getCookieUrlAndHtml(refineUrl, cookies ,cookieDomain, HTTPUtils.POST ,getRefineParams(sid, parentQid));
		htmlStr =  refineMap.get("html");
		
		// id="trueFinalResultCount">612</span>
		int count = 0; // 文章总数
		p = Pattern.compile("id=\"trueFinalResultCount\">(\\d)+</span>");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			String countStr = matcher.group().replaceAll("id=\"trueFinalResultCount\">", "").replaceAll("</span>", "");
			if(countStr.matches("\\d+")){
				count = Integer.valueOf(countStr);
			}
		}
		System.out.println("文章总数:"+count);
		
		String articleUrl = ""; // 形如: /full_record.do?product=WOS&search_mode=GeneralSearch&qid=1&SID=4FAffivmBEE7pthQi3e&page=1&doc=
		p = Pattern.compile("/full_record.do?(.)+doc=");
		matcher = p.matcher(htmlStr);
		if (matcher.find()) {
			articleUrl = matcher.group();
			articleUrl = StringEscapeUtils.unescapeHtml4(articleUrl);
		}
		
		// 一篇一篇抓取文章,文章url = articleUrl + 序号
		// e.g  /full_record.do?product=WOS&amp;search_mode=GeneralSearch&amp;qid=1&amp;SID=4FAffivmBEE7pthQi3e&amp;page=1&amp;doc=6
		
		for(int i = 1; i <= count;i++){
		// for(int i = 2; i <= 2 ; i++){ // for test
			// String aUrl = StringEscapeUtils.unescapeHtml4(articleUrl + i);
			String aUrl = domain + articleUrl + i;
			String articleHtml = HTTPUtils.getCookieUrlAndHtml(aUrl, cookies ,cookieDomain , HTTPUtils.GET ,null).get("html");
			// System.out.println("articleHtml " + articleHtml);
			WOS wos = WOSArticleParser.parseWOSFromHTML(articleHtml);
			System.out.println("title: " + wos.getTitle());
			wosService.saveWOS(wos);
		}

		System.out.println("finished");
	}
	*/
	
	/**
	 * 
	 * @param sid url里的SID
	 * @param pNames 要查询的期刊名
	 * @param domain 主域名  http://lib-proxy.pnc.edu:2311
	 * @param startYear 起始年 2009
	 * @param endYear  结束年 2014
	 * @return
	 */
	public static Map<String, String> getYearParameter(String sid, List<String> pNames, String domain,String startYear, String endYear) {
		Map<String, String> params = getBaseHttpRequestParameter(sid, pNames, domain);
		params.put("range","ALL");
		params.put("period","Year Range");
		params.put("startYear",startYear); 
		params.put("endYear",endYear);
		return params;
	}
	
	/**
	 * 设置查询请求的post body，特别
	 * @param sid url里的SID
	 * @param pNames 要查询的期刊名
	 * @param domain 主域名  http://lib-proxy.pnc.edu:2311
	 * @param range  时间  4week  YearToDate 等
	 * @return
	 */
	public static Map<String, String> getRangeParameter(String sid, List<String> pNames, String domain,String range) {
		Map<String, String> params = getBaseHttpRequestParameter(sid, pNames, domain);
		params.put("range",range);
		params.put("period","Range Selection");
		params.put("startYear","1992");
		params.put("endYear","2014");
		return params;
	}
	
	
	/**
	 * 设置查询请求的post body，包含大多数通用参数。
	 * @param sid url里的SID
	 * @param pNames 要查询的期刊名
	 * @param domain  主域名  http://lib-proxy.pnc.edu:2311
	 * @return
	 */
	private static Map<String, String> getBaseHttpRequestParameter(String sid, List<String> pNames,String domain) {
		Map<String, String> reqParam = new IdentityHashMap<String, String>(); 
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
//		String sysEndDate = sdf.format(new Date()); 
//		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy"); 
//		String year = sdf2.format(new Date());
		
		reqParam.put("fieldCount",String.valueOf(pNames.size())); 
		reqParam.put("action","search"); 
		reqParam.put("product","WOS");
		reqParam.put("search_mode","GeneralSearch"); 
		reqParam.put("SID", sid);  // SID in url
		reqParam.put("max_field_count","25");
		
		/* not urlDecoded yet
		reqParam.put("max_field_notice","%E6%B3%A8%E6%84%8F%3A+%E6%97%A0%E6%B3%95%E6%B7%BB%E5%8A%A0%E5%8F%A6%E4%B8%80%E5%AD%97%E6%AE%B5%E3%80%82");  // unknown param
		reqParam.put("input_invalid_notice","%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E8%AF%B7%E8%BE%93%E5%85%A5%E6%A3%80%E7%B4%A2%E8%AF%8D%E3%80%82");
		reqParam.put("exp_notice","%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A+%E4%B8%93%E5%88%A9%E6%A3%80%E7%B4%A2%E8%AF%8D%E5%8F%AF%E5%9C%A8%E5%A4%9A%E4%B8%AA%E5%AE%B6%E6%97%8F%E4%B8%AD%E6%89%BE%E5%88%B0+%28");  // unknown param
		reqParam.put("input_invalid_notice_limits","+%3Cbr%2F%3E%E6%B3%A8%3A+%E6%BB%9A%E5%8A%A8%E6%A1%86%E4%B8%AD%E6%98%BE%E7%A4%BA%E7%9A%84%E5%AD%97%E6%AE%B5%E5%BF%85%E9%A1%BB%E8%87%B3%E5%B0%91%E4%B8%8E%E4%B8%80%E4%B8%AA%E5%85%B6%E4%BB%96%E6%A3%80%E7%B4%A2%E5%AD%97%E6%AE%B5%E7%9B%B8%E7%BB%84%E9%85%8D%E3%80%82");  // unknown param
		reqParam.put("sa_params","WOS%7C%7C1ByB9OWWlCEAOeYCKIn%7Chttp%3A%2F%2Flib-proxy.pnc.edu%3A2311%7C%27&"); 
		*/
		
		// urlDecoded
		reqParam.put("max_field_notice","注意: 无法添加另一字段。");  // unknown param
		reqParam.put("input_invalid_notice","请输入检索词。"); // unknown param
		reqParam.put("exp_notice","检索错误: 专利检索词可在多个家族中找到 (");  // unknown param
		reqParam.put("input_invalid_notice_limits","滚动框中显示的字段必须至少与一个其他检索字段相组配。");  // unknown param
		reqParam.put("sa_params","WOS||" + sid + "|" + domain + "|'");
		
		reqParam.put("formUpdated","true");
		
		reqParam.put("value(input1)",pNames.get(0)); 
		reqParam.put("value(select1)","TS"); // may be "主题" select
		// reqParam.put("value(select1)","TI"); // may be "标题" select
		reqParam.put("value(hidInput1)","");
		// reqParam.put("value(bool_1_2)","OR");
		for(int i = 1 ; i < pNames.size(); i++){
			reqParam.put("value(bool_"+i+"_"+(i+1)+")","OR");
			reqParam.put("value(input"+(i+1)+")",pNames.get(i)); 
			reqParam.put("value(select"+(i+1)+")","TS");
			reqParam.put("value(hidInput"+(i+1)+")","");
		}
		
//		reqParam.put("x","77");
//		reqParam.put("y","18");
		
		reqParam.put("limitStatus","expanded"); 
		reqParam.put("ss_lemmatization","On");
		reqParam.put("ss_spellchecking","Suggest"); 
		reqParam.put("SinceLastVisit_UTC",""); 
		reqParam.put("SinceLastVisit_DATE",""); 
//		reqParam.put("range","ALL");  // ALL or  4week  ，在getRangeParameter或getYearParameter中指定
//		reqParam.put("period","Year Range"); // Year Range  or   Range+Selection ， 在getRangeParameter或getYearParameter中指定
//		reqParam.put("startYear",startYear); // 在getYearParameter中指定
// 		reqParam.put("endYear",endYear); // 在getYearParameter中指定
		reqParam.put("editions","SCI");
		reqParam.put("ssStatus","display:none");
		reqParam.put("ss_showsuggestions","ON");
		reqParam.put("ss_numDefaultGeneralSearchFields","1");
		reqParam.put("ss_query_language","");
		reqParam.put("rs_sort_by","PY.D;LD.D;SO.A;VL.D;PG.A;AU.A");
		
		// 尝试是否能直接发送refine分类， but failed
//		reqParam.put("refineSelection","DocumentType_ARTICLE");
//		reqParam.put("refineSelection","DocumentType_REVIEW");
//		reqParam.put("refineSelection","DocumentType_LETTER");
		
		return reqParam;
		
		/* email_capture里的代码，做参考
		reqParam.put("SID",this.sessionID);
		reqParam.put("SinceLastVisit_UTC",""); 
		reqParam.put("SinceLastVisit_DATE",""); 
		reqParam.put("action","search"); 
		reqParam.put("collapse_alt","Collapse these settings"); 
		reqParam.put("collapse_title","Collapse these settings"); 
		reqParam.put("defaultCollapsedListStatus","display: none"); 
		reqParam.put("defaultEditionsStatus","display: block"); 
		reqParam.put("editions","SCI"); 
		reqParam.put("endDate",	sysEndDate);
		reqParam.put("endYear", year);
		reqParam.put("expand_alt", "Expand these settings");
		reqParam.put("expand_title", "Expand these settings");
		reqParam.put("extraCollapsedListStatus", "display: inline");
		reqParam.put("extraEditionsStatus", "display: none");
		reqParam.put("fieldCount", "3");
		reqParam.put("input_invalid_notice", "Search Error: Please enter a search term");
		reqParam.put("input_invalid_notice_limits", "<br/>Note: Fields displayed in scrolling boxes must be combined with at least one other search field");
		reqParam.put("limitStatus", "collapsed");
		reqParam.put("max_field_count", "25");
		reqParam.put("max_field_notice", "Notice: You cannot add another field");
		reqParam.put("period", "Range Selection");
		reqParam.put("product", "WOS");
		reqParam.put("range", "ALL");
		reqParam.put("rsStatus", "display:none");
		reqParam.put("rs_linksWindows", "newWindow");
		reqParam.put("rs_rec_per_page", "10");
		reqParam.put("rs_refinePanel", "visibility:show");
		reqParam.put("rs_sort_by", "PY.D;LD.D;SO.A;VL.D;PG.A;AU.A");
		reqParam.put("sa_img_alt", "Select terms from the index");
		reqParam.put("sa_params",
				"WOS|http://0-apps.webofknowledge.com.brum.beds.ac.uk/InboundService.do%3Fproduct%3DWOS%26search_mode%3DGeneralSearch%26mode%3DGeneralSearch%26action%3Dtransfer%26viewType%3Dinput%26SID%3D"
				+this.sessionID+"%26inputbox%3Dinput???|"+this.sessionID+
				"|http://apps.webofknowledge.com|[name=au;value=au;keyname=;type=termlist;priority=10, name=GP;value=GP;keyName=;type=termlist;priority=10, name=SO;value=SO;keyName=;type=termlist;priority=10, name=OG;value=OG;keyName=;type=searchAid;priority=10]'"
				);
		reqParam.put("search_mode", "GeneralSearch");
		reqParam.put("ss_lemmatization", "On");
		reqParam.put("ss_query_language", "");
		reqParam.put("startDate", "2011-01-01");
		reqParam.put("startYear", "1975");
		reqParam.put("timeIndex", "LoadDate");
		reqParam.put("timeSpanCollapsedListStatus", "display: none");
		reqParam.put("timespanStatus", "display: block");
		reqParam.put("value(bool_1_2)", input.getBool12());
		reqParam.put("value(bool_2_3)", input.getBool23());
		reqParam.put("value(hidInput1)", "");
		reqParam.put("value(hidInput2)", "AU");
		reqParam.put("value(hidInput3)", "SO");
		reqParam.put("value(hidShowIcon1)", "0");
		reqParam.put("value(hidShowIcon2)", "1");
		reqParam.put("value(hidShowIcon3)", "1");
		reqParam.put("value(input1)", input.getKeyword1());
		reqParam.put("value(input2)", input.getKeyword2());
		reqParam.put("value(input3)", input.getKeyword3());
		reqParam.put("value(select1)", input.getSearchRange1());
		reqParam.put("value(select2)", input.getSearchRange2());
		reqParam.put("value(select3)", input.getSearchRange3());
		reqParam.put("x", "43");
		reqParam.put("y", "16");
		
		*/
		
		/* SEARCH BODY
	 	fieldCount=1&
		action=search&
		product=WOS&
		search_mode=GeneralSearch&
		SID=4Ff3v7CxaSyzVBaFefX&
		max_field_count=25&
		max_field_notice=%E6%B3%A8%E6%84%8F%3A%20%E6%97%A0%E6%B3%95%E6%B7%BB%E5%8A%A0%E5%8F%A6%E4%B8%80%E5%AD%97%E6%AE%B5%E3%80%82&
		input_invalid_notice=%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A%20%E8%AF%B7%E8%BE%93%E5%85%A5%E6%A3%80%E7%B4%A2%E8%AF%8D%E3%80%82&
		exp_notice=%E6%A3%80%E7%B4%A2%E9%94%99%E8%AF%AF%3A%20%E4%B8%93%E5%88%A9%E6%A3%80%E7%B4%A2%E8%AF%8D%E5%8F%AF%E5%9C%A8%E5%A4%9A%E4%B8%AA%E5%AE%B6%E6%97%8F%E4%B8%AD%E6%89%BE%E5%88%B0%20(&
		input_invalid_notice_limits=%20%3Cbr%2F%3E%E6%B3%A8%3A%20%E6%BB%9A%E5%8A%A8%E6%A1%86%E4%B8%AD%E6%98%BE%E7%A4%BA%E7%9A%84%E5%AD%97%E6%AE%B5%E5%BF%85%E9%A1%BB%E8%87%B3%E5%B0%91%E4%B8%8E%E4%B8%80%E4%B8%AA%E5%85%B6%E4%BB%96%E6%A3%80%E7%B4%A2%E5%AD%97%E6%AE%B5%E7%9B%B8%E7%BB%84%E9%85%8D%E3%80%82&
		sa_params=WOS%7C%7C4Ff3v7CxaSyzVBaFefX%7Chttp%3A%2F%2Flib-proxy.pnc.edu%3A2311%7C'&
		value(input1)=cell%20stem%20cell&
		value(select1)=SO&
		value(hidInput1)=&
		limitStatus=expanded&
		ss_lemmatization=On&
		ss_spellchecking=Suggest&
		SinceLastVisit_UTC=&
		SinceLastVisit_DATE=&
		range=ALL&
		period=Year%20Range&
		startYear=2009&
		endYear=2014&
		editions=SCI&
		ssStatus=display%3Anone&
		ss_showsuggestions=ON&
		ss_query_language=&
	 */
	}
	
	private static Map<String, String> getRefineParams(String sid , String parentQid){
		Map<String, String> reqParam = new IdentityHashMap<String, String>(); 
		reqParam.put("parentQid",parentQid); 
		reqParam.put("SID",sid); 
		reqParam.put("product","WOS"); 
		reqParam.put("databaseId","WOS"); 
		reqParam.put("colName","WOS");
		reqParam.put("search_mode","Refine"); 
		reqParam.put("queryOption(summary_search_mode)","GeneralSearch"); 
		reqParam.put("action","search"); 
		reqParam.put("clickRaMore","如果继续使用\"更多...\"功能，则屏幕中的精炼选择将不会保存。"); 
		reqParam.put("openCheckboxes","如果隐藏左侧面板，则其中的精炼选择将不会保存。"); 
		reqParam.put("refineSelectAtLeastOneCheckbox","请至少选中一个复选框来精炼检索结果。"); 
		reqParam.put("queryOption(sortBy)","PY.D;LD.D;SO.A;VL.D;PG.A;AU.A"); 
		reqParam.put("queryOption(ss_query_language)","auto"); 
		reqParam.put("sws",""); 
		reqParam.put("defaultsws","在如下结果集内检索...");
		reqParam.put("swsFields","TS"); 
		reqParam.put("swsHidden","在前 100,000 条结果内<br>检索");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put(new String("refineSelection"),"DocumentType_ARTICLE"); // IdentityHashMap判断key1==key2?，所以要写成两个不同对象形式
		reqParam.put(new String("refineSelection"),"DocumentType_REVIEW");
		// reqParam.put(new String("refineSelection"),"DocumentType_LETTER"); // 只要 article 和 review
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("exclude","");
		reqParam.put("mode","refine");
		
		return reqParam;
	
	/*
	 	parentQid=4&
		SID=4Ff3v7CxaSyzVBaFefX&
		product=WOS&
		databaseId=WOS&
		colName=WOS&
		search_mode=Refine&
		queryOption%28summary_search_mode%29=GeneralSearch&
		action=search&
		clickRaMore=%E5%A6%82%E6%9E%9C%E7%BB%A7%E7%BB%AD%E4%BD%BF%E7%94%A8%22%E6%9B%B4%E5%A4%9A...%22%E5%8A%9F%E8%83%BD%EF%BC%8C%E5%88%99%E5%B1%8F%E5%B9%95%E4%B8%AD%E7%9A%84%E7%B2%BE%E7%82%BC%E9%80%89%E6%8B%A9%E5%B0%86%E4%B8%8D%E4%BC%9A%E4%BF%9D%E5%AD%98%E3%80%82&
		openCheckboxes=%E5%A6%82%E6%9E%9C%E9%9A%90%E8%97%8F%E5%B7%A6%E4%BE%A7%E9%9D%A2%E6%9D%BF%EF%BC%8C%E5%88%99%E5%85%B6%E4%B8%AD%E7%9A%84%E7%B2%BE%E7%82%BC%E9%80%89%E6%8B%A9%E5%B0%86%E4%B8%8D%E4%BC%9A%E4%BF%9D%E5%AD%98%E3%80%82&
		refineSelectAtLeastOneCheckbox=%E8%AF%B7%E8%87%B3%E5%B0%91%E9%80%89%E4%B8%AD%E4%B8%80%E4%B8%AA%E5%A4%8D%E9%80%89%E6%A1%86%E6%9D%A5%E7%B2%BE%E7%82%BC%E6%A3%80%E7%B4%A2%E7%BB%93%E6%9E%9C%E3%80%82&
		queryOption%28sortBy%29=PY.D%3BLD.D%3BSO.A%3BVL.D%3BPG.A%3BAU.A&
		queryOption%28ss_query_language%29=auto&
		sws=&
		defaultsws=%E5%9C%A8%E5%A6%82%E4%B8%8B%E7%BB%93%E6%9E%9C%E9%9B%86%E5%86%85%E6%A3%80%E7%B4%A2...&
		swsFields=TS&
		swsHidden=%E5%9C%A8%E5%89%8D+100%2C000+%E6%9D%A1%E7%BB%93%E6%9E%9C%E5%86%85%3Cbr%3E%E6%A3%80%E7%B4%A2&
		exclude=&
		exclude=&
		refineSelection=DocumentType_ARTICLE&
		refineSelection=DocumentType_REVIEW&
		refineSelection=DocumentType_LETTER&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		exclude=&
		mode=refine
	 */
	}

	public boolean isStartFlag() {
		return startFlag;
	}

	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}

	public JTextArea4Log getLoggerTA() {
		return loggerTA;
	}

	public void setLoggerTA(JTextArea4Log loggerTA) {
		this.loggerTA = loggerTA;
	}
	
	
}
