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
import selleck.email.pojo.PMC;
import selleck.email.service.IPMCService;
import selleck.email.service.impl.PMCServiceImpl;
import selleck.utils.Constants;

public class PMCParse extends AbstractParser{
	
	public static String SOURCE; // 解析search_pmc_by_publication
	
	public static void main(String[] args) {
		
		/* ******   开始新任务时，需要修改的参数 ****   */
		DB =  Constants.LIFE_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.LOCAL; // 进行操作的数据库
		START_INDEX = 1; // search_wos_by_publication表起始id
		MAX_ID = 5468; // search_wos_by_publication表最后的id，通常是max(id) from search_wos_by_publication
		STEP = 10000; // 一次查询出来的数量
		SOURCE = "SearchPublication"; // 解析search_pmc_by_publication
		// SOURCE = "SearchRecord"; // 解析search_record where SOURCEES = 'PMC'
		// **************************************** //
		
		newEmailTableName = "selleck_edm_author_nodup_"
				+DB.split(" ")[0]+"_pmc_"
				+new SimpleDateFormat("yyyyMM").format(new Date());
		
		new PMCParse().allProcess();
	}

	/**
	 * @param args
	 */
	void importEDMDB() {
		if(SOURCE.equals("SearchPublication")){
			parsePMCSearchPublication();
		}else if(SOURCE.equals("SearchRecord")){
			parsePMCSearchRecord();
		}
	}
	
	/**
	 * 
	 * 把search_pmc_by_publication表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parsePMCSearchPublication() {
		IPMCService pmcService = new PMCServiceImpl(DB);
		int startIndex = START_INDEX;
		int step = STEP;
		while(startIndex <= MAX_ID){ // SELECT MAX(id) from search_pmc_by_publication
		// while (startIndex == 833838) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" have_read = 0 and id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			// criteria.put("have_read", 0);
			List<PMC> pmcList = pmcService.selectBySearchPublication(criteria);
			// System.out.println("wosList "+wosList.size());
			if (pmcList.size() != 0) {
				for (PMC pmc : pmcList) {
					try{
					if(pmc.getHaveRead() == 1){
						continue;
					}
					if(pmc.getTitle() == null || pmc.getTitle().isEmpty()){
						continue;
					}
					Article article = new Article();
					article.setAbs(pmc.getAbs());
					
					/*
					 * 邮件地址：
					 * 有时候抓不到邮件地址，要在通信信息里再找一次。
					 * PMC好像把所有邮件地址在页面源码里倒过来了，需要重新转回来。es.ug.igoloisyf@oahs.nijiur  --> ruijin.shao@fysiologi.gu.se
					 */
					if(pmc.getEmail() != null && !pmc.getEmail().isEmpty()){
						article.setEmail(new StringBuffer(pmc.getEmail()).reverse().toString());
					}else if(pmc.getCorrespondingInfo() != null && !pmc.getCorrespondingInfo().isEmpty()){
						String regex = "[\\w[.-]]+\\.[\\w]+@[\\w[.-]]+";
						Pattern p = Pattern.compile(regex);
						Matcher matcher = p.matcher(pmc.getCorrespondingInfo());
						StringBuffer emails = new StringBuffer();
						while(matcher.find()){
							emails.append(new StringBuffer(matcher.group()).reverse());
							emails.append("|");
						}
						article.setEmail(emails.toString());
					}else{
						article.setEmail("");
					}
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					
					if(pmc.getCorrespondingAuthor() != null && !pmc.getCorrespondingAuthor().isEmpty()){
						article.setCorrespondingAuthor(pmc.getCorrespondingAuthor().split("\\|")[0]);
						article.setFullCorrespondingAuthor(pmc.getCorrespondingAuthor().split("\\|")[0]);
					}else{
						article.setCorrespondingAuthor(""); // 在判断通讯作者的时候再赋值
					}
					if(pmc.getCorrespondingAddress() != null && !pmc.getCorrespondingAddress().isEmpty()){
						article.setCorrespondingAddress(pmc.getCorrespondingAddress());
					}else{
						article.setCorrespondingAddress("");// 在判断通讯作者的时候再赋值
					}
					
					
					article.setKeyword(pmc.getKeyword());
					if(pmc.getpDate() == null || pmc.getpDate().length() > 30){
						article.setpDate("");
					}else{
						article.setpDate(pmc.getpDate());
					}
					article.setSource("PMC");
					article.setSourcePublication(pmc.getSourcePublication());
					article.setTitle(pmc.getTitle());
					article.setTitleIndex(pmc.getTitle().length() >= 250 ? pmc.getTitle().substring(0, 250) : pmc.getTitle()); // 取title前250字符作为索引
					article.setReferrence(pmc.getReferrence());
					article.setFullText(pmc.getFullText());
					article.setCorrespondingInfo(pmc.getCorrespondingInfo());
					article.setType("Article");
					
					Pattern p;
					Matcher matcher;
					Map<String,String> addressMap = new HashMap<String,String>();
					if (pmc.getAddresses() != null && !pmc.getAddresses().trim().equals("")) {
						// 地址，用|号分隔
						// 形如：[ 1 ]Departments of Microbiology and Immunology, Medical University of South Carolina, 86 Jonathan Lucas St., Charleston, SC 29425, USA|[ 2 ]Departments of Medicine, Medical University of South Carolina, 86 Jonathan Lucas St., Charleston, SC 29425, USA|
						// [ n ]代表一个地址
						// 所有的address都以[ 1 ]开头 ， 所以addressses[0]肯定是空，要去除
						List<String> addresses = Arrays.asList(pmc.getAddresses().split("\\|"));		
						for(String add : addresses){
							add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments of Microbiology and Immunology ， 把空的[]去掉
							p = Pattern.compile("\\[ .+ \\]",Pattern.CASE_INSENSITIVE);
							matcher = p.matcher(add);
							if(matcher.find()){
								// 一个地址可能有多个符号，还要分开
								String[] addressId = matcher.group().replaceAll("\\[" , "").replaceAll("\\]" , "").trim().split(",");
								for(String ai : addressId){
									addressMap.put(ai , add.replaceAll("\\[ .+\\]", ""));
								}
							}else{
								if(!addressMap.containsKey("1")){
									addressMap.put("1", add);
								}
							}
						}
						
						
					}
					
					List<Author> authorList = new ArrayList<Author>();

					if (pmc.getAuthors() != null && !pmc.getAuthors().trim().equals("")) { 
						// 作者，形如：
						// Heather E Cunliffe[1,*]| Yuan Jiang[2,*]| Kimberly M Fornace[3]| Fan Yang[2]|| Paul S Meltzer[2]|  ，可能多个|符号连续。
						// ME.A. Vaskova[]|[]|[]| A.E. Stekleneva[]|[]|[]|  作者名的[]里可能为空，甚至整个作者名字为空
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(pmc.getAuthors().split("\\|"));
						int aSize = authors.size();
						int emailScore = 0;
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i);
							if(authorName.isEmpty()){
								continue;
							}
							authorName = authorName.replaceAll("\\[\\p{Space}*\\]", ""); // ME.A. Vaskova[] ， 把空的[]去掉
							
							String regex = "\\[.*\\]";
							String tmp = authorName.replaceAll(regex, ""); // 去掉[1,2,*] 之类的
							if(tmp.isEmpty()){
								continue;
							}
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("PMC");
							
							// 有时候作者名后面会跟一些机构名之类的，要去掉，如
							// Anders Perner the 6S trial groupthe Scandinavian Critical Care Trials Grou
							// Thomas Paffrath[3], the TraumaRegister DGUthe German Pelvic Injury Register of the Deutsche Gesellschaft für Unfallchirurgie
							if(tmp.contains(",")){
								tmp = tmp.substring(0, tmp.indexOf(","));
							}
							if(tmp.contains(" the ")){
								tmp = tmp.substring(0, tmp.indexOf(" the "));
							}
							if(tmp.length() > 50){ //如果还是很长，只取前两个单词作为作者名
								tmp = tmp.split(" ")[0] +" "+ tmp.split(" ")[1];
							}
							author.setFullName(tmp);
							author.setShortName(tmp);

							// set 作者地址
							p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
							matcher = p.matcher(authorName);
							String[] addressIds = null; // 一个作者拥有的地址id符号
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split(",");
								for(int j = 0; j < addressIds.length ;j++){
									if(addressIds[j].matches("[^\\d]*?\\d+[^\\d]*?")){ // 如果包含数字去掉，非数字的字符
										addressIds[j] = addressIds[j].replaceAll("[^\\d]", "");
									}
								}
							}
							if (addressIds == null) {
								author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1")); // 去掉开头的[ n ]
							} else {
								author.setAddress("");
								for (String idStr : addressIds) {
									if(addressMap.get(idStr) != null){
										author.setAddress(author.getAddress().concat(addressMap.get(idStr)).concat("~"));
									}	
								}
								if(author.getAddress().isEmpty()){
									author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1")); // 去掉开头的[ n ]
								}
							}

							
							// 邮件地址可能有多个，以|分隔。作者和邮件之间不一定有对应关系，甚至数量都不一定相同，可能要用类似人名和邮件名的匹配算法。
							// assumedEmail 找出这个作者匹配度最高的邮箱
							Map<String,Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2WithScorelimited(article.getEmail().replaceAll("\\|", ";"), author.getFullName(), "@" , 6);
							
							
							// pmc 不能直接在页面上抓到通讯作者和通讯地址，所以把第一个作者名和邮箱地址能匹配上的作者当做通讯作者
							if(!((String)assumedEmail.get("email")).isEmpty()){
								author.setEmail(((String)assumedEmail.get("email")).trim());
								author.setPriority(0); // 在作者文章关系表里把能直接在网页找到email的作为通讯作者，区别不是在网页上直接抓到的而是后期非通讯作者抓到的
								// 判断是否算作是通讯作者
								if((Integer)assumedEmail.get("total") > emailScore){
									article.setCorrespondingAuthor(author.getFullName());
									article.setFullCorrespondingAuthor(author.getFullName());
									article.setCorrespondingAddress(author.getAddress());
									emailScore = (Integer)assumedEmail.get("total");
								}
							}
							
							authorList.add(author);
						}
						
						/* PMC的通讯作者就是第一个匹配上邮箱的作者，所以不存在通讯作者没邮箱的情况
						//如果存在没有匹配上的邮件和通信作者，默认认为这个邮件是属于这个通信作者的。
						if(article.getEmail() != null && !article.getEmail().isEmpty()){
							List<String> emailList = new ArrayList<String>();
							String[] emailArray = article.getEmail().split("\\|");
							for(String e : emailArray){
								if(!e.isEmpty()){
									emailList.add(e);
								}
							}
							
							List<String> occupiedEmailList = new ArrayList<String>(); // 已确认归属作者的email
							for(Author a : authorList){
								if(a.getPriority() == 0 && (a.getEmail() != null && !a.getEmail().isEmpty())){
									occupiedEmailList.add(a.getEmail());
								}
							}
							
							emailList.removeAll(occupiedEmailList); // 总的email里删除已确认归属作者的email，留下还未确认归属的
							
							for(Author a : authorList){
								if(a.getPriority() == 0 && (a.getEmail() == null || a.getEmail().isEmpty()) && !emailList.isEmpty()){
									a.setEmail(emailList.get(0));
									emailList.remove(0);
								}
							}
						}
						*/
					}
					
					// 通讯作者会抓错，太长了就清空
					if(article.getCorrespondingAuthor() != null && article.getCorrespondingAuthor().length() > 50){
						article.setCorrespondingAuthor("");
					}
					if(article.getFullCorrespondingAuthor() != null && article.getFullCorrespondingAuthor().length() > 50){
						article.setFullCorrespondingAuthor("");
					}
					
					pmcService.saveArticleAndAuthor(article, authorList,pmc);
					System.out.println(pmc.getId() + " parsed");
					
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
	 * 把search_record表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parsePMCSearchRecord() {
		IPMCService pmcService = new PMCServiceImpl(DB);
		int startIndex = START_INDEX;
		int step = STEP;
		while(startIndex <= MAX_ID){ // SELECT MAX(id) from search_pmc_by_publication
		// while (startIndex == 833838) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" SOURCEES = 'PMC' and parsed = 0 ");
			startIndex += step;
			// criteria.put("have_read", 0);
			List<PMC> pmcList = pmcService.selectBySearchRecord(criteria);
			// System.out.println("wosList "+wosList.size());
			if (pmcList.size() != 0) {
				for (PMC pmc : pmcList) {
					try{
					if(pmc.getHaveRead() == 1){
						continue;
					}
					Article article = new Article();
					article.setAbs(pmc.getAbs());
					
					/*
					 * 邮件地址：
					 * 有时候抓不到邮件地址，要在通信信息里再找一次。
					 * PMC好像把所有邮件地址在页面源码里倒过来了，需要重新转回来。es.ug.igoloisyf@oahs.nijiur  --> ruijin.shao@fysiologi.gu.se
					 */
					if(pmc.getEmail() != null && !pmc.getEmail().isEmpty()){
						article.setEmail(new StringBuffer(pmc.getEmail()).reverse().toString());
					}else if(pmc.getCorrespondingInfo() != null && !pmc.getCorrespondingInfo().isEmpty()){
						String regex = "[\\w[.-]]+\\.[\\w]+@[\\w[.-]]+";
						Pattern p = Pattern.compile(regex);
						Matcher matcher = p.matcher(pmc.getCorrespondingInfo());
						StringBuffer emails = new StringBuffer();
						while(matcher.find()){
							emails.append(new StringBuffer(matcher.group()).reverse());
							emails.append("|");
						}
						article.setEmail(emails.toString());
					}else{
						article.setEmail("");
					}
					// 有时候抓到的email会很多，长度超过selleck_edm_article EMAIL 字段长度，去掉后面超过长度的email
					while(article.getEmail().length() > 255){
						article.setEmail(article.getEmail().substring(0, article.getEmail().lastIndexOf("|")));
					}
					
					if(pmc.getCorrespondingAuthor() != null && !pmc.getCorrespondingAuthor().isEmpty()){
						article.setCorrespondingAuthor(pmc.getCorrespondingAuthor().split("\\|")[0]);
						article.setFullCorrespondingAuthor(pmc.getCorrespondingAuthor().split("\\|")[0]);
					}else{
						article.setCorrespondingAuthor(""); // 在判断通讯作者的时候再赋值
					}
					if(pmc.getCorrespondingAddress() != null && !pmc.getCorrespondingAddress().isEmpty()){
						article.setCorrespondingAddress(pmc.getCorrespondingAddress());
					}else{
						article.setCorrespondingAddress("");// 在判断通讯作者的时候再赋值
					}
					
					
					article.setKeyword(pmc.getKeyword());
					if(pmc.getpDate() == null || pmc.getpDate().length() > 30){
						article.setpDate("");
					}else{
						article.setpDate(pmc.getpDate());
					}
					article.setSource("PMC-searchRecord");
					article.setSourcePublication(pmc.getSourcePublication());
					article.setTitle(pmc.getTitle());
					article.setTitleIndex(pmc.getTitle().length() >= 250 ? pmc.getTitle().substring(0, 250) : pmc.getTitle()); // 取title前250字符作为索引
					article.setReferrence(pmc.getReferrence());
					article.setFullText(pmc.getFullText());
					article.setCorrespondingInfo(pmc.getCorrespondingInfo());
					article.setType("Article");
					
					Pattern p;
					Matcher matcher;
					Map<String,String> addressMap = new HashMap<String,String>();
					if (pmc.getAddresses() != null && !pmc.getAddresses().trim().equals("")) {
						// 地址，用|号分隔
						// 形如：[ 1 ]Departments of Microbiology and Immunology, Medical University of South Carolina, 86 Jonathan Lucas St., Charleston, SC 29425, USA|[ 2 ]Departments of Medicine, Medical University of South Carolina, 86 Jonathan Lucas St., Charleston, SC 29425, USA|
						// [ n ]代表一个地址
						// 所有的address都以[ 1 ]开头 ， 所以addressses[0]肯定是空，要去除
						List<String> addresses = Arrays.asList(pmc.getAddresses().split("\\|"));		
						for(String add : addresses){
							add = add.replaceAll("\\[\\p{Space}*\\]", ""); // []Departments of Microbiology and Immunology ， 把空的[]去掉
							p = Pattern.compile("\\[ .+ \\]",Pattern.CASE_INSENSITIVE);
							matcher = p.matcher(add);
							if(matcher.find()){
								// 一个地址可能有多个符号，还要分开
								String[] addressId = matcher.group().replaceAll("\\[" , "").replaceAll("\\]" , "").trim().split(",");
								for(String ai : addressId){
									addressMap.put(ai , add.replaceAll("\\[ .+\\]", ""));
								}
							}else{
								if(!addressMap.containsKey("1")){
									addressMap.put("1", add);
								}
							}
						}
						
						
					}
					
					List<Author> authorList = new ArrayList<Author>();

					if (pmc.getAuthors() != null && !pmc.getAuthors().trim().equals("")) { 
						// 作者，形如：
						// Heather E Cunliffe[1,*]| Yuan Jiang[2,*]| Kimberly M Fornace[3]| Fan Yang[2]|| Paul S Meltzer[2]|  ，可能多个|符号连续。
						// ME.A. Vaskova[]|[]|[]| A.E. Stekleneva[]|[]|[]|  作者名的[]里可能为空，甚至整个作者名字为空
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(pmc.getAuthors().split("\\|"));
						int aSize = authors.size();
						int emailScore = 0;
						for(int i = 0;i < aSize;i++){
							String authorName = authors.get(i);
							if(authorName.isEmpty()){
								continue;
							}
							authorName = authorName.replaceAll("\\[\\p{Space}*\\]", ""); // ME.A. Vaskova[] ， 把空的[]去掉
							
							String regex = "\\[.*\\]";
							String tmp = authorName.replaceAll(regex, ""); // 去掉[1,2,*] 之类的
							if(tmp.isEmpty()){
								continue;
							}
						// for (String authorName : authors) {
							Author author = new Author();
							if(i == 0){
								author.setPriority(1);
							}
							if(i == 1){
								author.setPriority(2);
							}
							author.setSource("PMC-searchRecord");
							
							// 有时候作者名后面会跟一些机构名之类的，要去掉，如
							// Anders Perner the 6S trial groupthe Scandinavian Critical Care Trials Grou
							// Thomas Paffrath[3], the TraumaRegister DGUthe German Pelvic Injury Register of the Deutsche Gesellschaft für Unfallchirurgie
							if(tmp.contains(",")){
								tmp = tmp.substring(0, tmp.indexOf(","));
							}
							if(tmp.contains(" the ")){
								tmp = tmp.substring(0, tmp.indexOf(" the "));
							}
							if(tmp.length() > 50){ //如果还是很长，只取前两个单词作为作者名
								tmp = tmp.split(" ")[0] +" "+ tmp.split(" ")[1];
							}
							author.setFullName(tmp);
							author.setShortName(tmp);

							// set 作者地址
							p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
							matcher = p.matcher(authorName);
							String[] addressIds = null; // 一个作者拥有的地址id符号
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[*1,#2,3,*]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split(",");
								for(int j = 0; j < addressIds.length ;j++){
									if(addressIds[j].matches("[^\\d]*?\\d+[^\\d]*?")){ // 如果包含数字去掉，非数字的字符
										addressIds[j] = addressIds[j].replaceAll("[^\\d]", "");
									}
								}
							}
							if (addressIds == null) {
								author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1")); // 去掉开头的[ n ]
							} else {
								author.setAddress("");
								for (String idStr : addressIds) {
									if(addressMap.get(idStr) != null){
										author.setAddress(author.getAddress().concat(addressMap.get(idStr)).concat("~"));
									}	
								}
								if(author.getAddress().isEmpty()){
									author.setAddress(addressMap.get("1") == null ? "" : addressMap.get("1")); // 去掉开头的[ n ]
								}
							}

							
							// 邮件地址可能有多个，以|分隔。作者和邮件之间不一定有对应关系，甚至数量都不一定相同，可能要用类似人名和邮件名的匹配算法。
							// assumedEmail 找出这个作者匹配度最高的邮箱
							Map<String,Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2WithScorelimited(article.getEmail().replaceAll("\\|", ";"), author.getFullName(), "@" , 6);
							
							
							// pmc 不能直接在页面上抓到通讯作者和通讯地址，所以把第一个作者名和邮箱地址能匹配上的作者当做通讯作者
							if(!((String)assumedEmail.get("email")).isEmpty()){
								author.setEmail(((String)assumedEmail.get("email")).trim());
								author.setPriority(0); // 在作者文章关系表里把能直接在网页找到email的作为通讯作者，区别不是在网页上直接抓到的而是后期非通讯作者抓到的
								// 判断是否算作是通讯作者
								if((Integer)assumedEmail.get("total") > emailScore){
									article.setCorrespondingAuthor(author.getFullName());
									article.setFullCorrespondingAuthor(author.getFullName());
									article.setCorrespondingAddress(author.getAddress());
									emailScore = (Integer)assumedEmail.get("total");
								}
							}
							
							authorList.add(author);
						}
						
						/* PMC的通讯作者就是第一个匹配上邮箱的作者，所以不存在通讯作者没邮箱的情况
						//如果存在没有匹配上的邮件和通信作者，默认认为这个邮件是属于这个通信作者的。
						if(article.getEmail() != null && !article.getEmail().isEmpty()){
							List<String> emailList = new ArrayList<String>();
							String[] emailArray = article.getEmail().split("\\|");
							for(String e : emailArray){
								if(!e.isEmpty()){
									emailList.add(e);
								}
							}
							
							List<String> occupiedEmailList = new ArrayList<String>(); // 已确认归属作者的email
							for(Author a : authorList){
								if(a.getPriority() == 0 && (a.getEmail() != null && !a.getEmail().isEmpty())){
									occupiedEmailList.add(a.getEmail());
								}
							}
							
							emailList.removeAll(occupiedEmailList); // 总的email里删除已确认归属作者的email，留下还未确认归属的
							
							for(Author a : authorList){
								if(a.getPriority() == 0 && (a.getEmail() == null || a.getEmail().isEmpty()) && !emailList.isEmpty()){
									a.setEmail(emailList.get(0));
									emailList.remove(0);
								}
							}
						}
						*/
					}
					
					// 通讯作者会抓错，太长了就清空
					if(article.getCorrespondingAuthor() != null && article.getCorrespondingAuthor().length() > 50){
						article.setCorrespondingAuthor("");
					}
					if(article.getFullCorrespondingAuthor() != null && article.getFullCorrespondingAuthor().length() > 50){
						article.setFullCorrespondingAuthor("");
					}
					
					pmcService.saveArticleAndAuthorForSearchRecord(article, authorList,pmc);
					System.out.println(pmc.getId() + " parsed");
					
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
