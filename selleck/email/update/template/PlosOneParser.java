package selleck.email.update.template;

import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.update.tools.ParserUtils;

public class PlosOneParser extends PMCBaseArticleParser {
	// 摘要
	protected String parseAbstract(String htmlStr) {
		List<String> abses = ParserUtils.findWithPrefixAndSuffix(
				"<div id=\"__abstractid.* lang=\"en\" class=\"tsec sec\">",
				"</div></div>", htmlStr);
		String abs = "";
		if (abses.size() > 0) {
			abs = abses.get(0);
			abs = abs.replaceAll("</h2>", " ");
			abs = abs.replaceAll("</h3>", " ");
			abs = Jsoup.parse(abs).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			abs = StringEscapeUtils.unescapeHtml4(abs); // 去转义字符， &gt; 转换成>符号
			abs = ParserUtils.trim(abs);
		}
		return abs;
	}

	// 地址
	protected String parseAddresses(String htmlStr) {
		List<String> addresses = ParserUtils.findWithPrefixAndSuffix(
				"<div class=\"fm-affl\">", "</div>", htmlStr);
		StringBuffer address = new StringBuffer();
		for (String a : addresses) {
			a = a.replaceAll("<sup>", "[ ");
			a = a.replaceAll("</sup>", " ]");
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符， &gt; 转换成>符号
			a = ParserUtils.trim(a);
			address.append(a).append("|");
		}

		return address.toString();
	}

	// 作者
	protected String parseAuthors(String htmlStr) {
		List<String> authors = ParserUtils.findWithPrefixAndSuffix("5D\">",
				"(<a|</div>)", htmlStr);
		StringBuffer author = new StringBuffer();
		for (String a : authors) {
			a = a.replaceAll("</a>,", "");
			a = a.replaceAll(" and ", "");
			a = a.replaceAll("</sup><sup>", "");
			a = a.replaceAll("<sup>", "[");
			a = a.replaceAll("</sup>", "]");
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符， &gt; 转换成>符号
			a = ParserUtils.trim(a);
			author.append(a).append("|");
		}
		return authors.toString();
	}

	// 通讯地址
	protected String parseCorrespondingAddress(String htmlStr) {
		return ""; // PLos ONE 没有通讯地址
	}

	// 通讯作者
	protected String parseCorrespondingAuthor(String htmlStr) {
		List<String> correspondingAuthors = ParserUtils
				.findWithPrefixAndSuffix("[^<^>]*?", "5D\">",
						"</a>[^a]*?<sup>\\*</sup>", htmlStr);
		StringBuffer correspondingAuthor = new StringBuffer();
		for (String ca : correspondingAuthors) {
			ca = ca.replaceAll("</a>", "");
			ca = ca.replaceAll("<sup>.*</sup>", "");
			ca = Jsoup.parse(ca).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ca = StringEscapeUtils.unescapeHtml4(ca); // 去转义字符， &gt; 转换成>符号
			ca = ParserUtils.trim(ca);
			correspondingAuthor.append(ca).append("|");
		}
		return correspondingAuthor.toString();
	}

	// 邮件地址
	protected String parseEmail(String htmlStr) {
		List<String> emails = ParserUtils.findWithPrefixAndSuffix(
				"<a href=\"mailto:dev@null\" data-email=\".*?>", "</a>",
				htmlStr);
		StringBuffer email = new StringBuffer();
		for (String e : emails) {
			e = Jsoup.parse(e).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			e = e.replaceAll("/at/", "@");
			e = e.replaceAll(";", "|");
			e = StringEscapeUtils.unescapeHtml4(e); // 去转义字符， &gt; 转换成>符号
			e = ParserUtils.trim(e);
			email.append(e).append("|");
		}
		return email.reverse().toString(); // pmc需要把邮箱地址倒过来
	}

	// 关键词
	protected String parseKeyword(String htmlStr) {
		List<String> keywords = ParserUtils.findWithPrefixAndSuffix(
				"Keywords: </strong><span class=\"kwd-text\">", "</span>",
				htmlStr);
		String keyword = "";
		if (keywords.size() > 0) {
			keyword = keywords.get(0);
			keyword = Jsoup.parse(keyword).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			keyword = StringEscapeUtils.unescapeHtml4(keyword); // 去转义字符， &gt;
																// 转换成>符号
			keyword = ParserUtils.trim(keyword);
		}
		return keyword;
	}

	// 出版日期
	protected String parsePDate(String htmlStr) {
		List<String> pDates = ParserUtils.findWithPrefixAndSuffix(
				"Published online", "</span>", htmlStr);
		String pDate = "";
		if (pDates.size() > 0) {
			pDate = pDates.get(0);
			pDate = Jsoup.parse(pDate).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			pDate = pDate.replaceAll(",", "");
			pDate = pDate.replaceAll("\\.", "");
			pDate = pDate.replaceAll(";", "");
			pDate = StringEscapeUtils.unescapeHtml4(pDate); // 去转义字符， &gt;
															// 转换成>符号
			pDate = ParserUtils.trim(pDate);
		}

		return pDate;
	}

	// 发布期刊
	protected String parseSourcePublication(String htmlStr) {
		List<String> sourcePublications = ParserUtils.findWithPrefixAndSuffix(
				"<span class=\"cit\">", "\\.", htmlStr);
		String sourcePublication = "";
		if (sourcePublications.size() > 0) {
			sourcePublication = sourcePublications.get(0);
			sourcePublication = sourcePublication.replaceAll("\\.", "");
			sourcePublication = Jsoup.parse(sourcePublication).text(); // 去html标签
																		// ,去换行\t\n\x0B\f\r
			sourcePublication = StringEscapeUtils
					.unescapeHtml4(sourcePublication); // 去转义字符， &gt; 转换成>符号
			sourcePublication = ParserUtils.trim(sourcePublication);
		}

		return sourcePublication;
	}

	// 标题
	protected String parseTitle(String htmlStr) {
		List<String> titles = ParserUtils.findWithPrefixAndSuffix(
				"<h1 class=\"content-title\".*?>", "</h1>", htmlStr);
		String title = "";
		if (titles.size() > 0) {
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符， &gt;
															// 转换成>符号
			title = ParserUtils.trim(title);
		}

		return title;
	}

	// 全文
	protected String parseFullText(String htmlStr) {
		List<String> fullTexts = ParserUtils.findWithPrefixAndSuffix(
				"<div id=\"s1\" class=\"tsec sec\">",
				"<div id=\"__ref-listid.*?\" class=\"tsec sec\">", htmlStr);
		String fullText = "";
		if (fullTexts.size() > 0) {
			fullText = fullTexts.get(0);
			fullText = fullText.replaceAll("</h3>", " ");
			fullText = fullText.replaceAll("</h2>", " ");
			fullText = Jsoup.parse(fullText).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			fullText = StringEscapeUtils.unescapeHtml4(fullText); // 去转义字符， &gt;
																	// 转换成>符号
			fullText = ParserUtils.trim(fullText);
		}

		return fullText;
	}

	// 参考资料
	protected String parseReferrence(String htmlStr) {
		List<String> referrences = ParserUtils.findWithPrefixAndSuffix(
				"<div class=\"ref-cit-blk half_rhythm\".*?>",
				"</span></span></div>", htmlStr);
		StringBuffer referrence = new StringBuffer();
		for (String ref : referrences) {
			ref = Jsoup.parse(ref).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			ref = StringEscapeUtils.unescapeHtml4(ref); // 去转义字符， &gt; 转换成>符号
			ref = ParserUtils.trim(ref);
			referrence.append(ref).append("|");
		}

		return referrence.toString();

	}

	// 通讯资料
	protected String parseCorrespondingInfo(String htmlStr) {
		List<String> correspondingInfos = ParserUtils
				.findWithPrefixAndSuffix(
						"<div class=\"fm\\-authors\\-info fm\\-panel hide half_rhythm\" id=\"id.*?_ai\" style=\"display:none\">",
						"<div class=\"togglers\">", htmlStr);
		String correspondingInfo = "";
		if (correspondingInfos.size() > 0) {
			correspondingInfo = correspondingInfos.get(0);
			correspondingInfo = correspondingInfo.replaceAll(
					"<script.*?</script>", "");
			correspondingInfo = correspondingInfo.replaceAll("<sup>", "[");
			correspondingInfo = correspondingInfo.replaceAll("</sup>", "]");
			correspondingInfo = Jsoup.parse(correspondingInfo).text(); // 去html标签
																		// ,去换行\t\n\x0B\f\r
			correspondingInfo = StringEscapeUtils
					.unescapeHtml4(correspondingInfo); // 去转义字符， &gt; 转换成>符号
			correspondingInfo = ParserUtils.trim(correspondingInfo);
		}

		return correspondingInfo;
	}

}
