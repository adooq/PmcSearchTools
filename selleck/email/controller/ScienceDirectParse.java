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
import selleck.email.pojo.ScienceDirect;
import selleck.email.service.IScienceDirectService;
import selleck.email.service.impl.ScienceDirectServiceImpl;
import selleck.utils.Constants;

public class ScienceDirectParse extends AbstractParser{
	public static String SOURCE; // 解析search_sciencedirect_by_publication
	
	public static void main(String[] args) {
		
		/* ******   开始新任务时，需要修改的参数 ****   */
		DB =  Constants.LIFE_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.MATERIAL_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.LOCAL; // 进行操作的数据库
		START_INDEX = 5471880; // search_sciencedirect_by_publication表起始id
		MAX_ID = 5817472; // search_sciencedirect_by_publication表最后的id，通常是max(id) from search_sciencedirect_by_publication
		STEP = 10000; // 一次查询出来的数量
		SOURCE = "SearchPublication"; // 解析search_scienceDirect_by_publication
		// SOURCE = "SearchRecord"; // 解析search_record where SOURCEES = 'ScienceDirect'
		// **************************************** //
		
		newEmailTableName = "selleck_edm_author_nodup_"
				+DB.split(" ")[0]+"_sd_"
				+new SimpleDateFormat("yyyyMM").format(new Date());
		
		new ScienceDirectParse().allProcess();
	}

	/**
	 * @param args
	 */
	void importEDMDB() {
		if(SOURCE.equals("SearchPublication")){
			parseScienceDirectSearchPubcliation();
		}else if(SOURCE.equals("SearchRecord")){
			parseScienceDirectSearchRecord();
		}
	}
	
	/**
	 * 
	 * 把search_sciencedirect_by_publication表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseScienceDirectSearchPubcliation() {
		// IScienceDirectService sdService = new ScienceDirectServiceImpl(Constants.MATERIAL_SCIENCE_DB);
		IScienceDirectService sdService = new ScienceDirectServiceImpl(DB);
		int startIndex = START_INDEX; // 新更新ScienceDirect期刊文章起始id
		int step = STEP;
		while(startIndex <= MAX_ID){ 
			Criteria criteria =new Criteria();
			criteria.setWhereClause(" have_read = 0 and title is not null and title != '' and id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			List<ScienceDirect> sdList = sdService.selectBySearchPublicaton(criteria);
			if (sdList.size() != 0) {
				for (ScienceDirect scienceDirect : sdList) {
					try{
						// 作者、邮箱、全文不都为空才保存到数据库
						if((scienceDirect.getAuthors()==null || scienceDirect.getAuthors().isEmpty())
								&& (scienceDirect.getEmail()==null || scienceDirect.getEmail().isEmpty())
								&& (scienceDirect.getFullText()==null || scienceDirect.getFullText().isEmpty())
								)
						{
							continue;
						}
					Article article = new Article();
					article.setAbs(scienceDirect.getAbs());
					article.setClassification("");
					article.setEmail(scienceDirect.getEmail().replaceAll("\\s", ""));
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					article.setCorrespondingAuthor(scienceDirect.getCorrespondingAuthor());
					article.setFullCorrespondingAuthor(scienceDirect.getCorrespondingAuthor());
					article.setCorrespondingAddress(scienceDirect.getCorrespondingAddress());
					article.setKeyword(scienceDirect.getKeyword());
					article.setpDate(scienceDirect.getpDate());
					article.setSource("ScienceDirect");
					article.setSourcePublication(scienceDirect.getSourcePublication());
					article.setTitle(scienceDirect.getTitle());
					article.setType("Article");
					article.setTitleIndex(scienceDirect.getTitle().length() >= 250 ? scienceDirect.getTitle().substring(0, 250) : scienceDirect.getTitle()); // 取title前250字符作为索引
					article.setReferrence(scienceDirect.getReference());
					article.setFullText(scienceDirect.getFullText());
					
					List<Author> authorList = new ArrayList<Author>();

					if (scienceDirect.getAuthors() != null && !scienceDirect.getAuthors().trim().equals("")) {
						// 作者，形如：
						// Hao Guo[a,b,c]|Tetsunari Kimura[a,c,d]|Yuji Furutani[a,b,e]|Kenta Mizuse[a]|Asuka Fujii[]|
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(scienceDirect.getAuthors().split("\\|"));
						int aSize = authors.size();
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i); // 形如：  Hao Guo[a,b,c]
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("ScienceDirect");
							String tmp = authorName.replaceAll("\\[\\p{Graph}*\\]", "").trim(); // 去掉[xxx] 
							// tmp = splitName(tmp);
							author.setFullName(tmp);
							author.setShortName(tmp);

							Pattern p = Pattern.compile("\\[.*?\\]",Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							String[] addressIds = null; // 一个作者拥有的地址id符号
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split(",");
								for(int j = 0; j < addressIds.length ;j++){
									if(addressIds[j].matches("[^a-zA-Z0-9]*?[a-zA-Z0-9]+[^a-zA-Z0-9]*?")){ // 如果包含字母或数字，去掉非其他的字符
										addressIds[j] = addressIds[j].replaceAll("[^a-zA-Z0-9]", "");
									}
								}
							}
							
							if (scienceDirect.getAddresses() != null && !scienceDirect.getAddresses().trim().equals("")) {
								// 地址，形如：[a] Department of Surgery, Center for Prospective Trials, Children’s Mercy Hospital, 2401 Gillham Road, Kansas City, MO 64108, USA|[b] Department of Surgery, Children’s Mercy Hospital, 2401 Gillham Road, Kansas City, MO 64108, USA|
								// [n]代表一个地址 ,  | 分隔
								Map<String,String> addresses = new HashMap<String,String>(); // <地址的编号,地址>
								String[] addressArr = scienceDirect.getAddresses().split("\\|");
								for(String add : addressArr){
									add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments of Microbiology and Immunology ， 把空的[]去掉
									p = Pattern.compile("\\[.+\\]",Pattern.CASE_INSENSITIVE);
									matcher = p.matcher(add);
									if(matcher.find()){
										// 一个地址可能有多个符号，还要分开
										String[] addressId = matcher.group().replaceAll("\\[" , "").replaceAll("\\]" , "").trim().split(",");
										for(String ai : addressId){
											addresses.put(ai , add.replaceAll("\\[.+\\]", "").trim());
										}
									}else{
										if(!addresses.containsKey("1")){
											addresses.put("1", add);
										}
									}
								}
								
								if (addressIds == null) { // 如果作者没有地址编号，把第一个地址作为他的地址
									author.setAddress(addresses.values().iterator().next().replaceAll("[^\\p{Print}]", "").trim());
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										if(addresses.get(idStr) != null){
											author.setAddress(author.getAddress().concat(addresses.get(idStr).replaceAll("[^\\p{Print}]", "").trim()).trim().concat("~"));
										}
									}
									if(author.getAddress().isEmpty()){
										author.setAddress(addresses.get("1") == null ? "" : addresses.get("1"));
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

					sdService.saveArticleAndAuthor(article, authorList,scienceDirect);
					System.out.println(scienceDirect.getId() + " parsed");
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
	 * 把search_record表的ScienceDirect来源的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseScienceDirectSearchRecord() {
		// IScienceDirectService sdService = new ScienceDirectServiceImpl(Constants.MATERIAL_SCIENCE_DB);
		IScienceDirectService sdService = new ScienceDirectServiceImpl(DB);
		int startIndex = START_INDEX; // 新更新ScienceDirect期刊文章起始id
		int step = STEP;
		// while(startIndex <= 1200000){ // MAX(id) from search_sciencedirect_by_publication
		while(startIndex <= MAX_ID){ // MAX(id) from search_sciencedirect_by_publication
		// while (startIndex == 545) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" SOURCEES = 'ScienceDirect' and parsed = 0 ");
			startIndex += step;
			// criteria.put("have_read", 0);
			List<ScienceDirect> sdList = sdService.selectBySearchRecord(criteria);
			// System.out.println("wosList "+wosList.size());
			if (sdList.size() != 0) {
				for (ScienceDirect scienceDirect : sdList) {
					try{
						// 作者、邮箱、全文不都为空才保存到数据库
						if((scienceDirect.getAuthors()==null || scienceDirect.getAuthors().isEmpty())
								&& (scienceDirect.getEmail()==null || scienceDirect.getEmail().isEmpty())
								&& (scienceDirect.getFullText()==null || scienceDirect.getFullText().isEmpty())
								)
						{
							continue;
						}
					Article article = new Article();
					article.setAbs(scienceDirect.getAbs());
					article.setClassification("");
					article.setEmail(scienceDirect.getEmail().replaceAll("\\s", ""));
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					article.setCorrespondingAuthor(scienceDirect.getCorrespondingAuthor());
					article.setFullCorrespondingAuthor(scienceDirect.getCorrespondingAuthor());
					article.setCorrespondingAddress(scienceDirect.getCorrespondingAddress());
					article.setKeyword(scienceDirect.getKeyword());
					article.setpDate(scienceDirect.getpDate());
					article.setSource("ScienceDirect-searchRecord");
					article.setSourcePublication(scienceDirect.getSourcePublication());
					article.setTitle(scienceDirect.getTitle());
					article.setType("Article");
					article.setTitleIndex(scienceDirect.getTitle().length() >= 250 ? scienceDirect.getTitle().substring(0, 250) : scienceDirect.getTitle()); // 取title前250字符作为索引
					article.setReferrence(scienceDirect.getReference());
					article.setFullText(scienceDirect.getFullText());
					
					List<Author> authorList = new ArrayList<Author>();

					if (scienceDirect.getAuthors() != null && !scienceDirect.getAuthors().trim().equals("")) {
						// 作者，形如：
						// Hao Guo[a,b,c]|Tetsunari Kimura[a,c,d]|Yuji Furutani[a,b,e]|Kenta Mizuse[a]|Asuka Fujii[]|
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(scienceDirect.getAuthors().split("\\|"));
						int aSize = authors.size();
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i); // 形如：  Hao Guo[a,b,c]
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("ScienceDirect-searchRecord");
							String tmp = authorName.replaceAll("\\[\\p{Graph}*\\]", "").trim(); // 去掉[xxx] 
							// tmp = splitName(tmp);
							author.setFullName(tmp);
							author.setShortName(tmp);

							Pattern p = Pattern.compile("\\[.*?\\]",Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							String[] addressIds = null; // 一个作者拥有的地址id符号
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split(",");
								for(int j = 0; j < addressIds.length ;j++){
									if(addressIds[j].matches("[^a-zA-Z0-9]*?[a-zA-Z0-9]+[^a-zA-Z0-9]*?")){ // 如果包含字母或数字，去掉非其他的字符
										addressIds[j] = addressIds[j].replaceAll("[^a-zA-Z0-9]", "");
									}
								}
							}
							
							if (scienceDirect.getAddresses() != null && !scienceDirect.getAddresses().trim().equals("")) {
								// 地址，形如：[a] Department of Surgery, Center for Prospective Trials, Children’s Mercy Hospital, 2401 Gillham Road, Kansas City, MO 64108, USA|[b] Department of Surgery, Children’s Mercy Hospital, 2401 Gillham Road, Kansas City, MO 64108, USA|
								// [n]代表一个地址 ,  | 分隔
								Map<String,String> addresses = new HashMap<String,String>(); // <地址的编号,地址>
								String[] addressArr = scienceDirect.getAddresses().split("\\|");
								for(String add : addressArr){
									add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments of Microbiology and Immunology ， 把空的[]去掉
									p = Pattern.compile("\\[.+\\]",Pattern.CASE_INSENSITIVE);
									matcher = p.matcher(add);
									if(matcher.find()){
										// 一个地址可能有多个符号，还要分开
										String[] addressId = matcher.group().replaceAll("\\[" , "").replaceAll("\\]" , "").trim().split(",");
										for(String ai : addressId){
											addresses.put(ai , add.replaceAll("\\[.+\\]", "").trim());
										}
									}else{
										if(!addresses.containsKey("1")){
											addresses.put("1", add);
										}
									}
								}
								
								if (addressIds == null) { // 如果作者没有地址编号，把第一个地址作为他的地址
									author.setAddress(addresses.values().iterator().next().replaceAll("[^\\p{Print}]", "").trim());
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										if(addresses.get(idStr) != null){
											author.setAddress(author.getAddress().concat(addresses.get(idStr).replaceAll("[^\\p{Print}]", "").trim()).trim().concat("~"));
										}
									}
									if(author.getAddress().isEmpty()){
										author.setAddress(addresses.get("1") == null ? "" : addresses.get("1"));
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

					sdService.saveArticleAndAuthorForSearchRecord(article, authorList,scienceDirect);
					System.out.println(scienceDirect.getId() + " parsed");
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
				}
				
			}
		}
	}	
}
