package selleck.email.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.protocol.HTTP;
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
import selleck.utils.HTTPUtils;
import selleck.utils.StringUtils;

/**
 * @author lyh 2016.01
 * 
 */
public class FindEmailsFromLab2 {
	private final ILabEmailService labEmailService = new LabEmailServiceImpl(Constants.LIFE_SCIENCE_DB);
	private final int TIER = 6; // 网页抓取深度层级
	private final int PAGENUM = 50000; // 一个站点抓取最大页面数。
	private final String tierMark = "TierMark";// 队列中划分层次的标记。
	private final static String LAB_EXCEL = "newHost.xlsx"; // 网站网址excel文件名
	private final static SynchronousQueue<String> websites = new SynchronousQueue<String>(true);
	private static FileWriter newHostfw = null;// 保存新发现的host
	private static FileWriter numfw = null;// 保存抓取数量
	private static boolean flag = true;// 线程结束标志，excel表读取完毕后设为false

	/**
	 * 访问大学网站，爬取若干层，抓取email。 存入emailhunter.selleck_edm_lab_email
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		final FindEmailsFromLab2 fefl = new FindEmailsFromLab2();
		try {
			newHostfw = new FileWriter(
					new File("Z:\\EDM\\wos tools\\temp\\newHost\\" + System.currentTimeMillis() + ".txt"), true);
			numfw = new FileWriter(new File("Z:\\EDM\\wos tools\\temp\\num\\" + System.currentTimeMillis() + ".txt"),
					true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					fefl.loadWebsites();
				} catch (IOException e) {
					try {// 再来一次。
						Thread.sleep(5000);
						fefl.loadWebsites();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		}).start();

		for (int i = 0; i < 30; i++) {
			new Thread(new Runnable() {
				public void run() {

					while (flag) {
						try {
							fefl.crawl(websites.take());
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;
						}
					}
				}
			}).start();
		}

	}

	private void crawl(String host) {
		HashSet<String> allUrlSet = new HashSet<String>();// domain下遇到的所有url的集合，包括已访问和待访问。用MD5保存节省内存
		HashSet<String> hostSet = new HashSet<String>();// 找到的带关键字的新host
		Queue<String> urlsToVisit = new LinkedList<String>(); // 待访问的url队列，层次遍历，每层结尾要给下一层添加标记。
		allUrlSet.add(StringUtils.encodeByMD5(host));
		urlsToVisit.add(host);
		urlsToVisit.add(tierMark);
		int tier = 1;
		String html = "";
		Set<String> urls;
		while (tier <= TIER) {
			String currentUrl = urlsToVisit.poll();
			if (currentUrl.equals(tierMark)) {
				int visitedNum = allUrlSet.size() - urlsToVisit.size();
				if (tier < TIER && visitedNum < PAGENUM) {
					tier++;
					urlsToVisit.add(tierMark);
					continue;
				} else {// 爬完了，收尾。
					saveNewHost(hostSet);
					System.out.println("网站共有" + visitedNum + "个页面         " + host);
					saveVisitedNum(host, visitedNum);
					allUrlSet.clear();
					hostSet.clear();
					urlsToVisit.clear();
					return;
				}
			}
			html = HTTPUtils.getCookieUrlAndHtml(currentUrl, null, null, "GET", null).get("html");
			System.out.println(new SimpleDateFormat("HH:mm:ss  yyyy-MM-dd").format(new Date()) + "     " + currentUrl);
			if (StringUtils.isNullOrEmpty(html)) {
				System.out.println("访问失败            " + currentUrl);
				continue;
			}
			pickEmail(html, currentUrl, host, tier);

			urls = HTMLUtils.catchUrlList(html);
			for (String url : urls) {
				url = pretreatUrl(host, url);
				if (isOutside(host, url)) {
					findNewHost(url, host, hostSet);
				} else {// 站内
					url = assembleUrl(host, currentUrl, url);
					String urlMD5 = StringUtils.encodeByMD5(url);
					if (!url.isEmpty() && !allUrlSet.contains(urlMD5)) {
						allUrlSet.add(urlMD5);
						urlsToVisit.add(url);
					}
				}
			}
		}
	}

	private void saveVisitedNum(String host, int visitedNum) {
		try {
			numfw.write(host + "    " + visitedNum + "\r\n");
			numfw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveNewHost(HashSet<String> hostSet) {
		synchronized (newHostfw) {
			try {
				for (String newHost : hostSet) {
					newHostfw.write(newHost + "\r\n");
				}
				newHostfw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String pretreatUrl(String host, String url) {
		if (url.startsWith("//")) {
			url = host.substring(0, host.indexOf(':') + 1) + url;
		}
		if (url.contains("#")) {
			url = url.substring(0, url.indexOf('#'));
		}
		return url;
	}

	private boolean isOutside(String host, String url) {
		return url.startsWith("http") && !url.startsWith(host);
	}

	private String assembleUrl(String host, String currentUrl, String url) {
		if (url.startsWith("http")) {
			return url;
		} else if (url.startsWith("/")) {
			url = host + url.replaceFirst("/", "");
		} else if (url.startsWith("?")) {
			url = host + url;
		} else {
			url = currentUrl.substring(0, currentUrl.lastIndexOf("/") + 1) + url;
			// 去掉相对路径，类似http://cancer.umn.edu/./about/25th-anniversary/../../news/../patient-information/index.htm
			url = url.replaceAll("/\\./", "/").replaceAll("/[^/]+/\\.\\.", "").replaceAll("/[^/]+/\\.\\.", "")
					.replaceAll("/[^/]+/\\.\\.", "");
		}
		return url;
	}

	/**
	 * @param url
	 * @param domain
	 *            寻找带关键字的新的二级域名
	 */
	private void findNewHost(String url, String host, Set<String> hostSet) {
		String newHost = url.substring(0, url.indexOf("/", 8) + 1);
		if (hostSet.contains(newHost)) {
			return;
		}
		Pattern pattern = Pattern
				.compile("https?://(www\\d?\\.)?(\\w+\\.)?(\\w+\\.)?(?<keydomain>\\w+)\\.(?<topdomain>edu|org|ac)");
		Matcher m1 = pattern.matcher(host);
		Matcher m2 = pattern.matcher(newHost);
		if (m2.find() && m1.find()) {
			String domain1 = m1.group("keydomain") + m1.group("topdomain");
			String domain2 = m2.group("keydomain") + m2.group("topdomain");
			if (domain1.equals(domain2) && m2.group(2) != null) {
				String matchString = m2.group(2) + m2.group(3);
				if (containWords(matchString)) {
					hostSet.add(newHost);
				}
			}
		}
	}

	private boolean containWords(String matchString) {
		String[] arr = { "med", "bio", "ology", "pharma", "cell", "lifesci", "gene", "health", "neuro", "blood",
				"cancer", "hospi", "clinic", "cardi", "genome", "hsc", "som", "protein", "proteo", "mcb" };
		// hsc:Health Science Center,som:School of Medicine
		for (String s : arr) {
			if (matchString.contains(s))
				return true;
		}
		return false;
	}

	private void pickEmail(String html, String url, String domain, int tier) {
		Pattern p = Pattern.compile(StringUtils.EMAIL_REGEX);
		Matcher matcher = p.matcher(html);
		while (matcher.find()) {
			String email = matcher.group();
			LabEmail labEmail = new LabEmail();
			labEmail.setEmail(email);
			labEmail.setTier(tier);
			labEmail.setRealUrl(url);
			labEmail.setWebsite(domain);
			labEmailService.saveLabEmail(labEmail);
		}
	}

	/**
	 * 从excel里加载要抓取的网站,放入SynchronousQueue中，等待其它线程读取。
	 * 
	 * @throws IOException
	 */
	private void loadWebsites() throws IOException {
		Workbook wb = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream(LAB_EXCEL));

			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			for (int i = 0; i <= rowCount; i++) {
				try {
					Row row = sheet.getRow(i);
					if (row.getCell(1) == null || row.getCell(1).getNumericCellValue() != 1) {
						String labUrl = row.getCell(0).getStringCellValue().trim();
						websites.put(labUrl);
						System.out.println("开始抓取第" + i + "个机构             " + labUrl);
						row.createCell(1, Cell.CELL_TYPE_NUMERIC).setCellValue(1);// 已读的行标记1；
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				FileOutputStream out = null;
				try {
					out = new FileOutputStream(LAB_EXCEL);
					wb.write(out);
				} catch (IOException e) {
					System.out.println("Excel保存出错");
					e.printStackTrace();
					continue;
				} finally {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			flag = false;
		} catch (IOException e1) {
			throw e1;
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

}