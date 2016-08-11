package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.ScienceDirect;
import selleck.email.update.tools.ParserUtils;

public class ScienceDirectArticleParser {
	public static final char CHAR_10 = (char)10; // asc码值是10的字符，是个换行符之类的东西
	public static final String SPACE = "\\p{Space}*"; //  \p{Space}	A whitespace character: [ \t\n\x0B\f\r]
	
	/**
	 * 抓取期刊名，因为期刊名和其他属性不在一个页面上，所以分开处理。
	 * @param htmlStr
	 * @param scienceDirect
	 * @return
	 */
	public static ScienceDirect parsePubFromHTML(String htmlStr , ScienceDirect scienceDirect){
		// 期刊名
		List<String> pubNames = ParserUtils.findWithPrefixAndSuffix("<a title=\"Go to " , " on ScienceDirect" , htmlStr);
		String pubName = "";
		if(pubNames.size() > 0){
			pubName = pubNames.get(0);
			pubName = Jsoup.parse(pubName).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pubName = StringEscapeUtils.unescapeHtml4(pubName); // 去转义字符，  &gt;  转换成>符号
			pubName = ParserUtils.trim(pubName);
		}
		scienceDirect.setSourcePublication(pubName);
		return scienceDirect;
	}
	
	/**
	 * 解析scienceDirect页面，抓取scienceDirect各个属性
	 * @param htmlStr
	 * @param scienceDirect
	 * @return 从页面抓取各个属性后的scienceDirect对象
	 */
	public static ScienceDirect parseFromHTML(String htmlStr , ScienceDirect scienceDirect){
		// 标题
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<h1 class=\"svTitle\".*?>" , "</h1>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		scienceDirect.setTitle(title);
		
		// 地址
		List<String> addresses = ParserUtils.findWithPrefixAndSuffix("<li id=\"af\\p{Alnum}+\">" , "</li>" , htmlStr);
		StringBuffer address = new StringBuffer();
		for(String add : addresses){
			add = add.replaceAll("<sup>", "[").replaceAll("</sup>", "]");
			add = Jsoup.parse(add).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			add = StringEscapeUtils.unescapeHtml4(add); // 去转义字符，  &gt;  转换成>符号
			add = ParserUtils.trim(add);
			address.append(add).append("|");
		}
		scienceDirect.setAddresses(address.toString());
		// 地址可能有多种形式
		if(scienceDirect.getAddresses().isEmpty()){
			addresses = ParserUtils.findWithPrefixAndSuffix("<ul class=\"affiliation authAffil\">" , "</ul>" , htmlStr);
			address = new StringBuffer();
			for(String add : addresses){
				add = add.replaceAll("<sup>", "[").replaceAll("</sup>", "]");
				add = Jsoup.parse(add).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				add = StringEscapeUtils.unescapeHtml4(add); // 去转义字符，  &gt;  转换成>符号
				add = ParserUtils.trim(add);
				address.append(add).append("|");
			}
		}
		scienceDirect.setAddresses(address.toString());
		
		// 作者和邮箱，作者和邮箱是对应出现的，一起处理 
		// 通讯作者和通讯地址，Springer没有特定的通讯作者和通讯地址，把第一个有邮箱地址的作者作为通讯作者，把通讯作者的第一个地址作为通讯地址
		// 作者形式  Naoyuki Horiguchi[1][2]|Miao Liang[2]|Mingchao Li[3]  后面是地址编号
		// 邮件形式  dubl@suda.edu.cn[1]|asddfl@suda.edu.cn[2]  后面是作者序号
		List<String> authors = ParserUtils.findWithPrefixAndSuffix("<li><a href=\"#\" class=\"authorName[^>]+?>" , "</li>" , htmlStr);
		StringBuffer author = new StringBuffer();
		StringBuffer email = new StringBuffer();
		int authorIndex = 1;
		for (String a : authors) {
			List<String> addressNo = ParserUtils.findWithPrefixAndSuffix("<a title=\"Affiliation: [^>]+?>","</a>",a); // 作者的地址编号
			StringBuffer addNoSB = new StringBuffer("[");
			for(String adn : addressNo){
				adn = Jsoup.parse(adn).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				addNoSB.append(adn).append(",");
			}
			int idx = addNoSB.lastIndexOf(",");
			if(idx != -1){
				addNoSB.deleteCharAt(addNoSB.length()-1); // 去掉最后一个,
			}
			addNoSB.append("]"); // [<sup>1</sup>,]
			
			String authorName = a.substring(0, a.indexOf("</a>"));
			authorName = Jsoup.parse(authorName).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			authorName = StringEscapeUtils.unescapeHtml4(authorName); // 去转义字符，  &gt;  转换成>符号
			authorName = ParserUtils.trim(authorName);
			
			List<String> emails = ParserUtils.findWithPrefixAndSuffix("href=\"mailto:","\"",a);
			if(emails.size() > 0){
				email.append(emails.get(0)).append("[").append(authorIndex).append("]").append("|");
				if(scienceDirect.getCorrespondingAuthor() == null || scienceDirect.getCorrespondingAuthor().isEmpty()){
					scienceDirect.setCorrespondingAuthor(authorName);
				}
				if((scienceDirect.getCorrespondingAddress() == null || scienceDirect.getCorrespondingAddress().isEmpty()) && (scienceDirect.getAddresses() != null && !scienceDirect.getAddresses().isEmpty())){
					String aId = addNoSB.substring(addNoSB.indexOf("[") + 1, addNoSB.indexOf(",")== -1 ? addNoSB.indexOf("]") : addNoSB.indexOf(","));  // 作者的第一个地址编号
					Pattern p = Pattern.compile("\\["+aId+"\\][^\\|]+",Pattern.CASE_INSENSITIVE);
					Matcher matcher = p.matcher(scienceDirect.getAddresses());
					if(matcher.find()){
						scienceDirect.setCorrespondingAddress(matcher.group().replaceFirst("\\["+aId+"\\]", "").trim());
					}else{
						if(scienceDirect.getAddresses().contains("|")){
							scienceDirect.setCorrespondingAddress(scienceDirect.getAddresses().split("\\|")[0].replaceFirst("\\[.+?\\]", "").trim());
						}
					}
				}
			}
			author.append(authorName + addNoSB.toString()).append("|");
			authorIndex++;
		}
		scienceDirect.setAuthors(author.toString());
		scienceDirect.setEmail(email.toString());
		
		scienceDirect.setHaveRead((byte)0);
		
		// 发表日期
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix("<dd class=\"miscInfo\" id=\"misc0010\">Published: " , "</dd>" , htmlStr);
		String pDate = "";
		if(pDates.size() > 0){
			pDate = pDates.get(0);
			pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符，  &gt;  转换成>符号
			pDate = ParserUtils.trim(pDate);
		}
		// 发表日期 在页面上有多种形式
		if(pDate.isEmpty()){
			pDates = ParserUtils.findWithPrefixAndSuffix("<dl class=\"articleDates\">\\p{Print}*?Available online ","</dd>" , htmlStr);
			pDate = "";
			if(pDates.size() > 0){
				pDate = pDates.get(0);
				pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符，  &gt;  转换成>符号
				pDate = ParserUtils.trim(pDate);
			}
		}
		scienceDirect.setpDate(pDate);
		
		// 摘要
		List<String> abss = ParserUtils.findWithPrefixAndSuffix("<div class=\"abstract svAbstract \" data-etype=\"ab\">" , "</div>" , htmlStr);
		String abs = "";
		if(abss.size() > 0){
			abs = abss.get(0);
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
			abs = ParserUtils.trim(abs);
		}
		scienceDirect.setAbs(abs);
		
		// 关键词
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix(">[Kk]ey[ ]*[Ww]ord[s]*</h2>" , "</ul>" , htmlStr);
		String keyword = "";
		if(keywords.size() > 0){
			keyword = keywords.get(0);
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = StringEscapeUtils.unescapeHtml4(keyword); // 去转义字符，  &gt;  转换成>符号
			keyword = ParserUtils.trim(keyword);
		}
		scienceDirect.setKeyword(keyword);
		
		// 参考资料
		List<String> references = ParserUtils.findWithPrefixAndSuffix("<ul class=\"reference\" id=\"bibsbref\\d+\">" , "</ul>" , htmlStr);
		StringBuffer reference = new StringBuffer();
		for(String ref : references){
			ref = ref.replaceAll("</li>", " ");
			ref = Jsoup.parse(ref).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ref = StringEscapeUtils.unescapeHtml4(ref); // 去转义字符，  &gt;  转换成>符号
			ref = ParserUtils.trim(ref);
			reference.append(ref).append("|");
		}
		scienceDirect.setReference(reference.toString());
		
		// 全文
		List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<p class=\"svArticle section\" id=\"\\p{Alnum}+\">" , "</p>" , htmlStr);
		StringBuffer fullText = new StringBuffer();
		for(String ft : fullTexts){
			ft = Jsoup.parse(ft).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ft = StringEscapeUtils.unescapeHtml4(ft); // 去转义字符，  &gt;  转换成>符号
			ft = ParserUtils.trim(ft);
			fullText.append(ft).append(" ");
		}
		scienceDirect.setFullText(fullText.toString());
		
		
		return scienceDirect;
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
	    
	    ScienceDirect scienceDirect = new ScienceDirect();
	    scienceDirect = ScienceDirectArticleParser.parseFromHTML(htmlStr, scienceDirect);
	    
	    br.close();
	}

}
