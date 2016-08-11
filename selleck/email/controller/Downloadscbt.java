package selleck.email.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author lyh
 * 2016.5
 * 下载scbt产品数据
 * http://www.scbt.com/chapter_chemicals.html
 */
public class Downloadscbt {
	public static void main(String[] args) throws IOException, InterruptedException {

		Map<String, String> urls = new LinkedHashMap<>();

	//	urls = parsePage();
		 urls = readExcel();
		fetchByUrls(urls);

	}

	private static Map<String, String> parsePage() {
		Map<String, String> urls = new LinkedHashMap<>();
		try {
			Elements elements = Jsoup.connect("http://www.scbt.com/chapter_chemicals.html").get().select("#accordion")
					.get(3).select(".fontMedium");
			for (Element e : elements) {
				String name = e.text();
				String url = e.absUrl("href");
				urls.put(url, name);
				System.out.println(url + "    " + name);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return urls;
	}

	private static void fetchByUrls(Map<String, String> urls) {
		for (Entry<String, String> entry : urls.entrySet()) {
			try {
				System.out.println(entry.getKey() + "     " + entry.getValue());
				Elements productList = Jsoup.connect(entry.getKey()).timeout(300000).get()
						.select("table.productTable>tbody>tr");
				Thread.sleep(3000);
				productList.remove(0);
				List<Product> products = new LinkedList<>();
				for (Element ele : productList) {
					Product p = new Product();
					try {
						p.type = entry.getValue();
						p.name = ele.select("td>a").first().text();
						p.url = ele.select("td>a").first().absUrl("href");
						p.application = ele.select("td>div").get(1).text();
						p.cas = ele.select("td").get(1).text();
						p.catelog = ele.select("td").get(2).text();
						p.quantity = ele.select("td").get(3).text();
						p.price = ele.select("td").get(4).text();
						p.citation = ele.select("td").get(5).text();
						p.rating = ele.select("td").get(6).select("img[src$=star.gif]").size();
						p.crawl(p.url);

					} catch (Exception e) {
						e.printStackTrace();
						continue;
					} finally {
						System.out.println(p);
						products.add(p);
					}
				}
				System.out.printf("解析到%s个\r\n", productList.size());
				save(products);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}
		}
	}

	private static Map<String, String> readExcel() {
		Map<String, String> urls = new LinkedHashMap<>();
		Workbook wb = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream("urls.xlsx"));
			Sheet sheet = wb.getSheetAt(0);
			for (int i = 0; i < sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				urls.put(row.getCell(2).getStringCellValue(), row.getCell(0).getStringCellValue());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return urls;
	}

	private static void save(List<Product> products) {
		XSSFWorkbook wb = null;
		FileOutputStream out = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream("scbt.xlsx"));
			out = new FileOutputStream("scbt.xlsx");
			XSSFSheet sheet = wb.getSheetAt(0);
			for (Product p : products) {
				XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
				row.createCell(0).setCellValue(p.name);
				row.createCell(1).setCellValue(p.cas);
				row.createCell(2).setCellValue(p.catelog);
				row.createCell(3).setCellValue(p.citation);
				row.createCell(4).setCellValue(p.quantity);
				row.createCell(5).setCellValue(p.price);
				row.createCell(6).setCellValue(p.rating);
				row.createCell(7).setCellValue(p.application);
				row.createCell(8).setCellValue(p.type);
				row.createCell(9).setCellValue(p.synonym);
				row.createCell(10).setCellValue(p.description);
				row.createCell(11).setCellValue(p.molecularWeight);
				row.createCell(12).setCellValue(p.molecularFormula);
				row.createCell(13).setCellValue(p.url);
			}
			wb.write(out);
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
	}

	static class Product {
		String type;
		String name;
		String cas;
		String catelog;
		String quantity;
		String price;
		String citation;
		String application;
		String url;
		String synonym;
		String description;
		String molecularWeight;
		String molecularFormula;
		int rating;

		void crawl(String url) throws IOException, InterruptedException {
			Document doc = Jsoup.connect(url).timeout(300000).get();
			Thread.sleep(3000);
			Element table = doc.select("div#chemicals_product>table").get(1).select("td>table").get(0);
			synonym = table.select("tr:contains(synonym)").isEmpty() ? ""
					: table.select("tr:contains(synonym)").first().select("td").get(1).text();
			molecularWeight = table.select("tr:contains(weight)").isEmpty() ? ""
					: table.select("tr:contains(weight)").first().select("td").get(1).text();
			molecularFormula = table.select("tr:contains(formula)").isEmpty() ? ""
					: table.select("tr:contains(formula)").first().select("td").get(1).text();
			description = doc.select("div#chemicals_product>table>tbody>tr>td>div").get(3).text();
			if (description.equals("Description")) {
				description = doc.select("div#chemicals_product>table>tbody>tr>td>div").get(4).text();
			}
		}

		@Override
		public String toString() {
			return String.format(
					"name:%s %n cas:%s %n catelog:%s %n quantity:%s %n price:%s %n citation:%s %n rating:%s %n application:%s %n url:%s %n synonym:%s %n description:%s %n molecularWeight:%s %n molecularFormular:%s %n",
					name, cas, catelog, quantity, price, citation, rating, application, url, synonym, description,
					molecularWeight, molecularFormula);
		}
	}
}
