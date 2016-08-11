package selleck.utils;

import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	// 匹配邮箱地址的正则。邮箱地址的前缀和后缀的确会包含下划线、中划线、点，但认为不会以这些开头。
	// 邮箱前缀2-30个字符。后缀不超过5个单词，最后一个单词为2-3个字母。
	// \w : [a-zA-Z_0-9]
	public static final String EMAIL_REGEX = "[a-zA-Z0-9][\\w\\-\\.]{1,30}@[a-zA-Z0-9][\\w\\-]*(\\.[\\w\\-]+){0,3}(\\.[a-zA-Z]{2,3})";
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
			'e', 'f' };

	/**
	 * 将Java字串转换成SQL字串。
	 * 
	 * @param source可以为null
	 * @return 若输入为null，则返回""
	 */
	public static String toSqlForm(String source) {
		if (source == null) {
			return source;
		}

		return source.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
	}

	/**
	 * 判断Java字串是否为null或""。
	 * 
	 * @param source可以为null
	 * @return 若输入为null，则返回true
	 */
	public static boolean isNullOrEmpty(String source) {
		return source == null || source.isEmpty();
	}

	/**
	 * 将Java字串null转换""字串。
	 * 
	 * @param source可以为null
	 * @return 若输入为null，则返回""
	 */
	public static String nullToEmpty(String source) {
		return source == null ? "" : source;
	}

	/**
	 * 替换系统配置字串。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String replaceSysConfigStr(String source) {
		StringBuffer result = new StringBuffer();
		int iStart = -1;
		String key = "";

		for (int i = 0; i < source.length(); i++)
			if (iStart >= 0) {
				if (source.charAt(i) == '}') {
					key = source.substring(iStart, i);
					result.append(System.getProperties().getProperty(key, "${" + key + "}"));
					iStart = -1;
				}
			} else if (source.charAt(i) == '$' && source.charAt(i + 1) == '{') {
				iStart = i + 2;
				i++;
			} else {
				result.append(source.charAt(i));
			}

		return result.toString();
	}

	/**
	 * 字符串转成网页字符串。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String toWebForm(String source) {
		String result = "";

		for (int i = 0; i < source.length(); i++) {
			switch (source.charAt(i)) {
			case 13: // '\r'
				result = result + "<br>";
				break;
			case 60: // '<'
				result = result + "&lt;";
				break;
			case 62: // '>'
				result = result + "&gt;";
				break;
			case 32: // ' '
				result = result + "&nbsp;";
				break;
			default:
				result = result + source.charAt(i);
				break;
			case 10: // '\n'
				break;
			}
		}

		return result;
	}

	/**
	 * 字符串转成手机网页字符串。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String toWAP1Form(String source) {
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < source.length(); i++) {
			switch (source.charAt(i)) {
			case 13: // '\r'
				result.append("<br/>");
				break;
			case 60: // '<'
				result.append("&lt;");
				break;
			case 62: // '>'
				result.append("&gt;");
				break;
			case 38: // '&'
				result.append("&amp;");
				break;
			case 32: // ' '
				result.append("&nbsp;");
				break;
			default:
				result.append(source.charAt(i));
				break;
			case 10: // '\n'
				break;
			}
		}

		return result.toString();
	}

	/**
	 * 定长字串补全。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String fullNullStr(String source, int length) {
		String result = source;
		byte arrValue[] = source.getBytes();

		for (int i = arrValue.length; i < length; i++)
			result = result + '\0';

		return result;
	}

	/**
	 * 定长Byte数组补全。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static byte[] fullNullStrToBytes(String source, int length) {
		byte result[] = new byte[length];
		byte arrValue[] = source.getBytes();

		if (arrValue.length <= result.length)
			System.arraycopy(arrValue, 0, result, 0, arrValue.length);
		for (int i = arrValue.length; i < length; i++)
			result[i] = 0;

		return result;
	}

	/**
	 * 全角字符转成半角字符。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String caseSBCtoDBC(String source) {
		String result = "";
		String sSBC = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ｀１２３４５６７８９０－＝＼～！＃＄％＾＆（）＿＋｜［］｛｝；＇：＂，。／＜＞？";
		String sDBC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`1234567890-=\\~!#$%^&()_+|[]{};':\",./<>?";

		for (int i = 0; i < source.length(); i++) {
			int iIndex = sSBC.indexOf(source.substring(i, i + 1));
			if (iIndex > -1) {
				result = result + sDBC.charAt(iIndex);
			} else {
				result = result + source.charAt(i);
			}
		}

		return result;
	}

	/**
	 * 字符串是否是数据
	 * 
	 * @param source字符串
	 * @return 返回是否
	 */
	public static boolean isInteger(String source) {
		boolean result = true;

		for (int i = 0; i < source.length(); i++) {
			if ((source.charAt(i) >= '0' && source.charAt(i) <= '9') || source.charAt(i) == '-')
				continue;
			result = false;
			break;
		}

		return result;
	}

	/**
	 * 从字串右边开始字符的提取。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String rightWriteStr(String source, int len) {
		StringBuffer result = new StringBuffer();
		int iLeft = source.length() - len;

		for (int i = source.length() - 1; i >= iLeft; i--)
			result.insert(0, source.charAt(i));

		return result.toString();
	}

	/**
	 * 将路径字串转成JAVA字串。
	 * 
	 * @param path不可以为null
	 * @return 非null字串
	 */
	public static String deatlePath(String path) {
		String lastStr = path.substring(path.length() - 1);

		if (!lastStr.equals("/") && !lastStr.equals("\\"))
			path += "/";
		path = path.replace("\\", "/");

		return path;
	}

	/**
	 * 字串与字串全字符匹配。
	 * 
	 * @param source不可以为null
	 * @return 是否
	 */
	public static Boolean strMatcherStr(String source, String str) {
		Pattern pat = Pattern.compile(str);
		Matcher matcher = pat.matcher(source);

		return matcher.matches();
	}

	/**
	 * 字串中是否包含字串。
	 * 
	 * @param source不可以为null
	 * @return 是否
	 */
	public static Boolean strFindStr(String source, String str) {
		Pattern pat = Pattern.compile(str);
		Matcher matcher = pat.matcher(source);

		return matcher.find();
	}

	/**
	 * 在字串中基标签数组取字串。
	 * 
	 * @param source不可以为null
	 * @return 非null字串
	 */
	public static String strGetStr(String source, String[] mark) {
		int index = -1;

		if (mark.length > 2) {
			index = source.indexOf(mark[3]) + mark[3].length();
			if (index < mark[3].length())
				return "";
			source = source.substring(index);
			index = source.indexOf(mark[2]) + mark[2].length();
			if (index < mark[2].length())
				return "";
			source = source.substring(index);
		}
		index = source.indexOf(mark[1]) + mark[1].length();
		if (index < mark[1].length())
			return "";
		source = source.substring(index);
		index = source.indexOf(mark[0]);
		if (index < 0)
			return "";

		return source.substring(0, index);
	}

	/**
	 * 计算字串中另个字串总数。
	 * 
	 * @param source不可以为null
	 * @return 整数
	 */
	public static int strCountStr(String source, String find) {
		int count = 0;
		int index = -1;
		while (true) {
			index = source.indexOf(find) + find.length();
			if (index < find.length())
				break;
			source = source.substring(index);
			count++;
		}

		return count;
	}

	public static String encodeByMD5(String str) {
		if (str == null) {
			return null;
		}
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(str.getBytes());
			return getFormattedText(messageDigest.digest());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static String getFormattedText(byte[] bytes) {
		int len = bytes.length;
		StringBuilder buf = new StringBuilder(len * 2);
		// 把密文转换成十六进制的字符串形式
		for (int j = 0; j < len; j++) {
			buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
			buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
		}
		return buf.toString();
	}

}
