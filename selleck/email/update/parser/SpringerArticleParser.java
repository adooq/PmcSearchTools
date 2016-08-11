package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.Springer;
import selleck.email.update.tools.ParserUtils;

public class SpringerArticleParser {
	public static final char CHAR_10 = (char)10; // asc码值是10的字符，是个换行符之类的东西
	public static final String SPACE = "\\p{Space}*"; //  \p{Space}	A whitespace character: [ \t\n\x0B\f\r]
	
	/**
	 * 解析Springer 文章页面，抓取Springer各个属性。
	 * @param htmlStr
	 * @param springer
	 * @return 从页面抓取各个属性后生成的Springer对象
	 */
	public static Springer parseFromHTML(String htmlStr , Springer springer){
		
		//摘要
		List<String> abses = ParserUtils.findWithPrefixAndSuffix("<h2 class=\"Heading\">Abstract</h2>" , "</section>" , htmlStr);
		String abs = "";
		if(abses.size() > 0){
			abs = abses.get(0);
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
			abs = ParserUtils.trim(abs);
		}
		springer.setAbs(abs);
		
		// 地址 [1]State Key Laboratory of Desert and Oasis Ecology, China[1]Institute of Atmospheric Physics,Beijing, 100029, China[1]University of Chinese Academy of Sciences, Beijing, 100049, China[1]
		 List<String> addresses = ParserUtils.findWithPrefixAndSuffix("<span class=\"position\">" , "\\p{Space}</span>" , htmlStr);
		StringBuffer address = new StringBuffer();
		for (String a : addresses) {
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			a = a.replaceFirst("\\.", "]"); // 把地址开头的  1.  2.  3. 之类的改成  [1]  [2]  [3]
			address.append("[").append(a);
		}
		springer.setAddresses(address.toString());
		
		// 作者和邮箱，Springer的作者和邮箱是对应出现的，一起处理 
		// 通讯作者和通讯地址，Springer没有特定的通讯作者和通讯地址，把第一个有邮箱地址的作者作为通讯作者，把通讯作者的第一个地址作为通讯地址
		// 作者形式  Naoyuki Horiguchi[1][2]|Miao Liang[2]|Mingchao Li[3]  后面是地址编号
		// 邮件形式  dubl@suda.edu.cn[1]|asddfl@suda.edu.cn[2]  后面是作者序号
		List<String> authors = ParserUtils.findWithPrefixAndSuffix("<li itemprop=\"author\" itemscope=\"itemscope\" itemtype=\"http://schema.org/Person\">" , "</li>" , htmlStr);
		StringBuffer author = new StringBuffer();
		StringBuffer email = new StringBuffer();
		int authorIndex = 1;
		for (String a : authors) {
			List<String> emails = ParserUtils.findWithPrefixAndSuffix("<a class=\"envelope\" href=\"mailto:","\"",a);
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			a = a.replaceAll("\\(", "[");
			a = a.replaceAll("\\)", "]");
			if(emails.size() > 0){
				email.append(emails.get(0)).append("[").append(authorIndex).append("]").append("|");
				if(springer.getCorrespondingAuthor() == null || springer.getCorrespondingAuthor().isEmpty()){
					springer.setCorrespondingAuthor(a.replaceAll("\\[\\d+\\]", "").trim());
				}
				if((springer.getCorrespondingAddress() == null || springer.getCorrespondingAddress().isEmpty()) && (springer.getAddresses() != null && !springer.getAddresses().isEmpty())){
					int aId = Integer.valueOf(a.substring(a.indexOf("[") + 1, a.indexOf("]")));  // 作者的第一个地址编号
					Pattern p = Pattern.compile("\\["+aId+"\\][^\\[]+",Pattern.CASE_INSENSITIVE);
					Matcher matcher = p.matcher(springer.getAddresses());
					if(matcher.find()){
						springer.setCorrespondingAddress(matcher.group().replaceFirst("\\[\\d+\\]", "").trim());
					}
				}
			}
			author.append(a).append("|");
			authorIndex++;
		}
		springer.setAuthors(author.toString());
		springer.setEmail(email.toString());
		
		// 分类
//		List<String> classifications = ParserUtils.findWithPrefixAndSuffix("Web of Science 类别:</span>" , "</p>" , htmlStr);
//		String classification = "";
//		if(classifications.size() > 0){
//			classification = classifications.get(0);
//			classification = Jsoup.parse(classification).text(); // 去html标签 ,去换行\t\n\x0B\f\r
//			classification = StringEscapeUtils.unescapeHtml4(classification); // 去转义字符，  &gt;  转换成>符号
//			classification = ParserUtils.trim(classification);
//		}
//		springer.setClassification(classification);
		
		
		// 通讯作者   Springer没有特定的通讯作者 ,把第一个有邮箱地址的作者作为通讯作者
//		String correspondingAuthor = "";
//		if(springer.getAuthors() != null && !springer.getAuthors().isEmpty()){
//			correspondingAuthor = springer.getAuthors().split("\\|")[0];
//			correspondingAuthor = correspondingAuthor.replaceAll("\\[\\d+\\]", "").trim();
//		}
//		springer.setCorrespondingAuthor(correspondingAuthor);
		
		// 通讯地址  Springer没有特定的通讯地址 ,把通讯作者的第一个地址作为通讯地址
//		String correspondingAddress = "";
//		if(springer.getAddresses() != null && !springer.getAddresses().isEmpty()){
//			correspondingAddress = springer.getAddresses().split("\\[\\d+\\]")[1].trim();
//		}
//		springer.setCorrespondingAddress(correspondingAddress);
		
		// 关键词
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix("<ul class=\"abstract-keywords\">" , "</ul>" , htmlStr);
		String keyword = "";
		if(keywords.size() > 0){
			keyword = keywords.get(0);
			keyword = keyword.replaceAll("</li>", ";");
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = ParserUtils.trim(keyword);
		}
		springer.setKeyword(keyword);
		
		// keyword plus  实际页面上是Topic，类似keywordPlus
		List<String> keywordPluses = ParserUtils.findWithPrefixAndSuffix("<ul class=\"abstract-about-subject\">" , "</ul>" , htmlStr);
		String keywordPlus = "";
		if(keywordPluses.size() > 0){
			keywordPlus = keywordPluses.get(0);
			keywordPlus = keywordPlus.replaceAll("</a>", ";");
			keywordPlus = Jsoup.parse(keywordPlus).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keywordPlus = ParserUtils.trim(keywordPlus);
		}
		springer.setKeywordPlus(keywordPlus);
		
		// 出版日期
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix("<span id=\"date\" itemprop=\"datePublished\">" , "</span>" , htmlStr);
		String pDate = "";
		if(pDates.size() > 0){
			pDate = pDates.get(0);
			pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符，  &gt;  转换成>符号
			pDate = ParserUtils.trim(pDate);
		}
		springer.setpDate(pDate);
		
		// 研究方向
		List<String> researches = ParserUtils.findWithPrefixAndSuffix("<ul class=\"abstract-about-industrysectors\">" , "</ul>" , htmlStr);
		String research = "";
		if(researches.size() > 0){
			research = researches.get(0);
			research = research.replaceAll("</a>", ";");
			research = Jsoup.parse(research).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			research = StringEscapeUtils.unescapeHtml4(research); // 去转义字符，  &gt;  转换成>符号
			research = ParserUtils.trim(research);
		}
		springer.setResearch(research);
		
		// 发布期刊
		List<String> sourcePublications = ParserUtils.findWithPrefixAndSuffix("<a id=\"abstract-about-publication\" href=\"/journal/\\d+\">" , "</a>" , htmlStr); // 期刊
		String sourcePublication = "";
		if(sourcePublications.size() > 0 ){
			sourcePublication = sourcePublications.get(0);
			sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			sourcePublication = StringEscapeUtils.unescapeHtml4(sourcePublication); // 去转义字符，  &gt;  转换成>符号
			sourcePublication = ParserUtils.trim(sourcePublication);
		}else{
			sourcePublications = ParserUtils.findWithPrefixAndSuffix("<dd id=\"abstract-about-publication\">" , "</a>" , htmlStr); // Book
			if(sourcePublications.size() > 0 ){
				sourcePublication = sourcePublications.get(0);
				sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				sourcePublication = StringEscapeUtils.unescapeHtml4(sourcePublication); // 去转义字符，  &gt;  转换成>符号
				sourcePublication = ParserUtils.trim(sourcePublication);
			}
		}
		springer.setSourcePublication(sourcePublication);
		
		// 标题
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<dd id=\"abstract-about-title\">" , "</dd>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		springer.setTitle(title);
		
		// 参考资料
		List<String> refs = ParserUtils.findWithPrefixAndSuffix("<span class=\"authors\">" , "</span>" , htmlStr);
		StringBuffer ref = new StringBuffer();
		for(String r : refs){
			r = Jsoup.parse(r).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			r = StringEscapeUtils.unescapeHtml4(r); // 去转义字符，  &gt;  转换成>符号
			r = ParserUtils.trim(r);
			ref.append(r).append("|");
		}
		springer.setReference(ref.toString());
		
		// 文献类型 Springer 都是Article
		springer.setType("Article");
		
		springer.setHaveRead((byte)0);
		
		
		return springer;
	}
	
	/**
	 * 从文章的全文页面找出全文
	 * @param htmlStr
	 * @param springer
	 * @return
	 */
	public static Springer parseFullTextFromHTML(String htmlStr , Springer springer){
		List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<div class=\"Fulltext\">","<!--3.18-->",htmlStr);
		String fullText = "";
		if(fullTexts.size() > 0){
			fullText = fullTexts.get(0);
			fullText = Jsoup.parse(fullText).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			fullText = StringEscapeUtils.unescapeHtml4(fullText); // 去转义字符，  &gt;  转换成>符号
			fullText = ParserUtils.trim(fullText);
		}
		springer.setFullText(fullText);
		return springer;
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
	    
	    List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<div class=\"Fulltext\">","<!--3.18-->",htmlStr);
		String fullText = "";
		if(fullTexts.size() > 0){
			fullText = fullTexts.get(0);
			fullText = Jsoup.parse(fullText).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			fullText = StringEscapeUtils.unescapeHtml4(fullText); // 去转义字符，  &gt;  转换成>符号
			fullText = ParserUtils.trim(fullText);
		}
		
	    System.out.println(fullText);
	    
	    br.close();
	}
	
}
