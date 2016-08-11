package selleck.email.controller;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.ibm.icu.text.DecimalFormat;

import selleck.email.update.tools.ParserUtils;
import selleck.utils.HTTPUtils;


/**
 * 为官网产品的citation下载全文pdf
 * @author fscai
 *
 */
public class DownloadCitation {
	private static String pubmedCookie =	"";
	private static String sdCookie = "";
	private static String keti8Cookie = "";
	private static String pubmedDomain = "";
	private static String pubmedCookieDomain = "";
	private static String sdDomain = "";
	private static String sdCookieDomain = "";
	
	public static void main(String[] args) {
		downloadFromExcel();
		// countInvalidRow(); // excel总共6093，无效的行446，已下载dpf 4873个，对应5186个文献。
	}

	/**
	 * @param args
	 */
	public static void downloadFromExcel() {
		String url = "";
		String htmlStr = "";
		List<String> regexResult = null;
		Map<String, String> httpMap = null;
		
		loginKeti8();
		loginPubmed();
		// System.out.println("pubmed home cookie: " + pubmedCookie);
		// */
		
		// pubmed通道二  http://www.keti8.com/e/action/ListInfo/?classid=77
		// pubmed永久辅助通道  http://www.keti8.com/e/action/ListInfo/?classid=140
		
		
		// 新建全文pdf文件夹
		File dir = new File("citationDownload");
		if(!dir.exists()){
			dir.mkdir();
		}
		
		
		Workbook wb = null;
		File excelFile = new File("citation_所有产品.XLSX");
		try {
			wb = WorkbookFactory.create(excelFile);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheet("Sheet2");
		int rowCount = sheet.getLastRowNum();
		System.out.println("rowCount:" + rowCount);
		DecimalFormat df = new DecimalFormat("#"); // 转换成整型
		for (int i = 1; i <= rowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
		// for (int i = 565; i <= 565; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
		// for (int i = 1; i <= 5; i++) { // for test
			try{
			Row row = sheet.getRow(i);
			String fileName = "";
			if(row.getCell(3) != null){
				fileName = row.getCell(3).getStringCellValue().trim(); // filename. 文章图片的文件名，去掉.gif，加上.pdf变成全文pdf 
				if(fileName.endsWith(".gif")){
					fileName = fileName.substring(0, fileName.length() - 4); // 去掉最后的.gif
				}else{
					continue;
				}
			}else{
				continue;
			}
			
			// 如果已有pdf，就不去查询
			File file = new File("citationDownload\\"+fileName+".pdf");
			if(file.exists()){
				Cell cell = row.createCell(32);
				cell.setCellType(Cell.CELL_TYPE_STRING);
				cell.setCellValue(file.getName());
				continue;
			}
			
			String pmid = "";
			if(row.getCell(19) != null){
				double pmidDouble = row.getCell(19).getNumericCellValue(); // PMID，形如24454929
				pmid = df.format(pmidDouble);
			}
			if(row.getCell(20) != null){
				url = row.getCell(20).getStringCellValue(); // url，形如http://www.ncbi.nlm.nih.gov/pubmed/24454929?dopt=Abstract
			}
			if(row.getCell(19) == null && row.getCell(20) == null){
				continue;
			}
			if(!pmid.trim().isEmpty()){ // 有PMID说明是pubmed文章， 用PMID查询
				// 通道一 http://www-ncbi-nlm-nih-gov.proxy1.lib.uwo.ca/pubmed/?term=24454929
				 
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(pubmedDomain + "/pubmed/?term="+pmid, pubmedCookie, pubmedCookieDomain, HTTPUtils.GET, null);
				if(articleMap.get("html") == null || articleMap.get("html").isEmpty()){
					System.out.println("访问文章失败");
				}
				htmlStr = articleMap.get("html");
				
				// 全文链接
				List<String> links = ParserUtils.findWithPrefixAndSuffix("<div class=\"icons portlet\">","</a>",htmlStr);
				if(links.size() > 0){
					String link = links.get(0);
					List<String> hrefs = ParserUtils.findWithPrefixAndSuffix("href=\"","\"",link);
					// http://linkinghub.elsevier.com.proxy1.lib.uwo.ca/retrieve/pii/S0006-291X(14)02050-6
					if(hrefs.size() > 0 && hrefs.get(0).startsWith("http://linkinghub.elsevier.com")){ // 下载sciencedirect
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(hrefs.get(0), pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							
							// 下载pdf
							List<String> pdfUrls = ParserUtils.findWithPrefixAndSuffix("<a id=\"pdfLink\" rel=\"nofollow\"   href=\"" , "\"" , fullTextHtml);
							if(pdfUrls.size() > 0){
//								File file = new File("citationDownload\\"+fileName+".pdf");
//								if(file.exists()){
//									file.delete();
//								}
								HTTPUtils.downloadFile(pdfUrls.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
						}
					}else if(hrefs.size() > 0 && link.contains("production.springer.de")){ // 下载Springer
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(hrefs.get(0), pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");							
							String springerUrl = fulltextMap.get("url").replaceFirst("http[s]?://", ""); // http://link.springer.com.proxy1.lib.uwo.ca/article/10.1007%2Fs10495-010-0521-9
							String springerDomain = springerUrl.substring(0, springerUrl.indexOf("/"));  //形如 http://link.springer.com.proxy1.lib.uwo.ca
							// 下载pdf
							List<String> pdfUrls = ParserUtils.findWithPrefixAndSuffix("abstract\\-actions\\-download\\-article\\-pdf\\-link.+?href=\"" , "\"" , fullTextHtml);
							if(pdfUrls.size() > 0){
//								File file = new File("citationDownload\\"+fileName+".pdf");
//								if(file.exists()){
//									file.delete();
//								}
								HTTPUtils.downloadFile("http://" + springerDomain + pdfUrls.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
						}
					}else if(hrefs.size() > 0 && link.contains("media.wiley.com")){ // 下载Wiley
						String wileyHref = StringEscapeUtils.unescapeHtml4(hrefs.get(0)); // 去转义字符，  &gt;  转换成>符号
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(wileyHref, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("name=\"citation_pdf_url\" content=\"", "\"", fullTextHtml); // 找到pdf页面链接
							if(pdfs.size() > 0){
								Map<String,String> pdfMap = HTTPUtils.getCookieUrlAndHtml(pdfs.get(0), pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
								if(pdfMap.get("html") != null){
									String pdfHMTL = pdfMap.get("html");
									// 下载pdf
									List<String> pdfLinks = ParserUtils.findWithPrefixAndSuffix("id=\"pdfDocument\" src=\"", "\"", pdfHMTL); // 找到pdf源文件链接
									if(pdfLinks.size() > 0){
	//									File file = new File("citationDownload\\"+fileName+".pdf");
	//									if(file.exists()){
	//										file.delete();
	//									}
										HTTPUtils.downloadFile(pdfLinks.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
										Cell cell = row.createCell(32);
										cell.setCellType(Cell.CELL_TYPE_STRING);
										cell.setCellValue(file.getName());
									}
								}
							}		
							
						}
					}else if(hrefs.size() > 0 && (link.contains("aacrjournals.org") || link.contains("endocrinology-journals.org") || link.contains("oxfordjournals_final.gif")
								|| link.contains("jbc.org") || link.contains("aspetjournals.org") || link.contains("oxfordjournals.org")
								|| link.contains("_free.gif") || link.contains("final.gif") || link.contains("_full.gif") || link.contains("Icon for HighWire")) ){ // 下载 aacrjournals一系列，页面都是一样的
						String href = hrefs.get(0);
						href = StringEscapeUtils.unescapeHtml4(href); // 去转义字符，  &gt;  转换成>符号
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(href, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("notice full-text-pdf-view-link( primary)?\"><a href=\"", "\"", fullTextHtml); // 找到pdf页面链接
							if(pdfs.size() > 0){
								String aacrjournalsUrl = fulltextMap.get("url").replaceFirst("http[s]?://", ""); // http://clincancerres.aacrjournals.org.proxy1.lib.uwo.ca/content/20/13/3613.long
								String aacrjournalsDomain = aacrjournalsUrl.substring(0, aacrjournalsUrl.indexOf("/"));
								String pdfUrl = pdfs.get(0).replaceAll("\\+html", ""); // /content/20/13/3613.full.pdf+html  去掉最后的+html
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile("http://" + aacrjournalsDomain +pdfUrl, pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}else{
								pdfs = ParserUtils.findWithPrefixAndSuffix("<li class=\"last\"><a href=\"", "\"", fullTextHtml); // 找到pdf页面链接
								if(pdfs.size() > 0){
									String finalFreeUrl = fulltextMap.get("url").replaceFirst("http[s]?://", ""); // http://clincancerres.aacrjournals.org.proxy1.lib.uwo.ca/content/20/13/3613.long
									String finalFreeDomain = finalFreeUrl.substring(0, finalFreeUrl.indexOf("/"));
									String pdfUrl = pdfs.get(0).replaceAll("\\+html", "").replaceAll("\\-text", ""); // /content/20/13/3613.full.pdf+html  去掉最后的+html
		//							File file = new File("citationDownload\\"+fileName+".pdf");
		//							if(file.exists()){
		//								file.delete();
		//							}
									HTTPUtils.downloadFile("http://" + finalFreeDomain +pdfUrl, pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
									Cell cell = row.createCell(32);
									cell.setCellType(Cell.CELL_TYPE_STRING);
									cell.setCellValue(file.getName());
								}
							}
						}
					}else if(hrefs.size() > 0 && (link.contains("journal=\"Nature\"") || link.contains("lo_npg.gif") || link.contains("npg_free.gif")
									|| link.contains("nprot.gif") || link.contains("lo_ncb.gif") || link.contains("www.nature.com-images"))  ){ // 下载 nature , nature publish group(npg)
						String href = hrefs.get(0);
						href = StringEscapeUtils.unescapeHtml4(href); // 去转义字符，  &gt;  转换成>符号
						HTTPUtils.REFERER = articleMap.get("url");
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(href, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						HTTPUtils.REFERER = null;
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("class=\"download-pdf\"><a href=\"", "\"", fullTextHtml); // 找到pdf链接
							if(pdfs.size() > 0){
								String natureUrl = fulltextMap.get("url").replaceFirst("http[s]?://", "");
								String natureDomain = natureUrl.substring(0, natureUrl.indexOf("/"));
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile("http://" + natureDomain +pdfs.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}else{
								pdfs = ParserUtils.findWithPrefixAndSuffix("class=\"download-pdf\" href=\"", "\"", fullTextHtml); // 找到pdf链接
								if(pdfs.size() > 0){
									String natureUrl = fulltextMap.get("url").replaceFirst("http[s]?://", "");
									String natureDomain = natureUrl.substring(0, natureUrl.indexOf("/"));
		//							File file = new File("citationDownload\\"+fileName+".pdf");
		//							if(file.exists()){
		//								file.delete();
		//							}
									HTTPUtils.downloadFile("http://" + natureDomain +pdfs.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
									Cell cell = row.createCell(32);
									cell.setCellType(Cell.CELL_TYPE_STRING);
									cell.setCellValue(file.getName());
								}
							}
							
						}
					}else if(hrefs.size() > 0 && link.contains("www.tandfonline.com")){ // 下载 tandfonline
						String href = hrefs.get(0);
						href = StringEscapeUtils.unescapeHtml4(href); // 去转义字符，  &gt;  转换成>符号
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(href, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("class=\"pdf( last)?\" target=\"_blank\" href=\"", "\"", fullTextHtml); // 找到pdf链接
							if(pdfs.size() > 0){
								String tandfonlineUrl = fulltextMap.get("url").replaceFirst("http[s]?://", "");
								String tandfonlineDomain = tandfonlineUrl.substring(0, tandfonlineUrl.indexOf("/"));
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile("http://" + tandfonlineDomain +pdfs.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
							
						}
					}else if(hrefs.size() > 0 && link.contains("acs.org")){ // 下载 acs.org
						String href = hrefs.get(0);
						href = StringEscapeUtils.unescapeHtml4(href); // 去转义字符，  &gt;  转换成>符号
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(href, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("class=\"icon\\-item icon\\-item\\-24px subordinate pdf\\-high\\-res\">\\p{ASCII}*?<a href=\"", "\"", fullTextHtml); // 找到pdf链接
							if(pdfs.size() > 0){
								String acsUrl = fulltextMap.get("url").replaceFirst("http[s]?://", "");
								String acsDomain = acsUrl.substring(0, acsUrl.indexOf("/"));
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile("http://" + acsDomain + pdfs.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
							
						}
					}else if(hrefs.size() > 0 && link.contains("endocrine.org")){ // 下载 endocrine.org
						String href = hrefs.get(0);
						href = StringEscapeUtils.unescapeHtml4(href); // 去转义字符，  &gt;  转换成>符号
						Map<String,String> fulltextMap = HTTPUtils.getCookieUrlAndHtml(href, pubmedCookie ,pubmedCookieDomain, HTTPUtils.GET , null);
						if(fulltextMap.get("html") != null){
							String fullTextHtml = fulltextMap.get("html");
							List<String> pdfs = ParserUtils.findWithPrefixAndSuffix("<a href=\"/doi/pdf", "\">", fullTextHtml); // 找到pdf链接
							if(pdfs.size() > 0){
								String endocrineUrl = fulltextMap.get("url").replaceFirst("http[s]?://", "");
								String endocrineDomain = endocrineUrl.substring(0, endocrineUrl.indexOf("/"));
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile("http://" + endocrineDomain + "/doi/pdf" +pdfs.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
						}
					}
					
					continue;
				}
				
				
			}else if(url.startsWith("http://www.sciencedirect.com/")){ // 是否是sd文章
				url = sdDomain + url.replaceFirst("http://www.sciencedirect.com", "");
				httpMap = HTTPUtils.getCookieUrlAndHtml(url, sdCookie ,sdCookieDomain, HTTPUtils.GET , null);
				if(httpMap.get("html") != null){
					String fullTextHtml = httpMap.get("html");
					
					// 下载pdf
					List<String> pdfUrls = ParserUtils.findWithPrefixAndSuffix("<a id=\"pdfLink\" rel=\"nofollow\"   href=\"" , "\"" , fullTextHtml);
					if(pdfUrls.size() > 0){
//						File file = new File("citationDownload\\"+fileName+".pdf");
//						if(file.exists()){
//							file.delete();
//						}
						HTTPUtils.downloadFile(pdfUrls.get(0), sdCookie,sdCookieDomain, HTTPUtils.GET, null, file);
						Cell cell = row.createCell(32);
						cell.setCellType(Cell.CELL_TYPE_STRING);
						cell.setCellValue(file.getName());
					}
				}
			}else{
				// 检查有没有free PMC article
				List<String> pmcUrls = ParserUtils.findWithPrefixAndSuffix("<a class=\"status_icon\" href=\"" , "\"" , htmlStr);
				if(pmcUrls.size() > 0){
					String pmcUrl = pubmedDomain+pmcUrls.get(0).trim();
					Map<String,String> pmcArticleMap = HTTPUtils.getCookieUrlAndHtml(pmcUrl, pubmedCookie, pubmedCookieDomain, HTTPUtils.GET, null);
					if(pmcArticleMap.get("html") != null){
						htmlStr = pmcArticleMap.get("html");
						System.out.println(htmlStr);
						// /pmc/articles/PMC3893259/pdf/pone.0085759.pdf
						List<String> formats = ParserUtils.findWithPrefixAndSuffix("<h2>Formats:</h2><ul>" , "</ul>" , htmlStr);
						if(formats.size() > 0){
							List<String> pdfs = ParserUtils.findInContent("/pmc/articles/[^>]+?\\.pdf", formats.get(0));
							if(pdfs.size() > 0){
								String pdfUrl = pubmedDomain+pdfs.get(0);
	//							File file = new File("citationDownload\\"+fileName+".pdf");
	//							if(file.exists()){
	//								file.delete();
	//							}
								HTTPUtils.downloadFile(pdfUrl, pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
							
						}else{ // PMC页面有另一种新样式，epdf
							List<String> pdfLinks = ParserUtils.findWithPrefixAndSuffix("id=\"jr-pdf-sw\" href=\"" , "\"" , htmlStr);
							if(pdfLinks.size() > 0){
								HTTPUtils.downloadFile(pubmedDomain+pdfLinks.get(0), pubmedCookie,pubmedCookieDomain, HTTPUtils.GET, null, file);
								Cell cell = row.createCell(32);
								cell.setCellType(Cell.CELL_TYPE_STRING);
								cell.setCellValue(file.getName());
							}
						}
						
					}
				}
			}
			
			}catch(Exception e){
				e.printStackTrace();
				continue;
			}
		}
		
		System.out.println("读取完毕，生成结果文件");
		
		generateResult();
	}
	
	
	/**
	 * 把找到pdf的citation新增一列
	 */
	private static void generateResult(){
		File file = new File("citationDownload");
		List<String> fileNames = Arrays.asList(file.list());
		
		Workbook wb = null;
		File excelFile = new File("citation_所有产品.XLSX");
		try {
			wb = WorkbookFactory.create(excelFile);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheet("Sheet2");
		int rowCount = sheet.getLastRowNum();
		System.out.println("rowCount:" + rowCount);
		for (int i = 1; i <= rowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
		// for (int i = 1; i <= 5; i++) { // for test
			try{
				Row row = sheet.getRow(i);
				String fileName = "";
				if(row.getCell(3) != null){
					fileName = row.getCell(3).getStringCellValue(); // filename. 文章图片的文件名，去掉.gif，加上.pdf变成全文pdf 
					if(fileName.endsWith(".gif")){
						fileName = fileName.substring(0, fileName.length() - 4); // 去掉最后的.gif
					}else{
						continue;
					}
				}else{
					continue;
				}
				if(fileNames.contains(fileName+".pdf")){
					Cell cell = row.createCell(32);
					cell.setCellType(Cell.CELL_TYPE_STRING);
					cell.setCellValue(fileName+".pdf");
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		
		FileOutputStream fos = null;
		try {
			File resultFile = new File("citation_所有产品_result.XLSX");
			if(resultFile.exists()){
				resultFile.delete();
			}
			fos = new FileOutputStream(resultFile);
			wb.write(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private static void countInvalidRow(){
		Workbook wb = null;
		File excelFile = new File("citation_所有产品.XLSX");
		try {
			wb = WorkbookFactory.create(excelFile);
		} catch (InvalidFormatException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Sheet sheet = wb.getSheet("Sheet2");
		int rowCount = sheet.getLastRowNum();
		System.out.println("rowCount:" + rowCount);
		int invalidCount = 0;
		DecimalFormat df = new DecimalFormat("#"); // 转换成整型
		for (int i = 1; i <= rowCount; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
		// for (int i = 2426; i <= 2426; i++) { // row从0开始 , 实际excel从第二行开始，第一行为表头
		// for (int i = 1; i <= 5; i++) { // for test

			Row row = sheet.getRow(i);
			String pmid = "";
			if(row.getCell(19) != null){  // PMID，形如24454929
				continue;
			}
			if(row.getCell(20) != null){
				String url = row.getCell(20).getStringCellValue(); // url，形如http://www.ncbi.nlm.nih.gov/pubmed/24454929?dopt=Abstract
				if(url.startsWith("http://www.sciencedirect.com/")){
					continue;
				}
			}
			invalidCount ++;
		}
		
		System.out.println("invalidCount:" + invalidCount);
	}
	
	/**
	 * 登录keti8 ， 设置keti8Cookie
	 */
	private static void loginKeti8() {
		// 登录 www.keti8.com
		Map<String, String> params = new IdentityHashMap<String, String>();
		params.put("ecmsfrom", "");
		params.put("enews", "login");
		params.put("username", "selleck");
		params.put("password", "selleck2015");
		params.put("lifetime", "0");
		params.put("%E7%99%BB%E5%BD%95", "");
		params.put("ecmsfrom", "http://www.keti8.com/");

		Map<String, String> httpMap = null;
		httpMap = HTTPUtils.getCookieUrlAndHtml("http://www.keti8.com/e/member/doaction.php", null, null,HTTPUtils.POST, params);
		if (httpMap.get("html") == null || httpMap.get("html").isEmpty()) {
			System.out.println("登录www.keti8.com失败");
			return;
		}
		keti8Cookie = httpMap.get("cookie");
	}
	
	/**
	 * 选择pubmed通道
	 *  http://www.keti8.com/e/action/ListInfo/?classid=76
	 */
	private static void loginPubmed() {
		String url = "";
		String htmlStr = "";
		List<String> regexResult = null;
		Map<String, String> httpMap = null;
		// pubmed通道一 http://www.keti8.com/e/action/ListInfo/?classid=76
		// 访问pubmed通道一
		HTTPUtils.REFERER = "http://www.keti8.com/e/action/ListInfo/?classid=3";
		httpMap = HTTPUtils.getCookieUrlAndHtml("http://www.keti8.com/e/action/ListInfo/?classid=76",keti8Cookie, "www.keti8.com", HTTPUtils.GET, null);
		if (httpMap.get("html") == null || httpMap.get("html").isEmpty()) {
			System.out.println("连接pubmed通道一失败1");
			return;
		}
		HTTPUtils.REFERER = null;
		htmlStr = httpMap.get("html");
		regexResult = ParserUtils.findWithPrefixAndSuffix("location='", "'",htmlStr);
		if (regexResult.size() > 0) {
			url = regexResult.get(0);
		} else {
			System.out.println("连接pubmed通道一失败2");
			return;
		}
		httpMap = HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET,null);
		if (httpMap.get("html") == null || httpMap.get("html").isEmpty()) {
			System.out.println("访问Pubmed主页失败");
			return;
		}
		htmlStr = httpMap.get("html");
		pubmedCookie = httpMap.get("cookie");

		url = httpMap.get("url");
		pubmedDomain = url.replaceAll("http[s]?://", "");
		pubmedDomain = pubmedDomain.substring(0, pubmedDomain.indexOf("/"));
		pubmedCookieDomain = pubmedDomain.replaceFirst("www-ncbi-nlm-nih-gov.proxy[^\\.]?", "");
		pubmedDomain = "http://" + pubmedDomain;
	}
}
