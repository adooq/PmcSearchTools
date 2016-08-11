package selleck.email.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import selleck.email.update.tools.HMARobot;
import selleck.utils.HTTPUtils;

public class FindOrganizationWebsites {

	/**
	 * 寻找机构网址
	 * 
	 * @param args
	 * @author lyh
	 * 2015.12
	 */
	public static void main(String[] args) {
		// excel文件,内容为一列机构名，一列类别（true直接搜，false加 medical OR bio）
		final String ExcelFile = "data source/organizations.xlsx";
		List<Organization> organizations = readExcel(ExcelFile);

		int count = 1;
		for (Organization org : organizations) {
			String organName = org.organName;
			String country = org.country;
			boolean isUniversity = org.isUniversity;
			boolean isSpecialist = org.isSpecialist;
			String keyword = makeKeyWord(organName, isSpecialist, isUniversity, country);
			System.out.println(keyword);
			Set<String> resultSet = search(keyword);
			saveInExcel(organName, resultSet);
			System.out.println("搜完第" + (count++) + "个机构");
			try {
				Thread.sleep(20000 + new Random().nextInt(10000));// 隔二三十秒再抓下一次
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将搜索结果保存至Excel
	 * 
	 * @param name
	 *            机构名
	 * @param resultSet
	 *            可能的网址列表
	 */
	private static void saveInExcel(String name, Set<String> resultSet) {
		XSSFWorkbook wb = null;
		FileOutputStream out = null;
		try {

			wb = new XSSFWorkbook(new FileInputStream("data source/organWebsite.xlsx"));
			XSSFSheet sheet = wb.getSheetAt(0);
			XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
			XSSFCell cell = null;

			cell = row.createCell(0);
			cell.setCellValue(name);
			if (resultSet != null) {
				String[] arr = new String[resultSet.size()];
				arr = resultSet.toArray(arr);
				for (int i = 1; i <= arr.length; i++) {
					cell = row.createCell(i);
					cell.setCellValue(arr[i - 1]);
				}
				out = new FileOutputStream("data source/organWebsite.xlsx");
				wb.write(out);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (wb != null) {
				try {
					wb.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		for (String str : resultSet)
			System.out.println(name + "   " + str);

	}

	/**
	 * 使用Bing进行搜索
	 * 
	 * @param keyword
	 * @return
	 */
	private static LinkedHashSet<String> search(String keyword) {
		LinkedHashSet<String> resultSet = new LinkedHashSet<String>();

		String url = "http://global.bing.com/search?q=" + keyword;
		String defaultCookie = "MUID=3EEF06D0A82D66E32E290FC0A98C6750; SRCHD=AF=NOFORM; SRCHUSR=DOB=20160509; _SS=SID=1682E4E6E3BE671B04DCEDF6E21F66AA&HV=1462780531; SRCHHPGUSR=CW=1428&CH=172&DPR=1; WLS=C=&N=; SRCHUID=V=2&GUID=B77BD4B792974900A9D8FE2EABC2AD4A";

		while (true) {
			Map<String, String> queryPageMap = HTTPUtils.getCookieUrlAndHtml(url, defaultCookie, null, HTTPUtils.GET,
					null);
			String html = queryPageMap.get("html");
			if (html != null && html.contains("No results found for")) {
				System.out.println("没结果");
				break;
			} else if (html != null && html.contains("results")) {
				System.out.println("搜到了");
				parsehtml(resultSet, html);
				break;
			} else {
				System.out.println("要换IP啦");
				new HMARobot().changeIP();
			}
		}
		return resultSet;
	}

	/**
	 * 解析得到的搜索结果页
	 * 
	 * @param resultSet
	 * @param html
	 */
	private static void parsehtml(LinkedHashSet<String> resultSet, String html) {
		Pattern p = Pattern.compile("<h2><a href=\"(http(s)?.*?)\"");
		Matcher matcher = p.matcher(html);
		boolean haveFound = matcher.find();
		while (haveFound && resultSet.size() < 5) {
			String website = matcher.group(1);
			website = screenWebsite(website);
			if (!website.equals("")) {
				resultSet.add(website);
			}
			haveFound = matcher.find();
		}
	}

	/**
	 * 筛选搜索结果
	 * 
	 * @param website
	 * @return 被过滤的为"",未被过滤的为host或host/dir/，dir为关键词目录
	 */
	private static String screenWebsite(String website) {
		if (containWords(website)) {
			return "";
		}
		// 跳过schema后的双斜杠，取8个字符后出现的第一个"/",即分割host的位置。若host之后为关键词后缀则保留，否则只取host。
		int seperatorPosition = website.indexOf("/", 8);
		if (seperatorPosition > 0) {
			String[] suffix = { "med", "medicine", "bio", "biology", "pharma", "cell", "life", "gene", "health",
					"neuro", "cancer", "cardi", "gnome", "oncology" };
			for (String str : suffix) {
				if (website.startsWith(str + "/", seperatorPosition + 1)) {
					website = website.substring(0, seperatorPosition + 1) + str + "/";
					return website;
				}
			}
			website = website.substring(0, seperatorPosition + 1);
		}
		return website;
	}

	/**
	 * 过滤各种常见网页
	 * 
	 * @param webstie
	 * @return
	 */
	private static boolean containWords(String str) {

		String[] arr = { "nihaowang", "wikipedia", "sogou", "msn.com", "linkedin", "ranking", "studyin", "facebook",
				"yahoo", "google", "youtube", "baidu", ".bing", "at0086", "microsoft", "mbbs", "chinauniversity",
				"cucas" };
		for (String s : arr) {
			if (str.contains(s))
				return true;
		}
		return false;
	}

	/**
	 * 根据国家、是否大学、是否专门机构生成不同关键词
	 * 
	 * @param name
	 * @param isSpecialist
	 * @param country
	 * @param isUniversity
	 * @return
	 */
	private static String makeKeyWord(String name, Boolean isSpecialist, boolean isUniversity, String country) {
		String keyWord = "";
		try {
			if (!isSpecialist && isUniversity) {
				switch (country) {
				case "China":
				case "TaiWan":
					keyWord = java.net.URLEncoder.encode(name + " 医学院", "utf-8");
					break;
				case "Japan":
					keyWord = java.net.URLEncoder.encode(name + " 医学部", "utf-8");
					break;
				case "Netherlands":
				case "Belgium":
					keyWord = java.net.URLEncoder.encode(name + " School van de geneeskunde", "utf-8");
					break;
				case "Germany":
					keyWord = java.net.URLEncoder.encode(name + " Schule der Medizin", "utf-8");
					break;
				case "Italy":
					keyWord = java.net.URLEncoder.encode(name + " Scuola di medicina", "utf-8");
					break;
				default:// 默认英语
					keyWord = java.net.URLEncoder.encode(name + " Medical school", "utf-8");
					break;
				}
			} else {
				keyWord = java.net.URLEncoder.encode(name, "utf-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return keyWord;
	}

	/**
	 * 读取Excel文件
	 * 
	 * @param ExcelFile
	 *            Excel 文件名
	 * @return
	 */
	private static List<Organization> readExcel(String ExcelFile) {
		List<Organization> excelList = new LinkedList<Organization>();
		Workbook wb = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream(ExcelFile));
			Sheet sheet = wb.getSheetAt(0);
			int rowCount = sheet.getLastRowNum();
			for (int i = 0; i <= rowCount; i++) {
				Row row = sheet.getRow(i);
				Organization org = new Organization();
				org.organName = row.getCell(2).getStringCellValue().trim();
				org.country = row.getCell(3).getStringCellValue().trim();
				org.isUniversity = row.getCell(4).getBooleanCellValue();
				org.isSpecialist = row.getCell(5).getBooleanCellValue();
				excelList.add(org);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return excelList;
	}

}

class Organization {
	String organName;
	String country;
	boolean isUniversity;
	boolean isSpecialist;
	Set<String> websites;
}
