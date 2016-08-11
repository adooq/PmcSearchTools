package selleck.email.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.Wiley;
import selleck.email.service.IWileyService;
import selleck.email.service.impl.WileyServiceImpl;
import selleck.email.update.tools.ParserUtils;
import selleck.utils.Constants;

public class WileyParse extends AbstractParser {
	public static String SOURCE; // 解析search_wiley_by_publication

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/* ****** 开始新任务时，需要修改的参数 **** */
		DB = Constants.LIFE_SCIENCE_DB; // 进行操作的数据库
		// DB = Constants.MATERIAL_SCIENCE_DB; // 进行操作的数据库
		START_INDEX = 900006; // search_wos_by_publication表起始id
		MAX_ID = 1002470; // search_wos_by_publication表最后的id，通常是max(id) from
							// search_wos_by_publication
		STEP = 10000; // 一次查询出来的数量
		SOURCE = "SearchPublication"; // 解析search_wiley_by_publication
		// SOURCE = "SearchRecord"; // 解析search_record where SOURCEES = 'Wiley'
		// **************************************** //

		newEmailTableName = "selleck_edm_author_nodup_" + DB.split(" ")[0] + "_wiley_"
				+ new SimpleDateFormat("yyyyMM").format(new Date());

		new WileyParse().allProcess();
	}

	public void importEDMDB() {
		if (SOURCE.equals("SearchPublication")) {
			parseWileyFromSearchPublication();
		} else if (SOURCE.equals("SearchRecord")) {
			parseWileyFromSearchRecord();
		}
	}

	/**
	 * 
	 * 把 search_wiley_by_publication
	 * 表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseWileyFromSearchPublication() {
		IWileyService wileyService = new WileyServiceImpl(DB);
		// IWileyService wileyService = new
		// WileyServiceImpl(Constants.MATERIAL_SCIENCE_DB);
		int startIndex = START_INDEX;
		int step = STEP;
		while (startIndex <= MAX_ID) { // SELECT MAX(id) from
										// search_wiley_by_publication
			// while (startIndex == 107) { // for test
			Criteria criteria = new Criteria();
			criteria.setWhereClause(" have_read = 0 and title is not null and title != '' and id >= " + startIndex
					+ " and id < " + (startIndex + step));
			startIndex += step;
			List<Wiley> wileyList = wileyService.selectFromSearchPublication(criteria);
			// System.out.println("wosList "+wosList.size());
			if (wileyList.size() != 0) {
				for (Wiley wiley : wileyList) {
					try {
						// 作者、邮箱、全文不都为空才保存到数据库
						if ((wiley.getAuthors() == null || wiley.getAuthors().isEmpty())
								&& (wiley.getEmail() == null || wiley.getEmail().isEmpty())
								&& (wiley.getFullText() == null || wiley.getFullText().isEmpty())) {
							continue;
						}
						Article article = new Article();
						article.setAbs(wiley.getAbs());

						if (wiley.getEmail() != null && !wiley.getEmail().isEmpty()) {
							article.setEmail(wiley.getEmail());
						} else {
							article.setEmail("");
						}
						// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL
						// 字段长度，去掉后面超过长度的email
						while (article.getEmail().length() > 255) {
							article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
						}

						if (wiley.getCorrespondingAuthor() != null && !wiley.getCorrespondingAuthor().isEmpty()) {
							article.setCorrespondingAuthor(wiley.getCorrespondingAuthor().trim());
							article.setFullCorrespondingAuthor(wiley.getCorrespondingAuthor().trim());
						} else {
							article.setCorrespondingAuthor(""); // 在判断通讯作者的时候再赋值
						}
						// Wiley通讯地址在页面上比较难直接抓取，通过判断email的所有者的地址作为通讯地址
						article.setCorrespondingAddress("");// 在判断通讯作者的时候再赋值

						article.setKeyword(wiley.getKeyword());
						if (wiley.getPublicationDate() == null || wiley.getPublicationDate().length() > 30) {
							article.setpDate("");
						} else {
							article.setpDate(wiley.getPublicationDate());
						}
						article.setSource("Wiley");
						article.setSourcePublication(wiley.getJournal());
						article.setTitle(wiley.getTitle());
						article.setTitleIndex(wiley.getTitle().length() >= 250 ? wiley.getTitle().substring(0, 250)
								: wiley.getTitle()); // 取title前250字符作为索引
						article.setReferrence(wiley.getReference());
						article.setFullText(wiley.getFullText());
						article.setCorrespondingInfo(wiley.getCorrespondingInfo());
						article.setType("Article");

						Pattern p;
						Matcher matcher;
						Map<String, String> addressMap = new HashMap<String, String>();
						if (wiley.getAddresses() != null && !wiley.getAddresses().trim().equals("")) {
							// 地址，| 分隔
							// 形如：[1]Departments of Microbiology and Immunology,
							// Medical University of South Carolina, 86 Jonathan
							// Lucas St., Charleston, SC 29425,
							// USA|[2]Departments of Medicine, Medical
							// University of South Carolina, 86 Jonathan Lucas
							// St., Charleston, SC 29425, USA
							// [n]代表一个地址
							List<String> addresses = Arrays.asList(wiley.getAddresses().split("\\|"));
							for (String add : addresses) {
								add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments
																				// of
																				// Microbiology
																				// and
																				// Immunology
																				// ，
																				// 把空的[]去掉
								p = Pattern.compile("\\[.+\\]", Pattern.CASE_INSENSITIVE);
								matcher = p.matcher(add);
								if (matcher.find()) {
									// 一个地址可能有多个符号，还要分开
									String[] addressId = matcher.group().replaceAll("\\[", "").replaceAll("\\]", "")
											.trim().split(",");
									for (String ai : addressId) {
										addressMap.put(ai, add.replaceAll("\\[.+\\]", "").trim());
									}
								} else {
									if (!addressMap.containsKey("1")) {
										addressMap.put("1", add);
									}
								}
							}

						}

						List<Author> authorList = new ArrayList<Author>();

						if (wiley.getAuthors() != null && !wiley.getAuthors().trim().equals("")) {
							// 作者，| 分隔 ， 形如：
							// Kevin M. Gray[1]|Noreen L. Watson BS[1]|Matthew
							// J. Carpenter[1,2]|Steven D. LaRowe[1,3]|
							List<String> authors = Arrays.asList(wiley.getAuthors().split("\\|"));
							int aSize = authors.size();
							int emailScore = 0; // 作者名匹配邮件最高得分
							for (int i = 0; i < aSize; i++) {
								String authorName = authors.get(i);
								authorName = authorName.replaceAll("\\[\\p{Space}*\\]", ""); // ME.A.
																								// Vaskova[]
																								// ，
																								// 把空的[]去掉
								String tmp = authorName.replaceAll("\\[.*\\]", ""); // 去掉[1,2,*]
																					// 之类的
								if (tmp.isEmpty()) {
									continue;
								}
								// for (String authorName : authors) {
								Author author = new Author();
								if (i == 0) {
									author.setPriority(1);
								}
								if (i == 1) {
									author.setPriority(2);
								}
								author.setSource("Wiley");
								// 有时候作者名后面会跟一些机构名之类的，要去掉，如
								// Anders Perner the 6S trial groupthe
								// Scandinavian Critical Care Trials Grou
								// Thomas Paffrath[3], the TraumaRegister DGUthe
								// German Pelvic Injury Register of the Deutsche
								// Gesellschaft für Unfallchirurgie
								if (tmp.contains(",")) {
									tmp = tmp.substring(0, tmp.indexOf(","));
								}
								if (tmp.contains(" the ")) {
									tmp = tmp.substring(0, tmp.indexOf(" the "));
								}
								if (tmp.length() > 50) { // 如果还是很长，只取前两个单词作为作者名
									tmp = tmp.split(" ")[0] + " " + tmp.split(" ")[1];
								}
								author.setFullName(removeTitle(tmp));
								author.setShortName(removeTitle(tmp));

								// set 作者地址
								p = Pattern.compile("\\[.*\\]", Pattern.CASE_INSENSITIVE);
								matcher = p.matcher(authorName);
								String[] addressIds = null; // 一个作者拥有的地址id符号
								if (matcher.find()) {
									String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
									addressIds = idsStr.substring(1, idsStr.length() - 1).split(",");
									for (int j = 0; j < addressIds.length; j++) {
										if (addressIds[j].matches("[^\\d]*?\\d+[^\\d]*?")) { // 如果包含数字，去掉非数字的字符
											addressIds[j] = addressIds[j].replaceAll("[^\\d]", "");
										}
									}
								}
								if (addressIds == null) {
									author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1"));
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										if (addressMap.get(idStr) != null) {
											author.setAddress(
													author.getAddress().concat(addressMap.get(idStr)).concat("~"));
										}
									}
									if (author.getAddress().isEmpty()) {
										author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1"));
									}
								}

								// 邮件地址可能有多个，以|分隔。作者和邮件之间不一定有对应关系，甚至数量都不一定相同，可能要用类似人名和邮件名的匹配算法。
								// assumedEmail 找出这个作者匹配度最高的邮箱
								Map<String, Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2WithScorelimited(
										article.getEmail().replaceAll("\\|", ";"), author.getFullName(), "@", 4);
								// wiley
								// 不能直接在页面上抓到通讯作者和通讯地址，所以把第一个作者名和邮箱地址能匹配上的作者当做通讯作者
								if (!((String) assumedEmail.get("email")).isEmpty()) {
									author.setEmail(((String) assumedEmail.get("email")).trim());
									author.setPriority(0); // 在作者文章关系表里把能直接在网页找到email的作为通讯作者，区别不是在网页上直接抓到的而是后期非通讯作者抓到的
									// 判断是否算作是通讯作者
									if ((Integer) assumedEmail.get("total") > emailScore) {
										article.setCorrespondingAuthor(author.getFullName());
										article.setCorrespondingAddress(author.getAddress());
										article.setFullCorrespondingAuthor(author.getFullName());
										emailScore = (Integer) assumedEmail.get("total");
									}
								}

								authorList.add(author);
							}
						}

						// 如果存在没有匹配上的邮件和作者，权且认为这个邮件是属于这个通信作者的。
						if (article.getEmail() != null && !article.getEmail().isEmpty()) {
							List<String> emailList = new ArrayList<String>();
							String[] emailArray = article.getEmail().split("\\|");
							for (String e : emailArray) {
								if (!e.isEmpty()) {
									emailList.add(e);
								}
							}

							List<String> occupiedEmailList = new ArrayList<String>(); // 已确认归属作者的email
							for (Author a : authorList) {
								if (a.getEmail() != null && !a.getEmail().isEmpty()) {
									occupiedEmailList.add(a.getEmail());
								}
							}

							emailList.removeAll(occupiedEmailList); // 总的email里删除已确认归属作者的email，留下还未确认归属的

							for (Author a : authorList) {
								if ((a.getEmail() == null || a.getEmail().isEmpty()) && !emailList.isEmpty()) {
									a.setEmail(emailList.get(0));
									emailList.remove(0);
								}
							}
						}

						wileyService.saveArticleAndAuthor(article, authorList, wiley);
						System.out.println(wiley.getId() + " parsed");

					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

			}
		}
	}

	/**
	 * 
	 * 把 search_record 表的Wiley来源的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseWileyFromSearchRecord() {
		IWileyService wileyService = new WileyServiceImpl(DB);
		// IWileyService wileyService = new
		// WileyServiceImpl(Constants.MATERIAL_SCIENCE_DB);
		int startIndex = START_INDEX;
		int step = STEP;
		while (startIndex <= MAX_ID) { // SELECT MAX(id) from
										// search_wiley_by_publication
			// while (startIndex == 107) { // for test
			Criteria criteria = new Criteria();
			criteria.setWhereClause(" SOURCEES = 'Wiley' and parsed = 0 ");
			startIndex += step;
			List<Wiley> wileyList = wileyService.selectFromSearchRecord(criteria);
			// System.out.println("wosList "+wosList.size());
			if (wileyList.size() != 0) {
				for (Wiley wiley : wileyList) {
					try {
						// 作者、邮箱、全文不都为空才保存到数据库
						if ((wiley.getAuthors() == null || wiley.getAuthors().isEmpty())
								&& (wiley.getEmail() == null || wiley.getEmail().isEmpty())
								&& (wiley.getFullText() == null || wiley.getFullText().isEmpty())) {
							continue;
						}
						Article article = new Article();
						article.setAbs(wiley.getAbs());

						if (wiley.getEmail() != null && !wiley.getEmail().isEmpty()) {
							article.setEmail(wiley.getEmail());
						} else {
							article.setEmail("");
						}
						// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL
						// 字段长度，去掉后面超过长度的email
						while (article.getEmail().length() > 255) {
							article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
						}

						if (wiley.getCorrespondingAuthor() != null && !wiley.getCorrespondingAuthor().isEmpty()) {
							article.setCorrespondingAuthor(wiley.getCorrespondingAuthor().trim());
							article.setFullCorrespondingAuthor(wiley.getCorrespondingAuthor().trim());
						} else {
							article.setCorrespondingAuthor(""); // 在判断通讯作者的时候再赋值
						}
						// Wiley通讯地址在页面上比较难直接抓取，通过判断email的所有者的地址作为通讯地址
						article.setCorrespondingAddress("");// 在判断通讯作者的时候再赋值

						article.setKeyword(wiley.getKeyword());
						if (wiley.getPublicationDate() == null || wiley.getPublicationDate().length() > 30) {
							article.setpDate("");
						} else {
							article.setpDate(wiley.getPublicationDate());
						}
						article.setSource("Wiley-searchRecord");
						article.setSourcePublication(wiley.getJournal());
						article.setTitle(wiley.getTitle());
						article.setTitleIndex(wiley.getTitle().length() >= 250 ? wiley.getTitle().substring(0, 250)
								: wiley.getTitle()); // 取title前250字符作为索引
						article.setReferrence(wiley.getReference());
						article.setFullText(wiley.getFullText());
						article.setCorrespondingInfo(wiley.getCorrespondingInfo());
						article.setType("Article");

						Pattern p;
						Matcher matcher;
						Map<String, String> addressMap = new HashMap<String, String>();
						if (wiley.getAddresses() != null && !wiley.getAddresses().trim().equals("")) {
							// 地址，| 分隔
							// 形如：[1]Departments of Microbiology and Immunology,
							// Medical University of South Carolina, 86 Jonathan
							// Lucas St., Charleston, SC 29425,
							// USA|[2]Departments of Medicine, Medical
							// University of South Carolina, 86 Jonathan Lucas
							// St., Charleston, SC 29425, USA
							// [n]代表一个地址
							List<String> addresses = Arrays.asList(wiley.getAddresses().split("\\|"));
							for (String add : addresses) {
								add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments
																				// of
																				// Microbiology
																				// and
																				// Immunology
																				// ，
																				// 把空的[]去掉
								p = Pattern.compile("\\[.+\\]", Pattern.CASE_INSENSITIVE);
								matcher = p.matcher(add);
								if (matcher.find()) {
									// 一个地址可能有多个符号，还要分开
									String[] addressId = matcher.group().replaceAll("\\[", "").replaceAll("\\]", "")
											.trim().split(",");
									for (String ai : addressId) {
										addressMap.put(ai, add.replaceAll("\\[.+\\]", "").trim());
									}
								} else {
									if (!addressMap.containsKey("1")) {
										addressMap.put("1", add);
									}
								}
							}

						}

						List<Author> authorList = new ArrayList<Author>();

						if (wiley.getAuthors() != null && !wiley.getAuthors().trim().equals("")) {
							// 作者，| 分隔 ， 形如：
							// Kevin M. Gray[1]|Noreen L. Watson BS[1]|Matthew
							// J. Carpenter[1,2]|Steven D. LaRowe[1,3]|
							List<String> authors = Arrays.asList(wiley.getAuthors().split("\\|"));
							int aSize = authors.size();
							int emailScore = 0; // 作者名匹配邮件最高得分
							for (int i = 0; i < aSize; i++) {
								String authorName = authors.get(i);
								authorName = authorName.replaceAll("\\[\\p{Space}*\\]", ""); // ME.A.
																								// Vaskova[]
																								// ，
																								// 把空的[]去掉
								String tmp = authorName.replaceAll("\\[.*\\]", ""); // 去掉[1,2,*]
																					// 之类的
								if (tmp.isEmpty()) {
									continue;
								}
								// for (String authorName : authors) {
								Author author = new Author();
								if (i == 0) {
									author.setPriority(1);
								}
								if (i == 1) {
									author.setPriority(2);
								}
								author.setSource("Wiley-searchRecord");
								// 有时候作者名后面会跟一些机构名之类的，要去掉，如
								// Anders Perner the 6S trial groupthe
								// Scandinavian Critical Care Trials Grou
								// Thomas Paffrath[3], the TraumaRegister DGUthe
								// German Pelvic Injury Register of the Deutsche
								// Gesellschaft für Unfallchirurgie
								if (tmp.contains(",")) {
									tmp = tmp.substring(0, tmp.indexOf(","));
								}
								if (tmp.contains(" the ")) {
									tmp = tmp.substring(0, tmp.indexOf(" the "));
								}
								if (tmp.length() > 50) { // 如果还是很长，只取前两个单词作为作者名
									tmp = tmp.split(" ")[0] + " " + tmp.split(" ")[1];
								}
								author.setFullName(removeTitle(tmp));
								author.setShortName(removeTitle(tmp));

								// set 作者地址
								p = Pattern.compile("\\[.*\\]", Pattern.CASE_INSENSITIVE);
								matcher = p.matcher(authorName);
								String[] addressIds = null; // 一个作者拥有的地址id符号
								if (matcher.find()) {
									String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
									addressIds = idsStr.substring(1, idsStr.length() - 1).split(",");
									for (int j = 0; j < addressIds.length; j++) {
										if (addressIds[j].matches("[^\\d]*?\\d+[^\\d]*?")) { // 如果包含数字，去掉非数字的字符
											addressIds[j] = addressIds[j].replaceAll("[^\\d]", "");
										}
									}
								}
								if (addressIds == null) {
									author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1"));
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										if (addressMap.get(idStr) != null) {
											author.setAddress(
													author.getAddress().concat(addressMap.get(idStr)).concat("~"));
										}
									}
									if (author.getAddress().isEmpty()) {
										author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1"));
									}
								}

								// 邮件地址可能有多个，以|分隔。作者和邮件之间不一定有对应关系，甚至数量都不一定相同，可能要用类似人名和邮件名的匹配算法。
								// assumedEmail 找出这个作者匹配度最高的邮箱
								Map<String, Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2WithScorelimited(
										article.getEmail().replaceAll("\\|", ";"), author.getFullName(), "@", 4);
								// wiley
								// 不能直接在页面上抓到通讯作者和通讯地址，所以把第一个作者名和邮箱地址能匹配上的作者当做通讯作者
								if (!((String) assumedEmail.get("email")).isEmpty()) {
									author.setEmail(((String) assumedEmail.get("email")).trim());
									author.setPriority(0); // 在作者文章关系表里把能直接在网页找到email的作为通讯作者，区别不是在网页上直接抓到的而是后期非通讯作者抓到的
									// 判断是否算作是通讯作者
									if ((Integer) assumedEmail.get("total") > emailScore) {
										article.setCorrespondingAuthor(author.getFullName());
										article.setCorrespondingAddress(author.getAddress());
										article.setFullCorrespondingAuthor(author.getFullName());
										emailScore = (Integer) assumedEmail.get("total");
									}
								}

								authorList.add(author);
							}
						}

						// 如果存在没有匹配上的邮件和作者，权且认为这个邮件是属于这个通信作者的。
						if (article.getEmail() != null && !article.getEmail().isEmpty()) {
							List<String> emailList = new ArrayList<String>();
							String[] emailArray = article.getEmail().split("\\|");
							for (String e : emailArray) {
								if (!e.isEmpty()) {
									emailList.add(e);
								}
							}

							List<String> occupiedEmailList = new ArrayList<String>(); // 已确认归属作者的email
							for (Author a : authorList) {
								if (a.getEmail() != null && !a.getEmail().isEmpty()) {
									occupiedEmailList.add(a.getEmail());
								}
							}

							emailList.removeAll(occupiedEmailList); // 总的email里删除已确认归属作者的email，留下还未确认归属的

							for (Author a : authorList) {
								if ((a.getEmail() == null || a.getEmail().isEmpty()) && !emailList.isEmpty()) {
									a.setEmail(emailList.get(0));
									emailList.remove(0);
								}
							}
						}

						wileyService.saveArticleAndAuthorForSearchRecord(article, authorList, wiley);
						System.out.println(wiley.getId() + " parsed");

					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

			}
		}
	}

	/**
	 * 把人名中的头衔去掉
	 * 
	 * @param authorName
	 * @return
	 */
	private static String removeTitle(String authorName) {
		String[] names = authorName.split(TITLES_REX);
		for (String name : names) {
			if (!ParserUtils.trim(name).isEmpty()) {
				return name.trim();
			}
		}
		return "";
	}

	public static final String[] TITLES = { "RM", "BSN", "MSN", "MD", "RN", "Associate Professor",
			"Assistant Professor", "MS", "PhD", "MSc", "DDS", "Professor", "MSD", "Dentist", "FRCP", "FRCPCH", "FRCPC",
			"RPh", "FACC", "FAHA", "MPH", "MBA", "MDS", "BDS", "EdD", "MSW", "DACVECC", "DVM", "MRCP", "Vice President",
			"DACVIM", "DACVP", "MRCVS", "BVetMed", "DACVAA", "FACS", "Director", "Editor", "Issue Editor",
			"Guest Editor", "Research Scientist", "Dr", "Prof", "MBBS", "BSc", "BM", "MA", "FRCPA", "FRACP", "DO",
			"DSc", "DM", "MRCPsych", "Coordinator", "secretary", "President", "PD", "Master", "for" }; // 人名里的头衔
	public static String TITLES_REX = ""; // 去除人名里的头衔的正则表达式

	// 给TITLES_REX赋值，形如(?i)(\bR\.?M?(\.|\b))|(\bB\.?S\.?N?(\.|\b))|(\bM\.?S\.?N?(\.|\b))
	static {
		StringBuffer titleSB = new StringBuffer("(?i)"); // (?i)表示正则大小写不敏感
		for (String t : TITLES) {
			titleSB.append("(\\b");
			char[] chars = t.toCharArray();
			for (char c : chars) {
				titleSB.append(c).append("\\.?");
			}
			titleSB.delete(titleSB.length() - 3, titleSB.length()); // 去掉最后的\.?
			titleSB.append("(\\.|\\b))").append("|");
		}
		titleSB.deleteCharAt(titleSB.length() - 1); // 去掉最后一个|
		TITLES_REX = titleSB.toString();
	}

}
