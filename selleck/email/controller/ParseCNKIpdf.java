package selleck.email.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.ExtractText;

import common.handle.model.Criteria;
import selleck.email.pojo.CNKI;
import selleck.email.service.ICNKIService;
import selleck.email.service.impl.CNKIServiceImpl;

public class ParseCNKIpdf {
	private static ICNKIService cnkiService = new CNKIServiceImpl();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String pdfContent = parsePDF("6.pdf"); // for test pdfbox
//		System.out.println("pdfContent: "+pdfContent);
//		String emails = findEmails(pdfContent);
//		String abs = findAbstract(pdfContent);
//		System.out.println("emails: "+emails);
//		System.out.println("abs: "+abs);

		/*File pdfDir = new File(DownloadCNKIpdfMain.PDF_DIR);
		String[] fileNames = pdfDir.list();
		for (String s : fileNames) {
			String pdfContent = parsePDF(s);
			// String email = findEmail(result);
			// System.out.print(" email: "+email);
			String emails = findEmails(pdfContent);
			String abs = findAbstract(pdfContent);
			updateEmailAndAbsEn(Integer.valueOf(s.replaceAll("\\.pdf", "")),emails, abs);
		}*/
		
		//给数据库的数据进行抓取PDF里面的内容并且标记
		int idIndex = 1;
		int step = 1000;
		while(idIndex <= 92042){
    		Criteria criteria = new Criteria();
    		criteria.setWhereClause(" read_flag = '未分析' and id >= "+idIndex+" and id < "+(idIndex+step));
    		idIndex += step;
    		//criteria.put("readFlag", "未分析");
    		// criteria.setLimitNumber("1000");
    		List<CNKI> readList = cnkiService.selectByExample(criteria);
    		System.out.println("idIndex: "+idIndex);
    		for(CNKI example : readList){
    			String pdfName = example.getId()+".pdf";
    			if(!new File(DownloadCNKIpdfMain.PDF_DIR + pdfName).exists()){
    				continue;
    			}
    			String pdfContent = parsePDF(pdfName);
    			if(pdfContent==null) continue;
    			int id = example.getId();
    			String emails = findEmails(pdfContent);
    			String abs = findAbstract(pdfContent);
    			updateEmailAndAbsEn(id, emails, abs);
    		}
		}
	}

	/**
	 * 解析 pdf中的email和英文摘要
	 * 
	 * @param fileName
	 */
	private static String parsePDF(String fileName) {
		ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
		PrintStream stdout = System.out;
		System.setOut(new PrintStream(outBytes));
		String result = null;
		try {
			ExtractText.main(new String[] {
					DownloadCNKIpdfMain.PDF_DIR + fileName, "-console",
					"-encoding UTF-8" });
			result = outBytes.toString("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Restore stdout
			System.setOut(stdout);
		}
		return result;
	}
	

	private static String findEmails(String article) {
		StringBuffer sb = new StringBuffer();
		// System.out.println("result: " + result);
		// String regex =
		// "[\\w[.-]]+@[\\w[.-] ]+\\.[\\w ]*[,;\\)。）\\f\\n\\r\\t\\v]";
		String regex = "[\\w[.-] ]+@[\\w[.-] ]+\\.[\\w ]+";
		Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(article);
		boolean onlyOne = true;
		while (matcher.find()) {
			String email = matcher.group();
			if(email.matches("[\\d\\.@ ]+")){
				continue;
			}
			// System.out.print(matcher.group()+"|");
			if(onlyOne){
				onlyOne = false;
			}else{
				sb.append("|");
			}
			sb.append(email);
		}
		return sb.toString();
	}
	
	private static String findAbstract(String article){
		String s = new String();
		String regex = "[ ]*a[ ]*b[ ]*s[ ]*t[ ]*r[ ]*a[ ]*c[ ]*t\\W*";  // [ \f\n\r\t\v]
		String keyRegex = "[\\W]?k[ ]*e[ ]*y[ ]*w[ ]*o[ ]*r[ ]*d";  // [ \f\n\r\t\v]
		Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(article);
		if (matcher.find()) {
			 s = matcher.group();
			 s = article.substring(matcher.end());
			 pattern = Pattern.compile(keyRegex,Pattern.CASE_INSENSITIVE);
			 matcher = pattern.matcher(s);
			 if(matcher.find()){
				s = s.substring(0, matcher.start());
			 }
		}
		return s;
		
	}
	
	/**
	 * 更新数据cnki文章的email和英文摘要字段
	 * @param id  即pdf文件名 , e.g  1.pdf   12.pdf
	 * @param emails  形如abc@xxx.com|efd@yyy.com  一篇文章中可能找到多个email，用|分割
	 * @param abs 英文摘要
	 */
	private static void updateEmailAndAbsEn(int id,String emails,String abs){
		CNKI cnki = new CNKI();
		cnki.setId(id);
		cnki.setEmail(emails);
		cnki.setAbs_en(abs);
		cnki.setReadFlag("已分析");
		cnkiService.updateEmailAndEnAbs(cnki);
	}
}
