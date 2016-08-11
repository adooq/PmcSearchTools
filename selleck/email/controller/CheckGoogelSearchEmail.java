package selleck.email.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleRecord;
import selleck.email.pojo.GoogleSearch;
import selleck.email.pojo.GoogleSearchEmail;
import selleck.email.service.IGoogleRecordService;
import selleck.email.service.IGoogleSearchEmailService;
import selleck.email.service.IGoogleSearchService;
import selleck.email.service.impl.GoogleRecordServiceImpl;
import selleck.email.service.impl.GoogleSearchEmailServiceImpl;
import selleck.email.service.impl.GoogleSearchServiceImpl;
import selleck.utils.StringUtils;

/**
 * 重新给google_search_email里的all_email打分
 * 
 * @author fscai
 * 
 */
public class CheckGoogelSearchEmail {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IGoogleSearchEmailService gseService = new GoogleSearchEmailServiceImpl();
		IGoogleRecordService grService = new GoogleRecordServiceImpl();
		IGoogleSearchService gsService = new GoogleSearchServiceImpl();
		Criteria criteria = new Criteria();
		int idIndex = 1;
		int step = 10000;
		// while (idIndex <= 100) { // for test
		while(idIndex <= 3501624){ // select MAX(id) from google_search_email
			// google_search_email
			criteria.setWhereClause(" id >= " + idIndex + "  and id < " + (idIndex + step));
			idIndex = idIndex + step;
			try {
				List<GoogleSearchEmail> googleSearchEmails = gseService.selectGSEmailByCriteria(criteria);
				for (GoogleSearchEmail gse : googleSearchEmails) {
					System.out.println("id " + gse.getId()+"  ");
					String allEmail = gse.getAllEmail();
					String fullName = gse.getFullAuthor();
					String emailSuffix = gse.getEmailMark();
					Map<String, Object> rs = getRealEmail2(allEmail, fullName,emailSuffix);
					gse.setGetEmail((String) rs.get("email"));
					gse.setScore((Integer) rs.get("total"));
					gse.setDateTime(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date())); // 2013.12.02 16:33:43
					gseService.updateGetEmail(gse);
					
					
					if (gse.getGetEmail() == null	|| gse.getGetEmail().isEmpty()) {
						System.out.println(gse.getId() + "  no get email");
						grService.deleteByGoogleSearchEmail(gse);
						continue;
					}

					Criteria criteria1 = new Criteria();
					criteria1.setWhereClause(" title_id = (select gs.title_id from emailselleck.google_search gs where gs.id = "
									+ gse.getDataId()
									+ " ) and full_author = '"
									+ StringUtils.toSqlForm(gse.getFullAuthor()) + "' ");
					List<GoogleRecord> existGR = grService.selectByCriteria(criteria1);
					if (existGR.size() == 0) {
						System.out.println("not exist google_record");
						Criteria gsCriteria = new Criteria();
						gsCriteria.setWhereClause(" id = " + gse.getDataId());
						List<GoogleSearch> gsTemp = gsService.selectByCriteria(gsCriteria);
						if (gsTemp.size() != 0) {
							GoogleSearch gs = gsTemp.get(0);
							GoogleRecord gr = new GoogleRecord();
							gr.setAddress(gs.getAddress());
							gr.setBig(gs.getBig());
							gr.setDictKeys(gs.getDictKeys());
							gr.setEmail(gse.getGetEmail());
							gr.setFullName(gse.getFullAuthor());
							gr.setHtmlTitle(gs.getHtmlTitle());
							gr.setInterests(gs.getInterests());
							gr.setKeywords(gs.getKeywords());
							gr.setMatchKeys(gs.getMatchKeys());
							gr.setPickDate(gse.getDateTime());
							gr.setProduct(gs.getProduct());
							gr.setRealId(1);
							gr.setShortName(gs.getFullAuthor());
							gr.setSmall(gs.getSmall());
							gr.setSource("wos");
							gr.setTitle(gs.getTitle());
							gr.setTitleId(gs.getTitleId());

							grService.insertGoogleRecord(gr);
						}

					} else {
						System.out.println("exist google_record");
						grService.updateEmailByGoogleSearchEmail(gse);
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// for test
		// Map<String, Object> rsMap = getRealEmail2(
		// "jpdaoust@uottawa.ca;ruor@uottawa.ca;jean-philippe.thivierge@uottawa.ca;jleblond@uottawa.ca;",
		// "D'Aoust, Jean-Philippe","@uottawa.ca");
		//
		// System.out.println(rsMap);

	}
	
	/**
	 * 把邮箱前缀和人名进行匹配，找到匹配度最高的那个email。
	 * 不把全名分成x y z 三部分，直接合在一起分词
	 * 
	 * @param allEmail
	 *            所有找到的email ，分号分隔。
	 * @param name
	 *            作者全名
	 * @param flag
	 *            邮箱后缀。e.g @hkucc.hku.hk 。 如果只输入@符号，表示任何email后缀都能匹配。
	 * @param limitedScore 只获得>=limitedScore打分的email 
	 * @return Map<String, Object> <"email","得分最高的email"> <"total","最高的得分">
	 *         <"count","allEmail中总共有几个email">
	 * @see  selleck_google_robot.GooglePick.getRealEmail2()  两个算法一样，如有修改要保持一致
	 */ 
	public static Map<String, Object> getRealEmail2WithScorelimited(String allEmail,String name, String flag , int limitedScore){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("email", "");
		result.put("total", 0);
		result.put("count", 0);
		if (allEmail == null || allEmail.isEmpty()) {
			return result;
		}
		
		allEmail = allEmail.replaceAll("\\s", "");
		name = name.replaceAll("'", ""); // 去单引号'

		// 把驼峰式拼写的名字分开 e.g WangPing -> Wang Ping
		Pattern p = Pattern.compile("([a-z][A-Z][a-z]+)");
		Matcher matcher = p.matcher(name);
		while (matcher.find()) {
			String match = matcher.group();
			String replace = match.substring(0, 1) + " " + match.substring(1);
			name = name.replaceAll(match, replace).trim();
		}
		name = name.toLowerCase();
		String[] nameArr = name.split("(\\s*[\\.,\\- ]+\\s*)+"); // 以.,- 分隔
		List<String> names = new ArrayList<String>(nameArr.length);
		List<String> namesTemp = new ArrayList<String>(nameArr.length);
		for (String n : nameArr) {
			names.add(n);
			namesTemp.add(n);
		}
		// System.out.println("names: "+names);

		// Arrays.sort(names,new StrLenComparator()); // 按名字每个部分的长度，从长到短排列。

		String[] emails = allEmail.split(";");
		result.put("count", emails.length);
		int maxScore = 0;
		String suffix = flag.replace("@", "").toLowerCase();
		for (String email : emails) {
			int score = 0;
			if(email == null || email.isEmpty()){
				continue;
			}
			if (!suffix.isEmpty() && !(email.contains("@") && email.toLowerCase().contains(suffix))) {
				continue;
			}
			namesTemp.clear();
			for (String n : names) {
				namesTemp.add(n);
			}
			// System.out.println("namesTemp: "+namesTemp);
			int index = email.indexOf("@");
			if(index == -1){
				continue;
			}
			String emailPrefix = email.substring(0, index).toLowerCase();
			// System.out.println("emailPrefix: "+emailPrefix);

			boolean containAll = true; // 姓名完全包含在邮件前缀中
			boolean containPart = false; // 姓名部分包含在邮件前缀中
			boolean startWithPart = false; // 邮件前缀以姓名缩写开头

			// 第一轮完全匹配 每个加3分
			List<String> toRemove = new ArrayList<String>();
			for (String partOfName : namesTemp) {
				if (partOfName.length() != 1) {
					if (emailPrefix.contains(partOfName)) {
						// System.out.println("full contain: "+partOfName);
						emailPrefix = emailPrefix.replaceFirst(partOfName, "");
						toRemove.add(partOfName);
						score += 3;
					} else {
						containAll = false;
					}
				}
			}
			namesTemp.removeAll(toRemove);
			toRemove.clear();

			// 第二轮部分匹配 alexanderJ alexJ 。每个加2分。
			for (String partOfName : namesTemp) {
				if (partOfName.length() != 1) {
					String commonString = null;
					String[] prefixSplit = emailPrefix.split("[-\\.]");
					for (String ps : prefixSplit) {
						commonString = getStartWithSubstring(ps, partOfName);
						if (commonString != null) {
							break;
						}
					}
					if (commonString != null) {
						// System.out.println("partail contain: "+commonString);
						containPart = true;
						emailPrefix = emailPrefix.replaceFirst(commonString, "");
						toRemove.add(partOfName);
						score += 2;
					} else {
						containAll = false;
					}
				}
			}
			namesTemp.removeAll(toRemove);
			toRemove.clear();

			// 第三轮单个字母缩写匹配，每个加1分
			for (String partOfName : namesTemp) {
				String a = partOfName.substring(0, 1); // 名字某个部分的首字母
				if (emailPrefix.contains(a)) {
					// System.out.println("short: "+a);
					emailPrefix = emailPrefix.replaceFirst(a, "");
					toRemove.add(partOfName);
					score += 1;
				}
			}
			namesTemp.removeAll(toRemove);
			toRemove.clear();

			// 如果邮件前缀的字母全匹配到，加3分
			boolean hasOthers = true; // 含有除姓名外的字符 e.g michael2014
			emailPrefix = emailPrefix.replaceAll("\\.|\\-|\\d", "");
			if (emailPrefix.isEmpty()) {
				hasOthers = false;
				// System.out.println("final emailPrefix is empty ");
				if (score > 1) { // 如果匹配度很低，就算全都匹配到也不加分。 针对形如: 0928@seed.net.tw
					score += 3;
				}
			} else {
				// System.out.println("final emailPrefix: "+emailPrefix);
			}
			// System.out.println("email score: "+score);
			
			// 分数相等的情况优先取前面的email，因为google上搜的相关度更高。
			// if (score > 0 && score > maxScore) { // 看一下1分和2分的，虽然1分2分的几乎都不是正确的email
			if (score >= limitedScore && score > maxScore) {
				// if(score > maxScore){ 
				maxScore = score;
				result.put("total", maxScore);
				result.put("email", email);
			}
			// System.out.println();
		}

		return result;
	}

	/**
	 * 把邮箱前缀和人名进行匹配，找到匹配度最高的那个email。
	 * 不把全名分成x y z 三部分，直接合在一起分词
	 * 
	 * @param allEmail
	 *            所有找到的email ，分号分隔。
	 * @param name
	 *            作者全名
	 * @param flag
	 *            邮箱后缀。e.g @hkucc.hku.hk 。 如果只输入@符号，表示任何email后缀都能匹配。
	 * @return Map<String, Object> <"email","得分最高的email"> <"total","最高的得分">
	 *         <"count","allEmail中总共有几个email">
	 * @see  selleck_google_robot.GooglePick.getRealEmail2()  两个算法一样，如有修改要保持一致
	 */
	public static Map<String, Object> getRealEmail2(String allEmail,String name, String flag) {
		return getRealEmail2WithScorelimited(allEmail,name, flag ,3);
	}


	private static final Map<String, Integer> scoreMap = new HashMap<String, Integer>();

	// 这个打分可能有问题，叫万总重新列个打分表
	static {
		scoreMap.put("XY", 3);
		scoreMap.put("YX", 3);
		scoreMap.put("Xy", 2);
		scoreMap.put("yX", 2);
		scoreMap.put("xY", 2);
		scoreMap.put("Yx", 2);
		scoreMap.put("XZ", 3);
		scoreMap.put("ZX", 3);
		scoreMap.put("xZ", 2);
		scoreMap.put("Zx", 2);
		scoreMap.put("Xz", 2);
		scoreMap.put("zX", 2);
		scoreMap.put("YZ", 3);
		scoreMap.put("ZY", 3);
		scoreMap.put("Yz", 2);
		scoreMap.put("zY", 2);
		scoreMap.put("Zy", 2);
		scoreMap.put("yZ", 2);
		scoreMap.put("XYZ", 5);
		scoreMap.put("XZY", 5);
		scoreMap.put("YZX", 5);
		scoreMap.put("YXZ", 5);
		scoreMap.put("ZXY", 5);
		scoreMap.put("ZYX", 5);
		scoreMap.put("xyz", 1);
		scoreMap.put("xzy", 1);
		scoreMap.put("yxz", 1);
		scoreMap.put("yzx", 1);
		scoreMap.put("zxy", 1);
		scoreMap.put("zyx", 1);
		scoreMap.put("Xyz", 2);
		scoreMap.put("Xzy", 2);
		scoreMap.put("Yxz", 2);
		scoreMap.put("Yzx", 2);
		scoreMap.put("Zxy", 2);
		scoreMap.put("Zyx", 2);
		scoreMap.put("yXz", 2);
		scoreMap.put("zXy", 2);
		scoreMap.put("xYz", 2);
		scoreMap.put("zYx", 2);
		scoreMap.put("xZy", 2);
		scoreMap.put("yZx", 2);
		scoreMap.put("yzX", 2);
		scoreMap.put("zyX", 2);
		scoreMap.put("xzY", 2);
		scoreMap.put("zxY", 2);
		scoreMap.put("xyZ", 2);
		scoreMap.put("yxZ", 2);
		scoreMap.put("XYz", 4);
		scoreMap.put("YXz", 4);
		scoreMap.put("XzY", 4);
		scoreMap.put("YzX", 4);
		scoreMap.put("zXY", 4);
		scoreMap.put("zYX", 4);
		scoreMap.put("XyZ", 4);
		scoreMap.put("ZyX", 4);
		scoreMap.put("yXZ", 4);
		scoreMap.put("yZX", 4);
		scoreMap.put("XZy", 4);
		scoreMap.put("ZXy", 4);
		scoreMap.put("xYZ", 4);
		scoreMap.put("xZY", 4);
		scoreMap.put("YxZ", 4);
		scoreMap.put("ZxY", 4);
		scoreMap.put("YZx", 4);
		scoreMap.put("ZYx", 4);
		scoreMap.put("x*Z", 4);
		scoreMap.put("x**z**", 2);
		scoreMap.put("X", 2);
		scoreMap.put("Y", 1);
		scoreMap.put("Z", 1);
	}

	private static final String[] nameRules = { "XY", "YX", "Xy", "yX", "xY",
			"Yx", "XZ", "ZX", "xZ", "Zx", "Xz", "zX", "YZ", "ZY", "Yz", "zY",
			"Zy", "yZ", "XYZ", "XZY", "YZX", "YXZ", "ZXY", "ZYX", "xyz", "xzy",
			"yxz", "yzx", "zxy", "zyx", "Xyz", "Xzy", "Yxz", "Yzx", "Zxy",
			"Zyx", "yXz", "zXy", "xYz", "zYx", "xZy", "yZx", "yzX", "zyX",
			"xzY", "zxY", "xyZ", "yxZ", "XYz", "YXz", "XzY", "YzX", "zXY",
			"zYX", "XyZ", "ZyX", "yXZ", "yZX", "XZy", "ZXy", "xYZ", "xZY",
			"YxZ", "ZxY", "YZx", "ZYx", "x*Z", "x**z**", "X", "Y", "Z" };

	private static final int[] ruleScore = { 3, 3, 2, 2, 2, 2, 3, 3, 2, 2, 2,
			2, 3, 3, 2, 2, 2, 2, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 2, 2, 2,
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4,
			4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 1, 1, 1 };

	/**
	 * @param allEmail
	 *            所有找到的email ，分号分隔。
	 * @param name
	 *            作者全名
	 * @param flag
	 *            邮箱后缀。e.g @hkucc.hku.hk
	 * @return Map<String, Object> <"email","得分最高的email"> <"total","最高的得分">
	 *         <"count","allEmail中总共有几个email">
	 */

	public static Map<String, Object> getRealEmail(String allEmail,
			String name, String flag) {
		Map<String, Object> result = new HashMap<String, Object>();
		String[] emails = allEmail.split(";");
		String author = name.replaceAll(",|\\.", " ").replace("-", " ").trim();
		String[] names = author.toLowerCase().split(" ");
		String suffix = flag.replace("@", "");
		int[] sure = new int[emails.length];
		for (int i = 0; i < emails.length; i++) {
			// 开始根据规则打分
			if (!(emails[i].contains("@") && emails[i].contains(suffix)))
				continue;
			int index = emails[i].indexOf("@");
			String onlyEmail = emails[i].substring(0, index);
			onlyEmail = onlyEmail.replaceAll("\\.|-|_", "");
			if (onlyEmail.length() < 3)
				continue;
			// 名称简称匹配
			for (int k = 0; k < nameRules.length; k++) {
				String onlyName = getNameTogether(names, nameRules[k]);
				if (onlyName.equals(""))
					continue;
				if (onlyName.length() > 3 && onlyEmail.contains(onlyName)) {
					sure[i] += ruleScore[k];
				} else if (onlyName.length() == 3 && onlyEmail.length() > 2) {
					String prefix = onlyEmail.substring(0, onlyName.length());
					if (prefix.equals(onlyName))
						sure[i] += ruleScore[k];
				}
				if (onlyEmail.equals(onlyName))
					sure[i] += 2;
			}
		}
		int total = 0;
		String email = "";
		for (int i = 0; i < sure.length; i++) {
			if (sure[i] > total) {
				email = emails[i];
				total = sure[i];
			}
		}
		result.put("email", email);
		result.put("total", total);
		// result.put("count", emails.length);
		result.put("count", allEmail.trim().isEmpty() ? "0" : emails.length);
		return result;
	}

	// 根据作者的名称，组合成邮箱Email的规则大全
	private static String getNameTogether(String[] names, String type) {
		String name = "";// 返回组合成的Email名称
		// 删除不必要的组合信息
		for (int i = 1; i < names.length - 1; i++)
			name = name + names[i];
		if ((names[0].length() < 3 && type.contains("X"))
				|| (names[names.length - 1].length() < 3 && type.contains("Z"))
				|| (name.length() < 3 && type.contains("Y")))
			return "";

		name = "";
		if (type.equals("X")) {
			name = names[0];
		} else if (type.equals("Z")) {
			name = names[names.length - 1];
		} else if (type.equals("XZ")) {
			name = names[0] + names[names.length - 1];
		} else if (type.equals("ZX")) {
			name = names[names.length - 1] + names[0];
		} else if (type.equals("xZ")) {
			name = names[0].substring(0, 1) + names[names.length - 1];
		} else if (type.equals("Zx")) {
			name = names[names.length - 1] + names[0].substring(0, 1);
		} else if (type.equals("Xz")) {
			name = names[0] + names[names.length - 1].substring(0, 1);
		} else if (type.equals("zX")) {
			name = names[names.length - 1].substring(0, 1) + names[0];
		} else if (type.equals("x*Z")) {
			int index = 2;
			if (names[0].length() < 2)
				index = names[0].length();
			name = names[0].substring(0, index) + names[names.length - 1];
		} else if (type.equals("x**z**")) {
			int index = 4;
			if (names[0].length() < 4)
				index = names[0].length();
			int end = 4;
			if (names[names.length - 1].length() < 4)
				end = names[names.length - 1].length();
			name = names[0].substring(0, index)
					+ names[names.length - 1].substring(0, end);
		}
		if (names.length > 2) {
			if (type.contains("Y")) {
				for (int i = 1; i < names.length - 1; i++)
					name = name + names[i];
				if (type.equals("XY")) {
					name = names[0] + name;
				} else if (type.equals("YX")) {
					name = name + names[0];
				} else if (type.equals("xY")) {
					name = names[0].substring(0, 1) + name;
				} else if (type.equals("Yx")) {
					name = name + names[0].substring(0, 1);
				} else if (type.equals("ZY")) {
					name = names[names.length - 1] + name;
				} else if (type.equals("YZ")) {
					name = name + names[names.length - 1];
				} else if (type.equals("Yz")) {
					name = name + names[names.length - 1].substring(0, 1);
				} else if (type.equals("zY")) {
					name = names[names.length - 1].substring(0, 1) + name;
				} else if (type.equals("XYZ")) {
					name = names[0] + name + names[names.length - 1];
				} else if (type.equals("XZY")) {
					name = names[0] + names[names.length - 1] + name;
				} else if (type.equals("YXZ")) {
					name = name + names[0] + names[names.length - 1];
				} else if (type.equals("YZX")) {
					name = name + names[names.length - 1] + names[0];
				} else if (type.equals("ZXY")) {
					name = names[names.length - 1] + names[0] + name;
				} else if (type.equals("ZYX")) {
					name = names[names.length - 1] + name + names[0];
				} else if (type.equals("XYz")) {
					name = names[0] + name
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("YXz")) {
					name = name + names[0]
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("XzY")) {
					name = names[0] + names[names.length - 1].substring(0, 1)
							+ name;
				} else if (type.equals("YzX")) {
					name = name + names[names.length - 1].substring(0, 1)
							+ names[0];
				} else if (type.equals("zXY")) {
					name = names[names.length - 1].substring(0, 1) + names[0]
							+ name;
				} else if (type.equals("zYX")) {
					name = names[names.length - 1].substring(0, 1) + name
							+ names[0];
				} else if (type.equals("xYZ")) {
					name = names[0].substring(0, 1) + name
							+ names[names.length - 1];
				} else if (type.equals("xZY")) {
					name = names[0].substring(0, 1) + names[names.length - 1]
							+ name;
				} else if (type.equals("YxZ")) {
					name = name + names[0].substring(0, 1)
							+ names[names.length - 1];
				} else if (type.equals("ZxY")) {
					name = names[names.length - 1] + names[0].substring(0, 1)
							+ name;
				} else if (type.equals("YZx")) {
					name = name + names[names.length - 1]
							+ names[0].substring(0, 1);
				} else if (type.equals("ZYx")) {
					name = names[names.length - 1] + name
							+ names[0].substring(0, 1);
				} else if (type.equals("xYz")) {
					name = names[0].substring(0, 1) + name
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("zYx")) {
					name = names[names.length - 1].substring(0, 1) + name
							+ names[0].substring(0, 1);
				} else if (type.equals("Yxz")) {
					name = name + names[0].substring(0, 1)
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("Yzx")) {
					name = name + names[names.length - 1].substring(0, 1)
							+ names[0].substring(0, 1);
				} else if (type.equals("xzY")) {
					name = names[0].substring(0, 1)
							+ names[names.length - 1].substring(0, 1) + name;
				} else if (type.equals("zxY")) {
					name = names[names.length - 1].substring(0, 1)
							+ names[0].substring(0, 1) + name;
				}
			} else if (type.contains("y")) {
				for (int i = 1; i < names.length - 1; i++) {
					if (names[i].equals(""))
						continue;
					name = name + names[i].substring(0, 1);
				}
				if (type.equals("Xy")) {
					name = names[0] + name;
				} else if (type.equals("yX")) {
					name = name + names[0];
				} else if (type.equals("Zy")) {
					name = names[names.length - 1] + name;
				} else if (type.equals("yZ")) {
					name = name + names[names.length - 1];
				} else if (type.equals("xyz")) {
					name = names[0].substring(0, 1) + name
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("zyx")) {
					name = names[names.length - 1].substring(0, 1) + name
							+ names[0].substring(0, 1);
				} else if (type.equals("yxz")) {
					name = name + names[0].substring(0, 1)
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("yzx")) {
					name = name + names[names.length - 1].substring(0, 1)
							+ names[0].substring(0, 1);
				} else if (type.equals("xzy")) {
					name = names[0].substring(0, 1)
							+ names[names.length - 1].substring(0, 1) + name;
				} else if (type.equals("zxy")) {
					name = names[names.length - 1].substring(0, 1)
							+ names[0].substring(0, 1) + name;
				} else if (type.equals("Xyz")) {
					name = names[0] + name
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("Xzy")) {
					name = names[0] + names[names.length - 1].substring(0, 1)
							+ name;
				} else if (type.equals("yXz")) {
					name = name + names[0]
							+ names[names.length - 1].substring(0, 1);
				} else if (type.equals("zXy")) {
					name = names[names.length - 1].substring(0, 1) + names[0]
							+ name;
				} else if (type.equals("yzX")) {
					name = name + names[names.length - 1].substring(0, 1)
							+ names[0];
				} else if (type.equals("zyX")) {
					name = names[names.length - 1].substring(0, 1) + name
							+ names[0];
				} else if (type.equals("xyZ")) {
					name = names[0].substring(0, 1) + name
							+ names[names.length - 1];
				} else if (type.equals("yxZ")) {
					name = name + names[0].substring(0, 1)
							+ names[names.length - 1];
				} else if (type.equals("xZy")) {
					name = names[0].substring(0, 1) + names[names.length - 1]
							+ name;
				} else if (type.equals("yZx")) {
					name = name + names[names.length - 1]
							+ names[0].substring(0, 1);
				} else if (type.equals("Zxy")) {
					name = names[names.length - 1] + names[0].substring(0, 1)
							+ name;
				} else if (type.equals("Zyx")) {
					name = names[names.length - 1] + name
							+ names[0].substring(0, 1);
				} else if (type.equals("XyZ")) {
					name = names[0] + name + names[names.length - 1];
				} else if (type.equals("ZyX")) {
					name = names[names.length - 1] + name + names[0];
				} else if (type.equals("yXZ")) {
					name = name + names[0] + names[names.length - 1];
				} else if (type.equals("yZX")) {
					name = name + names[names.length - 1] + names[0];
				} else if (type.equals("XZy")) {
					name = names[0] + names[names.length - 1] + name;
				} else if (type.equals("ZXy")) {
					name = names[names.length - 1] + names[0] + name;
				}
			}
		}

		return name;
	}

	/**
	 * 找出两个字符串开头相同的部分（大于3个字符长度）
	 * 
	 * @param a
	 *            alexanderJ
	 * @param b
	 *            alexJ
	 * @return alex ; 没有返回null
	 */
	private static String getStartWithSubstring(String a, String b) {
		StringBuffer sb = new StringBuffer();
		char[] aa = a.toCharArray();
		char[] bb = b.toCharArray();
		int aLength = aa.length;
		int bLength = bb.length;
		int n = Math.min(aLength, bLength);
		if (n < 3) {
			return null;
		}

		for (int i = 0; i < n; i++) {
			if (aa[i] == bb[i]) {
				sb.append(aa[i]);
				continue;
			} else {
				if (i >= 3) { // 有3个字母或以上重复才认为是名字的简写
					return sb.toString();
				} else {
					return null;
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 找出两个字符串相同的部分（大于3个字符长度）
	 * 
	 * @param a
	 *            alexanderJ
	 * @param b
	 *            alexJ
	 * @return alex ; 没有返回null
	 */
	private static String getCommonSubstring(String a, String b) {
		StringBuffer sb = new StringBuffer();
		char[] aa = a.toCharArray();
		char[] bb = b.toCharArray();
		// List<Character> result = new ArrayList<Character>();
		int aLength = aa.length;
		int bLength = bb.length;
		int i, j;
		for (i = 0; i < aLength; i++) {
			for (j = 0; j < bLength; j++) {
				int n = 0;
				while ((i + n) < aLength && (j + n) < bLength
						&& aa[i + n] == bb[j + n]) {
					sb.append(aa[i + n]);
					n++;
				}
				if (sb.length() >= 3) { // 有3个字母以上重复才认为是名字的简写
					return sb.toString();
				} else {
					sb = new StringBuffer();
				}
			}
		}
		return null;
	}
}

class StrLenComparator implements Comparator<String> {
	public int compare(String s1, String s2) {

		int num = new Integer(s1.length()).compareTo(new Integer(s2.length()));

		/*
		 * if(s1.length() > s2.length()) return 1; if(s1.length() ==
		 * s2.length()) return 0; return -1;
		 */

		if (num == 0) {
			return s1.compareTo(s2);
		}

		return num;
	}
}