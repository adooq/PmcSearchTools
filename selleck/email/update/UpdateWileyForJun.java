package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import common.handle.model.Criteria;
import selleck.email.pojo.Wiley;
import selleck.email.service.IWileyService;
import selleck.email.service.impl.WileyServiceImpl;
import selleck.email.update.frame.UpdateWileyFrame;
import selleck.email.update.parser.WileyArticleParser;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.Constants;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdateWileyForJun {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
//	public static final String START_ENTREZ = "2009/01/01";
//	public static final String END_ENTREZ = "2014/06/30";
//	public static final String DOWNLOAD_DIR = "";
	public static final String DOMAIN = "http://onlinelibrary.wiley.com";
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	IWileyService wileyService;
	
	public static void main(String[] args) {
		final UpdateWileyFrame frame = UpdateWileyFrame.getFrame();
		final UpdateWileyForJun updateWiley = new UpdateWileyForJun();
		updateWiley.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updateWiley.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updateWiley.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updateWiley.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updateWiley.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("自动更新Wiley文章")) {
						INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
						String excelName = frame.getExcelText().getText();
						String pubRange = frame.getPubLabel().getText();
						String dateRangeSelect = frame.getRangeSelect().getSelectedItem().toString();
						String dateRange = "";
						if(dateRangeSelect.equals("Between")){ // 年份段  YYYY-YYYY
							dateRange = frame.getPublicationText().getText(); // 把publicationText当做range year用
						}else{ // last x month(s)
							dateRange = dateRangeSelect; // dateRangeSelect  1 Month  3 Months ....
						}
						String dbName = frame.getDbSelect().getSelectedItem().toString();
						updateWiley.wileyService = new WileyServiceImpl(Constants.JUN);
						updateWiley.loadPubFromExcel(excelName,dateRange,pubRange);
						// updatePMC.loadPubFromCSV(frame.getExcelText().getText(),frame.getPublicationText().getText(),frame.getCookieText().getText());
					}
					updateWiley.setStartFlag(false);
					frame.stopMouseClicked();
					updateWiley.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	
	/**
	 * 读取期刊的excel，根据期刊名一个个去wiley搜索文章。
	 * @param excel 纯excel文件名，不需要路径， 形如： wiley.xlsx
	 * @param dateRange  时间段，可以是YYYY-YYYY ，也可以是1 Month , 3 Months ....
	 * @param endEntrez   入库时间  2014
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	public void loadPubFromExcel(String excel ,String dateRange , String pubRange){
		Workbook wb = null;
		// HMARobot robot = new HMARobot();
		try {
			wb = WorkbookFactory.create(new File(excel));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			int pubStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
			int pubEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
			pubEnd = Math.min(pubEnd, rowCount);
			// for (int i = 0; i <= 0; i++) { // for test
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
			List<String> pubNames = new ArrayList<String>(pubEnd);
			for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
				Row row = sheet.getRow(i);
				String pubName = row.getCell(0).getStringCellValue().trim(); // 期刊名
				pubNames.add(pubName);
			}
			
			for (String pubName : pubNames) {
				try {
					this.getArticleUrl(pubName,dateRange); // 先搜索文章的url ， 存入库中
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
	 * @param keyword
	 * @param dateRange  时间段，可以是YYYY-YYYY ，也可以是1 Month , 3 Months ....
	 */
	public void getArticleUrl(String keyword , String dateRange) {
		loggerTA.append("开始查询关键词"+keyword+" "+dateRange+"\n");
		String resultCount = "";
		
		// search GET
		String url = "http://onlinelibrary.wiley.com/advanced/search";
		
		/* @deprecated
		// wiley 不允许查询关键词带有特殊字符和某个单词只有一个字母 
		journal = journal.replaceAll("\\p{Punct}", " ").replaceAll(" \\w ", " ");
		*/
		
//		try {
//			journal = java.net.URLEncoder.encode("\""+journal+"\"","utf-8");
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		
		Map<String, String> reqParam = getBaseHttpRequestParameter();
		reqParam.put("searchRowCriteria[0].queryString","\""+keyword+"\"");
		if(dateRange.contains("-")){ // dateRange = YYYY-YYYY
			reqParam.put("inTheLastList","6"); // 随意一个默认值
			reqParam.put("dateRange","between");
			reqParam.put("startYear",dateRange.split("-")[0]);
			reqParam.put("endYear",dateRange.split("-")[1]);
		}else{ // dateRange = 1 Month , 3 Months ....	
			reqParam.put("inTheLastList",dateRange.split(" ")[0]);
			reqParam.put("dateRange","inTheLast");
			reqParam.put("startYear","");
			reqParam.put("endYear","");
		}
		Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(url, null ,null, HTTPUtils.POST ,reqParam);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(searchMap.size() == 0){
			loggerTA.append("查询失败\n");
			System.out.println("查询失败");
			return;
		}

		String html = searchMap.get("html");
		
		// 获取文章总数
		Pattern p = Pattern.compile("<em>\\d+</em> results for:");
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countStr = matcher.group();
			resultCount = countStr.replace(" results for:", "").replace("<em>", "").replace("</em>", "").trim();
		}else{
			resultCount = "0";
		}
		
		// loop page
		// http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&dateRange=inTheLast&inTheLastList=6&startYear=2010&endYear=2014&queryStringEntered=false&searchRowCriteria[0].queryString=cell&searchRowCriteria[0].fieldName=document-title&searchRowCriteria[0].booleanConnector=and&searchRowCriteria[1].fieldName=all-fields&searchRowCriteria[1].booleanConnector=and&searchRowCriteria[2].fieldName=all-fields&searchRowCriteria[2].booleanConnector=and&start=21&ordering=relevancy
		// http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&dateRange=inTheLast&inTheLastList=6&startYear=2010&endYear=2014&queryStringEntered=false&searchRowCriteria[0].queryString=cell&searchRowCriteria[0].fieldName=document-title&searchRowCriteria[0].booleanConnector=and&searchRowCriteria[1].fieldName=all-fields&searchRowCriteria[1].booleanConnector=and&searchRowCriteria[2].fieldName=all-fields&searchRowCriteria[2].booleanConnector=and&start=81&ordering=relevancy
		// http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&dateRange=inTheLast&inTheLastList=6&startYear=      &endYear=       &queryStringEntered=false&searchRowCriteria[0].queryString=cell&searchRowCriteria[0].fieldName=publication-title&searchRowCriteria[0].booleanConnector=and&searchRowCriteria[1].fieldName=all-fields&searchRowCriteria[1].booleanConnector=and&searchRowCriteria[2].fieldName=all-fields&searchRowCriteria[2].booleanConnector=and&start=41&ordering=relevancy
		// http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&dateRange=inTheLast&inTheLastList=6&startYear=      &endYear=       &queryStringEntered=false&searchRowCriteria[0].queryString=cell&searchRowCriteria[0].fieldName=publication-title&searchRowCriteria[0].booleanConnector=and&searchRowCriteria[1].fieldName=all-fields&searchRowCriteria[1].booleanConnector=and&searchRowCriteria[2].fieldName=all-fields&searchRowCriteria[2].booleanConnector=and&start=61&ordering=relevancy
		// http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&dateRange=inTheLast&inTheLastList=1&startYear=&endYear=&queryStringEntered=false&searchRowCriteria[0].queryString=stem+cell&searchRowCriteria[0].fieldName=publication-title&searchRowCriteria[0].booleanConnector=and&searchRowCriteria[1].fieldName=all-fields&searchRowCriteria[1].booleanConnector=and&searchRowCriteria[2].fieldName=all-fields&searchRowCriteria[2].booleanConnector=and&start=41&ordering=relevancy
		String pageUrl ="http://onlinelibrary.wiley.com/advanced/search/results/reentry?scope=allContent&queryStringEntered=false&ordering=relevancy&start=1";
		
		for(Iterator<Entry<String,String>> iter = reqParam.entrySet().iterator();iter.hasNext();){
			Entry<String,String> param = iter.next();
			try {
				pageUrl += "&"+param.getKey()+"="+java.net.URLEncoder.encode(param.getValue(), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		int totalPage = ((Double)Math.ceil(Double.valueOf(resultCount)/20)).intValue();
		System.out.println("总页数 "+totalPage);
		loggerTA.append("总页数 "+totalPage+"\n");
		String pageHtml = "";
		for(int i = 1;i <= totalPage;i++){
		// for(int i = 1;i <= Math.min(totalPage,3);i++){ // for test
			try{
				pageUrl = pageUrl.replaceAll("&start=\\d+", "&start="+((i-1)*20+1));
				Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(pageUrl, null ,null, HTTPUtils.GET ,null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				pageHtml = pageMap.get("html");
				// <div class="citation article"><a href="/doi/10.1002/9781118647363.ch1/summary"
				List<String> articleUrlDivs= ParserUtils.findWithPrefixAndSuffix("<div class=\"citation .*?\">" , "</a>" , pageHtml);
				for(String articleUrlDiv : articleUrlDivs){
					String articleTitle = articleUrlDiv.substring(articleUrlDiv.lastIndexOf(">") + 1);
					// 期刊里有很多非正式学术的文章，比如勘误表、申明等，不记录抓取
					if(articleTitle.startsWith("Corrigendum") || articleTitle.startsWith("Corrigenda")
							||  articleTitle.startsWith("Errata") || articleTitle.startsWith("Erratum") || articleTitle.startsWith("Preface")
							|| articleTitle.startsWith("Announcement") || articleTitle.startsWith("Table of Contents") || articleTitle.startsWith("Content")
							|| articleTitle.startsWith("Editorial Board") || articleTitle.startsWith("Cover ") || articleTitle.startsWith("Conference Calendar")
							|| articleTitle.startsWith("Correction") || articleTitle.startsWith("Issue Information") || articleTitle.startsWith("Index")
							|| articleTitle.startsWith("Reply") || articleTitle.startsWith("Sponsors")
							|| articleTitle.matches("\\A.{0,10}Abstract.*") || articleTitle.matches("\\A.{0,10}Index.*") || articleTitle.matches("\\A.{0,10}issue.*")
							|| articleTitle.matches("\\A.{0,10}Snippet.*") ||articleTitle.startsWith("Inside Cover") || articleTitle.startsWith("Back Cover")
					){
						continue;
					}
					
					p = Pattern.compile("/doi/[\\d.]+/[\\p{Alnum}.\\-_]+/\\p{Alpha}+");
					matcher = p.matcher(articleUrlDiv);
					while(matcher.find()){
						String articleUrl = matcher.group();
						// http://onlinelibrary.wiley.com/doi/10.1002/9781118647363.ch8/summary
						
						/* 理论上在这里应该加入判断这篇文章的期刊是否在期刊列表中（因为Wiley会查到包含要搜索的期刊名的其他期刊），
							但是由于预先获得的期刊列表可能不全，所以也就把包含要搜索的期刊名的其他期刊的文章也抓下来。
							to do ...
						*/

						Wiley wiley = new Wiley();
						wiley.setType("Article");
						wiley.setJournal(keyword);
						wiley.setUrl(DOMAIN + articleUrl);
						
						try{
							wileyService.saveWiley(wiley);
							this.loggerTA.append(wiley.getUrl()+"\n");
						}catch(Exception ee){
							ee.printStackTrace();
							continue;
						}
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
	 * @param pubName excel中的期刊名。
	 */
	public void queryArticles(String pubName){
		Criteria criteria = new Criteria();
		criteria.setWhereClause(" (title is null or title = '') and journal = '"+StringUtils.toSqlForm(pubName)+"' ");
		List<Wiley> wileys = wileyService.selectFromSearchPublication(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+wileys.size()+"篇文章需要抓取\n");
		WileyArticleParser wileyParser = new WileyArticleParser();
		for(Wiley wiley : wileys){
			try{
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(wiley.getUrl(), null ,null, HTTPUtils.GET , null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String articleHtml = articleMap.get("html");
				if(articleHtml == null || articleHtml.isEmpty()){
					continue;
				}
				
				wiley = wileyParser.parseFromHTML(articleHtml , wiley);
				
				// <span class="freeAccess" title="You have free access to this content">You have free access to this content</span>
				List<String> freeSpans= ParserUtils.findInContent("<span class=\"freeAccess\" title=\"You have free access to this content\">", articleHtml);
				if(freeSpans.size() > 0){ // 说明是免费文章，有全文
					//  /doi/10.1002/9781118647363.ch8/full
					List<String> fullTextUrls = ParserUtils.findInContent(wiley.getUrl().substring(0, wiley.getUrl().lastIndexOf("/")) + "/full", articleHtml);
					if(fullTextUrls.size() > 0){
						wiley.setFullTextUrl(fullTextUrls.get(0));
						Map<String,String> fullArticleMap = HTTPUtils.getCookieUrlAndHtml(wiley.getFullTextUrl(), null ,null, HTTPUtils.GET , null);
						try {
							Thread.sleep(INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						String fullArticleHtml = fullArticleMap.get("html");
						if(fullArticleHtml != null && !fullArticleHtml.isEmpty()){
							wiley = wileyParser.parseFullTextFromHTML(fullArticleHtml , wiley);
						}
					}
				}
				
				loggerTA.append(wiley.getTitle()+"\n");
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					wileyService.updateWiley(wiley);
				}catch(Exception ee){
					ee.printStackTrace();
					continue;
				}
			}
			
		}
		
		// 补漏文章信息抓到但全文没抓到的情况
		criteria.setWhereClause(" (fulltext_url is not null and fulltext_url != '') and (full_text is null or full_text = '')  and journal = '"+StringUtils.toSqlForm(pubName)+"' ");
		wileys = wileyService.selectFromSearchPublication(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+wileys.size()+"篇文章需要抓取全文\n");
		for(Wiley wiley : wileys){
			try{
				Map<String,String> fullArticleMap = HTTPUtils.getCookieUrlAndHtml(wiley.getFullTextUrl(), null ,null, HTTPUtils.GET , null);
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String fullArticleHtml = fullArticleMap.get("html");
				if(fullArticleHtml != null && !fullArticleHtml.isEmpty()){
					wiley = wileyParser.parseFullTextFromHTML(fullArticleHtml , wiley);
				}
				wileyService.updateWiley(wiley);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 设置查询请求的post body，包含大多数通用参数。
	 * @return
	 */
	private static Map<String, String> getBaseHttpRequestParameter() {
		Map<String, String> reqParam = new IdentityHashMap<String, String>();
		reqParam.put("searchRowCriteria[0].queryString","");
		reqParam.put("searchRowCriteria[0].fieldName","fulltext"); // 全文 关键词
		reqParam.put("searchRowCriteria[0].booleanConnector","and");
		reqParam.put("searchRowCriteria[1].queryString","");
		reqParam.put("searchRowCriteria[1].fieldName","all-fields");
		reqParam.put("searchRowCriteria[1].booleanConnector","and");
		reqParam.put("searchRowCriteria[2].queryString","");
		reqParam.put("searchRowCriteria[2].fieldName","all-fields");
		
		reqParam.put("inTheLastList","6");
		reqParam.put("dateRange","between");
		reqParam.put("startYear","2010");
		reqParam.put("endYear","2014");
		
		return reqParam;
	}


	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
}
