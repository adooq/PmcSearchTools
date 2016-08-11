package selleck.email.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.WOS;
import selleck.email.service.IWOSService;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.utils.Constants;

public class WOSParse extends AbstractParser{
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* ******   开始新任务时，需要修改的参数 ****   */
		DB =  Constants.LIFE_SCIENCE_DB; // 进行操作的数据库
		// DB =  Constants.MATERIAL_SCIENCE_DB; // 进行操作的数据库
		START_INDEX = 5304623; // search_wos_by_publication表起始id
		MAX_ID = 542236; // search_wos_by_publication表最后的id，通常是max(id) from search_wos_by_publication
		STEP = 10000; // 一次查询出来的数量
		// **************************************** //
		
		newEmailTableName = "selleck_edm_author_nodup_"
				+DB.split(" ")[0]+"_wos_"
				+new SimpleDateFormat("yyyyMM").format(new Date());
		
		new WOSParse().allProcess();
	}
	
	
	
	/**
	 * 
	 * 把search_wos_by_publication表的内容导入到selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	void importEDMDB() {
		// IWOSService wosService = new WOSServiceImpl(Constants.LIFE_SCIENCE_DB);
		IWOSService wosService = new WOSServiceImpl(DB);
		int startIndex = START_INDEX; // 新更新wos期刊文章起始id
		int step = STEP;
		while(startIndex <= MAX_ID){ // MAX(id) from search_wos_by_publication
		// while (startIndex == 3787769) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" have_read = 0 and id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			// criteria.put("have_read", 0);
			List<WOS> wosList = wosService.selectByExample(criteria);
			// System.out.println("wosList "+wosList.size());
			if (wosList.size() != 0) {
				for (WOS wos : wosList) {
					try{
					if(wos.getHaveRead() == 1){
						continue;
					}
					Article article = new Article();
					article.setAbs(wos.getAbs());
					article.setClassification(wos.getClassification());
					article.setEmail(wos.getEmail().replaceAll("\\s", ""));
					article.setCorrespondingAuthor(wos.getCorrespondingAuthor());
					article.setCorrespondingAddress(wos.getCorrespondingAddress());
					article.setKeyword(wos.getKeyword());
					article.setKeywordPlus(wos.getKeywordPlus());
					article.setpDate(wos.getpDate());
					article.setResearch(wos.getResearch());
					article.setSource("WOS");
					article.setSourcePublication(wos.getSourcePublication());
					article.setTitle(wos.getTitle());
					article.setType(wos.getType());
					article.setTitleIndex(wos.getTitle().length() >= 250 ? wos.getTitle().substring(0, 250) : wos.getTitle()); // 取title前250字符作为索引

					List<Author> authorList = new ArrayList<Author>();

					if (wos.getAuthors() != null && !wos.getAuthors().trim().equals("") && !wos.getAuthors().trim().equals("[Anonymous]")) { // 作者名会有  [Anonymous]
						wos.setAuthors(wos.getAuthors().replaceAll("更多内容更少内容", ""));// 抓取器误抓多余的字符，先去掉
						// 作者，形如：
						// Ping,SY(Ping,Szu-Yuan)[2]|Wu,CL(Wu,Chia-Lun)[1]|Yu,DS(Yu,Dah-Shyong)[1,2]
						// |号分隔。名字后的[n]对应地址中的[n]
						List<String> authors = Arrays.asList(wos.getAuthors().split("\\|"));
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
							author.setSource("WOS");
							String regex = "\\[[,\\d]+\\]";
							String tmp = authorName.replaceAll(regex, "").replaceAll("\\s", ""); // 去掉[1,2] \\s之类的
							int startIdx = tmp.indexOf("(");  // 作者名肯定包含()，不用判断startIdx是否=-1
							int endIdx = tmp.indexOf(")");
							if (startIdx != -1 && endIdx != -1) { // 保险起见，还是判断一下
								String fullName = tmp.substring(startIdx + 1, endIdx);
								fullName = splitName(fullName);
								String shortName = tmp.substring(0, startIdx);
								shortName = splitName(shortName);
								author.setFullName(fullName);
								author.setShortName(shortName);
							} else {
								tmp = splitName(tmp);
								author.setFullName(tmp);
								author.setShortName(tmp);
							}

							Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							String[] addressIds = null;
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[1,2,3]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split(",");
							}

							if (wos.getAddresses() != null
									&& !wos.getAddresses().trim().equals("")) {
								// 地址，形如：[ 1 ] Chungnam Natl Univ, Taejon
								// 305764, South Korea 增强组织信息的名称 Chungnam National University|[ 2 ] Korea Inst Machinery & Mat, Taejon 305764, South Korea增强组织信息的名称Korea Institute of Machinery & Materials|
								// [ n ]代表一个地址，后面可能有一个或多个增强组织信息的名称，用|号分隔。
								// 所有的address都以[ 1 ]开头 ， 所以addressses[0]肯定是空，要去除
								List<String> addresses = Arrays.asList(wos.getAddresses().split("\\[ \\d+ \\]"));
								if (addressIds == null) {
									author.setAddress(addresses.get(1).split("增强组织信息的名称")[0].replaceAll("[^\\p{Print}]", "").trim());
								} else {
									author.setAddress("");
									author.setOrganization("");
									for (String idStr : addressIds) {
										author.setAddress(author.getAddress().concat(addresses.get(new Integer(idStr)).split("增强组织信息的名称")[0].replaceAll("[^\\p{Print}]", "")).trim().concat("~"));
										if(addresses.get(new Integer(idStr)).split("增强组织信息的名称").length > 1){
											author.setOrganization(author.getOrganization().concat(addresses.get(new Integer(idStr)).split("增强组织信息的名称")[1].replaceAll("[^\\p{Print}]", "")).trim());
										}	
									}
								}
							}
							
							// 判断是否是通讯作者
							// 邮件地址可能有多个，以"; "或","分隔，用判断非通讯作者邮箱的方法（人名和邮箱前缀匹配）来判断这些邮箱属于哪些作者
							Map<String,Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2(article.getEmail().replaceAll(",", ""), author.getFullName(), "@");
							if (wos.getCorrespondingAuthor() != null && !wos.getCorrespondingAuthor().trim().equals("")) {
								String trimCAName = wos.getCorrespondingAuthor().replaceAll("\\s", "");
								if (trimCAName.equals(author.getShortName().replaceAll("\\s", ""))) {
									// 形如：Chungnam Natl Univ, Taejon 305764,
									// South Korea.增强组织信息的名称Chungnam National
									// University|
									// 增强组织信息的名称之前是地址，之后是组织名（用|分割多个组织名）
									if (wos.getCorrespondingAddress() != null && !wos.getCorrespondingAddress().trim().equals("")) {
										String[] cAddress = wos.getCorrespondingAddress().split("增强组织信息的名称");
										author.setAddress(cAddress[0]);
										if (cAddress.length > 1) {
											author.setOrganization(cAddress[1].replaceAll("[^\\p{Print}]", "").trim());
										}
									}
									
									// 如果是通讯作者，和email匹配时打分要求比较低
									if(!((String)assumedEmail.get("email")).isEmpty()){
										author.setEmail(((String)assumedEmail.get("email")).trim());
									}
									author.setPriority(0);
									article.setFullCorrespondingAuthor(author.getFullName().trim());
								}else{
									// 如果不是通讯作者，和email匹配时打分要求较高。
									if(!((String)assumedEmail.get("email")).isEmpty() && ((Integer)assumedEmail.get("total")) >= 7){
										author.setEmail(((String)assumedEmail.get("email")).trim());
									}
								}
								
							}
							
							authorList.add(author);
						}
						
						//如果存在没有匹配上的邮件和通信作者，默认认为这个邮件是属于这个通信作者的。
						if(article.getEmail() != null && !article.getEmail().isEmpty()){
							List<String> emailList = new ArrayList<String>();
							String[] emailArray = article.getEmail().split(";");
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
						
					}

					wosService.saveArticleAndAuthor(article, authorList,wos);
					System.out.println(wos.getId() + " parsed");
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
