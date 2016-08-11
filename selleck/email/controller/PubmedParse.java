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
import selleck.email.pojo.Pubmed;
import selleck.email.pojo.Springer;
import selleck.email.service.IPubmedService;
import selleck.email.service.ISpringerService;
import selleck.email.service.impl.PubmedServiceImpl;
import selleck.email.service.impl.SpringerServiceImpl;
import selleck.utils.Constants;

public class PubmedParse extends AbstractParser{
	public static String SOURCE; // 解析search_pubmed_by_publication
	
	public static void main(String[] args) {
		
		/* ******   开始新任务时，需要修改的参数 ****   */
		DB =  Constants.LIFE_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.MATERIAL_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.LOCAL; // 进行操作的数据库
		START_INDEX = 9771874; // search_pubmed_by_publication表起始id
		MAX_ID = 9890419; // search_pubmed_by_publication表最后的id，通常是max(id) from search_pubmed_by_publication
		STEP = 10000; // 一次查询出来的数量
		SOURCE = "SearchPublication"; // 解析search_pubmed_by_publication
		// SOURCE = "SearchRecord"; // 解析search_record where SOURCEES = 'Springer'
		// **************************************** //
		
		newEmailTableName = "selleck_edm_author_nodup_"
				+DB.split(" ")[0]+"_pubmed_"
				+new SimpleDateFormat("yyyyMM").format(new Date());
		
		new PubmedParse().allProcess();
		
		// parsePubmedSearchPublication(); // for test
	}
	
	/**
	 * @param args
	 */
	void importEDMDB() {
		if(SOURCE.equals("SearchPublication")){
			parsePubmedSearchPublication();
		}else if(SOURCE.equals("SearchRecord")){
			parseSpringerSearchRecord();
		}
	}
	
	/**
	 * 
	 * 把search_pubmed_publication表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parsePubmedSearchPublication() {
		IPubmedService pubmedService = new PubmedServiceImpl(DB);
		int startIndex = START_INDEX; // 新更新springer期刊文章起始id
		int step = STEP;
		while(startIndex <= MAX_ID){ // MAX(id) from search_springer_by_publication
		// while (startIndex == 0) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" have_read = 0 and id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			// criteria.put("have_read", 0);
			List<Pubmed> pubmedList = pubmedService.selectBySearchPublication(criteria);

			if (pubmedList.size() != 0) {
				for (Pubmed pubmed : pubmedList) {
					try{
					if(pubmed.getHaveRead() == 1){
						continue;
					}
					if(pubmed.getTitle() == null || pubmed.getTitle().isEmpty()){
						continue;
					}
					Article article = new Article();
					article.setAbs(pubmed.getAbs());
					article.setClassification("");
					article.setEmail(pubmed.getEmail().replaceAll("\\s", ""));
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					article.setCorrespondingAuthor(pubmed.getCorrespondingAuthor());
					article.setFullCorrespondingAuthor(pubmed.getCorrespondingAuthor());
					article.setCorrespondingAddress(pubmed.getCorrespondingAddress());
					article.setKeyword(pubmed.getKeyword());
					article.setpDate(pubmed.getpDate());
					article.setSource("Pubmed");
					article.setSourcePublication(pubmed.getSourcePublication());
					article.setTitle(pubmed.getTitle());
					article.setTitleIndex(pubmed.getTitle().length() >= 250 ? pubmed.getTitle().substring(0, 250) : pubmed.getTitle()); // 取title前250字符作为索引
					
					List<Author> authorList = new ArrayList<Author>();

					if (pubmed.getAuthors() != null && !pubmed.getAuthors().trim().equals("")) {
						// 作者，形如：
						// Mladen Jergović [1] [5]|Krešo Bendelja [1]|Anđelko Vidović [2]|Ana Savić [1]|Valerija Vojvoda [1]|Neda Aberle [3]|Sabina Rabatić [5]|Tanja Jovanovic [4]|Ante Sabioncello [5]|
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(pubmed.getAuthors().split("\\|"));
						int aSize = authors.size();
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i);
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("Pubmed");
							String regex = "\\[[\\d]+\\]";
							String tmp = authorName.replaceAll(regex, "").trim(); // 去掉[1] [2] 之类的
							// tmp = splitName(tmp);
							if(tmp.length() > 50){ // 作者超过50个字的，大多数是组织名称之类的东西，就不作为一个作者了。
								continue;
							}
							author.setFullName(tmp);
							author.setShortName(tmp);

							Pattern p = Pattern.compile("\\[\\d+\\]",Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							List<String> addressIds = new ArrayList<String>(); // 作者的地址编号
							while (matcher.find()) {
								String idsStr = matcher.group(); // idsStr形如[1]
								addressIds.add(idsStr.substring(1,idsStr.length() - 1));
							}
							
							// 设置地址
							if (pubmed.getAddresses() != null && !pubmed.getAddresses().trim().equals("")) {
								// 地址，形如：[1]University of Tasmania, Hobart, TAS 7000, Australia.|[2]University of Sydney, Sydney, NSW 2006, Australia.|[3]UCLA, Los Angeles, CA 90095, USA.|
								// 地址用竖线分隔。[n]代表地址编号，有时会没有。
								Map<String,String> addresses = new HashMap<String,String>(); // <地址的编号,地址>
								String[] addressArr = pubmed.getAddresses().split("\\|");
								for(String ida : addressArr){ // ida 形如  [1] Chungnam Natl Univ, Taejon
									if(!ida.trim().isEmpty()){
										String aid = ida.substring(ida.indexOf("[") + 1, ida.indexOf("]")); // 地址的编号，会有字符编号
										String addStr = ida.replaceFirst("\\[\\d+\\]", "").trim(); // 地址
										addresses.put(aid, addStr);
									}
								}
								
								if (addressIds.isEmpty()) { // 如果作者没有地址编号，把第一个地址作为他的地址
									author.setAddress(addresses.values().iterator().next().replaceAll("[^\\p{Print}]", "").trim());
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										String a1 = addresses.get(idStr);
										String addessOne = a1.replaceAll("[^\\p{Print}]", "").trim();
										author.setAddress(author.getAddress().concat(addessOne).trim().concat("~"));
									}

								}
							}
							
							// 设置邮箱
							if (pubmed.getEmail() != null && !pubmed.getEmail().trim().equals("")) {
								// 邮箱地址，形如：[1]a.eder@uke.de|[2]i.vollert@uke.de|[3]ar.hansen@uke.de|[4]t.eschenhagen@uke.de|
								// 地址用竖线分隔。[n]代表地址编号，有时会没有。
								Map<String,String> emails = new HashMap<String,String>(); // <邮箱的编号,邮箱>
								String[] addressArr = pubmed.getAddresses().split("\\|");
								for(String ida : addressArr){ // ida 形如  [1]a.eder@uke.de
									if(!ida.trim().isEmpty()){
										String aid = ida.substring(ida.indexOf("[") + 1, ida.indexOf("]")); // 邮箱地址的编号，纯数字
										String addStr = ida.replaceFirst("\\[\\d+\\]", "").trim(); // 地址
										emails.put(aid, addStr);
									}
								}
								
								if (addressIds.isEmpty()) { // 如果作者没有邮箱编号，在最后通过人名和邮箱匹配
									
								} else {
									author.setEmail("");
									for (String idStr : addressIds) {
										String a1 = emails.get(idStr);
										String addessOne = a1.replaceAll("[^\\p{Print}]", "").trim();
										author.setEmail(addessOne);
									}

								}
							}
							
							authorList.add(author);
						}
						
						// 设置邮箱地址，补漏
						// 邮件地址可能有多个，以"|"分隔。邮件地址的编号和作者的编号相对应。
						// 形如 [1]pia.puhakka@uef.fi|[2]n.c.r.temoller@uu.nl|[3]isaac.afara@uef.fi|[4]janne.makela@uef.fi|[5]virpi.tiitu@uef.fi|
						if(article.getEmail() != null){
							Map<String,EmailScoreAuthor> emailMap = new HashMap<String,EmailScoreAuthor>();
							// 邮件地址可能没有编号， 要用类似人名和邮件名的匹配算法。
							// assumedEmail 找出这个作者匹配度最高的邮箱
							for(Author author : authorList){
								Map<String,Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2WithScorelimited(article.getEmail().replaceAll("\\|", ";"), author.getFullName(), "@" , 4);
								if(!((String)assumedEmail.get("email")).isEmpty()){
									if(emailMap.get(assumedEmail.get("email")) == null){
										EmailScoreAuthor esa = new EmailScoreAuthor();
										esa.setEmail((String)assumedEmail.get("email"));
										esa.setScore((Integer)assumedEmail.get("total"));
										esa.setAuthor(author);
										emailMap.put((String)assumedEmail.get("email"), esa);
									}else{
										EmailScoreAuthor esa = emailMap.get(assumedEmail.get("email"));
										if(esa.getScore() < (Integer)assumedEmail.get("total")){
											esa.setEmail((String)assumedEmail.get("email"));
											esa.setScore((Integer)assumedEmail.get("total"));
											esa.setAuthor(author);
										}
									}
								}
							}
								
							for(String e : emailMap.keySet()){
								Author a = emailMap.get(e).getAuthor();
								a.setEmail(e);
								a.setPriority(0);
								if(article.getCorrespondingAuthor() == null || article.getCorrespondingAuthor().isEmpty()){
									article.setCorrespondingAuthor(a.getFullName());
								}
								if(article.getFullCorrespondingAuthor() == null || article.getFullCorrespondingAuthor().isEmpty()){
									article.setFullCorrespondingAuthor(a.getFullName());
								}
								if(article.getCorrespondingAddress() == null || article.getCorrespondingAddress().isEmpty()){
									article.setCorrespondingAddress(a.getAddress());
								}
							}
						}
						
					}

					pubmedService.saveArticleAndAuthor(article, authorList,pubmed);
					System.out.println(pubmed.getId() + " parsed");
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
				}
				
			}
		}
	}
	
	/**
	 * 
	 * 把search_record表的Springer来源的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseSpringerSearchRecord() {
		ISpringerService springerService = new SpringerServiceImpl(DB);
		// ISpringerService springerService = new SpringerServiceImpl(Constants.LIFE_SCIENCE_DB);
		int startIndex = START_INDEX; // 新更新springer期刊文章起始id
		int step = STEP;
		while(startIndex <= MAX_ID){ // MAX(id) from search_springer_by_publication
		// while (startIndex == 0) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" SOURCEES = 'Springer' and parsed = 0 ");
			startIndex += step;
			// criteria.put("have_read", 0);
			List<Springer> springerList = springerService.selectBySearchRecord(criteria);
			// System.out.println("wosList "+wosList.size());
			if (springerList.size() != 0) {
				for (Springer springer : springerList) {
					try{
					if(springer.getHaveRead() == 1){
						continue;
					}
					Article article = new Article();
					article.setAbs(springer.getAbs());
					article.setClassification("");
					article.setEmail(springer.getEmail().replaceAll("\\s", ""));
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					article.setCorrespondingAuthor(springer.getCorrespondingAuthor());
					article.setFullCorrespondingAuthor(springer.getCorrespondingAuthor());
					article.setCorrespondingAddress(springer.getCorrespondingAddress());
					article.setKeyword(springer.getKeyword());
					article.setKeywordPlus(springer.getKeywordPlus());
					article.setpDate(springer.getpDate());
					article.setResearch(springer.getResearch());
					article.setSource("Springer-searchRecord");
					article.setSourcePublication(springer.getSourcePublication());
					article.setTitle(springer.getTitle());
					article.setType(springer.getType());
					article.setTitleIndex(springer.getTitle().length() >= 250 ? springer.getTitle().substring(0, 250) : springer.getTitle()); // 取title前250字符作为索引
					article.setReferrence(springer.getReference());
					article.setFullText(springer.getFullText());
					
					List<Author> authorList = new ArrayList<Author>();

					if (springer.getAuthors() != null && !springer.getAuthors().trim().equals("")) {
						// 作者，形如：
						// Mladen Jergović [1] [5]|Krešo Bendelja [1]|Anđelko Vidović [2]|Ana Savić [1]|Valerija Vojvoda [1]|Neda Aberle [3]|Sabina Rabatić [5]|Tanja Jovanovic [4]|Ante Sabioncello [5]|
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(springer.getAuthors().split("\\|"));
						int aSize = authors.size();
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i);
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("Springer-searchRecord");
							String regex = "\\[[\\d]+\\]";
							String tmp = authorName.replaceAll(regex, "").trim(); // 去掉[1] [2] 之类的
							// tmp = splitName(tmp);
							if(tmp.length() > 50){ // 作者超过50个字的，大多数是组织名称之类的东西，就不作为一个作者了。
								continue;
							}
							author.setFullName(tmp);
							author.setShortName(tmp);

							Pattern p = Pattern.compile("\\[\\d+\\]",Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							List<Integer> addressIds = new ArrayList<Integer>();
							while (matcher.find()) {
								String idsStr = matcher.group(); // idsStr形如[1]
								addressIds.add(Integer.valueOf(idsStr.substring(1,idsStr.length() - 1)));
							}

							if (springer.getAddresses() != null && !springer.getAddresses().trim().equals("")) {
								// 地址，形如：[1] Centre for research and knowledge transfer in biotechnology, University of Zagreb, Zagreb, Croatia[5] Department for Cellular Immunology, Institute of Immunology, Rockfellerova ulica 10, Zagreb, Croatia[2] Department of Psychiatry, University Hospital Dubrava, Zagreb, Croatia[3] General hospital “dr. Josip Benčević”, Slavonski Brod, Croatia[4] Department of Psychiatry & Behavioral Sciences, Emory University, Atlanta, GA, USA
								// [n]代表一个地址
								Map<Integer,String> addresses = new HashMap<Integer,String>(); // <地址的编号,地址>
								p = Pattern.compile("\\[\\d+\\][^\\[]+",Pattern.CASE_INSENSITIVE);
								matcher = p.matcher(springer.getAddresses());
								while(matcher.find()){
									String ida = matcher.group(); // ida 形如  [1] Chungnam Natl Univ, Taejon
									int aid = Integer.valueOf(ida.substring(ida.indexOf("[") + 1, ida.indexOf("]"))); // 地址的编号
									String addStr = ida.replaceFirst("\\[\\d+\\]", "").trim(); // 地址
									addresses.put(aid, addStr);
								}	
								
								if (addressIds.isEmpty()) { // 如果作者没有地址编号，把第一个地址作为他的地址
									author.setAddress(addresses.values().iterator().next().replaceAll("[^\\p{Print}]", "").trim());
								} else {
									author.setAddress("");
									for (int idStr : addressIds) {
										author.setAddress(author.getAddress().concat(addresses.get(idStr).replaceAll("[^\\p{Print}]", "").trim()).trim().concat("~"));
									}

								}
							}
							
							authorList.add(author);
						}
						
						// 判断是否是通讯作者
						// 邮件地址可能有多个，以"|"分隔。邮件地址有和作者顺序对应的编号。
						// 形如 mjergovi@unizg.hr[1]|kbendelja@imz.hr[2]|avidovic@live.com[3]|asavic@imz.hr[4]|vvojvoda@imz.hr[5]|neda.aberle@gmail.com[6]|srabatic1@gmail.com[7]|tjovano@emory.edu[8]|ante.sabioncello@gmail.com[9]|
						if(article.getEmail() != null){
							String[] emailArr = article.getEmail().split("\\|");
							for(String emailAndNo : emailArr){
								Pattern p = Pattern.compile("\\[[\\d]+\\]",Pattern.CASE_INSENSITIVE);
								Matcher matcher = p.matcher(emailAndNo);
								if(matcher.find()){
									int authorId = Integer.valueOf(matcher.group().replaceAll("\\[", "").replaceAll("\\]", ""));
									Author a = authorList.get(authorId - 1);
									a.setEmail(emailAndNo.replaceAll("\\[[\\d]+\\]", "").trim());
									a.setPriority(0);
								}
							}
						}
						
					}

					springerService.saveArticleAndAuthorForSearchRecord(article, authorList,springer);
					System.out.println(springer.getId() + " parsed");
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
				}
				
			}
		}
	}
	
	/**
	 * 有些记录的作者全名full_author不正确，例如：Bode, AnnM.
	 * 应该是Bode, Ann M.
	 * @param name  Bode, AnnM.
	 * @return Bode, Ann M.
	 */
	private static String splitName(String name){
		Pattern p = Pattern.compile("\\B[A-Z]\\.",Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(name);
		while(matcher.find()){
			name =  name.replace(matcher.group()," "+matcher.group());
		}
		name = name.replaceAll(",", ", "); // 为,后面加空格，为了符合英文书写规范和美观
		name = name.replaceAll("\\s+", " ");
		return name;
	}
	
	/**
	 * 从authors中去查找CorrespondingAuthor的全名
	 * 例如CorrespondingAuthor是 Yu, DS  ，authors是 Ping,SY(Ping,Szu-Yuan)[2]|Wu,CL(Wu,Chia-Lun)[1]|Yu,DS(Yu,Dah-Shyong)[1]
	 * return Yu,Dah-Shyong
	 * @param cName 通讯作者名
	 * @param authorNames 所有作者的名字
	 * @return
	 * @deprecated
	 */
	private static String findFullNameByCorrespondingAuthor(String cName,String authorNames){
		if(cName == null || cName.isEmpty()){
			return "";
		}
		int cNameIndex = authorNames.indexOf(cName.replaceAll("\\s", ""));
		if(cNameIndex == -1){
			return "";
		}
		String tmp = authorNames.substring(cNameIndex);
		return tmp.substring(tmp.indexOf("(")+1, tmp.indexOf(")"));
	}

}

/**
 * 跟作者名字做匹配的邮箱地址
 * @author fscai
 *
 */
class EmailScoreAuthor{
	private String email;
	private int score;
	private Author author;
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public Author getAuthor() {
		return author;
	}
	public void setAuthor(Author author) {
		this.author = author;
	}
	
	
}
