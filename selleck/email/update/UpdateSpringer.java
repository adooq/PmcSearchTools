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
import selleck.email.service.impl.SpringerServiceImpl;
import selleck.email.update.frame.UpdateSpringerFrame;
import selleck.email.update.parser.SpringerArticleParser;
import selleck.email.update.tools.HMARobot;
import selleck.email.update.tools.JTextArea4Log;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class UpdateSpringer {
	public static long INTERVAL = 30000; // 翻页和查询文章请求的间隔ms
	
	private boolean startFlag = false;// 本线程开始标志
	private JTextArea4Log loggerTA;
	ISpringerService springerService;
	
	public static void main(String[] args) {
		final UpdateSpringerFrame frame = UpdateSpringerFrame.getFrame();
		final UpdateSpringer updateSpringer = new UpdateSpringer();
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
					if (select.equals("自动更新Springer文章")) {
						INTERVAL = Integer.valueOf(frame.getIntervalText().getText())*1000;
						String excelName = frame.getExcelText().getText();
						String startEntrez = frame.getPublicationText().getText();
						String endEntrez = frame.getCookieText().getText();
						String pubRange = frame.getPubLabel().getText();
						String dbName = frame.getDbSelect().getSelectedItem().toString();
						updateSpringer.springerService = new SpringerServiceImpl(dbName);
						updateSpringer.loadPubFromExcel(excelName,startEntrez,endEntrez,pubRange);
						// updateSpringer.loadPubFromCSV(frame.getExcelText().getText(),frame.getPublicationText().getText(),frame.getCookieText().getText());
					}
					updateSpringer.setStartFlag(false);
					frame.stopMouseClicked();
					updateSpringer.loggerTA.append("======== 完成全部期刊搜索 ========");
				}
			}
		});
		gmailThread.start();
		
		
	}
	
	public void loadPubFromCSV(String csvPath ,String startEntrez, String endEntrez){
		BufferedReader br = null;
		HMARobot robot = new HMARobot();
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
	 * 读取期刊的excel，根据期刊名一个个去Springer搜索文章。
	 * @param excel 纯excel文件名，不需要路径， 形如：springer publications.xlsx
	 * @param startEntrez  文章时间  2009  Springer只接受年份查询
	 * @param endEntrez   文章时间  2014 Springer只接受年份查询
	 * @param pubRange  期刊excel里的范围   形如:  1-200
	 */
	public void loadPubFromExcel(String excel ,String startEntrez, String endEntrez, String pubRange){
		Workbook wb = null;
		// HMARobot robot = new HMARobot(); 暂时不考虑换ip
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
					loggerTA.append("开始查询第 "+ (i+1) +" 个期刊 "+pubName+"   "+startEntrez+"-"+endEntrez+"\n");
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
		int pageCount = 1;
		
		// search GET
		// http://link.springer.com/search?facet-content-type="Article"&facet-publication-title="Reactions+Weekly"&date-facet-mode=between&facet-start-year=2013&facet-end-year=2014
		String url = "http://link.springer.com/search?";
		String contentTypeParam = "";
		String publicationParam = "";
		try {
			contentTypeParam = "facet-content-type="+URLEncoder.encode("\"Article\"","utf-8");
			publicationParam = "facet-publication-title="+URLEncoder.encode("\""+journal+"\"", "utf-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String dateParam = "date-facet-mode=between&facet-start-year="+startEntrez+"&facet-end-year="+endEntrez;
		String totalParams = contentTypeParam + "&" + publicationParam + "&" + dateParam;
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
		
		// 获取文章总页数
		// <span class="number-of-pages">253</span>
		String regex = "<span class=\"number-of-pages\">\\d+</span>";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(html);
		if(matcher.find()){
			String countTemp = matcher.group();
			countTemp = countTemp.replace("<span class=\"number-of-pages\">", "").replace("</span>", "").trim();
			pageCount = Integer.valueOf(countTemp);
		}
		
		
		// loop page
		// http://link.springer.com/search/page/3?date-facet-mode=between&facet-start-year=2014&facet-end-year=2014&facet-content-type=%22Article%22&facet-publication-title=%22Reactions+Weekly%22
		String basePageUrl = "http://link.springer.com/search/page/";
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
		// criteria.setWhereClause(" (title is null or title = '') and id >= 296472 ");
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
