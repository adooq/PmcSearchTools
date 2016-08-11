package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.IdentityHashMap;
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
import selleck.email.pojo.PMC;
import selleck.email.service.IPMCService;
import selleck.email.service.impl.PMCServiceImpl;
import selleck.email.update.frame.UpdatePMCFrame;
import selleck.email.update.template.PMCBaseArticleParser;
import selleck.email.update.template.PlosOneParser;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.Constants;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdatePMC {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
//	public static final String START_ENTREZ = "2009/01/01";
//	public static final String END_ENTREZ = "2014/06/30";
//	public static final String DOWNLOAD_DIR = "";
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	IPMCService pmcService = new PMCServiceImpl(Constants.LIFE_SCIENCE_DB);
	
	public static void main(String[] args) {
		final UpdatePMCFrame frame = UpdatePMCFrame.getFrame();
		final UpdatePMC updatePMC = new UpdatePMC();
		updatePMC.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updatePMC.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updatePMC.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updatePMC.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updatePMC.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					if (select.equals("自动更新PMC文章")) {
						INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
						String excelName = frame.getExcelText().getText();
						String startEntrez = frame.getPublicationText().getText();
						String endEntrez = frame.getCookieText().getText();
						String pubRange = frame.getPubLabel().getText();
						updatePMC.loadPubFromExcel(excelName,startEntrez,endEntrez,pubRange);
						// updatePMC.loadPubFromCSV(frame.getExcelText().getText(),frame.getPublicationText().getText(),frame.getCookieText().getText());
					}
					updatePMC.setStartFlag(false);
					frame.stopMouseClicked();
					updatePMC.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	public void loadPubFromCSV(String csvPath ,String startEntrez, String endEntrez){
		BufferedReader br = null;
		HMARobot robot = new HMARobot(); //  for  HMA-2.8.11.1 旧版本界面位置不一样
		try {
			File csv = new File(csvPath); // CSV文件
			if (!csv.exists()) {
				this.loggerTA.append("CSV 文件不存在 \n");
				return;
			}

			br = new BufferedReader(new FileReader(csv));
			// 读取直到最后一行
			String line = "";
			while ((line = br.readLine()) != null) {
				this.getArticleUrl(line.trim(),startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
				this.queryArticles(line.trim()); // 按库中搜到的文章url更新文章的内容
				robot.changeIP(loggerTA); // 搜索完一个期刊，HMA 换个IP
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
	 * 读取期刊的excel，根据期刊名一个个去PMC搜索文章。
	 * @param excel 纯excel文件名，不需要路径， 形如： pmc.xlsx
	 * @param startEntrez  入库时间  2009/01/01
	 * @param endEntrez   入库时间  2014/06/30
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	public void loadPubFromExcel(String excel ,String startEntrez, String endEntrez, String pubRange){
		Workbook wb = null;
		HMARobot robot = new HMARobot();
		try {
			wb = WorkbookFactory.create(new File(excel));
			Sheet sheet = wb.getSheet("Sheet1");
			int rowCount = sheet.getLastRowNum();
			int pubStart = Integer.valueOf(pubRange.split("-")[0]) - 1;
			int pubEnd = Integer.valueOf(pubRange.split("-")[1]) - 1;
			pubEnd = Math.min(pubEnd, rowCount);
			// for (int i = 0; i <= 0; i++) { // for test
			// for (int i = 0; i <= rowCount; i++) { // row从0开始
			for (int i = pubStart; i <= pubEnd; i++) { // row从0开始
				try {
					Row row = sheet.getRow(i);
					String pubName = row.getCell(0).getStringCellValue().trim(); // 期刊名
					this.getArticleUrl(pubName,startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
					this.queryArticles(pubName); // 按库中搜到的文章url更新文章的内容
					robot.changeIP(loggerTA); // 搜索完一个期刊，HMA 换个IP
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
	 * @param journal
	 * @param startEntrez
	 * @param endEntrez
	 */
	public void getArticleUrl(String journal , String startEntrez , String endEntrez) {
		loggerTA.append("开始查询期刊"+journal+" "+startEntrez+" "+endEntrez+"\n");
		String term = "(\""+journal+"\"[Journal]) AND (\""+startEntrez+"\"[Entrez Date] : \""+endEntrez+"\"[Entrez Date])";
		String dbTerm = "\""+journal+"\"[Journal] AND (\""+startEntrez+"\"[EDate] : \""+endEntrez+"\"[EDate])"; // post body里用的参数
		String cookie = "";
		String resultCount = "";
		
		
		try {
			term = URLEncoder.encode(term,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// search GET
		String url = "http://www.ncbi.nlm.nih.gov/pmc?term="+term;
		Map<String,String> loginMap = HTTPUtils.getCookieUrlAndHtml(url, null ,null, HTTPUtils.GET ,null);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(loginMap.size() == 0){
			loggerTA.append("GET 查询失败\n");
			System.out.println("GET 查询失败");
			return;
		}

		cookie = loginMap.get("cookie");
		String html = loginMap.get("html");
		
		// 获取文章总数
		String regex = "<h2 class=\"result_count\">Results:.+</h2>";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countStr = matcher.group();
			countStr = countStr.replace("<h2 class=\"result_count\">Results:", "").replace("</h2>", "").trim();
			resultCount = countStr.replaceAll("\\d+ to \\d+ of", "").trim();
		}else{
			resultCount = "0";
		}
		
		// loop page
		String pageUrl ="http://www.ncbi.nlm.nih.gov/pmc";
		int totalPage = ((Double)Math.ceil(Double.valueOf(resultCount)/100)).intValue();
		System.out.println("总页数 "+totalPage);
		loggerTA.append("总页数 "+totalPage+"\n");
		String pageHtml = "";
		for(int i = 1;i <= totalPage;i++){
			try{
				// for(int i = 1;i <= 3;i++){ // for test
				Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(pageUrl, cookie ,".ncbi.nlm.nih.gov", HTTPUtils.POST ,getPostParams(term,dbTerm,String.valueOf(i),resultCount));
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				pageHtml = pageMap.get("html");
				// href="/pmc/articles/PMC4100575/?report=classic"
				String articleRegex = "/pmc/articles/PMC\\d+/\\?report=classic";
				p = Pattern.compile(articleRegex);
				matcher = p.matcher(pageHtml);
	//			int articleCount = 0;
				while(matcher.find()){
	//				// for test
	//				articleCount ++;
	//				if(articleCount > 5){
	//					break;
	//				}
					String articleUrl = matcher.group();
					if(articleUrl.contains("pageindex")){
						continue;
					}
					articleUrl = "http://www.ncbi.nlm.nih.gov" + articleUrl;
					PMC pmc = new PMC();
					pmc.setSourcePublication(journal);
					pmc.setUrl(articleUrl);
					try{
						pmcService.savePMC(pmc);
						this.loggerTA.append(pmc.getUrl()+"\n");
					}catch(Exception ee){
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
	 * @param pubName excel中的期刊名。
	 */
	public void queryArticles(String pubName){
		Criteria criteria = new Criteria();
		criteria.setWhereClause(" (title is null or title = '') and SOURCE_PUBLICATION = '"+StringUtils.toSqlForm(pubName)+"' ");
		List<PMC> pmcs = pmcService.selectBySearchPublication(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+pmcs.size()+"篇文章需要抓取\n");
		for(PMC pmc : pmcs){
			try{
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(pmc.getUrl(), null ,null, HTTPUtils.GET , null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String articleHtml = articleMap.get("html");
				if(articleHtml == null || articleHtml.isEmpty()){
					continue;
				}
				
				PMCBaseArticleParser pmcParser = null;
				if(pubName.equalsIgnoreCase(PMCBaseArticleParser.PLOS_ONE)){
					pmcParser = new PlosOneParser();
				}else{
					pmcParser = new PMCBaseArticleParser();
				}
				pmc = pmcParser.parseFromHTML(articleHtml , pmc);
				loggerTA.append(pmc.getTitle()+"\n");
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					pmcService.updatePMC(pmc);
				}catch(Exception ee){
					ee.printStackTrace();
					continue;
				}
			}
			
		}
	}
	
	
	
	/**
	 * 翻页的POST
	 * @param term URLEncoder.encode( ("PLoS ONE"[Journal]) AND ("2009/01/01"[Entrez Date] : "2014/06/30"[Entrez Date]) )
	 * @param dbTerm URLEncoder.encode( "PLoS ONE"[Journal] AND ("2009/01/01"[EDate] : "2014/06/30"[EDate]) )
	 * @param currPage 查询的页
	 * @param resultCount 文章总数
	 * @return
	 */
	private Map<String,String> getPostParams(String term, String dbTerm , String currPage , String resultCount){
		Map<String,String> params = new IdentityHashMap<String,String>();
		params.put("term", term);
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_PageController.PreviousPageName", "results");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailReport", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailFormat", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailCount", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailStart", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailSort", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.Email", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailSubject", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailText", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.EmailQueryKey", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.EmailTab.QueryDescription", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPresentation", "DocSum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPageSize", "100");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sSort", "none");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FFormat", "DocSum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FSort", "");
		params.put("email_format", "DocSum");
		params.put("email_sort", "");
		params.put("email_count", "100");
		params.put("email_start", "1");
		params.put("email_address", "");
		params.put("email_add_text", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FileFormat", "docsum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastPresentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Presentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.PageSize", "100");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastPageSize", "100");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Sort", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastSort", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FileSort", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.Format", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.LastFormat", "");
		params.put(new String("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.cPage"), "3"); // IdentityHashMap判断key1==key2?，所以要写成两个不同对象形式
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.CurrPage", currPage);
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_ResultsSearchController.ResultCount", resultCount);
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_ResultsSearchController.RunLastQuery", "");
		params.put(new String("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.cPage"), "1");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPresentation2", "DocSum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sPageSize2", "100");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.sSort2", "none");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FFormat2", "DocSum");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_DisplayBar.FSort2", "");
		params.put("email_format2", "DocSum");
		params.put("email_sort2", "");
		params.put("email_count2", "100");
		params.put("email_start2", "1");
		params.put("email_address2", "");
		params.put("email_add_text2", "");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_FilterTab.CurrFilter", "all");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_FilterTab.LastFilter", "all");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_MultiItemSupl.Pmc_RelatedDataLinks.rdDatabase", "rddbto");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Pmc_MultiItemSupl.Pmc_RelatedDataLinks.DbName", "pmc");
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Discovery_SearchDetails.SearchDetailsTerm", dbTerm);
		params.put("EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.HistoryDisplay.Cmd", "PageChanged");
		params.put("EntrezSystem2.PEntrez.DbConnector.Db", "pmc");
		params.put("EntrezSystem2.PEntrez.DbConnector.LastDb", "pmc");
		params.put("EntrezSystem2.PEntrez.DbConnector.Term", term);
		params.put("EntrezSystem2.PEntrez.DbConnector.LastTabCmd", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.LastQueryKey", "1");
		params.put("EntrezSystem2.PEntrez.DbConnector.IdsFromResult", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.LastIdsFromResult", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.LinkName", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.LinkReadableName", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.LinkSrcDb", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.Cmd", "PageChanged");
		params.put("EntrezSystem2.PEntrez.DbConnector.TabCmd", "");
		params.put("EntrezSystem2.PEntrez.DbConnector.QueryKey", "");
		params.put("p%24a", "EntrezSystem2.PEntrez.PMC.Pmc_ResultsPanel.Entrez_Pager.cPage");
		params.put("p%24l", "EntrezSystem2");
		params.put("p%24st", "pmc");
		return params;
	}


	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	
}
