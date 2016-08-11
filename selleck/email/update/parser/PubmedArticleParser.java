package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.Pubmed;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

public class PubmedArticleParser {
	/**
	 * 解析pubmed 文章页面，抓取pubmed各个属性。
	 * @param htmlStr
	 * @return 从页面抓取各个属性后生成的pubmed对象
	 */
	public final Pubmed parseFromHTML(String htmlStr , Pubmed pubmed){
		// pmcUrl ，先判断是不是free PMC article，如果是的话作者名和邮箱去PMC里查，PMC里更详细。
		parsePMCUrl(htmlStr , pubmed);
							
		//摘要
		parseAbstract(htmlStr, pubmed);
		
		// 作者
		parseAuthors(htmlStr, pubmed);
		
		// 地址、邮箱、通讯地址、通讯作者
		parseAddressesEmailAndCA(htmlStr, pubmed);
		
		// 通讯地址
		// parseCorrespondingAddress(htmlStr , pubmed);
		
		// 通讯作者
		// pubmed.setCorrespondingAuthor(parseCorrespondingAuthor(htmlStr));
		
		// 邮件地址
		// parseEmail(htmlStr,pubmed);
		
		pubmed.setHaveRead((byte)0);
		
		// 关键词
		parseKeyword(htmlStr , pubmed);
		
		// 出版日期
		parsePDate(htmlStr,pubmed);
		
		// 发布期刊
		parseSourcePublication(htmlStr , pubmed);
		
		// 标题
		parseTitle(htmlStr , pubmed);
		
		// PMID
		parsePMID(htmlStr , pubmed);
		
		// 有一些属性在PMC里才有，去访问PMC里抓
		parsePMC(pubmed);
		
		// 不是PMC，但也是free article的，去全文页面里抓email
		if(pubmed.getEmail() == null || pubmed.getEmail().trim().isEmpty()){
			findEmailInFulltextPage(htmlStr , pubmed);
		}
		
		// 全文
		// pubmed.setFullText(parseFullText(htmlStr));
		
		// 参考资料
		// pubmed.setReferrence(parseReferrence(htmlStr));
		
		// 通讯信息
		// pubmed.setCorrespondingInfo(parseCorrespondingInfo(htmlStr));
		
		return pubmed;
	}
	
	/**
	 * 有些pubmed有pmc全文链接
	 * @param htmlStr
	 * @param pubmed
	 */
	protected void parsePMCUrl(String htmlStr , Pubmed pubmed){
		// <a class="status_icon" href="
		List<String> pmcUrls = ParserUtils.findWithPrefixAndSuffix("<a class=\"status_icon\" href=\"" , "\"" , htmlStr);
		if(pmcUrls.size() > 0){
			pubmed.setPmcUrl("http://www.ncbi.nlm.nih.gov"+pmcUrls.get(0).trim());
		}
	}
	
	
	protected void parsePMID(String htmlStr , Pubmed pubmed){
		// <dt>PMID:</dt> <dd>
		List<String> pmids = ParserUtils.findWithPrefixAndSuffix("<dt>PMID:</dt> <dd>" , "</dd>" , htmlStr);
		if(pmids.size() > 0){
			pubmed.setPmid(pmids.get(0).trim());
		}
	}
	
	
	//摘要
	protected void parseAbstract(String htmlStr , Pubmed pubmed){
		List<String> abses = ParserUtils.findWithPrefixAndSuffix("<h3>Abstract</h3>" , "</div>" , htmlStr);
		String abs = "";
		if(abses.size() > 0){
			abs = abses.get(0);
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符，  &gt;  转换成>符号
			abs = ParserUtils.trim(abs);
		}
		pubmed.setAbs(abs);
	}
	
	// 地址，邮箱 ，通讯地址，通讯作者
	protected void parseAddressesEmailAndCA(String htmlStr , Pubmed pubmed){
		List<String> addresses =  ParserUtils.findWithPrefixAndSuffix("<ul class=\"ui\\-ncbi\\-toggler\\-slave\">" , "</ul>" , htmlStr);
		if(addresses.size() == 0){
			pubmed.setEmail("");
			pubmed.setAddresses("");
			return;
		}
		addresses = ParserUtils.findWithPrefixAndSuffix("<li>" , "</li>" , addresses.get(0));
		StringBuffer address = new StringBuffer();
		StringBuffer emails = new StringBuffer();
		for(String a : addresses){
			a = a.replaceAll("<sup>", "[");
			a = a.replaceAll("</sup>", "]");
			String[] aArr = a.split("Electronic address:");
			a = aArr[0];
			a = a.replaceAll("\\|", " "); // 把源地址中带的竖线去掉，以免和用作分隔的竖线搞混。
			List<String> es = ParserUtils.findInContent(StringUtils.EMAIL_REGEX, a);
			if(es.size() > 0){
				a = a.replaceAll("\\s*"+es.get(0)+".*", "");
			}
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			address.append(a).append("|");
			
			if(aArr.length > 1 || es.size() > 0){ // 说明后面带了email
				String emailTemp = aArr.length > 1 ? aArr[1].trim() : es.get(0);
				if(emailTemp.endsWith(".")){ // 去掉最后的点
					emailTemp = emailTemp.substring(0, emailTemp.length() - 1);
				}
				emailTemp = a.substring(0,a.indexOf("]")+1) + emailTemp; // 开头加上编号
				emails.append(emailTemp).append("|");
				
				// 带Email的认为是通讯地址
				if(pubmed.getCorrespondingAddress() == null || pubmed.getCorrespondingAddress().trim().isEmpty()){
					pubmed.setCorrespondingAddress(a.replaceFirst("\\[\\d+\\]", ""));
				}
				
				// 这个地址的归属者认为是通讯作者
				if(pubmed.getCorrespondingAuthor() == null || pubmed.getCorrespondingAuthor().trim().isEmpty()){
					List<String> aids = ParserUtils.findInContent("\\[\\d+\\]", a);
					if(aids.size() > 0){
						String aid = aids.get(0);
						if(pubmed.getAuthors() != null && !pubmed.getAuthors().trim().isEmpty()){
							// Sarno G[1]|Sarno G[1]|Sarno G[1]|
							String[] authorArr = pubmed.getAuthors().split("\\|");
							for(String au : authorArr){
								List<String> auids = ParserUtils.findInContent("\\[[\\d,]+\\]", au);
								List<String> auidsList = Arrays.asList(auids.get(0).replaceAll("\\[", "").replaceAll("\\]", "").split(",")); // 作者的地址编号
								if(auids.size() > 0 && auidsList.contains(aid.replaceAll("\\[", "").replaceAll("\\]", ""))){
									pubmed.setCorrespondingAuthor(au.replaceAll("\\[[\\d,]+\\]", "").trim());
									break;
								}
							}
						}
					}
				}
			}
			
		}
		
		pubmed.setAddresses(address.toString());
		pubmed.setEmail(emails.toString());
	}
	
	
	// 作者
	protected void parseAuthors(String htmlStr, Pubmed pubmed){
		List<String> authors = ParserUtils.findWithPrefixAndSuffix("<div class=\"auths\">" , "\\.</div>" , htmlStr);
		if(authors.size() == 0){
			return;
		}
		String[] authorsArr = authors.get(0).split(", ");
		StringBuffer author = new StringBuffer();
		for(String a : authorsArr){
			a = a.replaceAll("</sup><sup>", "");
			a = a.replaceAll("<sup>", "[");
			a = a.replaceAll("</sup>", "]");
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			author.append(a).append("|");
		}
		pubmed.setAuthors(author.toString());
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
		
		return cAddress.toString(); // PLos ONE 没有通讯地址
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
	protected String parseEmail(String htmlStr , Pubmed pubmed){
		List<String> emails = ParserUtils.findWithPrefixAndSuffix("<a href=\"mailto:dev@null\" data-email=\".*?>" , "</a>" , htmlStr);
		StringBuffer email = new StringBuffer();
		for(String e : emails){
			e = Jsoup.parse(e).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			e = e.replaceAll("/at/", "@");
			e = e.replaceAll(";", "|");
			e = StringEscapeUtils.unescapeHtml4(e); // 去转义字符，  &gt;  转换成>符号
			e = ParserUtils.trim(e);
			email.append(e).append("|");
		}
		// return email.reverse().toString(); // pmc需要把邮箱地址倒过来
		return email.toString(); // 把邮箱地址倒过来的工作在ParsePMC里再做。
	}
	
	
	// 关键词
	protected void parseKeyword(String htmlStr , Pubmed pubmed){
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix("<h4>KEYWORDS: </h4><p>" , "</p>" , htmlStr);
		String keyword = "";
		if(keywords.size() > 0){
			keyword = keywords.get(0);
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = StringEscapeUtils.unescapeHtml4(keyword); // 去转义字符，  &gt;  转换成>符号
			keyword = ParserUtils.trim(keyword);
		}
		pubmed.setKeyword(keyword);
	}
	
	
	// 出版日期
	protected void parsePDate(String htmlStr , Pubmed pubmed){
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix("<div class=\"cit\">" , "</div>" , htmlStr);
		String pDate = "";
		if(pDates.size() > 0){
			pDate = pDates.get(0);
			pDates = ParserUtils.findWithPrefixAndSuffix("</a>" , "[\\.;:]" , pDate);
			if(pDates.size() > 0){
				pDate = pDates.get(0);
				pDate = ParserUtils.trim(pDate);
			}
		}
		pubmed.setpDate(pDate);
	}
	
	
	// 发布期刊
	protected void parseSourcePublication(String htmlStr , Pubmed pubmed){
		List<String> sourcePublications = ParserUtils.findWithPrefixAndSuffix("<div class=\"cit\">" , "</a>" , htmlStr);
		String sourcePublication = "";
		if(sourcePublications.size() > 0){
			sourcePublication = sourcePublications.get(0);
			sourcePublication = sourcePublication.substring(0, sourcePublication.length() - 1);
			sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			sourcePublication = StringEscapeUtils.unescapeHtml4(sourcePublication); // 去转义字符，  &gt;  转换成>符号
			sourcePublication = ParserUtils.trim(sourcePublication);
		}
		
		pubmed.setSourcePublication(sourcePublication);
	}
	
	
	// 标题
	protected void parseTitle(String htmlStr , Pubmed pubmed){
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<h1>" , "</h1>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = title.replaceAll("- PubMed - NCBI", "");
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		pubmed.setTitle(title);
	}
	
	
	// 全文
	protected String parseFullText(String htmlStr){
		List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<p id=\"__p.*?\" class=\"p p-first\">" , "<div id=\"__ref-listid.*?\" class=\"tsec sec\">" , htmlStr);
		String fullText = "";
		if(fullTexts.size() > 0){
			fullText = fullTexts.get(0);
			fullText = fullText.replaceAll("</h3>", " ");
			fullText = fullText.replaceAll("</h2>", " ");
			fullText = Jsoup.parse(fullText).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			fullText = StringEscapeUtils.unescapeHtml4(fullText); // 去转义字符，  &gt;  转换成>符号
			fullText = ParserUtils.trim(fullText);
		}
		
		return fullText;
	}
	
	
	
	// 参考资料
	protected String parseReferrence(String htmlStr){
		// List<String> referrences = ParserUtils.findWithPrefixAndSuffix("<li id=\"B1\">" , "</li>" , htmlStr);
		List<String> referrences = ParserUtils.findWithPrefixAndSuffix("<h2 class=\"head no_bottom_margin\" id=\"__ref-listid\\d+?title\">References</h2>" , "</div></div>" , htmlStr);
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
		List<String> correspondingInfos = ParserUtils.findWithPrefixAndSuffix(
				"<div class=\"fm\\-authors\\-info fm\\-panel hide half_rhythm\" id=\"id.*?_ai\" style=\"display:none\">" ,
					"<div class=\"togglers\">" , htmlStr);
		String correspondingInfo = "";
		if(correspondingInfos.size() > 0){
			correspondingInfo = correspondingInfos.get(0);
			correspondingInfo = correspondingInfo.replaceAll("<script.*?</script>", "");
			correspondingInfo = correspondingInfo.replaceAll("<sup>", "[");
			correspondingInfo = correspondingInfo.replaceAll("</sup>", "]");
			correspondingInfo = Jsoup.parse(correspondingInfo).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			correspondingInfo = StringEscapeUtils.unescapeHtml4(correspondingInfo); // 去转义字符，  &gt;  转换成>符号
			correspondingInfo = ParserUtils.trim(correspondingInfo);
		}
		
		return correspondingInfo;
	}
	
	/** 访问PMC页面，获得更详细的作者名，email，fulltext, reference
	 * @param pubmed
	 * @return pmc页面是否访问正常（可能被封IP）
	 */
	public boolean parsePMC(Pubmed pubmed){
		// 先看看有没有pmc的链接
		if(pubmed.getPmcUrl() != null && !pubmed.getPmcUrl().trim().isEmpty()){
			Map<String,String> pmcMap = HTTPUtils.getCookieUrlAndHtml(pubmed.getPmcUrl(), null ,null, HTTPUtils.GET , null);
			if(pmcMap.get("html") != null && !pmcMap.get("html").isEmpty() ){
				String pmcHTML =  pmcMap.get("html");
				if(pmcHTML.contains("Your access to PubMed Central has been blocked")){
					return false;
				}
				// 如果pubmed里没有email，去pmc里搜索email
				if(pubmed.getEmail() == null || pubmed.getEmail().trim().isEmpty()){
					List<String> emails = ParserUtils.findWithPrefixAndSuffix("<a href=\"mailto:dev@null\" data-email=\".*?>" , "</a>" , pmcHTML);
					StringBuffer email = new StringBuffer();
					for(String e : emails){
						e = Jsoup.parse(e).text(); // 去html标签 ,去换行\t\n\x0B\f\r
						e = e.replaceAll("/at/", "@");
						e = e.replaceAll(";", "|");
						e = StringEscapeUtils.unescapeHtml4(e); // 去转义字符，  &gt;  转换成>符号
						e = ParserUtils.trim(e);
						email.append(e).append("|");
					}
					pubmed.setEmail(email.reverse().toString()); // pmc需要把邮箱地址倒过来
				}
				
				// 覆盖作者名,PMC里的作者名更完整
				// pubmed   Abdou JP[1]|Braggin GA[1]|Luo Y[1]|Stevenson AR[1]|Chun D[1]|Zhang S[1]|
				List<String> authorsIds = ParserUtils.findInContent("(\\[[\\d,]+\\])?\\|", pubmed.getAuthors()); // 作者编号和竖线
				Iterator<String> iter = authorsIds.iterator();
				List<String> authors = ParserUtils.findWithPrefixAndSuffix("5D\">" , "</a>" , pmcHTML);
				StringBuffer author = new StringBuffer();
				for(String pmcAuthors : authors){
					pmcAuthors = Jsoup.parse(pmcAuthors).text(); // 去html标签 ,去换行\t\n\x0B\f\r
					pmcAuthors = StringEscapeUtils.unescapeHtml4(pmcAuthors); // 去转义字符，  &gt;  转换成>符号
					pmcAuthors = ParserUtils.trim(pmcAuthors);
					author.append(pmcAuthors);
					if(iter.hasNext()){
						author.append(iter.next());
					}
				}
				pubmed.setAuthors(author.toString());
				
				// 抓全文
				List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix("<p id=\"__p.*?\" class=\"p p-first\">" , "<div id=\"__ref-listid.*?\" class=\"tsec sec\">" , pmcHTML);
				String fullText = "";
				if(fullTexts.size() > 0){
					fullText = fullTexts.get(0);
					fullText = fullText.replaceAll("</h3>", " ");
					fullText = fullText.replaceAll("</h2>", " ");
					fullText = Jsoup.parse(fullText).text(); // 去html标签 ,去换行\t\n\x0B\f\r
					fullText = StringEscapeUtils.unescapeHtml4(fullText); // 去转义字符，  &gt;  转换成>符号
					fullText = ParserUtils.trim(fullText);
				}
				pubmed.setFullText(fullText);
				
				// reference
				List<String> references = ParserUtils.findWithPrefixAndSuffix("<h2 class=\"head no_bottom_margin\" id=\"__ref-listid\\d+?title\">References</h2>" , "</div></div>" , pmcHTML);
				StringBuffer reference = new StringBuffer();
				for(String ref : references){
					ref = Jsoup.parse(ref).text(); // 去html标签 ,去换行\t\n\x0B\f\r
					ref = StringEscapeUtils.unescapeHtml4(ref); // 去转义字符，  &gt;  转换成>符号
					ref = ParserUtils.trim(ref);
					reference.append(ref).append("|");
				}
				pubmed.setReference(reference.toString());
				
			}else{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 进入全文页面，可能各种各样的网站，抓取
	 * @param htmlStr
	 * @param pubmed
	 */
	protected void findEmailInFulltextPage(String htmlStr , Pubmed pubmed){
		// 暂时先只抓plos one
		if(pubmed.getSourcePublication() == null || !pubmed.getSourcePublication().equals("PLoS One")){
			return;
		}
		
		List<String> links = ParserUtils.findWithPrefixAndSuffix("<div class=\"icons portlet\">","</a>",htmlStr);
		if(links.size() > 0){
			String link = links.get(0);
			List<String> hrefs = ParserUtils.findWithPrefixAndSuffix("href=\"","\"",link);
			if(hrefs.size() > 0){
				Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(hrefs.get(0), null ,null, HTTPUtils.GET , null);
				if(articleMap.get("html") != null){
					String articleHtml = articleMap.get("html");
					// System.out.println(articleHtml);
					List<String> emails = null;
					if(pubmed.getSourcePublication().equals("PLoS One")){ // 貌似plos one都没有email，量又特别大，所以特别抓一下
						emails = ParserUtils.findWithPrefixAndSuffix("<span class=\"email\">\\* E\\-mail:</span>","</a>",articleHtml);
					}else{ // 其他的期刊暂时先不抓
						// emails = ParserUtils.findInContent(StringUtils.EMAIL_REGEX, articleHtml);
					}
					if(emails != null && emails.size() > 0){
						String email = emails.get(0);
						email = Jsoup.parse(email).text(); // 去html标签 ,去换行\t\n\x0B\f\r
						email = StringEscapeUtils.unescapeHtml4(email); // 去转义字符，  &gt;  转换成>符号
						pubmed.setEmail(email);
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException{
		// testHtmlText();
		testUrl();
	}
	
	public static void testHtmlText() throws IOException{
		FileReader reader = new FileReader ("e:\\bbb.html");
		StringBuilder sb = new StringBuilder();
	    BufferedReader br = new BufferedReader(reader);
	    String line;
	    while ( (line=br.readLine()) != null) {
	      sb.append(line);
	    }
	    String htmlStr = sb.toString();
		
	    PubmedArticleParser pmcParser = new PubmedArticleParser();
		String ref = pmcParser.parseFullText(htmlStr);
		
		br.close();
		
		System.out.println(ref);
	}
	
	public static void testUrl(){
		String articleUrl = "http://www.ncbi.nlm.nih.gov/pubmed/26056919";
		Map<String,String> articleMap = HTTPUtils.getCookieUrlAndHtml(articleUrl, null ,null, HTTPUtils.GET , null);
		String articleHtml = articleMap.get("html");
		PubmedArticleParser pubmedParser = new PubmedArticleParser();
		Pubmed Pubmed = new Pubmed();
		pubmedParser.parseFromHTML(articleHtml, Pubmed);
		System.out.println("ref: "+Pubmed);
	}
}
