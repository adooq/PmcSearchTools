package selleck.email.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.controller.CheckGoogelSearchEmail;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.WOS;
import selleck.email.service.IWOSService;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.utils.Constants;

public class WOSParseForSearchRecord {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		parseWOS();
	}
	
	/**
	 * 
	 * 把252mysql的emailhunter库的search_record表的内容导入到local的emailhunter的selleck_edm_author,selleck_edm_article,
	 * selleck_edm_article_author_rel
	 */
	private static void parseWOS() {
		IWOSService wosService = new WOSServiceImpl(Constants.LIFE_SCIENCE_DB);
		int startIndex = 3586278; // SELECT MIN(id) from search_record where INSTR(pickdate, '2014') = 1
		int step = 10000;
		// while(startIndex <= 3826721){ //  SELECT max(id) from search_record
		while(startIndex <= 3891052){
		// while (startIndex == 0) { // for test
			Criteria criteria =new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" INSTR(pickdate, '2014') = 1 and parsed = 0 and id >= " + startIndex + " and id < "+(startIndex+step));
			startIndex += step;
			// criteria.put("have_read", 0);
			List<WOS> wosList = wosService.selectFromSearchRecord(criteria);
			// System.out.println("wosList "+wosList.size());
			if (wosList.size() != 0) {
				for (WOS wos : wosList) {
					if(wos.getHaveRead() == 1){
						continue;
					}
					try{
					Article article = new Article();
					article.setAbs(wos.getAbs());
					article.setClassification(wos.getClassification());
					article.setEmail(wos.getEmail().toLowerCase()); // 同一个作者会把同一个email有时大写有时小写，email统一为小写
					article.setCorrespondingAuthor(wos.getCorrespondingAuthor());
					article.setCorrespondingAddress(wos.getCorrespondingAddress());
					article.setKeyword(wos.getKeyword());
					article.setKeywordPlus(wos.getKeywordPlus());
					article.setpDate(wos.getpDate());
					article.setResearch(wos.getResearch());
					article.setSource("WOS-search_record");
					article.setSourcePublication(wos.getSourcePublication());
					article.setTitle(wos.getTitle());
					article.setType(wos.getType());
					article.setTitleIndex(wos.getTitle().length() >= 250 ? wos.getTitle().substring(0, 250) : wos.getTitle()); // 取title前250字符作为索引

					List<Author> authorList = new ArrayList<Author>();

					if (wos.getAuthors() != null && !wos.getAuthors().trim().equals("") && !wos.getAuthors().trim().equals("[Anonymous]")) { // 作者名会有  [Anonymous]
						wos.setAuthors(wos.getAuthors().replaceAll("更多内容更少内容", ""));// 抓取器误抓多余的字符，先去掉
					
						/* @deprecated
						作者，形如：
						Eisinger, DA (Eisinger, Daniela A.)  Ammer, H (Ammer, Hermann) 
						把各个作者名字分离开，形如Eisinger, DA (Eisinger, Daniela A.)
						List<String> authors= new ArrayList<String>(); 
						Pattern p = Pattern.compile("\\w[\\w\\.,\\- ]+\\([\\w\\.,\\- ]+\\)",Pattern.CASE_INSENSITIVE);
						Matcher matcher = p.matcher(wos.getAuthors());
						while(matcher.find()){
							authors.add(matcher.group());
						}
						*/
						
						// 作者，形如：
						// Ping,SY(Ping,Szu-Yuan)[2]|Wu,CL(Wu,Chia-Lun)[1]|Yu,DS(Yu,Dah-Shyong)[1, 2]
						// |号分隔。名字后的[n]对应地址中的[n] ，[n, m] 会包含空格
						List<String> authors= Arrays.asList(wos.getAuthors().split("\\|"));
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
							String tmp = authorName.replaceAll("\\s", ""); // 去掉 \\s之类的
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

							/* 原来的search_record表没有详细记录地址，不可用。
							 * 现在已经调整到和search_wos_by_publication中地址一样，就跟wos地址一样解析
							// search_record中的地址不可用,emailselleck的author_email_suffix中搜地址
							Criteria criteria1 =new Criteria();
							// criteria.setOracleStart(startIndex);
							// criteria.setOracleEnd(1000);
							criteria1.setWhereClause(" title = '" + StringUtils.toSqlForm(article.getTitle()) + "' and author = '"+StringUtils.toSqlForm(author.getFullName())+"'");
							String address = wosService.findAddressByAuthorName(criteria1);
							author.setAddress(address);
							*/
							
							String regex = "\\[[ ,\\d]+\\]"; // [1, 2] 与wos稍有不同，会包含空格
							Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
							Matcher matcher = p.matcher(authorName);
							String[] addressIds = null;
							if (matcher.find()) {
								String idsStr = authorName.substring(matcher.start(), matcher.end()); // idsStr形如[1,2,3]
								addressIds = idsStr.substring(1,idsStr.length() - 1).split("[, ]+");
							}

							if (wos.getAddresses() != null
									&& !wos.getAddresses().trim().equals("")) {
								// 地址，形如：[ 1 ] Chungnam Natl Univ, Taejon
								// 305764, South Korea增强组织信息的名称Chungnam National
								// University|[ 2 ] Korea Inst Machinery & Mat,
								// Taejon 305764, South Korea增强组织信息的名称Korea
								// Institute of Machinery & Materials|
								// [ n ]代表一个地址，后面可能有一个或多个增强组织信息的名称，用|号分隔。
								// 所有的address都以[ 1 ]开头 ， 所以addressses[0]肯定是空，要去除
								List<String> addresses = Arrays.asList(wos.getAddresses().split("\\[ \\d+ \\]"));
								if (addressIds == null) {
									author.setAddress(addresses.get(1).trim());
								} else {
									author.setAddress("");
									for (String idStr : addressIds) {
										author.setAddress(author.getAddress().concat(addresses.get(new Integer(idStr))).trim().concat("~"));
									}
									
								}
							}
							
							// 判断是否是通讯作者
							// 邮件地址可能有多个，以"; "分隔，用判断非通讯作者邮箱的方法（人名和邮箱前缀匹配）来判断这些邮箱属于哪些作者
							Map<String,Object> assumedEmail = CheckGoogelSearchEmail.getRealEmail2(article.getEmail(), author.getFullName(), "@");
							if (wos.getCorrespondingAuthor() != null
									&& !wos.getCorrespondingAuthor().trim().equals("") 
									&& wos.getCorrespondingAuthor().replaceAll("\\s", "").equals(author.getShortName().replaceAll("\\s", ""))) {
									// 形如：Chungnam Natl Univ, Taejon 305764,
									// South Korea.增强组织信息的名称Chungnam National
									// University|
									// 增强组织信息的名称之前是地址，之后是组织名（用|分割多个组织名）
									if (wos.getCorrespondingAddress() != null && !wos.getCorrespondingAddress().trim().equals("")) {
										author.setAddress(wos.getCorrespondingAddress());
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

					wosService.saveArticleAndAuthorFromSearchRecord(article, authorList,wos);
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
	 * @param name
	 * @return
	 */
	private static String splitName(String name){
		Pattern p = Pattern.compile("\\B[A-Z]\\.",Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(name);
		while(matcher.find()){
			name =  name.replace(matcher.group()," "+matcher.group());
		}
		name = name.replaceAll(",", ", "); // 为,后面加空格，为了美观
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
