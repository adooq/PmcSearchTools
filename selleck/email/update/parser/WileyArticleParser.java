package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.Wiley;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class WileyArticleParser {
	/* 在WileyParse里再去人名头衔
	 * 
	public static final String[] TITLES = {"RM","BSN","MSN","MD","RN","Associate Professor","Assistant Professor","MS","PhD","MSc","DDS","Professor",
		"MSD","Dentist","FRCP","FRCPCH","FRCPC","RPh","FACC","FAHA","MPH","MBA","MDS","BDS","EdD","MSW","DACVECC","DVM","MRCP","Vice President",
		"DACVIM","DACVP","MRCVS","BVetMed","DACVAA","FACS","Director","Editor","Issue Editor","Guest Editor","Research Scientist","Dr","Prof","MBBS",
		"BSc","BM","MA","FRCPA","FRACP","DO","DSc","DM","MRCPsych","Coordinator","secretary","President","PD","Master","for"}; // 人名里的头衔
	public static String TITLES_REX = ""; // 去除人名里的头衔的正则表达式
	
	// 给TITLES_REX赋值，形如(?i)(\bR\.?M?(\.|\b))|(\bB\.?S\.?N?(\.|\b))|(\bM\.?S\.?N?(\.|\b))
	static {
		StringBuffer titleSB = new StringBuffer("(?i)"); // (?i)表示正则大小写不敏感
		for(String t : TITLES){
			titleSB.append("(\\b");
			char[] chars = t.toCharArray();
			for(char c : chars){
				titleSB.append(c).append("\\.?");
			}
			titleSB.delete(titleSB.length()-3 , titleSB.length());  // 去掉最后的\.?
			titleSB.append("(\\.|\\b))").append("|");
		}
		titleSB.deleteCharAt(titleSB.length()-1); // 去掉最后一个|
		TITLES_REX = titleSB.toString();
	}
	*/
	
	/**
	 * 解析Wiley 文章页面，抓取Wiley各个属性。
	 * @param htmlStr
	 * @return 从页面抓取各个属性后生成的Wiley对象
	 */
	public final Wiley parseFromHTML(String htmlStr , Wiley wiley){		
		//摘要
		wiley.setAbs(parseAbstract(htmlStr));
		
		// 地址
		wiley.setAddresses(parseAddresses(htmlStr));
		
		// 作者
		wiley.setAuthors(parseAuthors(htmlStr));
		
		// 邮件地址
		wiley.setEmail(parseEmail(htmlStr));
		
		// 通讯地址，不直接在网页上查找通讯地址，难度较大。在后期解析时把email的归属者的地址作为通讯地址。
		// wiley.setCorrespondingAddress(parseCorrespondingAddress(htmlStr));
				
		// 通讯作者，不直接在网页上查找通讯地址，难度较大。在后期解析时把email的归属者作为通讯作者。
		// wiley.setCorrespondingAuthor(parseCorrespondingAuthor(htmlStr));
		
		wiley.setHaveRead((byte)0);
		
		// 关键词
		wiley.setKeyword(parseKeyword(htmlStr));
		
		// 出版日期
		wiley.setPublicationDate(parsePDate(htmlStr));
		
		// 发布期刊
		wiley.setJournal(parseSourcePublication(htmlStr));
		
		// 标题
		wiley.setTitle(parseTitle(htmlStr));
		
		// 通讯信息
		wiley.setCorrespondingInfo(parseCorrespondingInfo(htmlStr));
		
		// 全文
		wiley.setFullText(this.parseFullText(htmlStr));
						
		// 参考资料
		wiley.setReference(this.parseReference(htmlStr));
			
		return wiley;
	}
	
	public Wiley parseFullTextFromHTML(String htmlStr , Wiley wiley){
		// 摘要，有的时候abstract页面没有明显的摘要标示，要再设一下
		wiley.setAbs(this.parseAbstract(htmlStr));
		
		// 全文
		wiley.setFullText(this.parseFullText(htmlStr));
				
		// 参考资料
		wiley.setReference(this.parseReference(htmlStr));
		
		return wiley;
	}

	
	public static void main(String[] args) throws IOException{
		testHtmlText();
		// testUrl();
	}
	
	public static void testHtmlText() throws IOException{
		FileReader reader = new FileReader ("e:\\aaa.html");
		StringBuilder sb = new StringBuilder();
	    BufferedReader br = new BufferedReader(reader);
	    String line;
	    while ( (line=br.readLine()) != null) {
	      sb.append(line);
	    }
	    String htmlStr = sb.toString();
		
	    WileyArticleParser wileyParser = new WileyArticleParser();
		Wiley wiley = new Wiley();
		wiley = wileyParser.parseFromHTML(htmlStr, wiley);
		System.out.println(wiley.getFullText());
		
		br.close();
	}
	
	public static void testUrl(){
		String articleUrl = "http://onlinelibrary.wiley.com/doi/10.1111/and.12122/abstract";
		Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null ,null, HTTPUtils.GET , null);
		String articleHtml = articleMap.get("html");
		WileyArticleParser wileyParser = new WileyArticleParser();
		Wiley wiley = new Wiley();
		wiley = wileyParser.parseFromHTML(articleHtml, wiley);
		wiley = wileyParser.parseFullTextFromHTML(articleHtml, wiley);
		System.out.println();
	}
	
	
	//摘要
	protected String parseAbstract(String htmlStr){
		List<String> abses = ParserUtils.findWithPrefixAndSuffix("<h3>Abstract</h3>" , "</p>" , htmlStr);
		String abs = "";
		if(abses.size() > 0){
			abs = abses.get(0);
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
			abs = ParserUtils.trim(abs);
		}
		
		if(abs.isEmpty()){
			abses = ParserUtils.findWithPrefixAndSuffix("<div id=\"abstract\">" , "</p>" , htmlStr);
			if(abses.size() > 0){
				abs = abses.get(0);
				abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
				abs = ParserUtils.trim(abs);
			}
		}
		
		return abs;
	}
	
	
	// 地址
	protected String parseAddresses(String htmlStr){
		List<String> addresses =  ParserUtils.findWithPrefixAndSuffix("<li class=\"affiliation\">" , "</li>" , htmlStr);
		StringBuffer address = new StringBuffer();
		for(String a : addresses){
			a = a.replaceAll("<span class=\"affiliationNumber\">", "[");
			a = a.replaceAll("</span>", "]");
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			address.append(a).append("|");
		}
		
		return address.toString();
	}
	
	
	// 作者
	protected String parseAuthors(String htmlStr){
		// <ol id=\"authors\">
		List<String> authorsOLs = ParserUtils.findWithPrefixAndSuffix("<ol id=\"authors\">" , "</ol>" , htmlStr);
		StringBuffer author = new StringBuffer();
		if(authorsOLs.size() > 0){
			String authorOL = authorsOLs.get(0);
			List<String> authors = ParserUtils.findWithPrefixAndSuffix("<li.*?>" , "</li>" , authorOL);
			for(String a : authors){
				a = a.trim();
				a = a.replaceAll(" and", "");
				a = a.replaceAll(",\\z", "");
				a = a.replaceAll("<sup>", "[");
				a = a.replaceAll("</sup>", "]");
				a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
				
				// 把作者名提取出来，然后把里面的头衔去掉
				int bracketIdx = a.lastIndexOf("[");
				String authorName = a; // 作者的姓名部分
				String restOfName = ""; // 作者的地址编号部分[1,3]
				if(bracketIdx != -1){
					authorName = a.substring(0, bracketIdx);
					restOfName = a.substring(bracketIdx);
				}
				authorName = authorName.replaceAll("", "");
				// authorName = removeTitle(authorName.trim()); // 在WileyParse里再去人名头衔
				authorName = ParserUtils.trim(authorName);
				restOfName = ParserUtils.trim(restOfName);
				author.append(authorName).append(restOfName).append("|");
			}
		}
		
		
		return author.toString();
	}
	
	// 通讯地址
	protected String parseCorrespondingAddress(String htmlStr){
		 List<String> cAddresses = ParserUtils.findWithPrefixAndSuffix("<strong>Corresponding author</strong>.*?:" , "Email:" , htmlStr);
		StringBuffer cAddress = new StringBuffer();
		for(String a : cAddresses){
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			cAddress.append(a).append("|");
		}
		
		return cAddress.toString();
	}
	
	
	// 通讯作者
	protected String parseCorrespondingAuthor(String htmlStr){
		List<String> correspondingAuthors = ParserUtils.findWithPrefixAndSuffix("<span class=\"contrib-email\" id=\"A.*?\">" , ":" , htmlStr);
		StringBuffer correspondingAuthor = new StringBuffer();
		for(String ca : correspondingAuthors){
			ca = ca.replaceAll(" and ", "|");
			ca = ca.replaceAll(" or ", "|");
			ca = Jsoup.parse(ca).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ca = StringEscapeUtils.unescapeHtml4(ca); // 去转义字符，  &gt;  转换成>符号
			ca = ParserUtils.trim(ca);
			correspondingAuthor.append(ca).append("|");
		}
		return correspondingAuthor.toString();
	}
	
	
	// 邮件地址
	protected String parseEmail(String htmlStr){
		StringBuffer emailSB = new StringBuffer();
		
		List<String> emails = ParserUtils.findWithPrefixAndSuffix("<a href=\"mailto:.+?>" , "</a>" , htmlStr);
		Set<String> emailSet = new HashSet<String>(); // 用来email 去重
		for(String email : emails){
			email = Jsoup.parse(email).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			email = StringEscapeUtils.unescapeHtml4(email); // 去转义字符，  &gt;  转换成>符号
			Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
			Matcher matcher = p.matcher(email);
			while (matcher.find()) {
				emailSet.add(matcher.group());
			}
		}
		emails = ParserUtils.findWithPrefixAndSuffix("<span class=\"email\">" , "</span>" , htmlStr);
		for(String email : emails){
			email = Jsoup.parse(email).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			email = StringEscapeUtils.unescapeHtml4(email); // 去转义字符，  &gt;  转换成>符号
			Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
			Matcher matcher = p.matcher(email);
			while (matcher.find()) {
				emailSet.add(matcher.group());
			}
		}
		for(String e : emailSet){
			emailSB.append(e).append("|");
		}
		
		try {
			return java.net.URLDecoder.decode(emailSB.toString(),"utf-8").replaceAll("‐", ""); // 去掉换行符
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	
	// 关键词
	protected String parseKeyword(String htmlStr){
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix("<ul class=\"keywordList\" id=\"abstractKeywords1\">" , "</ul>" , htmlStr);
		String keyword = "";
		if(keywords.size() > 0){
			keyword = keywords.get(0);
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = StringEscapeUtils.unescapeHtml4(keyword); // 去转义字符，  &gt;  转换成>符号
			keyword = ParserUtils.trim(keyword);
		}
		return keyword;
	}
	
	
	// 出版日期
	protected String parsePDate(String htmlStr){
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix("Article first published online:" , "</p>" , htmlStr);
		String pDate = "";
		if(pDates.size() > 0){
			pDate = pDates.get(0);
			pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符，  &gt;  转换成>符号
			pDate = ParserUtils.trim(pDate);
		}
		return pDate;
	}
	
	
	// 发布期刊
	protected String parseSourcePublication(String htmlStr){
		List<String> sourcePublications = ParserUtils.findWithPrefixAndSuffix("<h2 id=\"productTitle\">" , "</h2>" , htmlStr);
		String sourcePublication = "";
		if(sourcePublications.size() > 0){
			sourcePublication = sourcePublications.get(0);
			sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			sourcePublication = StringEscapeUtils.unescapeHtml4(sourcePublication); // 去转义字符，  &gt;  转换成>符号
			sourcePublication = ParserUtils.trim(sourcePublication);
		}
		return sourcePublication;
	}
	
	// 标题
	protected String parseTitle(String htmlStr){
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<h1 class=\"articleTitle\">" , "</h1>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		
		return title;
	}
	
	// 全文
	protected String parseFullText(String htmlStr){
		List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<div class=\"para\">", "</div>" , htmlStr);
		StringBuffer fullText = new StringBuffer();
		for(String ft : fullTexts){
			ft = Jsoup.parse(ft).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ft = StringEscapeUtils.unescapeHtml4(ft); // 去转义字符，  &gt;  转换成>符号
			ft = ParserUtils.trim(ft);
			fullText.append(ft).append(" ");
		}
		return fullText.toString();
	}
	
	// 参考资料
	protected String parseReference(String htmlStr){
		List<String> referrences = ParserUtils.findWithPrefixAndSuffix("<cite id=\"cit\\d+\">" , "</cite>" , htmlStr);
		StringBuffer referrence = new StringBuffer();
		for(String ref : referrences){
			ref = Jsoup.parse(ref).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ref = StringEscapeUtils.unescapeHtml4(ref); // 去转义字符，  &gt;  转换成>符号
			ref = ParserUtils.trim(ref);
			referrence.append(ref).append("|");
		}
		
		return referrence.toString();
		
	}
	
	// 通讯资料
	protected String parseCorrespondingInfo(String htmlStr){
		List<String> correspondingInfos = ParserUtils.findWithPrefixAndSuffix("<h4>Author Information</h4>" , "</div>" , htmlStr);
		String correspondingInfo = "";
		if(correspondingInfos.size() > 0){
			correspondingInfo = correspondingInfos.get(0);
			correspondingInfo = Jsoup.parse(correspondingInfo).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			correspondingInfo = StringEscapeUtils.unescapeHtml4(correspondingInfo); // 去转义字符，  &gt;  转换成>符号
			correspondingInfo = ParserUtils.trim(correspondingInfo);
		}
		return correspondingInfo;
	}
	
	/**
	 * 把人名中的头衔去掉
	 * @param authorName
	 * @return
	 */
	/* 在WileyParse里再去人名头衔
	private String removeTitle(String authorName){
		String[] names = authorName.split(TITLES_REX);
		for(String name : names){
			if(!ParserUtils.trim(name).isEmpty()){
				return name.trim();
			}
		}
		return "";
	}
	*/
}
