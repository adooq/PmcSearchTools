package selleck.email.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
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
 * @author lyh 2016.6 抓取竞争对手价格数据
 * 
 */
public class QueryCompetitorPrice {

	final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
	List<Product> products = new LinkedList<Product>();

	public static void main(String args[]) throws Exception {
		List<String> queries = readExcel();
		for (final String query : queries) {
			final Class<QueryCompetitorPrice> clazz = QueryCompetitorPrice.class;
			java.lang.reflect.Method[] methods = clazz.getMethods();
			for (final java.lang.reflect.Method m : methods) {
				// 每个crawl开头的方法都是一个需要抓取的网站，开一个新的线程进行抓取。
				// 然而似乎clazz被锁了，实际还是顺序执行的，待改进。
				if (m.getName().startsWith("crawl")) {
					// new Thread(new Runnable() {
					// @Override
					// public void run() {
					try {
						QueryCompetitorPrice qcp = clazz.newInstance();
						System.out.println("开始抓取 " + m.getName().substring(5) + " " + query);
						m.invoke(qcp, query);
						qcp.saveProduct();
					} catch (Exception e) {
						e.printStackTrace();
					}
					// }}).run();
				}
			}
		}

	}

	private static List<String> readExcel() {
		List<String> products = new LinkedList<>();
		Workbook wb = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream("product.xlsx"));
			Sheet sheet = wb.getSheetAt(0);
			for (int i = 0; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				products.add(row.getCell(0).getStringCellValue());
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
		return products;
	}

	public void crawlCayman(String query) throws IOException {
		String url = "https://www.caymanchem.com/solr/cayman/select?bf=recip(rord(introduction_date)%2C1%2C1000%2C1000)&defType=edismax&enableElevation=true&fl=catalog_num%2Ccatalog_num_suffix%2Ccas_no%2Cname%2Cmarkup_name%2Csynonym%2Ctagline%2Ckey_information%2Cpreamble%2Ckeywords%2Cintroduction_date%2Cmojo%2Cscore%2Csize%2Csize_unpriced%2Csku%2Clibrary%2Cproduct_qualifier_name&forceElevation=true&mm=50%25&qf=catalog_num%5E1500+catalog_num_suffix%5E10+supplier_catalog_num%5E1000+discontinued_id+cas_no%5E1000+name%5E1000+synonym%5E100+tagline%5E0.01+key_information%5E0.01+preamble%5E0.01+keywords+product_qualifier_id%5E0.01+ngram_name%5E100+ngram_general%5E0.01+sku+library%5E0.01+inchi%5E1000+inchikey%5E1000+smiles%5E1000&rows=100&spellcheck=true&spellcheck.collate=true&spellcheck.count=10&spellcheck.extendedResults=true&spellcheck.onlyMorePopular=false&start=0&serial=0&version=2.2&wt=xml&q=%22"
				+ URLEncoder.encode(query, "UTF-8") + "%22";
		Document doc = Jsoup.connect(url).cookie("countryRegionId", "US").userAgent(USER_AGENT).timeout(300000).get();
		Elements docs = doc.getElementsByTag("doc");
		for (Element e : docs) {
			Product p = new Product();
			p.queryName = query;
			p.resultName = e.getElementsByAttributeValue("name", "name").first().text();
			p.cas = e.getElementsByAttributeValue("name", "cas_no").first().text();
			p.url = "https://www.caymanchem.com/product/"
					+ e.getElementsByAttributeValue("name", "catalog_num").first().text();
			Elements prices = e.getElementsByAttributeValue("name", "size").first().getElementsByTag("str");
			for (Element priceLine : prices) {
				String[] temp = priceLine.text().split("\\$");
				p.priceMap.put(temp[0], "$" + temp[1]);
			}
			System.out.println(p);
			products.add(p);
		}
	}

	public void crawlMce(String query) throws IOException, InterruptedException {
		String url = "http://www.medchemexpress.com/mce_searchproduct.shtml?productParam="
				+ URLEncoder.encode(query, "UTF-8");
		Document doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(300000).get();
		Elements elements = doc.select(".cpd_list_tbl>tbody>tr");
		elements.remove(0);// 去掉标题行
		for (Element ele : elements) {
			Product p = new Product();
			p.queryName = query;
			p.resultName = ele.select("td").first().text();
			p.url = ele.select("td>strong>a").first().absUrl("href");
			getMcePrice(p);
			System.out.println(p);
			products.add(p);
		}

	}

	private void getMcePrice(Product p) throws IOException, InterruptedException {
		Thread.sleep(3000);
		Document doc = Jsoup.connect(p.url).cookie("mce_country_name", "United%20States")
				.cookie("mce_country_type", "1").cookie("mce_cart_price_type", "%24").userAgent(USER_AGENT)
				.timeout(300000).get();
		p.cas = doc.select("tbody>tr:contains(CAS No)>td").text();
		Elements elements = doc.select(".price_info>table>tbody>tr");
		elements.remove(0);
		for (Element e : elements) {
			Elements td = e.select("td");
			if (td.size() == 4 && td.get(2).text().contains("In-stock") && td.get(1).text().contains("$")) {
				p.priceMap.put(td.get(0).text(), td.get(1).text());
			}
		}

	}

	public void crawlBiovision(String query) throws IOException {
		String url = "http://www.biovision.com/search/results.html?keywords=" + URLEncoder.encode(query, "UTF-8");
		Document document = Jsoup.connect(url).userAgent(USER_AGENT).timeout(300000).get();
		for (Element ele : document.select("tr.productListing-odd,tr.productListing-odd")) {
			Product p = new Product();
			p.queryName = query;
			p.resultName = ele.select("td").get(1).text();
			p.url = ele.select("td a").first().absUrl("href");
			p.priceMap.put(ele.select("td").get(2).text(), ele.select("td").get(3).text());
			System.out.println(p);
			products.add(p);
		}
	}

	public void crawlAxon(String query) throws IOException, InterruptedException {
		String url = "http://us.axonmedchem.com/catalogsearch/result/?cat=0&q=" + URLEncoder.encode(query, "UTF-8");
		Elements eles = Jsoup.connect(url).userAgent(USER_AGENT).timeout(300000).get().select("tr.hreview-aggregate");

		for (Element ele : eles) {
			Product p = new Product();
			p.queryName = query;
			p.url = ele.select("td.id>a").first().absUrl("href");
			p.resultName = ele.select("td.name span").first().text();
			getAxonPrice(p);
			System.out.println(p);
			products.add(p);
		}
	}

	private void getAxonPrice(Product p) throws InterruptedException, IOException {
		Thread.sleep(3000);
		Document document = Jsoup.connect(p.url).userAgent(USER_AGENT).get();
		p.cas = document.select(".cas").first().text();
		p.cas = p.cas.substring(p.cas.indexOf("[") + 1, p.cas.indexOf("]"));
		for (Element ele : document.select("table.product-options-list>tbody>tr")) {
			p.priceMap.put(ele.select("td.size").first().text(), ele.select("td.price").first().text());
		}
	}

	public void crawlSigma(String query) throws IOException, InterruptedException {

		String url = "http://www.sigmaaldrich.com/catalog/search?term=" + URLEncoder.encode(query, "UTF-8")
				+ "&interface=All&N=0&mode=match%20partialmax&lang=en&region=US&focus=product";
		Response res = Jsoup.connect(url).userAgent(USER_AGENT).cookie("country", "USA").timeout(300000)
				.method(Method.GET).execute();
		Map<String, String> cookies = res.cookies();
		Document doc = res.parse();
		for (Element ele : doc.select("div.productContainer")) {
			Product p = new Product();
			p.queryName = query;
			p.resultName = ele.select("h2.name").text();
			p.cas = ele.select("p:contains(CAS Number)>span.info").isEmpty() ? ""
					: ele.select("p:contains(CAS Number)>span.info").first().text();
			if (ele.select("li.productNumberValue>a").isEmpty()) {
				continue;
			}
			p.url = ele.select("li.productNumberValue>a").first().absUrl("href");
			p.priceMap = getSigmaPrice(cookies, ele);
			System.out.println(p);
			products.add(p);
		}
	}

	private Map<String, String> getSigmaPrice(Map<String, String> cookies, Element ele)
			throws InterruptedException, IOException {
		Map<String, String> result = new LinkedHashMap<>();

		String productNumber = ele.select("li.productNumberValue>a").first().text();
		String divid = ele.select("li.priceValue>div").first().id();
		String brand = divid.substring(divid.indexOf(productNumber) + productNumber.length(), divid.length());

		String postUrl = "http://www.sigmaaldrich.com/catalog/PricingAvailability.do?productNumber=" + productNumber
				+ "&brandKey=" + brand + "&divId=" + divid;

		Thread.sleep(3000);
		Document doc2 = Jsoup.connect(postUrl).userAgent(USER_AGENT).data("brand", brand)
				.data("productNumber", productNumber).data("divId", divid).data("loadFor", "SR_RS")
				.cookie("SialLocaleDef", "CountryCode~US|WebLang~-1|").cookie("country", "USA").cookies(cookies)
				.method(Method.POST).execute().parse();
		Elements es = doc2.select("tr[id*=" + productNumber + "]");
		for (Element e : es) {
			result.put(e.select("td.sku").text(), e.select("td.price").text());
		}
		return result;
	}

	private void printMap(Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			System.out.println(entry.getKey() + "     " + entry.getValue());
		}
	}

	public void crawlTocris(String query) throws IOException, InterruptedException {

		Document doc = Jsoup
				.connect("https://www.tocris.com/search.php?Value=" + URLEncoder.encode(query, "UTF-8")
						+ "&Type=QuickSearch&SrchdFrom=Header&srchId=59a35c33a6880088be1d31bd060dc1058c27cabc&srch=59a35c33a6880088be1d31bd060dc1058c27cabc&search.x=21&search.y=10")
				.userAgent(USER_AGENT).cookie("country", "290").timeout(300000).get();
		if (doc.text().contains("Search Results")) {
			Elements eles = doc.select("#srchRes>tbody>tr>td>a");
			for (Element e : eles) {
				Thread.sleep(3000);
				Document doc2 = Jsoup.connect(e.absUrl("href")).userAgent(USER_AGENT).cookie("country", "290")
						.timeout(300000).get();
				getTocrisPrice(doc2, query);
			}
		} else {
			getTocrisPrice(doc, query);
		}
	}

	private void getTocrisPrice(Document doc, String query) {
		Product p = new Product();
		p.queryName = query;
		p.resultName = doc.select("#pDdb1A>h1").first().text();
		Elements eles1 = doc.select(".pDunits");
		for (Element e : eles1) {
			e.select("a").remove();
			String priceStr = e.text();
			p.priceMap.put(priceStr.split("/")[0], priceStr.split("/")[1]);
		}
		Elements eles2 = doc.select("div.pDbdA>a.pDpropT");
		for (int i = 0; i < eles2.size(); i++) {
			if (eles2.get(i).text().contains("CAS No")) {
				p.cas = doc.select("div.pDbdA>div.pDprop").get(i).text();
				break;
			}
		}
		p.url = doc.select("meta[property$=url]").first().attr("content");
		System.out.println(p);
		products.add(p);

	}

	private void saveProduct() {
		XSSFWorkbook wb = null;
		FileOutputStream out = null;
		try {
			wb = new XSSFWorkbook(new FileInputStream("price.xlsx"));
			out = new FileOutputStream("price.xlsx");
			XSSFSheet sheet = wb.getSheetAt(0);
			for (Product p : products) {
				XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
				row.createCell(0).setCellValue(p.queryName);
				row.createCell(1).setCellValue(p.resultName);
				row.createCell(3).setCellValue(p.cas);
				row.createCell(2).setCellValue(p.priceToString());
				row.createCell(4).setCellValue(p.url);
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
		String queryName;
		String resultName;
		String cas;
		Map<String, String> priceMap = new LinkedHashMap<>();
		String url;

		private String priceToString() {
			String price = "";
			for (Map.Entry<String, String> entry : priceMap.entrySet()) {
				price += entry.getKey() + "   " + entry.getValue() + "\r\n";
			}
			return price;
		}

		@Override
		public String toString() {
			return String.format("query:%s%nName:%s%nCAS:%s%nurl:%s%n" + priceToString(), queryName, resultName, cas,
					url);
		}
	}
}
