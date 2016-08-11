package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.WOS;
import selleck.email.update.tools.ParserUtils;

public class WOSArticleParser {
	public static final char CHAR_10 = (char)10; // asc码值是10的字符，是个换行符之类的东西
	public static final String SPACE = "\\p{Space}*"; //  \p{Space}	A whitespace character: [ \t\n\x0B\f\r]
	
	/**
	 * 解析wos 文章页面，抓取wos各个属性。抓取的规则与火车头抓取wos任务保持一致。
	 * @param htmlStr
	 * @return 从页面抓取各个属性后生成的WOS对象
	 */
	public static WOS parseWOSFromHTML(String htmlStr){
		WOS wos = new WOS();
		
		//摘要
		List<String> abses = ParserUtils.findWithPrefixAndSuffix("<div class=\"title3\">摘要</div>"+SPACE+"<p class=\"FR_field\">" , "</p>"+SPACE+"</div>" , htmlStr);
		// List<String> abses = ParserUtils.findWithPrefixAndSuffix(">摘要" , "</p>" , htmlStr);
		String abs = "";
		if(abses.size() > 0){
			abs = abses.get(0);
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
			abs = ParserUtils.trim(abs);
		}
		wos.setAbs(abs);
		
		// 地址
		List<String> addresses = ParserUtils.findWithPrefixAndSuffix("<td class=\"fr_address_row2\"><a name=\"addressWOS.*?>" , "</td>" , htmlStr);
		StringBuffer address = new StringBuffer();
		for (String a : addresses) {
			a = a.replaceAll("</preferred_org>", "|");
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			address.append(a);
		}
		wos.setAddresses(address.toString());
		
		// 作者
		List<String> authors = ParserUtils.findWithPrefixAndSuffix("<span class=\"FR_label\">作者:</span>" , "</p>" , htmlStr);
		StringBuffer author = new StringBuffer();
		for (String a : authors) {
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			a = a.replaceAll("\\s", "");
			a = a.replaceAll(";", "|");
			a = a.replaceAll("更多内容更少内容", "");
			author.append(a).append("|");
		}
		wos.setAuthors(author.toString());
		
		// 分类
		List<String> classifications = ParserUtils.findWithPrefixAndSuffix("Web of Science 类别:</span>" , "</p>" , htmlStr);
		String classification = "";
		if(classifications.size() > 0){
			classification = classifications.get(0);
			classification = Jsoup.parse(classification).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			classification = StringEscapeUtils.unescapeHtml4(classification); // 去转义字符，  &gt;  转换成>符号
			classification = ParserUtils.trim(classification);
		}
		wos.setClassification(classification);
		
		// 通讯地址
		List<String> correspondingAddresses = ParserUtils.findWithPrefixAndSuffix("\\(通讯作者\\)<table class=\"FR_table_noborders\" cellspacing=\"0\" cellpadding=\"0\" RULES=\"NONE\" BORDER=\"0\">"+SPACE+"<tr>" , "</tr>" , htmlStr);
		String correspondingAddress = "";
		if(correspondingAddresses.size() > 0){
			correspondingAddress = correspondingAddresses.get(0);
			correspondingAddress = correspondingAddress.replaceAll("</preferred_org>", "|");
			correspondingAddress = Jsoup.parse(correspondingAddress).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			correspondingAddress = StringEscapeUtils.unescapeHtml4(correspondingAddress); // 去转义字符，  &gt;  转换成>符号
			correspondingAddress = ParserUtils.trim(correspondingAddress);
		}
		wos.setCorrespondingAddress(correspondingAddress);
		
		// 通讯作者
		List<String> correspondingAuthors = ParserUtils.findWithPrefixAndSuffix("<span class=\"FR_label\">通讯作者地址:.*?"+SPACE+"</span>" , "\\(通讯作者\\)" , htmlStr);
		String correspondingAuthor = "";
		if(correspondingAuthors.size() > 0){
			correspondingAuthor = correspondingAuthors.get(0);
			correspondingAuthor = Jsoup.parse(correspondingAuthor).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			correspondingAuthor = StringEscapeUtils.unescapeHtml4(correspondingAuthor); // 去转义字符，  &gt;  转换成>符号
			correspondingAuthor = ParserUtils.trim(correspondingAuthor);
		}
		wos.setCorrespondingAuthor(correspondingAuthor);
		
		// 邮件地址
		List<String> emails = ParserUtils.findWithPrefixAndSuffix("电子邮件地址:</span>" , "</p>" , htmlStr);
		String email = "";
		if(emails.size() > 0){
			email = emails.get(0);
			email = Jsoup.parse(email).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			email = ParserUtils.trim(email);
		}
		wos.setEmail(email);
		
		wos.setHaveRead((byte)0);
		
		// 关键词
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix("作者关键词:</span>" , "</p>" , htmlStr);
		String keyword = "";
		if(keywords.size() > 0){
			keyword = keywords.get(0);
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = ParserUtils.trim(keyword);
		}
		wos.setKeyword(keyword);
		
		// keyword plus
		List<String> keywordPluses = ParserUtils.findWithPrefixAndSuffix("KeyWords Plus:</span>" , "</p>" , htmlStr);
		String keywordPlus = "";
		if(keywordPluses.size() > 0){
			keywordPlus = keywordPluses.get(0);
			keywordPlus = Jsoup.parse(keywordPlus).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keywordPlus = ParserUtils.trim(keywordPlus);
		}
		wos.setKeywordPlus(keywordPlus);
		
		// 出版日期
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix("出版年:</span>"+SPACE+"<value>" , "</p>" , htmlStr);
		String pDate = "";
		if(pDates.size() > 0){
			pDate = pDates.get(0);
			pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符，  &gt;  转换成>符号
			pDate = ParserUtils.trim(pDate);
		}
		wos.setpDate(pDate);
		
		// 研究方向
		List<String> researches = ParserUtils.findWithPrefixAndSuffix("研究方向:</span>" , "</p>" , htmlStr);
		String research = "";
		if(researches.size() > 0){
			research = researches.get(0);
			research = Jsoup.parse(research).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			research = StringEscapeUtils.unescapeHtml4(research); // 去转义字符，  &gt;  转换成>符号
			research = ParserUtils.trim(research);
		}
		wos.setResearch(research);
		
		// 发布期刊
		List<String> sourcePublications = ParserUtils.findWithPrefixAndSuffix("<p class=\"sourceTitle\">"+SPACE+"<value>" , "</value>" , htmlStr);
		String sourcePublication = "";
		if(sourcePublications.size() > 0 ){
			sourcePublication = sourcePublications.get(0);
			sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			sourcePublication = StringEscapeUtils.unescapeHtml4(sourcePublication); // 去转义字符，  &gt;  转换成>符号
			sourcePublication = ParserUtils.trim(sourcePublication);
		}
		wos.setSourcePublication(sourcePublication);
		
		// 标题
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<div xmlns:ts=\"http://ts.thomson.com/framework/xml/transform\" xmlns:set=\"http://exslt.org/sets\" xmlns:bean=\"http://ts.thomson.com/ua/bean\" class=\"title\">"+SPACE+"<value>" , "</value>"+SPACE+"</div>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		wos.setTitle(title);
		
		
		// 文献类型 article or review or letter
		List<String> types = ParserUtils.findWithPrefixAndSuffix("文献类型:</span>" , "</p>" , htmlStr);
		String type = "";
		if(types.size() > 0){
			type = types.get(0);
			type = Jsoup.parse(type).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			type = StringEscapeUtils.unescapeHtml4(type); // 去转义字符，  &gt;  转换成>符号
			type = ParserUtils.trim(type);
		}
		wos.setType(type);
		
		
		return wos;
	}
	
	
	public static void main(String[] args) throws IOException{
		FileReader reader = new FileReader ("e:\\aaa.html");
		StringBuilder sb = new StringBuilder();
	    BufferedReader br = new BufferedReader(reader);
	    String line;
	    while ( (line=br.readLine()) != null) {
	      sb.append(line);
	    }
	    String htmlStr = sb.toString();
	    
//	    List<String> correspondingAddresses = ParserUtils.findWithPrefixAndSuffix("\\(通讯作者\\)<table class=\"FR_table_noborders\" cellspacing=\"0\" cellpadding=\"0\" RULES=\"NONE\" BORDER=\"0\"><tr>" , "</tr>" , htmlStr);
//		String correspondingAddress = "";
//		if(correspondingAddresses.size() > 0){
//			correspondingAddress = correspondingAddresses.get(0);
//			correspondingAddress = correspondingAddress.replaceAll("</preferred_org>", "|");
//			correspondingAddress = Jsoup.parse(correspondingAddress).text(); // 去html标签 ,去换行\t\n\x0B\f\r
//			correspondingAddress = StringEscapeUtils.unescapeHtml4(correspondingAddress); // 去转义字符，  &gt;  转换成>符号
//			correspondingAddress = ParserUtils.trim(correspondingAddress);
//		}
//		
//	    System.out.print(correspondingAddress);
//	    
//	    br.close();
	    
	    Pattern p = Pattern.compile("http(s)?://[^\"]+?qe2a\\-proxy\\.mun\\.ca[^\"]*?");
		Matcher m = p.matcher(htmlStr);
		StringBuffer sbb = new StringBuffer();
		while (m.find()) {
			System.out.println(m.group());
			m.appendReplacement(sbb, "wechat.selleckchem.com" + "/su?u=" + m.group().replaceAll("qe2a-proxy.mun.ca", "wechat.selleckchem.com"));
			// System.out.println(sb.toString());
		}
		m.appendTail(sbb);
		System.out.println(sbb.toString());
	}

}
