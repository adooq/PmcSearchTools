package selleck.email.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


public class CountPMCJournalArticles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File f = new File("e:\\journal.txt");
		BufferedReader reader = null;
		StringBuffer sb = new StringBuffer();
		String journal = "";
		try {
			reader = new BufferedReader(new FileReader(f));
			String tempString = null;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				// 显示行号
				sb.append(tempString);
			}
			journal = sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		String[] jArr = journal.split("\\|");
		// System.out.println("jArr size: "+jArr.length);
		
		int i = 0;
		for(String j : jArr){
//			// for test
//			i++;
//			if(i < 30){
//				continue;
//			}
//			if(i > 40){
//				break;
//			}
						
			System.out.print(j.trim());
			if(j.trim().isEmpty()){
				continue;
			}
			
	
			String baseURL = "http://www.ncbi.nlm.nih.gov/pmc/?term=";
			String queryString = "(\""+j.trim()+"\"[Journal]) AND (\"2009/01/01\"[Publication Date] : \"3000\"[Publication Date])";
			try {
				queryString = java.net.URLEncoder.encode(queryString, "utf-8");
				String cookie = "__utma=116886681.1450961827.1395301408.1395301408.1395301408.1; __utmz=116886681.1395301408.1.1.utmcsr=ncbi.nlm.nih.gov|utmccn=(referral)|utmcmd=referral|utmcct=/pccompound; pmc.article.report=classic; ncbi_sid=F4FB5202358F9411_0084SID; WebEnv=1vGMur3lMzWqYf8IhaE0jyYoK-pkjY0oibrVmzwNVIBsQIZHMxj91CHV6RTiCAnWT0YsIvgL4HCX94-SzXi086PlCxh5G0FC-Kp%40F4FB5202358F9411_0084SID; clicknext=link_id%3Dsearch%26link_text%3DSearch%26link_class%3Dbutton_search%252Cnowrap%26browserwidth%3D1440%26browserheight%3D808%26evt_coor_x%3D1104%26evt_coor_y%3D49%26jseventms%3D2f69da%26iscontextmenu%3Dfalse%26jsevent%3Dclicknext%26ancestorId%3DEntrezForm%26ancestorClassName%3Dnowrap%2Csearch_form%2Csearch%2Cheader%26maxScroll_x%3D0%26maxScroll_y%3D2200%26currScroll_x%3D0%26currScroll_y%3D0%26hasScrolled%3Dtrue%26ncbi_phid%3DF4FC0BA7395714210000000000C5D95C%26sgSource%3Dnative; prevsearch=jsevent%3Dsearchnext%26ncbi_app%3Dentrez%26ncbi_db%3Dpmc%26ncbi_pdid%3Ddocsum%26searchdb%3Dpmc%26searchtext%3D%28%2522Acta%2520Ortopedica%2520Brasileira%2522%255BJournal%255D%29%2520AND%2520%28%25222009%252F01%252F01%2522%255BPublication%2520Date%255D%2520%253A%2520%25223000%2522%255BPublication%2520Date%255D%29%26ncbi_phid%3DF4FC0BA7395714210000000000C5D95C%26ncbi_timesinceload%3D295177%26jseventms%3D2f69dp%26sgSource%3Dapi; ncbi_prevPHID=F4FC0BA7395714210000000000C5D95C; unloadnext=jsevent%3Dunloadnext%26ncbi_pingaction%3Dunload%26ncbi_timeonpage%3D295303%26ncbi_onloadTime%3D281%26jsperf_dns%3D0%26jsperf_connect%3D1%26jsperf_ttfb%3D1357%26jsperf_basePage%3D3%26jsperf_frontEnd%3D285%26jsperf_navType%3D0%26jsperf_redirectCount%3D0%26maxScroll_x%3D0%26maxScroll_y%3D2200%26currScroll_x%3D0%26currScroll_y%3D0%26hasScrolled%3Dtrue%26ncbi_phid%3DF4FC0BA7395714210000000000C5D95C%26sgSource%3Dnative";
				// System.out.println(baseURL+queryString);
				String htmlStr = getHtmlStr(baseURL+queryString , cookie);
				// System.out.println(htmlStr);
				
				// 获取文章总数
				String regex = "<h2 class=\"result_count\">Results:.+</h2>";
				Pattern p = Pattern.compile(regex);
				Matcher matcher = p.matcher(htmlStr);
				if(matcher.find()){
					String countStr = matcher.group();
					countStr = countStr.replace("<h2 class=\"result_count\">Results:", "").replace("</h2>", "").trim();
					String count = countStr.replaceAll("\\d+ to \\d+ of", "").trim();
					System.out.println(" , "+count + " , " + countStr);
				}else{
					System.out.println(" , "+0 + " , " + "no results count");
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private static String getHtmlStr(String targetURL , String cookie) {
		CloseableHttpClient httpclient = null;
			httpclient = HttpClientBuilder.create().build();
			HttpGet httpGet = new HttpGet(targetURL);
			httpGet.setHeader("Cookie", cookie);
			HttpResponse response = null;
			RequestConfig requestConfig = RequestConfig.custom()  
				    .setConnectionRequestTimeout(300000).setConnectTimeout(300000)  
				    .setSocketTimeout(300000).build();  
			httpGet.setConfig(requestConfig);  
			try {
				response = httpclient.execute(httpGet);
			} catch (ClientProtocolException e1) {
				e1.printStackTrace();
				System.out.println("httpclient.execute(httpGet) ClientProtocolException");
				// logger.append(" httpclient.execute(httpGet) ClientProtocolException\n");
				if(httpclient != null){
					try {
						httpclient.close();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}
				return "";
			} catch (IOException e1) {
				System.out.println("httpclient.execute(httpGet) IOException");
				// logger.append(" httpclient.execute(httpGet) IOException\n");
				e1.printStackTrace();
				if(httpclient != null){
					try {
						httpclient.close();
					} catch (IOException e) {
						//e.printStackTrace();
					}
				}
				return "";
			}

			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				// 请求成功
				// 取得请求内容
				HttpEntity entity = response.getEntity();

				// 显示内容
				// System.out.print("  " + entity.getContentType());
				// logger.append("  " + entity.getContentType());
				if(entity != null && entity.getContentType().toString().contains("html")){
					try {
						byte[] contentArray = EntityUtils.toByteArray(entity);
						if (contentArray.length > 0) {
							String contentType = entity.getContentType().toString();
							int charsetIndex = contentType.indexOf("charset=");
							String encoding = charsetIndex > -1 ? contentType.substring(charsetIndex+8) : null;
							// System.out.println("encoding: "+encoding);
							String htmlStr = new String(contentArray,encoding == null ? "iso-8859-1" : encoding);
							return htmlStr;
						}
					} catch (IllegalStateException e) {
						System.out.println("IllegalStateException");
						// logger.append(" IllegalStateException\n");
						//e.printStackTrace();
					} catch (IOException e) {
						System.out.println("html read IOException");
						// logger.append(" html read IOException\n");
						//e.printStackTrace();
					}
					
				}else{
					System.out.println(" 不是网页");
					// logger.append(" 不是网页" + "\n");
				}
				
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					System.out.println("  EntityUtils.consume(entity)  IOException");
					// logger.append("  EntityUtils.consume(entity)  IOException" + "\n");
					//e.printStackTrace();
				}
			}else{
				System.out.println("  访问失败 "+response.getStatusLine().getStatusCode());
				// logger.append("  下载请求失败" + "\n");
			}
			
			return "";
		}
	
}
