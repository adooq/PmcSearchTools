package selleck.email.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import selleck.email.pojo.LabEmail;
import selleck.email.service.ILabEmailService;
import selleck.email.service.impl.LabEmailServiceImpl;
import selleck.utils.Constants;
import selleck.utils.HTMLUtils;
import selleck.utils.StringUtils;

@Deprecated
public class FindEmailsFromLab {
	private static final ILabEmailService labEmailService = new LabEmailServiceImpl(Constants.LIFE_SCIENCE_DB);
	private static final int TIER = 6; // 网页抓取深度层级
	private static final String LAB_EXCEL = "lab list.xlsx"; // 网站网址excel文件名
	private static int totalPage; // 一个网站中的总页数
	private static String currentWebsite;

	/**
	 * 访问大学网站，爬取若干层，抓取email。 存入emailhunter.selleck_edm_lab_email
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		while (true) {
			List<String> websites = new ArrayList<String>();

			// 从代码里设置访问网站
			// websites.add("http://med.ufl.edu/");

			loadWebsites(websites); // 从excel下载
			if (websites.size() == 0) { // 没有要搜索的website
				break;
			}

			for (String website : websites) {
				totalPage = 0;
				currentWebsite = website.replaceFirst("http[s]?://", "");
				int index = currentWebsite.indexOf("/");
				if (index != -1) {
					currentWebsite = currentWebsite.substring(0, index);
				}
				System.out.println("lab website: " + currentWebsite);
				try {
					Set<String> allLinkSet = new HashSet<String>(); // 用来保存已经访问过的link
					getEmail(website, website, 1, allLinkSet);
					System.out.println(website + " total pages: " + totalPage);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}

	}

	/**
	 * @param url
	 * @param website
	 *            网站首页
	 * @param tier
	 *            抓取深度层级，控制迭代的次数
	 * @param allLinkSet
	 *            网站下的所有链接
	 * @return
	 * 
	 */
	public static void getEmail(String url, String website, int tier, Set<String> allLinkSet) {
		Map<String, String> htmlMap = selleck.utils.HTTPUtils.getCookieUrlAndHtml(url, null, null, HTTPUtils.GET, null);
		totalPage++;
		String html = htmlMap.get("html");
		String realUrl = htmlMap.get("url");
		if (html == null || html.isEmpty()) {
			return;
		}
		allLinkSet.add(url);

		html = StringEscapeUtils.unescapeHtml4(html); // 去转义字符， &gt; 转换成>符号

		// 查找email
		Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
		Matcher matcher = p.matcher(html);
		while (matcher.find()) {
			String email = matcher.group();
			LabEmail labEmail = new LabEmail();
			labEmail.setEmail(email);
			labEmail.setTier(tier);
			labEmail.setRealUrl(url);
			labEmail.setWebsite(website);
			labEmailService.saveLabEmail(labEmail);

			// System.out.println("find email: "+email);
		}

		if (tier >= TIER) {
			return;
		}

		tier++;

		// 查找link
		Set<String> links = HTMLUtils.catchUrlList(html);
		links.removeAll(allLinkSet);
		allLinkSet.addAll(links);

		// String domain = getDomainFromUrl(realUrl);
		URI uri = null;
		try {
			uri = new URI(realUrl);
			// String domain = uri.getHost();
			// domain = domain.substring(domain.indexOf(".") + 1); //
			// 去掉二级域名，允许在主域名内跳转
			String host = uri.getHost();
			String path = uri.getPath();
			String scheme = uri.getScheme();
			for (String link : links) {
				try {
					if (link.startsWith("http://") || link.startsWith("https://")) {
						String linkDomain = link.replaceFirst("http[s]?://", "");
						int index = linkDomain.indexOf("/");
						if (index != -1) {
							linkDomain = linkDomain.substring(0, index);
						}
						if (linkDomain.equalsIgnoreCase(currentWebsite) && !link.equals(website)) {
							getEmail(link, website, tier, allLinkSet);
							tier--;
						}
					} else {
						if (!host.equalsIgnoreCase(currentWebsite)) {
							continue;
						}
						if (!link.equals(website)) {
							if (link.startsWith("/")) { // 绝对路径
								getEmail(scheme + "://" + host + link, website, tier, allLinkSet);
								tier--;
							} else { // 相对路径
								String relativePath = path.substring(0,
										path.lastIndexOf("/") == -1 ? 0 : path.lastIndexOf("/"));
								getEmail(scheme + "://" + host + relativePath + "/" + link, website, tier, allLinkSet);
								tier--;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 从excel里加载要抓取的网站
	 * 
	 * @param websites
	 */
	private static void loadWebsites(List<String> websites) {
		Workbook wb = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream(LAB_EXCEL));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			for (int i = 0; i <= rowCount; i++) { // row从0开始
				try {
					Row row = sheet.getRow(i);
					if (row.getCell(1) == null) {
						String labUrl = row.getCell(0).getStringCellValue().trim();
						websites.add(labUrl);
						Cell cell = row.createCell(1, Cell.CELL_TYPE_NUMERIC);
						cell.setCellValue(1);
						break;
					}

				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(LAB_EXCEL);
				wb.write(out);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 检查url是否应该去访问，比如去掉文件下载的url 太难做了。。。
	 * 
	 * @param url
	 * @return
	 */
	private boolean checkUrl(String url) {
		String ending = url.substring(url.lastIndexOf(".")); // url结尾
		List<String> endings = Arrays.asList("html", "htm", "asp", "jsp");
		return false;
	}

}
