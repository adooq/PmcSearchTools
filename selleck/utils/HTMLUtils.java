package selleck.utils;

import java.util.HashSet;
import java.util.Set;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

public class HTMLUtils {
	/**
	 * 抓取当前页面所有link链接
	 * 
	 * @param
	 */
	public static Set<String> catchUrlList(String html) {
		Set<String> urlList = new HashSet<String>();
		Parser parser = null;
		NodeList nodeList = null;
		try {
			parser = new Parser(html);
			OrFilter linkFilter = new OrFilter(new NodeClassFilter(LinkTag.class), new NodeClassFilter(FrameTag.class));
			nodeList = parser.extractAllNodesThatMatch(linkFilter);
			for (int i = 0; i < nodeList.size(); i++) {
				Node tag = nodeList.elementAt(i);
				if (tag instanceof LinkTag) {// <a> 标签
					String link = ((LinkTag) tag).getLink();
					if (!link.startsWith("tel:") && !link.startsWith("ftp:") && !isfile(link)) {
						urlList.add(link.trim().replaceAll(" ", "%20"));
					}
				} else { // <frame> 标签
					String frameSrc = ((FrameTag) tag).getAttribute("src");
					if (frameSrc != null && !frameSrc.isEmpty()) {
						urlList.add(frameSrc.trim().replaceAll(" ", "%20"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		urlList.remove("/");
		return urlList;
	}

	private static boolean isfile(String link) {
		if (link.length() > 5) {
			link = link.substring(link.length() - 5, link.length());
			String[] suffix = { ".pdf", ".doc", ".ppt", ".xls", ".swf", ".zip", ".rar", ".mov", ".avi", ".jpg", ".jpeg",
					".gif" };
			for (String str : suffix) {
				if (link.contains(str)) {
					return true;
				}
			}
		}

		return false;
	}

}
