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
import selleck.email.pojo.Springer;
import selleck.email.service.ISpringerService;
import selleck.email.update.frame.UpdateScienceDirectFrame;
import selleck.email.update.parser.SpringerArticleParser;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdateScienceDirectCopy {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	ISpringerService springerService = null;
	
	public static void main(String[] args) {
		final UpdateScienceDirectFrame frame = UpdateScienceDirectFrame.getFrame();
		final UpdateScienceDirectCopy updateSD = new UpdateScienceDirectCopy();
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
	 * 读取期刊的excel，根据期刊名一个个去SciencDirect搜索文章。
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
					String pubName = row.getCell(0).getStringCellValue().trim(); // 期刊名
					this.getArticleUrl(pubName,startEntrez,endEntrez); // 先搜索文章的url ， 存入库中
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
	 * @param journal
	 * @param startEntrez
	 * @param endEntrez
	 */
	public void getArticleUrl(String journal , String startEntrez , String endEntrez) {
		loggerTA.append("开始查询期刊"+journal+"   "+startEntrez+"-"+endEntrez+"\n");
		int pageCount = 0; // 总页数
		int resultCount = 0 ; // 总数
		
		// search GET
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=all_search.tmpl&md5=bcb6a8104fc3e2467a87cd5c88f8d365&test_alid=&SearchText=cell&keywordOpt=TITLE-ABSTR-KEY&addTerm=1&addSearchText=cell&addkeywordOpt=FULL-TEXT&source=srcJrl&srcSel=18&srcSel=23&DateOpt=0&fromDate=2009&toDate=Present&RegularSearch=Search&RegularSearch=Search
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&SearchText=cell&keywordOpt=6&addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=5&srcSel=6&C1=FLA&C2=REV&DateOpt=0&fromDate=2009&toDate=Present&Volume=&Issue=&Page=&RegularSearch=Search
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&SearchText=cell&keywordOpt=6&addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=487          &C1=FLA&C2=REV&DateOpt=0&fromDate=2009&toDate=Present&Volume=&Issue=&Page=&RegularSearch=Search
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&SearchText=%7BAlzheimer%27s+%26+Dementia%7D&keywordOpt=11&addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=1&C1=FLA&C2=REV&C5=COR&DateOpt=0&fromDate=2004&toDate=Present&Volume=&Issue=&Page=&RegularSearch=Search
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&SearchText=%7BAnales+de+Pediatr%C3%ADa%7D   &keywordOpt=6 &addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=1&C1=FLA&C2=REV&C5=COR&DateOpt=0&fromDate=2004&toDate=Present&Volume=&Issue=&Page=&RegularSearch=Search
		// http://www.sciencedirect.com/science?_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&SearchText=%7BAnales+de+Pediatr%C3%ADa%7D   &keywordOpt=6 &addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=1&C1=FLA&C2=REV&C5=COR&DateOpt=0&fromDate=2010&toDate=Present&Volume=&Issue=&Page=&RegularSearch=Search
		//
		String url = "http://www.sciencedirect.com/science?";
		String contentTypeParam = "";
		String publicationParam = "";
		try {
			contentTypeParam = "C1=FLA&C2=REV&C5=COR"; // Article Review Correspondence
			journal = "{" + journal +"}"; // ScienceDirect 搜索{xxx} 表示精确匹配
			publicationParam = "SearchText="+URLEncoder.encode("\""+journal+"\"", "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		String dateParam = "DateOpt=0&fromDate="+startEntrez+"&toDate="+endEntrez;
		String otherParams = "_ob=MiamiSearchURL&_method=submitForm&_acct=C000228598&_temp=search.tmpl&md5=4f0858a9dcfa65ae9bcf4747cd5aac98&test_alid=&keywordOpt=6&addTerm=0&addSearchText=&addkeywordOpt=11&srcSel=1&Volume=&Issue=&Page=&RegularSearch=Search";
		String totalParams = otherParams + "&" + contentTypeParam + "&" + publicationParam + "&" + dateParam;
		url = url + totalParams;
		
		Map<String,String> searchMap = HTTPUtils.getCookieUrlAndHtml(url, null ,null, HTTPUtils.GET ,null);
		
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(searchMap.size() == 0){
			loggerTA.append("GET 查询失败\n");
			System.out.println("GET 查询失败");
			return;
		}
		
		String html = searchMap.get("html");
		
		// 获取文章总数
		// <span class="number-of-pages">253</span>
		String regex = "<strong>Search results: \\d+</strong>";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countTemp = matcher.group();
			countTemp = countTemp.replace("<strong>Search results: ", "").replace("</strong>", "").trim();
			resultCount = Integer.valueOf(countTemp);
		}
		
		/* 用文章总数/200的方法来获取文章总页数
		// 获取文章总页数
		// <span class="number-of-pages">253</span>
		String regex = "<input type=\"hidden\" name=\"TOTAL_PAGES\" value=\"\\d+\">";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countTemp = matcher.group();
			countTemp = countTemp.replace("<input type=\"hidden\" name=\"TOTAL_PAGES\" value=\"", "").replace("\">", "").trim();
			pageCount = Integer.valueOf(countTemp);
		}
		*/
		pageCount = (int)Math.ceil(resultCount / 200f);
		
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=979 &sort=r&filterType=&_chunk=0&hitCount=979  &NEXT_LIST=1&view=c&md5=357f4c46a982868580d765ecf08fae6b &_ArticleListID=-657884054&chunkSize=25&sisr_search=&TOTAL_PAGES=40  &zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&displayPerPageFlag=t&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=0&hitCount=8654&NEXT_LIST=1&view=c&md5=3209598a272f70978cb0eab158222eb2&_ArticleListID=-657909332&chunkSize=25&sisr_search=&TOTAL_PAGES=347&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&displayPerPageFlag=t&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=0&hitCount=8654&NEXT_LIST=1&view=c&md5=3209598a272f70978cb0eab158222eb2&_ArticleListID=-657909332&chunkSize=200&sisr_search=&TOTAL_PAGES=44&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=1&hitCount=8654&PREV_LIST=0&NEXT_LIST=2&view=c&md5=af6c7f5881a2bbe750af0d8236480e87&_ArticleListID=-657909332&chunkSize=25&sisr_search=&TOTAL_PAGES=347&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&displayPerPageFlag=t&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=0&hitCount=17968&NEXT_LIST=1&view=c&md5=b7a49083eace27663a5e3a341a7aa4ec&_ArticleListID=-657936755&chunkSize=25&sisr_search=&TOTAL_PAGES=719&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&displayPerPageFlag=t&resultsPerPage=200
		
		
		// loop page
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=0&hitCount=376572&NEXT_LIST=1&view=c&md5=e487d11a54a6632ffbf7e5419001b419&_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=1&hitCount=376572&PREV_LIST=0&NEXT_LIST=2&view=c&md5=ad1c553c71b0cb90e47a18a31103d99b&_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=2&hitCount=376572&PREV_LIST=1&NEXT_LIST=3&view=c&md5=44eec5fa3d862a20a5ae32c0d0310b32&_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=3&hitCount=376572&PREV_LIST=2&NEXT_LIST=4&view=c&md5=2c0f4dd8667bcd65982b390540fdaec3 &_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=4&hitCount=376576&PREV_LIST=3&NEXT_LIST=5&view=c&md5=53563e7c7a6250f47c2267f231c67bce &_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=4&count=1000&sort=r&filterType=&_chunk=5&hitCount=376572&PREV_LIST=4&NEXT_LIST=6&view=c&md5=371eaad21d11610a1ad8ce271e5110cb&_ArticleListID=-648815169&chunkSize=10&sisr_search=&TOTAL_PAGES=37658&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=10
		
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=0&hitCount=16328&NEXT_LIST=1                   &view=c&md5=c46bf43ed055eea6ab84283e87c3face&_ArticleListID=-649343621&chunkSize=200&sisr_search=&TOTAL_PAGES=82&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=1&hitCount=16328&PREV_LIST=0&NEXT_LIST=2&view=c&md5=7d21351d6e9e71d869d21879f30d9789&_ArticleListID=-649343621&chunkSize=200&sisr_search=&TOTAL_PAGES=82&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=2&hitCount=16328&PREV_LIST=1&NEXT_LIST=3&view=c&md5=aa6bbb1d1ac8af3cbf03b030a667a0b1&_ArticleListID=-649343621&chunkSize=200&sisr_search=&TOTAL_PAGES=82&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200
		// http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=3&hitCount=16328&PREV_LIST=2&NEXT_LIST=4&view=c&md5=768d276b51dc4dc8c61270eabc68c443&_ArticleListID=-649343621&chunkSize=200&sisr_search=&TOTAL_PAGES=82&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200
		// 
		
		// 获得 _ArticleListID
		// <input type="hidden" name="_ArticleListID" value=-649343621>
		String articleListID = "";
		regex = "<input type=\"hidden\" name=\"_ArticleListID\" value=\\-\\d+>";
		p = Pattern.compile(regex);
		matcher = p.matcher(html);
		if(matcher.find()){
			String countTemp = matcher.group();
			articleListID = countTemp.replace("<input type=\"hidden\" name=\"_ArticleListID\" value=\\-", "").replace(">", "").trim();
		}
				
		String basePageUrl = "http://www.sciencedirect.com/science?_ob=ArticleListURL&_method=tag&searchtype=a&refSource=search&_st=0&count=1000&sort=r&filterType=&_chunk=3&hitCount=16328&PREV_LIST=2&NEXT_LIST=4&view=c&md5=768d276b51dc4dc8c61270eabc68c443&_ArticleListID=-"+articleListID+"&chunkSize=200&sisr_search=&TOTAL_PAGES="+pageCount+"&zone=exportDropDown&citation-type=RIS&format=cite-abs&bottomPaginationBoxChanged=&bottomNext=Next+%3E%3E&displayPerPageFlag=f&resultsPerPage=200";
		System.out.println("总页数 "+pageCount);
		loggerTA.append("总页数 "+pageCount+"\n");
		String pageHtml = "";
		
		
		
		for(int i = 1;i <= pageCount;i++){
		// for(int i = 1;i <= 2;i++){ // for test
			try{
				String pageUrl = basePageUrl + String.valueOf(i)+"?"+totalParams;
				Map<String,String> pageMap = HTTPUtils.getCookieUrlAndHtml(pageUrl, null ,null, HTTPUtils.GET , null);
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				pageHtml = pageMap.get("html");
				if(pageHtml == null || pageHtml.isEmpty()){
					continue;
				}
				// 页面上url  /article/10.1186/s13058-014-0417-7
				// 完整url  http://link.springer.com/article/10.1186/s13058-014-0417-7
				// 全文url  http://link.springer.com/article/10.1186/s13058-014-0417-7/fulltext.html
				String articleRegex = "/article/[\\d\\.]+/[\\w\\d\\-]+\"";
				p = Pattern.compile(articleRegex);
				matcher = p.matcher(pageHtml);
				while(matcher.find()){
					String articleUrl = matcher.group().replaceAll("\"", ""); // 去掉最后的引号
					Springer springer = new Springer();
					
					// 查找有没有该文章的全文链接
					Pattern p1 = Pattern.compile(articleUrl + "/fulltext.html");
					Matcher matcher1 = p1.matcher(pageHtml);
					if(matcher1.find()){
						springer.setFullTextUrl("http://link.springer.com" + matcher1.group());
					}
					
					articleUrl = "http://link.springer.com" + articleUrl;
					springer.setSourcePublication(journal);
					springer.setUrl(articleUrl);
					
					try{
						springerService.saveSpringer(springer);
						this.loggerTA.append(springer.getUrl()+"\n");
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
		List<Springer> springers = springerService.selectBySearchPublication(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+springers.size()+"篇文章需要抓取\n");
		for(Springer springer : springers){
			try{
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(springer.getUrl(), null ,null, HTTPUtils.GET , null);
				
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
				
				springer = SpringerArticleParser.parseFromHTML(articleHtml , springer);
				
				// 如果有全文链接，再去访问全文链接获取全文
				if((springer.getFullTextUrl() != null && !springer.getFullTextUrl().isEmpty()) && (springer.getFullText() == null || springer.getFullText().isEmpty())){
					Map<String,String> fullArticleMap = HTTPUtils.getCookieUrlAndHtml(springer.getFullTextUrl(), null ,null, HTTPUtils.GET , null);
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					String fullArticleHtml = fullArticleMap.get("html");
					if(fullArticleHtml != null && !fullArticleHtml.isEmpty()){
						springer = SpringerArticleParser.parseFullTextFromHTML(fullArticleHtml , springer);
					}
				}
				
				loggerTA.append(springer.getTitle()+"\n");
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					springerService.updateSpringer(springer);
				}catch(Exception ee){
					ee.printStackTrace();
					continue;
				}
			}
			
			
		}
		
		// 补漏文章信息抓到但全文没抓到的情况
		criteria.setWhereClause(" (fulltext_url is not null and fulltext_url != '') and (full_text is null or full_text = '')  and SOURCE_PUBLICATION = '"+StringUtils.toSqlForm(pubName)+"' ");
		springers = springerService.selectBySearchPublication(criteria);
		this.loggerTA.append("期刊"+pubName+"总共有"+springers.size()+"篇文章需要抓取全文\n");
		for(Springer springer : springers){
			try{
				Map<String,String> fullArticleMap = HTTPUtils.getCookieUrlAndHtml(springer.getFullTextUrl(), null ,null, HTTPUtils.GET , null);
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
							
				String fullArticleHtml = fullArticleMap.get("html");
				if(fullArticleHtml != null && !fullArticleHtml.isEmpty()){
					springer = SpringerArticleParser.parseFullTextFromHTML(fullArticleHtml , springer);
				}
				springerService.updateSpringer(springer);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	public boolean isStartFlag() {
		return startFlag;
	}


	public void setStartFlag(boolean startFlag) {
		this.startFlag = startFlag;
	}
	
	
}
