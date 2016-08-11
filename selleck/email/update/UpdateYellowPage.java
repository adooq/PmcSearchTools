package selleck.email.update;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPage;
import selleck.email.service.IYellowPageService;
import selleck.email.service.impl.YellowPageServiceImpl;
import selleck.email.update.frame.UpdateYellowPageFrame;
import selleck.email.update.parser.YellowPageArticleParser;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdateYellowPage {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
//	public static final String START_ENTREZ = "2009/01/01";
//	public static final String END_ENTREZ = "2014/06/30";
//	public static final String DOWNLOAD_DIR = "";
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	IYellowPageService yellowPageService = new YellowPageServiceImpl();
	// public static Set<String> BusinessDetailSet = new HashSet<String>();
	// [Social Links, Services/Products, Neighborhoods, Hours, Payment method, General Info, Location, Price Range, Extra Phones, Brands, Categories, Languages, Accreditation, Other Links, Category, Neighborhood, AKA, Amenities, Associations, Other Link, Other Email]
	// 单数 Category, Neighborhood, Other Link
	public static void main(String[] args) {
		final UpdateYellowPageFrame frame = UpdateYellowPageFrame.getFrame();
		final UpdateYellowPage updateSpringer = new UpdateYellowPage();
		updateSpringer.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updateSpringer.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updateSpringer.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updateSpringer.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updateSpringer.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("自动更新YellowPage")) {
						INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
						String excelName = frame.getExcelText().getText();
//						String keyword = frame.getPubLabel().getText();
//						String location = frame.getCookieText().getText();
						String pubRange = frame.getPubLabel().getText();
						updateSpringer.loadPubFromExcel(excelName,pubRange);
					}
					updateSpringer.setStartFlag(false);
					frame.stopMouseClicked();
					updateSpringer.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	/**
	 * 读取期刊的excel，根据期刊里的关键词，一个个去Yellow Page搜索。
	 * @param excel 纯excel文件名，不需要路径， 形如：yellowPage.xlsx
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	public void loadPubFromExcel(String excel , String pubRange){
		Workbook wb = null;
		// HMARobot robot = new HMARobot(); // 暂不考虑换IP
		try {
			wb = WorkbookFactory.create(new File(excel));
			Sheet sheet = wb.getSheet("Sheet1");
			int rowCount = sheet.getLastRowNum(); // row从0开始
			int stateStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
			int stateEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
			stateEnd = Math.min(stateEnd, STATES.length-1);
			for (int i = stateStart; i <= stateEnd; i++) { 
				try {
					String state = STATES[i];
					for(int j = 0;j <= rowCount; j++){
						Row row = sheet.getRow(j); // row从0开始
						String keword = row.getCell(0).getStringCellValue().trim(); // 期刊名
						this.getArticleUrl(keword,state); // 先搜索文章的url ， 存入库中
						this.queryArticles(keword,state); // 按库中搜到的文章url更新文章的内容
					}
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
	 * @param keyword
	 * @param near
	 */
	public void getArticleUrl(String keyword , String near) {
		loggerTA.append("开始查询"+keyword+" in "+near+"\n");
		String keywordTerm = keyword;
		String locationTerm = near;
		String resultCount = "0";
		
		
		try {
			keywordTerm = URLEncoder.encode(keywordTerm,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// search GET
		// http://www.yellowpages.com/search?search_terms=veterinary%20compounding%20pharmacy&geo_location_terms=TX&page=2&s=relevance
		String url = "http://www.yellowpages.com/search?search_terms="+keywordTerm+"&geo_location_terms="+locationTerm+"&page=1";
		Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(url, null ,null, HTTPUtils.GET ,null);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(searchMap.size() == 0){
			loggerTA.append("GET 查询失败\n");
			System.out.println("GET 查询失败");
			changeIP();
			return;
		}

		String html = searchMap.get("html");
		
		// 获取总页数
		String countRegex = "\\d+<span>results</span>";
		Pattern p = Pattern.compile(countRegex);
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countStr = matcher.group();
			resultCount = countStr.replace("<span>results</span>", "").trim();
		}
		if(resultCount.equals("0")){
			return;
		}
		
		int totalPage = ((Double)Math.ceil(Double.valueOf(resultCount)/30)).intValue(); // 每页30个
		System.out.println("总页数 "+totalPage);
		loggerTA.append("总页数 "+totalPage+"\n");
		
		// 抓取第一页上的页面链接
		// <a href="/weslaco-tx/mip/border-animal-hospital-pc-3475718?lid=3475718" data-analytics='{"target":"name","feature_click":""}'
		String articleRegex = "<a href=\"\\p{Graph}+?\" data-analytics='\\{\"target\":\"name\",\"feature_click\":\"\"\\}'";
		p = Pattern.compile(articleRegex);
		matcher = p.matcher(html);
		while(matcher.find()){
			String articleUrl = matcher.group();
			articleUrl = articleUrl.replaceAll("<a href=\"", "").replaceAll("\" data-analytics='\\{\"target\":\"name\",\"feature_click\":\"\"\\}'", "");
			articleUrl = "http://www.yellowpages.com" + articleUrl;
			YellowPage yp = new YellowPage();
			yp.setUrl(articleUrl);
			yp.setKeyword(keyword);
			yp.setNear(near);
			yp.setCountry("USA");
			try{
				yellowPageService.saveYellowPage(yp);
				this.loggerTA.append(yp.getUrl()+"\n");
			}catch(Exception ee){
				ee.printStackTrace();
				continue;
			}
		}
		
		// loop page
		// http://www.yellowpages.com/search?search_terms=veterinary%20compounding%20pharmacy&geo_location_terms=TX&page=2&s=relevance
		String pageHtml = "";
		for(int i = 2;i <= totalPage;i++){
		// for(int i = 2;i <= Math.min(totalPage, 50);i++){ // for test
			try{
				String pageUrl = "http://www.yellowpages.com/search?search_terms="+keywordTerm+"&geo_location_terms="+locationTerm+"&page="+i;
				Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(pageUrl, null ,null, HTTPUtils.GET ,null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if(pageMap.size() == 0){
					loggerTA.append("GET 查询失败\n");
					System.out.println("GET 查询失败");
					changeIP();
					continue;
				}
				
				pageHtml = pageMap.get("html");
				p = Pattern.compile(articleRegex);
				matcher = p.matcher(pageHtml);
				while(matcher.find()){
					String articleUrl = matcher.group();
					articleUrl = "http://www.yellowpages.com" + articleUrl;
					articleUrl = articleUrl.replaceAll("<a href=\"", "").replaceAll("\" data-analytics='\\{\"target\":\"name\",\"feature_click\":\"\"\\}'", "");
					YellowPage yp = new YellowPage();
					yp.setUrl(articleUrl);
					yp.setKeyword(keyword);
					yp.setNear(near);
					yp.setCountry("USA");
					try{
						yellowPageService.saveYellowPage(yp);
						this.loggerTA.append(yp.getUrl()+"\n");
					}catch(Exception ee){
						ee.printStackTrace();
						continue;
					}
				}
			}catch(Exception ee){
				ee.printStackTrace();
				continue;
			}
		}
		
		
	}
	
	/**
	 * 按文章的url去访问文章网页并抓取内容
	 * @param keyword 查询的关键字
	 * @param near 查询的地区
	 */
	public void queryArticles(String keyword , String near){
		Criteria criteria = new Criteria();
		criteria.setWhereClause(" (title is null or title = '') and keyword = '"+StringUtils.toSqlForm(keyword)+"' and near = '"+StringUtils.toSqlForm(near)+"'");
		// criteria.setWhereClause(" (title is null or title = '')");
		List<YellowPage> yellowPages = yellowPageService.selectByExample(criteria);
		this.loggerTA.append("关键字"+keyword+" in "+near+"总共有"+yellowPages.size()+"个页面需要抓取\n");
		for(YellowPage yp : yellowPages){
			try{
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(yp.getUrl(), null ,null, HTTPUtils.GET , null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String articleHtml = articleMap.get("html");
				if(articleHtml == null || articleHtml.isEmpty()){
					loggerTA.append("GET 查询失败\n");
					System.out.println("GET 查询失败");
					changeIP();
					continue;
				}
				
				yp = YellowPageArticleParser.parseFromHTML(articleHtml,yp);
				loggerTA.append(yp.getTitle()+"\n");
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					yellowPageService.updateYellowPage(yp);
				}catch(Exception ee){
					ee.printStackTrace();
					continue;
				}
			}
			
		}
	}
	
	// 美国州名缩写
	private static final String[] STATES = {"AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"};


	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	private void changeIP(){
		HMARobot hma = new HMARobot();
		hma.changeIP(loggerTA);
	}
	
	
}
