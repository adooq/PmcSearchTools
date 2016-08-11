package selleck.email.controller;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;
import selleck.email.pojo.NewEmail;
import selleck.email.pojo.WOS;
import selleck.email.service.IWOSService;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.utils.Constants;

/**
 * t_wos_new_email是search_wos_all_attributes表里有但t_edm_email表里没有的新的email。
 * t_wos_edm_dup是重复的email。 FindNewEmailAndFullname是查找t_wos_new_email和对应的通讯作者的全名
 * 
 * @author fscai
 * 
 */
public class FindNewEmailAndFullname {
	private static int newEmailNum = 3;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IWOSService wosService = new WOSServiceImpl(Constants.LIFE_SCIENCE_DB);
		Criteria criteria1 = new Criteria();
		criteria1.setOrderByClause("full_name");
		criteria1.setOracleStart(369);
		criteria1.setOracleEnd(newEmailNum);
		List<String> newEmails = wosService.selectNewEmail(criteria1);
		//System.out.println("newEmails size: "+newEmails.size());
		for (String ne : newEmails) {
			NewEmail newEmail = new NewEmail();
			newEmail.setEmail(ne);

			Criteria criteria = new Criteria();
			//System.out.println("newEmail.getEmail(): "+newEmail.getEmail());
			criteria.put("email", newEmail.getEmail());
			criteria.setOracleStart(0);
			criteria.setOracleEnd(1);
			WOS wos = wosService.selectWOSByNewEmail(criteria);

			if (wos.getAuthors() != null && !wos.getAuthors().trim().equals("")
					&& !wos.getAuthors().trim().equals("[Anonymous]")) { // 作者名会有
																			// [Anonymous]
				wos.setAuthors(wos.getAuthors().replaceAll("更多内容更少内容", ""));// 抓取器误抓多余的字符，先去掉
				// 作者，形如：
				// Ping,SY(Ping,Szu-Yuan)[2]|Wu,CL(Wu,Chia-Lun)[1]|Yu,DS(Yu,Dah-Shyong)[1,2]
				// |号分隔。名字后的[n]对应地址中的[n]
				List<String> authors = Arrays.asList(wos.getAuthors().split("\\|"));
				for (String authorName : authors) {
					Author author = new Author();
					author.setSource("WOS");
					String regex = "\\[[,\\d]+\\]";
					String tmp = authorName.replaceAll(regex, "").replaceAll("\\s", ""); // 去掉[1,2] \\s之类的
					int startIdx = tmp.indexOf("("); // 作者名肯定包含()，不用判断startIdx是否=-1
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

					// 判断是否是通讯作者
					if (wos.getCorrespondingAuthor() != null
							&& !wos.getCorrespondingAuthor().trim().equals("")) {
						
						String trimCAName = wos.getCorrespondingAuthor().replaceAll("\\s", "");
//						System.out.println(trimCAName);
//						System.out.println(author.getShortName().replaceAll("\\s", ""));
						if (trimCAName.equals(author.getShortName().replaceAll("\\s", ""))) {
							// 形如：Chungnam Natl Univ, Taejon 305764,
							// South Korea.增强组织信息的名称Chungnam National
							// University|
							// 增强组织信息的名称之前是地址，之后是组织名（用|分割多个组织名）
							if (wos.getCorrespondingAddress() != null
									&& !wos.getCorrespondingAddress().trim()
											.equals("")) {
								String[] cAddress = wos
										.getCorrespondingAddress().split(
												"增强组织信息的名称");
								author.setAddress(cAddress[0]);
								if (cAddress.length > 1) {
									author.setOrganization(cAddress[1]);
								}
							}

							author.setEmail(wos.getEmail());
							
							System.out.println("full name: "+author.getFullName());
							newEmail.setFullName(author.getFullName());
						}
					}
				}

				wosService.updateNewEmail(newEmail);
			}
		}
	}
	
	
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

}
