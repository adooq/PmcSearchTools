package selleck.email.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.interest.beans.ProductClass;
import selleck.email.service.IProductService;
import selleck.email.service.impl.ProductServiceImpl;
import selleck.utils.Constants;

public class DownloadNCBIResultForJiaoLei {

	/**
	 * 功能：自动搜索产品表(239测试库)里cat号S开头并且上架(online=1)的产品在PubMed(http://www.ncbi.nlm.
	 * nih.gov/pubmed/advanced)中的相关文章数量。 最终在同级目录下生成一个"ncbi count.csv"
	 * ，第一列是cat号，第二列是年份YYYY，第三列是相关文章数量。 程序需要运行12小时左右。 执行方法： java -jar
	 * DownloadNCBIResultForJiaoLei.jar 需求人：焦磊 作者： 蔡福生
	 * 
	 * @param args
	 *            数据库连接配置 args[0]:ip , args[1]:port , args[2]:user ,
	 *            args[3]:password
	 */
	public static void main(String[] args) {
		 Properties prop = new Properties();
		 prop.setProperty("ip", args[0]);
		 prop.setProperty("port", args[1]);
		 prop.setProperty("user", args[2]);
		 prop.setProperty("password", args[3]);
		 System.out.println("ip: "+args[0]);
		 System.out.println("port: "+args[1]);
		 System.out.println("user: "+args[2]);
		 System.out.println("password: "+args[3]);
		IProductService productService = new ProductServiceImpl(Constants.DYNAMIC,prop);
//		 IProductService productService = new
//		 ProductServiceImpl(Constants.LOCAL239); // for test
		Criteria criteria = new Criteria();
		 criteria.setWhereClause(" !(sql_name is null or sql_name = '') and INSTR(cat,'S') = 1 and online = 1");	
		List<ProductClass> productMap = productService.getProductsByCriteria(criteria);
		String domain = "http://www.ncbi.nlm.nih.gov/pubmed/?term=";
		BufferedWriter bw = null;
		Calendar rightNow = Calendar.getInstance();
		int currentYear = rightNow.get(Calendar.YEAR);
		try {
			File csv = new File("ncbi count.csv"); // 最终输出CSV文件
			if (csv.exists()) {
				csv.delete();
			}
			csv.createNewFile();
			bw = new BufferedWriter(new FileWriter(csv, true));

			for (ProductClass product : productMap) {
				String sqlName = product.getSql_name();

				/*
				 * // for test if(product.getId() < 50 || product.getId() >=
				 * 100){ continue; }
				 */

				// http://www.ncbi.nlm.nih.gov/pubmed/?term=Rapamycin[All
				// Fields] OR Sirolimus[All Fields] AND ("2014/01/01"[PDAT] :
				// "2014/12/31"[PDAT])
				if (sqlName.endsWith("|")) {
					sqlName = sqlName.substring(0, sqlName.length() - 1);
				}
				sqlName = sqlName.replaceAll("\\|", "[All Fields] OR ") + "[All Fields]";
				try {
					for (int i = 0; i < 5; i++) {
						String term = sqlName + " AND (\"" + (currentYear - i) + "/01/01\"[PDAT] : \""
								+ (currentYear - i) + "/12/31\"[PDAT])";
						try {
							term = URLEncoder.encode(term, "utf-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						String targetURL = domain + term;
						try {
							Map<String, String> articleListResult = selleck.utils.HTTPUtils
									.getCookieUrlAndHtml(targetURL, null, null, HTTPUtils.GET, null);
							Thread.sleep(2000);
							String html = articleListResult.get("html");
							String count = "0";
							// id="resultcount" value="2367"
							Pattern p = Pattern.compile("id=\"resultcount\" value=\"\\d+\"");
							Matcher matcher = p.matcher(html);
							if (matcher.find()) {
								count = matcher.group().replaceAll("id=\"resultcount\" value=\"", "").replaceAll("\"",
										"");
							}
							String newLine = product.getCat() + "," + (currentYear - i) + "," + count;
							System.out.println(newLine);
							bw.write(newLine);
							bw.newLine();
							bw.flush();
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
