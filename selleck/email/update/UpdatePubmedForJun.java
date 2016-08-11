package selleck.email.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.Pubmed;
import selleck.email.service.IPubmedService;
import selleck.email.service.impl.PubmedServiceImpl;
import selleck.email.update.frame.UpdatePubmedFrame;
import selleck.email.update.parser.PubmedArticleParser;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.Constants;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdatePubmedForJun {
	public static long INTERVAL = 0; // 翻页和查询文章请求的间隔ms
//	public static final String START_ENTREZ = "2009/01/01";
//	public static final String END_ENTREZ = "2014/06/30";
//	public static final String DOWNLOAD_DIR = "";
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	IPubmedService pubmedService = new PubmedServiceImpl(Constants.JUN);
	
	public static void main(String[] args) {
		final UpdatePubmedFrame frame = UpdatePubmedFrame.getFrame();
		final UpdatePubmedForJun updatePubmed = new UpdatePubmedForJun();
		updatePubmed.loggerTA = frame.getLoggerTA();
		
		frame.getStartBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.startMouseClicked();
				updatePubmed.setStartFlag(true);
			}
		});
		frame.getStopBT().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getStopBT().setEnabled(false);
				updatePubmed.setStartFlag(false);
			}
		});

		Thread gmailThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					boolean StartFlag = updatePubmed.isStartFlag();
					while (!StartFlag) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						StartFlag = updatePubmed.isStartFlag();
					}
					String select = frame.getMoveCB().getSelectedItem().toString();
					INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
					if (select.equals("自动更新Pubmed文章")) {
						String excelName = frame.getExcelText().getText();
						String startEntrez = frame.getPublicationText().getText();
						String endEntrez = frame.getCookieText().getText();
						String pubRange = frame.getPubLabel().getText();
						updatePubmed.loadPubFromExcel(excelName,startEntrez,endEntrez,pubRange);
						// updatePMC.loadPubFromCSV(frame.getExcelText().getText(),frame.getPublicationText().getText(),frame.getCookieText().getText());
					}
					updatePubmed.setStartFlag(false);
					frame.stopMouseClicked();
					updatePubmed.loggerTA.append("======== 完成全部期刊搜索 ========");
					System.out.println("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	/**
	 * @deprecated 现在在访问pubmed文章页面时，直接去访问PMC页面。
	 */
	public void queryEmailFromPMC(){
		PubmedArticleParser pmcParser = new PubmedArticleParser();
		HMARobot robot = new HMARobot();
		Criteria criteria = new Criteria();
		String whereClause = " pmcUrl is not null and full_text is null ";
		criteria.setWhereClause(whereClause);
		List<Pubmed> pubmeds = pubmedService.selectBySearchPublication(criteria);
		for(int i = 0;i < pubmeds.size();i++){
			Pubmed pubmed = pubmeds.get(i);
			boolean success = pmcParser.parsePMC(pubmed); // 搜索email 等信息
			
			// IP 被封的页面
			if(!success){
				robot.changeIP(loggerTA);
				i --;
				continue;
			}

			pubmedService.updatePubmed(pubmed);
		}
		
	}
	
	/*
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
	*/
	
	/**
	 * 读取期刊的excel，根据期刊名一个个去PMC搜索文章。
	 * @param excel 纯excel文件名，不需要路径， 形如： pmc.xlsx
	 * @param startEntrez  入库时间  2009/01/01
	 * @param endEntrez   入库时间  2014/06/30
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	/*
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
					
					// this.getArticleUrl(pubName,startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
					// this.queryArticles(pubName); // 按库中搜到的文章url更新文章的内容
					
					// this.getArticleUrl("",startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
					this.queryArticles(""); // 按库中搜到的文章url更新文章的内容
					
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
	*/
	
	public void loadPubFromExcel(String keyword ,String startEntrez, String endEntrez, String pubRange){
		this.getArticleUrl(keyword,startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
		// this.queryArticles(""); // 按库中搜到的文章url更新文章的内容
	}
	
	/**
	 * 根据搜索条件，获得所有文章url，先保存到库里。
	 * @param keyword，关键词
	 * @param startEntrez
	 * @param endEntrez
	 */
	public void getArticleUrl(String keyword , String startEntrez , String endEntrez) {
		loggerTA.append("开始查询"+keyword+" "+startEntrez+" "+endEntrez+"\n");
		String journalTerm = "";
		if(!keyword.isEmpty()){
			journalTerm = "\""+keyword+"\"[Text Word]) AND ";
		}
		String term = journalTerm + "(\""+startEntrez+"\"[Date - Entrez] : \""+endEntrez+"\"[Date - Entrez])";
		String dbTerm = journalTerm + "(\""+startEntrez+"\"[EDate] : \""+endEntrez+"\"[EDate])"; // post body里用的参数
		// (\"2015/06/01\"[Date+-+Entrez]+:+\"2015/06/30\"[Date+-+-+PubMed
		String subj = "(\""+startEntrez+"\"[Date - Entrez] : \""+endEntrez+"\"[Date - - PubMed";
		String cookie = "";
		String resultCount = "";
		
		
		try {
			term = URLEncoder.encode(term,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// search GET
		String url = "http://www.ncbi.nlm.nih.gov/pubmed?term="+term;
		Map<String,String> loginMap = HTTPUtils.getCookieUrlAndHtml(url, null ,null, HTTPUtils.GET ,null);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(loginMap.size() == 0){
			loggerTA.append("GET 查询失败\n");
			System.out.println("GET 查询失败");
			return;
		}

		cookie = loginMap.get("cookie");
		String html = loginMap.get("html");
		// System.out.println(html);
		
		// 获取文章总数
		List<String> articleCounts = ParserUtils.findWithPrefixAndSuffix("<meta name=\"ncbi_resultcount\" content=\"", "\"", html);
		if(articleCounts.size() > 0){
			resultCount = articleCounts.get(0);
		}else{
			resultCount = "0";
		}
		
		/* deprecated  Pubmed好像改版了
		String regex = "<h2 class=\"result_count\">Results:.+</h2>";  
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countStr = matcher.group();
			countStr = countStr.replace("<h2 class=\"result_count\">Results:", "").replace("</h2>", "").trim();
			resultCount = countStr.replaceAll("\\d+ to \\d+ of", "").trim();
		}else{
			resultCount = "0";
		}
		*/
		
		// loop page
		String pageUrl ="http://www.ncbi.nlm.nih.gov/pubmed";
		int totalPage = ((Double)Math.ceil(Double.valueOf(resultCount)/200)).intValue(); // 每页200
		System.out.println("总页数 " + totalPage + " 总文章数" + resultCount);
		loggerTA.append("总页数 "+totalPage + " 总文章数" + resultCount+"\n");
		String pageHtml = "";
		PubmedArticleParser pubmedParser = new PubmedArticleParser();
		int realArticleCount = 0; // 实际抓取的文章数量
		for(int i = 1;i <= totalPage;i++){
			try{
				// for(int i = 1;i <= 3;i++){ // for test
				Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(pageUrl, cookie ,".ncbi.nlm.nih.gov", HTTPUtils.POST ,getPostParams(term,dbTerm,String.valueOf(i),resultCount,subj));
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				pageHtml = pageMap.get("html");
				// System.out.println(pageHtml);
				
				// <p class="title"><a href="/pubmed/26126293" 
				String articleRegex = "<p class=\"title\"><a href=\"/pubmed/\\d+";
				Pattern p = Pattern.compile(articleRegex);
				Matcher matcher = p.matcher(pageHtml);
	//			int articleCount = 0;
				while(matcher.find()){
					try{
						String articleUrl = matcher.group().replaceFirst("<p class=\"title\"><a href=\"","");
						if(articleUrl.contains("pageindex")){
							continue;
						}
						articleUrl = "http://www.ncbi.nlm.nih.gov" + articleUrl;
						Pubmed pubmed = new Pubmed();
						pubmed.setUrl(articleUrl);
						pubmedService.savePubmed(pubmed); // 先去保存url主要为了和数据库里去重
						if(pubmed.getId() == 0){ // id为0，说明数据库里已有该url
							continue;
						}
						
						// 访问文章链接
						Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null ,null, HTTPUtils.GET , null);
						
						String articleHtml = articleMap.get("html");
						if(articleHtml == null || articleHtml.isEmpty()){
							continue;
						}
						pubmed = pubmedParser.parseFromHTML(articleHtml, pubmed);
						pubmedService.updatePubmed(pubmed);
						loggerTA.append(pubmed.getTitle()+"\n");
						realArticleCount ++;
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
				}
			}catch(Exception ee){
				ee.printStackTrace();
				continue;
			}
		}
		
		loggerTA.append("抓取文章 "+realArticleCount+"\n");
		System.out.println("抓取文章 "+realArticleCount);
		
		
	}
	
	/**
	 * 按文章的url去访问文章网页并抓取内容
	 * @param pubName excel中的期刊名。传入null， 表示不按期刊查。
	 * @deprecated 现在在翻页过程中直接访问文章页面
	 */
	public void queryArticles(String pubName){
		int currentId = 0;
		Criteria criteria = new Criteria();
		String whereClause = " (title is null or title = '') ";
		if(pubName != null && !pubName.trim().isEmpty()){
			whereClause +=" and SOURCE_PUBLICATION = '"+StringUtils.toSqlForm(pubName)+"' ";
		}
		while(true){
			criteria.setWhereClause(whereClause + " and id >" + currentId + " limit 1");
			try{
				List<Pubmed> pubmeds = pubmedService.selectBySearchPublication(criteria);
				if(pubmeds.size() == 0){
					break;
				}
				// this.loggerTA.append("期刊"+pubName+"总共有"+pubmeds.size()+"篇文章需要抓取\n");
				for(Pubmed pubmed : pubmeds){
					currentId =	pubmed.getId();
					Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(pubmed.getUrl(), null ,null, HTTPUtils.GET , null);
	
					String articleHtml = articleMap.get("html");
					if(articleHtml == null || articleHtml.isEmpty()){
						continue;
					}
					
					/*
					// IP 被封的页面
					if(articleHtml.contains("")){
						
					}
					*/
					
					PubmedArticleParser pubmedParser = new PubmedArticleParser();
					pubmed = pubmedParser.parseFromHTML(articleHtml, pubmed);
					loggerTA.append(pubmed.getTitle()+"\n");
					pubmedService.updatePubmed(pubmed);

				}
			}catch(Exception e){
				e.printStackTrace();
				break;
			}finally{
				try{
					Thread.sleep(INTERVAL);
				}catch(Exception ee){
					ee.printStackTrace();
					break;
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
	private Map<String,String> getPostParams(String term, String dbTerm , String currPage , String resultCount, String subj){
		Map<String,String> params = new IdentityHashMap<String,String>();
		params.put("term", term);
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_PageController.PreviousPageName", "results");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_PageController.SpecialPageName", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_Facets.FacetsUrlFrag", "filters=");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_Facets.FacetSubmitted", "false");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_Facets.BMFacets", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sPresentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sPageSize", "200");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sSort", "none");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FFormat", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FSort", "");
		params.put("email_format", "docsum");
		params.put("email_sort", "");
		params.put("email_count", "200");
		params.put("email_start", "1");
		params.put("email_address", "");
		params.put("email_subj", subj);
		params.put("email_add_text", "");
		params.put(new String("EmailCheck1"), "");
		params.put(new String("EmailCheck2"), "");
		params.put("coll_start", "1");
		params.put("citman_count", "200");
		params.put("citman_start", "1");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FileFormat", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.LastPresentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.Presentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.PageSize", "200");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.LastPageSize", "200");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.Sort", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.LastSort", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FileSort", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.Format", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.LastFormat", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.PrevPageSize", "200");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.PrevPresentation", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.PrevSort", "");
		params.put("CollectionStartIndex", "1");
		params.put("CitationManagerStartIndex", "1");
		params.put("CitationManagerCustomRange", "false");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Entrez_Pager.cPage","2");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Entrez_Pager.CurrPage", currPage);
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_ResultsController.ResultCount", resultCount);
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_ResultsController.RunLastQuery", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Entrez_Pager.cPage", "1");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sPresentation2", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sPageSize2", "200");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.sSort2", "none");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FFormat2", "docsum");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Pubmed_DisplayBar.FSort2", "");
		params.put("email_format2", "docsum");
		params.put("email_sort2", "");
		params.put("email_count2", "200");
		params.put("email_start2", "1");
		params.put("email_address2", "");
		params.put("email_subj2", subj);
		params.put("email_add_text2", "");
		params.put(new String("EmailCheck1"), "");
		params.put(new String("EmailCheck2"), "");
		params.put("coll_start2", "1");
		params.put("citman_count2", "200");
		params.put("citman_start2", "1");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailReport", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailFormat", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailCount", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailStart", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailSort", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.Email", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailSubject", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailText", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailQueryKey", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.EmailHID", "1netUx8Nnwgst-lgS-j-l-uDdiwBcfgK6n-Ij-kBBW7QpsaOr9tKhBNx_VVqYWP_EAU47mngwsecd7n5SXMvZ4WHOXIOXv7Q-a");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.QueryDescription", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.Key", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.Answer", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.Holding", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.HoldingFft", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.HoldingNdiSet", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.OToolValue", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.EmailTab.SubjectList", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.TimelineAdPlaceHolder.CurrTimelineYear", "");
		params.put("EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.TimelineAdPlaceHolder.BlobID", "NCID_1_152272154_130.14.22.215_9001_1437547379_354216472_0MetA0_S_MegaStore_F_1");
		params.put("EntrezSystem2.PEntrez.DbConnector.Db", "pubmed");
		params.put("EntrezSystem2.PEntrez.DbConnector.LastDb", "pubmed");
		params.put("EntrezSystem2.PEntrez.DbConnector.Term", dbTerm);
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
		params.put("p%24a", "EntrezSystem2.PEntrez.PubMed.Pubmed_ResultsPanel.Entrez_Pager.cPage");
		params.put("p%24l", "EntrezSystem2");
		params.put("p%24st", "pubmed");
		
		return params;
	}
	

	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	
}
