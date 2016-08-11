package selleck.email.update.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import selleck.utils.StringUtils;

/**
 * 解析文章用的工具类
 * @author fscai
 *
 */
public class ParserUtils {
	
	/**
	 * 查找content中的，prefix和suffix之间的内容。
	 * @param prefix
	 * @param suffix
	 * @param content
	 * @return 如果没有找到，返回""
	 */
	public static List<String> findWithPrefixAndSuffix(String prefix , String suffix , String content){
		List<String> rs = new ArrayList<String>();
		String regex = prefix + "[\\s\\S]+?" + suffix;
		Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(content);
		while (matcher.find()) {
			rs.add(matcher.group().replaceFirst(prefix, "").replaceAll(suffix+"\\z", ""));
		}
		return rs;
	}
	
	/**
	 * 查找content中的，prefix和suffix之间的内容，并且内容符合target表达式。
	 * @param target 
	 * @param prefix
	 * @param suffix
	 * @param content
	 * @return 如果没有找到，返回""
	 */
	public static List<String> findWithPrefixAndSuffix(String target , String prefix , String suffix , String content){
		List<String> rs = new ArrayList<String>();
		String regex = prefix + target + suffix;
		Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(content);
		while (matcher.find()) {
			rs.add(matcher.group().replaceFirst(prefix, "").replaceAll(suffix+"\\z", ""));
		}
		return rs;
	}
	
	/**
	 * 在content中搜索符合regex的内容
	 * @param regex
	 * @param content
	 * @return
	 */
	public static List<String> findInContent(String regex , String content){
		List<String> rs = new ArrayList<String>();
		Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(content);
		while (matcher.find()) {
			rs.add(matcher.group());
		}
		return rs;
	}
	
	/**
	 * 从一段文字中筛选出一个符合邮箱地址格式的部分，注意，只选出一个
	 * @param source
	 * @return 筛选出的邮箱地址，没有找到返回""
	 */
	public static String cleanEmail(String source){
		Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
		Matcher matcher = p.matcher(source);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}
	
	public static String trim(String s){
		return s.trim().replaceAll("\\A[^\\p{ASCII}^\\p{IsLatin}^\\p{InGreek}]+", "").replaceAll("[^\\p{ASCII}^\\p{IsLatin}^\\p{InGreek}]+\\z", "").trim();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String s = "'ss.fff@fsss.com++";
		System.out.println(cleanEmail(s));

	}

}
